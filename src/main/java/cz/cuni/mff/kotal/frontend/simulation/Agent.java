package cz.cuni.mff.kotal.frontend.simulation;

import cz.cuni.mff.kotal.simulation.graph.Edge;
import javafx.scene.shape.Rectangle;

public class Agent extends cz.cuni.mff.kotal.simulation.Agent {
   private final double l, w; // size
   private double x, y;         // location
   private final Rectangle graphic;

   public Agent(int id, double l, double w, double x, double y, Rectangle graphic, Edge location) {
      super(id, location);
      this.l = l;
      this.w = w;
      this.x = x;
      this.y = y;
      this.graphic = graphic;
   }

   public double getL() {
      return l;
   }

   public double getW() {
      return w;
   }

   public double getX() {
      return x;
   }

   public Agent setX(double x) {
      this.x = x;
      return this;
   }

   public double getY() {
      return y;
   }

   public Agent setY(double y) {
      this.y = y;
      return this;
   }

   public Rectangle getGraphic() {
      return graphic;
   }
}
