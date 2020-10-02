package cz.cuni.mff.kotal.frontend.simulation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Event implements Comparator<Event> {
   private final Type type;
   private final Edge edgeLocation;
   private final Vertex vertexLocation;
   private final List<Agent> agents;
   private final long time;

   public Event(Type eventType, Edge location, long time, Agent... agents) {
      this(eventType, location, null, time, agents);
   }

   public Event(Type eventType, Vertex location, long time, Agent... agents) {
      this(eventType, null, location, time, agents);
   }

   private Event(Type eventType, Edge edgeLocation, Vertex vertexLocation, long time, Agent... agents) {
      type = eventType;
      this.edgeLocation = edgeLocation;
      this.vertexLocation = vertexLocation;
      this.agents = Arrays.asList(agents);
      this.time = time;
   }
   @Override
   public int compare(Event e0, Event e1) {
      if (e0.time - e1.time == 0L) {
         return Integer.compare(e0.type.priority, e1.type.priority);
      }
      return Long.compare(e0.time, e1.time);
   }

   public enum Type {
      COLLISION(0),
      ARRIVAL(1),
      LEAVING(3),
      POSITION(2),
      ;

      private final int priority;

      Type(int priority) {
         this.priority = priority;
      }
   }
}
