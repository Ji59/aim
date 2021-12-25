package cz.cuni.mff.kotal.helpers;


import cz.cuni.mff.kotal.frontend.simulation.Point;

/**
 * Class with special number operations.
 */
public class MyNumberOperations {

	/**
	 * Private constructor to mask implicit one.
	 */
	private MyNumberOperations() {
	}

	/**
	 * Compare if two double values are similar enough.
	 *
	 * @param d0        First number
	 * @param d1        Second number
	 * @param proximity Maximal allowed difference between the numbers
	 * @return True if difference between the numbers are within specified range
	 */
	public static boolean doubleAlmostEqual(double d0, double d1, double proximity) {
		double difference = Math.abs(d0 - d1);
		return difference <= proximity;
	}

	/**
	 * Compute angle of line between specified two points.
	 *
	 * @param startX Starting X coordinate of the first point
	 * @param startY Starting Y coordinate of the first point
	 * @param endX   Ending X coordinate of the first point
	 * @param endY   Ending Y coordinate of the first point
	 * @return Angle of line
	 */
	public static double computeRotation(double startX, double startY, double endX, double endY) {
		double xDiff = endX - startX;
		double yDiff = endY - startY;
		if (yDiff == 0) {
			if (xDiff == 0) {
				return -1;
			} else {
				if (xDiff > 0) return 90;
				return 270;
			}
		}
		double angle = Math.toDegrees(Math.atan(Math.abs(xDiff / yDiff)));
		if (xDiff >= 0 && yDiff < 0) {
			angle = 180 - angle;
		} else if (xDiff < 0 && yDiff >= 0) {
			angle = 360 - angle;
		} else if (xDiff < 0 && yDiff < 0) {
			angle = 180 + angle;
		}
		return 360 - angle;
	}

	/**
	 * Computes modulo and convert result to non-negative numbers.
	 *
	 * @param number Input number to modify
	 * @param modulo Number taken as base
	 * @return Positive modulo of number
	 */
	public static long myModulo(long number, long modulo) {
		long result = number % modulo;
		return result >= 0 ? result : modulo + result;
	}

	// TODO
	public static double distance(double x0, double y0, double x1, double y1) {
		double xDiff = x1 - x0;
		double yDiff = y1 - y0;
		return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
	}

	public static double distance(Point p0, Point p1) {
		return distance(p0.getX(), p0.getY(), p1.getX(), p1.getY());
	}

	public static double perimeter(double l, double w) {
		return Math.sqrt(l * l + w * w) / 2;
	}
}
