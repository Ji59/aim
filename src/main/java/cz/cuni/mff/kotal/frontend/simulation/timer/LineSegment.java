package cz.cuni.mff.kotal.frontend.simulation.timer;


import cz.cuni.mff.kotal.frontend.simulation.AgentPane;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


class LineSegment {
	private final Point point0,
		point1;
	private final AgentPane agentPane;

	LineSegment(double x0, double y0, double x1, double y1, AgentPane agentPane) {
		this.point0 = new Point(x0, y0);
		this.point1 = new Point(x1, y1);
		this.agentPane = agentPane;
	}

	private LineSegment(Point point0, Point point1, AgentPane agentPane) {
		this.point0 = point0;
		this.point1 = point1;
		this.agentPane = agentPane;
	}

	LineSegment(Point point0, Point point1) {
		this(point0, point1, null);
	}

	public double getYAtX(double x) {
		double ratioToEnd = (point0.getX() - x) / (point1.getX() - point0.getX());
		return point0.getY() + (point1.getY() - point0.getY()) * ratioToEnd;
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

	public AgentPane getAgentPane() {
		return agentPane;
	}

	/**
	 * Create LineSegments based on agent rectangle vertices.
	 *
	 * @param agentPane Agent to create line segments from.
	 * @return Line segments associated with agents sides.
	 */
	public static Set<LineSegment> getLineSegmentsFromAgent(AgentPane agentPane) {
		List<Point> vertices = agentPane.getCornerPoints();

		return getLineSegments(agentPane, vertices);
	}

	/**
	 * Create LineSegments based on polygon vertices.
	 *
	 * @param agentPane Agent to create line segments from.
	 * @param vertices  Sorted list of vertices creating closed loop.
	 * @return Line segments associated with agents sides.
	 */
	public static Set<LineSegment> getLineSegments(AgentPane agentPane, List<Point> vertices) {
		Set<LineSegment> lineSegments = new HashSet<>();
		int verticesCount = vertices.size();
		for (int i = 0; i < verticesCount; ) {
			lineSegments.add(new LineSegment(vertices.get(i), vertices.get(++i % verticesCount), agentPane));
		}
		return lineSegments;
	}

	public double getSide(Point point) {
		double x = point.getX(),
			y = point.getY();
		return (x - getX0()) * (getY1() - getY0()) - (y - getY0()) * (getX1() - getX0());
	}
}
