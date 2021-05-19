package autonomouscar.sondas.roadtype;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import sua.autonomouscar.devices.interfaces.IRoadSensor;

public class Activator implements BundleActivator {
	
	protected STipoCarretera sonda = null;
	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		
		sonda = new STipoCarretera(context);
		
		String listenerFiltro = "(objectclass=" + IRoadSensor.class.getName() + ")";
		this.context.addServiceListener(sonda, listenerFiltro);

	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}
