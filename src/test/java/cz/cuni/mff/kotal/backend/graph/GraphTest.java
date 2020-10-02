package cz.cuni.mff.kotal.backend.graph;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {
   private static final Vertex v0 = new Vertex(0),
      v1 = new Vertex(50),
      v2 = new Vertex(100);
   private static final Edge   e0 = new Edge(v0, v1, null),
      e1 = new Edge(v0, v2, null),
      e2 = new Edge(v1, v0, null);

   @Test
   public void testGraph() {
      Graph g = new Graph(false, Set.of(v0, v1, v2), Set.of(e0, e1, e2));

      // check vertices
      assert g.getVertices().size() == 3;
      assert g.getVertices().containsAll(Set.of(v0, v1, v2));

      // check edges
      assert g.getEdges().size() == 4;
      assert g.getEdges().containsAll(Set.of(new Edge(v0, v1, null), new Edge(v2, v0, null), new Edge(v1, v0, null), new Edge(v0, v2, null)));
   }
}