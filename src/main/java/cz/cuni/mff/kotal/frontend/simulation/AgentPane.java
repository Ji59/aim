package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.MyNumberOperations;
import cz.cuni.mff.kotal.frontend.simulation.timer.Point;
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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static cz.cuni.mff.kotal.MyGenerator.generateRandomInt;


public class AgentPane extends StackPane {

	private final Rectangle rectangle;
	private final Rotate rotation = new Rotate();
	private final Agent agent;
	private double distanceTraveled;
	// TODO what is previousAgents
	private final Collection<AgentPane> previousAgents;

	private double angle;

	// TODO extract to timer
	private long startTime;
	private double period; // period in nanoseconds
	private double relativeDistanceTraveled; // TODO is this necessary?

	public AgentPane(long agentID, long startTime, Agent agent, double period, Collection<AgentPane> previousAgents) {
		this.agent = agent;
		this.distanceTraveled = 0;
		this.previousAgents = previousAgents;
		this.startTime = startTime;
		this.period = period * 1_000_000;

		rectangle = new Rectangle(agent.getW(), agent.getL());
		TextField text = new TextField(String.valueOf(agent.getId()));
		text.setAlignment(Pos.CENTER);

		// TODO set rotation based on the arriving location
		rotation.setPivotX(rectangle.getWidth() / 2);
		rotation.setPivotY(rectangle.getHeight() / 2);
		rectangle.getTransforms().add(rotation);

		text.setBackground(Background.EMPTY);

		// TODO set color properly
		rectangle.setFill(Color.rgb(generateRandomInt(255), generateRandomInt(255), generateRandomInt(255)));

		// TODO set proper agent size
		setPrefWidth(agent.getW());
		setPrefHeight(agent.getL());

		getChildren().addAll(rectangle, text);

		updateAgent(0);
	}

	AgentPane(Agent agent) {
		this.agent = agent;

		previousAgents = null;
		rectangle = new Rectangle(agent.getW(), agent.getL());

		rotation.setPivotX(rectangle.getWidth() / 2);
		rotation.setPivotY(rectangle.getHeight() / 2);
		rectangle.getTransforms().add(rotation);

		getChildren().add(rectangle);
	}

	private void updatePosition() {
		// TODO do position properly
//			setLayoutX(agent.getX() - agent.getL() / 2);
//			setLayoutY(agent.getY() - agent.getW() / 2);
		// TODO if agents too small the box is spawned at wrong position
		setLayoutX(agent.getX() - (getWidth() == 0 ? agent.getW() / 2 : getWidth() / 2));
		setLayoutY(agent.getY() - (getHeight() == 0 ? agent.getL() / 2 : getHeight() / 2));
	}

	public void updateRotation(double time) {
		Map<Long, Vertex> vertices = SimulationAgents.getSimulation().getIntersectionGraph().getVerticesWithIDs();
		Pair<Long, Long> previousNext = agent.getPreviousNextVertexIDs(time);
		GraphicalVertex start = (GraphicalVertex) vertices.get(previousNext.getKey());
		GraphicalVertex end = (GraphicalVertex) vertices.get(previousNext.getValue());

		double newAngle = MyNumberOperations.computeRotation(start.getX(), start.getY(), end.getX(), end.getY());
		if (newAngle > 0 && newAngle != angle) {
			angle = newAngle;
			rotation.setAngle(angle);
		}
	}

	public void updateAgent(double time) {
		updateRotation(time);
		updatePosition();
	}

	public double getRelativeTimeTraveled(long time) {
		return (time - startTime) / period;
	}

	public boolean handleTick(long now) {
		double time = relativeDistanceTraveled + getRelativeTimeTraveled(now);
		try {
			getAgent().computeNextXY(time, SimulationAgents.getSimulation().getIntersectionGraph().getVerticesWithIDs());
		} catch (IndexOutOfBoundsException e) {
			// TODO
//				removeAgent(agent.getId());
			return true;
		}
		updateAgent(time);
		return false;
	}

	public List<Point> getCornerPoints() {

		double radianAngle = Math.toRadians(angle);
		double sinAngle = Math.sin(radianAngle),
			cosAngle = Math.cos(radianAngle);

		double halfWidth = agent.getW() / 2,
			halfHeight = agent.getL() / 2;

		double sx = getLayoutX() + getWidth() / 2, sy = getLayoutY() + getHeight() / 2,
			middleWidthX = halfWidth * cosAngle, middleWidthY = halfWidth * sinAngle,
			counterMiddleHeightX = halfHeight * sinAngle, middleHeightY = halfHeight * cosAngle;

		List<Point> vertices = new LinkedList<>();
		vertices.add(new Point(sx - middleWidthX + counterMiddleHeightX, sy - middleWidthY - middleHeightY)); // rotated top left vertex of the rectangle
		vertices.add(new Point(sx + middleWidthX + counterMiddleHeightX, sy + middleWidthY - middleHeightY)); // rotated top right vertex of the rectangle
		vertices.add(new Point(sx + middleWidthX - counterMiddleHeightX, sy + middleWidthY + middleHeightY)); // rotated bottom right vertex of the rectangle
		vertices.add(new Point(sx - middleWidthX - counterMiddleHeightX, sy - middleWidthY + middleHeightY)); // rotated bottom left vertex of the rectangle

		// TODO maybe remove
//			vertices.sort(Comparator.comparingDouble(Pair::getKey));

		return vertices;
	}

	public double[] getBoundingBox() {
		double[] corners = new double[4];
		Bounds boundingBox = getBoundsInParent();
		corners[0] = boundingBox.getMinX();
		corners[1] = boundingBox.getMinY();
		corners[2] = boundingBox.getMaxX();
		corners[3] = boundingBox.getMaxY();
		return corners;
	}

	// TODO remove
	public void pause(long now) {
//			if (timer != null) {
//				long now = System.nanoTime();
//				timer.stop();
		distanceTraveled = getRelativeTimeTraveled(now);
//				timer = null;
//			}
	}

	//
	public void resume(double period, long now) {
//			assert timer == null;
//			long now = System.nanoTime();
//			timer = new AgentTimer(now, period, distanceTraveled, this);
		this.period = period * 1_000_000;
		this.relativeDistanceTraveled = distanceTraveled;
		this.startTime = now;
//			timer.start();
	}

	public void collide() {
		// TODO
//			this.stop();
		this.rectangle.setFill(Color.RED);
		this.setDisable(true); // TODO don't use this
	}

	public long getAgentID() {
		return agent.getId();
	}

	public double getRotation() {
		return rotation.getAngle();
	}

	public Agent getAgent() {
		return agent;
	}

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

	public void setWidth(double width) {
		super.setWidth(width);
	}

	public void setHeight(double height) {
		super.setHeight(height);
	}
}
