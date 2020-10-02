package cz.cuni.mff.kotal.backend.simulation;

import cz.cuni.mff.kotal.backend.simulation.graph.Edge;
import cz.cuni.mff.kotal.backend.simulation.graph.Vertex;
import cz.cuni.mff.kotal.simulation.event.EventArrivalLeaving;
import cz.cuni.mff.kotal.simulation.event.EventCollision;
import cz.cuni.mff.kotal.simulation.event.EventOnWay;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Objects;

import static cz.cuni.mff.kotal.simulation.event.Action.*;
import static org.junit.jupiter.api.Assertions.*;

class PlanTest {

   @Test
   public void testHeap() {
      Vertex v = new Vertex(0);
      Edge e = new Edge(v, v, null);
      Agent a = new Agent(0), a1 = new Agent(1);

      Plan s = new Plan();
      s.getHeap().add(new EventArrivalLeaving(LEAVING, BigInteger.ONE, a, v));          // 5
      s.getHeap().add(new EventArrivalLeaving(ARRIVAL, BigInteger.ZERO, a, v));         // 2
      s.getHeap().add(new EventOnWay(ON_WAY, BigInteger.TWO, a, e));                   // 8
      s.getHeap().add(new EventCollision(COLLISION, BigInteger.TWO, a1, a, v));          // 6
      s.getHeap().add(new EventOnWay(ON_WAY, BigInteger.ZERO, a, e));                  // 1
      s.getHeap().add(new EventCollision(COLLISION, BigInteger.ZERO, a1, a, v));         // 0
      s.getHeap().add(new EventCollision(COLLISION, BigInteger.ONE, a, a1, v));          // 3
      s.getHeap().add(new EventCollision(COLLISION, BigInteger.ONE, a, a1, v));          // 4
      s.getHeap().add(new EventArrivalLeaving(LEAVING, BigInteger.TWO, a, v));        // 7

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