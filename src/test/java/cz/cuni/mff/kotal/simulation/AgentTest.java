package cz.cuni.mff.kotal.simulation;


import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import javafx.util.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.cuni.mff.kotal.MyNumberOperations.doubleAlmostEqual;


class AgentTest {
	private final long id = 0, l = 1, w = 0,
		start = 0, end = 7;
	private final double speed = 1,
		x = 0, y = 1,
		arrivalTime = 0, nonTrivialArrivalTime = 0.75;
	private Agent agent, nonTrivialAgent;
	private List<Long> path; // X-Y: 0-1, 3-2, 4-5, 7-6, 8-9, 11-10, 12-13, 15-14
	private Map<Long, Vertex> vertices;

	@BeforeEach
	void startUp() {
		agent = new Agent(id, start, end, speed, arrivalTime, l, w, x, y);
		path = new ArrayList<>();
		vertices = new HashMap<>();
		for (long i = start; i <= end; i++) {
			path.add(i);
			Vertex vertex = new GraphicalVertex(i, i % 2 == 0 ? 2 * i : 2 * i + 1, i % 2 == 0 ? 2 * i + 1 : 2 * i);
			vertices.put(i, vertex);
		}
		agent.setPath(path);

		nonTrivialAgent = new Agent(1, 0, 4, 1.25, nonTrivialArrivalTime, 1, 1, 0, 1);
		nonTrivialAgent.setPath(path.subList(0, 5));
	}

	@Test
	void testGetPreviousNextVertexIDs() {
		for (long i = -3; i <= end + 4; i++) {
			try {
				long nextID = i < 0 ? 0 : i >= end ? end : i + 1,
					previousID = i == end ? nextID : nextID > 0 ? nextID - 1 : 0;
				Pair<Long, Long> IDsPrediction = agent.getPreviousNextVertexIDs(i);
				assert IDsPrediction.getKey() == previousID;
				assert IDsPrediction.getValue() == nextID;
			} catch (IndexOutOfBoundsException e) {
				assert i >= end;
			}
		}

		for (double i = -3.42; i <= end + 4; i++) {
			try {
				long nextID = i < 0 ? 0 : i > end ? end : Math.round(i),
					previousID = nextID > 0 ? nextID - 1 : 0;
				Pair<Long, Long> IDsPrediction = agent.getPreviousNextVertexIDs(i);
				assert IDsPrediction.getKey() == previousID;
				assert IDsPrediction.getValue() == nextID;
			} catch (IndexOutOfBoundsException e) {
				assert i >= end;
			}
		}

		for (double i = -3.69; i <= end + 4; i++) {
			try {
				long nextID = i < 0 ? 0 : i > end ? end : Math.round(i) + 1,
					previousID = nextID > 0 ? nextID - 1 : 0;
				Pair<Long, Long> IDsPrediction = agent.getPreviousNextVertexIDs(i);
				assert IDsPrediction.getKey() == previousID;
				assert IDsPrediction.getValue() == nextID;
			} catch (IndexOutOfBoundsException e) {
				assert i >= end;
			}
		}

		Pair<Long, Long> IDsPrediction = nonTrivialAgent.getPreviousNextVertexIDs(0.7);
		assert IDsPrediction.getKey() == 0 && IDsPrediction.getValue() == 1;

		IDsPrediction = nonTrivialAgent.getPreviousNextVertexIDs(0.8);
		assert IDsPrediction.getKey() == 1 && IDsPrediction.getValue() == 2;

		IDsPrediction = nonTrivialAgent.getPreviousNextVertexIDs(0.9);
		assert IDsPrediction.getKey() == 1 && IDsPrediction.getValue() == 2;

		IDsPrediction = nonTrivialAgent.getPreviousNextVertexIDs(1.55);
		assert IDsPrediction.getKey() == 1 && IDsPrediction.getValue() == 2;

		IDsPrediction = nonTrivialAgent.getPreviousNextVertexIDs(2);
		assert IDsPrediction.getKey() == 2 && IDsPrediction.getValue() == 3;

		IDsPrediction = nonTrivialAgent.getPreviousNextVertexIDs(2.39);
		assert IDsPrediction.getKey() == 2 && IDsPrediction.getValue() == 3;

		IDsPrediction = nonTrivialAgent.getPreviousNextVertexIDs(2.4);
		assert IDsPrediction.getKey() == 3 && IDsPrediction.getValue() == 4;

		IDsPrediction = nonTrivialAgent.getPreviousNextVertexIDs(3);
		assert IDsPrediction.getKey() == 3 && IDsPrediction.getValue() == 4;

		IDsPrediction = nonTrivialAgent.getPreviousNextVertexIDs(3.2);
		assert IDsPrediction.getKey() == 4 && IDsPrediction.getValue() == 4;

		try {
			nonTrivialAgent.getPreviousNextVertexIDs(3.3);
			assert false;
		} catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}

	@Test
	void testComputeNextXY() {
		double proximity = 0.0001;

		for (long i = 0; i <= end + 1; i++) {
			try {
				agent.computeNextXY(i, vertices);
				assert doubleAlmostEqual(agent.getY(), (i % 2 == 0 ? 2 * i + 1 : 2 * i), proximity);
				assert doubleAlmostEqual(agent.getX(), (i % 2 == 0 ? 2 * i : 2 * i + 1), proximity);
			} catch (IndexOutOfBoundsException e) {
				assert i > end;
			}
		}

		for (double i = 0; i < end; i++) {
			agent.computeNextXY(i + 0.5, vertices);
			assert doubleAlmostEqual(agent.getY(), 2 * i + 1.5, proximity); // 1.5 3.5 5.5 7.5 9.5 11.5 13.5
			assert doubleAlmostEqual(agent.getX(), 2 * i + 1.5, proximity); // 1.5 3.5 5.5 7.5 9.5 11.5 13.5
		}

		nonTrivialAgent.computeNextXY(0.1 + nonTrivialArrivalTime, vertices);
		assert doubleAlmostEqual(nonTrivialAgent.getX(), 0.375, proximity);
		assert doubleAlmostEqual(nonTrivialAgent.getY(), 1.125, proximity);

		nonTrivialAgent.computeNextXY(0.5 + nonTrivialArrivalTime, vertices);
		assert doubleAlmostEqual(nonTrivialAgent.getX(), 1.875, proximity);
		assert doubleAlmostEqual(nonTrivialAgent.getY(), 1.625, proximity);

		nonTrivialAgent.computeNextXY(1 + nonTrivialArrivalTime, vertices);
		assert doubleAlmostEqual(nonTrivialAgent.getX(), 3.25, proximity);
		assert doubleAlmostEqual(nonTrivialAgent.getY(), 2.75, proximity);

		nonTrivialAgent.computeNextXY(1.6 + nonTrivialArrivalTime, vertices);
		assert doubleAlmostEqual(nonTrivialAgent.getX(), 4, proximity);
		assert doubleAlmostEqual(nonTrivialAgent.getY(), 5, proximity);

		nonTrivialAgent.computeNextXY(2.15 + nonTrivialArrivalTime, vertices);
		assert doubleAlmostEqual(nonTrivialAgent.getX(), 4 + (2.15 / 0.8 - 2) * 3, proximity);
		assert doubleAlmostEqual(nonTrivialAgent.getY(), 5 + 2.15 / 0.8 - 2, proximity);

		nonTrivialAgent.computeNextXY(2.8 + nonTrivialArrivalTime, vertices);
		assert doubleAlmostEqual(nonTrivialAgent.getX(), 7.5, proximity);
		assert doubleAlmostEqual(nonTrivialAgent.getY(), 7.5, proximity);

		try {
			nonTrivialAgent.computeNextXY(3.3 + nonTrivialArrivalTime, vertices);
			assert false;
		} catch (IndexOutOfBoundsException e) {
			assert true;
		}
	}
}