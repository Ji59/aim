package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import org.jetbrains.annotations.NotNull;

public class LinkVertex extends GraphicalVertex {
	private final int realID;

	public LinkVertex(int id, @NotNull GraphicalVertex vertex) {
		super(id, vertex);
		realID = vertex.getID();
	}

	public int getRealID() {
		return realID;
	}
}
