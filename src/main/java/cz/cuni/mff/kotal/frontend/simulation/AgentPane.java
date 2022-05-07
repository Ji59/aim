package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static cz.cuni.mff.kotal.helpers.MyGenerator.generateRandomInt;


/**
 * Class containing all agent gui elements.
 */
public class AgentPane extends StackPane {

	public final Color color = Color.rgb(generateRandomInt(255), generateRandomInt(255), generateRandomInt(255));
	private final Vertex[] simulationVertices;  // FIXME remove
	private final Rectangle rectangle;
	private final Rotate rotation = new Rotate();
	private final Agent agent;
	@Deprecated
	private double distanceTraveled;

	private double angle;

	// TODO extract to timer
	@Deprecated
	private long startTime;
	@Deprecated
	private double period; // period in nanoseconds
	@Deprecated
	private double relativeDistanceTraveled; // TODO is this necessary?

	/**
	 * Create new agent pane and all GUI elements, set position to starting state.
	 *
	 * @param startTime          System time, when the agent was created
	 * @param agent              Agent tied to this pane
	 * @param period             Delay between steps
	 * @param simulationVertices Intersection vertices the agent is travelling on
	 */
	@Deprecated
	public AgentPane(long startTime, double firstStep, Agent agent, double period, Vertex[] simulationVertices, double cellSize) {
		this.agent = agent;
		this.distanceTraveled = 0;
		this.startTime = startTime;
		this.period = period;
		this.simulationVertices = simulationVertices;

		this.rectangle = createNodes(agent, cellSize);

		boolean invalidAgent = handleTick(firstStep);
		if (invalidAgent) {  // TODO
			throw new RuntimeException("Creating agent pane failed.");
		}
	}

	public AgentPane(double firstStep, Agent agent, Vertex[] simulationVertices, double cellSize) {
		this.agent = agent;
		this.simulationVertices = simulationVertices;

		this.rectangle = createNodes(agent, cellSize);

		boolean invalidAgent = handleTick(firstStep);
		if (invalidAgent) {  // TODO
			throw new RuntimeException("Creating agent pane failed.");
		}
	}

	@NotNull
	private Rectangle createNodes(Agent agent, double cellSize) {
		// Create rectangle representing agent
		double width = agent.getW() * cellSize;
		double height = agent.getL() * cellSize;
		final Rectangle rectangle = new Rectangle(width, height);

		// Create ID label
		Label label = new Label(String.valueOf(agent.getId()));

		// TODO set rotation based on the arriving location
		// Add rotation parameters
		rotation.setPivotX(width / 2);
		rotation.setPivotY(height / 2);
		rectangle.getTransforms().add(rotation);


		// TODO set color properly
		rectangle.setFill(color);

		// TODO set proper agent size
		setPrefWidth(width);
		setPrefHeight(height);

		getChildren().addAll(rectangle, label);

		setMouseTransparent(true);

		return rectangle;
	}

	/**
	 * Create agent used for tests.
	 *
	 * @param agent Agent the pane is based on
	 */
	AgentPane(Agent agent) {
		this.agent = agent;

		rectangle = new Rectangle(agent.getW(), agent.getL());

		rotation.setPivotX(rectangle.getWidth() / 2);
		rotation.setPivotY(rectangle.getHeight() / 2);
		rectangle.getTransforms().add(rotation);

		getChildren().add(rectangle);
		simulationVertices = null;
	}

	/**
	 * Update coordinates of the pane to fit new agent state.
	 */
	private void updatePosition() {
		// TODO do position properly
//			setLayoutX(agent.getX() - agent.getL() / 2);
//			setLayoutY(agent.getY() - agent.getW() / 2);
		// TODO if agents too small the box is spawned at wrong position
		setLayoutX(agent.getX() * IntersectionModel.getPreferredHeight() - (getWidth() == 0 ? getW() / 2 : getWidth() / 2));
		setLayoutY(agent.getY() * IntersectionModel.getPreferredHeight() - (getHeight() == 0 ? getL() / 2 : getHeight() / 2));
	}

	/**
	 * Update rotation of the agent at given time.
	 *
	 * @param time System time used in computation
	 */
	public void updateRotation(double time) {
		if (computeNewAngle(time)) { // FIXME condition, why angle >= 0?
			rotation.setAngle(angle);
		}
	}

	/**
	 * TODO
	 *
	 * @param time
	 * @return
	 */
	private boolean computeNewAngle(double time) {
		Pair<Integer, Integer> previousNext = agent.getPreviousNextVertexIDs(time);
		GraphicalVertex start = (GraphicalVertex) simulationVertices[previousNext.getKey()];
		GraphicalVertex end = (GraphicalVertex) simulationVertices[previousNext.getValue()];

		double newAngle = MyNumberOperations.computeRotation(start.getX(), start.getY(), end.getX(), end.getY());
		if (newAngle >= 0 && newAngle != angle) {
			angle = newAngle;
			return true;
		}
		return false;
	}

	/**
	 * Update rotation and position of the pane.
	 *
	 * @param time System time of the update FIXME what is time
	 */
	public void updateAgent(double time) {
		updateRotation(time);
		updatePosition();
	}

	/**
	 * @param time Given system time
	 * @return number of steps already traveled, could be rational number
	 */
	@Deprecated
	public double getRelativeTimeTraveled(long time) {
		return (time - startTime) / period;
	}

	/**
	 * Compute X and Y coordinates at given time.
	 * Then update agent node.
	 *
	 * @param now System time of time moment
	 * @return True if agent is out of simulation, otherwise false
	 */
	@Deprecated
	public boolean handleTick(long now) {
		double time = relativeDistanceTraveled + getRelativeTimeTraveled(now);
		boolean finished = getAgent().computeNextXY(time, simulationVertices);
		if (finished) {
			return true;
		}
		updateAgent(time);
		return false;
	}

	/**
	 * TODO
	 *
	 * @param step
	 * @return
	 */
	public boolean handleTick(double step) {
		double time = step - agent.getPlannedTime();
		boolean finished = agent.computeNextXY(time, simulationVertices);
		if (finished) {
			return true;
		}
		updateAgent(time);
		return false;
	}

	/**
	 * TODO
	 *
	 * @param step
	 * @return
	 */
	public boolean handleSimulatedTick(double step) {
		double time = step - agent.getPlannedTime();
		boolean finished = agent.computeNextXY(time, simulationVertices);
		if (finished) {
			return true;
		}
		computeNewAngle(time);
		return false;
	}

	/**
	 * Compute coordinates of agent rectangle corner points.
	 *
	 * @return List of corner points
	 */
	public List<Point> getCornerPoints() {
		double radianAngle = Math.toRadians(angle);
		double sinAngle = Math.sin(radianAngle);
		double cosAngle = Math.cos(radianAngle);

		double halfWidth = agent.getW() / 2 * IntersectionModel.getGraph().getCellSize();
		double halfHeight = agent.getL() / 2 * IntersectionModel.getGraph().getCellSize();

		double sx = agent.getX();
		double sy = agent.getY();
		double middleWidthX = halfWidth * cosAngle;
		double middleWidthY = halfWidth * sinAngle;
		double counterMiddleHeightX = halfHeight * sinAngle;
		double middleHeightY = halfHeight * cosAngle;

		List<Point> vertices = new LinkedList<>();
		vertices.add(new Point(sx - middleWidthX + counterMiddleHeightX, sy - middleWidthY - middleHeightY)); // rotated top left vertex of the rectangle
		vertices.add(new Point(sx + middleWidthX + counterMiddleHeightX, sy + middleWidthY - middleHeightY)); // rotated top right vertex of the rectangle
		vertices.add(new Point(sx + middleWidthX - counterMiddleHeightX, sy + middleWidthY + middleHeightY)); // rotated bottom right vertex of the rectangle
		vertices.add(new Point(sx - middleWidthX - counterMiddleHeightX, sy - middleWidthY + middleHeightY)); // rotated bottom left vertex of the rectangle

		return vertices;
	}

	/**
	 * @return Array containing node bounding box in format [mix_x, min_y, max_x, max_y]
	 */
	public double[] getBoundingBox() {
		double perimeter = agent.getAgentPerimeter();
		double[] corners = new double[4];
		corners[0] = agent.getX() - perimeter;
		corners[1] = agent.getY() - perimeter;
		corners[2] = agent.getX() + perimeter;
		corners[3] = agent.getY() + perimeter;

//		Bounds boundingBox = getBoundsInParent();
//		corners[0] = boundingBox.getMinX();
//		corners[1] = boundingBox.getMinY();
//		corners[2] = boundingBox.getMaxX();
//		corners[3] = boundingBox.getMaxY();
		return corners;
	}

	/**
	 * Compute agent state at pause time and save it to parameters.
	 *
	 * @param now System time at pause
	 */
	@Deprecated
	public void pause(long now) {
//			if (timer != null) {
//				long now = System.nanoTime();
//				timer.stop();
		distanceTraveled = getRelativeTimeTraveled(now);
//				timer = null;
//			}
	}

	/**
	 * Set agent node parameters according to new simulation parameters.
	 *
	 * @param period Delay between steps
	 * @param now    System time of resume
	 */
	@Deprecated
	public void resume(double period, long now) {
		// TODO refactor
//			assert timer == null;
//			long now = System.nanoTime();
//			timer = new AgentTimer(now, period, distanceTraveled, this);
		this.period = period;
		this.relativeDistanceTraveled = distanceTraveled;
		this.startTime = now;
//			timer.start();
	}

	/**
	 * Set node parameters to fit collided agent state.
	 *
	 * @param step TODO
	 */
	public void collide(double step) {
		// TODO refactor
		agent.setCollisionStep(step);
		collidePane();
	}

	public void collide() {
		handleTick(getCollisionStep());
		collidePane();
	}

	private void collidePane() {
		rectangle.toBack();
		rectangle.setFill(Color.RED);
		rectangle.setOpacity(0.5);
	}

	public void resetColors() {
		rectangle.setFill(color);
		rectangle.setOpacity(1);
	}

	/**
	 * @return ID of the agent belonging to this pane
	 */
	public long getAgentID() {
		return agent.getId();
	}

	/**
	 * @return Rotation object angle
	 */
	public double getRotation() {
		return rotation.getAngle();
	}

	/**
	 * @return Agent belonging to this pane
	 */
	public Agent getAgent() {
		return agent;
	}

	/**
	 * @return Rotation attribute value
	 */
	public double getAngle() {
		return angle;
	}

	/**
	 * Set angle rotation of the rectangle.
	 *
	 * @param angle Angle in degrees
	 */
	void setAngle(double angle) {
		this.angle = angle;
	}

	//TODO
	double getL() {
		return rectangle.getHeight();
	}

	//TODO
	double getW() {
		return rectangle.getWidth();
	}

	/**
	 * TODO
	 *
	 * @return
	 */
	public Color getColor() {
		return color;
	}

	public double getCollisionStep() {
		return agent.getCollisionStep();
	}

	/**
	 * Set width of this pane.
	 * Use only in tests.
	 *
	 * @param width New width of the pane
	 */
	@Override
	@TestOnly
	public void setWidth(double width) {
		super.setWidth(width);
	}

	/**
	 * Set width of this pane.
	 * Use only in tests.
	 *
	 * @param height New height of the pane
	 */
	@Override
	@TestOnly
	public void setHeight(double height) {
		super.setHeight(height);
	}
}
