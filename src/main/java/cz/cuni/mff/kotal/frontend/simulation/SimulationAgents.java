package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.simulation.timer.SimulationTimer;
import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.InvalidSimulation;
import cz.cuni.mff.kotal.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.TextAlignment;

import java.util.*;


/**
 * Pane with agent rectangles.
 */
public class SimulationAgents extends Pane {
	public static final int LABEL_MOUSE_OFFSET = 12;
	private Simulation simulation = new InvalidSimulation();
	private final Map<Long, AgentPane> activeAgents = new HashMap<>();
	private final Map<Agent, AgentPane> allAgents = new HashMap<>();
	private final PriorityQueue<Agent> arrivingAgents = new PriorityQueue<>(Comparator.comparingDouble(Agent::getPlannedTime));
	private double cellSize; // FIXME move somewhere else

	private final Label vertexLabel = new Label();
	private long vertexLabelID;
	private final Label agentLabel = new Label();
	private Set<Node> agentPath = null;

	private SimulationTimer timer;

	private final Set<Polygon> rectangles = new HashSet<>(); // only for debug

	/**
	 * Create new pane with specified parameters.
	 *
	 * @param height     Width and height of the pane
	 * @param simulation Simulation associated with this pane
	 */
	public SimulationAgents(double height, Simulation simulation) {
		this.simulation = simulation;
		setPrefWidth(height);
		setPrefHeight(height);
	}

	/**
	 * Create new pane with specified size.
	 *
	 * @param height Width and height of the pane
	 */
	public SimulationAgents(double height) {
		setPrefWidth(height);
		setPrefHeight(height);

		setLabelParameters(vertexLabel);
		setLabelParameters(agentLabel);

		setOnMouseMoved(this::onMouseMoveAction);
	}

	private void setLabelParameters(Label vertexLabel) {
		vertexLabel.setTextAlignment(TextAlignment.CENTER);
		vertexLabel.setMouseTransparent(true);
		vertexLabel.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(4), new Insets(-4))));
		vertexLabel.setVisible(false);
		vertexLabel.toFront();
		getChildren().add(vertexLabel);
	}

	/**
	 * TODO
	 */
	public void setVertexLabelText() {
		if (!vertexLabel.isVisible()) {
			return;
		}
		double vertexUsage = SimulationTimer.getVertexUsage((int) vertexLabelID);
		vertexLabel.setText(String.format("ID: %d%nUsage: %.2f", vertexLabelID, vertexUsage));
	}

	/**
	 * TODO
	 */
	public void setAgentLabelText(Agent agent) {
		Long exit = agent.getPath().get(agent.getPath().size() - 1);
		agentLabel.setText(String.format("ID: %d%nArrival: %,.2f%nPlanned: %d%nEntry: %d%nExit: %d", agent.getId(), agent.getArrivalTime(), agent.getPlannedTime(), agent.getEntry(), exit));
	}

	/**
	 * Create and add new agent pane to this simulation.
	 *
	 * @param agent  Agent to be added
	 * @param period Time delay between simulation steps
	 */
	@Deprecated
	public void addAgent(Agent agent, double period) {
		long agentID = agent.getId();
		double cellSize = simulation.getIntersectionGraph().getCellSize() * IntersectionModel.getPreferredHeight(); // FIXME refactor
		long startTime = simulation.getTime(agent.getPlannedTime());
		AgentPane agentPane = new AgentPane(startTime, 0, agent, period, simulation.getIntersectionGraph().getVerticesWithIDs(), cellSize);

		synchronized (activeAgents) {
			activeAgents.put(agentID, agentPane);
		}

		Platform.runLater(() -> {
			if (simulation.isRunning()) {
				getChildren().add(agentPane);
			}
		});
	}

	/**
	 * TODO
	 * Create and add new agent pane to this simulation.
	 *
	 * @param agent Agent to be added
	 */
	public void addAgent(Agent agent) {
		synchronized (arrivingAgents) {
			arrivingAgents.add(agent);
		}
//		addAgent(agent, simulation.getPeriod());
	}

	/**
	 * @param agents
	 */
	public void addAgents(Collection<Agent> agents) {
		synchronized (arrivingAgents) {
			arrivingAgents.addAll(agents);
		}
	}

	/**
	 * TODO
	 *
	 * @param agent
	 * @param step
	 */

	private void addAgentPane(Agent agent, double step) {
		long agentID = agent.getId();
		if (allAgents.containsKey(agent)) {
			AgentPane agentPane = allAgents.get(agent);
			double collisionStep = agentPane.getCollisionStep();
			if (collisionStep >= 0 && collisionStep <= step) {
				if (collisionStep + SimulationTimer.COLLISION_AGENTS_SHOWN_STEPS > step) {
					agentPane.collide();
					agentPane.handleTick(collisionStep);
				} else {
					return;
				}
			} else {
				agentPane.resetColors();
				agentPane.handleTick(step);
			}

			synchronized (activeAgents) {
				activeAgents.put(agentID, agentPane);
			}
			addPaneToChildren(agentPane);
			return;
		}
		long startTime = simulation.getTime(agent.getPlannedTime());
		AgentPane agentPane = new AgentPane(startTime, step, agent, simulation.getPeriod(), simulation.getIntersectionGraph().getVerticesWithIDs(), cellSize);

		synchronized (activeAgents) {
			activeAgents.put(agentID, agentPane);
		}
		allAgents.put(agent, agentPane);

		addPaneToChildren(agentPane);
	}

	/**
	 * TODO
	 *
	 * @param agentPane
	 */
	private void addPaneToChildren(AgentPane agentPane) {
		getChildren().add(agentPane);
		agentPane.toBack();
	}

	/**
	 * Remove this agent from the simulation GUI.
	 *
	 * @param agentID ID of the agent to be removed
	 */
	public void removeAgent(long agentID) {
		AgentPane agentPane;
		synchronized (activeAgents) {
			agentPane = activeAgents.remove(agentID);
			if (agentPane == null) {
				return;
			}
		}
		removeAgentPane(agentPane);
	}

	/**
	 * Remove this agent from the simulation GUI.
	 *
	 * @param agentEntry Agent with its ID
	 */
	public void removeAgent(Map.Entry<Long, AgentPane> agentEntry) {
		synchronized (activeAgents) {
			activeAgents.remove(agentEntry.getKey());
		}
		AgentPane agentPane = agentEntry.getValue();
		removeAgentPane(agentPane);
	}

	/**
	 * TODO
	 *
	 * @param agentPane
	 */
	public void removeAgentPane(AgentPane agentPane) {
		getChildren().remove(agentPane);
	}

	/**
	 * Set actually using simulation.
	 *
	 * @param simulation Running simulation
	 */
	public void setSimulation(Simulation simulation) {
		this.simulation = simulation;
	}

	/**
	 * Stop running agent panes simulation.
	 */
	public void pauseSimulation() {
		timer.stop();
	}

	/**
	 * Resume already started simulation.
	 *
	 * @param simulation Simulation to be resumed
	 */
	public void resumeSimulation(Simulation simulation, long startTime) {
		this.simulation = simulation;

		synchronized (activeAgents) {
			activeAgents.values().forEach(agentPane -> agentPane.resume(simulation.getPeriod(), startTime));
		}
		cellSize = simulation.getIntersectionGraph().getCellSize() * IntersectionModel.getPreferredHeight(); // FIXME refactor
		timer = new SimulationTimer(activeAgents, this);
		timer.start();
	}

	/**
	 * TODO
	 *
	 * @param simulation Simulation to be resumed
	 */
	@Deprecated
	public void resumeSimulationWithAgents(Simulation simulation, long startTime, Collection<Agent> agents) {
		setAgents(agents, simulation.getStep(startTime));
		resumeSimulation(simulation, startTime);
	}

	/**
	 * TODO
	 *
	 * @param agents
	 * @param step
	 */
	public void setAgents(Collection<Agent> agents, double step) {
		clearActiveAgents();
		addAgents(agents);
		addArrivedAgents(step);
	}

	/**
	 * Restart whole pane to starting state.
	 */
	public void resetSimulation() {
		clearActiveAgents();
		allAgents.clear();
	}

	/**
	 * TODO
	 */
	private void clearActiveAgents() {
		timer.stop();
		getChildren().setAll(vertexLabel, agentLabel);
		activeAgents.clear();
		arrivingAgents.clear();
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	public void addArrivedAgents(double step) {
		synchronized (arrivingAgents) {
			Iterator<Agent> iterator = arrivingAgents.iterator();
			while (iterator.hasNext()) {
				Agent agent = iterator.next();
				if (agent.getPlannedTime() > step) {
					return;
				}
				if (agent.getPlannedTime() + agent.getPath().size() > step + 1) {
					addAgentPane(agent, step);
				}
				iterator.remove();
			}
		}
	}

	public PriorityQueue<Agent> getArrivingAgents() {
		return arrivingAgents;
	}

	/**
	 * Only for debug purposes
	 */
	public void resetRectangles() {
		getChildren().removeAll(rectangles);
		rectangles.clear();
	}

	/**
	 * Only for debug purposes
	 */
	public void addRectangle(List<Point> cornerPoints) {
		double[] points = new double[cornerPoints.size() * 2];
		for (int i = 0; i < cornerPoints.size(); i++) {
			points[2 * i] = cornerPoints.get(i).getX();
			points[2 * i + 1] = cornerPoints.get(i).getY();
		}
		addRectangle(points, 0);
	}

	/**
	 * Only for debug purposes
	 */
	public void addRectangle(double[] boundingBox) {
		double[] points = new double[boundingBox.length * 2];
		points[0] = boundingBox[0];
		points[1] = boundingBox[1];
		points[2] = boundingBox[2];
		points[3] = boundingBox[1];
		points[4] = boundingBox[2];
		points[5] = boundingBox[3];
		points[6] = boundingBox[0];
		points[7] = boundingBox[3];
		addRectangle(points, 255);
	}

	/**
	 * Only for debug purposes
	 */
	public void addRectangle(double[] points, int red) {
		Polygon rectangle = new Polygon(points);
		rectangle.setFill(Color.rgb(red, 0, 0, 0.3));
		rectangle.toBack();
		rectangles.add(rectangle);
		getChildren().add(rectangle);
	}

	public Simulation getSimulation() {
		return simulation;
	}

	/**
	 * TODO
	 *
	 * @param event
	 */
	private void onMouseMoveAction(MouseEvent event) {
		double x = event.getX();
		double y = event.getY();

		SimulationGraph graph = IntersectionModel.getGraph();
		double size = IntersectionModel.getPreferredHeight();
		double cellSize = graph.getCellSize();

		if (!simulation.isRunning() && coordinatesOverAgent(x, y, graph, size, cellSize)) {
			return;
		}
		resetAgentPath();

		x /= size;
		y /= size;

		double precision = cellSize * IntersectionModel.VERTEX_RATIO; // TODO extract constant

		for (GraphicalVertex vertex : graph.getVertices()) {
			if (MyNumberOperations.distance(x, y, vertex.getX(), vertex.getY()) <= precision) {
				vertexLabelID = vertex.getID();
				updateVertexLabelProperties(event);
				return;
			}
		}
		vertexLabel.setVisible(false);
	}

	/**
	 * TODO
	 *
	 * @param event
	 */
	private void updateVertexLabelProperties(MouseEvent event) {
		setVertexLabelText();
		vertexLabel.setLayoutX(event.getX() + 12);
		vertexLabel.setLayoutY(event.getY() + 12);
		vertexLabel.setVisible(true);
	}

	private void resetAgentPath() {
		if (agentPath != null) {
			agentLabel.setVisible(false);
			getChildren().removeAll(agentPath);
			agentPath = null;
		}
	}

	/**
	 * TODO
	 *
	 * @param x
	 * @param y
	 * @param graph
	 * @param size
	 * @param cellSize
	 * @return
	 */
	private boolean coordinatesOverAgent(double x, double y, SimulationGraph graph, double size, double cellSize) {
		double agentSizeScale = cellSize * size / 2;
		for (AgentPane agentPane : activeAgents.values()) {
			Agent agent = agentPane.getAgent();
			if (MyNumberOperations.distance(x, y, agent.getX(), agent.getY()) <= Math.min(agent.getW(), agent.getL()) * agentSizeScale) {
				agentLabel.setLayoutX(x + LABEL_MOUSE_OFFSET);
				agentLabel.setLayoutY(y + LABEL_MOUSE_OFFSET);

				if (agentPath == null) {
					SimulationAgents.this.setAgentLabelText(agent);
					agentLabel.setVisible(true);
					vertexLabel.setVisible(false);
					createAgentPath(graph, size, agentPane, agent);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * TODO
	 *
	 * @param graph
	 * @param size
	 * @param agentPane
	 * @param agent
	 */
	private void createAgentPath(SimulationGraph graph, double size, AgentPane agentPane, Agent agent) {
		List<Long> path = agent.getPath();
		agentPath = new HashSet<>(path.size());
		for (int i = 1; i < path.size(); i++) {
			GraphicalVertex lastVertex = graph.getVertex(path.get(i - 1));
			GraphicalVertex nextVertex = graph.getVertex(path.get(i));
			Line line = new Line(lastVertex.getX() * size, lastVertex.getY() * size, nextVertex.getX() * size, nextVertex.getY() * size);
			getChildren().add(line);
			setPathLineProperties(agentPane, line);
			agentPath.add(line);
		}
	}

	/**
	 * TODO
	 *
	 * @param agentPane
	 * @param line
	 */
	private void setPathLineProperties(AgentPane agentPane, Line line) {
		line.toBack();
		line.setStroke(agentPane.getColor());
		line.setStrokeLineCap(StrokeLineCap.ROUND);
	}
}