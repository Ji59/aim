package cz.cuni.mff.kotal.frontend.simulation;

import java.util.PriorityQueue;

public class Simulation extends PriorityQueue<Event> {
   private final Graph intersectionGraph;
   Simulation(Graph intersectionGraph) {
      this.intersectionGraph = intersectionGraph;
   }
}
