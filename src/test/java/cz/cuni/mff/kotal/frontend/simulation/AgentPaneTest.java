package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.simulation.Agent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;


public class AgentPaneTest {

	public static final double LENGTH = 100;
	public static final double HALF_LENGTH = LENGTH / 2;
	public static final double WIDTH = 70;
	public static final Agent AGENT = new Agent(0, 0, 1, 1, 0, LENGTH, WIDTH, 300, 69);
	public static final AgentPane AGENT_PANE = new AgentPane(AGENT);
	public static final double DIAGONAL_ANGLE = Math.atan(WIDTH / LENGTH);
	public static final double HALF_DIAGONAL = Math.sqrt(LENGTH * LENGTH + WIDTH * WIDTH) / 2;
	public static final double HALF_WIDTH = WIDTH / 2;
	public static final int CENTER_X = 200;
	public static final int CENTER_Y = 100;
	public static final double CORNER_X = CENTER_X - HALF_WIDTH;
	public static final double CORNER_Y = CENTER_Y - HALF_LENGTH;

	@Test
	void getCornerPoints() {
		setAgentPaneParameters(AGENT_PANE);

		List<Point> cornerPoints = AGENT_PANE.getCornerPoints();
//		cornerPoints.forEach(point -> System.out.println(point.getX() + "; " + point.getY()));
		assert cornerPoints.contains(new Point(CENTER_X - HALF_WIDTH, CENTER_Y - HALF_LENGTH));
		assert cornerPoints.contains(new Point(CENTER_X + HALF_WIDTH, CENTER_Y - HALF_LENGTH));
		assert cornerPoints.contains(new Point(CENTER_X - HALF_WIDTH, CENTER_Y + HALF_LENGTH));
		assert cornerPoints.contains(new Point(CENTER_X + HALF_WIDTH, CENTER_Y + HALF_LENGTH));

		checkZeroAngle();

		checkCornersAtAngle2(100, 100);
		checkCornersAtAngle2(100, 50);
		checkCornersAtAngle2(70, 100);
		checkCornersAtAngle2(10, 300);
		checkCornersAtAngle2(90, 100);
		checkCornersAtAngle2(1000, 50);
		checkCornersAtAngle2(77, 1000);
		checkCornersAtAngle2(13.5, 42.73);

		checkCornersAtAngle2(100, 100, true);
		checkCornersAtAngle2(100, 50, true);
		checkCornersAtAngle2(70, 100, true);
		checkCornersAtAngle2(10, 300, true);
		checkCornersAtAngle2(90, 100, true);
		checkCornersAtAngle2(1000, 50, true);
		checkCornersAtAngle2(77, 1000, true);
		checkCornersAtAngle2(13.5, 42.73, true);

		checkCornersAtAngle2(100, 100, false, true);
		checkCornersAtAngle2(100, 50, false, true);
		checkCornersAtAngle2(70, 100, false, true);
		checkCornersAtAngle2(10, 300, false, true);
		checkCornersAtAngle2(90, 100, false, true);
		checkCornersAtAngle2(1000, 50, false, true);
		checkCornersAtAngle2(77, 1000, false, true);
		checkCornersAtAngle2(13.5, 42.73, false, true);

		checkCornersAtAngle2(100, 100, true, true);
		checkCornersAtAngle2(100, 50, true, true);
		checkCornersAtAngle2(70, 100, true, true);
		checkCornersAtAngle2(10, 300, true, true);
		checkCornersAtAngle2(90, 100, true, true);
		checkCornersAtAngle2(1000, 50, true, true);
		checkCornersAtAngle2(77, 1000, true, true);
		checkCornersAtAngle2(13.5, 42.73, true, true);
	}

	private void checkZeroAngle() {
		List<Point> cornerPoints;

		cornerPoints = AgentPaneTest.AGENT_PANE.getCornerPoints();
		cornerPoints.forEach(point -> System.out.println(point.getX() + "; " + point.getY()));

		cornerPointsContainPoint(cornerPoints, CENTER_X - WIDTH / 2, CENTER_Y - LENGTH / 2);
		cornerPointsContainPoint(cornerPoints, CENTER_X + WIDTH / 2, CENTER_Y - LENGTH / 2);
		cornerPointsContainPoint(cornerPoints, CENTER_X - WIDTH / 2, CENTER_Y + LENGTH / 2);
		cornerPointsContainPoint(cornerPoints, CENTER_X + WIDTH / 2, CENTER_Y + LENGTH / 2);
	}

	private void checkCornersAtAngle(double newAngle) {
		System.out.println("\nNEW ANGLE: " + Math.toDegrees(newAngle));

		List<Point> cornerPoints;
		AgentPaneTest.AGENT_PANE.setAngle(newAngle);
		cornerPoints = AgentPaneTest.AGENT_PANE.getCornerPoints();
		cornerPoints.forEach(point -> System.out.println(point.getX() + "; " + point.getY()));
		double xDiffAC = Math.sin(Math.PI - newAngle - AgentPaneTest.DIAGONAL_ANGLE) * AgentPaneTest.HALF_DIAGONAL;
		double yDiffAC = Math.cos(Math.PI - newAngle - AgentPaneTest.DIAGONAL_ANGLE) * AgentPaneTest.HALF_DIAGONAL;
		double newAX = AgentPaneTest.CENTER_X - xDiffAC;
		double newAY = AgentPaneTest.CENTER_Y + yDiffAC;

		double newBX = AgentPaneTest.CENTER_X + yDiffAC;
		double newBY = AgentPaneTest.CENTER_Y + xDiffAC;

		double newCX = AgentPaneTest.CENTER_X + xDiffAC;
		double newCY = AgentPaneTest.CENTER_Y - yDiffAC;

		double newDX = AgentPaneTest.CENTER_X - yDiffAC;
		double newDY = AgentPaneTest.CENTER_Y - xDiffAC;

		System.out.println();
		System.out.println(newAX + "; " + newAY);
		System.out.println(newBX + "; " + newBY);
		System.out.println(newCX + "; " + newCY);
		System.out.println(newDX + "; " + newDY);

		double aAB = (newBY - newAY) / (newBX - newAX);
		double aBC = (newBY - newCY) / (newBX - newCX);
		double aCD = (newDY - newCY) / (newDX - newCX);
		double aDA = (newDY - newAY) / (newDX - newAX);

		System.out.println("y = " + aAB + " * x + " + (newAY - aAB * newAX));
		System.out.println("y = " + aBC + " * x + " + (newCY - aBC * newCX));
		System.out.println("y = " + aCD + " * x + " + (newCY - aCD * newCX));
		System.out.println("y = " + aDA + " * x + " + (newAY - aDA * newAX));

		assert cornerPointsContainPoint(cornerPoints, newAX, newAY);
		assert cornerPointsContainPoint(cornerPoints, newBX, newBY);
		assert cornerPointsContainPoint(cornerPoints, newCX, newCY);
		assert cornerPointsContainPoint(cornerPoints, newDX, newDY);
	}

	private void checkCornersAtAngle2(double length, double width) {
		checkCornersAtAngle2(length, width, false, false);
	}

	private void checkCornersAtAngle2(double length, double width, boolean inverted) {
		checkCornersAtAngle2(length, width, inverted, false);
	}

	private void checkCornersAtAngle2(double length, double width, boolean inverted, boolean plus90) {
		Agent agent = new Agent(0, 0, 1, 1, 0, length, width, CENTER_X, CENTER_Y);

		AgentPane agentPane = setAgentPaneParameters(new AgentPane(agent));

		double diagonalAngle = Math.atan(width / length);
		if (inverted) {
			diagonalAngle += Math.PI;
		}
		if (plus90) {
			diagonalAngle += Math.PI / 2;
		}

		double halfDiagonal = Math.sqrt(length * length + width * width) / 2;

		System.out.println("\nANGLE: " + Math.toDegrees(diagonalAngle) + "\n");

		List<Point> cornerPoints;
		cornerPoints = agentPane.getCornerPoints();
		cornerPoints.forEach(point -> System.out.println(point.getX() + "; " + point.getY()));
		System.out.println();

		agentPane.setAngle(Math.toDegrees(diagonalAngle));

		cornerPoints = agentPane.getCornerPoints();
		cornerPoints.forEach(point -> System.out.println(point.getX() + "; " + point.getY()));

		double xShift0 = 0;
		double yShift0 = halfDiagonal;
		double xShift1 = halfDiagonal * Math.sin(2 * diagonalAngle);
		double yShift1 = halfDiagonal * Math.cos(2 * diagonalAngle);

		if (plus90) {
			xShift0 = halfDiagonal;
			yShift0 = 0;

			double temp = xShift1;
			xShift1 = -yShift1;
			yShift1 = temp;
		}

		double newAX = CENTER_X + xShift0;
		double newAY = CENTER_Y + yShift0;

		double newBX = CENTER_X - xShift1;
		double newBY = CENTER_Y + yShift1;

		double newCX = CENTER_X - xShift0;
		double newCY = CENTER_Y - yShift0;

		double newDX = CENTER_X + xShift1;
		double newDY = CENTER_Y - yShift1;

		printLines(newAX, newAY, newBX, newBY, newCX, newCY, newDX, newDY);

		assert cornerPointsContainPoint(cornerPoints, newAX, newAY);
		assert cornerPointsContainPoint(cornerPoints, newBX, newBY);
		assert cornerPointsContainPoint(cornerPoints, newCX, newCY);
		assert cornerPointsContainPoint(cornerPoints, newDX, newDY);
	}

	public static void printLines(double... points) {
		int length = points.length;
		if (length <= 2) {
			System.out.println(Arrays.toString(points));
			return;
		}


		double ax;
		double ay;
		double bx = points[0];
		double by = points[1];

		for (int i = 1; i < length / 2 + 1; i++) {
			ax = bx;
			ay = by;

			bx = points[(2 * i) % length];
			by = points[(2 * i + 1) % length];

			System.out.println();
			System.out.println(ax + "; " + ay);
			System.out.println(bx + "; " + by);

			double aAB = (by - ay) / (bx - ax);

			System.out.println("y = " + aAB + " * x + " + (ay - aAB * ax));
		}
	}

	private boolean cornerPointsContainPoint(List<Point> cornerPoints, double newX, double newY) {
		return cornerPoints.stream().anyMatch(point -> MyNumberOperations.doubleAlmostEqual(point.getX(), newX, 1e-7) && MyNumberOperations.doubleAlmostEqual(point.getY(), newY, 1e-7));
	}

	private static AgentPane setAgentPaneParameters(AgentPane agentPane) {
		agentPane.setLayoutX(CORNER_X);
		agentPane.setLayoutY(CORNER_Y);
		agentPane.setWidth(WIDTH);
		agentPane.setHeight(LENGTH);
		return agentPane;
	}
}