package cz.cuni.mff.kotal;


public class MyNumberOperations {
	public static boolean doubleAlmostEqual(double d0, double d1, double proximity) {
		double difference = Math.abs(d0 - d1);
		return difference <= proximity;
	}

	public static double computeRotation(double startX, double startY, double endX, double endY) {
		double xDiff = endX - startX,
			yDiff = endY - startY;
		if (yDiff == 0) {
			return xDiff == 0 ? -1 : xDiff > 0 ? 90 : 270;
		}
		double angle = Math.toDegrees(Math.atan(Math.abs(xDiff / yDiff)));
		if (xDiff >= 0 && yDiff < 0) {
			angle = 180 - angle;
		} else if (xDiff < 0 && yDiff >= 0) {
			angle = 360 - angle;
		} else if (xDiff < 0 && yDiff < 0) {
			angle = 180 + angle;
		}
		// TODO solve the minus
		return 360 - angle;
	}
}
