package cz.cuni.mff.kotal.backend.simulation;


import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.Edge;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import cz.cuni.mff.kotal.simulation.event.EventArrivalLeaving;
import cz.cuni.mff.kotal.simulation.event.EventCollision;
import cz.cuni.mff.kotal.simulation.event.EventOnWay;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Objects;

import static cz.cuni.mff.kotal.simulation.event.Action.*;


class PlanTest {

	@Test
	public void testHeap() {
		Vertex v = new Vertex(0);
		Edge e = new Edge(v, v, null);
		Agent a = new Agent(0, 0, 0), a1 = new Agent(1, 0, 0);

		Plan s = new Plan();
		s.getHeap().add(new EventArrivalLeaving(LEAVING, 1, a, 0)); // 5
		s.getHeap().add(new EventArrivalLeaving(ARRIVAL, 0, a, 0)); // 2
		s.getHeap().add(new EventOnWay(ON_WAY, 2, a, 0, 0));    // 8
		s.getHeap().add(new EventCollision(COLLISION, 2, a1, a, e));         // 6
		s.getHeap().add(new EventOnWay(ON_WAY, 0, a, 0, 0));    // 1
		s.getHeap().add(new EventCollision(COLLISION, 0, a1, a, e));         // 0
		s.getHeap().add(new EventCollision(COLLISION, 1, a, a1, e));         // 3
		s.getHeap().add(new EventCollision(COLLISION, 1, a, a1, e));         // 4
		s.getHeap().add(new EventArrivalLeaving(LEAVING, 2, a, 0)); // 7

		assert s.getHeap().size() == 9;
		assert Objects.requireNonNull(s.getHeap().poll()).getAction() == COLLISION;
		assert Objects.requireNonNull(s.getHeap().poll()).getAction() == ON_WAY;
		assert Objects.requireNonNull(s.getHeap().poll()).getAction() == ARRIVAL;
		assert Objects.requireNonNull(s.getHeap().poll()).getAction() == COLLISION;
		assert Objects.requireNonNull(s.getHeap().poll()).getAction() == COLLISION;
		assert Objects.requireNonNull(s.getHeap().poll()).getAction() == LEAVING;
		assert Objects.requireNonNull(s.getHeap().poll()).getAction() == COLLISION;
		assert Objects.requireNonNull(s.getHeap().poll()).getAction() == LEAVING;
		assert Objects.requireNonNull(s.getHeap().poll()).getAction() == ON_WAY;
	}
}