package cz.cuni.mff.kotal.frontend.simulation;

public class Vertex {
   private final int id;
   private final double x, y;


   public Vertex(int id, double x, double y) {
      this.id = id;
      this.x = x;
      this.y = y;
   }

   public int getId() {
      return id;
   }

   public double getX() {
      return x;
   }

   public double getY() {
      return y;
   }
}
