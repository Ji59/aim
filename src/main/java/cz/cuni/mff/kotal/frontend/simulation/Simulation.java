package cz.cuni.mff.kotal.frontend.simulation;

import cz.cuni.mff.kotal.simulation.event.Action;
import cz.cuni.mff.kotal.simulation.event.Event;
import cz.cuni.mff.kotal.simulation.event.EventOnWay;
import cz.cuni.mff.kotal.simulation.graph.Graph;

import java.math.BigInteger;
import java.util.*;

public class Simulation {
   private final Graph intersectionGraph;
   private final Queue<Event> heap = new PriorityQueue<>();
   private final Set<Agent> agents = new HashSet<>();

   Simulation(Graph intersectionGraph) {
      this.intersectionGraph = intersectionGraph;
   }

   private boolean collisionCheck() {
      List<Path> paths = new ArrayList<>();
      Map<Agent, Event> agentEventMap = new HashMap<>();

      Event e = new EventOnWay(Action.COLLISION, BigInteger.ONE, new Agent(5, 1, 5, 4, 8, null, null), null);

      Set<Agent> agentsTemp = new HashSet<>(agents);

      eventLoop:
      for (Event event : heap) {
         for (Object agentListed : event.getAgents()) {
            Agent agent = (Agent) agentListed;
            if (agentsTemp.contains(agent)) {
               agentEventMap.put(agent, event);
               agentsTemp.remove(agent);
               if (agentsTemp.isEmpty()) {
                  break eventLoop;
               }
            }
         }
      }

      for (Agent agent : agents) {
         Event agentEvent = agentEventMap.get(agent);

         assert agentEvent instanceof EventOnWay;

         Vertex from = (Vertex) ((EventOnWay) agentEvent).getEdge().getU(),
            to = (Vertex) ((EventOnWay) agentEvent).getEdge().getV();
         paths.add(new Path(agent, new Point(from.getX(), from.getY()), new Point(to.getX(), to.getY())));
      }

      paths.sort(Comparator.comparingDouble(path -> path.getFrom().getX()));


      return false;
   }

   private static class Path {
      private final Agent agent;
      private final Point from, to;

      private Path(Agent agent, Point from, Point to) {
         this.agent = agent;
         this.from = from;
         this.to = to;
      }

      public Agent getAgent() {
         return agent;
      }

      public Point getFrom() {
         return from;
      }

      public Point getTo() {
         return to;
      }
   }

   private static class Point {
      private final double x, y;

      private Point(double x, double y) {
         this.x = x;
         this.y = y;
      }

      public double getX() {
         return x;
      }

      public double getY() {
         return y;
      }
   }
}
