package cz.cuni.mff.kotal.simulation.graph;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Set;


class GraphTest {
   private static final Vertex v0 = new Vertex(0),
      v1 = new Vertex(50),
      v2 = new Vertex(100);
   private static final Edge   e0 = new Edge(v0, v1, 0),
      e1 = new Edge(v0, v2, 0),
      e2 = new Edge(v1, v0, 0);

   @Test
   public void testGraph() {
      Graph g = new Graph(false, Set.of(v0, v1, v2), new HashMap<>(), Set.of(e0, e1, e2));

      // check vertices
      assert g.getVertices().length == 3;
      assert List.of(g.getVertices()).containsAll(Set.of(v0, v1, v2)); // TODO refactor

      // check edges
      assert g.getEdges().size() == 4;
      assert g.getEdges().containsAll(Set.of(new Edge(v0, v1, 0), new Edge(v2, v0, 0), new Edge(v1, v0, 0), new Edge(v0, v2, 0)));
   }
}