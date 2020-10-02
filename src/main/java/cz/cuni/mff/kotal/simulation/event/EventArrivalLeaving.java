package cz.cuni.mff.kotal.simulation.event;

import cz.cuni.mff.kotal.backend.simulation.Agent;
import cz.cuni.mff.kotal.backend.simulation.graph.Vertex;

import java.math.BigInteger;

public class EventArrivalLeaving extends Event {
   private final Vertex vertex;

   public EventArrivalLeaving(Action action, BigInteger time, Agent agent, Vertex vertex) {
      super(action, time, agent);
      assert action == Action.ARRIVAL || action == Action.LEAVING;
      this.vertex = vertex;
   }

   public Vertex getVertex() {
      return vertex;
   }
}