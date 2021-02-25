package cz.cuni.mff.kotal.frontend.simulation;


public class Vertex extends cz.cuni.mff.kotal.simulation.graph.Vertex {
	private final double x, y;


	public Vertex(long id, double x, double y, Type type) {
		super(id, type);
		this.x = x;
		this.y = y;
	}

	public Vertex(long id, double x, double y) {
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
