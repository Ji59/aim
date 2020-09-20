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

      drawBackground(height - 2 * PADDING);
   }

   public void drawSquareModel(long granularity, long entries, long exits) {
      getChildren().clear();

      // TODO udelat velikost poradne
      double height = getHeight() - 2 * PADDING, shift = height / (granularity + 2);

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

   public void drawHexagonalModel(long granularity, long entries, long exits) {
      getChildren().clear();

      // TODO udelat velikost poradne
      double height = getHeight() - 2 * PADDING, shift = height / (2 * granularity), edgeLength = Math.sqrt(3) * shift / 3;

      drawBackground(height);

      double sidePadding = (height - edgeLength * (3 * granularity - 2)) / 2 + PADDING;
      for (int i = 0; i < granularity; i++) {
         double x = sidePadding,
            y = (granularity / 2. + i) * shift + PADDING;
         for (int j = 0; j < i + granularity; j++, x += edgeLength * 1.5, y -= shift / 2) {
            createHexagon(shift, edgeLength, x, y);
         }
      }

      for (int i = 1; i < granularity; i++) {
         double x = sidePadding + 1.5 * i * edgeLength,
            y = (granularity * 3 + i - 2) * shift / 2 + PADDING;
         for (int j = 0; j < granularity * 2 - i - 1; j++, x += edgeLength * 1.5, y -= shift / 2) {
            createHexagon(shift, edgeLength, x, y);
         }
      }

      drawHexagonalEntriesAndExits(granularity, entries, exits, shift, edgeLength, sidePadding - PADDING);
   }

   private void drawHexagonalEntriesAndExits(long granularity, long entries, long exits, double shift, double edgeLength, double sidePadding) {

      // TODO udelej to poradne!

      long empty = granularity - entries - exits,
         padding = empty / 3,
         index = empty % 3 == 2 ? ++padding : padding;

      while (entries-- > 0) {
         Rectangle entryB = new Rectangle(sidePadding, shift, Color.LIGHTSLATEGRAY),
            entryE = new Rectangle(sidePadding, shift, Color.LIGHTSLATEGRAY);

         entryB.setX(PADDING);
         entryB.setY(getHeight() - (granularity / 2. + index + 1) * shift - PADDING);
         entryB.setStroke(Color.BLACK);

         entryE.setX(getHeight() - sidePadding - PADDING);
         entryE.setY((granularity / 2. + index) * shift + PADDING);
         entryE.setStroke(Color.BLACK);

         // TODO dopocitat do i pro leve a prave okraje

         double a0x = index * edgeLength * 1.5 - edgeLength / 2 + sidePadding + PADDING, a0y = (granularity - index + 1) * shift / 2 + PADDING,     // bot left
            a1x = a0x + edgeLength * 1.5, a1y = a0y - shift / 2,                                                                                            // bot right
            a2x = a1x - Math.sqrt(3) / 3 * (a1y - PADDING), a3x = a2x - 2 * Math.sqrt(3) / 3 * shift;                         // top x

         double f0x = getHeight() / 2 + edgeLength * (index * 1.5 + 1), f0y = shift * (index / 2. + 1) + PADDING,    // bot right
            f1x = f0x - edgeLength * 1.5, f1y = f0y - shift / 2,                                                                                             // bot left
            f2x = f1x + Math.sqrt(3) / 3 * (f1y - PADDING), f3x = f2x + 2 * Math.sqrt(3) / 3 * shift;                         // top x

         Polygon entryA = new Polygon( // top left entry
            a0x, a0y,               // bot left
            a1x, a1y,               // bot right
            a2x, PADDING,  // top right
            a3x, PADDING   // top left
         ),
            entryC = new Polygon(   // bot left entry
               getHeight() - f0x, getHeight() - f0y, // top left
               getHeight() - f1x, getHeight() - f1y,                 // top right
               getHeight() - f2x, getHeight() - PADDING,    // bot right
               getHeight() - f3x, getHeight() - PADDING     // bot left
            ),
            entryD = new Polygon(
               getHeight() - a0x, getHeight() - a0y,  // top right
               getHeight() - a1x, getHeight() - a1y,                    // top left
               getHeight() - a2x, getHeight() - PADDING,       // bot left
               getHeight() - a3x, getHeight() - PADDING        // bot right
            ),
            entryF = new Polygon( // top right entry
               f0x, f0y,    // bot left
               f1x, f1y,                     // bot right
               f2x, PADDING,        // top right
               f3x, PADDING         // top left
            );

         entryA.setFill(Color.LIGHTSLATEGRAY);
         entryA.setStroke(Color.BLACK);

         entryC.setFill(Color.LIGHTSLATEGRAY);
         entryC.setStroke(Color.BLACK);

         entryD.setFill(Color.LIGHTSLATEGRAY);
         entryD.setStroke(Color.BLACK);

         entryF.setFill(Color.LIGHTSLATEGRAY);
         entryF.setStroke(Color.BLACK);

         getChildren().addAll(entryA, entryB, entryC, entryD, entryE, entryF);

         index++;
      }

      index += empty % 3 == 2 ? empty - 2 * padding : padding;

      // TODO zbavit se duplicit

      while (exits-- > 0) {
         Rectangle exitB = new Rectangle(sidePadding, shift, Color.GRAY),
            exitE = new Rectangle(sidePadding, shift, Color.GRAY);

         exitB.setX(PADDING);
         exitB.setY(getHeight() - (granularity / 2. + index + 1) * shift - PADDING);
         exitB.setStroke(Color.BLACK);

         exitE.setX(getHeight() - sidePadding - PADDING);
         exitE.setY((granularity / 2. + index) * shift + PADDING);
         exitE.setStroke(Color.BLACK);

         double a0x = index * edgeLength * 1.5 - edgeLength / 2 + sidePadding + PADDING, a0y = (granularity - index + 1) * shift / 2 + PADDING,     // bot left
            a1x = a0x + edgeLength * 1.5, a1y = a0y - shift / 2,                                                                                            // bot right
            a2x = a1x - Math.sqrt(3) / 3 * (a1y - PADDING), a3x = a2x - 2 * Math.sqrt(3) / 3 * shift;                         // top x

         double f0x = getHeight() / 2 + edgeLength * (index * 1.5 + 1), f0y = shift * (index / 2. + 1) + PADDING,    // bot right
            f1x = f0x - edgeLength * 1.5, f1y = f0y - shift / 2,                                                                                             // bot left
            f2x = f1x + Math.sqrt(3) / 3 * (f1y - PADDING), f3x = f2x + 2 * Math.sqrt(3) / 3 * shift;                         // top x

         Polygon exitA = new Polygon( // top left entry
            a0x, a0y,               // bot left
            a1x, a1y,               // bot right
            a2x, PADDING,  // top right
            a3x, PADDING   // top left
         ),
            exitC = new Polygon(   // bot left entry
               getHeight() - f0x, getHeight() - f0y, // top left
               getHeight() - f1x, getHeight() - f1y,                 // top right
               getHeight() - f2x, getHeight() - PADDING,    // bot right
               getHeight() - f3x, getHeight() - PADDING     // bot left
            ),
            exitD = new Polygon(
               getHeight() - a0x, getHeight() - a0y,  // top right
               getHeight() - a1x, getHeight() - a1y,                    // top left
               getHeight() - a2x, getHeight() - PADDING,       // bot left
               getHeight() - a3x, getHeight() - PADDING        // bot right
            ),
            exitF = new Polygon( // top right entry
               f0x, f0y,    // bot left
               f1x, f1y,                     // bot right
               f2x, PADDING,        // top right
               f3x, PADDING         // top left
            );

         exitA.setFill(Color.GRAY);
         exitA.setStroke(Color.BLACK);

         exitC.setFill(Color.GRAY);
         exitC.setStroke(Color.BLACK);

         exitD.setFill(Color.GRAY);
         exitD.setStroke(Color.BLACK);

         exitF.setFill(Color.GRAY);
         exitF.setStroke(Color.BLACK);

         getChildren().addAll(exitA, exitB, exitC, exitD, exitE, exitF);

         index++;
      }
   }

   private void createHexagon(double shift, double edgeLength, double x, double y) {
      Polygon hexagon = new Polygon(
         x, y,                                  // top left
         x + edgeLength, y,                                      // top right
         x + edgeLength * 1.5, y + shift / 2,       // mid right
         x + edgeLength, y + shift,                        // bot right
         x, y + shift,                                    // bot right
         x - edgeLength * 0.5, y + shift / 2
      );

      hexagon.setFill(Color.LIGHTGRAY);
      hexagon.setStroke(Color.BLACK);

      getChildren().add(hexagon);
   }

   public void drawOctagonalModel(long granularity, long entries, long exits) {
      getChildren().clear();

      // TODO udelat velikost poradne
      double height = getHeight() - 2 * PADDING, shift = height / (granularity + 2);

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
         midBotY = (row + 1 - OCTAGON_RATIO) * shift + PADDING,
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

   private void drawBackground(double height) {
      Rectangle backgroundSquare = new Rectangle(PADDING, PADDING, height, height);
      backgroundSquare.setFill(Color.LAWNGREEN);

      getChildren().add(backgroundSquare);
   }
}
