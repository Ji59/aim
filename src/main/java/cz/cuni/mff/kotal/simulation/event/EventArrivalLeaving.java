package cz.cuni.mff.kotal.simulation.event;

import cz.cuni.mff.kotal.simulation.Agent;


public class EventArrivalLeaving extends Event {
   private final long vertexID;

   public EventArrivalLeaving(Action action, double time, Agent agent, long vertexID) {
      super(action, time, agent);
      assert action == Action.ARRIVAL || action == Action.LEAVING;
      this.vertexID = vertexID;
   }

   public long getVertexID() {
      return vertexID;
   }
}