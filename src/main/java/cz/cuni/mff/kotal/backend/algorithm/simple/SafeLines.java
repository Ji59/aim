package cz.cuni.mff.kotal.backend.algorithm.simple;

import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.backend.algorithm.LinkGraph;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.helpers.MyNumberOperations.*;

public class SafeLines implements Algorithm {
	protected static final String SAFE_DISTANCE_NAME = "Safe distance";
	protected static final double SAFE_DISTANCE_DEF = 0.;
	public static final Map<String, Object> PARAMETERS = Map.of(SAFE_DISTANCE_NAME, SAFE_DISTANCE_DEF);
	protected final Map<Integer, Set<Integer>> directionExits = new HashMap<>();


	private final double safeDistance;
	protected final SimulationGraph graph;
	protected final Map<Long, Map<Integer, Agent>> stepOccupiedVertices = new HashMap<>();
	protected final Map<Integer, PriorityQueue<VertexDistance>> verticesDistances = new HashMap<>();
	protected double largestAgentPerimeter = 0;

	public SafeLines(SimulationGraph graph) {
		this.graph = graph;

		List<GraphicalVertex> sortedGraphicalVertices = graph.getVerticesSet().stream().map(GraphicalVertex.class::cast).sorted(Comparator.comparingInt(Vertex::getID)).toList();
		sortedGraphicalVertices.forEach(v -> verticesDistances.put(v.getID(), new PriorityQueue<>(sortedGraphicalVertices.size())));
		sortedGraphicalVertices.parallelStream().forEach(v0 -> {
			final int v0ID = v0.getID();
			List<VertexDistance> v0Distances = sortedGraphicalVertices.stream()
				.takeWhile(v1 -> v1.getID() <= v0ID)
				.map(v1 -> {
					final double distance = distance(v0.getX(), v0.getY(), v1.getX(), v1.getY());
					final int v1ID = v1.getID();
					if (v0ID != v1ID) {
						verticesDistances.get(v1ID).add(new VertexDistance(v0ID, distance));
					}
					return new VertexDistance(v1ID, distance);
				})
				.toList();
			verticesDistances.get(v0ID).addAll(v0Distances);
		});

		graph.getEntryExitVertices().forEach((key, value) -> {
			Set<Integer> exits = value.stream()
				.filter(vertex -> vertex.getType().isExit())
				.map(Vertex::getID)
				.collect(Collectors.toSet());
			directionExits.put(key, exits);
		});

		safeDistance = AlgorithmMenuTab2.getDoubleParameter(SAFE_DISTANCE_NAME, SAFE_DISTANCE_DEF) * graph.getCellSize();
	}

	@Override
	public Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		filterStepOccupiedVertices(step);
		return agents.stream().filter(agent -> planAgent(agent, step) != null).toList();
	}

	@Override
	public Agent planAgent(Agent agent, long step) {
		List<Integer> selectedPath = null;
		double agentPerimeter = agent.getAgentPerimeter();
		if (agent.getExit() < 0) {
			List<List<Integer>> directionPaths = directionExits.get(agent.getExitDirection()).stream()
				.map(exit -> graph.getLines().get(agent.getEntry()).get(exit))
				.sorted(Comparator.comparingInt(List::size)).toList();
			for (List<Integer> path : directionPaths) {
				if (validPath(step, path, agentPerimeter)) {
					selectedPath = path;
					break;
				}
			}
		} else {
			List<Integer> path = graph.getLines().get(agent.getEntry()).get(agent.getExit());
			if (validPath(step, path, agentPerimeter)) {
				selectedPath = path;
			}
		}

		if (selectedPath == null) {
			return null;
		}
		agent.setPath(selectedPath, step);
		addPlannedAgent(agent);

		return agent;
	}

	protected Set<Integer> getExits(Agent agent) {
		return agent.getExit() < 0 ? directionExits.get(agent.getExitDirection()) : Collections.singleton(agent.getExit());
	}

	@Override
	public Agent planAgent(Agent agent, int entryID, Set<Integer> exitsID, long step) {
		// FIXME
		throw new UnsupportedOperationException();
	}

	@Override
	public void addPlannedAgent(Agent agent) {
		List<Integer> path = agent.getPath();
		for (int i = 0; i < path.size(); i++) {
			long step = agent.getPlannedTime() + i;
			stepOccupiedVertices.putIfAbsent(step, new HashMap<>());
			stepOccupiedVertices.get(step).put(path.get(i), agent);
		}

		if (graph instanceof LinkGraph linkGraph) {
			path = linkGraph.getRealPath(path);
			agent.setPath(path, agent.getPlannedTime());
		}

		setLargestAgentPerimeter(agent);
	}

	protected void setLargestAgentPerimeter(Agent agent) {
		double agentPerimeter = agent.getAgentPerimeter();
		if (agentPerimeter > largestAgentPerimeter) {
			largestAgentPerimeter = agentPerimeter;
		}
	}

	@Override
	public void addPlannedPath(Agent agent, List<Integer> path, long step) {
		for (int i = 0; i < path.size(); i++) {
			stepOccupiedVertices.putIfAbsent(step + i, new HashMap<>());
			stepOccupiedVertices.get(step + i).put(path.get(i), agent);
		}

		setLargestAgentPerimeter(agent);
	}

	public boolean validPath(long step, List<Integer> path, double agentPerimeter) {
		for (int i = 0; i < path.size(); i++) {
			long actualStep = step + i;
			if (stepOccupiedVertices.containsKey(actualStep)) {
				int vertexID = path.get(i);
				if (!safeVertex(actualStep, vertexID, agentPerimeter) ||
					(i >= 1 && !safeStepTo(actualStep, vertexID, path.get(i - 1), agentPerimeter)) ||
					(i < path.size() - 1 && !safeStepFrom(actualStep, vertexID, path.get(i + 1), agentPerimeter))
				) {
					return false;
				}
			} else {
				stepOccupiedVertices.put(actualStep, new HashMap<>());
			}
		}
		return true;
	}

	protected boolean safeVertex(long step, int vertexID, double agentPerimeter) {
		return safeVertex(stepOccupiedVertices, step, vertexID, agentPerimeter);
	}

	protected boolean safeVertex(final Map<Long, Map<Integer, Agent>> illegalMoveTable, long step, int vertexID, double agentPerimeter) {
		if (!illegalMoveTable.containsKey(step)) {
			return true;
		}

		final double safePerimeter = agentPerimeter + safeDistance;
		return verticesDistances.get(vertexID).stream()
			.takeWhile(v -> v.distance() <= safePerimeter + largestAgentPerimeter)
			.allMatch(v -> {
				Agent neighbour = illegalMoveTable.get(step).get(v.vertexID());
				return neighbour == null || neighbour.getAgentPerimeter() + safePerimeter < v.distance;
			});
	}

	protected boolean safeVertex(final Map<Long, Map<Integer, Set<Agent>>> illegalMoveTable, long step, int vertexID, double agentPerimeter) {
		if (!illegalMoveTable.containsKey(step)) {
			return true;
		}

		final double safePerimeter = agentPerimeter + safeDistance;
		return verticesDistances.get(vertexID).stream()
			.takeWhile(v -> v.distance() <= safePerimeter + largestAgentPerimeter)
			.allMatch(v -> {
				Agent neighbour = illegalMoveTable.get(step).get(v.vertexID()).stream().max(Comparator.comparing(Agent::getAgentPerimeter)).orElse(null);
				return neighbour == null || neighbour.getAgentPerimeter() + safePerimeter < v.distance;
			});
	}

	@Nullable
	protected Agent collisionAgentAtVertex(Map<Long, Map<Integer, Agent>> illegalMoveTable, long step, int vertexID, double agentPerimeter) {
		if (!illegalMoveTable.containsKey(step)) {
			return null;
		}

		final double safePerimeter = agentPerimeter + safeDistance;
		return verticesDistances.get(vertexID).stream()  // FIXME add other agent size to computation
			.takeWhile(v -> v.distance() <= safePerimeter + largestAgentPerimeter)
			.map(v -> {
				Agent neighbour = illegalMoveTable.get(step).get(v.vertexID());
				if (neighbour == null || neighbour.getAgentPerimeter() + safePerimeter < v.distance) {
					return null;
				}
				return neighbour;
			})
			.filter(Objects::nonNull)
			.findFirst().orElse(null);
	}

	protected int[] conflictVertices(int vertexID, double agentsPerimeter) {
		return verticesDistances.get(vertexID).stream().takeWhile(v -> v.distance <= agentsPerimeter + safeDistance).mapToInt(VertexDistance::vertexID).toArray();
	}

	protected Set<Integer> conflictVerticesSet(int vertexID, double agentsPerimeter) {
		return verticesDistances.get(vertexID).stream().takeWhile(v -> v.distance <= agentsPerimeter + safeDistance).map(VertexDistance::vertexID).collect(Collectors.toSet());
	}

	public boolean safeStepTo(long step, int vertexID, int previousVertexID, double agentPerimeter) {
		return safeStepTo(stepOccupiedVertices, step, vertexID, previousVertexID, agentPerimeter);
	}

	public boolean safeStepTo(Map<Long, Map<Integer, Agent>> illegalMoveTable, long step, int vertexID, int previousVertexID, double agentPerimeter) {
		if (!illegalMoveTable.containsKey(step - 1)) {
			return true;
		}

		Agent neighbour = illegalMoveTable.get(step - 1).get(vertexID);
		if (neighbour == null) {
			return true;
		}
		int neighbourVertexID = getNeighbourVertexID(step, neighbour);
		if (neighbourVertexID < 0) {
			return true;
		}
		return checkNeighbour(vertexID, previousVertexID, neighbourVertexID, agentPerimeter, neighbour.getAgentPerimeter());
	}

	public boolean safeStepFrom(long step, int vertexID, int nextVertexID, double agentPerimeter) {
		return safeStepFrom(stepOccupiedVertices, step, vertexID, nextVertexID, agentPerimeter);
	}

	public boolean safeStepFrom(Map<Long, Map<Integer, Agent>> illegalMoveTable, long step, int vertexID, int nextVertexID, double agentPerimeter) {
		if (!illegalMoveTable.containsKey(step + 1)) {
			return true;
		}
		Agent neighbour = illegalMoveTable.get(step + 1).get(vertexID);
		if (neighbour == null) {
			return true;
		}
		int neighbourVertexID = getNeighbourVertexID(step, neighbour);
		if (neighbourVertexID < 0) {
			return true;
		}
		return checkNeighbour(vertexID, neighbourVertexID, nextVertexID, agentPerimeter, neighbour.getAgentPerimeter());
	}

	protected int getNeighbourVertexID(long step, Agent neighbour) {
		final long plannedTime = neighbour.getPlannedTime() >= 0 ? neighbour.getPlannedTime() : step;
		final int neighbourStep = (int) (step - plannedTime);
		if (neighbourStep >= neighbour.getPath().size()) {
			return -1;
		}

		int neighbourVertexID = neighbour.getPath().get(neighbourStep);
		if (graph instanceof LinkGraph linkGraph) {
			return linkGraph.getLinkID(neighbourVertexID);
		}
		return neighbourVertexID;
	}

	protected boolean checkNeighbour(int vertexID, int adjacentVertexID, int neighbourVertexID, double agentPerimeter, double neighbourPerimeter) {
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

		double perimetersSquared = neighbourPerimeter + agentPerimeter + safeDistance;
		perimetersSquared *= perimetersSquared;

		if (doubleAlmostEqual(Math.abs(x0), 0, graph.getCellSize() / 0x100) && MyNumberOperations.doubleAlmostEqual(Math.abs(y0), 0, graph.getCellSize() / 0x100)) {
			return x1 * x1 + y1 * y1 > perimetersSquared;
		}

		double closestTime = (x1 * x0 + y1 * y0) / (x0 * x0 + y0 * y0);
		double xDiffAtT = closestTime * x0 - x1;
		assert doubleAlmostEqual(Math.abs(xDiffAtT), Math.abs((closestTime * x + (1 - closestTime) * xA) - (closestTime * xN + (1 - closestTime) * x)), 1e-7);
		xDiffAtT *= xDiffAtT;
		double yDiffAtT = closestTime * y0 - y1;
		assert doubleAlmostEqual(Math.abs(yDiffAtT), Math.abs((closestTime * y + (1 - closestTime) * yA) - (closestTime * yN + (1 - closestTime) * y)), 1e-7);
		yDiffAtT *= yDiffAtT;

		return xDiffAtT + yDiffAtT > perimetersSquared;
	}

	public Map<Long, Map<Integer, Agent>> getStepOccupiedVertices() {
		return stepOccupiedVertices;
	}

	protected void filterStepOccupiedVertices(long step) {
		Iterator<Map.Entry<Long, Map<Integer, Agent>>> it = stepOccupiedVertices.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, Map<Integer, Agent>> stepVertices = it.next();
			long occupiedStep = stepVertices.getKey();
			if (occupiedStep < step) {
				it.remove();
			}
		}
	}

	public boolean inCollision(Collection<Agent> agents0, Collection<Agent> agents1) {
		for (Agent agent0 : agents0) {
			for (Agent agent1 : agents1) {
				if (inCollision(agent0, agent1)) {
					System.out.println(agent0.getId() + " collides with " + agent1.getId());
					return true;
				}
			}
		}

		return false;
	}

	public boolean inCollision(Agent agent0, Agent agent1) {
		final long agent0PlannedTime = agent0.getPlannedTime();
		final long agent1PlannedTime = agent1.getPlannedTime();
		final List<Integer> agent0Path = agent0.getPath();
		final List<Integer> agent1Path = agent1.getPath();
		final double agent0Perimeter = agent0.getAgentPerimeter();
		final double agent1Perimeter = agent1.getAgentPerimeter();
		final double agentsPerimeter = agent0Perimeter + agent1Perimeter;

		final long startStep = Math.min(agent0PlannedTime, agent1PlannedTime);
		final long endStep = Math.min(agent0PlannedTime + agent0Path.size(), agent1PlannedTime + agent1Path.size());

		if (endStep < startStep) {
			return false;
		}

		ListIterator<Integer> itP0 = agent0Path.listIterator((int) (startStep - agent0PlannedTime));
		ListIterator<Integer> itP1 = agent1Path.listIterator((int) (startStep - agent1PlannedTime));

		int vertexP0 = itP0.next();
		int vertexP1 = itP1.next();
		int vertexP0Last;
		int vertexP1Last;

		if (conflictVerticesSet(vertexP0, agentsPerimeter).contains(vertexP1)) {
			return true;
		}

		while (itP0.hasNext() && itP1.hasNext()) {
			vertexP0Last = vertexP0;
			vertexP1Last = vertexP1;

			vertexP0 = itP0.next();
			vertexP1 = itP1.next();

			if (conflictVerticesSet(vertexP0, agentsPerimeter).contains(vertexP1)) {
				return true;
			}

			if (vertexP0 == vertexP1Last) {
				if (!checkNeighbour(vertexP0, vertexP0Last, vertexP1, agent0Perimeter, agent1Perimeter)) {
					return true;
				}
			}

			if (vertexP1 == vertexP0Last) {
				if (!checkNeighbour(vertexP1, vertexP1Last, vertexP0, agent1Perimeter, agent0Perimeter)) {
					return true;
				}
			}
		}

		return false;
	}

	protected record VertexDistance(int vertexID, double distance) implements Comparable<VertexDistance> {

		@Override
		public int compareTo(@NotNull VertexDistance o) {
			return Double.compare(distance, o.distance);
		}
	}
}
