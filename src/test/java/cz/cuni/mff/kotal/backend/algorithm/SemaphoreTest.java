package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.backend.algorithm.simple.Semaphore;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.OctagonalGraph;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class SemaphoreTest {
	private final SimulationGraph graph = new OctagonalGraph(5, 2, 1);
	private final Semaphore semaphore = new Semaphore(graph);
	private final Map<Long, Map<Integer, Agent>> stepOccupiedVertices = semaphore.getStepOccupiedVertices();

	private final Agent agent0 = new Agent(0, 22, 32, 1, 3, 1, 248, 0.44999999999999996, 0.30000000000000004, 0, 0)
		.setPath(Arrays.asList(22, 18, 14, 9, 4, 0, 32), 266);
	private final Agent agent1 = new Agent(1, 28, 29, 3, 0, 1, 255, 0.44999999999999996, 0.30000000000000004, 0, 0)
		.setPath(Arrays.asList(28, 1, 5, 38, 9, 41, 13, 29), 266);

	@BeforeEach
	void setUp() {
		for (long i = 248; i < 300; i++) {
			stepOccupiedVertices.put(i, new HashMap<>());
		}
	}

	@Test
	void safeStepTo() {
		addPath(agent0);
		boolean safeStep = semaphore.safeStepTo(agent1.getPlannedStep() + agent1.getPath().indexOf(9), 38, 9, agent1.getAgentPerimeter(graph));
		assert !safeStep;
	}

	@Test
	void safeStepFrom() {
		addPath(agent1);
//		boolean safeStep = semaphore.safeStepFrom(agent0.getPlannedStep() + agent0.getPath().indexOf(9), 9, 4, agent1.getAgentPerimeter(graph));
//		assert !safeStep;
	}

	private void addPath(@NotNull Agent agent1) {
		for (int i = 0; i < agent1.getPath().size(); i++) {
			stepOccupiedVertices.get(agent1.getPlannedStep() + i).put(agent1.getPath().get(i), agent1);
		}
	}
}