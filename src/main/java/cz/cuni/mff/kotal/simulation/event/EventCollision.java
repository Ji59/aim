package cz.cuni.mff.kotal.simulation.event;

import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.Edge;

import java.math.BigInteger;

public class EventCollision extends Event {

   private final Edge location;

   public EventCollision(Action action, BigInteger time, Agent agent0, Agent agent1, Edge e) {
      super(action, time, agent0);
      assert action == Action.COLLISION;
      location = e;
   }

   public Edge getLocation() {
      return location;
   }
}