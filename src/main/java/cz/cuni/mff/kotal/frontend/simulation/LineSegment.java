package cz.cuni.mff.kotal.frontend.simulation;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class representing line segment between two points.
 */
public class LineSegment {
	private final Point point0;
	private final Point point1;

	private Double a = null;
	private Double b = null;

	/**
	 * Create new line segment between points [x0, y0] and [x1, y1], sorted increasingly by X coordinate, then by Y.
	 *
	 * @param x0 Coordinate X of the first end
	 * @param y0 Coordinate Y of the first end
	 * @param x1 Coordinate X of the second end
	 * @param y1 Coordinate Y of the second end
	 */
	public LineSegment(double x0, double y0, double x1, double y1) {
		if (x0 < x1 || (x0 == x1 && y0 <= y1)) {
			this.point0 = new Point(x0, y0);
			this.point1 = new Point(x1, y1);
		} else {
			this.point0 = new Point(x1, y1);
			this.point1 = new Point(x0, y0);
		}
	}

	/**
	 * Create new line segment between given points, sorted increasingly by X coordinate, then by Y.
	 *
	 * @param point0 First end if the new line segment
	 * @param point1 Second end of the new line segment
	 */
	public LineSegment(Point point0, Point point1) {
		if (point0.getX() < point1.getX() || (point0.getX() == point1.getX() && point0.getY() <= point1.getY())) {
			this.point0 = point0;
			this.point1 = point1;
		} else {
			this.point0 = point1;
			this.point1 = point0;
		}
	}

	/**
	 * Compute Y coordinate where this line segment intersects the `x = param.x` line. Does not check if the X is in the range of the segment.
	 *
	 * @param x Coordinate X to use in calculations
	 * @return Coordinate Y at given X
	 */
	public double getYAtX(double x) {
		double ratioToEnd = (point0.getX() - x) / (point1.getX() - point0.getX());
		return point0.getY() + (point1.getY() - point0.getY()) * ratioToEnd;
	}

	/**
	 * Compute on which side of this line does provided point lie using linear algebra.
	 *
	 * @param point Point used in calculations
	 * @return Shifted scalar product of the point and the line segment.
	 */
	public double getSide(Point point) {
		double x = point.getX();
		double y = point.getY();
		return (x - getX0()) * (getY1() - getY0()) - (y - getY0()) * (getX1() - getX0());
	}

	/**
	 * Compute in what ratio of the other line segment intersect this line segment.
	 * Does not check if the segments intersect.
	 *
	 * @param lineSegment Line segment dividing this line
	 * @return Ratio in which is this line segment divided
	 */
	public double getIntersectionRatio(LineSegment lineSegment) {
		if (a == null) {
			computeLineEquation();
		}
		if (lineSegment.a == null) {
			lineSegment.computeLineEquation();
		}

		if (a.isNaN()) {
			if (lineSegment.a.isNaN()) {
				return 0;
			}
			return getVerticalRatio(lineSegment.a, lineSegment.b, b);
		}
		double x = lineSegment.a.isNaN() ? lineSegment.b : (lineSegment.b - b) / (a - lineSegment.a);
		return (x - getX0()) / (getX1() - getX0());
	}

	/**
	 * Compute ratio in which this line segment is vertical and intersects other line.
	 *
	 * @param a Parametric representation of not vertical line
	 * @param b Parametric representation of not vertical line
	 * @param x Coordinate X where the lines intersect
	 * @return Ratio in which is this line segment divided
	 */
	private double getVerticalRatio(double a, double b, double x) {
		double y = a * x + b;
		return (y - getY0()) / (getY1() - getY0());
	}

	/**
	 * Computes line equation on which lies this line segment. The equation is in form `y = this.a * x + this.b`.
	 */
	private void computeLineEquation() {
		a = (getY1() - getY0()) / (getX1() - getX0());
		if (a.isNaN()) {
			b = getX0();
		} else {
			b = getY0() - a * getX0();
		}
	}

	public Point getPoint0() {
		return point0;
	}

	public double getX0() {
		return point0.getX();
	}

	public double getY0() {
		return point0.getY();
	}

	public Point getPoint1() {
		return point1;
	}

	public double getX1() {
		return point1.getX();
	}

	public double getY1() {
		return point1.getY();
	}

	/**
	 * Create LineSegments based on agent rectangle vertices.
	 *
	 * @param agentPane Agent to create line segments from
	 * @return Line segments associated with agents sides
	 */
	public static Set<LineSegment> getLineSegmentsFromAgent(AgentPane agentPane) {
		List<Point> vertices = agentPane.getCornerPoints();

		return getLineSegments(vertices);
	}

	/**
	 * Create LineSegments based on polygon vertices.
	 *
	 * @param vertices Sorted list of vertices creating closed loop
	 * @return Line segments associated with agents sides
	 */
	public static Set<LineSegment> getLineSegments(List<Point> vertices) {
		Set<LineSegment> lineSegments = new HashSet<>();
		int verticesCount = vertices.size();
		for (int i = 0; i < verticesCount; ) {
			lineSegments.add(new LineSegment(vertices.get(i), vertices.get(++i % verticesCount)));
		}
		return lineSegments;
	}
}
