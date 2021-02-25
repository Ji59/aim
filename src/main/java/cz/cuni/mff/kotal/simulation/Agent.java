package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.simulation.graph.Edge;
import cz.cuni.mff.kotal.simulation.graph.Vertex;


public class Agent {
	private final long id;
	private Edge location = null;
	protected final long start;
	protected final long end;

	public Agent(long id, long start, long end) {
		this.id = id;
		this.start = start;
		this.end = end;
	}

	public Agent(long id, Edge v, long start, long end) {
		this.id = id;
		location = v;
		this.start = start;
		this.end = end;
	}

	public long getId() {
		return id;
	}

	public Edge getLocation() {
		return location;
	}

	public Agent setLocation(Edge location) {
		this.location = location;
		return this;
	}

	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}
}