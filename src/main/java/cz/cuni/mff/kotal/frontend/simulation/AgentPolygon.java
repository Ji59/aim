package cz.cuni.mff.kotal.frontend.simulation;

import java.util.List;

public class AgentPolygon {
	private final long id;

	private final List<Point> corners;

	public AgentPolygon(long id, List<Point> corners) {
		this.id = id;
		this.corners = corners;
	}

	public long getId() {
		return id;
	}

	public List<Point> getCorners() {
		return corners;
	}
}
