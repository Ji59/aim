package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.OctagonalGraph;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static cz.cuni.mff.kotal.helpers.MyNumberOperations.perimeter;
import static org.junit.jupiter.api.Assertions.*;

class SemaphoreTest {
	private final SimulationGraph graph = new OctagonalGraph(5, 2, 1);
	private final Semaphore semaphore = new Semaphore(graph);
	private final Map<Long, Map<Long, Agent>> stepOccupiedVertices = semaphore.getStepOccupiedVertices();

	private final Agent agent0 = new Agent(0, 22, 32, 1, 3, 1, 248, 0.44999999999999996, 0.30000000000000004, 0, 0).setPlannedTime(266)
		.setPath(Arrays.asList(22L, 18L, 14L, 9L, 4L, 0L, 32L));
	private final Agent agent1 = new Agent(1, 28, 29, 3, 0, 1, 255, 0.44999999999999996, 0.30000000000000004, 0, 0).setPlannedTime(266)
		.setPath(Arrays.asList(28L, 1L, 5L, 38L, 9L, 41L, 13L, 29L));

	@BeforeEach
	void setUp() {
		for (long i = 248; i < 300; i++) {
			stepOccupiedVertices.put(i, new HashMap<>());
		}
	}

	@Test
	void safeStepTo() {
		addPath(agent0);
		boolean safeStep = semaphore.safeStepTo(agent1.getPlannedTime() + agent1.getPath().indexOf(9L), 9, 38, perimeter(agent1.getL(), agent1.getW()) * graph.getCellSize());
		assert !safeStep;
	}

	@Test
	void safeStepFrom() {
		addPath(agent1);
		boolean safeStep = semaphore.safeStepFrom(agent0.getPlannedTime() + agent0.getPath().indexOf(9L), 9, 4, perimeter(agent1.getL(), agent1.getW()) * graph.getCellSize());
		assert !safeStep;
	}

	private void addPath(Agent agent1) {
		for (int i = 0; i < agent1.getPath().size(); i++) {
			stepOccupiedVertices.get(agent1.getPlannedTime() + i).put(agent1.getPath().get(i), agent1);
		}
	}
}