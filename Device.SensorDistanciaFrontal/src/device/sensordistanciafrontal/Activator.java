package device.sensordistanciafrontal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ADS.interfaces.ESensorPosition;
import devices.interfaces.IDistanceSensor;

public class Activator implements BundleActivator {

	protected IDistanceSensor sensor = null;
	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		
		sensor = new SensorDistanciaFrontal();
		
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("position", ESensorPosition.FRONT);
		
		this.context.registerService(IDistanceSensor.class, sensor, props);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}
