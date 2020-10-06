package cz.cuni.mff.kotal.simulation.graph;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Graph {

   private final Set<Vertex> v;
   private final Set<Edge> e;

   public Graph(boolean oriented, Set<Vertex> vertices, Set<Edge> edges) {
      v = vertices;
      if (oriented) {
         e = edges;
      } else {
         e = new HashSet<>(edges);
         e.addAll(edges.parallelStream().map(Edge::reverse).collect(Collectors.toSet())); // add reversed edges
      }
   }

   public Set<Vertex> getVertices() {
      return v;
   }

   public Set<Edge> getEdges() {
      return e;
   }
}