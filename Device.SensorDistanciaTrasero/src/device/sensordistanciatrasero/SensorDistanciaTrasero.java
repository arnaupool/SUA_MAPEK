package device.sensordistanciatrasero;

import java.util.Random;

import devices.interfaces.IDistanceSensor;

public class SensorDistanciaTrasero implements IDistanceSensor{

	@Override
	public int getDistance() {
		return new Random().nextInt(300);
	}

}
