package cz.cuni.mff.kotal.frontend.intersection;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IntersectionGraph extends Pane {
   private static final double PADDING = 30, GOLDEN_RATIO = (Math.sqrt(5) - 1) / 2;
   private static final double OCTAGON_RATIO = 1 / Math.E, VERTEX_RATIO = GOLDEN_RATIO / 2;
   private static final Color ROAD_COLOR = Color.LIGHTGREY,
      STROKE_COLOR = Color.BLACK,
      ENTRY_COLOR = Color.LIGHTSLATEGRAY,
      EXIT_COLOR = Color.GRAY;

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

      // TODO add real / abstract model button

      drawSquareEntriesAndExits(granularity, entries, exits, shift);

      for (int i = 1; i <= granularity; i++) {
         for (int j = 1; j <= granularity; j++) {
//            drawSquareRoad(shift, i * shift + PADDING, j * shift + PADDING);
            drawAbstractSquareRoad(shift, (i + 0.5) * shift + PADDING, (j + 0.5) * shift + PADDING, i < granularity, j < granularity);
         }
      }
   }

   private void drawSquareRoad(double shift, double x, double y) {
      Rectangle square = new Rectangle(shift, shift, ROAD_COLOR);
      square.setX(x);
      square.setY(y);
      square.setStroke(STROKE_COLOR);
      getChildren().add(square);
   }

   private void drawAbstractSquareRoad(double shift, double x, double y, boolean withRightRoad, boolean withDownRoad) {
      if (withRightRoad) {
         Line rightRoad = new Line(x, y, x + shift, y);
         getChildren().add(rightRoad);
      }

      if (withDownRoad) {
         Line downRoad = new Line(x, y, x, y + shift);
         getChildren().add(downRoad);
      }

      Circle vertex = new Circle(x, y, shift * VERTEX_RATIO);
      vertex.setFill(ROAD_COLOR);
      vertex.setStroke(STROKE_COLOR);

      getChildren().add(vertex);
   }

   private void drawSquareEntriesAndExits(long granularity, long entries, long exits, double shift) {

      // TODO add real / abstract model button

      long empty = granularity - entries - exits,
         padding = empty / 3,
         index = empty % 3 == 2 ? ++padding : padding;

      while (entries-- > 0) {
//         drawRectangularEntries(granularity, shift, ++index, true, true);
         drawAbstractRectangularEntries(granularity, shift, ++index, true);
      }

      index += empty - 2 * padding;

      while (exits-- > 0) {
//         drawRectangularEntries(granularity, shift, ++index, false, true);
         drawAbstractRectangularEntries(granularity, shift, ++index, false);
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
            drawHexagon(shift, edgeLength, x, y);
         }
      }

      for (int i = 1; i < granularity; i++) {
         double x = sidePadding + 1.5 * i * edgeLength,
            y = (granularity * 3 + i - 2) * shift / 2 + PADDING;
         for (int j = 0; j < granularity * 2 - i - 1; j++, x += edgeLength * 1.5, y -= shift / 2) {
            drawHexagon(shift, edgeLength, x, y);
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
         Rectangle entryB = new Rectangle(sidePadding, shift, ENTRY_COLOR),
            entryE = new Rectangle(sidePadding, shift, ENTRY_COLOR);

         entryB.setX(PADDING);
         entryB.setY(getHeight() - (granularity / 2. + index + 1) * shift - PADDING);
         entryB.setStroke(STROKE_COLOR);

         entryE.setX(getHeight() - sidePadding - PADDING);
         entryE.setY((granularity / 2. + index) * shift + PADDING);
         entryE.setStroke(STROKE_COLOR);

         // TODO dopocitat do i pro leve a prave okraje

         double a0x = index * edgeLength * 1.5 - edgeLength / 2 + sidePadding + PADDING, a0y = (granularity - index + 1) * shift / 2 + PADDING,     // bot left
            a1x = a0x + edgeLength * 1.5, a1y = a0y - shift / 2,                                                                                            // bot right
            a2xTemp = a1x - Math.sqrt(3) / 3 * (a1y - PADDING), a3xTemp = a2xTemp - 2 * Math.sqrt(3) / 3 * shift,                         // top x
            a2x = Math.max(a2xTemp, PADDING), a2y = a2x == a2xTemp ? PADDING : a1y - Math.sqrt(3) * (a1x - PADDING),
            a3x = Math.max(a3xTemp, PADDING), a3y = a3x == a3xTemp ? PADDING : a0y - Math.sqrt(3) * (a0x - PADDING);

         double f0x = getHeight() / 2 + edgeLength * (index * 1.5 + 1), f0y = shift * (index / 2. + 1) + PADDING,    // bot right
            f1x = f0x - edgeLength * 1.5, f1y = f0y - shift / 2,                                                                                             // bot left
            f2xTemp = f1x + Math.sqrt(3) / 3 * (f1y - PADDING), f3xTemp = f2xTemp + 2 * Math.sqrt(3) / 3 * shift,                         // top x
            f2x = Math.min(f2xTemp, getHeight() - PADDING), f2y = f2x == f2xTemp ? PADDING : f1y - Math.sqrt(3) * (getHeight() - f1x - PADDING),
            f3x = Math.min(f3xTemp, getHeight() - PADDING), f3y = f3x == f3xTemp ? PADDING : f0y - Math.sqrt(3) * (getHeight() - f0x - PADDING);

         List<Double> entryAPoints = new ArrayList<>(List.of(
            a3x, a3y,                // top left
            a0x, a0y,                // bot left
            a1x, a1y,                // bot right
            a2x, a2y                // top right
         )),
            entryDPoints = new ArrayList<>(List.of(
               getHeight() - a3x, getHeight() - a3y,        // bot right
               getHeight() - a0x, getHeight() - a0y,        // top right
               getHeight() - a1x, getHeight() - a1y,        // top left
               getHeight() - a2x, getHeight() - a2y        // bot left
            ));

         if (a2x != PADDING && a3x == PADDING) {
            entryAPoints.add(PADDING);
            entryAPoints.add(PADDING);
            entryDPoints.add(getHeight() - PADDING);
            entryDPoints.add(getHeight() - PADDING);
         }

         Polygon entryA = new Polygon(entryAPoints.stream().mapToDouble(Double::doubleValue).toArray()), // top left entry
            entryC = new Polygon(   // bot left entry
               getHeight() - f0x, getHeight() - f0y, // top left
               getHeight() - f1x, getHeight() - f1y,                 // top right
               getHeight() - f2x, getHeight() - f2y,    // bot right
               getHeight() - f3x, getHeight() - f3y     // bot left
            ),
            entryD = new Polygon(entryDPoints.stream().mapToDouble(Double::doubleValue).toArray()),
            entryF = new Polygon( // top right entry
               f0x, f0y,    // bot left
               f1x, f1y,                     // bot right
               f2x, f2y,        // top right
               f3x, f3y         // top left
            );

         entryA.setFill(ENTRY_COLOR);
         entryA.setStroke(STROKE_COLOR);

         entryC.setFill(ENTRY_COLOR);
         entryC.setStroke(STROKE_COLOR);

         entryD.setFill(ENTRY_COLOR);
         entryD.setStroke(STROKE_COLOR);

         entryF.setFill(ENTRY_COLOR);
         entryF.setStroke(STROKE_COLOR);

         getChildren().addAll(entryA, entryB, entryC, entryD, entryE, entryF);

         index++;
      }

      index += empty % 3 == 2 ? empty - 2 * padding : padding;

      // TODO zbavit se duplicit

      while (exits-- > 0) {
         Rectangle exitB = new Rectangle(sidePadding, shift, EXIT_COLOR),
            exitE = new Rectangle(sidePadding, shift, EXIT_COLOR);

         exitB.setX(PADDING);
         exitB.setY(getHeight() - (granularity / 2. + index + 1) * shift - PADDING);
         exitB.setStroke(STROKE_COLOR);

         exitE.setX(getHeight() - sidePadding - PADDING);
         exitE.setY((granularity / 2. + index) * shift + PADDING);
         exitE.setStroke(STROKE_COLOR);

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

         exitA.setFill(EXIT_COLOR);
         exitA.setStroke(STROKE_COLOR);

         exitC.setFill(EXIT_COLOR);
         exitC.setStroke(STROKE_COLOR);

         exitD.setFill(EXIT_COLOR);
         exitD.setStroke(STROKE_COLOR);

         exitF.setFill(EXIT_COLOR);
         exitF.setStroke(STROKE_COLOR);

         getChildren().addAll(exitA, exitB, exitC, exitD, exitE, exitF);

         index++;
      }
   }

   private void drawHexagon(double shift, double edgeLength, double x, double y) {
      Polygon hexagon = new Polygon(
         x, y,                                  // top left
         x + edgeLength, y,                                      // top right
         x + edgeLength * 1.5, y + shift / 2,       // mid right
         x + edgeLength, y + shift,                        // bot right
         x, y + shift,                                    // bot right
         x - edgeLength * 0.5, y + shift / 2
      );

      hexagon.setFill(ROAD_COLOR);
      hexagon.setStroke(STROKE_COLOR);

      getChildren().add(hexagon);
   }

   public void drawOctagonalModel(long granularity, long entries, long exits) {
      getChildren().clear();

      // TODO udelat velikost poradne
      double height = getHeight() - 2 * PADDING, shift = height / (granularity + 2);

      drawBackground(height);

      drawOctagonalEntriesAndExits(granularity, entries, exits, shift);

      for (int i = 1; i <= granularity; i++) {
         drawOctagonalLine(granularity, i, shift, i < granularity, i < granularity && i != 1);
      }
   }

   private void drawOctagonalLine(long granularity, long row, double shift, boolean withSquares, boolean withCorners) {
      double topY = row * shift + PADDING;

      // TODO add real / abstract model button

      for (int j = withCorners ? 1 : 2; j <= (withCorners ? granularity : granularity - 1); j++) {
//         drawOctagonRoad(shift, topY, (row + OCTAGON_RATIO) * shift + PADDING, j);
         drawAbstractOctagonRoad(shift, (j + 0.5) * shift + PADDING, topY + shift / 2, granularity, row, j);
      }

      if (!withSquares) {
         return;
      }

      for (int j = 2; j <= granularity; j++) {
//         drawOctagonalSquareRoad(shift, topY, (row + OCTAGON_RATIO) * shift + PADDING, j);
         drawAbstractOctagonalSquareRoad(shift, j * shift + PADDING, topY + shift, row < granularity - 1 || j > 2, row < granularity - 1 || j < granularity);
      }
   }

   private void drawOctagonalSquareRoad(double shift, double topY, double midTopY, int j) {
      Polygon square = new Polygon(
         j * shift + PADDING, midTopY + (1 - 2 * OCTAGON_RATIO) * shift,      // top
         (j - OCTAGON_RATIO) * shift + PADDING, topY + shift,                                      // right
         j * shift + PADDING, midTopY + shift,                                                                     // bottom
         (j + OCTAGON_RATIO) * shift + PADDING, topY + shift                                       // left
      );

      square.setFill(ROAD_COLOR);
      square.setStroke(STROKE_COLOR);

      getChildren().add(square);
   }

   private void drawAbstractOctagonalSquareRoad(double shift, double x, double y, boolean withLeftLine, boolean withRightLine) {
      if (withLeftLine) {
         Line leftObliqueLine = new Line(x, y, x - shift / 2, y + shift / 2);

         getChildren().add(leftObliqueLine);
      }

      if (withRightLine) {
         Line rightObliqueLine = new Line(x, y, x + shift / 2, y + shift / 2);

         getChildren().add(rightObliqueLine);
      }

      Circle vertex = new Circle(x, y, shift * VERTEX_RATIO);
      vertex.setFill(ROAD_COLOR);
      vertex.setStroke(STROKE_COLOR);

      getChildren().add(vertex);
   }

   private void drawOctagonalEntriesAndExits(long granularity, long entries, long exits, double shift) {

      // TODO add real / abstract model button

      long empty = granularity - entries - exits,
         padding = empty / 3,
         index = empty % 3 == 2 ? ++padding : padding;

      while (entries-- > 0) {
//         drawRectangularEntries(granularity, shift, ++index, true, false);
         drawAbstractRectangularEntries(granularity, shift, ++index, true);
      }

      index += empty - 2 * padding;

      while (exits-- > 0) {
//         drawRectangularEntries(granularity, shift, ++index, false, false);
         drawAbstractRectangularEntries(granularity, shift, ++index, false);
      }
   }

   private void drawRectangularEntries(long granularity, double shift, long index, boolean entry, boolean square) {
      double side2 = shift * (1 + (square ? 0 : OCTAGON_RATIO)),
         endPadding = (granularity + 1 - (square ? 0 : OCTAGON_RATIO)) * shift + PADDING;

      Color color = entry ? ENTRY_COLOR : EXIT_COLOR;

      Rectangle entryN = new Rectangle(shift, side2, color),
         entryS = new Rectangle(shift, side2, color),
         entryW = new Rectangle(side2, shift, color),
         entryE = new Rectangle(side2, shift, color);

      entryN.setX(index * shift + PADDING);
      entryN.setY(PADDING);
      entryN.setStroke(STROKE_COLOR);

      entryS.setX((granularity - index + 1) * shift + PADDING);
      entryS.setY(endPadding);
      entryS.setStroke(STROKE_COLOR);

      entryW.setX(PADDING);
      entryW.setY((granularity - index + 1) * shift + PADDING);
      entryW.setStroke(STROKE_COLOR);

      entryE.setX(endPadding);
      entryE.setY(index * shift + PADDING);
      entryE.setStroke(STROKE_COLOR);

      getChildren().addAll(entryN, entryS, entryW, entryE);
   }

   private void drawAbstractRectangularEntries(long granularity, double shift, long index, boolean entry) {
      double length = shift * (1 + OCTAGON_RATIO),
         centerNX = (index + 0.5) * shift + PADDING,
         centerNY = shift * 0.5 + PADDING,
         centerSX = (granularity - index + 1.5) * shift + PADDING,
         centerSY = (granularity + 1 + 0.5) * shift + PADDING;

      Line entryNLine = new Line(centerNX, centerNY, centerNX, centerNY + shift),
         entrySLine = new Line(centerSX, centerSY - shift, centerSX, centerSY),
         entryWLine = new Line(centerNY, centerSX, centerNY + shift, centerSX),
         entryELine = new Line(centerSY - shift, centerNX, centerSY, centerNX);

      Color color = entry ? ENTRY_COLOR : EXIT_COLOR;

      Circle entryNVertex = new Circle(centerNX, centerNY, shift * VERTEX_RATIO),
         entrySVertex = new Circle(centerSX, centerSY, shift * VERTEX_RATIO),
         entryWVertex = new Circle(centerNY, centerSX, shift * VERTEX_RATIO),
         entryEVertex = new Circle(centerSY, centerNX, shift * VERTEX_RATIO);

      entryNVertex.setFill(color);
      entryNVertex.setStroke(STROKE_COLOR);

      entrySVertex.setFill(color);
      entrySVertex.setStroke(STROKE_COLOR);

      entryWVertex.setFill(color);
      entryWVertex.setStroke(STROKE_COLOR);

      entryEVertex.setFill(color);
      entryEVertex.setStroke(STROKE_COLOR);

      getChildren().addAll(entryNLine, entrySLine, entryWLine, entryELine, entryNVertex, entrySVertex, entryWVertex, entryEVertex);
   }

   private void drawOctagonRoad(double shift, double topY, double midTopY, int j) {
      Polygon octagon = new Polygon(
         (j + OCTAGON_RATIO) * shift + PADDING, topY,                        // top left
         (j + 1 - OCTAGON_RATIO) * shift + PADDING, topY,                                   // top right
         (j + 1) * shift + PADDING, midTopY,                                                               // mid-top right
         (j + 1) * shift + PADDING, midTopY + (1 - 2 * OCTAGON_RATIO) * shift,    // mid-bot right
         (j + 1 - OCTAGON_RATIO) * shift + PADDING, topY + shift,                       // bot right
         (j + OCTAGON_RATIO) * shift + PADDING, topY + shift,                            // bot left
         j * shift + PADDING, midTopY + (1 - 2 * OCTAGON_RATIO) * shift,           // mid-bot left
         j * shift + PADDING, midTopY                                                                       // mid-top left
      );

      octagon.setFill(ROAD_COLOR);
      octagon.setStroke(STROKE_COLOR);

      getChildren().add(octagon);
   }

   private void drawAbstractOctagonRoad(double shift, double x, double y, long granularity, long row, long column) {
      boolean withRightRoad = (row > 1 && row < granularity && column < granularity) || column < granularity - 1,
         withDownRoad = row < granularity - 1 || (row == granularity - 1 && column > 1 && column < granularity);

      if (row < granularity && column > 1) {
         Line obliqueLeftEdge = new Line(x, y, x - shift / 2, y + shift / 2);

         getChildren().add(obliqueLeftEdge);
      }

      if (row < granularity && column < granularity) {
         Line obliqueRightEdge = new Line(x, y, x + shift / 2, y + shift / 2);

         getChildren().add(obliqueRightEdge);
      }

      drawAbstractSquareRoad(shift, x, y, withRightRoad, withDownRoad);
   }

   private void drawBackground(double height) {
      Rectangle backgroundSquare = new Rectangle(PADDING, PADDING, height, height);
      backgroundSquare.setFill(Color.LAWNGREEN);

      getChildren().add(backgroundSquare);
   }
}
