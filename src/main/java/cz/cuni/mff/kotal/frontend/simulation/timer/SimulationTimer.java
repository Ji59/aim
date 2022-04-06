package cz.cuni.mff.kotal.frontend.simulation.timer;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
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
	//	private final Map<Long, AgentPolygon> lastState = new HashMap<>();
	private final SimulationAgents simulationAgents;

	private long generatedStep = -1;

	private static long[] verticesUsage = null;
	private static long frames = 0;
	private static double maxStep = 0;

	/**
	 * Create new timer.
	 *
	 * @param agents           Collection of agents to get data from
	 * @param simulationAgents GUI node starting this timer
	 */
	public SimulationTimer(Map<Long, AgentPane> agents, SimulationAgents simulationAgents) {
		this.agents = agents;
		this.simulationAgents = simulationAgents;

		if (verticesUsage == null) {
			verticesUsage = new long[simulationAgents.getSimulation().getIntersectionGraph().getVertices().size()];
		}
	}

	/**
	 * Compute agents positions, update values, check for collisions.
	 *
	 * @param now System time of frame
	 */
	@Override
	public void handle(long now) {
		if (simulationAgents.getSimulation().isEnded() && agents.isEmpty() && simulationAgents.getArrivingAgents().isEmpty()) {
			IntersectionMenu.pauseSimulation();
		}

		frames++;

//		simulationAgents.resetRectangles(); TODO
		double step = simulationAgents.getSimulation().getStep(now);
		IntersectionMenu.setStep(step);

		simulationAgents.getSimulation().loadAgents(step);
		simulationAgents.addArrivedAgents(step);

		simulationAgents.getSimulation().updateStatistics(step);

		synchronized (agents) {
			Set<Map.Entry<Long, AgentPane>> finishedAgents = agents.entrySet().stream().filter(a -> !a.getValue().isDisable() && a.getValue().handleTick(step)).collect(Collectors.toSet());
			finishedAgents.forEach(simulationAgents::removeAgent);

			Set<Pair<AgentPane, AgentPane>> overlappingAgents = Collisions.getBoundingBoxesOverlaps(agents);
			overlappingAgents = overlappingAgents.stream().filter(pair -> Collisions.inCollision(pair.getKey(), pair.getValue())).collect(Collectors.toSet());

			// Handle collisions
			overlappingAgents.forEach(agentPanePair -> {
				AgentPane agentPane0 = agentPanePair.getKey();
				AgentPane agentPane1 = agentPanePair.getValue();

				if (agentPane0.getCollisionStep() < 0) {
					simulationAgents.getSimulation().addCollision();
				}
				if (agentPane1.getCollisionStep() < 0) {
					simulationAgents.getSimulation().addCollision();
				}

				agentPane0.collide(step);
				agentPane1.collide(step);

				// TODO remove log
				System.out.println(agentPane0.getAgentID() + " collides with " + agentPane1.getAgentID());

				// Change agents, set timer for removal
				new Thread(() -> {
					try {
						// TODO replace with number of steps
						Thread.sleep(simulationAgents.getSimulation().getPeriod() / 1_000_000); // TODO millis
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

			long stepDiscrete = (long) step;
			if (stepDiscrete > generatedStep) {
//				TODO
//				generatedStep = stepDiscrete;
//				Collection<AgentPane> agentPanes = simulationAgents.addStepAgentPanes(generatedStep, agents.values());
//				assert agentPanes == null;
//				SimulationMenuTab3.setTimelineMaximum(stepDiscrete);
			}


		}

		maxStep = Math.max(step, maxStep);
		IntersectionMenu.setTimelineMaximum(step, maxStep);
		updateVerticesUsage(step);

		simulationAgents.setVertexLabelText();
	}

	private void updateVerticesUsage(double step) {
		agents.values().stream()
			.filter(agentPane -> !agentPane.isDisable())
			.map(AgentPane::getAgent)
			.forEach(agent -> {
				int vertex = (int) agent.getNearestPathVertexId(step - agent.getPlannedTime());
				verticesUsage[vertex]++;
			});
		IntersectionModel.updateVertexNodesColors(verticesUsage, frames);
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

	public static double getVertexUsage(int id) {
		if (frames <= 0) {
			return 0;
		}

		return (double) verticesUsage[id] / frames;
	}

	public static void resetVerticesUsage() {
		verticesUsage = null;
		frames = 0;
		maxStep = 0;
	}
}
