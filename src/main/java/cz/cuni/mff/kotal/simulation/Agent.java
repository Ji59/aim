package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.graph.Edge;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cz.cuni.mff.kotal.MyNumberOperations.doubleAlmostEqual;


public class Agent {
	private final long id;
	private final double l, w; // size
	private Edge location = null;
	private final long start,
		end;
	private final double speed,
		arrivalTime;
	private List<Long> path = new ArrayList<>();
	private double x, y;       // location
	private static final double PROXIMITY = 0.0001;

	public Agent(long id, long start, long end, double speed, double arrivalTime, double l, double w, double x, double y) {
		this.id = id;
		this.start = start;
		this.end = end;
		this.speed = speed;
		this.arrivalTime = arrivalTime;
		this.l = l;
		this.w = w;
		this.x = x;
		this.y = y;
	}

	public Agent(long id, Edge v, long start, long end, double speed, double arrivalTime, double l, double w, double x, double y) {
		this.id = id;
		location = v;
		this.start = start;
		this.end = end;
		this.speed = speed;
		this.arrivalTime = arrivalTime;
		this.l = l;
		this.w = w;
		this.x = x;
		this.y = y;
	}

	/**
	 * Return previous and next target vertex ID at given time.
	 * If time is before arrival, return twice ID of starting vertex.
	 * If time is greater then arrival at the end time, throw exception.
	 * Else return the nearest next vertex ID and vertex ID of its predecessor.
	 *
	 * @param time Relative time to arrival time.
	 * @return Pair od IDs of the previous and next vertex at given time.
	 * @throws IndexOutOfBoundsException TODO
	 */
	public Pair<Long, Long> getPreviousNextVertexIDs(double time) throws IndexOutOfBoundsException {
		// TODO add exception
		if (time < 0) {
			Long first = path.get(0);
			return new Pair<>(first, first);
		}
		if (doubleAlmostEqual(time, (path.size() - 1) / speed, PROXIMITY)) {
			long exitID = path.get(path.size() - 1);
			return new Pair<>(exitID, exitID);
		}
		int nextIndex = (int) Math.round((time) * speed + 0.5),
			previousIndex = nextIndex - 1;
		return new Pair<>(path.get(previousIndex), path.get(nextIndex));
	}

	public long getId() {
		return id;
	}

	public Edge getLocation() {
		return location;
	}

	public Agent setLocation(Edge location) {
		this.location = location;
		return this;
	}

	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}

	public double getSpeed() {
		return speed;
	}

	public List<Long> getPath() {
		return path;
	}

	public void setPath(List<Long> path) {
		this.path = path;
	}

	public void computeNextXY(double time, Map<Long, Vertex> vertices) throws IndexOutOfBoundsException {
		double travelTime = time /*- arrivalTime*/,
			currentEdgeTravelPart = (travelTime * speed) % 1,
			currentEdgeTravelRemain = 1 - currentEdgeTravelPart;
		Pair<Long, Long> previousNextGoalID = getPreviousNextVertexIDs(time);
		GraphicalVertex previousGoal = (GraphicalVertex) vertices.get(previousNextGoalID.getKey()),
			nextGoal = (GraphicalVertex) vertices.get(previousNextGoalID.getValue());

		x = previousGoal.getX() * currentEdgeTravelRemain + nextGoal.getX() * currentEdgeTravelPart;
		y = previousGoal.getY() * currentEdgeTravelRemain + nextGoal.getY() * currentEdgeTravelPart;
	}

	public double getL() {
		return l;
	}

	public double getW() {
		return w;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}
}