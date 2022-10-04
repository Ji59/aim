package cz.cuni.mff.kotal.frontend.simulation;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;


/**
 * Polygon object representing agent shape.
 * Main use purpose is in collision algorithms.
 *
 * TODO
 */
public class AgentPolygon {
	private final List<Point> corners;

	/**
	 * Create new polygon with specified attributes.
	 *
	 * @param id      ID of belonging agent
	 * @param corners Corners of belonging agent; in format [x0, y0, x1, y1, ... , xn, yn]
	 */
	public AgentPolygon(long id, List<Point> corners) {
		this.corners = corners;
	}

	/**
	 *
	 * @param id
	 * @param points
	 */
	@TestOnly
	public AgentPolygon(long id, double @NotNull ... points) {
		this.corners = new ArrayList<>(points.length / 2);
		for (int i = 0; i < points.length; i += 2) {
			corners.add(new Point(points[i], points[i + 1]));
		}
	}

	/**
	 * @return Corner coordinates of this polygon
	 */
	public List<Point> getCorners() {
		return corners;
	}
}
