package cz.cuni.mff.kotal.frontend.intersection;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public class IntersectionGraph extends Pane {
   private static final double PADDING = 30;
   private static final double OCTAGON_RATIO = 1 / Math.E;

   public IntersectionGraph(double height) {
      // TODO udelat velikost poradne
      setPrefWidth(height);
      setPrefHeight(height);

      drawBackground(height - 60);
   }

   public void drawSquareModel(long granularity, long entries, long exits) {
      getChildren().clear();

      // TODO udelat velikost poradne
      double height = getHeight() - 60, shift = height / (granularity + 2);

      drawBackground(height);

      drawSquareEntriesAndExits(granularity, entries, exits, shift);

      for (int i = 1; i <= granularity; i++) {
         for (int j = 1; j <= granularity; j++) {
            Rectangle square = new Rectangle(shift, shift, Color.LIGHTGRAY);
            square.setX(i * shift + PADDING);
            square.setY(j * shift + PADDING);
            square.setStroke(Color.BLACK);
            getChildren().add(square);
         }
      }
   }

   public void drawOctagonalModel(long granularity, long entries, long exits) {
      getChildren().clear();

      // TODO udelat velikost poradne
      double height = getHeight() - 60, shift = height / (granularity + 2);

      drawBackground(height);

      drawOctagonalLine(granularity, 1, shift, false, false);

      for (int i = 2; i < granularity; i++) {
         drawOctagonalLine(granularity, i, shift, false, true);
      }

      drawOctagonalLine(granularity, granularity, shift, true, false);

      drawOctagonalEntriesAndExits(granularity, entries, exits, shift);
   }

   private void drawOctagonalLine(long granularity, long row, double shift, boolean notSquares, boolean withCorners) {
      double topY = row * shift + PADDING,
         midTopY = (row + OCTAGON_RATIO) * shift + PADDING,
         midBotY = (row + 1  - OCTAGON_RATIO) * shift + PADDING,
         botY = (row + 1) * shift + PADDING;

      for (int j = withCorners ? 1 : 2; j <= (withCorners ? granularity : granularity - 1); j++) {
         Polygon octagon = new Polygon(
            (j + OCTAGON_RATIO) * shift + PADDING, topY,       // top left
            (j + 1 - OCTAGON_RATIO) * shift + PADDING, topY,                  // top right
            (j + 1) * shift + PADDING, midTopY,                                                 // mid-top right
            (j + 1) * shift + PADDING, midBotY,                                                 // mid-bot right
            (j + 1 - OCTAGON_RATIO) * shift + PADDING, botY,                  // bot right
            (j + OCTAGON_RATIO) * shift + PADDING, botY,                       // bot left
            j * shift + PADDING, midBotY,                                                         // mid-bot left
            j * shift + PADDING, midTopY                                                          // mid-top left
         );

         octagon.setFill(Color.LIGHTGREY);
         octagon.setStroke(Color.BLACK);

         getChildren().add(octagon);
      }

      if (notSquares) {
         return;
      }

      for (int j = 2; j <= granularity; j++) {
         Polygon square = new Polygon(
            j * shift + PADDING, midBotY,                                        // top
            (j - OCTAGON_RATIO) * shift + PADDING, botY,                      // right
            j * shift + PADDING, midTopY + shift,                                          // bottom
            (j + OCTAGON_RATIO) * shift + PADDING, botY                      // left
         );

         square.setFill(Color.LIGHTGRAY);
         square.setStroke(Color.BLACK);

         getChildren().add(square);
      }
   }

   private void drawBackground(double height) {
      Rectangle backgroundSquare = new Rectangle(PADDING, PADDING, height, height);
      backgroundSquare.setFill(Color.LAWNGREEN);

      getChildren().add(backgroundSquare);
   }

   private void drawSquareEntriesAndExits(long granularity, long entries, long exits, double shift) {

      // TODO udelej to poradne!

      long empty = granularity - entries - exits,
         padding = empty / 3,
         index = empty % 3 == 2 ? ++padding : padding;

      while (entries-- > 0) {
         index++;

         Rectangle entryN = new Rectangle(shift, shift, Color.LIGHTSLATEGRAY),
            entryS = new Rectangle(shift, shift, Color.LIGHTSLATEGRAY),
            entryW = new Rectangle(shift, shift, Color.LIGHTSLATEGRAY),
            entryE = new Rectangle(shift, shift, Color.LIGHTSLATEGRAY);

         entryN.setX(index * shift + PADDING);
         entryN.setY(PADDING);
         entryN.setStroke(Color.BLACK);

         entryS.setX((granularity - index + 1) * shift + PADDING);
         entryS.setY((granularity + 1) * shift + PADDING);
         entryS.setStroke(Color.BLACK);

         entryW.setX(PADDING);
         entryW.setY((granularity - index + 1) * shift + PADDING);
         entryW.setStroke(Color.BLACK);

         entryE.setX((granularity + 1) * shift + PADDING);
         entryE.setY(index * shift + PADDING);
         entryE.setStroke(Color.BLACK);

         getChildren().addAll(entryN, entryS, entryW, entryE);
      }

      index += empty - 2 * padding;

      while (exits-- > 0) {
         index++;

         Rectangle exitN = new Rectangle(shift, shift, Color.GRAY),
            exitS = new Rectangle(shift, shift, Color.GRAY),
            exitW = new Rectangle(shift, shift, Color.GRAY),
            exitE = new Rectangle(shift, shift, Color.GRAY);

         exitN.setX(index * shift + PADDING);
         exitN.setY(PADDING);
         exitN.setStroke(Color.BLACK);

         exitS.setX((granularity - index + 1) * shift + PADDING);
         exitS.setY((granularity + 1) * shift + PADDING);
         exitS.setStroke(Color.BLACK);

         exitW.setX(PADDING);
         exitW.setY((granularity - index + 1) * shift + PADDING);
         exitW.setStroke(Color.BLACK);

         exitE.setX((granularity + 1) * shift + PADDING);
         exitE.setY(index * shift + PADDING);
         exitE.setStroke(Color.BLACK);

         getChildren().addAll(exitN, exitS, exitW, exitE);
      }
   }

   private void drawOctagonalEntriesAndExits(long granularity, long entries, long exits, double shift) {

      // TODO udelej to poradne!

      long empty = granularity - entries - exits,
         padding = empty / 3,
         index = empty % 3 == 2 ? ++padding : padding;

      while (entries-- > 0) {
         index++;

         Rectangle entryN = new Rectangle(shift, shift * (1 + OCTAGON_RATIO), Color.LIGHTSLATEGRAY),
            entryS = new Rectangle(shift, shift * (1 + OCTAGON_RATIO), Color.LIGHTSLATEGRAY),
            entryW = new Rectangle(shift * (1 + OCTAGON_RATIO), shift, Color.LIGHTSLATEGRAY),
            entryE = new Rectangle(shift * (1 + OCTAGON_RATIO), shift, Color.LIGHTSLATEGRAY);

         entryN.setX(index * shift + PADDING);
         entryN.setY(PADDING);
         entryN.setStroke(Color.BLACK);

         entryS.setX((granularity - index + 1) * shift + PADDING);
         entryS.setY((granularity + 1 - OCTAGON_RATIO) * shift + PADDING);
         entryS.setStroke(Color.BLACK);

         entryW.setX(PADDING);
         entryW.setY((granularity - index + 1) * shift + PADDING);
         entryW.setStroke(Color.BLACK);

         entryE.setX((granularity + 1 - OCTAGON_RATIO) * shift + PADDING);
         entryE.setY(index * shift + PADDING);
         entryE.setStroke(Color.BLACK);

         getChildren().addAll(entryN, entryS, entryW, entryE);
      }

      index += empty - 2 * padding;

      while (exits-- > 0) {
         index++;

         Rectangle exitN = new Rectangle(shift, shift * (1 + OCTAGON_RATIO), Color.GRAY),
            exitS = new Rectangle(shift, shift * (1 + OCTAGON_RATIO), Color.GRAY),
            exitW = new Rectangle(shift * (1 + OCTAGON_RATIO), shift, Color.GRAY),
            exitE = new Rectangle(shift * (1 + OCTAGON_RATIO), shift, Color.GRAY);

         exitN.setX(index * shift + PADDING);
         exitN.setY(PADDING);
         exitN.setStroke(Color.BLACK);

         exitS.setX((granularity - index + 1) * shift + PADDING);
         exitS.setY((granularity + 1 - OCTAGON_RATIO) * shift + PADDING);
         exitS.setStroke(Color.BLACK);

         exitW.setX(PADDING);
         exitW.setY((granularity - index + 1) * shift + PADDING);
         exitW.setStroke(Color.BLACK);

         exitE.setX((granularity + 1 - OCTAGON_RATIO) * shift + PADDING);
         exitE.setY(index * shift + PADDING);
         exitE.setStroke(Color.BLACK);

         getChildren().addAll(exitN, exitS, exitW, exitE);
      }
   }
}
