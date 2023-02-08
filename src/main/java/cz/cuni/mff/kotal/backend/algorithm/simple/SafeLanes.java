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
		for (SimulationGraph.VertexDistance vertexDistance : graph.getSortedVertexVerticesTravelDistances()[vertexID].get(nextVertexID)) {
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

	public boolean safeStepTo(@NotNull Map<Long, Map<Integer, Set<Agent>>> illegalMovesTable, long step, int vertexID, int nextVertexID, double agentPerimeter) {
		if (!(illegalMovesTable.containsKey(step) && illegalMovesTable.containsKey(step + 1)) ||
			illegalMovesTable.get(step).isEmpty() || illegalMovesTable.get(step + 1).isEmpty()
		) {
			return true;
		}
		if (illegalMovesTable.get(step).containsKey(vertexID) || illegalMovesTable.get(step + 1).containsKey(nextVertexID)) {
			return false;
		}

		@NotNull final Map<Integer, Set<Agent>> occupiedVertices = illegalMovesTable.get(step);
		@NotNull final Map<Integer, Double>[] transferMap = graph.getTravelDistances()[vertexID].get(nextVertexID);
		final double agentSafeDistance = agentPerimeter + safeDistance;
		final double largestSafePerimeter = agentSafeDistance + largestAgentPerimeter;
		for (SimulationGraph.VertexDistance vertexDistance : graph.getSortedVertexVerticesTravelDistances()[vertexID].get(nextVertexID)) {
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

			for (@NotNull final Agent plannedAgent : occupiedVertices.get(agentVertex)) {
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
		}

		return true;
	}

	@Deprecated
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
					return true;
				}
			}
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

		final long startStep = Math.max(agent0PlannedTime, agent1PlannedTime);
		final long endStep = Math.min(agent0PlannedTime + agent0Path.size(), agent1PlannedTime + agent1Path.size());

		if (endStep < startStep) {
			return Optional.empty();
		}

		@NotNull ListIterator<Integer> itP0 = agent0Path.listIterator((int) (startStep - agent0PlannedTime));
		@NotNull ListIterator<Integer> itP1 = agent1Path.listIterator((int) (startStep - agent1PlannedTime));

		return collisionPath(startStep, itP1, itP0, agent0Perimeter + agent1Perimeter + safeDistance);
	}

	private @NotNull Optional<Pair<Long, Boolean>> collisionPath(long step, @NotNull ListIterator<Integer> itP1, @NotNull ListIterator<Integer> itP0, double safePerimeter) {
		assert itP0.hasNext() && itP1.hasNext();
		int vertexP0 = itP0.next();
		int vertexP1 = itP1.next();
		if (safePerimeter >= verticesDistance(vertexP0, vertexP1)) {
			return Optional.of(new Pair<>(step, false));
		}

		while (itP0.hasNext() && itP1.hasNext()) {
			final int nextVertexP0 = itP0.next();
			final int nextVertexP1 = itP1.next();
			step++;

			if (safePerimeter >= verticesDistance(nextVertexP0, nextVertexP1)) {
				return Optional.of(new Pair<>(step, false));
			}

			if (safePerimeter >= graph.getTravelDistances()[vertexP0].get(nextVertexP0)[vertexP1].get(nextVertexP1)) {
				return Optional.of(new Pair<>(step, true));
			}

			vertexP0 = nextVertexP0;
			vertexP1 = nextVertexP1;
		}
		return Optional.empty();
	}

	protected double verticesDistance(int u, int v) {
		return graph.getTravelDistances()[u].get(u)[v].get(v);
	}

	public boolean inCollision(final @NotNull Collection<Agent> agents) {
		Agent[] visitedAgents = new Agent[agents.size()];
		int i = 0;
		for (final Iterator<Agent> iterator = agents.iterator(); iterator.hasNext(); i++) {
			final Agent agent0 = iterator.next();
			for (int j = 0; j < i; j++) {
				final Agent agent1 = visitedAgents[j];
				if (agent0 != agent1 && inCollision(agent0, agent1).isPresent()) {
					return true;
				}
			}

			visitedAgents[i] = agent0;
		}

		return false;
	}

	protected int @NotNull [] conflictVertices(int vertexID, double distance) {
		return graph.getSortedVertexVerticesDistances()[vertexID].stream()
			.takeWhile(v -> v.distance() <= distance)
			.mapToInt(SimulationGraph.VertexDistance::vertexID)
			.toArray();
	}

	protected @NotNull Set<Integer> conflictVerticesSet(int vertexID, double distance) {
		return graph.getSortedVertexVerticesDistances()[vertexID].stream()
			.takeWhile(v -> v.distance() <= distance)
			.map(SimulationGraph.VertexDistance::vertexID)
			.collect(Collectors.toSet());
	}
}
