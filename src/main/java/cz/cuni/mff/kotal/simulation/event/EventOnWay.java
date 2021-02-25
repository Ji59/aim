package cz.cuni.mff.kotal.simulation.event;

import cz.cuni.mff.kotal.simulation.Agent;


public class EventOnWay extends Event {

   private final long fromID,
     toID;

   public EventOnWay(Action action, double time, Agent agent, long fromID, long toID) {
      super(action, time, agent);
      this.fromID = fromID;
      this.toID = toID;
      assert action == Action.ON_WAY;
   }

   public EventOnWay(double time, Agent agent, long fromID, long toID) {
      super(Action.ON_WAY, time, agent);
      this.fromID = fromID;
      this.toID = toID;
   }

   public long getFromID() {
      return fromID;
   }

   public long getToID() {
      return toID;
   }
}