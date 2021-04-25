package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.frontend.simulation.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class AgentTest {
	private final long id = 0, l = 1, w = 0,
		start = 0, end = 7;
	private final double speed = 1,
		x = 0, y = 0,
		arrivalTime = 0;
	private Agent agent;
	private List<Long> path;
	private Map<Long, Vertex> vertices;

	@BeforeEach
	void startUp() {
		agent = new Agent(id, start, end, speed, arrivalTime, l, w, x, y);
		path = new ArrayList<>();
		vertices = new HashMap<>();
		for (long i = start; i <= end; i++) {
			path.add(i);
			Vertex vertex = new GraphicalVertex(i, 0, i % 2 == 0 ? 2 * i + 1 : 2 * i);
			vertices.put(i, vertex);
		}
		agent.setPath(path);
	}

	@Test
	void getNextVertexID() {
		for (long i = -3; i <= end + 4; i++) {
			long nextID = i < 0 ? 0 : i > end ? end : i + 1,
				nextIDPrediction = agent.getNextVertexID(i);
			System.out.println(i + "; " + nextID + "; " + nextIDPrediction);
			assert nextIDPrediction == nextID;
		}
	}

	@Test
	void getPreviousVertexID() {
	}

	@Test
	void computeNextXY() {
		for (long i = 0; i <= end; i++) {
			agent.computeNextXY(i, vertices);
			assert agent.getY() == (i % 2 == 0 ? 2 * i + 1 : 2 * i);
			System.out.println(agent.getY());
		}
	}
}