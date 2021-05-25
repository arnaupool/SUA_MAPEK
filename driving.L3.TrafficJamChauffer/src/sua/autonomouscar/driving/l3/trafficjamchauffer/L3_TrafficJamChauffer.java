package sua.autonomouscar.driving.l3.trafficjamchauffer;

import org.osgi.framework.BundleContext;

import sua.autonomouscar.devices.interfaces.ISpeedometer;
import sua.autonomouscar.driving.interfaces.IDrivingService;
import sua.autonomouscar.driving.interfaces.IL3_DrivingService;
import sua.autonomouscar.driving.interfaces.IL3_TrafficJamChauffer;
import sua.autonomouscar.infrastructure.OSGiUtils;
import sua.autonomouscar.infrastructure.devices.Engine;
import sua.autonomouscar.infrastructure.devices.Steering;
import sua.autonomouscar.infrastructure.driving.L3_DrivingService;

public class L3_TrafficJamChauffer extends L3_DrivingService implements IL3_TrafficJamChauffer {
	
	public static final int DEFAULT_LONGITUDINAL_SECURITY_DISTANCE = 6000;
	public static final int DEFAULT_LATERAL_SECURITY_DISTANCE = 150;
	public static final int DEFAULT_REFERENCE_SPEED = 60;

	public static final int MY_FINE_ACCELERATION_RPM = 5;
	public static final int MY_SMOOTH_ACCELERATION_RPM = 50;
	public static final int MY_MEDIUM_ACCELERATION_RPM = 100;
	public static final int MY_HIGH_ACCELERATION_RPM = 200;
	public static final int MY_AGGRESSIVE_ACCELERATION_RPM = 300;

	public L3_TrafficJamChauffer(BundleContext context, String id) {
		super(context, id);
		this.addImplementedInterface(IL3_TrafficJamChauffer.class.getName());
		this.setReferenceSpeed(DEFAULT_REFERENCE_SPEED);
		this.setLongitudinalSecurityDistance(DEFAULT_LONGITUDINAL_SECURITY_DISTANCE);
		this.setLateralSecurityDistance(DEFAULT_LATERAL_SECURITY_DISTANCE);
	}

	@Override
	public IDrivingService performTheDrivingFunction() {
		
		// L3 traffic jam chauffer
		
		// Comprobamos que NO podemos mantener la conducción en nivel 3 de autonomia
		
		//Requisito ADS_L3-1
		if ( !canDriveL3Mode() ) {
			// No podemos seguir conduciendo de manera autónoma
			this.debugMessage("Cannot drive in L3 Autonomy level ...");
			this.getNotificationService().notify("Cannot drive in L3 Autonomy level ...");
			
			tryChangeL2Driving();
			return this;
		}
		
		//Requisito ADS_L3-5: Estando en TrafficJamChauffer
		if (isRoadCity()) {
				this.getNotificationService().notify("Changing to L3_CityChaffeur...");
				System.out.println("[L3_TrafficJamChauffer] Changing to L3_CityChaffeur");
				this.change2CityChauffer();
				return this;
			
		}
		
		//ADS_L3-4
		// Estando en traficJamChauffer via rapida descongestiona -> HigwayChauffer
		if(isRoadFluid()) {
			this.getNotificationService().notify("[L3_TrafficJamChauffer] Road fluid, changing to L3_HighwayChauffer");
			changeDrivingHighwayChauffer();
			return this;
		}
			

		//
		// Control de la función primaria: MOVIMIENTO LONGITUDINAL
		//
		
		boolean longitudinal_correction_performed = false;
		ISpeedometer speedometer = OSGiUtils.getService(context, ISpeedometer.class);
		int currentSpeed = speedometer.getCurrentSpeed();
		this.debugMessage(String.format("Current Speed: %d Km/h", currentSpeed));

		// Reducimos la velocidad un poco si detectamos distancia frontal inferior a distancia de seguridad
		if ( this.getLongitudinalSecurityDistance() > this.getFrontDistanceSensor().getDistance() ) {

			this.getEngine().decelerate(Engine.MEDIUM_ACCELERATION_RPM);
			longitudinal_correction_performed = true;
			this.debugMessage("Font Distance Warning: ⊼");
			this.getNotificationService().notify("Font Distance Warning: Braking ...");

		} else {
			

			// Intentamos acercarnos a la velocidad referencia
			int diffSpeed = this.getReferenceSpeed() - currentSpeed;
			
			int rpmCorrection = MY_FINE_ACCELERATION_RPM;
			String rpmAppliedCorrection = "fine";
			if ( Math.abs(diffSpeed) > 30 ) { rpmCorrection = MY_AGGRESSIVE_ACCELERATION_RPM; rpmAppliedCorrection = "aggressive"; }
			else if ( Math.abs(diffSpeed) > 15 ) { rpmCorrection = MY_HIGH_ACCELERATION_RPM; rpmAppliedCorrection = "high"; }
			else if ( Math.abs(diffSpeed) > 5 ) { rpmCorrection = MY_MEDIUM_ACCELERATION_RPM; rpmAppliedCorrection = "medium"; }
			else if ( Math.abs(diffSpeed) > 1 ) { rpmCorrection = MY_SMOOTH_ACCELERATION_RPM; rpmAppliedCorrection = "smooth"; }


			if ( diffSpeed > 0  ) {
				this.getEngine().accelerate(rpmCorrection);
				longitudinal_correction_performed = true;
				this.debugMessage(String.format("Accelerating (%s) to get the reference speeed of %d Km/h", rpmAppliedCorrection, this.getReferenceSpeed()));
			} else if ( diffSpeed < 0 ) {
				this.getEngine().decelerate(rpmCorrection);
				longitudinal_correction_performed = true;
				this.debugMessage(String.format("Decelerating (%s) to get the reference speeed of %d Km/h", rpmAppliedCorrection, this.getReferenceSpeed()));
			}
			
		}

		//
		// Control de la función primaria: MOVIMIENTO LATERAL
		//
		
		boolean lateral_correction_performed = false;
		// Control de las distancias laterales
		if ( this.getRightDistanceSensor().getDistance() < this.getLateralSecurityDistance() ) {
			if ( this.getLeftDistanceSensor().getDistance() > this.getLateralSecurityDistance() ) {
				this.debugMessage("Right Distance Warning: > @ . Turning the Steering to the left ...");
				this.getSteering().rotateLeft(Steering.SEVERE_CORRECTION_ANGLE);
				lateral_correction_performed = true;
			} else {
				this.debugMessage("Right and Left Distance Warning: @ <  > @ . Obstacles too close!!");
				this.getNotificationService().notify("Right and Left Distance Warning: @ <  > @ . Obstacles too close!!");
				lateral_correction_performed = true;
			}
		}

		if ( !lateral_correction_performed && 
			 this.getLeftDistanceSensor().getDistance() < this.getLateralSecurityDistance() ) {
			if ( this.getRightDistanceSensor().getDistance() > this.getLateralSecurityDistance() ) {
				this.debugMessage("Left Distance Warning: @ < . Turning the Steering to the right ...");
				this.getSteering().rotateRight(Steering.SEVERE_CORRECTION_ANGLE);
				lateral_correction_performed = true;
			}
		}

		if ( !lateral_correction_performed ) {
			// Control de la dirección si nos salimos del carril
			if ( this.getLeftLineSensor().isLineDetected() ) {
				this.getSteering().rotateRight(Steering.SMOOTH_CORRECTION_ANGLE);
				lateral_correction_performed = true;
				this.debugMessage("Left Line Sensor Warning: |< . Turning the Steering to the right ...");
				this.getNotificationService().notify("Left Line Sensor Warning: Turning the Steering to the right ...");
			}
			
			if ( this.getRightLineSensor().isLineDetected() ) {
				this.getSteering().rotateLeft(Steering.SMOOTH_CORRECTION_ANGLE);
				lateral_correction_performed = true;
				this.debugMessage("Right Line Sensor Warning: >| . Turning the Steering to the left ...");
				this.getNotificationService().notify("Right Line Sensor Warning: Turning to the left ...");
			}
		}
		
		
			
		// Si todo va bien, indicamos que seguimos como estamos ...
		if ( !longitudinal_correction_performed && !lateral_correction_performed) {
			this.debugMessage("Controlling the driving function. Mantaining the current configuration ...");
		}
		
		
		//
		// Interacción con el conductor
		//

		// Advertimos al humano sí ...

		// ... està distraído o dormido ...
		
		//Requisito INTERACT-1
		checkDriverStatus();
		
				
		// ... el conductor no tiene las manos en el volante ...
		
		
		//Requisito INTERACT-2
		checkHandsStatus();
		
		// ... el conductor no está en el asiento del conductor ...
		
		//Requisito INTERACT-3
		checkDriverSeatStatus();
		
		
		//Requisito ADS-2
		if(!this.isWorking()) {
			changeDrivingL0Service();
			return this;
		}
		
		//Requisitos ADS_L3-7 y ADS_L3-8
		sensorFail();
		setEmergencyPlan();
		
		return this;
	}

	public IL3_DrivingService attachSensors(IL3_TrafficJamChauffer TJChauffeur) {

		// Refactorizar esto

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

		return TJChauffeur;
	}

}
