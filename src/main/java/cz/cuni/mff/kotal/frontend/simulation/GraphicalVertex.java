package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.simulation.graph.Vertex;


public class GraphicalVertex extends Vertex {
	private final double x, y;


	public GraphicalVertex(long id, double x, double y, Type type) {
		super(id, type);
		this.x = x;
		this.y = y;
	}

	public GraphicalVertex(long id, double x, double y) {
		super(id);
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}
}
