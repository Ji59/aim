package cz.cuni.mff.kotal.simulation.timer;

import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.simulation.AgentPane;
import cz.cuni.mff.kotal.simulation.Simulation;
import javafx.application.Platform;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SimulationBackgroundTicker implements SimulationTicker {
	private static boolean terminated;

	private final Map<Long, AgentPane> agents;
	private final Simulation simulation;

	public SimulationBackgroundTicker(Map<Long, AgentPane> agents, Simulation simulation) {
		this.agents = agents;
		this.simulation = simulation;
	}

	public void start() {
		terminated = false;
		final double step = 0;

		if (Platform.isFxApplicationThread()) {
			new Thread(() -> start(step)).start();
		} else {
			start(step);
		}
	}

	private void start(double step) {
		while (!(terminated || (simulation.ended() && agents.isEmpty() && IntersectionScene.getSimulationAgents().getArrivingAgents().isEmpty()))) {
			handleStep(step);
			step += MAXIMUM_STEP_SIZE_PER_FRAME;
		}
		IntersectionMenu.pauseSimulation();
	}

	@Override
	public void handleStep(double step) {
		SimulationTicker.updateSimulation(step);

		Set<AgentPane> activeAgents = new HashSet<>(agents.size());
		updateAgents(step, activeAgents);

		handleCollisions(step, activeAgents);

		updateVerticesUsageAndTimeline(step);

		System.out.println("Computed " + step);
	}

	public static void stop() {
		terminated = true;
	}

	public static void resetValues() {
		terminated = true;
	}

	@Override
	public void updateAgents(double step, Set<AgentPane> activeAgents) {
		synchronized (agents) {
			Iterator<Map.Entry<Long, AgentPane>> activeAgentsIterator = agents.entrySet().iterator();
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

	public void updateCollidedAgents(double step, AgentPane agentPane0, AgentPane agentPane1) {
		agents.remove(agentPane0.getAgent().setCollisionStep(step).getId());
		agents.remove(agentPane1.getAgent().setCollisionStep(step).getId());
	}
}

