package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;

public class AStarBidirectionalRoundabout extends AStarRoundabout {
	public AStarBidirectionalRoundabout(@NotNull SimulationGraph graph) {
		super(graph, false);
	}
}
