package cz.cuni.mff.kotal.frontend.simulation;


import java.util.Objects;


/**
 * Class representing point in intersection plane.
 * Intended to use in collision algorithm.
 */
public class Point {
	private final double x;
	private final double y;

	/**
	 * Create new point at given coordinates.
	 *
	 * @param x Coordinate X of the point
	 * @param y Coordinate Y of the point
	 */
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * @return Coordinate X of this point
	 */
	public double getX() {
		return x;
	}

	/**
	 * @return Coordinate Y of this point
	 */
	public double getY() {
		return y;
	}

	/**
	 * Two points are equal if and only if they lie at the same coordinates.
	 *
	 * @param o Comparing point
	 * @return True if the points have same coordinates, otherwise false
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Point point = (Point) o;
		return Double.compare(point.x, x) == 0 && Double.compare(point.y, y) == 0;
	}


	/**
	 * @return Hashcode from X and Y coordinate
	 */
	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}
}
