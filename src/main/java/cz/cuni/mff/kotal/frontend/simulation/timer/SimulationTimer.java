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
	public static final int COLLISION_AGENTS_SHOWN_STEPS = 1;
	public static final double MINIMUM_STEP_SIZE_PER_FRAME = 0.25;  // 2 ^ -2
	public static final double MAXIMUM_STEP_SIZE_PER_FRAME = 0.375;  // 3 * 2 ^ -3

	private static long[] verticesUsage = null;
	private static long frames = 0;
	private static double maxStep = 0;

	private static double lastStep = -1;
	private static long[] lastCalls = new long[128];
	private static int lastCallsIt = 0;

	private final Map<Long, AgentPane> agents;
	//	private final Map<Long, AgentPolygon> lastState = new HashMap<>();
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

//		simulationAgents.resetRectangles(); TODO
		double step = simulationAgents.getSimulation().getStep(now);

//		if (lastStep > 0 && step - lastStep > MINIMUM_STEP_SIZE_PER_FRAME) {
//			IntersectionMenu.decreaseSpeed();
//			lastStep = -1;
//		} else if (lastStep > 0 && step - lastStep < MAXIMUM_STEP_SIZE_PER_FRAME) {
//			IntersectionMenu.increaseSpeed();
//		} else {
//			lastStep = step;
//		}

		IntersectionMenu.setStep(step);

		simulationAgents.getSimulation().loadAgents(step);
		simulationAgents.addArrivedAgents(step);

		simulationAgents.getSimulation().updateStatistics(step);

		Set<AgentPane> activeAgents = new HashSet<>(agents.size());
		synchronized (agents) {
			Iterator<Map.Entry<Long, AgentPane>> activeAgentsIterator = agents.entrySet().iterator();
			while (activeAgentsIterator.hasNext()) {
				Map.Entry<Long, AgentPane> a = activeAgentsIterator.next();
				AgentPane agentPane = a.getValue();
				if (agentPane.getCollisionStep() <= 0 || agentPane.getCollisionStep() > step) {
					boolean finished = agentPane.handleTick(step);
					if (finished) {
						removeAgent(activeAgentsIterator, agentPane);
					} else {
						activeAgents.add(agentPane);
					}
				} else {
					if (agentPane.getCollisionStep() + COLLISION_AGENTS_SHOWN_STEPS <= step) {
						removeAgent(activeAgentsIterator, agentPane);
					}
				}
			}

			Set<Pair<AgentPane, AgentPane>> overlappingAgents = Collisions.getBoundingBoxesOverlaps(activeAgents);
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
//				new Thread(() -> {
//					try {
//						// TODO replace with number of steps
//						Thread.sleep(simulationAgents.getSimulation().getPeriod() / 1_000_000); // TODO millis
//						Platform.runLater(() -> {
//							simulationAgents.removeAgent(agentPane0.getAgentID());
//							simulationAgents.removeAgent(agentPane1.getAgentID());
//						});
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					} catch (NullPointerException ignored) {
//					}
//				}).start();
			});
		}

		if (maxStep < step) {
			frames++;
			updateVerticesUsage(step);
			simulationAgents.setVertexLabelText();
		}

		maxStep = Math.max(step, maxStep);
		IntersectionMenu.setTimelineMaximum(step, maxStep);

		lastCallsIt = (lastCallsIt + 1) % 128;
		if (lastCallsIt % 16 == 0) {
			System.out.println("FPS: " + (1_000_000_000. * 128 / (now - lastCalls[lastCallsIt])));
		}
		lastCalls[lastCallsIt] = now;
	}

	private void removeAgent(Iterator<Map.Entry<Long, AgentPane>> activeAgentsIterator, AgentPane agentPane) {
		activeAgentsIterator.remove();
		simulationAgents.removeAgentPane(agentPane);
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
		lastStep = -1;
	}
}
