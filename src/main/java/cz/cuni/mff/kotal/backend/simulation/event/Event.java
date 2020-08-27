package cz.cuni.mff.kotal.backend.simulation.event;

import cz.cuni.mff.kotal.backend.Agent;

import java.math.BigInteger;
import java.util.Comparator;

public abstract class Event implements Comparable<Event> {
   private final Action action;
   private final BigInteger time;
   private final Agent agent;

   public Event(Action action, BigInteger time, Agent agent) {
      this.action = action;
      this.time = time;
      this.agent = agent;
   }

   public Action getAction() {
      return action;
   }

   public BigInteger getTime() {
      return time;
   }

   public Agent getAgent() {
      return agent;
   }

   @Override
   public int compareTo(Event e) {
      int timeDifference = time.compareTo(e.time);
      if (timeDifference == 0) {
         return action.getPriority() - e.action.getPriority();
      }
      return  timeDifference;
   }
}