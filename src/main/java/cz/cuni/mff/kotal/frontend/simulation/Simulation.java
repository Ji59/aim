package cz.cuni.mff.kotal.frontend.simulation;

import cz.cuni.mff.kotal.frontend.simulation.graph.Graph;
import cz.cuni.mff.kotal.simulation.event.Event;

import java.util.PriorityQueue;

public class Simulation extends PriorityQueue<Event> {
   private final Graph intersectionGraph;
   Simulation(Graph intersectionGraph) {
      this.intersectionGraph = intersectionGraph;
   }
}
