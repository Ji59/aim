package cz.cuni.mff.kotal.frontend.simulation;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class Vertex extends cz.cuni.mff.kotal.simulation.graph.Vertex {
	private final double x, y;
	private final Set<Long> neighbour_ids = new HashSet<>();


	public Vertex(long id, double x, double y, Type type) {
		super(id, type);
		this.x = x;
		this.y = y;
	}

	public boolean addNeighbourID(Long... ids) {

		return neighbour_ids.addAll(Arrays.asList(ids));
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}
}
