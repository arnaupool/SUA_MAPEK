package device.engine;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import devices.interfaces.IEngine;

public class Activator implements BundleActivator {

	private static BundleContext context;
	protected IEngine engine = null;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		
		engine = new Engine();
		engine.setRPM(2000);
		
		bundleContext.registerService(IEngine.class, engine, null);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		engine.setRPM(0);
		Activator.context = null;
	}

}
