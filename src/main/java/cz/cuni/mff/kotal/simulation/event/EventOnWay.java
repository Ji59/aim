package cz.cuni.mff.kotal.simulation.event;

import cz.cuni.mff.kotal.backend.simulation.Agent;
import cz.cuni.mff.kotal.backend.simulation.graph.Edge;

import java.math.BigInteger;

public class EventOnWay extends Event {

   private final Edge edge;

   public EventOnWay(Action action, BigInteger time, Agent agent, Edge edge) {
      super(action, time, agent);
      assert action == Action.ON_WAY;
      this.edge = edge;
   }

   public Edge getEdge() {
      return edge;
   }
}