package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.frontend.simulation.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.util.*;


class SimulationTest {
	private final long GRAPH_GRANULARITY = 4,
		GRAPH_ENTRIES = 2,
		GRAPH_EXITS = 1;
	private final IntersectionMenuTab0.Parameters.Models GRAPH_MODEL = IntersectionMenuTab0.Parameters.Models.SQUARE;
	private final double GRAPH_SIZE = 1000;
	private final boolean GRAPH_ABSTRACT = true;
	private Graph graph;

	private final long NEW_AGENTS_MINIMUM = 2,
		NEW_AGENTS_MAXIMUM = 4;
	List<Long> DISTRIBUTION = Arrays.asList(10L, 70L, 20L, 0L);
	private Simulation simulation;


	@Before
	public void setUp() {
		graph = new SimulationGraph(GRAPH_GRANULARITY, GRAPH_MODEL, GRAPH_ENTRIES, GRAPH_EXITS, GRAPH_SIZE, GRAPH_ABSTRACT);
		simulation = new Simulation(graph, NEW_AGENTS_MINIMUM, NEW_AGENTS_MAXIMUM, DISTRIBUTION);
	}

	@Test
	void tick() {
	}

	@Test
	void generateAgents() {
		assert (simulation != null && graph != null);

		simulation.generateAgents();
		Collection<Agent> agents = simulation.getAllAgents();

		assert (agents.size() >= NEW_AGENTS_MINIMUM && agents.size() <= NEW_AGENTS_MAXIMUM);

		agents.forEach(agent -> {
			Vertex entry = findGraphEntryExit(agent.getStart());
			assert (entry != null && entry.getType().isEntry());
			Vertex exit = findGraphEntryExit(agent.getEnd());
			assert (exit != null && !entry.getType().isEntry());
		});
	}

	private Vertex findGraphEntryExit(long vertexID) {
		Map<Integer, List<Vertex>> entries = graph.getEntryExitVertices();
		for (int i = 0; i < DISTRIBUTION.size(); i++) {
			for (Vertex vertex : entries.get(i)) {
				if (vertex.getID() == vertexID) {
					return vertex;
				}
			}
		}
		return null;
	}
}