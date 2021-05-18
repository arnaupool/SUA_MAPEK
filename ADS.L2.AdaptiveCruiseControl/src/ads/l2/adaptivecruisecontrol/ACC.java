package ads.l2.adaptivecruisecontrol;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import ADS.interfaces.ESensorPosition;
import ADS.interfaces.IL2_ACC;
import devices.interfaces.IDistanceSensor;
import devices.interfaces.IEngine;

public class ACC implements IL2_ACC {
	
	protected boolean isStarted = false;
	protected BundleContext b_cont = null;
	protected IEngine engine = null;
	protected IDistanceSensor distanceSensor = null;
	
	public ACC(BundleContext cont) {
		b_cont = cont;
	}
	
	@Override
	public void start() {
		System.out.println("[ACC] Starting");
		isStarted = true;
		ServiceReference ref =  b_cont.getServiceReference(IEngine.class);
		if (ref != null) {
			 engine = (IEngine) b_cont.getService(ref);
			System.out.println("[ACC] Engine RPM is: " + engine.getRPM());
		} else {
			System.out.println("[ACC] Engine not found");
		}
		String filtroSensorDistFrontal = "(position=" + ESensorPosition.FRONT + ")";
		try {
			ServiceReference[] refs = this.b_cont.getServiceReferences(IDistanceSensor.class.getName(), filtroSensorDistFrontal);
			if (refs != null) {
				this.distanceSensor = (IDistanceSensor) this.b_cont.getService(refs[0]);
				
				int distFrontal = this.distanceSensor.getDistance();
				if (distFrontal > 150 && this.engine != null) {
					this.engine.setRPM(this.engine.getRPM() + 50);
				} else if (distFrontal < 100 && this.engine != null) {
					this.engine.setRPM(this.engine.getRPM() - 500);
				}
			} else {
				System.out.println("[ACC] Front distance sensor not found");
			}
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void stop() {
		System.out.println("[ACC] Stopping");
		isStarted = false;
	}

	@Override
	public boolean isStarted() {
		return isStarted;
	}

}
