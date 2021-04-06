package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.graph.Edge;

import java.util.ArrayList;
import java.util.List;


public class Agent {
	private final long id;
	private final double l;
	private final double w; // size
	private Edge location = null;
	private final long start,
		end;
	private final double speed,
		arrivalTime;
	private final List<Long> path = new ArrayList<>();
	private double x;
	private double y;       // location

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

	// TODO add exception
	public long getNextVertexID(double time) throws IndexOutOfBoundsException {
		if (time < arrivalTime) {
			return path.get(0);
		}
		return path.get((int) Math.round((time - arrivalTime) / speed));
	}

	public long getPreviousVertexID(double time) {
		if (time <= arrivalTime + speed) {
			return path.get(0);
		} else if (time > arrivalTime + path.size() * speed) {
			return path.get(path.size() - 1);
		}
		return path.get((int) Math.round((time - arrivalTime) / speed - 1));
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

	public void computeNextXY(double time) throws IndexOutOfBoundsException {
		double travelTime = time - arrivalTime,
			previousGoalTime = travelTime % speed,
			currentEdgeTravelPart = (travelTime - previousGoalTime) / speed,
			currentEdgeTravelRemain = 1 - currentEdgeTravelPart;
		long nextGoalID = path.get((int) (previousGoalTime / speed)),
			previousGoalID = path.get((int) (previousGoalTime / speed + 1));
		GraphicalVertex nextGoal = IntersectionModel.getGraph().getVertex(nextGoalID),
			previousGoal = IntersectionModel.getGraph().getVertex(previousGoalID);

		x = previousGoal.getX() * currentEdgeTravelPart + nextGoal.getX() * currentEdgeTravelRemain;
		y = previousGoal.getY() * currentEdgeTravelPart + nextGoal.getY() * currentEdgeTravelRemain;
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