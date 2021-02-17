package cz.cuni.mff.kotal.frontend.intersection;


import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.frontend.simulation.SimulationGraph;
import cz.cuni.mff.kotal.frontend.simulation.Vertex;
import cz.cuni.mff.kotal.simulation.graph.Edge;
import cz.cuni.mff.kotal.simulation.graph.Vertex.Type;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;
import javafx.stage.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters.Models.OCTAGONAL;
import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters.Models.SQUARE;


public class IntersectionGraph extends Pane {
	private static final double GOLDEN_RATIO = (Math.sqrt(5) - 1) / 2;
	private static final double OCTAGON_RATIO = 1 / Math.E, VERTEX_RATIO = GOLDEN_RATIO / 2;
	private static final Color ROAD_COLOR = Color.LIGHTGREY,
		STROKE_COLOR = Color.BLACK,
		ENTRY_COLOR = Color.LIGHTSLATEGRAY,
		EXIT_COLOR = Color.GRAY;

	private static double preferredHeight = Screen.getPrimary().getVisualBounds().getHeight();
	private static SimulationGraph graph;


	/**
	 * Create square graph.
	 *
	 * @param height dimension of the graph.
	 */
	public IntersectionGraph(double height) {
		// TODO udelat velikost poradne
		setPrefWidth(height);
		setPrefHeight(height);
		preferredHeight = height;

		drawBackground(height);
	}

	private void createSquareModel(long granularity, long entries, long exits) {

		graph = new SimulationGraph(granularity, SQUARE, entries, exits);
	}

	/**
	 * Draw square model.
	 *
	 * @param granularity Granularity of the model.
	 * @param entries     Number of entries.
	 * @param exits       Number of exits.
	 */
	private void drawSquareModel(long granularity, long entries, long exits) {
		graph = new SimulationGraph(granularity, SQUARE, entries, exits);
		getChildren().clear();

		// TODO udelat velikost poradne
		double height = preferredHeight, shift = height / (granularity + 2);

		if (!IntersectionMenu.isAbstract()) {
			for (Vertex v : graph.getVertices()) {
				drawSquare(shift, v.getX(), v.getY(), String.valueOf(v.getID()), v.getType().getColor());
			}
		} else {
			for (Edge e : graph.getEdges()) {
				Vertex u = (Vertex) e.getU(),
					v = (Vertex) e.getV();
				Line l = new Line(u.getX(), u.getY(), v.getX(), v.getY());
				getChildren().add(l);
			}
			for (Vertex v : graph.getVertices()) {
				Circle c = new Circle(shift * VERTEX_RATIO, v.getType().getColor());
				Text t = new Text(String.valueOf(v.getID()));
				t.setBoundsType(TextBoundsType.VISUAL);
				StackPane stack = new StackPane(c, t);
				stack.setLayoutX(v.getX() - shift * VERTEX_RATIO);
				stack.setLayoutY(v.getY() - shift * VERTEX_RATIO);
				getChildren().add(stack);
			}
		}
	}

	/**
	 * Draw square at specified location.
	 *
	 * @param size  Size of the square side.
	 * @param x     Coordinate X of center of the square.
	 * @param y     Coordinate Y of center of the square.
	 * @param text  The text inside of the square.
	 * @param color Color of the square.
	 */
	private void drawSquare(double size, double x, double y, String text, Color color) {
		double halfSize = size / 2;

		Rectangle square = new Rectangle(size, size, color);
		square.setStroke(STROKE_COLOR);
		square.setX(x - halfSize);
		square.setY(y - halfSize);
		getChildren().add(square);

		addTextField(x - halfSize, y - halfSize, size, size, text);
	}

	/**
	 * Draw square at specified location rotated 45 deg.
	 *
	 * @param size  Distance between opposite vertices.
	 * @param x     Coordinate X of center of the square.
	 * @param y     Coordinate Y of center of the square.
	 * @param text  The text inside of the square.
	 * @param color Color of the square.
	 */
	private void drawObliqueSquare(double size, double x, double y, String text, Color color) {
		double halfSize = size / 2;

		// create square
		Polygon square = new Polygon(
			x, y - halfSize, // top
			x - halfSize, y, // left
			x, y + halfSize, // bottom
			x + halfSize, y  // right
		);
		square.setFill(color);
		square.setStroke(STROKE_COLOR);
		getChildren().add(square);

		addTextField(x - halfSize, y - halfSize, size, size, text);
	}

	/**
	 * Draw octagon at specified location.
	 *
	 * @param size  Distance between opposite sides.
	 * @param x     Coordinate X of center of the octagon.
	 * @param y     Coordinate Y of center of the octagon.
	 * @param text  The text inside of the octagon.
	 * @param color Color of the octagon.
	 */
	private void drawOctagon(double size, double x, double y, String text, Color color) {
		double halfSize = size / 2,
			shorterSize = OCTAGON_RATIO * halfSize;

		// create polygon
		Polygon octagon = new Polygon(
			x - shorterSize, y - halfSize, // top left
			x + shorterSize, y - halfSize, // top right
			x + halfSize, y - shorterSize, // mid-top right
			x + halfSize, y + shorterSize, // mid-bot right
			x + shorterSize, y + halfSize, // bot right
			x - shorterSize, y + halfSize, // bot left
			x - halfSize, y + shorterSize, // mid-bot left
			x - halfSize, y - shorterSize  // mid-top left);
		);
		octagon.setFill(color);
		octagon.setStroke(STROKE_COLOR);
		getChildren().add(octagon);

		addTextField(x - halfSize, y - halfSize, size, size, text);
	}

	/**
	 * Create rectangular entry / exit for octagonal model.
	 *
	 * @param size      "Width" of the road.
	 * @param x         Coordination X of the entry / exit in graph.
	 * @param y         Coordination Y of the entry / exit in graph.
	 * @param text      The text inside the entry / exit.
	 * @param color     Color of the rectangle.
	 * @param direction Direction of the entry / exit. (See Vertex object for more details).
	 */
	private void drawOctagonalEntry(double size, double x, double y, String text, Color color, int direction) {
		assert (direction < 4);
		// create rectangle
		Rectangle rectangle = new Rectangle();
		rectangle.setFill(color);
		rectangle.setStroke(STROKE_COLOR);

		// compute location and size
		double halfSize = size / 2,
			xLocation = x - (direction == 3 ? (2 - OCTAGON_RATIO) * halfSize : halfSize),
			yLocation = y - (direction == 1 ? (2 - OCTAGON_RATIO) * halfSize : halfSize),
			width = direction < 2 ? size : (3 - OCTAGON_RATIO) * halfSize,
			height = direction < 2 ? (3 - OCTAGON_RATIO) * halfSize : size;

		// set rectangle location and size
		rectangle.setX(xLocation);
		rectangle.setY(yLocation);
		rectangle.setWidth(width);
		rectangle.setHeight(height);
		getChildren().add(rectangle);

		// add text
		addTextField(xLocation, yLocation, width, height, text);
	}

	/**
	 * Adds rectangular text field.
	 *
	 * @param x      Coordination X of the text field.
	 * @param y      Coordination Y of the text field.
	 * @param width  Width of the text field.
	 * @param height Height of the text field.
	 * @param text   Text of the text field.
	 */
	private void addTextField(double x, double y, double width, double height, String text) {
		TextField t = new TextField(text);
		t.setBackground(Background.EMPTY);
		t.setLayoutX(x);
		t.setLayoutY(y);
		t.setPrefWidth(width);
		t.setPrefHeight(height);
		t.setAlignment(Pos.CENTER);
		t.setEditable(false);
		getChildren().add(t);
	}

	/**
	 * Draw abstract vertex in between square grid with roads to lower neighbours.
	 *
	 * @param shift         Distance between 2 vertices.
	 * @param x             Coordinate X of drawn vertex.
	 * @param y             Coordinate Y of drawn vertex.
	 * @param withRightRoad Mark if there is a neighbour on the right.
	 * @param withDownRoad  Mark if there is a neighbour under.
	 */
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
		fillCircle(vertex, ROAD_COLOR);

		getChildren().add(vertex);
	}

	/**
	 * Draw all of the entries and exits of the square model.
	 *
	 * @param granularity Granularity of the model.
	 * @param entries     Number of entries.
	 * @param exits       Number of exits.
	 * @param shift       Distance between 2 vertices.
	 */
	private void drawSquareEntriesAndExits(long granularity, long entries, long exits, double shift) {
		// compute outer empty spaces
		long empty = granularity - entries - exits,
			padding = empty / 3,
			index = empty % 3 == 2 ? ++padding : padding;

		// draw all of the entries
		while (entries-- > 0) {
			if (IntersectionMenu.isAbstract()) {
				drawAbstractRectangularEntries(granularity, shift, ++index, true);
			} else {
				drawRectangularEntries(granularity, shift, ++index, true, true);
			}
		}

		// skip middle empty space
		index += empty - 2 * padding;

		// draw all of the exits
		while (exits-- > 0) {
			if (IntersectionMenu.isAbstract()) {
				drawAbstractRectangularEntries(granularity, shift, ++index, false);
			} else {
				drawRectangularEntries(granularity, shift, ++index, false, true);
			}
		}
	}

	/**
	 * Draw hexagonal model.
	 *
	 * @param granularity Granularity of the model.
	 * @param entries     Number of entries.
	 * @param exits       Number of exits.
	 */
	private void drawHexagonalModel(long granularity, long entries, long exits) {
		getChildren().clear();

		// TODO udelat velikost a dokumentaci poradne
		double height = preferredHeight, shift = height / (2 * granularity + 1);


		double edgeLength = Math.sqrt(3) * shift / 3;
		double sidePadding = (height - edgeLength * (3 * granularity - 2)) / 2;
		for (int i = 0; i < granularity; i++) {
			double x = sidePadding,
				y = (granularity / 2. + i + 0.5) * shift;
			for (int j = 0; j < i + granularity; j++, x += edgeLength * 1.5, y -= shift / 2) {
				if (IntersectionMenu.isAbstract()) {
					drawAbstractHexagon(shift, edgeLength, x, y, j < i + granularity - 1, i < granularity - 1 || j < i + granularity - 1, j > 1 || i < granularity - 1);
				} else {
					drawRealHexagon(shift, edgeLength, x, y);
				}
			}
		}

		for (int i = 1; i < granularity; i++) {
			double x = sidePadding + 1.5 * i * edgeLength,
				y = (granularity * 3 + i - 1) * shift / 2;
			for (int j = 0; j < granularity * 2 - i - 1; j++, x += edgeLength * 1.5, y -= shift / 2) {
				if (IntersectionMenu.isAbstract()) {
					drawAbstractHexagon(shift, edgeLength, x, y, j < granularity * 2 - i - 2, j < granularity * 2 - i - 2 && i < granularity - 1, i < granularity - 1 && j > 0);
				} else {
					drawRealHexagon(shift, edgeLength, x, y);
				}
			}
		}

		drawRealHexagonalEntriesAndExits(granularity, entries, exits, shift, edgeLength, sidePadding);

		drawBackground(height);
	}

	/**
	 * Draw circle representing vertex of hexagonal model with roads connecting it to neighbours.
	 *
	 * @param shift               Distance between 2 vertices.
	 * @param edgeLength          Length of a edge of hexagon if it was real model. Equal to horizontal distance to upper neighbours.
	 * @param x                   Coordinate X of the highest leftest point if it was a hexagon.
	 * @param y                   Coordinate X of the highest leftest point if it was a hexagon.
	 * @param withUpObliqueLine   Mark if there is a neighbour to the up and right.
	 * @param withDownObliqueLine Mark if there is a neighbour to the down and right.
	 * @param withDownLine        Mark if there is a neighbour under this vertex.
	 */
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

	/**
	 * Draw hexagonal vertex.
	 *
	 * @param shift      Distance between 2 vertices.
	 * @param edgeLength Length of the side of the hexagon
	 * @param x          Coordinate X of the highest leftest point.
	 * @param y          Coordinate Y of the highest leftest point.
	 */
	private void drawRealHexagon(double shift, double edgeLength, double x, double y) {
		Polygon hexagon = new Polygon(
			x, y,                            // top left
			x + edgeLength, y,                       // top right
			x + edgeLength * 1.5, y + shift / 2,     // mid right
			x + edgeLength, y + shift,               // bot right
			x, y + shift,                            // bot left
			x - edgeLength * 0.5, y + shift / 2      // mid left
		);

		hexagon.setFill(ROAD_COLOR);
		hexagon.setStroke(STROKE_COLOR);

		getChildren().add(hexagon);
	}

	/**
	 * Draw all of the entries and exits of the hexagonal model.
	 *
	 * @param granularity Granularity of the model.
	 * @param entries     Number of entries.
	 * @param exits       Number of exits.
	 * @param shift       Distance between 2 vertices.
	 * @param edgeLength  Length of the edge on a hexagonal vertex.
	 * @param sidePadding Distance of the intersection to the side.
	 */
	private void drawRealHexagonalEntriesAndExits(long granularity, long entries, long exits, double shift, double edgeLength, double sidePadding) {
		// compute outer empty spaces
		long empty = granularity - entries - exits,
			padding = empty / 3,
			index = empty % 3 == 2 ? ++padding : padding;

		// draw all of the entries
		while (entries-- > 0) {
			if (IntersectionMenu.isAbstract()) {
				drawAbstractHexagonalEntry(granularity, shift, edgeLength, sidePadding, true, index++);
			} else {
				drawRealHexagonalEntry(granularity, shift, edgeLength, sidePadding, true, index++);
			}
		}

		// skip middle empty space
		index += empty % 3 == 2 ? empty - 2 * padding : padding;

		// draw all of the exits
		while (exits-- > 0) {

			if (IntersectionMenu.isAbstract()) {
				drawAbstractHexagonalEntry(granularity, shift, edgeLength, sidePadding, false, index++);
			} else {
				drawRealHexagonalEntry(granularity, shift, edgeLength, sidePadding, false, index++);
			}
		}
	}

	/**
	 * Draw polygons representing entries / exits from all 6 sides to hexagonal model.
	 *
	 * @param granularity Granularity of the model.
	 * @param shift       Distance between 2 vertices.
	 * @param edgeLength  Length of the edge on a hexagonal vertex.
	 * @param sidePadding Distance of the intersection to the side.
	 * @param entry       Mark if it is entry, otherwise exit.
	 * @param index       Index of entry. Equal to the index of vertex it should be connected to.
	 */
	private void drawRealHexagonalEntry(long granularity, double shift, double edgeLength, double sidePadding, boolean entry, long index) {
		Color color = entry ? ENTRY_COLOR : EXIT_COLOR;

		// create horizontal entries
		Rectangle entryB = new Rectangle(sidePadding, shift, color),
			entryE = new Rectangle(sidePadding, shift, color);

		entryB.setX(0);
		entryB.setY(preferredHeight - (granularity / 2. + index + 1.5) * shift);
		entryB.setStroke(STROKE_COLOR);

		entryE.setX(preferredHeight - sidePadding);
		entryE.setY((granularity / 2. + index + 0.5) * shift);
		entryE.setStroke(STROKE_COLOR);


		// compute coordinates of top left entry
		double a0x = (1.5 * index - 0.5) * edgeLength + sidePadding, a0y = (granularity - index + 2) * shift / 2,             // bot left
			a1x = a0x + edgeLength * 1.5, a1y = a0y - shift / 2,                                                                // bot right
			a2xTemp = a1x - Math.sqrt(3) / 3 * (a1y), a3xTemp = a2xTemp - 2 * Math.sqrt(3) / 3 * shift,                         // top x
			a2x = Math.max(a2xTemp, 0), a2y = a2x == a2xTemp ? 0 : a1y - Math.sqrt(3) * (a1x),
			a3x = Math.max(a3xTemp, 0), a3y = a3x == a3xTemp ? 0 : a0y - Math.sqrt(3) * (a0x);

		// compute coordinates of top right entry
		double f0x = preferredHeight / 2 + edgeLength * (index * 1.5 + 1), f0y = shift * (index / 2. + 1.5),                  // bot right
			f1x = f0x - edgeLength * 1.5, f1y = f0y - shift / 2,                                                                // bot left
			f2xTemp = f1x + Math.sqrt(3) / 3 * (f1y), f3xTemp = f2xTemp + 2 * Math.sqrt(3) / 3 * shift,                         // top x
			f2x = Math.min(f2xTemp, preferredHeight), f2y = f2x == f2xTemp ? 0 : f1y - Math.sqrt(3) * (preferredHeight - f1x),
			f3x = Math.min(f3xTemp, preferredHeight), f3y = f3x == f3xTemp ? 0 : f0y - Math.sqrt(3) * (preferredHeight - f0x);

		// create oblique entries
		List<Double> entryAPoints = new ArrayList<>(List.of(
			a3x, a3y,                // top left
			a0x, a0y,                // bot left
			a1x, a1y,                // bot right
			a2x, a2y                 // top right
		)),
			entryCPoints = new ArrayList<>(List.of(
				preferredHeight - f3x, preferredHeight - f3y,     // bot left
				preferredHeight - f0x, preferredHeight - f0y,     // top left
				preferredHeight - f1x, preferredHeight - f1y,     // top right
				preferredHeight - f2x, preferredHeight - f2y      // bot right
			)),
			entryDPoints = new ArrayList<>(List.of(
				preferredHeight - a3x, preferredHeight - a3y,        // bot right
				preferredHeight - a0x, preferredHeight - a0y,        // top right
				preferredHeight - a1x, preferredHeight - a1y,        // top left
				preferredHeight - a2x, preferredHeight - a2y         // bot left
			)),
			entryFPoints = new ArrayList<>(List.of(
				f3x, f3y,         // top right
				f0x, f0y,         // bot right
				f1x, f1y,         // bot left
				f2x, f2y          // top left
			));

		// if polygons over background vertex
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
			entryC = new Polygon(getHexagonalEntryPoints(entryCPoints)),          // bot left entry
			entryD = new Polygon(getHexagonalEntryPoints(entryDPoints)),          // bot right entry
			entryF = new Polygon(getHexagonalEntryPoints(entryFPoints));          // top right entry

		for (Polygon polygon : Arrays.asList(entryA, entryC, entryD, entryF)) {
			polygon.setFill(color);
			polygon.setStroke(STROKE_COLOR);
		}

		getChildren().addAll(entryA, entryB, entryC, entryD, entryE, entryF);
	}

	/**
	 * Draw lines and circles representing all 6 entries / exits of a hexagonal model.
	 *
	 * @param granularity Granularity of the model.
	 * @param shift       Distance between 2 vertices.
	 * @param edgeLength  Horizontal distance of vertex to his upper lefter neighbour. Equal to an edge length in real model.
	 * @param sidePadding Distance of the intersection to the side.
	 * @param entry       Mark if it is entry, otherwise exit.
	 * @param index       Index of entry. Equal to the index of vertex it should be connected to.
	 */
	private void drawAbstractHexagonalEntry(long granularity, double shift, double edgeLength, double sidePadding, boolean entry, long index) {
		Color color = entry ? ENTRY_COLOR : EXIT_COLOR;

		double xShift = shift / 2, yShift = Math.sqrt(3) / 2 * shift,
			radius = 2 * edgeLength * VERTEX_RATIO;

		// compute coordinates
		double a0x = (1.5 * index + 0.5) * edgeLength + sidePadding, a0y = (granularity - index + 2) * shift / 2,     // top left road center
			f0x = preferredHeight / 2 + edgeLength * index * 1.5, f0y = shift * (index / 2. + 1.5),                     // top right road center
			c0x = preferredHeight - f0x, c0y = preferredHeight - f0y,
			d0x = preferredHeight - a0x, d0y = preferredHeight - a0y,
			b0x = sidePadding + edgeLength / 2, e0y = (granularity / 2. + index + 1) * shift,
			e0x = preferredHeight - b0x, b0y = preferredHeight - e0y;

		// create roads
		Line entryALine = new Line(a0x - xShift, a0y - yShift, a0x, a0y),
			entryBLine = new Line(b0x - shift, b0y, b0x, b0y),
			entryCLine = new Line(c0x - xShift, c0y + yShift, c0x, c0y),
			entryDLine = new Line(d0x + xShift, d0y + yShift, d0x, d0y),
			entryELine = new Line(e0x + shift, e0y, e0x, e0y),
			entryFLine = new Line(f0x + xShift, f0y - yShift, f0x, f0y);

		// create vertices
		Circle entryA = new Circle(a0x - xShift, a0y - yShift, radius),  // top left entry
			entryB = new Circle(b0x - shift, b0y, radius),                         // mid left entry
			entryC = new Circle(c0x - xShift, c0y + yShift, radius),       // bot left entry
			entryD = new Circle(d0x + xShift, d0y + yShift, radius),       // bot right entry
			entryE = new Circle(e0x + shift, e0y, radius),                         // mid right entry
			entryF = new Circle(f0x + xShift, f0y - yShift, radius);       // top right entry


		for (Circle vertex : Arrays.asList(entryA, entryB, entryC, entryD, entryE, entryF)) {
			fillCircle(vertex, color);
		}

		getChildren().addAll(entryALine, entryBLine, entryCLine, entryDLine, entryELine, entryFLine, entryA, entryB, entryC, entryD, entryE, entryF);

		// push lines to back
		entryALine.toBack();
		entryBLine.toBack();
		entryCLine.toBack();
		entryDLine.toBack();
		entryELine.toBack();
		entryFLine.toBack();
	}

	/**
	 * Convert List of double values into array of double.
	 *
	 * @param values List of double values.
	 * @return Array of converted values.
	 */
	private double[] getHexagonalEntryPoints(List<Double> values) {
		return values.stream().mapToDouble(Double::doubleValue).toArray();
	}

	/**
	 * Draw octagonal model.
	 *
	 * @param granularity Granularity of the model.
	 * @param entries     Number of entries.
	 * @param exits       Number of exits.
	 */
	private void drawOctagonalModel(long granularity, long entries, long exits) {
		graph = new SimulationGraph(granularity, OCTAGONAL, entries, exits);
		getChildren().clear();

		// TODO udelat velikost poradne
		double height = preferredHeight, shift = height / (granularity + 2);

		if (!IntersectionMenu.isAbstract()) {
			for (Vertex v : graph.getVertices()) {
				if (v.getType() == Type.ROAD) {
					if (v.getID() < granularity * granularity - 4) {
						drawOctagon(shift, v.getX(), v.getY(), String.valueOf(v.getID()), v.getType().getColor());
					} else if (v.getID() < 2 * granularity * (granularity - 1) - 3) {
						drawObliqueSquare((1 - OCTAGON_RATIO) * shift, v.getX(), v.getY(), String.valueOf(v.getID()), v.getType().getColor());
					}
				} else {
					drawOctagonalEntry(shift, v.getX(), v.getY(), String.valueOf(v.getID()), v.getType().getColor(), v.getType().getDirection());
				}
			}
		} else {
			for (Edge e : graph.getEdges()) {
				Vertex u = (Vertex) e.getU(),
					v = (Vertex) e.getV();
				Line l = new Line(u.getX(), u.getY(), v.getX(), v.getY());
				getChildren().add(l);
			}
			for (Vertex v : graph.getVertices()) {
				Circle c = new Circle(shift * VERTEX_RATIO, v.getType().getColor());
				Text t = new Text(String.valueOf(v.getID()));
				t.setBoundsType(TextBoundsType.VISUAL);
				StackPane stack = new StackPane(c, t);
				stack.setLayoutX(v.getX() - shift * VERTEX_RATIO);
				stack.setLayoutY(v.getY() - shift * VERTEX_RATIO);
				getChildren().add(stack);
			}
		}
	}

	/**
	 * Draw one line of the Octagonal model.
	 *
	 * @param granularity Granularity of the graph.
	 * @param row         Number of the row.
	 * @param shift       Distance between 2 vertices.
	 * @param withSquares Mark if there is a row of in-between vertices under the row.
	 * @param withCorners Mark if there are present corner vertices.
	 */
	private void drawOctagonalLine(long granularity, long row, double shift, boolean withSquares, boolean withCorners) {
		double topY = row * shift; // coordination Y of the highest point in the row

		// draw grid vertices in the row
		for (int j = withCorners ? 1 : 2; j <= (withCorners ? granularity : granularity - 1); j++) {
			if (IntersectionMenu.isAbstract()) {
				drawAbstractOctagonalRoad(shift, (j + 0.5) * shift, topY + shift / 2, granularity, row, j);
			} else {
				drawRealOctagonalRoad(shift, topY, (row + OCTAGON_RATIO) * shift, j);
			}
		}

		if (!withSquares) {
			return;
		}

		// draw in-between vertices under the row
		for (int j = 2; j <= granularity; j++) {
			if (IntersectionMenu.isAbstract()) {
				drawAbstractOctagonalSquareRoad(shift, j * shift, topY + shift, row < granularity - 1 || j > 2, row < granularity - 1 || j < granularity);
			} else {
				drawRealOctagonalSquareRoad(shift, topY, (row + OCTAGON_RATIO) * shift, j);
			}
		}
	}

	/**
	 * Draw vertex in between square grid with roads to lower neighbours.
	 *
	 * @param shift   Distance between 2 vertices.
	 * @param topY    Coordination Y of the highest point in the octagon representing the vertex.
	 * @param midTopY Coordination Y of the highest point on the leftest line in the octagon representing the vertex.
	 * @param rowID   Index of the vertex in the row.
	 */
	private void drawRealOctagonalSquareRoad(double shift, double topY, double midTopY, int rowID) {
		Polygon square = new Polygon(
			rowID * shift, midTopY + (1 - 2 * OCTAGON_RATIO) * shift,      // top
			(rowID - OCTAGON_RATIO) * shift, topY + shift,                         // right
			rowID * shift, midTopY + shift,                                        // bottom
			(rowID + OCTAGON_RATIO) * shift, topY + shift                          // left
		);

		square.setFill(ROAD_COLOR);
		square.setStroke(STROKE_COLOR);

		getChildren().add(square);
	}

	/**
	 * Draw abstract vertex in between square grid with roads to lower neighbours.
	 *
	 * @param shift         Distance between 2 vertices.
	 * @param x             Coordinate X of drawn vertex.
	 * @param y             Coordinate Y of drawn vertex.
	 * @param withLeftLine  Mark if there is a neighbour on the left.
	 * @param withRightLine Mark if there is a neighbour on the right.
	 */
	private void drawAbstractOctagonalSquareRoad(double shift, double x, double y, boolean withLeftLine, boolean withRightLine) {
		if (withLeftLine) {
			Line leftObliqueLine = new Line(x, y, x - shift / 2, y + shift / 2);

			getChildren().add(leftObliqueLine);
		}

		if (withRightLine) {
			Line rightObliqueLine = new Line(x, y, x + shift / 2, y + shift / 2);

			getChildren().add(rightObliqueLine);
		}

		// create vertex
		Circle vertex = new Circle(x, y, shift * VERTEX_RATIO);
		fillCircle(vertex, ROAD_COLOR);

		getChildren().add(vertex);
	}

	/**
	 * Draw all rectangles representing entry / exit roads of the graph.
	 *
	 * @param granularity Granularity of the graph.
	 * @param entries     Desired number of entries.
	 * @param exits       Desired number of exits.
	 * @param shift       Distance between 2 vertices.
	 */
	private void drawOctagonalEntriesAndExits(long granularity, long entries, long exits, double shift) {
		// compute outer empty spaces
		long empty = granularity - entries - exits,
			padding = empty / 3,
			index = empty % 3 == 2 ? ++padding : padding;

		// draw all of the entries
		while (entries-- > 0) {
			if (IntersectionMenu.isAbstract()) {
				drawAbstractRectangularEntries(granularity, shift, ++index, true);
			} else {
				drawRectangularEntries(granularity, shift, ++index, true, false);
			}
		}

		// skip middle empty space
		index += empty - 2 * padding;

		// draw all of the exits
		while (exits-- > 0) {
			if (IntersectionMenu.isAbstract()) {
				drawAbstractRectangularEntries(granularity, shift, ++index, false);
			} else {
				drawRectangularEntries(granularity, shift, ++index, false, false);
			}
		}
	}

	/**
	 * Draw rectangles representing entry / exit roads of the graph from all 4 directions.
	 *
	 * @param granularity Granularity of the graph.
	 * @param shift       Distance between 2 vertices.
	 * @param index       Index of the entry, counting from the edge.
	 * @param entry       If set, it is entry, otherwise exit.
	 * @param square      If set, drawing square model, otherwise octagonal.
	 */
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

	/**
	 * Draw abstract entry / exit vertexes to the graph from all 4 directions and connect them to the graph with roads.
	 *
	 * @param granularity Granularity of the graph.
	 * @param shift       Distance between 2 vertices.
	 * @param index       Index of the entry, counting from the edge.
	 * @param entry       If set, it is entry, otherwise exit.
	 */
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

		fillCircle(entryNVertex, color);
		fillCircle(entrySVertex, color);
		fillCircle(entryWVertex, color);
		fillCircle(entryEVertex, color);

		getChildren().addAll(entryNLine, entrySLine, entryWLine, entryELine, entryNVertex, entrySVertex, entryWVertex, entryEVertex);
	}

	/**
	 * Set fill of the vertex to color.
	 *
	 * @param Vertex Affected circle object.
	 * @param color  Color to fill the vertex with.
	 */
	private void fillCircle(Circle Vertex, Color color) {
		Vertex.setFill(color);
		Vertex.setStroke(STROKE_COLOR);
	}

	/**
	 * Draw rectangles representing roads connecting vertex to its neighbours.
	 *
	 * @param shift   Distance between 2 vertices.
	 * @param topY    Coordination Y of the highest point in the octagon representing the vertex.
	 * @param midTopY Coordination Y of the highest point on the leftest line in the octagon representing the vertex.
	 * @param rowID   Index of the vertex in the row.
	 */
	private void drawRealOctagonalRoad(double shift, double topY, double midTopY, int rowID) {
		Polygon octagon = new Polygon(
			(rowID + OCTAGON_RATIO) * shift, topY,                     // top left
			(rowID + 1 - OCTAGON_RATIO) * shift, topY,                         // top right
			(rowID + 1) * shift, midTopY,                                      // mid-top right
			(rowID + 1) * shift, midTopY + (1 - 2 * OCTAGON_RATIO) * shift,    // mid-bot right
			(rowID + 1 - OCTAGON_RATIO) * shift, topY + shift,                 // bot right
			(rowID + OCTAGON_RATIO) * shift, topY + shift,                     // bot left
			rowID * shift, midTopY + (1 - 2 * OCTAGON_RATIO) * shift,          // mid-bot left
			rowID * shift, midTopY                                             // mid-top left
		);

		octagon.setFill(ROAD_COLOR);
		octagon.setStroke(STROKE_COLOR);

		getChildren().add(octagon);
	}

	/**
	 * Draw lines representing abstract road connecting vertex to its neighbours.
	 *
	 * @param shift       Distance between 2 vertices.
	 * @param x           Coordinate X of drawn vertex.
	 * @param y           Coordinate Y of drawn vertex.
	 * @param granularity Granularity of drawn graph.
	 * @param row         Index of the row in which is the vertex located.
	 * @param column      Index of the column in which is the vertex located.
	 */
	private void drawAbstractOctagonalRoad(double shift, double x, double y, long granularity, long row, long column) {
		boolean withRightRoad = (row > 1 && row < granularity && column < granularity) || column < granularity - 1,
			withDownRoad = row < granularity - 1 || (row == granularity - 1 && column > 1 && column < granularity);

		// if vertex not in first row or first column
		if (row < granularity && column > 1) {
			// draw line to upper left neighbour
			Line obliqueLeftEdge = new Line(x, y, x - shift / 2, y + shift / 2);

			getChildren().add(obliqueLeftEdge);
		}

		// if vertex not in first row or last column
		if (row < granularity && column < granularity) {
			// draw line to upper right neighbour
			Line obliqueRightEdge = new Line(x, y, x + shift / 2, y + shift / 2);

			getChildren().add(obliqueRightEdge);
		}

		// draw the rest of the lines
		drawAbstractSquareRoad(shift, x, y, withRightRoad, withDownRoad);
	}

	/**
	 * Draw green square.
	 *
	 * @param height size of the square.
	 */
	private void drawBackground(double height) {
		Rectangle backgroundSquare = new Rectangle(0, 0, height, height);
		backgroundSquare.setFill(Color.LAWNGREEN);

		getChildren().add(backgroundSquare);
		backgroundSquare.toBack();
	}

	/**
	 * Redraw graph based on selected properties in menu window.
	 */
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
		drawBackground(preferredHeight);
	}

	/**
	 * @return Preferred height of the graph.
	 */
	public static double getPreferredHeight() {
		return preferredHeight;
	}

	/**
	 * Set preferred height of the graph.
	 *
	 * @param preferredHeight New height the graph should have.
	 */
	public static void setPreferredHeight(double preferredHeight) {
		IntersectionGraph.preferredHeight = preferredHeight;
	}
}
