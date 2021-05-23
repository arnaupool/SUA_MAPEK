package sua.autonomouscar.infrastructure.driving;

import org.osgi.framework.BundleContext;

import sua.autonomouscar.devices.interfaces.IHumanSensors;
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

	// Implementación:

	/**
	 * Cambia el modo de conducción a L2 cuando sea posible, si no es posible cambia
	 * a modo de conducción L1.
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
		// Paramos el modo de conducción L3
		this.stopDriving();
		// Registramos modo conduccion L2
		L2_AdaptiveCruiseControl adaptativeCruiseControlDriving = new L2_AdaptiveCruiseControl(context,
				"L2_AdaptiveCruiseControl");
		adaptativeCruiseControlDriving.registerThing();

		// Registramos los componentes necesarios para el modo de conducción L2.
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
	 * Devuelve un boolean que representa si el coche autónomo puede funcionar con
	 * un modo de conducción autónoma L3 Requirement ADS_L3-1: Deberá cambiar a L2
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
	 * Según el estado del conductor, activa los mecanismos de interacción adecuados
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


}
