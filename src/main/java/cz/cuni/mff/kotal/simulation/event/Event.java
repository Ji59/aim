package cz.cuni.mff.kotal.simulation.event;

import cz.cuni.mff.kotal.simulation.Agent;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Event implements Comparable<Event> {
   private final Action action;
   private final BigInteger time;
   private final Set<? extends Agent> agents;

   public Event(Action action, BigInteger time, Agent ... agent) {
      this.action = action;
      this.time = time;
      this.agents = Arrays.stream(agent).collect(Collectors.toSet());
   }

   public Action getAction() {
      return action;
   }

   public BigInteger getTime() {
      return time;
   }

   public Set<? extends Agent> getAgents() {
      return agents;
   }

   @Override
   public int compareTo(Event e) {
      int timeDifference = time.compareTo(e.time);
      if (timeDifference == 0) {
         return Integer.compare(action.getPriority(), e.action.getPriority());
      }
      return  timeDifference;
   }
}