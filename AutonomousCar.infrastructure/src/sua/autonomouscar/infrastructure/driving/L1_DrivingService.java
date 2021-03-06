package sua.autonomouscar.infrastructure.driving;

import org.osgi.framework.BundleContext;

import sua.autonomouscar.devices.interfaces.IDistanceSensor;
import sua.autonomouscar.devices.interfaces.ILineSensor;
import sua.autonomouscar.driving.interfaces.IDrivingService;
import sua.autonomouscar.driving.interfaces.IL0_ManualDriving;
import sua.autonomouscar.driving.interfaces.IL1_DrivingService;
import sua.autonomouscar.driving.l0.manual.L0_ManualDriving;
import sua.autonomouscar.infrastructure.OSGiUtils;
import sua.autonomouscar.infrastructure.devices.DistanceSensor;
import sua.autonomouscar.interaction.interfaces.INotificationService;
import sua.autonomouscar.interfaces.IIdentifiable;

public abstract class L1_DrivingService extends L0_DrivingService implements IL1_DrivingService {

	public final static String LONGITUDINAL_SECURITY_DISTANCE = "longitudinal-security-distance";   // expressed in cms

	protected String frontDistanceSensor = null;
	protected String rightLineSensor = null;
	protected String leftLineSensor = null;

	protected String notificationService = null;
	
	public L1_DrivingService(BundleContext context, String id) {
		super(context, id);
		this.addImplementedInterface(IL1_DrivingService.class.getName());
	}

	@Override
	public void setLongitudinalSecurityDistance(int distance) {
		this.setProperty(L1_DrivingService.LONGITUDINAL_SECURITY_DISTANCE, distance);
		return;
	}
	
	@Override
	public int getLongitudinalSecurityDistance() {
		return (int)this.getProperty(L1_DrivingService.LONGITUDINAL_SECURITY_DISTANCE);
	}
	
	
	@Override
	public void setFrontDistanceSensor(String sensor) {
		this.frontDistanceSensor = sensor;
		return;
	}
	
	protected IDistanceSensor getFrontDistanceSensor() {
		return OSGiUtils.getService(context, IDistanceSensor.class, String.format("(%s=%s)", IIdentifiable.ID, this.frontDistanceSensor));
	}

	
	@Override
	public void setRightLineSensor(String sensor) {
		this.rightLineSensor = sensor;
		return;
	}

	protected ILineSensor getRightLineSensor() {
		return OSGiUtils.getService(context, ILineSensor.class, String.format("(%s=%s)", IIdentifiable.ID, this.rightLineSensor));
	}

	@Override
	public void setLeftLineSensor(String sensor) {
		this.leftLineSensor = sensor;
		return;
	}
	
	protected ILineSensor getLeftLineSensor() {
		return OSGiUtils.getService(context, ILineSensor.class, String.format("(%s=%s)", IIdentifiable.ID, this.leftLineSensor));
	}
	

	@Override
	public void setNotificationService(String service) {
		this.notificationService = service;
		return;
	}
	
	protected INotificationService getNotificationService() {
		return OSGiUtils.getService(context, INotificationService.class, String.format("(%s=%s)", IIdentifiable.ID, this.notificationService));
	}


	@Override
	public IDrivingService stopTheDrivingFunction() {
		return this;
	}
	
	protected void changeDrivingL0Service() {
		
		//Paramos el actual
		this.stopDriving();
		
		L0_ManualDriving manualDriving = (L0_ManualDriving) OSGiUtils.getService(context, IL0_ManualDriving.class);
		
		manualDriving.startDriving();
	
	}
	
	/* Parte del requisito ADS_L3-7
	 * L1 y L2 s?lo tiene un sensor frontal
	 * */
	protected boolean allWorking() {
		IDistanceSensor FrontDistanceSensor = OSGiUtils.getService(context, IDistanceSensor.class, "(" + IIdentifiable.ID + "=FrontDistanceSensor)");
		
		return ((DistanceSensor) FrontDistanceSensor).isWorking();
	}
	
	protected boolean LIDARWorking() {
		IDistanceSensor FrontDistanceSensor = OSGiUtils.getService(context, IDistanceSensor.class, "(" + IIdentifiable.ID + "=LIDAR-FrontDistanceSensor)");
		
		return ((DistanceSensor) FrontDistanceSensor).isWorking();
	}
	
	protected void setNormalSensors() {
		setFrontDistanceSensor("FrontDistanceSensor");
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
