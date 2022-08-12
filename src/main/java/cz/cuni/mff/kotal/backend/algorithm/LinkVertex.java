package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;

public class LinkVertex extends GraphicalVertex {
	private final int realID;

	public LinkVertex(int id, GraphicalVertex vertex) {
		super(id, vertex);
		realID = vertex.getID();
	}

	public int getRealID() {
		return realID;
	}
}
