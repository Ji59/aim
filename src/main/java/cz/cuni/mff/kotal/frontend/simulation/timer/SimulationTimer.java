package cz.cuni.mff.kotal.frontend.simulation.timer;


import cz.cuni.mff.kotal.frontend.simulation.*;
import cz.cuni.mff.kotal.helpers.Collisions;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Timer to be tick every frame to redraw agents and check for collisions.
 */
public class SimulationTimer extends AnimationTimer {
	private final Map<Long, AgentPane> agents;
	private final Map<Long, AgentPolygon> lastState = new HashMap<>();
	private final SimulationAgents simulationAgents;

	/**
	 * Create new timer.
	 *
	 * @param agents           Collection of agents to get data from
	 * @param simulationAgents GUI node starting this timer
	 */
	public SimulationTimer(Map<Long, AgentPane> agents, SimulationAgents simulationAgents) {
		this.agents = agents;
		this.simulationAgents = simulationAgents;
	}

	/**
	 * Compute agents positions, update values, check for collisions.
	 *
	 * @param now System time of frame
	 */
	@Override
	public void handle(long now) {
//		simulationAgents.resetRectangles();
		synchronized (agents) {
			Set<Map.Entry<Long, AgentPane>> finishedAgents = agents.entrySet().stream().filter(a -> !a.getValue().isDisable()).filter(entry -> entry.getValue().handleTick(now)).collect(Collectors.toSet());
			finishedAgents.forEach(simulationAgents::removeAgent);

			Set<Pair<AgentPane, AgentPane>> overlappingAgents = Collisions.getBoundingBoxesOverlaps(agents);
			overlappingAgents = overlappingAgents.stream().filter(pair -> Collisions.inCollision(pair.getKey(), pair.getValue())).collect(Collectors.toSet());

			// Handle collisions
			overlappingAgents.forEach(agentPanePair -> {
				AgentPane agentPane0 = agentPanePair.getKey();
				AgentPane agentPane1 = agentPanePair.getValue();

				agentPane0.collide();
				agentPane1.collide();

				simulationAgents.getSimulation().addCollision();

				// TODO remove log
				System.out.println(agentPane0.getAgentID() + " collides with " + agentPane1.getAgentID());

				// Change agents, set timer for removal
				new Thread(() -> {
					try {
						// TODO replace with number of steps
						Thread.sleep(simulationAgents.getSimulation().getPeriod());
						Platform.runLater(() -> {
							simulationAgents.removeAgent(agentPane0.getAgentID());
							simulationAgents.removeAgent(agentPane1.getAgentID());
						});
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (NullPointerException ignored) {
					}
				}).start();

			});
		}
	}

	/**
	 * Stop this timer, save info about last state to all agents.
	 */
	@Override
	public void stop() {
		long now = System.nanoTime();
		super.stop();
		synchronized (agents) {
			agents.values().forEach(a -> a.pause(now));
		}
	}
}
