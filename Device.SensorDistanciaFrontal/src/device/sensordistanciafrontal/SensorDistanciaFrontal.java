package device.sensordistanciafrontal;

import java.util.Random;

import devices.interfaces.IDistanceSensor;

public class SensorDistanciaFrontal implements IDistanceSensor{

	@Override
	public int getDistance() {
		return new Random().nextInt(300);
	}

}
