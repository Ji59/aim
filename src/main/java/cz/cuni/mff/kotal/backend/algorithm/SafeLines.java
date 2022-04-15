package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static cz.cuni.mff.kotal.helpers.MyNumberOperations.*;

public class SafeLines implements Algorithm {
	protected final SimulationGraph graph;
	protected final Map<Long, Map<Long, Agent>> stepOccupiedVertices = new HashMap<>();
	protected final Map<Integer, List<Vertex>> directionExits = new HashMap<>();
	protected final Map<Long, PriorityQueue<VertexDistance>> verticesDistances = new HashMap<>();

	public SafeLines(SimulationGraph graph) {
		this.graph = graph;

		List<GraphicalVertex> sortedGraphicalVertices = graph.getVertices().parallelStream().map(GraphicalVertex.class::cast).sorted(Comparator.comparingLong(Vertex::getID)).toList();
		sortedGraphicalVertices.forEach(v -> verticesDistances.put(v.getID(), new PriorityQueue<>(sortedGraphicalVertices.size())));
		sortedGraphicalVertices.parallelStream().forEach(v0 -> {
			List<VertexDistance> v0Distances = sortedGraphicalVertices.stream()
				.takeWhile(v1 -> v1.getID() <= v0.getID())
				.map(v1 -> {
					double distance = distance(v0.getX(), v0.getY(), v1.getX(), v1.getY());
					verticesDistances.get(v1.getID()).add(new VertexDistance(v0.getID(), distance));
					return new VertexDistance(v1.getID(), distance);
				})
				.toList();
			verticesDistances.get(v0.getID()).addAll(v0Distances);
		});

		graph.getEntryExitVertices().forEach((key, value) -> directionExits.put(key, value.stream().filter(vertex -> vertex.getType().isExit()).toList()));
	}

	@Override
	public Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		stepOccupiedVertices.entrySet().removeIf(occupiedVerticesEntry -> occupiedVerticesEntry.getKey() < step);
		return agents.stream().filter(agent -> planAgent(agent, step) != null).toList();
	}

	@Override
	public Agent planAgent(Agent agent, long step) {
		List<Long> selectedPath = null;
		double agentPerimeter = agent.getAgentPerimeter();
		if (agent.getExit() < 0) {
			List<List<Long>> directionPaths = directionExits.get((int) agent.getExitDirection()).stream()
				.map(exit -> graph.getLines().get(agent.getEntry()).get(exit.getID()))
				.sorted(Comparator.comparingInt(List::size)).toList();
			for (List<Long> path : directionPaths) {
				if (validPath(step, path, agentPerimeter)) {
					selectedPath = path;
					break;
				}
			}
		} else {
			List<Long> path = graph.getLines().get(agent.getEntry()).get(agent.getExit());
			if (validPath(step, path, agentPerimeter)) {
				selectedPath = path;
			}
		}

		if (selectedPath == null) {
			return null;
		}
		agent.setPath(selectedPath, step);
		for (int i = 0; i < selectedPath.size(); i++) {
			stepOccupiedVertices.get(step + i).put(selectedPath.get(i), agent);
		}
		return agent;
	}

	boolean validPath(long step, @NotNull List<Long> path, double agentPerimeter) {
		for (int i = 0; i < path.size(); i++) {
			long actualStep = step + i;
			if (stepOccupiedVertices.containsKey(actualStep)) {
				Long vertexID = path.get(i);
				if (!safeVertex(actualStep, vertexID, agentPerimeter) ||
					(i >= 1 && stepOccupiedVertices.containsKey(actualStep - 1) && !safeStepTo(actualStep, vertexID, path.get(i - 1), agentPerimeter)) ||
					(i < path.size() - 1 && stepOccupiedVertices.containsKey(actualStep + 1) && !safeStepFrom(actualStep, vertexID, path.get(i + 1), agentPerimeter))
				) {
					return false;
				}
			} else {
				stepOccupiedVertices.put(actualStep, new HashMap<>());
			}
		}
		return true;
	}

	private boolean safeVertex(long step, long vertexID, double agentPerimeter) {
		return verticesDistances.get(vertexID).stream()
			.takeWhile(v -> v.distance() <= agentPerimeter)
			.noneMatch(v -> stepOccupiedVertices.get(step).containsKey(v.vertexID()));
	}

	boolean safeStepTo(long step, long vertexID, long previousVertexID, double agentPerimeter) {
		assert stepOccupiedVertices.containsKey(step - 1);
		Agent neighbour = stepOccupiedVertices.get(step - 1).get(vertexID);
		if (neighbour == null) {
			return true;
		}
		long neighbourVertexID = getNeighbourVertexID(step, neighbour);
		if (neighbourVertexID < 0) {
			return true;
		}
		return checkNeighbour(vertexID, previousVertexID, neighbourVertexID, agentPerimeter, neighbour);
	}

	boolean safeStepFrom(long step, long vertexID, long nextVertexID, double agentPerimeter) {
		assert stepOccupiedVertices.containsKey(step + 1);
		Agent neighbour = stepOccupiedVertices.get(step + 1).get(vertexID);
		if (neighbour == null) {
			return true;
		}
		long neighbourVertexID = getNeighbourVertexID(step, neighbour);
		if (neighbourVertexID < 0) {
			return true;
		}
		return checkNeighbour(vertexID, neighbourVertexID, nextVertexID, agentPerimeter, neighbour);
	}

	private long getNeighbourVertexID(long step, Agent neighbour) {
		long plannedTime = neighbour.getPlannedTime() >= 0 ? neighbour.getPlannedTime() : step;
		int neighbourStep = (int) (step - plannedTime);
		if (neighbourStep >= neighbour.getPath().size()) {
			return -1;
		}
		return neighbour.getPath().get(neighbourStep);
	}

	private boolean checkNeighbour(long vertexID, long adjacentVertexID, long neighbourVertexID, double agentPerimeter, Agent neighbour) {

		double neighbourPerimeter = neighbour.getAgentPerimeter();

		GraphicalVertex v = graph.getVertex(vertexID);
		GraphicalVertex agentAdjacentVertex = graph.getVertex(adjacentVertexID);
		GraphicalVertex neighbourAdjacentVertex = graph.getVertex(neighbourVertexID);

		double x = v.getX();
		double y = v.getY();
		double xA = agentAdjacentVertex.getX();
		double yA = agentAdjacentVertex.getY();
		double xN = neighbourAdjacentVertex.getX();
		double yN = neighbourAdjacentVertex.getY();

		double x0 = xA + xN - 2 * x;
		double y0 = yA + yN - 2 * y;
		double x1 = xA - x;
		double y1 = yA - y;

		if (doubleAlmostEqual(Math.abs(x0), 0, graph.getCellSize() / 0x100) && MyNumberOperations.doubleAlmostEqual(Math.abs(y0), 0, graph.getCellSize() / 0x100)) {
			double perimetersSum = agentPerimeter + neighbourPerimeter;
			return x1 * x1 + y1 * y1 > perimetersSum * perimetersSum;
		}

		double closestTime = (x1 * x0 + y1 * y0) / (x0 * x0 + y0 * y0);
		double xDiffAtT = closestTime * x0 - x1;
		assert doubleAlmostEqual(Math.abs(xDiffAtT), Math.abs((closestTime * x + (1 - closestTime) * xA) - (closestTime * xN + (1 - closestTime) * x)), 1e-7);
		xDiffAtT *= xDiffAtT;
		double yDiffAtT = closestTime * y0 - y1;
		assert doubleAlmostEqual(Math.abs(yDiffAtT), Math.abs((closestTime * y + (1 - closestTime) * yA) - (closestTime * yN + (1 - closestTime) * y)), 1e-7);
		yDiffAtT *= yDiffAtT;

		double perimetersSquared = neighbourPerimeter + agentPerimeter;
		perimetersSquared *= perimetersSquared;

		return xDiffAtT + yDiffAtT > perimetersSquared;
	}

	Map<Long, Map<Long, Agent>> getStepOccupiedVertices() {
		return stepOccupiedVertices;
	}

	protected record VertexDistance(long vertexID, double distance) implements Comparable<VertexDistance> {

		@Override
		public int compareTo(@NotNull VertexDistance o) {
			return Double.compare(distance, o.distance);
		}
	}
}
