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
	private final BigDecimal value;

	/**
	 * Create new edge between specified vertices with specified value.
	 *
	 * @param u     First vertex
	 * @param v     Second vertex
	 * @param value Value of the edge
	 */
	public Edge(@NotNull Vertex u, @NotNull Vertex v, BigDecimal value) {
		this.u = u;
		this.v = v;
		this.value = value;
	}

	/**
	 * Create new edge between specified vertices with value 1.
	 *
	 * @param u First vertex
	 * @param v Second vertex
	 */
	public Edge(Vertex u, Vertex v) {
		this(u, v, BigDecimal.ONE);
	}

	/**
	 * @return New Edge with vertices reversed
	 */
	public Edge reverse() {
		return new Edge(v, u, value);
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
	public BigDecimal getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Edge edge = (Edge) o;
		if (u.equals(edge.u) && v.equals(edge.v)) {
			assert Objects.equals(value, edge.value);
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(u, v, value);
	}
}