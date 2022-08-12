package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.backend.algorithm.simple.Roundabout;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SquareGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
//import static org.hamcrest.Matchers.nullValue;
//import static org.hamcrest.Matchers.not;


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

//		assertThat(previousAgent.getPath(), contains(19, 2, 3, 7, 11, 15, 14, 13, 12, 8, 20));
		assertThat(previousAgent.getPath()).contains(19, 2, 3, 7, 11, 15, 14, 13, 12, 8, 20);

		Agent plannedAgent = roundabout.planAgent(nextAgent, NEXT_AGENT_START);
//		assertThat(plannedAgent, not(nullValue()));
		assertThat(plannedAgent).isNotNull();
	}
}