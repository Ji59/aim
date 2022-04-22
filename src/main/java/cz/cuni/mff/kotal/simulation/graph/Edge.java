package cz.cuni.mff.kotal.simulation.graph;


import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;


/**
 * Class representing graph edge with value.
 */
public class Edge {

	private final Vertex u;
	private final Vertex v;
	private final double distance;

	/**
	 * Create new edge between specified vertices with specified value.
	 *
	 * @param u     First vertex
	 * @param v     Second vertex
	 * @param value Value of the edge
	 */
	public Edge(@NotNull Vertex u, @NotNull Vertex v, double value) {
		this.u = u;
		this.v = v;
		this.distance = value;
	}

	/**
	 * Create new edge between specified vertices with value 1.
	 *
	 * @param u First vertex
	 * @param v Second vertex
	 */
	public Edge(Vertex u, Vertex v) {
		this(u, v, 1.);
	}

	/**
	 * @return New Edge with vertices reversed
	 */
	public Edge reverse() {
		return new Edge(v, u, distance);
	}

	/**
	 *
	 * @return First vertex
	 */
	public Vertex getU() {
		return u;
	}

	/**
	 *
	 * @return Second vertex
	 */
	public Vertex getV() {
		return v;
	}

	/**
	 *
	 * @return Edge value
	 */
	public double getDistance() {
		return distance;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Edge edge = (Edge) o;
		if (u.equals(edge.u) && v.equals(edge.v)) {
			assert Objects.equals(distance, edge.distance);
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(u, v, distance);
	}
}