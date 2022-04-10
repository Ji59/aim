package cz.cuni.mff.kotal.helpers;

import cz.cuni.mff.kotal.frontend.simulation.AgentPolygon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static cz.cuni.mff.kotal.helpers.Collisions.getTimeShiftCollisions;
import static org.assertj.core.api.Assertions.assertThat;

class CollisionsTest {

	@BeforeEach
	void setUp() {
	}

	@Test
	void getTimeShiftCollisionsTestSimple() {
		Map<Long, AgentPolygon> lastState = new HashMap<>();
		lastState.put(0L, new AgentPolygon(0, 0, 0, 0, 1, 1, 1, 1, 0));
		Map<Long, AgentPolygon> newState = new HashMap<>();
		newState.put(0L, new AgentPolygon(0, 2, 0, 2, 1, 3, 1, 3, 0));

		Set<Collision> collisions = getTimeShiftCollisions(lastState, newState);
		assertThat(collisions).isEmpty();
	}

	@Test
	void getTimeShiftCollisionsTestDouble() {
		Map<Long, AgentPolygon> lastState = new HashMap<>();
		long id0 = 0L;
		long id1 = 1L;
		lastState.put(id0, new AgentPolygon(id0, 0, 0, 0, 1, 1, 1, 1, 0));
		lastState.put(id1, new AgentPolygon(id1, 1.5, -2, 2.5, -2, 2.5, -1, 1.5, -1));
		Map<Long, AgentPolygon> newState = new HashMap<>();
		newState.put(id0, new AgentPolygon(id0, 2, 0, 2, 1, 3, 1, 3, 0));
		newState.put(id1, new AgentPolygon(id1, 1.5, 2, 2.5, 2, 2.5, 3, 1.5, 3));


		Set<Collision> collisions;
//		collisions = getTimeShiftCollisions(lastState, newState);
//		assertThat(collisions).hasSize(1);

		lastState.clear();
		lastState.put(id0, new AgentPolygon(id0, 0, 0, 0, 1, 1, 1, 1, 0));
		lastState.put(id1, new AgentPolygon(id1, -2, 0, -2, 1, -1, 1, -1, 0));
		newState.clear();
		newState.put(id0, new AgentPolygon(id0, 2, 0, 2, 1, 3, 1, 3, 0));
		newState.put(id1, new AgentPolygon(id1, 0, 0, 0, 1, 1, 1, 1, 0));

		collisions = getTimeShiftCollisions(lastState, newState);
		assertThat(collisions).isEmpty();

	}
}