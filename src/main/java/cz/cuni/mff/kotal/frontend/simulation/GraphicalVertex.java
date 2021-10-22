package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.simulation.graph.Vertex;


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
	public GraphicalVertex(long id, double x, double y, Type type) {
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
	public GraphicalVertex(long id, double x, double y) {
		super(id);
		this.x = x;
		this.y = y;
	}

	protected GraphicalVertex(GraphicalVertex vertex) {
		super(vertex);
		this.x = vertex.getX();
		this.y = vertex.getY();
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
}
