package cz.cuni.mff.kotal.simulation;

import java.util.Collection;

public class InvalidSimulation extends Simulation {
	public InvalidSimulation() {
		ended = true;
	}

	@Override
	public void loadAndUpdateAgents(double step) {
	}

	@Override
	protected void start() {

	}

	@Override
	@Deprecated
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
	protected Collection<Agent> loadAgents(long step) {
		return null;
	}
}
