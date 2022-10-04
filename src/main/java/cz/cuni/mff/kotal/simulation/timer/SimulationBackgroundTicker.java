package cz.cuni.mff.kotal.simulation.timer;

import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.simulation.AgentPane;
import cz.cuni.mff.kotal.simulation.Simulation;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimulationBackgroundTicker implements SimulationTicker {
	private static final Lock terminatedLock = new ReentrantLock(true);
	private static boolean terminated;
	private final Map<Long, AgentPane> agents;
	private final Simulation simulation;

	public SimulationBackgroundTicker(Map<Long, AgentPane> agents, Simulation simulation) {
		this.agents = agents;
		this.simulation = simulation;
	}

	public void start() {
		terminatedLock.lock();
		terminated = false;
		terminatedLock.unlock();
		final double step = simulation.getNextStep();

		if (Platform.isFxApplicationThread()) {
			new Thread(() -> start(step)).start();
		} else {
			start(step);
		}
	}

	private void start(double step) {
		step -= MAXIMUM_STEP_SIZE_PER_FRAME;
		while (!(terminated || (simulation.ended() && agents.isEmpty() && IntersectionScene.getSimulationAgents().emptyArrivingAgents()))) {
			step += MAXIMUM_STEP_SIZE_PER_FRAME;
			handleStep(step);
		}
		IntersectionMenu.pauseSimulation();
		simulation.updateStatistics(step);
		simulation.setStartingStep(step);
		forceUpdateSimulationStats(step, simulation);
	}

	@Override
	public void handleStep(double step) {
		assert step <= simulation.getLoadedStep();
		SimulationTicker.updateSimulation(step, false);

		terminatedLock.lock();
		if (!terminated) {


			@NotNull Set<AgentPane> activeAgents = new HashSet<>(agents.size());
			updateAgents(step, activeAgents);

			handleCollisions(step, activeAgents);

			updateVerticesUsageAndTimeline(step);
		}
		terminatedLock.unlock();
	}

	@Override
	public void stop() {
		terminatedLock.lock();
		terminated = true;
		terminatedLock.unlock();
	}

	@Override
	public void updateAgents(double step, @NotNull Set<AgentPane> activeAgents) {
		synchronized (agents) {
			@NotNull Iterator<Map.Entry<Long, AgentPane>> activeAgentsIterator = agents.entrySet().iterator();
			while (activeAgentsIterator.hasNext()) {
				Map.Entry<Long, AgentPane> a = activeAgentsIterator.next();
				AgentPane agentPane = a.getValue();
				boolean finished = agentPane.handleSimulatedTick(step);
				if (finished) {
					activeAgentsIterator.remove();
				} else {
					activeAgents.add(agentPane);
				}
			}
		}
	}

	public void updateCollidedAgents(double step, @NotNull AgentPane agentPane0, @NotNull AgentPane agentPane1) {
		agents.remove(agentPane0.getAgent().setCollisionStep(step).getId());
		agents.remove(agentPane1.getAgent().setCollisionStep(step).getId());
	}
}

