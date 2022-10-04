package cz.cuni.mff.kotal.simulation.graph;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EdgeTest {

   @Test
   public void testReverse() {
      @NotNull Edge e = new Edge(new Vertex(0), new Vertex(11), 0);

      assert e.reverse().equals(new Edge(new Vertex(11), new Vertex(0), 0));
   }

   @Test
   public void testEquals() {
      @NotNull Edge e0 = new Edge(new Vertex(0), new Vertex(50), 0),
         e1 = new Edge(new Vertex(0), new Vertex(50), 0),
         e2 = new Edge(new Vertex(0), new Vertex(70), 0);

      assert e0.equals(e1);
      assert !e0.equals(e2);
   }
}