package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static cz.cuni.mff.kotal.helpers.MyGenerator.generateRandomInt;


/**
 * Class containing all agent gui elements.
 */
public class AgentPane extends StackPane {

	private final Map<Long, Vertex> simulationVertices;
	private final Rectangle rectangle;
	private final Rotate rotation = new Rotate();
	private final Agent agent;
	private double distanceTraveled;

	private double angle;

	// TODO extract to timer
	private long startTime;
	private double period; // period in nanoseconds
	private double relativeDistanceTraveled; // TODO is this necessary?

	/**
	 * Create new agent pane and all GUI elements, set position to starting state.
	 *
	 * @param startTime          System time, when the agent was created
	 * @param agent              Agent tied to this pane
	 * @param period             Delay between steps
	 * @param simulationVertices Intersection vertices the agent is travelling on
	 */
	public AgentPane(long startTime, Agent agent, double period, Map<Long, Vertex> simulationVertices) {
		this.agent = agent;
		this.distanceTraveled = 0;
		this.startTime = startTime;
		this.period = period * 1_000_000;
		this.simulationVertices = simulationVertices;

		// Create rectangle representing agent
		rectangle = new Rectangle(agent.getW(), agent.getL());

		// TODO change to label
		// Create ID text field
		TextField text = new TextField(String.valueOf(agent.getId()));
		text.setAlignment(Pos.CENTER);
		text.setBackground(Background.EMPTY);

		// TODO set rotation based on the arriving location
		// Add rotation parameters
		rotation.setPivotX(rectangle.getWidth() / 2);
		rotation.setPivotY(rectangle.getHeight() / 2);
		rectangle.getTransforms().add(rotation);


		// TODO set color properly
		rectangle.setFill(Color.rgb(generateRandomInt(255), generateRandomInt(255), generateRandomInt(255)));

		// TODO set proper agent size
		setPrefWidth(agent.getW());
		setPrefHeight(agent.getL());

		getChildren().addAll(rectangle, text);

		updateAgent(0);
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
		setLayoutX(agent.getX() - (getWidth() == 0 ? agent.getW() / 2 : getWidth() / 2));
		setLayoutY(agent.getY() - (getHeight() == 0 ? agent.getL() / 2 : getHeight() / 2));
	}

	/**
	 * Update rotation of the agent at given time.
	 *
	 * @param time System time used in computation
	 */
	public void updateRotation(double time) {
		Map<Long, Vertex> vertices = simulationVertices;
		Pair<Long, Long> previousNext = agent.getPreviousNextVertexIDs(time);
		GraphicalVertex start = (GraphicalVertex) vertices.get(previousNext.getKey());
		GraphicalVertex end = (GraphicalVertex) vertices.get(previousNext.getValue());

		double newAngle = MyNumberOperations.computeRotation(start.getX(), start.getY(), end.getX(), end.getY());
		if (newAngle > 0 && newAngle != angle) {
			angle = newAngle;
			rotation.setAngle(angle);
		}
	}

	/**
	 * Update rotation and position of the pane.
	 *
	 * @param time System time of the update
	 */
	public void updateAgent(double time) {
		updateRotation(time);
		updatePosition();
	}

	/**
	 * @param time Given system time
	 * @return number of steps already traveled, could be rational number
	 */
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
	public boolean handleTick(long now) {
		double time = relativeDistanceTraveled + getRelativeTimeTraveled(now);
		try {
			getAgent().computeNextXY(time, simulationVertices);
		} catch (IndexOutOfBoundsException e) {
			// TODO
//				removeAgent(agent.getId());
			return true;
		}
		updateAgent(time);
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

		double halfWidth = agent.getW() / 2;
		double halfHeight = agent.getL() / 2;

		double sx = getLayoutX() + getWidth() / 2;
		double sy = getLayoutY() + getHeight() / 2;
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
		double[] corners = new double[4];
		Bounds boundingBox = getBoundsInParent();
		corners[0] = boundingBox.getMinX();
		corners[1] = boundingBox.getMinY();
		corners[2] = boundingBox.getMaxX();
		corners[3] = boundingBox.getMaxY();
		return corners;
	}

	/**
	 * Compute agent state at pause time and save it to parameters.
	 *
	 * @param now System time at pause
	 */
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
	public void resume(double period, long now) {
		// TODO refactor
//			assert timer == null;
//			long now = System.nanoTime();
//			timer = new AgentTimer(now, period, distanceTraveled, this);
		this.period = period * 1_000_000;
		this.relativeDistanceTraveled = distanceTraveled;
		this.startTime = now;
//			timer.start();
	}

	/**
	 * Set node parameters to fit collided agent state.
	 */
	public void collide() {
		// TODO refactor
//			this.stop();
		rectangle.toBack();
		rectangle.setFill(Color.RED);
		rectangle.setOpacity(0.5);
		this.setDisable(true); // TODO don't use this
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

	/**
	 * Set width of this pane.
	 * Use only in tests.
	 *
	 * @param width New width of the pane
	 */
	@Override
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
	public void setHeight(double height) {
		super.setHeight(height);
	}
}
