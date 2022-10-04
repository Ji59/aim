package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.InvalidSimulation;
import cz.cuni.mff.kotal.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import cz.cuni.mff.kotal.simulation.timer.SimulationAnimationTimer;
import cz.cuni.mff.kotal.simulation.timer.SimulationBackgroundTicker;
import cz.cuni.mff.kotal.simulation.timer.SimulationTicker;
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
import org.jetbrains.annotations.TestOnly;

import java.util.*;


/**
 * Pane with agent rectangles.
 */
public class SimulationAgents extends Pane {
	public static final int LABEL_MOUSE_OFFSET = 12;
	private static final Map<Long, AgentPane> activeAgents = new HashMap<>();
	private final PriorityQueue<Agent> arrivingAgents = new PriorityQueue<>(Comparator.comparingDouble(Agent::getPlannedTime));
	private final Label vertexLabel = new Label();
	private final Label agentLabel = new Label();
	@TestOnly
	private final Set<Polygon> rectangles = new HashSet<>(); // only for debug
	private Simulation simulation = new InvalidSimulation();
	private double cellSize; // FIXME move somewhere else
	private boolean vertexLabelUpdateRunning = false;
	private int vertexLabelID;
	private Set<Node> agentPath = null;
	private SimulationTicker timer;

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
		setOnMouseExited(event -> {
			vertexLabel.setVisible(false);
			agentLabel.setVisible(false);
		});
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
	 *
	 * @param event
	 */
	private void onMouseMoveAction(MouseEvent event) {
		double size = IntersectionModel.getPreferredHeight();

		double x = event.getX() / size;
		double y = event.getY() / size;

		SimulationGraph graph = IntersectionModel.getGraph();
		double cellSize = graph.getCellSize();

		if (!simulation.isRunning() && coordinatesOverAgent(event, x, y, graph, cellSize)) {
			return;
		}
		resetAgentPath();

		double precision = cellSize * IntersectionModel.VERTEX_RATIO; // TODO extract constant

		for (Vertex v : graph.getVertices()) {
			GraphicalVertex vertex = (GraphicalVertex) v;
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

	/**
	 * TODO
	 */
	public void setVertexLabelText() {
		if (!vertexLabel.isVisible() || vertexLabelUpdateRunning) {
			return;
		}
		vertexLabelUpdateRunning = true;
		Platform.runLater(() -> {
			// FIXME when graph changes, throws exception
			double vertexUsage = SimulationTicker.getVertexUsage(vertexLabelID);
			vertexLabel.setText(String.format("ID: %d%nUsage: %.2f", vertexLabelID, vertexUsage));
			vertexLabelUpdateRunning = false;
		});
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
	 * @param cellSize
	 *
	 * @return
	 */
	private boolean coordinatesOverAgent(MouseEvent event, double x, double y, SimulationGraph graph, double cellSize) {
		double agentSizeScale = cellSize / 2;
		for (AgentPane agentPane : activeAgents.values()) {
			Agent agent = agentPane.getAgent();
			if (MyNumberOperations.distance(x, y, agent.getX(), agent.getY()) <= Math.min(agent.getW(), agent.getL()) * agentSizeScale) {
				agentLabel.setLayoutX(event.getX() + LABEL_MOUSE_OFFSET);
				agentLabel.setLayoutY(event.getY() + LABEL_MOUSE_OFFSET);

				if (agentPath == null) {
					SimulationAgents.this.setAgentLabelText(agent);
					agentLabel.setVisible(true);
					vertexLabel.setVisible(false);
					createAgentPath(graph, agentPane, agent);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * TODO
	 */
	public void setAgentLabelText(Agent agent) {
		int exit = agent.getPath().get(agent.getPath().size() - 1);
		agentLabel.setText(String.format("ID: %d%nArrival: %,.2f%nPlanned: %d%nEntry: %d%nExit: %d", agent.getId(), agent.getArrivalTime(), agent.getPlannedTime(), agent.getEntry(), exit));
	}

	/**
	 * TODO
	 *
	 * @param graph
	 * @param agentPane
	 * @param agent
	 */
	private void createAgentPath(SimulationGraph graph, AgentPane agentPane, Agent agent) {
		double size = IntersectionModel.getPreferredHeight();
		List<Integer> path = agent.getPath();
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
		line.toFront();
		line.setStroke(agentPane.getColor());
		line.setStrokeLineCap(StrokeLineCap.ROUND);
	}

	/**
	 * TODO
	 *
	 * @return
	 */
	public static Map<Long, AgentPane> getActiveAgents() {
		return activeAgents;
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
		AgentPane agentPane = new AgentPane(startTime, 0, agent, period, simulation.getIntersectionGraph().getVertices(), cellSize);

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

	private AgentPane initAgentPane(Agent agent, double step) {
		AgentPane agentPane = new AgentPane(step, agent, simulation.getIntersectionGraph().getVertices(), cellSize);
		double collisionStep = agentPane.getCollisionStep();
		if (collisionStep <= step) {
			if (collisionStep + SimulationTicker.COLLISION_AGENTS_SHOWN_STEPS > step) {
				agentPane.collide();
				agentPane.handleTick(collisionStep);
			} else {
				return null;
			}
		} else {
			agentPane.resetColors();
			agentPane.handleTick(step);
		}

		synchronized (activeAgents) {
			activeAgents.put(agent.getId(), agentPane);
		}

		return agentPane;
	}

	/**
	 * TODO
	 *
	 * @param agentPane
	 */
	private void addPaneToChildren(AgentPane agentPane) {
		Runnable action = () -> {
			getChildren().add(agentPane);
			agentPane.toBack();
		};
		if (Platform.isFxApplicationThread()) {
			action.run();
		} else {
			Platform.runLater(action);
		}
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
	 * TODO
	 *
	 * @param agentPane
	 */
	public void removeAgentPane(AgentPane agentPane) {
		getChildren().remove(agentPane);
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
	 * Stop running agent panes simulation.
	 * TODO
	 */
	public void pauseSimulation() {
		if (timer != null) {
			timer.stop();
		}
	}

	/**
	 * Resume already started simulation.
	 *
	 * @param simulation Simulation to be resumed
	 */
	public void resumeSimulation(Simulation simulation) {
		this.simulation = simulation;

//		synchronized (activeAgents) {
//			activeAgents.values().forEach(agentPane -> agentPane.resume(simulation.getPeriod(), startTime));
//		}
		cellSize = simulation.getIntersectionGraph().getCellSize() * IntersectionModel.getPreferredHeight(); // FIXME refactor

		if (IntersectionMenu.playSimulationInBackground()) {
			timer = new SimulationBackgroundTicker(activeAgents, simulation);
			resetSimulation();
			clearChildrenNodes();
		} else {
			timer = new SimulationAnimationTimer(activeAgents, simulation);
		}

		simulation.getStateLock().lock();
		if (simulation.isRunning()) {
			timer.start();
		}
		simulation.getStateLock().unlock();
	}

	/**
	 * TODO
	 *
	 * @param simulation Simulation to be resumed
	 */
	@Deprecated
	public void resumeSimulationWithAgents(Simulation simulation, long startTime, Collection<Agent> agents) {
		setAgents(agents, simulation.getStep(startTime));
		resumeSimulation(simulation);
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
	}

	/**
	 * TODO
	 */
	private void clearActiveAgents() {
		if (timer != null) {
			timer.stop();
		}
		clearChildrenNodes();
		activeAgents.clear();
		synchronized (arrivingAgents) {
			arrivingAgents.clear();
		}
	}

	private void clearChildrenNodes() {
		Runnable guiReset = () -> getChildren().setAll(vertexLabel, agentLabel);
		if (Platform.isFxApplicationThread()) {
			guiReset.run();
		} else {
			Platform.runLater(guiReset);
		}
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	public void addArrivedAgents(double step) {
		addArrivedAgents(step, true);
	}

	/**
	 * TODO
	 *
	 * @param step
	 */
	public void addArrivedAgents(double step, boolean showAgent) {
		synchronized (arrivingAgents) {
			Iterator<Agent> iterator = arrivingAgents.iterator();
			while (iterator.hasNext()) {
				Agent agent = iterator.next();
				if (agent.getPlannedTime() > step) {
					return;
				}
				if (agent.getPlannedTime() + agent.getPath().size() > step + 1) {
					AgentPane agentPane = initAgentPane(agent, step);
					if (agentPane != null && showAgent) {
						addPaneToChildren(agentPane);
					}
				}
				iterator.remove();
			}
		}
	}

	public boolean emptyArrivingAgents() {
		boolean emptyArrivingAgents;
		synchronized (arrivingAgents) {
			emptyArrivingAgents = arrivingAgents.isEmpty();
		}
		return emptyArrivingAgents;
	}

	/**
	 * Only for debug purposes
	 */
	@TestOnly
	public void resetRectangles() {
		getChildren().removeAll(rectangles);
		rectangles.clear();
	}

	/**
	 * Only for debug purposes
	 */
	@TestOnly
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
	@TestOnly
	public void addRectangle(double[] points, int red) {
		Polygon rectangle = new Polygon(points);
		rectangle.setFill(Color.rgb(red, 0, 0, 0.3));
		rectangle.toBack();
		rectangles.add(rectangle);
		getChildren().add(rectangle);
	}

	/**
	 * Only for debug purposes
	 */
	@TestOnly
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
	 * TODO
	 *
	 * @return
	 */
	public Simulation getSimulation() {
		return simulation;
	}

	/**
	 * Set actually using simulation.
	 *
	 * @param simulation Running simulation
	 */
	public void setSimulation(Simulation simulation) {
		this.simulation = simulation;
	}
}