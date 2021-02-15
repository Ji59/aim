package cz.cuni.mff.kotal.simulation.graph;


import java.math.BigDecimal;
import java.util.Objects;


public class Edge {

	private final Vertex u, v;
	private final BigDecimal value;

	public Edge(Vertex u, Vertex v, BigDecimal value) {
		assert u != null;
		assert v != null;

		this.u = u;
		this.v = v;
		this.value = value;
	}

	public Edge(Vertex u, Vertex v) {
		this(u, v, BigDecimal.ONE);
	}

	public Edge reverse() {
		return new Edge(v, u, value);
	}

	public Vertex getU() {
		return u;
	}

	public Vertex getV() {
		return v;
	}

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