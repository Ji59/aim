package cz.cuni.mff.kotal.simulation.graph;

import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Class used for A* computation.
 * It also takes into account angles along some path.
 */
public class VertexWithDirection implements Comparable<VertexWithDirection> {
	protected final GraphicalVertex vertex;
	protected final double angle;
	protected final @NotNull Distance distance;
	protected final double estimate;

	/**
	 * Create new object with specified values, zero distance, angle and estimate.
	 *
	 * @param vertex Vertex this object is linked to
	 */
	public VertexWithDirection(GraphicalVertex vertex) {
		this(vertex, 0);
	}

	/**
	 * Create new object with specified values, zero distance and estimate.
	 *
	 * @param vertex Vertex this object is linked to
	 * @param angle  Starting angle of a path
	 */
	public VertexWithDirection(GraphicalVertex vertex, double angle) {
		this(vertex, angle, 0);
	}

	/**
	 * Create new object with specified values and zero distance.
	 *
	 * @param vertex   Vertex this object is linked to
	 * @param angle    Starting angle of a path
	 * @param estimate Estimate to goal
	 */
	public VertexWithDirection(GraphicalVertex vertex, double angle, double estimate) {
		this.vertex = vertex;
		this.angle = angle;
		this.distance = new Distance();
		this.estimate = estimate;
	}

	public VertexWithDirection(@NotNull VertexWithDirection previous, GraphicalVertex actual, Edge edge, double cellDistance) {
		this(previous, actual, edge, cellDistance, 0);
	}

	/**
	 * Create new object according to arguments.
	 * Distance is taken from parent and then add distance between vertices from argument.
	 * This distance is computed from edge and cell distance.
	 * Then compute angle from previous vertex to actual.
	 *
	 * @param previous     Last Vertex with safe distance and direction
	 * @param actual       New vertex on path
	 * @param edge         Edge between those vertices
	 * @param cellDistance Base distance between two vertices defined by graph, edge size is scaled with this value
	 * @param estimate     Estimate to goal
	 */
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

		this.distance = new Distance(previous.distance, verticesDistance, angleDiff, angleDiff == 0 ? 0 : 1);
		this.estimate = estimate;
	}

	/**
	 * Create new object according to arguments.
	 * Distance is taken from parent and then add distance between vertices from argument.
	 * Then compute angle from previous vertex to actual.
	 *
	 * @param previous Last Vertex with safe distance and direction
	 * @param actual   New vertex on path
	 * @param distance Distance between those vertices
	 * @param estimate Estimate to goal
	 */
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

	/**
	 * Compute angle between two vertices
	 *
	 * @return Angle on path between the two vertices in radians
	 */
	public static double computeAngle(@NotNull GraphicalVertex start, @NotNull GraphicalVertex end) {
		double xDiff = start.getX() - end.getX();
		double yDiff = start.getY() - end.getY();
		return VertexWithDirection.computeAngle(xDiff, yDiff);
	}

	/**
	 * Compute angle from (1, 0) to (xDiff, yDiff), result is in radians (-pi, pi).
	 *
	 * @param xDiff X coordinate of vector
	 * @param yDiff Y coordinate of vector
	 * @return angle of the vector in radians
	 */
	public static double computeAngle(double xDiff, double yDiff) {
		return Math.atan2(yDiff, xDiff);
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

	/**
	 * Compare this vertex with argument.
	 * If the argument is {@link Vertex}, compare its ID with vertex of this object.
	 * If the argument is another {@link VertexWithDirection}, also compare IDs of vertices these object represent.
	 *
	 * @param o the object to be compared
	 * @return True if the object represents same vertex
	 */
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

	/**
	 * @return Vertex this object represents
	 */
	public GraphicalVertex getVertex() {
		return vertex;
	}

	/**
	 * Compare two vertices for A* usage according to their sum of distance and heuristic.
	 * If the sum is equal, compare distance objects.
	 * If this sum is also equal, return comparison of the vertex IDs.
	 *
	 * @param o the object to be compared
	 * @return Value < 0 if path to this vertex is better than the other
	 */
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

	/**
	 * Class for holding and comparing path distances.
	 */
	public static class Distance implements Comparable<Distance> {
		protected final double distance;
		protected final double turnPenalty;
		protected final int turns;


		/**
		 * Create new distance with zero values.
		 */
		public Distance() {
			this.distance = 0;
			this.turnPenalty = 0;
			this.turns = 0;
		}

		/**
		 * Create new distance by summing previous values and parameters values.
		 */
		public Distance(double distance, double turnPenalty, int turns) {
			this.distance = distance;
			this.turnPenalty = turnPenalty;
			this.turns = turns;
		}

		/**
		 * Create new distance by summing previous values and parameters values.
		 */
		public Distance(@NotNull Distance previous, double distance, double turnPenalty, int turns) {
			this.distance = previous.distance + distance;
			this.turnPenalty = previous.turnPenalty + turnPenalty;
			this.turns = previous.turns + turns;
		}

		/**
		 * Create new distance with values computed from path on specified graph.
		 *
		 * @param path  Path to determine distance
		 * @param graph Graph the path is on
		 */
		public Distance(@NotNull List<Integer> path, @NotNull SimulationGraph graph) {
			double distance = 0;
			double turnPenalty = 0;
			int turns = 0;

			if (path.size() >= 2) {
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

		/**
		 * Create new distance by summing their values.
		 */
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
		 * Compare distances first according to turn penalty, then number of turns and finally by distance.
		 *
		 * @param o the object to be compared.
		 * @return True if the distances have same values
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
