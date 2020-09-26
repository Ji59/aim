package cz.cuni.mff.kotal.frontend.intersection;

import cz.cuni.mff.kotal.frontend.MyApplication;
import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IntersectionGraph extends Pane {
   private static double preferredHeight = Screen.getPrimary().getBounds().getHeight() * MyApplication.getHEIGHT_RATIO() - IntersectionScene.PADDING * 2;
   private static final double GOLDEN_RATIO = (Math.sqrt(5) - 1) / 2;
   private static final double OCTAGON_RATIO = 1 / Math.E, VERTEX_RATIO = GOLDEN_RATIO / 2;
   private static final Color ROAD_COLOR = Color.LIGHTGREY,
      STROKE_COLOR = Color.BLACK,
      ENTRY_COLOR = Color.LIGHTSLATEGRAY,
      EXIT_COLOR = Color.GRAY;

   public IntersectionGraph(double height) {
      // TODO udelat velikost poradne
      setPrefWidth(height);
      setPrefHeight(height);

      drawBackground(height);
   }

   private void drawSquareModel(long granularity, long entries, long exits) {
      getChildren().clear();

      // TODO udelat velikost poradne
      double height = preferredHeight, shift = height / (granularity + 2);

      // TODO add real / abstract model button

      drawSquareEntriesAndExits(granularity, entries, exits, shift);

      for (int i = 1; i <= granularity; i++) {
         for (int j = 1; j <= granularity; j++) {
//            drawSquareRoad(shift, i * shift , j * shift );
            drawAbstractSquareRoad(shift, (i + 0.5) * shift, (j + 0.5) * shift, i < granularity, j < granularity);
         }
      }

      drawBackground(height);
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

   private void drawHexagonalModel(long granularity, long entries, long exits) {
      getChildren().clear();

      // TODO udelat velikost poradne
      double height = preferredHeight, shift = height / (2 * granularity + 1);


      double edgeLength = Math.sqrt(3) * shift / 3;
      double sidePadding = (height - edgeLength * (3 * granularity - 2)) / 2;
      for (int i = 0; i < granularity; i++) {
         double x = sidePadding,
            y = (granularity / 2. + i + 0.5) * shift;
         for (int j = 0; j < i + granularity; j++, x += edgeLength * 1.5, y -= shift / 2) {
            drawHexagon(shift, edgeLength, x, y);
//            drawAbstractHexagon(shift, edgeLength, x, y, j < i + granularity - 1, i < granularity - 1 || j < i + granularity - 1, j > 1 || i < granularity - 1);
         }
      }

      for (int i = 1; i < granularity; i++) {
         double x = sidePadding + 1.5 * i * edgeLength,
            y = (granularity * 3 + i - 1) * shift / 2;
         for (int j = 0; j < granularity * 2 - i - 1; j++, x += edgeLength * 1.5, y -= shift / 2) {
            drawHexagon(shift, edgeLength, x, y);
//            drawAbstractHexagon(shift, edgeLength, x, y, j < granularity * 2 - i - 2, j < granularity * 2 - i - 2 && i < granularity - 1, i < granularity - 1 && j > 0);
         }
      }

//      drawAbstractHexagonalEntriesAndExits(granularity, entries, exits, shift, edgeLength, sidePadding );
      drawHexagonalEntriesAndExits(granularity, entries, exits, shift, edgeLength, sidePadding);

      drawBackground(height);
   }

   private void drawAbstractHexagon(double shift, double edgeLength, double x, double y, boolean withUpObliqueLine, boolean withDownObliqueLine, boolean withDownLine) {
      double centerX = x + edgeLength / 2, centerY = y + shift / 2;

      if (withUpObliqueLine) {
         Line upObliqueLine = new Line(centerX, centerY, centerX + edgeLength * 1.5, centerY - shift / 2);
         getChildren().add(upObliqueLine);
      }
      if (withDownObliqueLine) {
         Line downObliqueLine = new Line(centerX, centerY, centerX + edgeLength * 1.5, centerY + shift / 2);
         getChildren().add(downObliqueLine);
      }
      if (withDownLine) {
         Line downLine = new Line(centerX, centerY, centerX, centerY + shift);
         getChildren().add(downLine);
      }

      Circle vertex = new Circle(centerX, centerY, 2 * edgeLength * VERTEX_RATIO, ROAD_COLOR);
      vertex.setStroke(STROKE_COLOR);
      getChildren().add(vertex);
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

   private void drawHexagonalEntriesAndExits(long granularity, long entries, long exits, double shift, double edgeLength, double sidePadding) {
      long empty = granularity - entries - exits,
         padding = empty / 3,
         index = empty % 3 == 2 ? ++padding : padding;

      while (entries-- > 0) {
         drawHexagonalEntry(granularity, shift, edgeLength, sidePadding, true, index++);
      }

      index += empty % 3 == 2 ? empty - 2 * padding : padding;


      while (exits-- > 0) {
         drawHexagonalEntry(granularity, shift, edgeLength, sidePadding, false, index++);
      }
   }

   private void drawAbstractHexagonalEntriesAndExits(long granularity, long entries, long exits, double shift, double edgeLength, double sidePadding) {
      long empty = granularity - entries - exits,
         padding = empty / 3,
         index = empty % 3 == 2 ? ++padding : padding;

      while (entries-- > 0) {
         drawAbstractHexagonalEntry(granularity, shift, edgeLength, sidePadding, true, index++);
      }

      index += empty % 3 == 2 ? empty - 2 * padding : padding;


      while (exits-- > 0) {
         drawAbstractHexagonalEntry(granularity, shift, edgeLength, sidePadding, false, index++);
      }
   }

   private void drawHexagonalEntry(long granularity, double shift, double edgeLength, double sidePadding, boolean entry, long index) {
      Color color = entry ? ENTRY_COLOR : EXIT_COLOR;
      Rectangle entryB = new Rectangle(sidePadding, shift, color),
         entryE = new Rectangle(sidePadding, shift, color);

      entryB.setX(0);
      entryB.setY(preferredHeight - (granularity / 2. + index + 1.5) * shift);
      entryB.setStroke(STROKE_COLOR);

      entryE.setX(preferredHeight - sidePadding);
      entryE.setY((granularity / 2. + index + 0.5) * shift);
      entryE.setStroke(STROKE_COLOR);

      double a0x = (1.5 * index - 0.5) * edgeLength + sidePadding, a0y = (granularity - index + 2) * shift / 2,     // bot left
         a1x = a0x + edgeLength * 1.5, a1y = a0y - shift / 2,                                                                                                                                                       // bot right
         a2xTemp = a1x - Math.sqrt(3) / 3 * (a1y), a3xTemp = a2xTemp - 2 * Math.sqrt(3) / 3 * shift,                         // top x
         a2x = Math.max(a2xTemp, 0), a2y = a2x == a2xTemp ? 0 : a1y - Math.sqrt(3) * (a1x),
         a3x = Math.max(a3xTemp, 0), a3y = a3x == a3xTemp ? 0 : a0y - Math.sqrt(3) * (a0x);

      double f0x = preferredHeight / 2 + edgeLength * (index * 1.5 + 1), f0y = shift * (index / 2. + 1.5),                // bot right
         f1x = f0x - edgeLength * 1.5, f1y = f0y - shift / 2,                                                                                                        // bot left
         f2xTemp = f1x + Math.sqrt(3) / 3 * (f1y), f3xTemp = f2xTemp + 2 * Math.sqrt(3) / 3 * shift,        // top x
         f2x = Math.min(f2xTemp, preferredHeight), f2y = f2x == f2xTemp ? 0 : f1y - Math.sqrt(3) * (preferredHeight - f1x),
         f3x = Math.min(f3xTemp, preferredHeight), f3y = f3x == f3xTemp ? 0 : f0y - Math.sqrt(3) * (preferredHeight - f0x);

      List<Double> entryAPoints = new ArrayList<>(List.of(
         a3x, a3y,                // top left
         a0x, a0y,                // bot left
         a1x, a1y,                // bot right
         a2x, a2y                // top right
      )), entryCPoints = new ArrayList<>(List.of(
         preferredHeight - f3x, preferredHeight - f3y,     // bot left
         preferredHeight - f0x, preferredHeight - f0y,     // top left
         preferredHeight - f1x, preferredHeight - f1y,      // top right
         preferredHeight - f2x, preferredHeight - f2y      // bot right
      )),
         entryDPoints = new ArrayList<>(List.of(
            preferredHeight - a3x, preferredHeight - a3y,        // bot right
            preferredHeight - a0x, preferredHeight - a0y,        // top right
            preferredHeight - a1x, preferredHeight - a1y,        // top left
            preferredHeight - a2x, preferredHeight - a2y        // bot left
         )), entryFPoints = new ArrayList<>(List.of(
         f3x, f3y,         // top right
         f0x, f0y,         // bot right
         f1x, f1y,          // bot left
         f2x, f2y          // top left
      ));

      // if polygons over vertex

      if (a2x != 0 && a3x == 0) {
         entryAPoints.add(0.);
         entryAPoints.add(0.);
         entryDPoints.add(preferredHeight);
         entryDPoints.add(preferredHeight);
      }
      if (f2x != preferredHeight && f3x == preferredHeight) {
         entryCPoints.add(0.);
         entryCPoints.add(preferredHeight);
         entryFPoints.add(preferredHeight);
         entryFPoints.add(0.);
      }

      Polygon entryA = new Polygon(getHexagonalEntryPoints(entryAPoints)),    // top left entry
         entryC = new Polygon(getHexagonalEntryPoints(entryCPoints)),                // bot left entry
         entryD = new Polygon(getHexagonalEntryPoints(entryDPoints)),               // bot right entry
         entryF = new Polygon(getHexagonalEntryPoints(entryFPoints));                // top right entry

      for (Polygon polygon : Arrays.asList(entryA, entryC, entryD, entryF)) {
         polygon.setFill(color);
         polygon.setStroke(STROKE_COLOR);
      }

      getChildren().addAll(entryA, entryB, entryC, entryD, entryE, entryF);
   }

   private void drawAbstractHexagonalEntry(long granularity, double shift, double edgeLength, double sidePadding, boolean entry, long index) {
      Color color = entry ? ENTRY_COLOR : EXIT_COLOR;

      double xShift = shift / 2, yShift = Math.sqrt(3) / 2 * shift,
         radius = 2 * edgeLength * VERTEX_RATIO;

      double a0x = (1.5 * index + 0.5) * edgeLength + sidePadding, a0y = (granularity - index + 2) * shift / 2,     // top left road center
         f0x = preferredHeight / 2 + edgeLength * index * 1.5, f0y = shift * (index / 2. + 1.5),                                     // top right road center
         c0x = preferredHeight - f0x, c0y = preferredHeight - f0y,
         d0x = preferredHeight - a0x, d0y = preferredHeight - a0y,
         b0x = sidePadding + edgeLength / 2, e0y = (granularity / 2. + index + 1) * shift,
         e0x = preferredHeight - b0x, b0y = preferredHeight - e0y;

      Line entryALine = new Line(a0x - xShift, a0y - yShift, a0x, a0y),
         entryBLine = new Line(b0x - shift, b0y, b0x, b0y),
         entryCLine = new Line(c0x - xShift, c0y + yShift, c0x, c0y),
         entryDLine = new Line(d0x + xShift, d0y + yShift, d0x, d0y),
         entryELine = new Line(e0x + shift, e0y, e0x, e0y),
         entryFLine = new Line(f0x + xShift, f0y - yShift, f0x, f0y);


      Circle entryA = new Circle(a0x - xShift, a0y - yShift, radius),                                       // top left entry
         entryB = new Circle(b0x - shift, b0y, radius),                                                                               // mid left entry
         entryC = new Circle(c0x - xShift, c0y + yShift, radius),   // bot left entry
         entryD = new Circle(d0x + xShift, d0y + yShift, radius),   // bot right entry
         entryE = new Circle(e0x + shift, e0y, radius),                                                                               // mid right entry
         entryF = new Circle(f0x + xShift, f0y - yShift, radius);                                               // top right entry


      for (Circle vertex : Arrays.asList(entryA, entryB, entryC, entryD, entryE, entryF)) {
         vertex.setFill(color);
         vertex.setStroke(STROKE_COLOR);
      }

      getChildren().addAll(entryALine, entryBLine, entryCLine, entryDLine, entryELine, entryFLine, entryA, entryB, entryC, entryD, entryE, entryF);

      entryALine.toBack();
      entryBLine.toBack();
      entryCLine.toBack();
      entryDLine.toBack();
      entryELine.toBack();
      entryFLine.toBack();
   }

   private double[] getHexagonalEntryPoints(List<Double> entryAPoints) {
      return entryAPoints.stream().mapToDouble(Double::doubleValue).toArray();
   }

   private void drawOctagonalModel(long granularity, long entries, long exits) {
      getChildren().clear();

      // TODO udelat velikost poradne
      double height = preferredHeight, shift = height / (granularity + 2);

      drawBackground(height);

      drawOctagonalEntriesAndExits(granularity, entries, exits, shift);

      for (int i = 1; i <= granularity; i++) {
         drawOctagonalLine(granularity, i, shift, i < granularity, i < granularity && i != 1);
      }
   }

   private void drawOctagonalLine(long granularity, long row, double shift, boolean withSquares, boolean withCorners) {
      double topY = row * shift;

      // TODO add real / abstract model button

      for (int j = withCorners ? 1 : 2; j <= (withCorners ? granularity : granularity - 1); j++) {
//         drawOctagonRoad(shift, topY, (row + OCTAGON_RATIO) * shift , j);
         drawAbstractOctagonRoad(shift, (j + 0.5) * shift, topY + shift / 2, granularity, row, j);
      }

      if (!withSquares) {
         return;
      }

      for (int j = 2; j <= granularity; j++) {
//         drawOctagonalSquareRoad(shift, topY, (row + OCTAGON_RATIO) * shift , j);
         drawAbstractOctagonalSquareRoad(shift, j * shift, topY + shift, row < granularity - 1 || j > 2, row < granularity - 1 || j < granularity);
      }
   }

   private void drawOctagonalSquareRoad(double shift, double topY, double midTopY, int j) {
      Polygon square = new Polygon(
         j * shift, midTopY + (1 - 2 * OCTAGON_RATIO) * shift,      // top
         (j - OCTAGON_RATIO) * shift, topY + shift,                                      // right
         j * shift, midTopY + shift,                                                                     // bottom
         (j + OCTAGON_RATIO) * shift, topY + shift                                       // left
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
         endPadding = (granularity + 1 - (square ? 0 : OCTAGON_RATIO)) * shift;

      Color color = entry ? ENTRY_COLOR : EXIT_COLOR;

      Rectangle entryN = new Rectangle(shift, side2, color),
         entryS = new Rectangle(shift, side2, color),
         entryW = new Rectangle(side2, shift, color),
         entryE = new Rectangle(side2, shift, color);

      entryN.setX(index * shift);
      entryN.setY(0);
      entryN.setStroke(STROKE_COLOR);

      entryS.setX((granularity - index + 1) * shift);
      entryS.setY(endPadding);
      entryS.setStroke(STROKE_COLOR);

      entryW.setX(0);
      entryW.setY((granularity - index + 1) * shift);
      entryW.setStroke(STROKE_COLOR);

      entryE.setX(endPadding);
      entryE.setY(index * shift);
      entryE.setStroke(STROKE_COLOR);

      getChildren().addAll(entryN, entryS, entryW, entryE);
   }

   private void drawAbstractRectangularEntries(long granularity, double shift, long index, boolean entry) {
      double centerNX = (index + 0.5) * shift,
         centerNY = shift * 0.5,
         centerSX = (granularity - index + 1.5) * shift,
         centerSY = (granularity + 1 + 0.5) * shift;

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
         (j + OCTAGON_RATIO) * shift, topY,                        // top left
         (j + 1 - OCTAGON_RATIO) * shift, topY,                                   // top right
         (j + 1) * shift, midTopY,                                                               // mid-top right
         (j + 1) * shift, midTopY + (1 - 2 * OCTAGON_RATIO) * shift,    // mid-bot right
         (j + 1 - OCTAGON_RATIO) * shift, topY + shift,                       // bot right
         (j + OCTAGON_RATIO) * shift, topY + shift,                            // bot left
         j * shift, midTopY + (1 - 2 * OCTAGON_RATIO) * shift,           // mid-bot left
         j * shift, midTopY                                                                       // mid-top left
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
      Rectangle backgroundSquare = new Rectangle(0, 0, height, height);
      backgroundSquare.setFill(Color.LAWNGREEN);

      getChildren().add(backgroundSquare);
      backgroundSquare.toBack();
   }

   public void redraw() {
      long granularity = IntersectionMenuTab0.getGranularity().getValue(), entries = IntersectionMenuTab0.getEntries().getValue(), exits = IntersectionMenuTab0.getExits().getValue();

      switch (IntersectionMenuTab0.getModel()) {
         case SQUARE:
            drawSquareModel(granularity, entries, exits);
            break;
         case HEXAGONAL:
            drawHexagonalModel(granularity, entries, exits);
            break;
         case OCTAGONAL:
            drawOctagonalModel(granularity, entries, exits);
            break;
         case CUSTOM:
            break;
      }
   }

   public static double getPreferredHeight() {
      return preferredHeight;
   }

   public static void setPreferredHeight(double preferredHeight) {
      IntersectionGraph.preferredHeight = preferredHeight;
   }
}
