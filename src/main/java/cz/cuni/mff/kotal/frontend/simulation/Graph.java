package cz.cuni.mff.kotal.frontend.simulation;

import java.util.Map;
import java.util.Set;

public class Graph {
   private final Set<Vertex> vertices;
   private final Set<Edge> edges;


   public Graph(Set<Vertex> vertices, Set<Edge> edges) {
      this.vertices = vertices;
      this.edges = edges;
   }
}
