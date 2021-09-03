package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.MyNumberOperations;
import cz.cuni.mff.kotal.frontend.simulation.timer.Point;
import cz.cuni.mff.kotal.simulation.Agent;
import org.junit.jupiter.api.Test;

import java.util.List;


class AgentPaneTest {

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

	@Test
	void getCornerPoints() {
		AGENT_PANE.setLayoutX(CENTER_X);
		AGENT_PANE.setLayoutY(CENTER_Y);

		List<Point> cornerPoints = AGENT_PANE.getCornerPoints();
		cornerPoints.forEach(point -> System.out.println(point.getX() + "; " + point.getY()));
		assert cornerPoints.contains(new Point(CENTER_X - HALF_WIDTH, CENTER_Y - HALF_LENGTH));
		assert cornerPoints.contains(new Point(CENTER_X + HALF_WIDTH, CENTER_Y - HALF_LENGTH));
		assert cornerPoints.contains(new Point(CENTER_X - HALF_WIDTH, CENTER_Y + HALF_LENGTH));
		assert cornerPoints.contains(new Point(CENTER_X + HALF_WIDTH, CENTER_Y + HALF_LENGTH));

		double newAngle = Math.PI / 4;
		checkCornersAtAngle(newAngle);

		newAngle = Math.PI / 6;
		checkCornersAtAngle(newAngle);

		newAngle = Math.PI / 2;
		checkCornersAtAngle(newAngle);
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

//		assert cornerPointsContainPoint(cornerPoints, newAX, newAY);
//		assert cornerPointsContainPoint(cornerPoints, newBX, newBY);
//		assert cornerPointsContainPoint(cornerPoints, newCX, newCY);
//		assert cornerPointsContainPoint(cornerPoints, newDX, newDY);
	}

	private boolean cornerPointsContainPoint(List<Point> cornerPoints, double newAX, double newAY) {
		return cornerPoints.stream().anyMatch(point -> MyNumberOperations.doubleAlmostEqual(point.getX(), newAX, 1e-4) && MyNumberOperations.doubleAlmostEqual(point.getY(), newAY, 1e-4));
	}
}