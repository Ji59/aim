package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.backend.algorithm.AStarRoundabout.LinkGraph;
import cz.cuni.mff.kotal.backend.algorithm.AStarRoundabout.LinkVertex;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.graph.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AStarRoundaboutTest {

	@Test
	void testCreateRoundaboutGraphSimple() {
		SimulationGraph square = new SquareGraph(4, 1, 1);
		LinkGraph graph = AStarRoundabout.createRoundaboutGraph(square, 1, true);

		int[] roadVertexIDs = {0, 1, 2, 3, 4, 7, 8, 11, 12, 13, 14, 15};
		int[] vertices = Arrays.stream(graph.getVertices()).mapToInt(v -> ((LinkVertex) v).getRealID()).toArray();
		assertThat(vertices).contains(roadVertexIDs).contains(16, 17, 18, 19, 20, 21, 22, 23);

		for (int id : roadVertexIDs) {
			LinkVertex vertex = graph.getLinkVertexByReal(id);
			int neighbours = Set.of(1, 7, 8, 14).contains(id) ? 2 : 1;
			assertThat(vertex.getNeighbourIDs()).hasSize(neighbours);
		}

		assertThat(graph.getEntryExitVertices()).hasSize(4);
		Collection<List<Vertex>> entryExitValues = graph.getEntryExitVertices().values();
		assertThat(entryExitValues.stream().mapToInt(List::size).sum()).isEqualTo(8);
		List<LinkVertex> entriesExits = entryExitValues.stream()
			.flatMap(List::stream)
			.map(v -> graph.getLinkVertexByReal(v.getID())).toList();
		assertThat(
			entriesExits.stream()
				.filter(v -> v.getType().isEntry())
				.filter(v -> v.getNeighbourIDs().size() == 1)
				.flatMap(v -> v.getNeighbourIDs().stream())
				.map(id -> graph.getLinkVertex(id).getRealID())
				.toArray()
		).containsExactlyInAnyOrder(2, 4, 11, 13);
		assertThat(entriesExits.stream()
			.filter(v -> v.getType().isExit())
			.flatMap(v -> v.getNeighbourIDs().stream())
			.toArray()
		).isEmpty();
	}

	@Test
	void testCreateRoundaboutGraphDoubleLanesOriented() {
		SimulationGraph square = new SquareGraph(4, 1, 1);
		LinkGraph graph = AStarRoundabout.createRoundaboutGraph(square, 2, true);

		assertThat(
			Arrays.stream(graph.getVertices())
				.map(v -> square.getVertex(((LinkVertex) v).getRealID()))
				.toList()
		).containsExactlyInAnyOrderElementsOf(
			Arrays.stream(square.getVertices())
				.map(GraphicalVertex.class::cast)
				.toList()
		);

		assertThat(graph.getLinkVertexByReal(0).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(1);
		assertThat(graph.getLinkVertexByReal(1).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(2, 23);
		assertThat(graph.getLinkVertexByReal(2).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(3, 6);
		assertThat(graph.getLinkVertexByReal(3).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(7);

		assertThat(graph.getLinkVertexByReal(4).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(0, 5);
		assertThat(graph.getLinkVertexByReal(5).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(1, 6);
		assertThat(graph.getLinkVertexByReal(6).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(7, 10);
		assertThat(graph.getLinkVertexByReal(7).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(11, 22);

		assertThat(graph.getLinkVertexByReal(8).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(4, 20);
		assertThat(graph.getLinkVertexByReal(9).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(5, 8);
		assertThat(graph.getLinkVertexByReal(10).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(9, 14);
		assertThat(graph.getLinkVertexByReal(11).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(10, 15);

		assertThat(graph.getLinkVertexByReal(12).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(8);
		assertThat(graph.getLinkVertexByReal(13).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(9, 12);
		assertThat(graph.getLinkVertexByReal(14).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(21, 13);
		assertThat(graph.getLinkVertexByReal(15).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(14);

		// entries
		assertThat(graph.getLinkVertexByReal(16).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactly(4);
		assertThat(graph.getLinkVertexByReal(17).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactly(13);
		assertThat(graph.getLinkVertexByReal(18).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactly(11);
		assertThat(graph.getLinkVertexByReal(19).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactly(2);

		// exits
		assertThat(graph.getLinkVertexByReal(20).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(21).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(22).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(23).getNeighbourIDs()).isEmpty();
	}

	@Test
	void testCreateRoundaboutGraphDoubleLanesNonDirect() {
		SimulationGraph octagonal = new SquareGraph(4, 1, 1);
		LinkGraph graph = AStarRoundabout.createRoundaboutGraph(octagonal, 2, false);

		assertThat(
			Arrays.stream(graph.getVertices())
				.map(v -> octagonal.getVertex(((LinkVertex) v).getRealID()))
				.toList()
		).containsExactlyInAnyOrderElementsOf(
			Arrays.stream(octagonal.getVertices())
				.map(GraphicalVertex.class::cast)
				.toList()
		);

		assertThat(graph.getLinkVertexByReal(0).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(1, 4);
		assertThat(graph.getLinkVertexByReal(1).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(0, 2, 5, 23);
		assertThat(graph.getLinkVertexByReal(2).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(1, 3, 6);
		assertThat(graph.getLinkVertexByReal(3).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(2, 7);

		assertThat(graph.getLinkVertexByReal(4).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(0, 5, 8);
		assertThat(graph.getLinkVertexByReal(5).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(1, 4, 6, 9);
		assertThat(graph.getLinkVertexByReal(6).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(2, 5, 7, 10);
		assertThat(graph.getLinkVertexByReal(7).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(3, 6, 11, 22);

		assertThat(graph.getLinkVertexByReal(8).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(4, 9, 12, 20);
		assertThat(graph.getLinkVertexByReal(9).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(5, 8, 10, 13);
		assertThat(graph.getLinkVertexByReal(10).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(6, 9, 11, 14);
		assertThat(graph.getLinkVertexByReal(11).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(7, 10, 15);

		// entries
		assertThat(graph.getLinkVertexByReal(12).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(8, 13);
		assertThat(graph.getLinkVertexByReal(13).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(9, 12, 14);
		assertThat(graph.getLinkVertexByReal(14).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(10, 13, 15, 21);
		assertThat(graph.getLinkVertexByReal(15).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(11, 14);

		// exits
		assertThat(graph.getLinkVertexByReal(16).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactly(4);
		assertThat(graph.getLinkVertexByReal(17).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactly(13);
		assertThat(graph.getLinkVertexByReal(18).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactly(11);
		assertThat(graph.getLinkVertexByReal(19).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactly(2);

		assertThat(graph.getLinkVertexByReal(20).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(21).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(22).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(23).getNeighbourIDs()).isEmpty();
	}

	@Test
	void testCreateRoundaboutGraphTripleLanesOctagonal() {
		SimulationGraph octagonal = new OctagonalGraph(4, 1, 1);
		LinkGraph graph = AStarRoundabout.createRoundaboutGraph(octagonal, 3, true);

		assertThat(
			Arrays.stream(graph.getVertices())
				.map(v -> octagonal.getVertex(((LinkVertex) v).getRealID()))
				.toList()
		).containsExactlyInAnyOrderElementsOf(
			Arrays.stream(octagonal.getVertices())
				.map(GraphicalVertex.class::cast)
				.toList()
		);

		// octagonal vertices
		assertThat(graph.getLinkVertexByReal(0).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(1, 19, 21);
		assertThat(graph.getLinkVertexByReal(1).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(4, 22);

		assertThat(graph.getLinkVertexByReal(2).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(3, 20);
		assertThat(graph.getLinkVertexByReal(3).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(0, 4, 20, 21, 24);
		assertThat(graph.getLinkVertexByReal(4).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(5, 8, 22, 24, 25);
		assertThat(graph.getLinkVertexByReal(5).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(9, 18, 25);

		assertThat(graph.getLinkVertexByReal(6).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(2, 16, 23);
		assertThat(graph.getLinkVertexByReal(7).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(3, 6, 23, 24, 26);
		assertThat(graph.getLinkVertexByReal(8).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(7, 11, 24, 27, 28);
		assertThat(graph.getLinkVertexByReal(9).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(8, 28);

		assertThat(graph.getLinkVertexByReal(10).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(7, 26);
		assertThat(graph.getLinkVertexByReal(11).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(10, 17, 27);

		// entries
		assertThat(graph.getLinkVertexByReal(12).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(2);
		assertThat(graph.getLinkVertexByReal(13).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(10);
		assertThat(graph.getLinkVertexByReal(14).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(9);
		assertThat(graph.getLinkVertexByReal(15).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(1);

		// exits
		assertThat(graph.getLinkVertexByReal(16).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(17).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(18).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(19).getNeighbourIDs()).isEmpty();

		// square vertices
		assertThat(graph.getLinkVertexByReal(20).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(0, 3);
		assertThat(graph.getLinkVertexByReal(21).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(1, 4);
		assertThat(graph.getLinkVertexByReal(22).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(4, 5);

		assertThat(graph.getLinkVertexByReal(23).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(2, 3);
		assertThat(graph.getLinkVertexByReal(24).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(3, 4, 7, 8);
		assertThat(graph.getLinkVertexByReal(25).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(8, 9);

		assertThat(graph.getLinkVertexByReal(26).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(6, 7);
		assertThat(graph.getLinkVertexByReal(27).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(7, 10);
		assertThat(graph.getLinkVertexByReal(28).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(8, 11);
	}

	@Test
	void testCreateRoundaboutGraphTripleLanesHexagonal() {
		SimulationGraph hexagonal = new HexagonalGraph(4, 1, 1);
		LinkGraph graph = AStarRoundabout.createRoundaboutGraph(hexagonal, 3, true);

		assertThat(
			Arrays.stream(graph.getVertices())
				.map(v -> hexagonal.getVertex(((LinkVertex) v).getRealID()))
				.toList()
		).containsExactlyInAnyOrderElementsOf(
			Arrays.stream(hexagonal.getVertices())
				.map(GraphicalVertex.class::cast)
				.filter(v -> v.getID() != 0)
				.toList()
		);

		// road vertices
		assertThat(graph.getLinkVertexByReal(1).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(6, 7, 18);
		assertThat(graph.getLinkVertexByReal(2).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(1, 8, 9);
		assertThat(graph.getLinkVertexByReal(3).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(2, 10, 11);
		assertThat(graph.getLinkVertexByReal(4).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(3, 12, 13);
		assertThat(graph.getLinkVertexByReal(5).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(4, 14, 15);
		assertThat(graph.getLinkVertexByReal(6).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(5, 16, 17);

		assertThat(graph.getLinkVertexByReal(7).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(1, 18, 19, 36);
		assertThat(graph.getLinkVertexByReal(8).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(1, 7, 20);

		assertThat(graph.getLinkVertexByReal(9).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(2, 8, 21, 22);
		assertThat(graph.getLinkVertexByReal(10).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(2, 9, 23);

		assertThat(graph.getLinkVertexByReal(11).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(3, 10, 24, 25);
		assertThat(graph.getLinkVertexByReal(12).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(3, 11, 26);

		assertThat(graph.getLinkVertexByReal(13).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(4, 12, 27, 28);
		assertThat(graph.getLinkVertexByReal(14).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(4, 13, 29);

		assertThat(graph.getLinkVertexByReal(15).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(5, 14, 30, 31);
		assertThat(graph.getLinkVertexByReal(16).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(5, 15, 32);

		assertThat(graph.getLinkVertexByReal(17).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(6, 16, 33, 34);
		assertThat(graph.getLinkVertexByReal(18).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(6, 17, 35);

		assertThat(graph.getLinkVertexByReal(19).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(7, 36);
		assertThat(graph.getLinkVertexByReal(20).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(7, 19);
		assertThat(graph.getLinkVertexByReal(21).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(8, 20, 43);

		assertThat(graph.getLinkVertexByReal(22).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(9, 21);
		assertThat(graph.getLinkVertexByReal(23).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(9, 22);
		assertThat(graph.getLinkVertexByReal(24).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(10, 23, 44);

		assertThat(graph.getLinkVertexByReal(25).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(11, 24);
		assertThat(graph.getLinkVertexByReal(26).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(11, 25);
		assertThat(graph.getLinkVertexByReal(27).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(12, 26, 45);

		assertThat(graph.getLinkVertexByReal(28).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(13, 27);
		assertThat(graph.getLinkVertexByReal(29).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(13, 28);
		assertThat(graph.getLinkVertexByReal(30).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(14, 29, 46);

		assertThat(graph.getLinkVertexByReal(31).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(15, 30);
		assertThat(graph.getLinkVertexByReal(32).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(15, 31);
		assertThat(graph.getLinkVertexByReal(33).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(16, 32, 47);

		assertThat(graph.getLinkVertexByReal(34).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(17, 33);
		assertThat(graph.getLinkVertexByReal(35).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(17, 34);
		assertThat(graph.getLinkVertexByReal(36).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(18, 35, 48);

		// entries
		assertThat(graph.getLinkVertexByReal(37).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(20);
		assertThat(graph.getLinkVertexByReal(38).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(23);
		assertThat(graph.getLinkVertexByReal(39).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(26);
		assertThat(graph.getLinkVertexByReal(40).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(29);
		assertThat(graph.getLinkVertexByReal(41).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(32);
		assertThat(graph.getLinkVertexByReal(42).getNeighbourIDs().stream().mapToInt(id -> graph.getLinkVertex(id).getRealID())).containsExactlyInAnyOrder(35);

		// exits
		assertThat(graph.getLinkVertexByReal(43).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(44).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(45).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(46).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(47).getNeighbourIDs()).isEmpty();
		assertThat(graph.getLinkVertexByReal(48).getNeighbourIDs()).isEmpty();
	}
}