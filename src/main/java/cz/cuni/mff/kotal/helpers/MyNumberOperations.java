package cz.cuni.mff.kotal.helpers;


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
}
