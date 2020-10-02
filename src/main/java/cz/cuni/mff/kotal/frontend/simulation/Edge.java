package cz.cuni.mff.kotal.frontend.simulation;

public class Edge {
   private final Vertex from, to;

   public Edge(Vertex from, Vertex to) {
      this.from = from;
      this.to = to;
   }

   public Vertex getFrom() {
      return from;
   }

   public Vertex getTo() {
      return to;
   }
}
