package contexto.carretera;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import context.interfaces.ERoadStatus;
import context.interfaces.ERoadType;
import context.interfaces.IRoadContext;

public class ContextoCarretera implements IRoadContext {

	protected BundleContext context = null;
 	protected Dictionary<String, Object> props = new Hashtable<String, Object>();
 	protected ServiceRegistration sr = null;
	
	public ContextoCarretera(BundleContext cont) {
		context = cont;
		
		this.setStatus(ERoadStatus.UNKNOWN);
		this.setType(ERoadType.UNKNOWN);
		
		sr = this.context.registerService(IRoadContext.class, this, props);
	}
	
	@Override
	public ERoadType getType() {
		return (ERoadType) props.get("type");
	}

	@Override
	public ERoadStatus getStatus() {
		return (ERoadStatus) props.get("status");
	}

	@Override
	public void setType(ERoadType type) {
		props.put("type", type);
		this._updateProps();
	}

	@Override
	public void setStatus(ERoadStatus status) {
		props.put("status", status);
		this._updateProps();
	}
	
	private void _updateProps() {
		if (this.sr != null) {
			this.sr.setProperties(this.props);
		}
	}

}
