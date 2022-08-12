package cz.cuni.mff.kotal.simulation.graph;

import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;

public class VertexWithDirectionParent extends VertexWithDirection {
	private final VertexWithDirectionParent parent;

	public VertexWithDirectionParent(GraphicalVertex vertex) {
		this(vertex, 0);
	}

	public VertexWithDirectionParent(GraphicalVertex vertex, double angle) {
		this(vertex, angle, 0);
	}

	public VertexWithDirectionParent(GraphicalVertex vertex, double angle, double estimate) {
		super(vertex, angle, estimate);
		parent = null;
	}

	public VertexWithDirectionParent(VertexWithDirectionParent previous, GraphicalVertex actual, Edge edge, double cellDistance) {
		this(previous, actual, edge, cellDistance, 0);
	}

	// TODO refactor
	public VertexWithDirectionParent(VertexWithDirectionParent previous, GraphicalVertex actual, Edge edge, double cellDistance, double estimate) {
		super(previous, actual, edge, cellDistance, estimate);
		this.parent = previous;
	}

	public VertexWithDirectionParent(VertexWithDirectionParent previous, GraphicalVertex actual, double distance, double estimate) {
		super(previous, actual, distance, estimate);

		this.parent = previous;
	}

	public VertexWithDirectionParent(VertexWithDirectionParent previous, GraphicalVertex actual, double cellDistance) {
		this(previous, actual, null, cellDistance, 0);
	}

	public VertexWithDirectionParent getParent() {
		return parent;
	}

}
