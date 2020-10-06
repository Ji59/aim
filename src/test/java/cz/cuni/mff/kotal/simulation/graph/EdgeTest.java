package cz.cuni.mff.kotal.simulation.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EdgeTest {

   @Test
   public void testReverse() {
      Edge e = new Edge(new Vertex(0), new Vertex(11), null);

      assert e.reverse().equals(new Edge(new Vertex(11), new Vertex(0), null));
   }

   @Test
   public void testEquals() {
      Edge e0 = new Edge(new Vertex(0), new Vertex(50), null),
         e1 = new Edge(new Vertex(0), new Vertex(50), null),
         e2 = new Edge(new Vertex(0), new Vertex(70), null);

      assert e0.equals(e1);
      assert !e0.equals(e2);
   }
}