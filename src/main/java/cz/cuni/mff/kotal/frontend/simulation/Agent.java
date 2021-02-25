package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.simulation.graph.Edge;


public class Agent extends cz.cuni.mff.kotal.simulation.Agent {
	private final double l, w; // size
	private double x, y;       // location

	public Agent(long id, double l, double w, double x, double y, Edge location, long start, long end) {
		super(id, location, start, end);
		this.l = l;
		this.w = w;
		this.x = x;
		this.y = y;
	}

	public double getL() {
		return l;
	}

	public double getW() {
		return w;
	}

	public double getX() {
		return x;
	}

	public Agent setX(double x) {
		this.x = x;
		return this;
	}

	public double getY() {
		return y;
	}

	public Agent setY(double y) {
		this.y = y;
		return this;
	}
}
