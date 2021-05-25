package sua.autonomouscar.infrastructure;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import sua.autonomouscar.devices.interfaces.ILineSensor;
import sua.autonomouscar.devices.interfaces.IThing;
import sua.autonomouscar.interfaces.IIdentifiable;

public class Thing implements IThing {

	protected BundleContext context = null;
	protected Dictionary<String, Object> props = null;
	protected ServiceRegistration<?> s_reg = null;
	protected List<String> implementedInterfaces = null;

	public Thing(BundleContext context, String id) {
		this.context = context;
		this.props = new Hashtable<String, Object>();
		this.getDeviceProperties().put(IIdentifiable.ID, id);

		this.implementedInterfaces = new ArrayList<String>();
	}

	protected BundleContext getBundleContext() {
		return this.context;
	}

	protected Dictionary<String, Object> getDeviceProperties() {
		return this.props;
	}

	// IDevice

	@Override
	public String getId() {
		return (String) this.getDeviceProperties().get(IIdentifiable.ID);
	}

	@Override
	public IThing setProperty(String propName, Object value) {
		this.getDeviceProperties().put(propName, value);
		if (this.s_reg != null)
			this.s_reg.setProperties(this.getDeviceProperties());
		return this;
	}

	@Override
	public Object getProperty(String propName) {
		return this.getDeviceProperties().get(propName);
	}

	@Override
	public IThing addImplementedInterface(String c) {
		this.implementedInterfaces.add(c);
		return this;
	}

	@Override
	public IThing registerThing() {
		this.s_reg = this.getBundleContext().registerService(
				this.implementedInterfaces.toArray(new String[this.implementedInterfaces.size()]), this,
				this.getDeviceProperties());
		return this;
	}

	@Override
	public IThing unregisterThing() {
		if (this.s_reg != null)
			this.s_reg.unregister();
		return this;
	}

	// Devuelve true si funciona, false si no,
	// Para ADS-2: si DrivingService no funciona consideramos fallo general
	public boolean isWorking() {
		String query = "(" + IIdentifiable.ID + "=" + this.props.get(IIdentifiable.ID) + ")";
		IThing sensor = OSGiUtils.getService(context, this.getClass(), query);
		return sensor != null;
	}

}
