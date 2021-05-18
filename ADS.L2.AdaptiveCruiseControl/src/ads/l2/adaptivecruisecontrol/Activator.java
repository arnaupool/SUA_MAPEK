package ads.l2.adaptivecruisecontrol;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ADS.interfaces.IL2_ACC;

public class Activator implements BundleActivator {

	private static BundleContext context;
	protected IL2_ACC acc = null;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		
		acc = new ACC(bundleContext);
		acc.start();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		acc.stop();
		Activator.context = null;
	}

}
