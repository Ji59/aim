package cz.cuni.mff.kotal.frontend.intersection;


import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.frontend.simulation.AbstractGraph;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.graph.*;
import cz.cuni.mff.kotal.simulation.graph.Vertex.Type;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.stage.Screen;

import java.util.*;


/**
 * Pane for Intersection model.
 */
public class IntersectionModel extends Pane {
	private static final double GOLDEN_RATIO = (Math.sqrt(5) - 1) / 2;
	private static final double OCTAGON_RATIO = 1 / Math.E;
	public static final double VERTEX_RATIO = GOLDEN_RATIO / 2;
	private static final Color STROKE_COLOR = Color.BLACK;
	public static final double MAX_COLOR_CHANGE = 0.0078125;
	public static final Color BACKGROUND_COLOR = Color.color(0.25, 0.34375, 0.28125);

	// TODO dont use static, use Component
	private static double preferredHeight = Screen.getPrimary().getVisualBounds().getHeight();
	private static SimulationGraph graph;
	private List<Node> nodes = new ArrayList<>();
	private static final Map<Integer, Shape> vertexNodes = new HashMap<>();
	private final Map<SimulationGraph, SimulationGraph> createdGraphs = new HashMap<>();
	private final Deque<SimulationGraph> historyPrevious = new ArrayDeque<>();
	private final Deque<SimulationGraph> historyNext = new ArrayDeque<>();


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
			Line l = new Line(vCoor(u.getX()), vCoor(u.getY()), vCoor(v.getX()), vCoor(v.getY()));
			nodes.add(l);
		}
		for (GraphicalVertex vertex : graph.getVertices()) {
			Circle circle = new Circle(shift * VERTEX_RATIO, vertex.getType().getColor());
			Text t = new Text(String.valueOf(vertex.getID()));
			t.setBoundsType(TextBoundsType.VISUAL);
			StackPane stack = new StackPane(circle, t);
			stack.setLayoutX(vCoor(vertex.getX()) - shift * VERTEX_RATIO);
			stack.setLayoutY(vCoor(vertex.getY()) - shift * VERTEX_RATIO);
			nodes.add(stack);
			vertexNodes.put(vertex.getID(), circle);
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
			for (GraphicalVertex vertex : graph.getVertices()) {
				drawSquareVertex(shift, vCoor(vertex.getX()), vCoor(vertex.getY()), vertex.getID(), vertex.getType().getColor());
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
			for (GraphicalVertex vertex : graph.getVertices()) {
				if (vertex.getType() == Type.ROAD) {
					drawHexagon(shift, vCoor(vertex.getX()), vCoor(vertex.getY()), vertex.getID(), vertex.getType().getColor());
				} else {
					drawHexagonEntry(shift, vertex);
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
		double shift = height * graph.getCellSize();

		if (!IntersectionMenu.isAbstract()) {
			for (GraphicalVertex vertex : graph.getVertices()) {
				if (vertex.getType() == Type.ROAD) {
					if (vertex.getID() < granularity * granularity - 4) {
						drawOctagon(shift, vCoor(vertex.getX()), vCoor(vertex.getY()), vertex.getID(), vertex.getType().getColor());
					} else {
						drawObliqueSquare((1 - OCTAGON_RATIO) * shift, vCoor(vertex.getX()), vCoor(vertex.getY()), vertex.getID(), vertex.getType().getColor());
					}
				} else {
					drawOctagonalEntry(shift, vCoor(vertex.getX()), vCoor(vertex.getY()), vertex.getID(), vertex.getType().getColor(), vertex.getType().getDirection());
				}
			}
		} else {
			drawAbstractModel(shift);
		}
	}

	/**
	 * Draw square representing vertex at specified location.
	 * Adds vertex node to map.
	 *
	 * @param size  Size of the square side
	 * @param x     Coordinate X of center of the square
	 * @param y     Coordinate Y of center of the square
	 * @param id    The id of vertex to be shown inside the square
	 * @param color Color of the square
	 */
	private void drawSquareVertex(double size, double x, double y, int id, Color color) {
		double halfSize = size / 2;

		Rectangle square = new Rectangle(size, size, color);
		square.setStroke(STROKE_COLOR);
		square.setX(x - halfSize);
		square.setY(y - halfSize);
		nodes.add(square);
		vertexNodes.put(id, square);

		addTextField(x - halfSize, y - halfSize, size, size, String.valueOf(id));
	}

	/**
	 * Draw rectangle vertex with id in it.
	 *
	 * @param x      Coordinate X of the top left corner
	 * @param y      Coordinate Y of the top left corner
	 * @param width  Width of the rectangle
	 * @param height Height of the rectangle
	 * @param id     ID to be shown inside the rectangle
	 * @param color  Color inside the rectangle
	 */
	private void drawHexagonalModelRectangleEntry(double x, double y, double width, double height, int id, Color color) {
		Rectangle rectangle = new Rectangle(x, y, width, height);
		rectangle.setStroke(STROKE_COLOR);
		rectangle.setFill(color);
		nodes.add(rectangle);
		vertexNodes.put(id, rectangle);

		addTextField(x, y, width, height, String.valueOf(id));
	}

	/**
	 * Draw hexagon representing vertex.
	 *
	 * @param shift Distance between 2 opposite sides
	 * @param x     Coordinate X of the center of the hexagon
	 * @param y     Coordinate Y of the center of the hexagon
	 * @param id    ID of the vertex to be shown inside the hexagon
	 * @param color Color inside the hexagon
	 */
	private void drawHexagon(double shift, double x, double y, int id, Color color) {
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
		vertexNodes.put(id, hexagon);

		addTextField(x - 2 * tan30HalfShift, y - shift / 2, 4 * tan30HalfShift, shift, String.valueOf(id));
	}

	/**
	 * Draw octagon representing vertex at specified location.
	 *
	 * @param size  Distance between opposite sides
	 * @param x     Coordinate X of center of the octagon
	 * @param y     Coordinate Y of center of the octagon
	 * @param id    ID of vertex to be shown inside the octagon
	 * @param color Color of the octagon
	 */
	private void drawOctagon(double size, double x, double y, int id, Color color) {
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
		vertexNodes.put(id, octagon);

		addTextField(x - halfSize, y - halfSize, size, size, String.valueOf(id));
	}

	/**
	 * Draw square at specified location rotated 45 deg representing vertex in octagonal model.
	 *
	 * @param size  Distance between opposite vertices
	 * @param x     Coordinate X of center of the square
	 * @param y     Coordinate Y of center of the square
	 * @param id    The ID of vertex to be shown inside the square
	 * @param color Color of the square
	 */
	private void drawObliqueSquare(double size, double x, double y, int id, Color color) {
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
		vertexNodes.put(id, square);

		addTextField(x - halfSize, y - halfSize, size, size, String.valueOf(id));
	}

	/**
	 * Draw an entry / exit.
	 *
	 * @param shift  Distance between 2 opposite sides of hexagons in the model
	 * @param vertex Vertex symbolizing the entry / exit
	 */
	private void drawHexagonEntry(double shift, GraphicalVertex vertex) {
		switch (vertex.getType()) {
			case ENTRY2, EXIT2, ENTRY5, EXIT5 -> {
				double x = vertex.getType() == Type.ENTRY5 || vertex.getType() == Type.EXIT5 ? 0 : vCoor(vertex.getX()) + (Math.sqrt(3) / 6 - 1) * shift;
				double y = vCoor(vertex.getY()) - shift / 2;
				double width = (graph.getGranularity() - Math.sqrt(3) * (graph.getGranularity() - 2. / 3) / 2 + 0.5) * shift;
				double height = shift;
				drawHexagonalModelRectangleEntry(x, y, width, height, vertex.getID(), vertex.getType().getColor());
				return;
			}
			case ENTRY0, EXIT0 -> {
				double x0 = vCoor(vertex.getX()) + shift * (1. / 2 - 1 / Math.sqrt(3));
				double y0 = vCoor(vertex.getY()) + Math.sqrt(3) * shift / 2;
				double x1 = vCoor(vertex.getX()) + shift * (1 + Math.sqrt(3) / 3) / 2;
				double y1 = vCoor(vertex.getY()) + (Math.sqrt(3) - 1) * shift / 2;
				double x2 = Math.max(x1 - Math.sqrt(3) * y1 / 3, 0);
				double y2 = x2 == 0 ? y1 - Math.sqrt(3) * x1 : 0;
				double x3 = Math.max(x0 - Math.sqrt(3) * y0 / 3, 0);
				double y3 = x3 == 0 ? y0 - Math.sqrt(3) * x0 : 0;
				if (x3 == 0 && y2 == 0) {
					drawHexagonalModelObliqueEntries(vertex, x0, y0, x1, y1, x2, y2, 0, 0, x3, y3);
				} else {
					drawHexagonalModelObliqueEntries(vertex, x0, y0, x1, y1, x2, y2, x3, y3);
				}
			}
			case ENTRY1, EXIT1 -> {
				double x0 = vCoor(vertex.getX()) - shift * (1 + Math.sqrt(3) / 3) / 2;
				double y0 = vCoor(vertex.getY()) + (Math.sqrt(3) - 1) * shift / 2;
				double x1 = vCoor(vertex.getX()) + shift * (1 / Math.sqrt(3) - 1. / 2);
				double y1 = vCoor(vertex.getY()) + Math.sqrt(3) * shift / 2;
				double x2 = Math.min(x1 + Math.sqrt(3) * y1 / 3, preferredHeight);
				double y2 = x2 == preferredHeight ? y1 - Math.sqrt(3) * (preferredHeight - x1) : 0;
				double x3 = Math.min(x0 + Math.sqrt(3) * y0 / 3, preferredHeight);
				double y3 = x3 == preferredHeight ? y0 - Math.sqrt(3) * (preferredHeight - x0) : 0;
				if (x2 == preferredHeight && y3 == 0) {
					drawHexagonalModelObliqueEntries(vertex, x0, y0, x1, y1, x2, y2, preferredHeight, 0, x3, y3);
				} else {
					drawHexagonalModelObliqueEntries(vertex, x0, y0, x1, y1, x2, y2, x3, y3);
				}
			}
		}
		addTextField(vCoor(vertex.getX()) - shift / 2, vCoor(vertex.getY()) - shift / 2, shift, shift, String.valueOf(vertex.getID()));
	}

	/**
	 * Create two quadrilaterals or pentagons symbolizing opposite oblique entries / exits for hexagonal model.
	 * If the entry is over frame corner, it adds fifth point.
	 *
	 * @param color  TODO
	 * @param points Polygon points location in format x0, y0, x1, y1, ...
	 * @return
	 */
	private Polygon drawHexagonalModelObliqueEntry(Color color, double... points) {
		// create associated entry
		Polygon polygon = new Polygon(
						points
		);
		polygon.setStroke(STROKE_COLOR);
		polygon.setFill(color);
		nodes.add(polygon);
		return polygon;
	}

	/**
	 * TODO
	 *
	 * @param vertex
	 * @param points
	 */
	private void drawHexagonalModelObliqueEntries(GraphicalVertex vertex, double... points) {
		Color color = vertex.getType().getColor();
		int id = vertex.getID();

		Polygon polygon = drawHexagonalModelObliqueEntry(color, points);
		vertexNodes.put(id, polygon);

		for (int i = 0; i < points.length; i++) {
			points[i] = preferredHeight - points[i];
		}
		id += 3;

		polygon = drawHexagonalModelObliqueEntry(color, points);
		vertexNodes.put(id, polygon);
	}

	/**
	 * Create rectangular entry / exit vertex for octagonal model.
	 *
	 * @param size      "Width" of the road
	 * @param x         Coordinate X of the entry / exit in graph
	 * @param y         Coordinate Y of the entry / exit in graph
	 * @param id        The id to be displayed inside the entry / exit
	 * @param color     Color of the rectangle
	 * @param direction Direction of the entry / exit. (See Vertex object for more details)
	 */
	private void drawOctagonalEntry(double size, double x, double y, int id, Color color, int direction) {
		assert (direction < 4);
		// create rectangle
		Rectangle rectangle = new Rectangle();
		rectangle.setFill(color);
		rectangle.setStroke(STROKE_COLOR);

		// compute location and size
		double halfSize = size / 2;
		double xLocation = x - (direction == 1 ? (2 - OCTAGON_RATIO) * halfSize : halfSize);
		double yLocation = y - (direction == 2 ? (2 - OCTAGON_RATIO) * halfSize : halfSize);
		double width = direction % 2 == 0 ? size : (3 - OCTAGON_RATIO) * halfSize;
		double height = direction % 2 == 0 ? (3 - OCTAGON_RATIO) * halfSize : size;

		// set rectangle location and size
		rectangle.setX(xLocation);
		rectangle.setY(yLocation);
		rectangle.setWidth(width);
		rectangle.setHeight(height);
		nodes.add(rectangle);
		vertexNodes.put(id, rectangle);

		// add id
		addTextField(xLocation, yLocation, width, height, String.valueOf(id));
	}

	/**
	 * Adds rectangular vertex id field.
	 *
	 * @param x      Coordinate X of the id field
	 * @param y      Coordinate Y of the id field
	 * @param width  Width of the id field
	 * @param height Height of the id field
	 * @param id     Text of the id field
	 */
	private void addTextField(double x, double y, double width, double height, String id) {
		Label vertexLabel = new Label(id);
		vertexLabel.setBackground(Background.EMPTY);
		vertexLabel.setLayoutX(x);
		vertexLabel.setLayoutY(y);
		vertexLabel.setPrefWidth(width);
		vertexLabel.setPrefHeight(height);
		vertexLabel.setAlignment(Pos.CENTER);
		nodes.add(vertexLabel);
	}

	/*
	 * TODO
	 */
	private double vCoor(double coordination) {
		return coordination * preferredHeight;
	}

	/**
	 * Redraw graph based on selected properties in menu window.
	 * If the graph was not changed, do nothing.
	 */
	public void redraw() {
		redraw(false);
	}

	/**
	 * Redraw graph based on selected properties in menu window.
	 *
	 * @param ignoreOld If redraw is forced, ignoring fact old graph is identical to the new one
	 */
	public void redraw(boolean ignoreOld) {
		// get graph properties
		int granularity = IntersectionMenuTab0.getGranularity().getValue();
		int entries = IntersectionMenuTab0.getEntries().getValue();
		int exits = IntersectionMenuTab0.getExits().getValue();
		IntersectionMenuTab0.Parameters.Models model = IntersectionMenuTab0.getModel();

		// create graph with key properties set
		SimulationGraph graphAbstract = new AbstractGraph(model, granularity, entries, exits, false);

		if (!ignoreOld && graphAbstract.equals(graph)) { // if last graph is the same, return
			return;
		}

		if (graph != null && !graphAbstract.equals(graph)) { // else add graph to previous stack
			historyPrevious.push(graph);
		}

		if (createdGraphs.containsKey(graphAbstract)) { // if graph already exists and model objects already exist
			setGraphFromAbstract(graphAbstract);
			IntersectionMenu.pauseSimulation();
		} else { // create new graph and all the model nodes
			IntersectionScene.resetSimulation();
			historyNext.clear();
			nodes = new ArrayList<>();

			switch (model) {
				case SQUARE -> graph = new SquareGraph(granularity, entries, exits);
				case HEXAGONAL -> graph = new HexagonalGraph(granularity, entries, exits);
				case OCTAGONAL -> graph = new OctagonalGraph(granularity, entries, exits);
				case CUSTOM -> {
				}
			}

			createGraphNodes();

			createdGraphs.put(graph, graph);
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
		graph = createdGraphs.get(graphAbstract);
		nodes.clear();
		vertexNodes.clear();
		createGraphNodes();
		getChildren().setAll(nodes);
	}

	/**
	 * Create graphical nodes from graph.
	 */
	private void createGraphNodes() {
		final long granularity = graph.getGranularity();
		switch (graph.getModel()) {
			case SQUARE -> drawSquareModel(granularity);
			case HEXAGONAL -> drawHexagonalModel(granularity);
			case OCTAGONAL -> drawOctagonalModel(granularity);
			default -> throw new IllegalStateException("Unexpected value: " + graph.getModel()); // TODO
		}
	}

	/**
	 * Draw last saved different model.
	 */
	public void drawPreviousGraph() {
		if (!historyPrevious.isEmpty()) {
			SimulationGraph graphAbstract = historyPrevious.pop();
			if (graph != null) {
				historyNext.push(graph);
			}
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
			if (graph != null) {
				historyPrevious.push(graph);
			}
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
		backgroundSquare.setFill(BACKGROUND_COLOR);

		getChildren().add(backgroundSquare);
		backgroundSquare.toBack();
	}

	/**
	 * TODO
	 *
	 * @param verticesUsage
	 * @param frames
	 */
	public static void updateVertexNodesColors(long[] verticesUsage, double frames) {
		for (int vertexID = 0; vertexID < verticesUsage.length; vertexID++) {
			Color color = graph.getVertex(vertexID).getType().getColor();

			Shape vertexNode = vertexNodes.get(vertexID);
			double oldColorShift = ((Color) vertexNode.getFill()).getGreen() / color.getGreen();
			double colorShift = 1 - verticesUsage[vertexID] / frames;
			if (Math.abs(oldColorShift - colorShift) > MAX_COLOR_CHANGE) {
				colorShift = oldColorShift + (colorShift > oldColorShift ? MAX_COLOR_CHANGE : -MAX_COLOR_CHANGE);
			}

			colorShift = Math.max(0, colorShift);

			vertexNode.setFill(Color.color(color.getRed() * colorShift, color.getGreen() * colorShift, color.getBlue() * colorShift));
		}
	}

	public static void resetVertexNodesColors() {
		for (Map.Entry<Integer, Shape> vertexNode : vertexNodes.entrySet()) {
			Color color = graph.getVertex(vertexNode.getKey()).getType().getColor();
			vertexNode.getValue().setFill(color);
		}
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
