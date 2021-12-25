package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SquareGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundaboutTest {
	private Roundabout roundabout;
	private Agent previousAgent;
	private Agent nextAgent;
	private static final long NEXT_AGENT_START = 1;

	@BeforeEach
	void setUp() {
		roundabout = new Roundabout(new SquareGraph(4, 1, 1));
		previousAgent = new Agent(0, 19, 20, 3, 0, 1, 0, 0.6, 0.35, 0, 0);

		nextAgent = new Agent(1, 19, 22, 3, 2, 1, 0, 0.6, 0.35, 0, 0);

	}

	@Test
	void planAgent() {
		roundabout.planAgent(previousAgent, 0);
		previousAgent.setPlannedTime(0);

		assertThat(previousAgent.getPath(), contains(19L, 2L, 3L, 7L, 11L, 15L, 14L, 13L, 12L, 8L, 20L));

		Agent plannedAgent = roundabout.planAgent(nextAgent, NEXT_AGENT_START);
		assertNotNull(plannedAgent);
	}
}