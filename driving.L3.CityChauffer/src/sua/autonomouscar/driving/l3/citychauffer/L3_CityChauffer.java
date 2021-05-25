package sua.autonomouscar.driving.l3.citychauffer;

import org.osgi.framework.BundleContext;

import sua.autonomouscar.devices.interfaces.ISpeedometer;
import sua.autonomouscar.driving.interfaces.IDrivingService;
import sua.autonomouscar.driving.interfaces.IL3_CityChauffer;
import sua.autonomouscar.infrastructure.OSGiUtils;
import sua.autonomouscar.infrastructure.devices.Engine;
import sua.autonomouscar.infrastructure.devices.Steering;
import sua.autonomouscar.infrastructure.driving.L3_DrivingService;

public class L3_CityChauffer extends L3_DrivingService implements IL3_CityChauffer {

	public static final int DEFAULT_LONGITUDINAL_SECURITY_DISTANCE = 3000;
	public static final int DEFAULT_LATERAL_SECURITY_DISTANCE = 100;
	public static final int DEFAULT_REFERENCE_SPEED = 40;

	public static final int MY_FINE_ACCELERATION_RPM = 5;
	public static final int MY_SMOOTH_ACCELERATION_RPM = 30;
	public static final int MY_MEDIUM_ACCELERATION_RPM = 70;
	public static final int MY_HIGH_ACCELERATION_RPM = 100;
	public static final int MY_AGGRESSIVE_ACCELERATION_RPM = 150;

	public L3_CityChauffer(BundleContext context, String id) {
		super(context, id);
		this.addImplementedInterface(IL3_CityChauffer.class.getName());
		this.setReferenceSpeed(DEFAULT_REFERENCE_SPEED);
		this.setLongitudinalSecurityDistance(DEFAULT_LONGITUDINAL_SECURITY_DISTANCE);
		this.setLateralSecurityDistance(DEFAULT_LATERAL_SECURITY_DISTANCE);
	}

	@Override
	public IDrivingService performTheDrivingFunction() {

		// L3 city chauffer

		// Comprobamos que NO podemos mantener la conducción en nivel 3 de autonomia

		// Requisito ADS_L3-1
		if (!canDriveL3Mode()) {
			this.debugMessage("Cannot drive in L3 Autonomy level ...");
			this.getNotificationService().notify("Cannot drive in L3 Autonomy level ...");

			tryChangeL2Driving();
			return this;
		}
		
		// Requisito ADS_L3-6
		if (isRoadHighway()) {
			this.getNotificationService().notify("Changing to L3_HighwayChauffer...");
			System.out.println("[L3_CityChauffer] Changing to L3_HighwayChauffer");
			this.changeDrivingHighwayChauffer();
			return this;
		}
		
		if (!isRoadFluid()) {
			this.getNotificationService().notify("Changing to L3_TrafficJamChauffer...");
			System.out.println("[L3_CityChauffer] Changing to L3_TrafficJamChauffer");
			this.changeDrivingTrafficJamChauffer();
			return this;
		}

		//
		// Control de la función primaria: MOVIMIENTO LONGITUDINAL
		//

		boolean longitudinal_correction_performed = false;
		ISpeedometer speedometer = OSGiUtils.getService(context, ISpeedometer.class);
		int currentSpeed = speedometer.getCurrentSpeed();
		this.debugMessage(String.format("Current Speed: %d Km/h", currentSpeed));

		// Reducimos la velocidad un poco si detectamos distancia frontal inferior a
		// distancia de seguridad
		if (this.getLongitudinalSecurityDistance() > this.getFrontDistanceSensor().getDistance()) {

			this.getEngine().decelerate(Engine.MEDIUM_ACCELERATION_RPM);
			longitudinal_correction_performed = true;
			this.debugMessage("Font Distance Warning: ⊼");
			this.getNotificationService().notify("Font Distance Warning: Braking ...");

		} else {

			// Intentamos acercarnos a la velocidad referencia
			int diffSpeed = this.getReferenceSpeed() - currentSpeed;

			int rpmCorrection = MY_FINE_ACCELERATION_RPM;
			String rpmAppliedCorrection = "fine";
			if (Math.abs(diffSpeed) > 30) {
				rpmCorrection = MY_AGGRESSIVE_ACCELERATION_RPM;
				rpmAppliedCorrection = "aggressive";
			} else if (Math.abs(diffSpeed) > 15) {
				rpmCorrection = MY_HIGH_ACCELERATION_RPM;
				rpmAppliedCorrection = "high";
			} else if (Math.abs(diffSpeed) > 5) {
				rpmCorrection = MY_MEDIUM_ACCELERATION_RPM;
				rpmAppliedCorrection = "medium";
			} else if (Math.abs(diffSpeed) > 1) {
				rpmCorrection = MY_SMOOTH_ACCELERATION_RPM;
				rpmAppliedCorrection = "smooth";
			}

			if (diffSpeed > 0) {
				this.getEngine().accelerate(rpmCorrection);
				longitudinal_correction_performed = true;
				this.debugMessage(String.format("Accelerating (%s) to get the reference speeed of %d Km/h",
						rpmAppliedCorrection, this.getReferenceSpeed()));
			} else if (diffSpeed < 0) {
				this.getEngine().decelerate(rpmCorrection);
				longitudinal_correction_performed = true;
				this.debugMessage(String.format("Decelerating (%s) to get the reference speeed of %d Km/h",
						rpmAppliedCorrection, this.getReferenceSpeed()));
			}

		}

		//
		// Control de la función primaria: MOVIMIENTO LATERAL
		//

		boolean lateral_correction_performed = false;
		// Control de las distancias laterales
		if (this.getRightDistanceSensor().getDistance() < this.getLateralSecurityDistance()) {
			if (this.getLeftDistanceSensor().getDistance() > this.getLateralSecurityDistance()) {
				this.debugMessage("Right Distance Warning: > @ . Turning the Steering to the left ...");
				this.getSteering().rotateLeft(Steering.SEVERE_CORRECTION_ANGLE);
				lateral_correction_performed = true;
			} else {
				this.debugMessage("Right and Left Distance Warning: @ <  > @ . Obstacles too close!!");
				this.getNotificationService()
						.notify("Right and Left Distance Warning: @ <  > @ . Obstacles too close!!");
				lateral_correction_performed = true;
			}
		}

		if (!lateral_correction_performed
				&& this.getLeftDistanceSensor().getDistance() < this.getLateralSecurityDistance()) {
			if (this.getRightDistanceSensor().getDistance() > this.getLateralSecurityDistance()) {
				this.debugMessage("Left Distance Warning: @ < . Turning the Steering to the right ...");
				this.getSteering().rotateRight(Steering.SEVERE_CORRECTION_ANGLE);
				lateral_correction_performed = true;
			}
		}

		if (!lateral_correction_performed) {
			// Control de la dirección si nos salimos del carril
			if (this.getLeftLineSensor().isLineDetected()) {
				this.getSteering().rotateRight(Steering.SMOOTH_CORRECTION_ANGLE);
				lateral_correction_performed = true;
				this.debugMessage("Left Line Sensor Warning: |< . Turning the Steering to the right ...");
				this.getNotificationService().notify("Left Line Sensor Warning: Turning the Steering to the right ...");
			}

			if (this.getRightLineSensor().isLineDetected()) {
				this.getSteering().rotateLeft(Steering.SMOOTH_CORRECTION_ANGLE);
				lateral_correction_performed = true;
				this.debugMessage("Right Line Sensor Warning: >| . Turning the Steering to the left ...");
				this.getNotificationService().notify("Right Line Sensor Warning: Turning to the left ...");
			}
		}

		// Si todo va bien, indicamos que seguimos como estamos ...
		if (!longitudinal_correction_performed && !lateral_correction_performed) {
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
		
		// Requisito INTERACT-2
		checkHandsStatus();

		// ... el conductor no está en el asiento del conductor ...
		
		//Requisito INTERACT-3
		checkDriverSeatStatus();
		
		/* Requisito ADS-1
		 * En caso de estar disponibles, el servicio usar� los mejores sensores disponibles
		 * */
		setBetterSensors();
		
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

}
 