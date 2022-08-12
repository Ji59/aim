package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

public class AStarBidirectionalRoundabout extends AStarRoundabout {
	public AStarBidirectionalRoundabout(SimulationGraph graph) {
		super(graph, false);
	}
}
