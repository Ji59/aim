package cz.cuni.mff.kotal.simulation;

import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.simulation.AgentPane;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import cz.cuni.mff.kotal.helpers.Collisions;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import javafx.application.Platform;
import javafx.util.Pair;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SimulationHandler {
	public static final double MAXIMUM_STEP_SIZE_PER_FRAME = 0.25;  // 2 ^ -2

	private static long[] verticesUsage = null;
	private static long frames = 0;
	private static double maxStep = 0;
	private static boolean terminated;

	private final Map<Long, AgentPane> agents;
	private final Simulation simulation;

	public SimulationHandler(Map<Long, AgentPane> agents, Simulation simulation) {
		this.agents = agents;
		this.simulation = simulation;
		if (verticesUsage == null) {
			resetValues(simulation.getIntersectionGraph());
		}
	}

	public void start() {
		terminated = false;
		final double step = 0;

		if (Platform.isFxApplicationThread()) {
			System.out.println("JavaFX thread");
			new Thread(() -> start(step)).start();
		} else {
			System.out.println("not JavaFX thread");

			start(step);
		}
	}

	private void start(double step) {
		while (!(terminated || (simulation.ended() && agents.isEmpty() && IntersectionScene.getSimulationAgents().getArrivingAgents().isEmpty()))) {
			handle(step);
			step += MAXIMUM_STEP_SIZE_PER_FRAME;
		}
		IntersectionMenu.pauseSimulation();
	}

	public boolean handle(double step) {
		updateSimulation(step);

		Set<AgentPane> activeAgents = new HashSet<>(agents.size());
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

		Set<Pair<AgentPane, AgentPane>> overlappingAgents = Collisions.getBoundingBoxesOverlaps(activeAgents);
		overlappingAgents = overlappingAgents.stream().filter(pair -> Collisions.inCollision(pair.getKey(), pair.getValue())).collect(Collectors.toSet());

		// Handle collisions
		processCollisions(overlappingAgents);


		updateVerticesUsage(step);

		System.out.println("Computed " + step);
		return true;
	}

	private void processCollisions(Set<Pair<AgentPane, AgentPane>> overlappingAgents) {
		overlappingAgents.forEach(agentPanePair -> {
			AgentPane agentPane0 = agentPanePair.getKey();
			AgentPane agentPane1 = agentPanePair.getValue();

			simulation.addCollision();
			simulation.addCollision();

			agents.remove(agentPane0.getAgentID());
			agents.remove(agentPane1.getAgentID());

			// TODO remove log
			System.out.println(agentPane0.getAgentID() + " collides with " + agentPane1.getAgentID());
		});
	}

	public void updateSimulation(double step) {
		simulation.loadAndUpdateAgents(step); // TODO wait if steps not generated
		IntersectionScene.getSimulationAgents().addArrivedAgents(step, false);
		IntersectionMenu.setStep(step);
		simulation.updateStatistics(step);
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void updateVerticesUsage(double step) {
		if (step <= maxStep) {
			return;
		}

		frames++;
		SimulationAgents.getActiveAgents().values().stream()
			.filter(agentPane -> !agentPane.isDisable())
			.map(AgentPane::getAgent)
			.forEach(agent -> {
				int vertex = (int) agent.getNearestPathVertexId(step - agent.getPlannedTime());
				verticesUsage[vertex]++;
			});

		maxStep = Math.max(step, maxStep);
	}

	public static void stop() {
		terminated = true;
	}

	public static void resetValues(Graph graph) {
		terminated = true;
		verticesUsage = new long[graph.getVertices().length];
		frames = 0;
		maxStep = 0;
	}
}

