package cz.cuni.mff.kotal.simulation.event;

import cz.cuni.mff.kotal.backend.simulation.Agent;
import cz.cuni.mff.kotal.backend.simulation.graph.Vertex;

import java.math.BigInteger;

public class EventCollision extends Event {

   private final Agent agent1;
   private final Vertex location;

   public EventCollision(Action action, BigInteger time, Agent agent0, Agent agent1, Vertex v) {
      super(action, time, agent0);
      assert action == Action.COLLISION;
      this.agent1 = agent1;
      location = v;
   }

   public Agent getAgent0() {
      return super.getAgent();
   }

   public Agent getAgent1() {
      return agent1;
   }

   public Vertex getLocation() {
      return location;
   }
}