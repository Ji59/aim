package cz.cuni.mff.kotal.backend.algorithm.simple;

import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.backend.algorithm.LinkGraph;
import cz.cuni.mff.kotal.frontend.menu.tabs.AgentParametersMenuTab4;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import cz.cuni.mff.kotal.simulation.graph.VertexWithDirectionParent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.helpers.MyNumberOperations.doubleAlmostEqual;

/**
 * Algorithm implementing simple strategy.
 * Agent can drive only on predetermined lanes.
 * Agents are planned one ofter other until all are planned or rejected.
 */
public class SafeLanes implements Algorithm {
	public static final @NotNull String SAFE_DISTANCE_NAME = "Safe distance";
	public static final double SAFE_DISTANCE_DEF = 0.;
	public static final @NotNull Map<String, Object> PARAMETERS = Map.of(SAFE_DISTANCE_NAME, SAFE_DISTANCE_DEF);
	protected final @NotNull Map<Integer, Set<Integer>> directionExits = new HashMap<>();
	protected final @NotNull SimulationGraph graph;
	protected final @NotNull Map<Long, Map<Integer, Agent>> stepOccupiedVertices = new HashMap<>();
	protected final @NotNull Map<Integer, List<Integer>>[] lanes;
	protected final double safeDistance;
	protected final double largestAgentPerimeter = MyNumberOperations.perimeter(AgentParametersMenuTab4.getMaximalLength(), AgentParametersMenuTab4.getMaximalWidth());
	protected boolean stopped = false;

	public SafeLanes(@NotNull SimulationGraph graph) {
		this.graph = graph;

		graph.getEntryExitVertices().forEach((key, value) -> {
			@NotNull Set<Integer> exits = value.stream()
				.filter(vertex -> vertex.getType().isExit())
				.map(Vertex::getID)
				.collect(Collectors.toSet());
			directionExits.put(key, exits);
		});

		safeDistance = AlgorithmMenuTab2.getDoubleParameter(SAFE_DISTANCE_NAME, SAFE_DISTANCE_DEF) * graph.getCellSize();

		lanes = createLanes();
	}

	@TestOnly
	protected SafeLanes(@NotNull SimulationGraph graph, double safeDistance) {
		this.graph = graph;

		graph.getEntryExitVertices().forEach((key, value) -> {
			@NotNull Set<Integer> exits = value.stream()
				.filter(vertex -> vertex.getType().isExit())
				.map(Vertex::getID)
				.collect(Collectors.toSet());
			directionExits.put(key, exits);
		});

		this.safeDistance = safeDistance;
		lanes = createLanes();
	}

	@Override
	public @Nullable Agent planAgent(@NotNull Agent agent, int entryID, @NotNull Set<Integer> exitsID, long step) {
		if (exitsID.isEmpty()) {
			exitsID = getExits(agent);
		}
		@Nullable List<Integer> selectedPath = null;
		double agentPerimeter = agent.getAgentPerimeter();
		@NotNull List<List<Integer>> directionPaths = exitsID.stream()
			.map(exit -> lanes[agent.getEntry()].get(exit))
			.sorted(Comparator.comparingInt(List::size)).toList();
		for (@NotNull List<Integer> path : directionPaths) {
			if (validPath(step, path, agentPerimeter)) {
				selectedPath = path;
				break;
			}
		}

		if (selectedPath == null) {
			return null;
		}
		agent.setPath(selectedPath, step);
		addPlannedAgent(agent);

		return agent;
	}

	public Set<Integer> getExits(@NotNull Agent agent) {
		return agent.getExit() < 0 ? directionExits.get(agent.getExitDirection()) : Collections.singleton(agent.getExit());
	}

	@Override
	public void addPlannedAgent(@NotNull Agent agent) {
		List<Integer> path = agent.getPath();
		for (int i = 0; i < path.size(); i++) {
			long step = agent.getPlannedStep() + i;
			stepOccupiedVertices.putIfAbsent(step, new HashMap<>());
			stepOccupiedVertices.get(step).put(path.get(i), agent);
		}

		if (graph instanceof LinkGraph linkGraph) {
			path = linkGraph.getRealPath(path);
			agent.setPath(path, agent.getPlannedStep());
		}
	}

	@Override
	public void addPlannedPath(@NotNull Agent agent, @NotNull List<Integer> path, long step) {
		for (int i = 0; i < path.size(); i++) {
			stepOccupiedVertices.putIfAbsent(step + i, new HashMap<>());
			stepOccupiedVertices.get(step + i).put(path.get(i), agent);
		}
	}

	@Override
	public void stop() {
		stopped = true;
	}


	public boolean validPath(long step, @NotNull List<Integer> path, double agentPerimeter) {
		if (stopped) {
			return false;
		}

		for (int i = 0; i < path.size() - 1; i++) {
			long actualStep = step + i;
			if (stepOccupiedVertices.containsKey(actualStep)) {
				int vertexID = path.get(i);
				int nextVertexID = path.get(i + 1);
				if (!safeStepTo(actualStep, vertexID, nextVertexID, agentPerimeter)) {
					return false;
				}
			} else {
				stepOccupiedVertices.put(actualStep, new HashMap<>());
			}
		}
		return true;
	}

	@Deprecated
	protected boolean safeVertex(final long step, final int vertexID, final double agentPerimeter) {
		return !(stepOccupiedVertices.containsKey(step) && stepOccupiedVertices.get(step).containsKey(vertexID));
//		if (!stepOccupiedVertices.containsKey(step)) {
//			return true;
//		}
//
//		final double safePerimeter = agentPerimeter + safeDistance;
//		return verticesDistances.get(vertexID).stream()
//			.takeWhile(v -> v.distance() <= safePerimeter + largestAgentPerimeter)
//			.allMatch(v -> {
//				Agent neighbour = stepOccupiedVertices.get(step).get(v.vertexID());
//				return neighbour == null || neighbour.getAgentPerimeter() + safePerimeter < v.distance;
//			});
	}

	@Deprecated
	protected boolean safeVertex(final @NotNull Map<Long, Map<Integer, Set<Agent>>> illegalMovesTable, long step, int vertexID, double agentPerimeter) {
		return true;
//		if (!illegalMovesTable.containsKey(step)) {
//			return true;
//		}
//
////		final double safePerimeter = agentPerimeter + safeDistance;
////		return verticesDistances.get(vertexID).stream()
////			.takeWhile(v -> v.distance() <= safePerimeter + largestAgentPerimeter)
////			.allMatch(v -> {
////				Collection<Agent> neighbours = illegalMovesTable.get(step).get(v.vertexID());
////				return neighbours == null || neighbours.stream().mapToDouble(Agent::getAgentPerimeter).max().orElse(0) + safePerimeter < v.distance;
////			});
	}

	@Deprecated
	@Nullable
	protected Agent collisionAgentAtVertex(@NotNull Map<Long, Map<Integer, Agent>> illegalMoveTable, long step, int vertexID, double agentPerimeter) {
		return null;
//		if (!illegalMoveTable.containsKey(step)) {
//			return null;
//		}
//
//		final double safePerimeter = agentPerimeter + safeDistance;
//		return verticesDistances.get(vertexID).stream()
//			.takeWhile(v -> v.distance() <= safePerimeter + largestAgentPerimeter)
//			.map(v -> {
//				Agent neighbour = illegalMoveTable.get(step).get(v.vertexID());
//				if (neighbour == null || neighbour.getAgentPerimeter() + safePerimeter < v.distance) {
//					return null;
//				}
//				return neighbour;
//			})
//			.filter(Objects::nonNull)
//			.findFirst().orElse(null);
	}

	protected int[] conflictVertices(int vertexID, double agentsPerimeter) {
		return null;
//		return verticesDistances.get(vertexID).stream().takeWhile(v -> v.distance <= agentsPerimeter + safeDistance).mapToInt(VertexDistance::vertexID).toArray();
	}

	protected @NotNull Set<Integer> conflictVerticesSet(int vertexID, double agentsPerimeter) {
		return null;
//		return verticesDistances.get(vertexID).stream().takeWhile(v -> v.distance <= agentsPerimeter + safeDistance).map(VertexDistance::vertexID).collect(Collectors.toSet());
	}

	public boolean safeStepTo(long step, int vertexID, int nextVertexID, double agentPerimeter) {
		if (!(stepOccupiedVertices.containsKey(step) && stepOccupiedVertices.containsKey(step + 1)) ||
			stepOccupiedVertices.get(step).isEmpty() || stepOccupiedVertices.get(step + 1).isEmpty()
		) {
			return true;
		}
		if (stepOccupiedVertices.get(step).containsKey(vertexID) || stepOccupiedVertices.get(step + 1).containsKey(nextVertexID)) {
			return false;
		}

		@NotNull final Map<Integer, Agent> occupiedVertices = stepOccupiedVertices.get(step);
		@NotNull final Map<Integer, Double>[] transferMap = graph.getTravelDistances()[vertexID].get(nextVertexID);
		final double agentSafeDistance = agentPerimeter + safeDistance;
		final double largestSafePerimeter = agentSafeDistance + largestAgentPerimeter;
		for (SimulationGraph.VertexDistance vertexDistance : graph.getSortedVertexVerticesDistances()[vertexID].get(nextVertexID)) {
			if (vertexDistance.distance() > largestSafePerimeter) {
				return true;
			}

			final int agentVertex = vertexDistance.vertexID();
			if (!occupiedVertices.containsKey(agentVertex)) {
				continue;
			}
			if (agentVertex == vertexID) {
				return false;
			}

			@NotNull final Agent plannedAgent = occupiedVertices.get(agentVertex);
			final int plannedAgentTime = (int) (step - plannedAgent.getPlannedStep() + 1);
			@NotNull List<Integer> path = plannedAgent.getPath();
			if (plannedAgentTime >= path.size()) {
				continue;
			}

			final int agentNextVertex = path.get(plannedAgentTime);
			if (agentNextVertex == nextVertexID) {
				return false;
			}

			final double perimeters = agentSafeDistance + plannedAgent.getAgentPerimeter();
			if (transferMap[agentVertex].get(agentNextVertex) <= perimeters) {
				return false;
			}
		}

		return true;
	}

	@Deprecated
	public boolean safeStepTo(@NotNull Map<Long, Map<Integer, Set<Agent>>> illegalMovesTable, long step, int vertexID, int previousVertexID, double agentPerimeter) {
		if (!illegalMovesTable.containsKey(step - 1) || !illegalMovesTable.get(step - 1).containsKey(vertexID)) {
			return true;
		}

		return illegalMovesTable.get(step - 1).get(vertexID).stream().allMatch(neighbour -> {
			int neighbourVertexID = getNeighbourVertexID(step, neighbour);
			if (neighbourVertexID < 0) {
				return true;
			}
			return checkNeighbour(vertexID, previousVertexID, neighbourVertexID, agentPerimeter, neighbour.getAgentPerimeter());
		});
	}

	public boolean safeStepFrom(long step, int vertexID, int nextVertexID, double agentPerimeter) {
		return true;

//		if (!stepOccupiedVertices.containsKey(step + 1)) {
//			return true;
//		}
//
//		final Agent neighbour = stepOccupiedVertices.get(step + 1).get(vertexID);
//		if (neighbour == null) {
//			return true;
//		}
//		int neighbourVertexID = getNeighbourVertexID(step, neighbour);
//		if (neighbourVertexID < 0) {
//			return true;
//		}
//		return checkNeighbour(vertexID, neighbourVertexID, nextVertexID, agentPerimeter, neighbour.getAgentPerimeter());
	}

	public boolean safeStepFrom(@NotNull Map<Long, Map<Integer, Set<Agent>>> illegalMovesTable, long step, int vertexID, int nextVertexID, double agentPerimeter) {
		if (!illegalMovesTable.containsKey(step + 1) || !illegalMovesTable.get(step + 1).containsKey(vertexID)) {
			return true;
		}
		return illegalMovesTable.get(step + 1).get(vertexID).stream().allMatch(neighbour -> {
			final int neighbourVertexID = getNeighbourVertexID(step, neighbour);
			if (neighbourVertexID < 0) {
				return true;
			}
			return checkNeighbour(vertexID, neighbourVertexID, nextVertexID, agentPerimeter, neighbour.getAgentPerimeter());
		});
	}

	@NotNull
	public Set<Agent> collisionsOnWayFrom(@NotNull Map<Long, Map<Integer, Set<Agent>>> conflictAvoidanceTable, long step, int vertexID, int nextVertexID, double agentPerimeter) {
		if (!conflictAvoidanceTable.containsKey(step + 1) || !conflictAvoidanceTable.get(step + 1).containsKey(vertexID)) {
			return Collections.emptySet();
		}

		final Set<Agent> neighbours = conflictAvoidanceTable.get(step + 1).get(vertexID);

		return neighbours.stream()
			.filter(neighbour -> {
				final int neighbourVertexID = getNeighbourVertexID(step, neighbour);
				if (neighbourVertexID < 0) {
					return false;
				}
				return !checkNeighbour(vertexID, neighbourVertexID, nextVertexID, agentPerimeter, neighbour.getAgentPerimeter());
			})
			.collect(Collectors.toSet());
	}

	protected int getNeighbourVertexID(long step, @NotNull Agent neighbour) {
		final long plannedTime = neighbour.getPlannedStep() >= 0 ? neighbour.getPlannedStep() : step;
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
		if (adjacentVertexID == neighbourVertexID) {
			return false;
		}

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
		double x1 = xN - x;
		double y1 = yN - y;

		double perimetersSquared = neighbourPerimeter + agentPerimeter + safeDistance;
		perimetersSquared *= perimetersSquared;

		if (doubleAlmostEqual(Math.abs(x0), 0, graph.getCellSize() / 0x100) && MyNumberOperations.doubleAlmostEqual(Math.abs(y0), 0, graph.getCellSize() / 0x100)) {
			return x1 * x1 + y1 * y1 > perimetersSquared;
		}

		double closestTime = (x1 * x0 + y1 * y0) / (x0 * x0 + y0 * y0);
		double xDiffAtT = closestTime * x0 - x1;
		boolean b = doubleAlmostEqual(Math.abs(xDiffAtT), Math.abs((closestTime * xA + (1 - closestTime) * x) - (closestTime * x + (1 - closestTime) * xN)), 1e-7);
		assert b;
		xDiffAtT *= xDiffAtT;
		double yDiffAtT = closestTime * y0 - y1;
		assert doubleAlmostEqual(Math.abs(yDiffAtT), Math.abs((closestTime * yA + (1 - closestTime) * y) - (closestTime * y + (1 - closestTime) * yN)), 1e-7);
		yDiffAtT *= yDiffAtT;

		return xDiffAtT + yDiffAtT > perimetersSquared;
	}

	protected boolean noCollisionOnWay(int au, int av, int bu, int bv, double aPerimeter, double bPerimeter) {
		@NotNull GraphicalVertex u = graph.getVertex(au);
		@NotNull GraphicalVertex v = graph.getVertex(av);
		@NotNull GraphicalVertex p = graph.getVertex(bu);
		@NotNull GraphicalVertex q = graph.getVertex(bv);

		double cx = q.getX() - v.getX();
		double cy = q.getY() - v.getY();
		double dx = u.getX() - p.getX() + cx;
		double dy = u.getY() - p.getY() + cy;

		double t = 0;
		if (dx != 0 || dy != 0) {
			t = (cx * dx + cy * dy) / (dx * dx + dy * dy);
			t = Math.min(1, Math.max(0, t));
		}


		double closestX = t * dx - cx;
		closestX *= closestX;

		double closestY = t * dy - cy;
		closestY *= closestY;

		double perimeters = aPerimeter + bPerimeter + safeDistance;
		double perimetersSquared = perimeters * perimeters;

		return closestX + closestY > perimetersSquared;
	}

	public @NotNull Map<Long, Map<Integer, Agent>> getStepOccupiedVertices() {
		return stepOccupiedVertices;
	}

	protected void filterStepOccupiedVertices(long step) {
		@NotNull Iterator<Map.Entry<Long, Map<Integer, Agent>>> it = stepOccupiedVertices.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, Map<Integer, Agent>> stepVertices = it.next();
			long occupiedStep = stepVertices.getKey();
			if (occupiedStep < step) {
				it.remove();
			}
		}

		stepOccupiedVertices.computeIfAbsent(step, k -> new HashMap<>());
	}

	public boolean inCollision(@NotNull Collection<Agent> agents0, @NotNull Collection<Agent> agents1) {
		for (@NotNull Agent agent0 : agents0) {
			for (@NotNull Agent agent1 : agents1) {
				if (agent0 != agent1 && inCollision(agent0, agent1).isPresent()) {
					System.out.println(agent0.getId() + " collides with " + agent1.getId());
					return true;
				}
			}
		}

		return false;
	}

	public boolean inCollision(final @NotNull Collection<Agent> agents) {
		Agent[] visitedAgents = new Agent[agents.size()];
		int i = 0;
		for (Iterator<Agent> iterator = agents.iterator(); iterator.hasNext(); i++) {
			final Agent agent0 = iterator.next();
			for (int j = 0; j < i; j++) {
				final Agent agent1 = visitedAgents[j];
				if (agent0 != agent1 && inCollision(agent0, agent1).isPresent()) {
					System.out.println(agent0.getId() + " collides with " + agent1.getId());
					return true;
				}
			}

			visitedAgents[i] = agent0;
		}

		return false;
	}

	public @NotNull Optional<Pair<Long, Boolean>> inCollision(@NotNull Agent agent0, @NotNull Agent agent1) {
		final long agent0PlannedTime = agent0.getPlannedStep();
		final long agent1PlannedTime = agent1.getPlannedStep();
		final List<Integer> agent0Path = agent0.getPath();
		final List<Integer> agent1Path = agent1.getPath();
		final double agent0Perimeter = agent0.getAgentPerimeter();
		final double agent1Perimeter = agent1.getAgentPerimeter();
		final double agentsPerimeter = agent0Perimeter + agent1Perimeter;

		final long startStep = Math.max(agent0PlannedTime, agent1PlannedTime);
		final long endStep = Math.min(agent0PlannedTime + agent0Path.size(), agent1PlannedTime + agent1Path.size());

		if (endStep < startStep) {
			return Optional.empty();
		}

		@NotNull ListIterator<Integer> itP0 = agent0Path.listIterator((int) (startStep - agent0PlannedTime));
		@NotNull ListIterator<Integer> itP1 = agent1Path.listIterator((int) (startStep - agent1PlannedTime));

		int vertexP0 = itP0.next();
		int vertexP1 = itP1.next();

		if (conflictVerticesSet(vertexP0, agentsPerimeter).contains(vertexP1)) {
			return Optional.of(new Pair<>(startStep, true));
		}

		return collisionPath(agent0Perimeter, agent1Perimeter, startStep, itP0, itP1, vertexP0, vertexP1);
	}

	private @NotNull Optional<Pair<Long, Boolean>> collisionPath(double agent0Perimeter, double agent1Perimeter, long startStep, @NotNull ListIterator<Integer> itP0, @NotNull ListIterator<Integer> itP1, int vertexP0, int vertexP1) {
		int vertexP0Last;
		int vertexP1Last;
		long step = startStep + 1;

		while (itP0.hasNext() && itP1.hasNext()) {
			vertexP0Last = vertexP0;
			vertexP1Last = vertexP1;

			vertexP0 = itP0.next();
			vertexP1 = itP1.next();

			if (conflictVerticesSet(vertexP0, agent0Perimeter + agent1Perimeter).contains(vertexP1)) {
				return Optional.of(new Pair<>(step, true));
			}

			if (vertexP0 == vertexP1Last) {
				if (!checkNeighbour(vertexP0, vertexP0Last, vertexP1, agent0Perimeter, agent1Perimeter)) {
					assert !checkNeighbour(vertexP1Last, vertexP0Last, vertexP1, agent1Perimeter, agent0Perimeter);
					return Optional.of(new Pair<>(step, false));
				}
			}

			if (vertexP1 == vertexP0Last) {
				if (!checkNeighbour(vertexP0Last, vertexP1Last, vertexP0, agent0Perimeter, agent1Perimeter)) {
					assert !checkNeighbour(vertexP1, vertexP1Last, vertexP0, agent1Perimeter, agent0Perimeter);
					return Optional.of(new Pair<>(step, false));
				}
			}

			step++;
		}
		return Optional.empty();
	}

	public Map<Integer, List<Integer>>[] createLanes() {
		final Map<Integer, List<Integer>>[] lanes = new Map[graph.getVertices().length];
		Arrays.stream(graph.getVertices()).parallel().map(GraphicalVertex.class::cast)
			.map(GraphicalVertex.class::cast)
			.forEach(entry -> lanes[entry.getID()] = ucs(entry));
		return lanes;
	}

	private @NotNull Map<Integer, List<Integer>> ucs(@NotNull GraphicalVertex startingVertex) {
		@NotNull Map<Integer, List<Integer>> paths = new HashMap<>(graph.getVertices().length);

		@NotNull PriorityQueue<VertexWithDirectionParent> queue = new PriorityQueue<>();
		queue.add(new VertexWithDirectionParent(startingVertex));

		while (!queue.isEmpty()) {
			final VertexWithDirectionParent vertex = queue.poll();
			if (!paths.containsKey(vertex.getID())) {
				if (vertex.getParent() == null) {
					paths.put(vertex.getID(), Collections.singletonList(vertex.getID()));
				} else {
					@NotNull List<Integer> vertexPath = new ArrayList<>(paths.get(vertex.getParent().getID()));
					vertexPath.add(vertex.getID());
					paths.put(vertex.getID(), vertexPath);
				}

				graph.getVerticesDistances().get(vertex.getVertex()).forEach((neighbour, edge) -> {
					if (!paths.containsKey(neighbour.getID())) {
						queue.add(new VertexWithDirectionParent(vertex, neighbour, edge, graph.getCellSize()));
					}
				});
			}
		}
		return paths;
	}
}
