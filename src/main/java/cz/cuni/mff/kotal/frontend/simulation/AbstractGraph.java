package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.simulation.graph.Edge;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class AbstractGraph extends SimulationGraph {
	private final IntersectionMenuTab0.Parameters.Models model;

	public AbstractGraph(IntersectionMenuTab0.Parameters.Models model, long granularity, long entries, long exits, boolean oriented, Set<GraphicalVertex> vertices, Map<Integer, List<Vertex>> entryExitVertices, Set<Edge> edges) {
		super(granularity, entries, exits, oriented, vertices, entryExitVertices, edges);
		this.model = model;
	}

	@Override
	public IntersectionMenuTab0.Parameters.Models getModel() {
		return model;
	}

	@Override
	public double getCellSize() {
		return 0;
	}
}
