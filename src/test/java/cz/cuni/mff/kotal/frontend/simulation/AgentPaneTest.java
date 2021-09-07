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

		checkCornersAtAngle2(100, 100, false , true);
		checkCornersAtAngle2(100, 50, false , true);
		checkCornersAtAngle2(70, 100, false , true);
		checkCornersAtAngle2(10, 300, false , true);
		checkCornersAtAngle2(90, 100, false , true);
		checkCornersAtAngle2(1000, 50, false , true);
		checkCornersAtAngle2(77, 1000, false , true);
		checkCornersAtAngle2(13.5, 42.73, false , true);
		
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
		checkCornersAtAngle2(length, width, false);
	}

	private void checkCornersAtAngle2(double length, double width, boolean inverted) {
		checkCornersAtAngle2(length, width, inverted, false);
	}

	private void checkCornersAtAngle2(double length, double width, boolean inverted, boolean plus90) {
		int centerX = 200;
		int centerY = 100;
		Agent agent = new Agent(0, 0, 1, 1, 0, length, width, centerX, centerY);

		AgentPane agentPane = new AgentPane(agent);
		agentPane.setLayoutX(centerX);
		agentPane.setLayoutY(centerY);

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

		agentPane.setAngle(diagonalAngle);

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
			xShift1 =  - yShift1;
			yShift1 = temp;
		}

		double newAX = centerX + xShift0;
		double newAY = centerY + yShift0;

		double newBX = centerX - xShift1;
		double newBY = centerY + yShift1;

		double newCX = centerX - xShift0;
		double newCY = centerY - yShift0;

		double newDX = centerX + xShift1;
		double newDY = centerY - yShift1;

		System.out.println();
		System.out.println(newAX + "; " + newAY);
		System.out.println(newBX + "; " + newBY);
		System.out.println(newCX + "; " + newCY);
		System.out.println(newDX + "; " + newDY);

		double aAB = (newBY - newAY) / (newBX - newAX);
		double aBC = (newBY - newCY) / (newBX - newCX);
		double aCD = (newDY - newCY) / (newDX - newCX);
		double aDA = (newDY - newAY) / (newDX - newAX);

		System.out.println("\ny = " + aAB + " * x + " + (newAY - aAB * newAX));
		System.out.println("y = " + aBC + " * x + " + (newCY - aBC * newCX));
		System.out.println("y = " + aCD + " * x + " + (newCY - aCD * newCX));
		System.out.println("y = " + aDA + " * x + " + (newAY - aDA * newAX));

		assert cornerPointsContainPoint(cornerPoints, newAX, newAY);
		assert cornerPointsContainPoint(cornerPoints, newBX, newBY);
		assert cornerPointsContainPoint(cornerPoints, newCX, newCY);
		assert cornerPointsContainPoint(cornerPoints, newDX, newDY);
	}

	private boolean cornerPointsContainPoint(List<Point> cornerPoints, double newX, double newY) {
		return cornerPoints.stream().anyMatch(point -> MyNumberOperations.doubleAlmostEqual(point.getX(), newX, 1e-7) && MyNumberOperations.doubleAlmostEqual(point.getY(), newY, 1e-7));
	}
}