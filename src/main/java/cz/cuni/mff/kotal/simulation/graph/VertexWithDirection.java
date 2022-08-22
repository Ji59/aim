package cz.cuni.mff.kotal.simulation.graph;

import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class VertexWithDirection implements Comparable<VertexWithDirection> {
	protected final GraphicalVertex vertex;
	protected final double angle;
	protected final double distance;
	protected final double turnPenalty;
	protected final int turns;
	protected final double estimate;

	public VertexWithDirection(GraphicalVertex vertex) {
		this(vertex, 0);
	}

	public VertexWithDirection(GraphicalVertex vertex, double angle) {
		this(vertex, angle, 0);
	}

	public VertexWithDirection(GraphicalVertex vertex, double angle, double estimate) {
		this.vertex = vertex;
		this.angle = angle;
		this.distance = 0;
		this.turnPenalty = 0;
		this.turns = 0;
		this.estimate = estimate;
	}

	public VertexWithDirection(VertexWithDirection previous, GraphicalVertex actual, Edge edge, double cellDistance) {
		this(previous, actual, edge, cellDistance, 0);
	}

	// TODO refactor
	public VertexWithDirection(VertexWithDirection previous, GraphicalVertex actual, Edge edge, double cellDistance, double estimate) {
		vertex = actual;

		GraphicalVertex previousVertex = previous.getVertex();
		double xDiff = previousVertex.getX() - vertex.getX();
		double yDiff = previousVertex.getY() - vertex.getY();
		this.angle = computeAngle(xDiff, yDiff);

		double verticesDistance = edge == null ? Math.sqrt(xDiff * xDiff + yDiff * yDiff) / cellDistance : edge.getDistance();

		double angleDiff;
		if (previous.getID() == getID()) {
			angleDiff = 0;
		} else {
			angleDiff = Math.abs(this.angle - previous.getAngle());
			if (angleDiff > Math.PI) {
				angleDiff = 2 * Math.PI - angleDiff;
			}
		}

//			double middleDistanceX = getX() - 0.5;
//			double middleDistanceY = getY() - 0.5;
//			double middleDistance = Math.sqrt(middleDistanceX * middleDistanceX + middleDistanceY * middleDistanceY);  // TODO

		this.distance = previous.distance + verticesDistance;
		this.turnPenalty = previous.turnPenalty + angleDiff;
		this.turns = previous.turns + (angleDiff == 0 ? 0 : 1);
		this.estimate = estimate;
	}

	public VertexWithDirection(VertexWithDirection previous, GraphicalVertex actual, double distance, double estimate) {
		vertex = actual;
		GraphicalVertex previousVertex = previous.vertex;
		double xDiff = previousVertex.getX() - vertex.getX();
		double yDiff = previousVertex.getY() - vertex.getY();
		this.angle = computeAngle(xDiff, yDiff);

		double angleDiff;
		if (previous.getID() == getID()) {
			angleDiff = 0;
		} else {
			angleDiff = Math.abs(this.angle - previous.getAngle());
			if (angleDiff > Math.PI) {
				angleDiff = 2 * Math.PI - angleDiff;
			}
		}

		this.distance = previous.distance + distance;
		this.turnPenalty = previous.turnPenalty + angleDiff;
		this.turns = previous.turns + (angleDiff == 0 ? 0 : 1);
		this.estimate = estimate;
	}

	public VertexWithDirection(VertexWithDirectionParent previous, GraphicalVertex actual, double cellDistance) {
		this(previous, actual, null, cellDistance, 0);
	}

	public static double computeAngle(GraphicalVertex start, GraphicalVertex end) {
		double xDiff = start.getX() - end.getX();
		double yDiff = start.getY() - end.getY();
		return VertexWithDirection.computeAngle(xDiff, yDiff);
	}

	public static double computeAngle(double xDiff, double yDiff) {
		return Math.atan2(yDiff, xDiff);
	}

	@Deprecated
	public static double getDistance(VertexWithDirection start, GraphicalVertex end) {
		GraphicalVertex startVertex = start.getVertex();
		double x0 = startVertex.getX();
		double y0 = startVertex.getY();
		double x1 = end.getX();
		double y1 = end.getY();

		double xDiff = x1 - x0;
		double yDiff = y1 - y0;

		double verticesDistance = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
		double angle = Math.atan2(yDiff, xDiff);
		double angleDiff = Math.abs(angle - start.getAngle());
		if (angleDiff > Math.PI) {
			angleDiff -= Math.PI;
		}
		return start.getDistance() + verticesDistance + angleDiff * SimulationGraph.EPSILON;
	}

	public double getAngle() {
		return angle;
	}

	public double getDistance() {
		return distance;
	}

	public double getTurnPenalty() {
		return turnPenalty;
	}

	public int getTurns() {
		return turns;
	}

	public double getEstimate() {
		return estimate;
	}

	public int getID() {
		return vertex.getID();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Vertex vertexObject) {
			return vertex.getID() == vertexObject.getID();
		} else if (o instanceof VertexWithDirection vertexWithDirection) {
			return vertex.getID() == vertexWithDirection.vertex.getID();
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(vertex.getID());
	}

	public GraphicalVertex getVertex() {
		return vertex;
	}

	@Override
	public int compareTo(@NotNull final VertexWithDirection o) {
		final int heuristicsComparison = Double.compare(distance + estimate, o.distance + o.estimate);
		if (heuristicsComparison == 0) {
			final int turnPenaltyComparison = Double.compare(turnPenalty, o.turnPenalty);
			if (turnPenaltyComparison == 0) {
				final int turnsComparison = Integer.compare(o.turns, turns);  // prefer vertex with higher number of turns on path
				if (turnsComparison == 0) {
					final int distanceComparison = Double.compare(o.distance, distance);  // prefer vertex with greater traveled distance
					if (distanceComparison == 0) {
						return Integer.compare(vertex.getID(), o.vertex.getID());
					}
					return distanceComparison;
				}
				return turnsComparison;
			}
			return turnPenaltyComparison;
		}
		return heuristicsComparison;
	}
}
