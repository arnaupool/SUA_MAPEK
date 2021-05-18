package device.engine;

import devices.interfaces.IEngine;

public class Engine implements IEngine {
	
	protected int rpm = 0;
	
	@Override
	public void setRPM(int rpm) {
		this.rpm = rpm;
		System.out.println("[Engine] RPM set to: " + this.rpm);
	}

	@Override
	public int getRPM() {
		return this.rpm;
	}

}
