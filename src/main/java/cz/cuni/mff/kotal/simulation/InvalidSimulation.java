package cz.cuni.mff.kotal.simulation;

public class InvalidSimulation extends Simulation {
	public InvalidSimulation() {
		ended = true;
	}

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
