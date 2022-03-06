package cz.cuni.mff.kotal.simulation;

import java.util.Set;

public class InvalidSimulation extends Simulation {
	@Override
	public void loadAgents(double step) {

	}

	@Override
	protected void start() {

	}

	@Override
	protected void stopSimulation() {

	}

	@Override
	protected void resetSimulation() {

	}

	@Override
	public boolean isRunning() {
		return false;
	}

	@Override
	protected boolean loadAndUpdateStepAgents(long step) {
		return true;
	}
}
