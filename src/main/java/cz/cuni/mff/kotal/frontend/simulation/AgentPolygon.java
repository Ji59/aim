package cz.cuni.mff.kotal.frontend.simulation;


import java.util.List;


/**
 * Polygon object representing agent shape.
 * Main use purpose is in collision algorithms.
 *
 * TODO
 */
public class AgentPolygon {
	private final long id;
	private final List<Point> corners;

	/**
	 * Create new polygon with specified attributes.
	 *
	 * @param id      ID of belonging agent
	 * @param corners Corners of belonging agent; in format [x0, y0, x1, y1, ... , xn, yn]
	 */
	public AgentPolygon(long id, List<Point> corners) {
		this.id = id;
		this.corners = corners;
	}

	/**
	 * @return ID of belonging agent
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return Corner coordinates of this polygon
	 */
	public List<Point> getCorners() {
		return corners;
	}
}
