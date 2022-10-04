package cz.cuni.mff.kotal.helpers;


import cz.cuni.mff.kotal.frontend.simulation.LineSegment;
import cz.cuni.mff.kotal.frontend.simulation.Point;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


/**
 * Class representing segment with saved ID value.
 */
public class LineSegmentWithID extends LineSegment implements Comparable<LineSegment> {
	private final long id;

	/**
	 * Create new line segment between [x0, y0] and [x1, y1] with given ID.
	 *
	 * @param x0 Coordinate X of the first point
	 * @param y0 Coordinate Y of the first point
	 * @param x1 Coordinate X of the second point
	 * @param y1 Coordinate Y of the second point
	 * @param id Agent ID the line segment is associated with
	 */
	public LineSegmentWithID(double x0, double y0, double x1, double y1, long id) {
		super(x0, y0, x1, y1);
		this.id = id;
	}

	/**
	 * Create new line segment between the points with given ID.
	 *
	 * @param point0 Starting point of the line segment
	 * @param point1 Ending point of the line segment
	 * @param id     Agent ID the line segment is associated with
	 */
	public LineSegmentWithID(@NotNull Point point0, @NotNull Point point1, long id) {
		super(point0, point1);
		this.id = id;
	}

	/**
	 * @return Agent ID the line segment is associated with
	 */
	public long getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * Compare this line segment to other one.
	 *
	 * @param lineSegment Comparing line segment
	 * @return First point X difference if not zero, otherwise first point Y difference
	 */
	@Override
	public int compareTo(@NotNull LineSegment lineSegment) {
		int xDiff = Double.compare(getX0(), lineSegment.getX0());
		if (xDiff == 0) {
			return Double.compare(getY0(), lineSegment.getY0());
		}
		return xDiff;
	}
}
