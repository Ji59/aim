package cz.cuni.mff.kotal.simulation;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class InvalidSimulation extends Simulation {
	public InvalidSimulation() {
		ended = true;
	}

	@Override
	public void loadAndUpdateAgents(double step) {
	}

	@Override
	protected @Nullable Collection<Agent> loadAgents(long step) {
		return null;
	}

	@Override
	public boolean isRunning() {
		return false;
	}

	@Override
	protected void start() {

	}

	@Override
	protected void resetSimulation() {

	}
}
