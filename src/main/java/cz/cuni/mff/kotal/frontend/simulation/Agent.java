package cz.cuni.mff.kotal.frontend.simulation;

import javafx.scene.shape.Rectangle;

public class Agent {
   private final int id;
   private final double l, w; // size
   private double x, y;         // location
   private final Rectangle graphic;

   public Agent(int id, double l, double w, double x, double y, Rectangle graphic) {
      this.id = id;
      this.l = l;
      this.w = w;
      this.x = x;
      this.y = y;
      this.graphic = graphic;
   }
}
