package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


class SimulationGraphTest {

	@Test
	public void testSquareSimulationGraph() {
		int totalDegree = 0;

		int granularity = 4;
		SimulationGraph graph = new SimulationGraph(granularity, IntersectionMenuTab0.Parameters.Models.SQUARE, 1, 1, 100, false);
		assert graph.getVertices().size() == 24;

		for (long i = 0; i < granularity; i++) {
			for (long j = 0; j < granularity; j++) {
				long id = i * granularity + j;
				Set<Long> neighbourIDs = graph.getVertex(id).getNeighbourIDs();
				assert i <= 0 || neighbourIDs.contains(id - granularity);
				assert i >= granularity - 1 || neighbourIDs.contains(id + granularity);
				assert j <= 0 || neighbourIDs.contains(id - 1);
				assert j >= granularity - 1 || neighbourIDs.contains(id + 1);

				if ((i == 0 || i == granularity - 1) && (j == 0 || j == granularity - 1)) {
					assert neighbourIDs.size() == 2;
				} else if (i == 0 || i == granularity - 1 || j == 0 || j == granularity - 1) {
					assert neighbourIDs.size() == 3 || (Set.of(1, 7, 8, 14).contains((int) id) && neighbourIDs.size() == 4);
				} else {
					assert neighbourIDs.size() == 4;
				}
				totalDegree += neighbourIDs.size();
			}
		}

		long g2 = granularity * granularity;
		Vertex entryN = graph.getVertex(g2);
		assert entryN.getNeighbourIDs().contains(4L);
		totalDegree += entryN.getNeighbourIDs().size();
		Vertex entryS = graph.getVertex(g2 + 1);
		assert entryS.getNeighbourIDs().contains(11L);
		totalDegree += entryS.getNeighbourIDs().size();
		Vertex entryW = graph.getVertex(g2 + 2);
		assert entryW.getNeighbourIDs().contains(2L);
		totalDegree += entryW.getNeighbourIDs().size();
		Vertex entryE = graph.getVertex(g2 + 3);
		assert entryE.getNeighbourIDs().contains(13L);
		totalDegree += entryE.getNeighbourIDs().size();

		long g2g = g2 + granularity;
		Vertex nextToExitN = graph.getVertex(8);
		assert nextToExitN.getNeighbourIDs().contains(g2g);
		totalDegree += graph.getVertex(g2g).getNeighbourIDs().size();
		Vertex nextToExitS = graph.getVertex(7);
		assert nextToExitS.getNeighbourIDs().contains(g2g + 1);
		totalDegree += graph.getVertex(g2g + 1).getNeighbourIDs().size();
		Vertex nextToExitW = graph.getVertex(1);
		assert nextToExitW.getNeighbourIDs().contains(g2g + 2);
		totalDegree += graph.getVertex(g2g + 2).getNeighbourIDs().size();
		Vertex nextToExitE = graph.getVertex(14);
		assert nextToExitE.getNeighbourIDs().contains(g2g + 3);
		totalDegree += graph.getVertex(g2g + 3).getNeighbourIDs().size();

		assert totalDegree == g2 * 4 - 8;
	}

}