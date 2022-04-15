package cz.cuni.mff.kotal.simulation.timer;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.simulation.*;
import cz.cuni.mff.kotal.helpers.Collisions;
import cz.cuni.mff.kotal.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import javafx.animation.AnimationTimer;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Timer to be tick every frame to redraw agents and check for collisions.
 */
public class SimulationTimer extends AnimationTimer {
	public static final int COLLISION_AGENTS_SHOWN_STEPS = 1;
	public static final double MINIMUM_STEP_SIZE_PER_FRAME = 0.0625;  // 2 ^ -4
	public static final double MAXIMUM_STEP_SIZE_PER_FRAME = 0.25;  // 3 * 2 ^ -2

	private static long[] verticesUsage = null;
	private static long frames = 0;
	private static double maxStep = 0;

	private static double lastStep = Double.MAX_VALUE;
	private static int overLimitCount = 0;
	private static long[] lastCalls = new long[128];
	private static int lastCallsIt = 0;

	private final Map<Long, AgentPane> agents;
	private final Simulation simulation;


	/**
	 * Create new timer.
	 *
	 * @param agents     Collection of agents to get data from
	 * @param simulation GUI node starting this timer TODO
	 */
	public SimulationTimer(Map<Long, AgentPane> agents, Simulation simulation) {
		this.agents = agents;
		this.simulation = simulation;

		if (verticesUsage == null) {
			verticesUsage = new long[simulation.getIntersectionGraph().getVertices().size()];
		}
	}

	/**
	 * Compute agents positions, update values, check for collisions.
	 *
	 * @param now System time of frame
	 */
	@Override
	public void handle(long now) {
		//		simulationAgents.resetRectangles(); TODO
		double step = simulation.getStep(now);

		if (simulation.ended() && agents.isEmpty() && IntersectionScene.getSimulationAgents().getArrivingAgents().isEmpty()) {
			IntersectionMenu.pauseSimulation();
		}

		final double stepSize = step - lastStep;
//		if (stepSize < MINIMUM_STEP_SIZE_PER_FRAME) {
//			if (++overLimitCount > 4 && IntersectionMenu.increaseSpeed()) {
//				setLastStep(Double.MAX_VALUE);
//			} else {
//				lastStep = step;
//			}
//		} else
		if (stepSize > MAXIMUM_STEP_SIZE_PER_FRAME) {
			if ((--overLimitCount < 8 || stepSize > 1) && IntersectionMenu.decreaseSpeed()) {
				setLastStep(Double.MAX_VALUE);
			} else {
				lastStep = step;
			}
		} else {
			setLastStep(step);
		}

		IntersectionMenu.setStep(step);

		simulation.loadAndUpdateAgents(step);
		IntersectionScene.getSimulationAgents().addArrivedAgents(step);
		simulation.updateStatistics(step);

		Set<AgentPane> activeAgents = new HashSet<>(agents.size());
		synchronized (agents) {
			Iterator<Map.Entry<Long, AgentPane>> activeAgentsIterator = agents.entrySet().iterator();
			while (activeAgentsIterator.hasNext()) {
				Map.Entry<Long, AgentPane> a = activeAgentsIterator.next();
				AgentPane agentPane = a.getValue();
				if (agentPane.getCollisionStep() > step) {
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

				if (step < agentPane0.getCollisionStep()) {
					simulation.addCollision();
				}
				if (step < agentPane1.getCollisionStep()) {
					simulation.addCollision();
				}

				agentPane0.collide(step);
				agentPane1.collide(step);

				// TODO remove log
				System.out.println(agentPane0.getAgentID() + " collides with " + agentPane1.getAgentID());
			});
		}

		if (maxStep < step) {
			updateVerticesUsage(step);
			IntersectionScene.getSimulationAgents().setVertexLabelText();
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
		IntersectionScene.getSimulationAgents().removeAgentPane(agentPane);
	}

	private void updateVerticesUsage(double step) {
		frames++;
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
//		long now = System.nanoTime();  TODO remove
		super.stop();
//		synchronized (agents) {
//			agents.values().forEach(a -> a.pause(now));
//		}
	}

	public static double getVertexUsage(int id) {
		if (frames <= 0) {
			return 0;
		}

		return (double) verticesUsage[id] / frames;
	}

	/**
	 * TODO
	 *
	 * @param value
	 */
	private static void setLastStep(double value) {
		lastStep = value;
		overLimitCount = 0;
	}

	/**
	 * TODO
	 *
	 * @param graph
	 */
	public static void resetValues(Graph graph) {
		verticesUsage = new long[graph.getVertices().size()];
		frames = 0;
		maxStep = 0;
		setLastStep(Double.MAX_VALUE);
		lastCalls = new long[128];
	}
}
