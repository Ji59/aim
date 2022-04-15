package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SquareGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.cuni.mff.kotal.helpers.MyNumberOperations.perimeter;
import static org.assertj.core.api.Assertions.*;


class SafeLinesTest {
	List<Long> path0 = Arrays.asList(16L, 4L, 0L, 1L, 2L, 3L, 7L, 22L);
	List<Long> path1 = Arrays.asList(19L, 2L, 1L, 0L, 4L, 8L, 20L);
	Agent agent0 = new Agent(0, 16, 22, 0, 2, 1, 13, 0.5800000000000001, 0.31000000000000005, 0, 0).setPath(path0, 24);
	Agent agent1 = new Agent(1, 19, 20, 3, 0, 1, 9, 0.5800000000000001, 0.31000000000000005, 0, 0).setPath(path1, 24);

	SafeLines safeLines;

	@BeforeEach
	void setUp() {
		safeLines = new SafeLines(new SquareGraph(4, 1, 1));
		for (int i = 0; i < path0.size(); i++) {
			Map<Long, Agent> stepMap = new HashMap<>();
			stepMap.put(path0.get(i), agent0);
			safeLines.getStepOccupiedVertices().put(i + agent0.getPlannedTime(), stepMap);
		}
	}

	@Test
	void validPath() {
		double agentPerimeter = agent1.getAgentPerimeter();
		assertThat(safeLines.validPath(agent1.getPlannedTime(), agent1.getPath(), agentPerimeter)).isFalse();
	}
}