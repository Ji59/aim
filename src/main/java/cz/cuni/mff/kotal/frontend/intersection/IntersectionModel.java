package cz.cuni.mff.kotal.frontend.intersection;


import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.frontend.simulation.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Edge;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import cz.cuni.mff.kotal.simulation.graph.Vertex.Type;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import javafx.scene.text.TextBoundsType;
import javafx.stage.Screen;
import javafx.util.Pair;

import java.util.*;


/**
 * Pane for Intersection model.
 */
public class IntersectionModel extends Pane {
	private static final double GOLDEN_RATIO = (Math.sqrt(5) - 1) / 2;
	private static final double OCTAGON_RATIO = 1 / Math.E;
	private static final double VERTEX_RATIO = GOLDEN_RATIO / 2;
	private static final Color STROKE_COLOR = Color.BLACK;

	// TODO dont use static, use Component
	private static double preferredHeight = Screen.getPrimary().getVisualBounds().getHeight();
	private static SimulationGraph graph;
	private static List<Node> nodes = new ArrayList<>();
	private static final Map<SimulationGraph, Pair<Graph, List<Node>>> createdGraphs = new HashMap<>();
	private static final Deque<SimulationGraph> historyPrevious = new ArrayDeque<>();
	private static final Deque<SimulationGraph> historyNext = new ArrayDeque<>();


	/**
	 * Create square graph.
	 *
	 * @param height dimension of the graph
	 */
	public IntersectionModel(double height) {
		// TODO udelat velikost poradne
		setPrefWidth(height);
		setPrefHeight(height);
		preferredHeight = height;
		drawBackground(height);
	}

	/**
	 * Draw abstract model of the graph.
	 *
	 * @param shift Distance between 2 vertices
	 */
	private void drawAbstractModel(double shift) {
		for (Edge e : graph.getEdges()) {
			GraphicalVertex u = (GraphicalVertex) e.getU();
			GraphicalVertex v = (GraphicalVertex) e.getV();
			Line l = new Line(u.getX(), u.getY(), v.getX(), v.getY());
			nodes.add(l);
		}
		for (GraphicalVertex v : graph.getVertices()) {
			Circle c = new Circle(shift * VERTEX_RATIO, v.getType().getColor());
			Text t = new Text(String.valueOf(v.getID()));
			t.setBoundsType(TextBoundsType.VISUAL);
			StackPane stack = new StackPane(c, t);
			stack.setLayoutX(v.getX() - shift * VERTEX_RATIO);
			stack.setLayoutY(v.getY() - shift * VERTEX_RATIO);
			nodes.add(stack);
		}
	}

	/**
	 * Draw square model.
	 *
	 * @param granularity Granularity of the model
	 */
	private void drawSquareModel(long granularity) {
		// TODO udelat velikost poradne
		double height = preferredHeight;
		double shift = height / (granularity + 2);

		if (!IntersectionMenu.isAbstract()) {
			for (GraphicalVertex v : graph.getVertices()) {
				drawSquare(shift, v.getX(), v.getY(), String.valueOf(v.getID()), v.getType().getColor());
			}
		} else {
			drawAbstractModel(shift);
		}
	}

	/**
	 * Draw hexagonal model.
	 *
	 * @param granularity Granularity of the model
	 */
	private void drawHexagonalModel(long granularity) {
		// TODO udelat velikost poradne

		double height = preferredHeight;
		double shift = height / (2 * granularity + 1);
		if (IntersectionMenu.isAbstract()) {
			drawAbstractModel(shift);
		} else {
			for (GraphicalVertex v : graph.getVertices()) {
				if (v.getType() == Type.ROAD) {
					drawHexagon(shift, v.getX(), v.getY(), String.valueOf(v.getID()), v.getType().getColor());
				} else {
					drawHexagonEntry(shift, v);
				}
			}
		}
	}

	/**
	 * Draw octagonal model.
	 *
	 * @param granularity Granularity of the model
	 */
	private void drawOctagonalModel(long granularity) {
		// TODO udelat velikost poradne
		double height = preferredHeight;
		double shift = height / (granularity + 2);

		if (!IntersectionMenu.isAbstract()) {
			for (GraphicalVertex v : graph.getVertices()) {
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
			drawAbstractModel(shift);
		}
	}

	/**
	 * Draw square at specified location.
	 *
	 * @param size  Size of the square side
	 * @param x     Coordinate X of center of the square
	 * @param y     Coordinate Y of center of the square
	 * @param text  The text inside the square
	 * @param color Color of the square
	 */
	private void drawSquare(double size, double x, double y, String text, Color color) {
		double halfSize = size / 2;

		Rectangle square = new Rectangle(size, size, color);
		square.setStroke(STROKE_COLOR);
		square.setX(x - halfSize);
		square.setY(y - halfSize);
		nodes.add(square);

		addTextField(x - halfSize, y - halfSize, size, size, text);
	}

	/**
	 * Draw rectangle with text in it.
	 *
	 * @param x      Coordinate X of the top left corner
	 * @param y      Coordinate Y of the top left corner
	 * @param width  Width of the rectangle
	 * @param height Height of the rectangle
	 * @param text   Text to be in the rectangle
	 * @param color  Color inside the rectangle
	 */
	private void drawRectangle(double x, double y, double width, double height, String text, Color color) {
		Rectangle rectangle = new Rectangle(x, y, width, height);
		rectangle.setStroke(STROKE_COLOR);
		rectangle.setFill(color);
		nodes.add(rectangle);

		addTextField(x, y, width, height, text);
	}

	/**
	 * Draw hexagon.
	 *
	 * @param shift Distance between 2 opposite sides
	 * @param x     Coordinate X of the center of the hexagon
	 * @param y     Coordinate Y of the center of the hexagon
	 * @param text  Text to be inside the hexagon
	 * @param color Color inside the hexagon
	 */
	private void drawHexagon(double shift, double x, double y, String text, Color color) {
		double tan30HalfShift = Math.sqrt(3) * shift / 6;
		double halfShift = shift / 2;

		Polygon hexagon = new Polygon(
			x + tan30HalfShift, y - halfShift, // top right
			x + 2 * tan30HalfShift, y,                 // right
			x + tan30HalfShift, y + halfShift,         // bottom right
			x - tan30HalfShift, y + halfShift,         // bottom left
			x - 2 * tan30HalfShift, y,                 // left
			x - tan30HalfShift, y - halfShift          // top left
		);
		hexagon.setFill(color);
		hexagon.setStroke(STROKE_COLOR);
		nodes.add(hexagon);

		addTextField(x - 2 * tan30HalfShift, y - shift / 2, 4 * tan30HalfShift, shift, text);
	}

	/**
	 * Draw octagon at specified location.
	 *
	 * @param size  Distance between opposite sides
	 * @param x     Coordinate X of center of the octagon
	 * @param y     Coordinate Y of center of the octagon
	 * @param text  The text inside the octagon
	 * @param color Color of the octagon
	 */
	private void drawOctagon(double size, double x, double y, String text, Color color) {
		double halfSize = size / 2;
		double shorterSize = OCTAGON_RATIO * halfSize;

		// create polygon
		Polygon octagon = new Polygon(
			x - shorterSize, y - halfSize, // top left
			x + shorterSize, y - halfSize, // top right
			x + halfSize, y - shorterSize, // mid-top right
			x + halfSize, y + shorterSize, // mid-bot right
			x + shorterSize, y + halfSize, // bot right
			x - shorterSize, y + halfSize, // bot left
			x - halfSize, y + shorterSize, // mid-bot left
			x - halfSize, y - shorterSize  // mid-top left
		);
		octagon.setFill(color);
		octagon.setStroke(STROKE_COLOR);
		nodes.add(octagon);

		addTextField(x - halfSize, y - halfSize, size, size, text);
	}

	/**
	 * Draw square at specified location rotated 45 deg.
	 *
	 * @param size  Distance between opposite vertices
	 * @param x     Coordinate X of center of the square
	 * @param y     Coordinate Y of center of the square
	 * @param text  The text inside the square
	 * @param color Color of the square
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
		nodes.add(square);

		addTextField(x - halfSize, y - halfSize, size, size, text);
	}

	/**
	 * Draw an entry / exit.
	 *
	 * @param shift Distance between 2 opposite sides of hexagons in the model
	 * @param v     Vertex symbolizing the entry / exit
	 */
	private void drawHexagonEntry(double shift, GraphicalVertex v) {
		switch (v.getType()) {
			case ENTRY2, EXIT2, ENTRY5, EXIT5 -> {
				double x = v.getType() == Type.ENTRY5 || v.getType() == Type.EXIT5 ? 0 : v.getX() + (Math.sqrt(3) / 6 - 1) * shift;
				double y = v.getY() - shift / 2;
				double width = (graph.getGranularity() - Math.sqrt(3) * (graph.getGranularity() - 2. / 3) / 2 + 0.5) * shift;
				double height = shift;
				drawRectangle(x, y, width, height, String.valueOf(v.getID()), v.getType().getColor());
				return;
			}
			case ENTRY0, EXIT0 -> {
				double x0 = v.getX() + shift * (1. / 2 - 1 / Math.sqrt(3));
				double y0 = v.getY() + Math.sqrt(3) * shift / 2;
				double x1 = v.getX() + shift * (1 + Math.sqrt(3) / 3) / 2;
				double y1 = v.getY() + (Math.sqrt(3) - 1) * shift / 2;
				double x2 = Math.max(x1 - Math.sqrt(3) * y1 / 3, 0);
				double y2 = x2 == 0 ? y1 - Math.sqrt(3) * x1 : 0;
				double x3 = Math.max(x0 - Math.sqrt(3) * y0 / 3, 0);
				double y3 = x3 == 0 ? y0 - Math.sqrt(3) * x0 : 0;
				drawHexagonalModelObliqueEntry(x0, y0, x1, y1, x2, y2, x3, y3, x3 == 0, y2 == 0, 0., v.getType().getColor());
			}
			case ENTRY1, EXIT1 -> {
				double x0 = v.getX() - shift * (1 + Math.sqrt(3) / 3) / 2;
				double y0 = v.getY() + (Math.sqrt(3) - 1) * shift / 2;
				double x1 = v.getX() + shift * (1 / Math.sqrt(3) - 1. / 2);
				double y1 = v.getY() + Math.sqrt(3) * shift / 2;
				double x2 = Math.min(x1 + Math.sqrt(3) * y1 / 3, preferredHeight);
				double y2 = x2 == preferredHeight ? y1 - Math.sqrt(3) * (preferredHeight - x1) : 0;
				double x3 = Math.min(x0 + Math.sqrt(3) * y0 / 3, preferredHeight);
				double y3 = x3 == preferredHeight ? y0 - Math.sqrt(3) * (preferredHeight - x0) : 0;
				drawHexagonalModelObliqueEntry(x0, y0, x1, y1, x2, y2, x3, y3, x2 == preferredHeight, y3 == 0, preferredHeight, v.getType().getColor());
			}
			default -> {
			}
		}
		addTextField(v.getX() - shift / 2, v.getY() - shift / 2, shift, shift, String.valueOf(v.getID()));
	}

	/**
	 * Create two quadrilaterals or pentagons symbolizing opposite oblique entries / exits for hexagonal model.
	 * If the entry is over frame corner, it adds fifth point.
	 *
	 * @param x0      Coordinate X of bottom left corner
	 * @param y0      Coordinate Y of bottom left corner
	 * @param x1      Coordinate X of bottom right corner
	 * @param y1      Coordinate Y of bottom right corner
	 * @param x2      Coordinate X of top right corner
	 * @param y2      Coordinate Y of top right corner
	 * @param x3      Coordinate X of top left corner
	 * @param y3      Coordinate Y of top left corner
	 * @param zeroX   Flag if the entry is touching sides
	 * @param zeroY   Flag if the entry is touching top side
	 * @param cornerX Coordinate X of corner the entry is directing to
	 */
	private void drawHexagonalModelObliqueEntry(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, boolean zeroX, boolean zeroY, double cornerX, Color color) {
		// create associated entry
		Polygon polygon = new Polygon(
			x0, y0,
			x1, y1,
			x2, y2,
			x3, y3
		);
		if (zeroX && zeroY) {
			polygon.getPoints().addAll(6, Arrays.asList(cornerX, 0.));
		}
		polygon.setStroke(STROKE_COLOR);
		polygon.setFill(color);
		nodes.add(polygon);

		// create opposite entry
		Polygon polygon1 = new Polygon(
			preferredHeight - x0, preferredHeight - y0,
			preferredHeight - x1, preferredHeight - y1,
			preferredHeight - x2, preferredHeight - y2,
			preferredHeight - x3, preferredHeight - y3
		);
		if (zeroX && zeroY) {
			polygon1.getPoints().addAll(6, Arrays.asList(preferredHeight - cornerX, preferredHeight));
		}
		polygon1.setStroke(STROKE_COLOR);
		polygon1.setFill(color);
		nodes.add(polygon1);
	}

	/**
	 * Create rectangular entry / exit for octagonal model.
	 *
	 * @param size      "Width" of the road
	 * @param x         Coordinate X of the entry / exit in graph
	 * @param y         Coordinate Y of the entry / exit in graph
	 * @param text      The text inside the entry / exit
	 * @param color     Color of the rectangle
	 * @param direction Direction of the entry / exit. (See Vertex object for more details)
	 */
	private void drawOctagonalEntry(double size, double x, double y, String text, Color color, int direction) {
		assert (direction < 4);
		// create rectangle
		Rectangle rectangle = new Rectangle();
		rectangle.setFill(color);
		rectangle.setStroke(STROKE_COLOR);

		// compute location and size
		double halfSize = size / 2;
		double xLocation = x - (direction == 3 ? (2 - OCTAGON_RATIO) * halfSize : halfSize);
		double yLocation = y - (direction == 1 ? (2 - OCTAGON_RATIO) * halfSize : halfSize);
		double width = direction < 2 ? size : (3 - OCTAGON_RATIO) * halfSize;
		double height = direction < 2 ? (3 - OCTAGON_RATIO) * halfSize : size;

		// set rectangle location and size
		rectangle.setX(xLocation);
		rectangle.setY(yLocation);
		rectangle.setWidth(width);
		rectangle.setHeight(height);
		nodes.add(rectangle);

		// add text
		addTextField(xLocation, yLocation, width, height, text);
	}

	/**
	 * Adds rectangular text field.
	 *
	 * @param x      Coordinate X of the text field
	 * @param y      Coordinate Y of the text field
	 * @param width  Width of the text field
	 * @param height Height of the text field
	 * @param text   Text of the text field
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
		nodes.add(t);
	}

	/**
	 * Redraw graph based on selected properties in menu window.
	 * If the graph was not changed, do nothing.
	 */
	public void redraw() {
		// get graph properties
		long granularity = IntersectionMenuTab0.getGranularity().getValue();
		long entries = IntersectionMenuTab0.getEntries().getValue();
		long exits = IntersectionMenuTab0.getExits().getValue();
		IntersectionMenuTab0.Parameters.Models model = IntersectionMenuTab0.getModel();

		// create graph with key properties set
		SimulationGraph graphAbstract = new SimulationGraph(granularity, entries, exits, model, false, null, null, null, preferredHeight, IntersectionMenu.isAbstract());

		if (graphAbstract.equals(graph)) { // if last graph is the same, return
			return;
		}

		IntersectionScene.resetSimulation();

		if (graph != null && preferredHeight != graph.getSize()) { // if size of the model changed, discard all saved graphs
			createdGraphs.clear();
			historyNext.clear();
			historyPrevious.clear();
		} else if (graph != null) { // else add graph to previous stack
			historyPrevious.push(graph);
		}

		if (createdGraphs.containsKey(graphAbstract)) { // if graph already exists and model objects already exist
			setGraphFromAbstract(graphAbstract);
		} else { // create new graph and all the model nodes.
			historyNext.clear();
			graph = new SimulationGraph(granularity, model, entries, exits, preferredHeight, IntersectionMenu.isAbstract());
			nodes = new ArrayList<>();

			switch (model) {
				case SQUARE:
					drawSquareModel(granularity);
					break;
				case HEXAGONAL:
					drawHexagonalModel(granularity);
					break;
				case OCTAGONAL:
					drawOctagonalModel(granularity);
					break;
				case CUSTOM:
					break;
			}

			createdGraphs.put(graph, new Pair<>(graph, nodes));
			getChildren().setAll(nodes);
		}

		drawBackground(preferredHeight);
	}

	/**
	 * Add vertices and edges to the abstract graph and set visual elements to math the graph.
	 *
	 * @param graphAbstract Graph with necessary elements
	 */
	private void setGraphFromAbstract(SimulationGraph graphAbstract) {
		assert createdGraphs.containsKey(graphAbstract);
		Pair<Graph, List<Node>> graphValues = createdGraphs.get(graphAbstract);
		getChildren().setAll(graphValues.getValue());
		graphAbstract.addGraphVertices(graphValues.getKey());
		graph = graphAbstract;
	}

	/**
	 * Draw last saved different model.
	 */
	public void drawPreviousGraph() {
		if (!historyPrevious.isEmpty()) {
			SimulationGraph graphAbstract = historyPrevious.pop();
			historyNext.push(graph);
			IntersectionScene.resetSimulation();
			setGraphFromAbstract(graphAbstract);
			drawBackground(preferredHeight);
		}
	}

	/**
	 * Draw first graph created after the current.
	 */
	public void drawNextGraph() {
		if (!historyNext.isEmpty()) {
			SimulationGraph graphAbstract = historyNext.pop();
			historyPrevious.push(graph);
			IntersectionScene.resetSimulation();
			setGraphFromAbstract(graphAbstract);
			drawBackground(preferredHeight);
		}
	}

	/**
	 * Draw green square.
	 *
	 * @param height size of the square
	 */
	private void drawBackground(double height) {
		Rectangle backgroundSquare = new Rectangle(0, 0, height, height);
		backgroundSquare.setFill(Color.LAWNGREEN);

		getChildren().add(backgroundSquare);
		backgroundSquare.toBack();
	}

	/**
	 * @return Preferred height of the graph
	 */
	public static double getPreferredHeight() {
		return preferredHeight;
	}

	/**
	 * Set preferred height of the graph.
	 *
	 * @param preferredHeight New height the graph should have
	 */
	public static void setPreferredHeight(double preferredHeight) {
		IntersectionModel.preferredHeight = preferredHeight;
	}

	/**
	 * @return Currently shown graph
	 */
	public static SimulationGraph getGraph() {
		return graph;
	}
}
