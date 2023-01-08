package cz.cuni.mff.kotal.simulation.graph;

import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class VertexWithDirection implements Comparable<VertexWithDirection> {
	protected final GraphicalVertex vertex;
	protected final double angle;
	protected final @NotNull Distance distance;
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
		this.distance = new Distance();
		this.estimate = estimate;
	}

	public VertexWithDirection(@NotNull VertexWithDirection previous, GraphicalVertex actual, Edge edge, double cellDistance) {
		this(previous, actual, edge, cellDistance, 0);
	}

	// TODO refactor
	public VertexWithDirection(@NotNull VertexWithDirection previous, GraphicalVertex actual, @Nullable Edge edge, double cellDistance, double estimate) {
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

		this.distance = new Distance(previous.distance, verticesDistance, angleDiff, angleDiff == 0 ? 0 : 1);
		this.estimate = estimate;
	}

	public VertexWithDirection(@NotNull VertexWithDirection previous, GraphicalVertex actual, double distance, double estimate) {
		vertex = actual;
		GraphicalVertex previousVertex = previous.vertex;
		double xDiff = previousVertex.getX() - vertex.getX();
		double yDiff = previousVertex.getY() - vertex.getY();

		double angleDiff;
		if (previous.getID() == getID()) {
			angleDiff = 0;
			this.angle = previous.angle;
		} else {
			this.angle = computeAngle(xDiff, yDiff);
			angleDiff = Math.abs(this.angle - previous.getAngle());
			if (angleDiff > Math.PI) {
				angleDiff = 2 * Math.PI - angleDiff;
			}
		}

		this.distance = new Distance(previous.distance, distance, angleDiff, angleDiff == 0 ? 0 : 1);
		this.estimate = estimate;
	}

	public VertexWithDirection(@NotNull VertexWithDirectionParent previous, GraphicalVertex actual, double cellDistance) {
		this(previous, actual, null, cellDistance, 0);
	}

	public static double computeAngle(@NotNull GraphicalVertex start, @NotNull GraphicalVertex end) {
		double xDiff = start.getX() - end.getX();
		double yDiff = start.getY() - end.getY();
		return VertexWithDirection.computeAngle(xDiff, yDiff);
	}

	public static double computeAngle(double xDiff, double yDiff) {
		return Math.atan2(yDiff, xDiff);
	}

	@Deprecated
	public static double getDistance(@NotNull VertexWithDirection start, @NotNull GraphicalVertex end) {
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
		return distance.distance;
	}

	public double getTurnPenalty() {
		return distance.turnPenalty;
	}

	public int getTurns() {
		return distance.turns;
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
		final int heuristicsComparison = Double.compare(distance.distance + estimate, o.distance.distance + o.estimate);
		if (heuristicsComparison == 0) {
			final int distanceComparison = distance.compareTo(o.distance);
			if (distanceComparison == 0) {
				return Integer.compare(vertex.getID(), o.vertex.getID());
			}
			return distanceComparison;
		}
		return heuristicsComparison;
	}

	public static class Distance implements Comparable<Distance> {
		protected final double distance;
		protected final double turnPenalty;
		protected final int turns;

		public Distance() {
			this.distance = 0;
			this.turnPenalty = 0;
			this.turns = 0;
		}

		public Distance(double distance, double turnPenalty, int turns) {
			this.distance = distance;
			this.turnPenalty = turnPenalty;
			this.turns = turns;
		}

		public Distance(@NotNull Distance previous, double distance, double turnPenalty, int turns) {
			this.distance = previous.distance + distance;
			this.turnPenalty = previous.turnPenalty + turnPenalty;
			this.turns = previous.turns + turns;
		}

		public Distance(@NotNull List<Integer> path, @NotNull SimulationGraph graph) {
			double distance = 0;
			double turnPenalty = 0;
			int turns = 0;

			if (	path.size() >= 2) {
				double angle = 0;
				GraphicalVertex lastVertex;
				GraphicalVertex vertex;
				for (int i = 1, lastID = path.get(0), vertexID; i < path.size(); i++, lastID = vertexID) {
					vertexID = path.get(i);
					lastVertex = graph.getVertex(lastID);
					vertex = graph.getVertex(vertexID);

					distance += graph.getDistance(lastID, vertexID);

					double newAngle = computeAngle(lastVertex, vertex);
					if (i > 1 && lastID != vertexID) {
						double angleDiff = Math.abs(angle - newAngle);
						if (angleDiff > Math.PI) {
							angleDiff = 2 * Math.PI - angleDiff;
						}
						turnPenalty += angleDiff;
						if (angleDiff != 0) {
							turns++;
						}
					}
					angle = newAngle;
				}
			}

			this.distance = distance;
			this.turnPenalty = turnPenalty;
			this.turns = turns;
		}

		public Distance(@Nullable Distance distance0, @Nullable Distance distance1) {
			if (distance0 == null && distance1 == null) {
				this.distance = 0;
				this.turnPenalty = 0;
				this.turns = 0;
			} else if (distance1 == null) {
				this.distance = distance0.distance;
				this.turnPenalty = distance0.turnPenalty;
				this.turns = distance0.turns;
			} else if (distance0 == null) {
				this.distance = distance1.distance;
				this.turnPenalty = distance1.turnPenalty;
				this.turns = distance1.turns;
			} else {
				this.distance = distance0.distance + distance1.distance;
				this.turnPenalty = distance0.turnPenalty + distance1.turnPenalty;
				this.turns = distance0.turns + distance1.turns;
			}
		}

		/**
		 * @param o the object to be compared.
		 * @return
		 */
		@Override
		public int compareTo(@NotNull VertexWithDirection.Distance o) {
			final int turnPenaltyComparison = Double.compare(turnPenalty, o.turnPenalty);
			if (turnPenaltyComparison == 0) {
				final int turnsComparison = Integer.compare(o.turns, turns);  // prefer vertex with higher number of turns on path
				if (turnsComparison == 0) {
					return Double.compare(o.distance, distance);
				}
				return turnsComparison;
			}
			return turnPenaltyComparison;
		}
	}
}
