package sua.autonomouscar.infrastructure.driving;

import org.osgi.framework.BundleContext;

import sua.autonomouscar.devices.interfaces.IDistanceSensor;
import sua.autonomouscar.devices.interfaces.IHumanSensors;
import sua.autonomouscar.devices.interfaces.ILineSensor;
import sua.autonomouscar.devices.interfaces.IRoadSensor;
import sua.autonomouscar.driving.interfaces.IFallbackPlan;
import sua.autonomouscar.driving.interfaces.IL1_AssistedDriving;
import sua.autonomouscar.driving.interfaces.IL2_AdaptiveCruiseControl;
import sua.autonomouscar.driving.interfaces.IL3_DrivingService;
import sua.autonomouscar.driving.interfaces.IL3_HighwayChauffer;
import sua.autonomouscar.driving.interfaces.IL3_TrafficJamChauffer;
import sua.autonomouscar.driving.l1.assisteddriving.L1_AssistedDriving;
import sua.autonomouscar.driving.l2.acc.L2_AdaptiveCruiseControl;
import sua.autonomouscar.driving.l3.highwaychauffer.L3_HighwayChauffer;
import sua.autonomouscar.driving.l3.trafficjamchauffer.L3_TrafficJamChauffer;
import sua.autonomouscar.infrastructure.OSGiUtils;
import sua.autonomouscar.infrastructure.devices.DistanceSensor;
import sua.autonomouscar.infrastructure.devices.LineSensor;
import sua.autonomouscar.interfaces.EFaceStatus;
import sua.autonomouscar.interfaces.ERoadStatus;
import sua.autonomouscar.interfaces.ERoadType;
import sua.autonomouscar.interfaces.IIdentifiable;

public abstract class L3_DrivingService extends L2_DrivingService implements IL3_DrivingService {

	public final static String REFERENCE_SPEED = "reference-speed";

	protected String humanSensors = null;
	protected String roadSensor = null;
	protected String fallbackPlan = null;

	protected int lateralSecurityDistance = 1;

	public L3_DrivingService(BundleContext context, String id) {
		super(context, id);
		this.addImplementedInterface(IL3_DrivingService.class.getName());
	}

	@Override
	public void setHumanSensors(String humanSensors) {
		this.humanSensors = humanSensors;
		return;
	}

	protected IHumanSensors getHumanSensors() {
		return OSGiUtils.getService(context, IHumanSensors.class,
				String.format("(%s=%s)", IIdentifiable.ID, this.humanSensors));
	}

	@Override
	public void setRoadSensor(String sensor) {
		this.roadSensor = sensor;
	}

	protected IRoadSensor getRoadSensor() {
		return OSGiUtils.getService(context, IRoadSensor.class,
				String.format("(%s=%s)", IIdentifiable.ID, this.roadSensor));
	}

	@Override
	public void setFallbackPlan(String plan) {
		this.fallbackPlan = plan;
		return;
	}

	protected IFallbackPlan getFallbackPlan() {
		return OSGiUtils.getService(context, IFallbackPlan.class,
				String.format("(%s=%s)", IIdentifiable.ID, this.fallbackPlan));
	}

	@Override
	public void setReferenceSpeed(int speed) {
		this.setProperty(L3_DrivingService.REFERENCE_SPEED, speed);
	}

	@Override
	public int getReferenceSpeed() {
		return (int) this.getProperty(L3_DrivingService.REFERENCE_SPEED);
	}

	@Override
	public IL3_DrivingService performTheTakeOver() {
		this.stopDriving();
		this.getNotificationService().notify("Exited Autonomous Mode");
		return this;
	}

	@Override
	public IL3_DrivingService activateTheFallbackPlan() {
		this.stopDriving();
		this.getFallbackPlan().startDriving();

		return this;
	}

	// Implementaci?n:

	/**
	 * Cambia el modo de conducci?n a L2 cuando sea posible, si no es posible cambia
	 * a modo de conducci?n L1.
	 */
	protected void tryChangeL2Driving() {
		this.getNotificationService().notify("Trying to change to L2 driving funcion");
		if (canDriveL2_ACCMode()) {
			changeToL2Driving();
		} else
			tryChangeL1Driving();
	}

	protected void tryChangeL1Driving() {

		if (canDriveL1Mode()) {
			changeL1Driving();
		} else {
			// ... o si no podemos, activamos el Fallback Plan
			this.debugMessage("Activating the Fallback Plan ...");
			this.activateTheFallbackPlan();
		}
	}

	protected boolean isRoadFluid() {
		return !(this.getRoadSensor().getRoadStatus() == ERoadStatus.COLLAPSED
				|| this.getRoadSensor().getRoadStatus() == ERoadStatus.JAM);
	}

	private void changeL1Driving() {
		this.getNotificationService().notify("Changing to L1 driving function");
		L1_AssistedDriving assistedControlDriving = new L1_AssistedDriving(context, "L1_AssistedDriving");
		assistedControlDriving.registerThing();

		IL1_AssistedDriving assistedDrivingControlService = OSGiUtils.getService(context, IL1_AssistedDriving.class);
		assistedDrivingControlService.setFrontDistanceSensor("FrontDistanceSensor");
		assistedDrivingControlService.setLeftLineSensor("LeftLineSensor");
		assistedDrivingControlService.setNotificationService("NotificationService");
		assistedDrivingControlService.setRightLineSensor("RightLineSensor");
		assistedDrivingControlService
				.setLongitudinalSecurityDistance(L1_AssistedDriving.DEFAULT_LONGITUDINAL_SECURITY_DISTANCE);

		this.debugMessage("The driver is ready to TakeOver ...");
		this.getNotificationService().notify("The driver is ready to TakeOver ...");
		this.performTheTakeOver();
		assistedDrivingControlService.startDriving();
	}

	protected boolean canDriveL1Mode() {
		return this.getHumanSensors().getFaceStatus() == EFaceStatus.LOOKING_FORWARD
				&& this.getHumanSensors().areTheHandsOnTheWheel() && this.getHumanSensors().isDriverSeatOccupied();
	}

	private void changeToL2Driving() {
		// Paramos el modo de conducci?n L3
		this.stopDriving();
		// Registramos modo conduccion L2
		L2_AdaptiveCruiseControl adaptativeCruiseControlDriving = new L2_AdaptiveCruiseControl(context,
				"L2_AdaptiveCruiseControl");
		adaptativeCruiseControlDriving.registerThing();

		// Registramos los componentes necesarios para el modo de conducci?n L2.
		IL2_AdaptiveCruiseControl adaptativeCruiseControlService = OSGiUtils.getService(context,
				IL2_AdaptiveCruiseControl.class);
		adaptativeCruiseControlService.setEngine("Engine");
		adaptativeCruiseControlService.setFrontDistanceSensor("FrontDistanceSensor");
		adaptativeCruiseControlService
				.setLongitudinalSecurityDistance(L2_AdaptiveCruiseControl.DEFAULT_LONGITUDINAL_SECURITY_DISTANCE);

		// Empezamos a conducir en el nivel L2.
		adaptativeCruiseControlService.startDriving();
		this.getNotificationService().notify("Changed to L2 driving funcion successfully");
	}

	public void change2CityChauffer() {

		this.stopDriving();

		IL3_TrafficJamChauffer TJChauffeur = OSGiUtils.getService(context, IL3_TrafficJamChauffer.class);
		TJChauffeur.setHumanSensors("HumanSensors");
		TJChauffeur.setRoadSensor("RoadSensor");
		TJChauffeur.setFallbackPlan("EmergencyFallBackPlan");
		TJChauffeur.setEngine("Engine");
		TJChauffeur.setSteering("Steering");
		TJChauffeur.setFrontDistanceSensor("FrontDistanceSensor");
		TJChauffeur.setRearDistanceSensor("RearDistanceSensor");
		TJChauffeur.setRightDistanceSensor("RightDistanceSensor");
		TJChauffeur.setLeftDistanceSensor("LeftDistanceSensor");
		TJChauffeur.setRightLineSensor("RightLineSensor");
		TJChauffeur.setLeftLineSensor("LeftLineSensor");
		TJChauffeur.setReferenceSpeed(L3_TrafficJamChauffer.DEFAULT_REFERENCE_SPEED);
		TJChauffeur.setLongitudinalSecurityDistance(L3_TrafficJamChauffer.DEFAULT_LONGITUDINAL_SECURITY_DISTANCE);
		TJChauffeur.setLateralSecurityDistance(L3_TrafficJamChauffer.DEFAULT_LATERAL_SECURITY_DISTANCE);
		TJChauffeur.setNotificationService("NotificationService");
		// attachSensors(TJChauffeur);

		this.startDriving();
	}

	/**
	 * Devuelve un boolean que representa si el coche aut?nomo puede funcionar con
	 * un modo de conducci?n aut?noma L3 Requirement ADS_L3-1: Deber? cambiar a L2
	 * cuando el tipo de carretera sea estandar o offroad.
	 * 
	 * @return
	 */
	protected boolean canDriveL3Mode() {
		return !(this.getRoadSensor().getRoadType() == ERoadType.OFF_ROAD
				|| this.getRoadSensor().getRoadType() == ERoadType.STD_ROAD);
	}

	protected boolean isRoadCity() {
		return this.getRoadSensor().getRoadType() == ERoadType.CITY;
	}
	
	protected boolean isRoadHighway() {
		return this.getRoadSensor().getRoadType() == ERoadType.HIGHWAY;
	}
	
	protected boolean isRoadNormal() {
		return this.getRoadSensor().getRoadType() == ERoadType.STD_ROAD;
	}
	

	protected void changeDrivingTrafficJamChauffer() {

		// Paramos el actual
		this.stopDriving();

		// Registramos el L3_TrafficJamChauffer
		L3_TrafficJamChauffer trafficJamChaufferDriving = new L3_TrafficJamChauffer(context, "L3_TrafficJamChauffer");
		trafficJamChaufferDriving.registerThing();

		// Inicializamos
		IL3_TrafficJamChauffer trafficJamChaufferDrivingService = OSGiUtils.getService(context,
				IL3_TrafficJamChauffer.class);
		trafficJamChaufferDrivingService.setRoadSensor("RoadSensor");
		trafficJamChaufferDrivingService.setHumanSensors("HumanSensors");
		trafficJamChaufferDrivingService.setEngine("Engine");
		trafficJamChaufferDrivingService.setSteering("Steering");
		trafficJamChaufferDrivingService.setFrontDistanceSensor("FrontDistanceSensor");
		trafficJamChaufferDrivingService.setLeftDistanceSensor("LeftDistanceSensor");
		trafficJamChaufferDrivingService.setRightDistanceSensor("RightDistanceSensor");
		trafficJamChaufferDrivingService.setRearDistanceSensor("RearDistanceSensor");
		trafficJamChaufferDrivingService.setLeftLineSensor("LeftLineSensor");
		trafficJamChaufferDrivingService.setRightLineSensor("RightLineSensor");
		trafficJamChaufferDrivingService.setNotificationService("NotificationService");
		trafficJamChaufferDrivingService.setFallbackPlan("EmergencyFallbackPlan");
		trafficJamChaufferDrivingService.setReferenceSpeed(L3_TrafficJamChauffer.DEFAULT_REFERENCE_SPEED);
		trafficJamChaufferDrivingService
				.setLongitudinalSecurityDistance(L3_TrafficJamChauffer.DEFAULT_LONGITUDINAL_SECURITY_DISTANCE);
		trafficJamChaufferDrivingService
				.setLateralSecurityDistance(L3_TrafficJamChauffer.DEFAULT_LATERAL_SECURITY_DISTANCE);

		trafficJamChaufferDrivingService.startDriving();
	}
	
	protected void changeDrivingHighwayChauffer() {
		
		this.stopDriving();
		
		L3_HighwayChauffer highwayChauffer = new L3_HighwayChauffer(context, "L3_HighwayChauffer");
		highwayChauffer.registerThing();
		
		IL3_HighwayChauffer highwayChaufferDrivingservice = OSGiUtils.getService(context, IL3_HighwayChauffer.class);
		highwayChaufferDrivingservice.setRoadSensor("RoadSensor");
		highwayChaufferDrivingservice.setHumanSensors("HumanSensors");
		highwayChaufferDrivingservice.setEngine("Engine");
		highwayChaufferDrivingservice.setSteering("Steering");
		highwayChaufferDrivingservice.setFrontDistanceSensor("FrontDistanceSensor");
		highwayChaufferDrivingservice.setLeftDistanceSensor("LeftDistanceSensor");
		highwayChaufferDrivingservice.setRightDistanceSensor("RightDistanceSensor");
		highwayChaufferDrivingservice.setRearDistanceSensor("RearDistanceSensor");
		highwayChaufferDrivingservice.setLeftLineSensor("LeftLineSensor");
		highwayChaufferDrivingservice.setRightLineSensor("RightLineSensor");
		highwayChaufferDrivingservice.setNotificationService("NotificationService");
		highwayChaufferDrivingservice.setFallbackPlan("EmergencyFallbackPlan");
		
		highwayChaufferDrivingservice.startDriving();
	}
	
	/*
	 * Seg?n el estado del conductor, activa los mecanismos de interacci?n adecuados
	 * Requisito INTERACT-1
	 */
	protected void checkDriverStatus() {
		switch (this.getHumanSensors().getFaceStatus()) {
		case LOOKING_FORWARD:
			if(this.getHumanSensors().areTheHandsOnTheWheel() && this.getHumanSensors().isDriverSeatOccupied()) {
				this.getNotificationService().removeAllInteractionMechanisms();
				this.getNotificationService().addInteractionMechanism("SteeringWheel_HapticVibration");
				this.getNotificationService().addInteractionMechanism("DriverDisplay_VisualText");
				this.getNotificationService().addInteractionMechanism("Speakers_AuditoryBeep");
			}
			break;
		case DISTRACTED:
			this.getNotificationService().notify("Please, look forward!");
			this.getNotificationService().removeAllInteractionMechanisms();
			this.getNotificationService().addInteractionMechanism("SteeringWheel_HapticVibration");
			this.getNotificationService().addInteractionMechanism("DriverDisplay_VisualText");
			this.getNotificationService().addInteractionMechanism("Speakers_AuditoryBeep");
			break;
		case SLEEPING:
			this.getNotificationService().notify("Please, WAKE UP! ... and look forward!");
			this.getNotificationService().removeAllInteractionMechanisms();
			this.getNotificationService().addInteractionMechanism("SteeringWheel_HapticVibration");
			this.getNotificationService().addInteractionMechanism("DriverSeat_HapticVibration");
			this.getNotificationService().addInteractionMechanism("Speakers_AuditorySound");
			break;
		default:
			break;
		}
	}
	
	/*
	 * Activa mecanismos de interacci?n seg?n situaci?n de "manos en el volante"
	 * Requisito INTERACT-2
	 */
	protected void checkHandsStatus() {
		if (!this.getHumanSensors().areTheHandsOnTheWheel()) {
			this.getNotificationService().notify("Please, put the hands on the wheel!");	
			this.getNotificationService().removeInteractionMechanism("SteeringWheel_HapticVibration");
		}
		else if(!this.getHumanSensors().isDriverSeatOccupied()) {
			this.getNotificationService().removeInteractionMechanism("DriverSeat_HapticVibration");
		}
		else {
			this.getNotificationService().addInteractionMechanism("SteeringWheel_HapticVibration");
		}
	}
	
	
	/*
	 * Activa mecanismos de interacci?n seg?n situaci?n de "ubicaci?n del conductor"
	 */
	protected void checkDriverSeatStatus() {
		if ( !this.getHumanSensors().isDriverSeatOccupied() ) {
			if ( this.getHumanSensors().isCopilotSeatOccupied() ) {
				this.getNotificationService().notify("Please, move to the driver seat!");
				this.getNotificationService().removeInteractionMechanism("DriverSeat_HapticVibration");
				this.getNotificationService().removeInteractionMechanism("DriverDisplay_VisualText");
			}
			else {
				// No se puede conducir en L3 sin conductor. Activamos plan de emergencia
				this.getNotificationService().notify("Cannot drive with a driver! Activating the Fallback Plan ...");
				this.activateTheFallbackPlan();
			}
		}
		else {
			this.getNotificationService().addInteractionMechanism("DriverSeat_HapticVibration");
			this.getNotificationService().addInteractionMechanism("DriverDisplay_VisualText");

		}
	}
	
	/*
	 * Requisitos ADS_L3-7 y ADS_L3-8
	 * */
	protected void sensorFail() {
		// Si falla alg?n distanceSensor, emplear el LIDAR
		
		if (!allWorking()) {
			this.debugMessage("[L3_DrivingService] Sensor fail encountered, attempting to resolve the issue...");
			this.getNotificationService().notify("[L3_DrivingService] Sensor fail encountered, attempting to resolve the issue...");
			
			//Si alguno falla, cogemos los del LIDAR
			if (LIDARWorking()) {
				
				//Si est?n disponibles y en funcionamiento, seteamos los sensores del LIDAR
				setLIDARSensors();
			} else {
				this.debugMessage("[L3_DrivingService] Failed to resolve the issue, activating fallback plan");
				this.getNotificationService().notify("[L3_DrivingService]  Failed to resolve the issue, activating fallback plan");
				/* Si no, paramos conducci?n y tomamos el control
				 * ParkInTheRoadFallBack plan: Requiere una v?a r?pida o normal, acceso al control del motor y steering,
				 * y sensores de distancia lateral y de carril derechos.
				 * No se consideran los fallos en el motor o steering
				 * 
				 * 1. Paramos el de conducir 
				 * */
				this.stopDriving();
				
				if (canBeSetParkFallbackPlan()) {
					
					setFallbackPlan("ParkInTheRoadShoulderFallbackPlan");
					activateFallback();
				} else {
					//Todo ha fallado, iniciar el EmergencyFallbackPlan
					
					this.debugMessage("[L3_DrivingService] Everything has failed, activating last resort. If you survive, remember that this company can't be held responsible against" + 
							"any harm caused to you or the environment that surrounds you. For more information, visit http://www.thiscompany.com/terms-of-use/. Best of luck!");
					this.getNotificationService().notify("[L3_DrivingService] Everything has failed, activating last resort. If you survive, remember that this company can't be held responsible against"
							+ "any harm caused to you or the environment that surrounds you. For more information, visit http://www.thiscompany.com/terms-of-use/. Best of luck!");
					
					setFallbackPlan("EmergencyFallbackPlan");
					activateFallback();
					
				}
				
			}
		}
	}
	
	protected void setEmergencyPlan() {
		if (getFallbackPlan() == null) {
			if (canBeSetParkFallbackPlan()) {
				setFallbackPlan("ParkInTheRoadShoulderFallbackPlan");
			} else {
				setFallbackPlan("EmergencyFallbackPlan");
			}
		}
	}
	
	protected boolean canBeSetParkFallbackPlan() {
		IDistanceSensor RightDistanceSensor = OSGiUtils.getService(context, IDistanceSensor.class, "(" + IIdentifiable.ID + "=RightDistanceSensor)");			
		ILineSensor RightLineSensor = OSGiUtils.getService(context, ILineSensor.class, "(" + IIdentifiable.ID + "=RightLineSensor)");
		
		if ((isRoadHighway() || isRoadNormal()) && ((DistanceSensor) RightDistanceSensor).isWorking()
				&& ((LineSensor) RightLineSensor).isWorking()) {
			return true;
		}
		
		return false;
	}
	
	protected IL3_DrivingService activateFallback() {
		this.stopDriving();
		getFallbackPlan().startDriving();
		
		return this;
	}
	
	protected void setLIDARSensors() {
		this.setFrontDistanceSensor("LIDAR-FrontDistanceSensor");
    	this.setRearDistanceSensor("LIDAR-RearDistanceSensor");
    	this.setRightDistanceSensor("LIDAR-RightDistanceSensor");
    	this.setLeftDistanceSensor("LIDAR-LeftDistanceSensor");
	}
	
	protected void setNormalSensors() {
		setFrontDistanceSensor("FrontDistanceSensor");
		setLeftDistanceSensor("LeftDistanceSensor");
		setRightDistanceSensor("RightDistanceSensor");
		setRearDistanceSensor("RearDistanceSensor");
	}
	
	/* Parte del requisito ADS_L3-7
	 * L3 tiene todos los sensores
	 * */
	protected boolean allWorking() {
		IDistanceSensor FrontDistanceSensor = OSGiUtils.getService(context, IDistanceSensor.class, "(" + IIdentifiable.ID + "=FrontDistanceSensor)");
		IDistanceSensor RearDistanceSensor = OSGiUtils.getService(context, IDistanceSensor.class, "(" + IIdentifiable.ID + "=RearDistanceSensor)");
		IDistanceSensor RightDistanceSensor = OSGiUtils.getService(context, IDistanceSensor.class, "(" + IIdentifiable.ID + "=RightDistanceSensor)");
		IDistanceSensor LeftDistanceSensor = OSGiUtils.getService(context, IDistanceSensor.class, "(" + IIdentifiable.ID + "=LeftDistanceSensor)");
		
		return ((DistanceSensor) FrontDistanceSensor).isWorking() && ((DistanceSensor) RearDistanceSensor).isWorking()
				&& ((DistanceSensor) RightDistanceSensor).isWorking() && ((DistanceSensor) LeftDistanceSensor).isWorking();
	}
	
	protected boolean LIDARWorking() {
		IDistanceSensor FrontDistanceSensor = OSGiUtils.getService(context, IDistanceSensor.class, "(" + IIdentifiable.ID + "=LIDAR-FrontDistanceSensor)");
		IDistanceSensor RearDistanceSensor = OSGiUtils.getService(context, IDistanceSensor.class, "(" + IIdentifiable.ID + "=LIDAR-RearDistanceSensor)");
		IDistanceSensor RightDistanceSensor = OSGiUtils.getService(context, IDistanceSensor.class, "(" + IIdentifiable.ID + "=LIDAR-RightDistanceSensor)");
		IDistanceSensor LeftDistanceSensor = OSGiUtils.getService(context, IDistanceSensor.class, "(" + IIdentifiable.ID + "=LIDAR-LeftDistanceSensor)");
		
		return ((DistanceSensor) FrontDistanceSensor).isWorking() && ((DistanceSensor) RearDistanceSensor).isWorking()
				&& ((DistanceSensor) RightDistanceSensor).isWorking() && ((DistanceSensor) LeftDistanceSensor).isWorking();
	}
	
	/* Requisito ADS-1
	 * En caso de estar disponibles, el servicio usar? los mejores sensores disponibles
	 * */
	protected void setBetterSensors() {
		//check si est? el LIDAR activado
		
		if (LIDARWorking()) {
			if (allWorking()) {
				setNormalSensors();
			}
		}
	}
	
}
