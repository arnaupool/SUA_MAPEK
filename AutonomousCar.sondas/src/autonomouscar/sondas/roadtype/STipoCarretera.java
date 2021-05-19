package autonomouscar.sondas.roadtype;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import sua.autonomouscar.devices.interfaces.IRoadSensor;
import sua.autonomouscar.infrastructure.devices.RoadSensor;
import sua.autonomouscar.interfaces.ERoadType;

public class STipoCarretera implements ServiceListener {
	protected BundleContext context = null;
	
	public STipoCarretera (BundleContext context) {
		this.context = context;
	}
	
	@Override
	public void serviceChanged(ServiceEvent event) {
		IRoadSensor contextoCarretera = (IRoadSensor) this.context.getService(event.getServiceReference());
		ERoadType tipoCarretera = contextoCarretera.getRoadType();
		
		ServiceReference ref = this.context.getServiceReference(RoadSensor.class);
		if ( ref == null ) {
			System.out.println("[STipoCarretera] No RoadSensor found");
			return;
		}
		
		
		switch (event.getType()) {
		case ServiceEvent.MODIFIED:
		case ServiceEvent.REGISTERED:
			switch (tipoCarretera) {

			case HIGHWAY:
				System.out.println("[STipoCarretera] Highway");
				break;
			case STD_ROAD:
				System.out.println("[STipoCarretera] STD");
				break;
			case OFF_ROAD:
				System.out.println("[STipoCarretera] Off_road");
				break;
			case CITY:
				System.out.println("[STipoCarretera] City");
				break;
			default:
				System.out.println("[STipoCarretera] Default");
			}
		}
	}
}
