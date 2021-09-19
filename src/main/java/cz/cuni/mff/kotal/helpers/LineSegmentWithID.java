package cz.cuni.mff.kotal.helpers;

import cz.cuni.mff.kotal.frontend.simulation.LineSegment;
import cz.cuni.mff.kotal.frontend.simulation.Point;
import org.jetbrains.annotations.NotNull;

public class LineSegmentWithID extends LineSegment implements Comparable<LineSegment> {
	private final long id;

	public LineSegmentWithID(double x0, double y0, double x1, double y1, long id) {
		super(x0, y0, x1, y1);
		this.id = id;
	}

	public LineSegmentWithID(Point point0, Point point1, long id) {
		super(point0, point1);
		this.id = id;
	}

	public long getId() {
		return id;
	}

	@Override
	public int compareTo(@NotNull LineSegment lineSegment) {
		int xDiff = Double.compare(getX0(), lineSegment.getX0());
		if (xDiff == 0) {
			return Double.compare(getY0(), getY1());
		}
		return xDiff;
	}
}
