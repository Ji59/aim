package cz.cuni.mff.kotal.frontend.intersection;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class IntersectionGraph extends Pane {

   public IntersectionGraph(double height) {
      // TODO udelat velikost poradne
      setPrefWidth(height);
      setPrefHeight(height);

      drawBackground(height - 60);
   }

   public void drawSquareModel(int granularity, int entries, int exits) {
      getChildren().clear();

      // TODO udelat velikost poradne
      double height = getHeight() - 60, shift = height / (granularity + 2);

      drawBackground(height);

      drawSquareEntriesAndExits(granularity, entries, exits, shift);

      for (int i = 1; i <= granularity; i++) {
         for (int j = 1; j <= granularity; j++) {
            Rectangle square = new Rectangle(shift, shift, Color.LIGHTGRAY);
            square.setX(i * shift + 30);
            square.setY(j * shift + 30);
            getChildren().add(square);
         }
      }
   }

   private void drawBackground(double height) {
      Rectangle backgroundSquare = new Rectangle(30, 30, height, height);
      backgroundSquare.setFill(Color.LAWNGREEN);

      getChildren().add(backgroundSquare);
   }

   private void drawSquareEntriesAndExits(int granularity, int entries, int exits, double shift) {

      // TODO udelej to poradne!

      int empty = granularity - entries - exits,
         padding = empty / 3,
         index = empty % 3 == 2 ? ++padding : padding;

      while (entries-- > 0) {
         index++;

         Rectangle entryN = new Rectangle(shift, shift, Color.LIGHTSLATEGRAY),
            entryS = new Rectangle(shift, shift, Color.LIGHTSLATEGRAY),
            entryW = new Rectangle(shift, shift, Color.LIGHTSLATEGRAY),
            entryE = new Rectangle(shift, shift, Color.LIGHTSLATEGRAY);

         entryN.setX(index * shift + 30);
         entryN.setY(30);

         entryS.setX((granularity - index + 1) * shift + 30);
         entryS.setY((granularity + 1) * shift + 30);

         entryW.setX(30);
         entryW.setY((granularity - index + 1) * shift + 30);

         entryE.setX((granularity + 1) * shift + 30);
         entryE.setY(index * shift + 30);

         getChildren().addAll(entryN, entryS, entryW, entryE);
      }

      index += empty - 2 * padding;

      while (exits-- > 0) {
         index++;

         Rectangle exitN = new Rectangle(shift, shift, Color.GRAY),
            exitS = new Rectangle(shift, shift, Color.GRAY),
            exitW = new Rectangle(shift, shift, Color.GRAY),
            exitE = new Rectangle(shift, shift, Color.GRAY);

         exitN.setX(index * shift + 30);
         exitN.setY(30);

         exitS.setX((granularity - index + 1) * shift + 30);
         exitS.setY((granularity + 1) * shift + 30);

         exitW.setX(30);
         exitW.setY((granularity - index + 1) * shift + 30);

         exitE.setX((granularity + 1) * shift + 30);
         exitE.setY(index * shift + 30);

         getChildren().addAll(exitN, exitS, exitW, exitE);
      }
   }
}
