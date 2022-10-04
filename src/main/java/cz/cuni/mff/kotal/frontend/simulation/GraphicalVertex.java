package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;


/**
 * Vertex with added coordinates.
 */
public class GraphicalVertex extends Vertex {
	private final double x;
	private final double y;

	/**
	 * Create new vertex with specified attributes.
	 *
	 * @param id   ID of the vertex
	 * @param x    X coordinate of the vertex
	 * @param y    Y coordinate of the vertex
	 * @param type Vertex type
	 */
	public GraphicalVertex(int id, double x, double y, Type type) {
		super(id, type);
		this.x = x;
		this.y = y;
	}

	/**
	 * Create new vertex with specified attributes and default vertex type.
	 *
	 * @param id ID of the vertex
	 * @param x  X coordinate of the vertex
	 * @param y  Y coordinate of the vertex
	 */
	public GraphicalVertex(int id, double x, double y) {
		super(id);
		this.x = x;
		this.y = y;
	}

	protected GraphicalVertex(@NotNull GraphicalVertex vertex) {
		super(vertex);
		this.x = vertex.getX();
		this.y = vertex.getY();
	}

	public GraphicalVertex(int id, double x, double y, Type type, Set<Integer> neighbourIDs) {
		super(id, type, neighbourIDs);
		this.x = x;
		this.y = y;
	}

	public GraphicalVertex(int id, @NotNull GraphicalVertex vertex) {
		super(id, vertex.getType());
		x = vertex.x;
		y = vertex.y;
	}

	/**
	 * @return Coordinate X of the vertex
	 */
	public double getX() {
		return x;
	}

	/**
	 * @return Coordinate Y of the vertex
	 */

	public double getY() {
		return y;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vertex vertex)) return false;
		return vertex.getID() == id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
