package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;


class SimulationGraphTest {
	SimulationGraph graph;
	private long granularity = 4;

	// FIXME

//	@Test
//	public void testSquareSimulationGraph() {
//		int totalDegree = 0;
//
//		graph = new SimulationGraph(granularity, IntersectionMenuTab0.Parameters.Models.SQUARE, 1, 1, 100, false);
//		assert graph.getVertices().size() == 24;
//
//		for (long i = 0; i < granularity; i++) {
//			for (long j = 0; j < granularity; j++) {
//				long id = i * granularity + j;
//				Set<Long> neighbourIDs = graph.getVertex(id).getNeighbourIDs();
//				assert i <= 0 || neighbourIDs.contains(id - granularity);
//				assert i >= granularity - 1 || neighbourIDs.contains(id + granularity);
//				assert j <= 0 || neighbourIDs.contains(id - 1);
//				assert j >= granularity - 1 || neighbourIDs.contains(id + 1);
//
//				if ((i == 0 || i == granularity - 1) && (j == 0 || j == granularity - 1)) {
//					assert neighbourIDs.size() == 2;
//				} else if (i == 0 || i == granularity - 1 || j == 0 || j == granularity - 1) {
//					assert neighbourIDs.size() == 3 || (Set.of(1, 7, 8, 14).contains((int) id) && neighbourIDs.size() == 4);
//				} else {
//					assert neighbourIDs.size() == 4;
//				}
//				totalDegree += neighbourIDs.size();
//			}
//		}
//
//		long g2 = granularity * granularity;
//		Vertex entryN = graph.getVertex(g2);
//		assert entryN.getNeighbourIDs().contains(4L);
//		totalDegree += entryN.getNeighbourIDs().size();
//		Vertex entryS = graph.getVertex(g2 + 1);
//		assert entryS.getNeighbourIDs().contains(11L);
//		totalDegree += entryS.getNeighbourIDs().size();
//		Vertex entryW = graph.getVertex(g2 + 2);
//		assert entryW.getNeighbourIDs().contains(2L);
//		totalDegree += entryW.getNeighbourIDs().size();
//		Vertex entryE = graph.getVertex(g2 + 3);
//		assert entryE.getNeighbourIDs().contains(13L);
//		totalDegree += entryE.getNeighbourIDs().size();
//
//		long g2g = g2 + granularity;
//		Vertex nextToExitN = graph.getVertex(8);
//		assert nextToExitN.getNeighbourIDs().contains(g2g);
//		totalDegree += graph.getVertex(g2g).getNeighbourIDs().size();
//		Vertex nextToExitS = graph.getVertex(7);
//		assert nextToExitS.getNeighbourIDs().contains(g2g + 1);
//		totalDegree += graph.getVertex(g2g + 1).getNeighbourIDs().size();
//		Vertex nextToExitW = graph.getVertex(1);
//		assert nextToExitW.getNeighbourIDs().contains(g2g + 2);
//		totalDegree += graph.getVertex(g2g + 2).getNeighbourIDs().size();
//		Vertex nextToExitE = graph.getVertex(14);
//		assert nextToExitE.getNeighbourIDs().contains(g2g + 3);
//		totalDegree += graph.getVertex(g2g + 3).getNeighbourIDs().size();
//
//		assert totalDegree == g2 * 4 - 8;
//		assert totalDegree == graph.getEdges().size();
//	}
//
//	@Test
//	public void testOctagonalSimulationGraph() {
//		int totalDegree = 0;
//
//		graph = new SimulationGraph(granularity, IntersectionMenuTab0.Parameters.Models.OCTAGONAL, 1, 1, 100, false);
//		assert graph.getVertices().size() == 16 + 9 + 4;
//
//		totalDegree += checkNeighbourIDs(0, 1, 3, 12, 13, 27);
//		totalDegree += checkNeighbourIDs(1, 0, 4, 13, 14);
//
//		totalDegree += checkNeighbourIDs(2, 3, 6, 12, 15);
//		totalDegree += checkNeighbourIDs(3, 0, 2, 4, 7, 12, 13, 15, 16);
//		totalDegree += checkNeighbourIDs(4, 1, 3, 5, 8, 13, 14, 16, 17);
//		totalDegree += checkNeighbourIDs(5, 4, 9, 14, 17, 26);
//
//		totalDegree += checkNeighbourIDs(6, 2, 7, 15, 18, 25);
//		totalDegree += checkNeighbourIDs(7, 3, 6, 8, 10, 15, 16, 18, 19);
//		totalDegree += checkNeighbourIDs(8, 4, 7, 9, 11, 16, 17, 19, 20);
//		totalDegree += checkNeighbourIDs(9, 5, 8, 17, 20);
//
//		totalDegree += checkNeighbourIDs(10, 7, 11, 18, 19);
//		totalDegree += checkNeighbourIDs(11, 8, 10, 19, 20, 28);
//
//		totalDegree += checkNeighbourIDs(12, 0, 2, 3);
//		totalDegree += checkNeighbourIDs(13, 0, 1, 3, 4);
//		totalDegree += checkNeighbourIDs(14, 1, 4, 5);
//
//		totalDegree += checkNeighbourIDs(15, 2, 3, 6, 7);
//		totalDegree += checkNeighbourIDs(16, 3, 4, 7, 8);
//		totalDegree += checkNeighbourIDs(17, 4, 5, 8, 9);
//
//		totalDegree += checkNeighbourIDs(18, 6, 7, 10);
//		totalDegree += checkNeighbourIDs(19, 7, 8, 10, 11);
//		totalDegree += checkNeighbourIDs(20, 8, 9, 11);
//
//		totalDegree += checkNeighbourIDs(21, 2);
//		totalDegree += checkNeighbourIDs(22, 9);
//		totalDegree += checkNeighbourIDs(23, 1);
//		totalDegree += checkNeighbourIDs(24, 10);
//
//		totalDegree += graph.getVertex(25).getNeighbourIDs().size();
//		totalDegree += graph.getVertex(26).getNeighbourIDs().size();
//		totalDegree += graph.getVertex(27).getNeighbourIDs().size();
//		totalDegree += graph.getVertex(28).getNeighbourIDs().size();
//
//		assert totalDegree == 2 * (2 * 4 * 4 /*diagonal*/ + 2 * 4 * 2 /*vertical + horizontal*/) + 8 /*entries and exits*/;
//		assert totalDegree == graph.getEdges().size();
//	}
//
//	@Test
//	public void testHexagonalSimulationGraph() {
//		int totalDegree = 0;
//		granularity = 3;
//		graph = new SimulationGraph(granularity, IntersectionMenuTab0.Parameters.Models.HEXAGONAL, 1, 1, 100, false);
//		assert graph.getVertices().size() == 1 + 6 + 12 +12;
//
//		totalDegree += checkNeighbourIDs(7, 1, 8, 18);
//		totalDegree += checkNeighbourIDs(18, 1, 6, 7, 17, 30);
//		totalDegree += checkNeighbourIDs(17, 6, 18, 16);
//
//		totalDegree += checkNeighbourIDs(8, 1, 2, 7, 9, 25);
//		totalDegree += checkNeighbourIDs(1, 0, 2, 6, 7, 8, 18);
//		totalDegree += checkNeighbourIDs(6, 0, 1, 5, 16, 17, 18);
//		totalDegree += checkNeighbourIDs(16, 5, 6, 15, 17, 29);
//
//		totalDegree += checkNeighbourIDs(9, 2, 8, 10);
//		totalDegree += checkNeighbourIDs(2, 0, 1, 3, 8, 9, 10);
//		totalDegree += checkNeighbourIDs(0, 1, 2, 3, 4, 5, 6);
//		totalDegree += checkNeighbourIDs(5, 0, 4, 6, 14, 15, 16);
//		totalDegree += checkNeighbourIDs(15, 5, 14, 16);
//
//		totalDegree += checkNeighbourIDs(10, 2, 3, 9, 11, 26);
//		totalDegree += checkNeighbourIDs(3, 0, 2, 4, 10, 11, 12);
//		totalDegree += checkNeighbourIDs(4, 0, 3, 5, 12, 13, 14);
//		totalDegree += checkNeighbourIDs(14, 4, 5, 13, 15, 28);
//
//		totalDegree += checkNeighbourIDs(11, 3, 10, 12);
//		totalDegree += checkNeighbourIDs(12, 3, 4, 11, 13, 27);
//		totalDegree += checkNeighbourIDs(13, 4, 12, 14);
//
//		for (int i = 19, j = 7; i < 25; i++, j += 2) {
//			totalDegree += checkNeighbourIDs(i, j);
//		}
//
//		for (int i = 25; i < 31; i++) {
//			totalDegree += checkNeighbourIDs(i);
//		}
//
//		assert totalDegree == 2 * 3 * (4 + 2 * 3 + 2 * 2) + 12;
//		assert totalDegree == graph.getEdges().size();
//	}

	private int checkNeighbourIDs(int id, long... neighbours) {
		GraphicalVertex vertex = graph.getVertex(id);
		Set<Long> neighbourIDs = vertex.getNeighbourIDs();
		assert neighbourIDs.containsAll(Arrays.stream(neighbours).boxed().collect(Collectors.toSet()));
		assert neighbourIDs.size() == neighbours.length;
		assert neighbourIDs.stream().map(idn -> graph.getVertex(idn)).allMatch(neighbour -> graph.getEdges().stream().anyMatch(edge -> edge.getU() == vertex && edge.getV() == neighbour));
		return neighbourIDs.size();
	}

}