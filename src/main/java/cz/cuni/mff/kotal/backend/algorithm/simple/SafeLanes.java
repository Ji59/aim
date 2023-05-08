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
	/**
	 * Parameters names and default values
	 */
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

	/**
	 * Create a new instance finding path in specified graph.
	 * Also set internal parameters values.
	 *
	 * @param graph Graph on which are agents travelling
	 */
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

	/**
	 * Find the shortest paths for each pair of vertices (if any path exists).
	 *
	 * @return Array of maps containing for each vertex shortest path to all other vertices indexed by their ID
	 */
	public Map<Integer, List<Integer>>[] createLanes() {
		final Map<Integer, List<Integer>>[] lanes = new Map[graph.getVertices().length];
		Arrays.stream(graph.getVertices()).parallel().map(GraphicalVertex.class::cast)
			.map(GraphicalVertex.class::cast)
			.forEach(entry -> lanes[entry.getID()] = ucs(entry));
		return lanes;
	}

	/**
	 * Search all reachable vertices from specified vertex.
	 * For each reachable vertex, find the shortest path to it from start and save it.
	 *
	 * @param startingVertex Vertex from which are paths found
	 * @return Map from vertex ID to found the shortest path
	 */
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

	/**
	 * Create a new instance finding path in specified graph.
	 * Also set internal parameters values.
	 *
	 * @param graph        Graph on which are agents travelling
	 * @param safeDistance Value of {@link #safeDistance} parameter
	 */
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

	/**
	 * Try to plan agent in lane from specified vertex to the closest valid target.
	 *
	 * @param agent   Agent to be planned
	 * @param entryID ID of starting vertex for algorithm
	 * @param exitsID Set of IDs of target vertices for algorithm
	 * @param step    Step in which agents should be planned
	 * @return Agent if planning was successful, otherwise null
	 */
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

	/**
	 * @param agent Agent, which targets are looked for
	 * @return Set of agent's targets
	 */
	public Set<Integer> getExits(@NotNull Agent agent) {
		return agent.getExit() < 0 ? directionExits.get(agent.getExitDirection()) : Collections.singleton(agent.getExit());
	}

	/**
	 * Inform algorithm about new agent it should avoid while planning.
	 * Agent should have a valid path.
	 *
	 * @param agent Agent to be avoided by future agents
	 */
	@Override
	public void addPlannedAgent(@NotNull Agent agent) {
		List<Integer> path = agent.getPath();
		long step = agent.getPlannedStep();
		addPlannedPath(agent, path, step);

		if (graph instanceof LinkGraph linkGraph) {
			path = linkGraph.getRealPath(path);
			agent.setPath(path, agent.getPlannedStep());
		}
	}

	/**
	 * Inform algorithm about new agent it should avoid while planning.
	 *
	 * @param agent Agent to be avoided by future agents
	 * @param path  Path of the agent
	 * @param step  Step in which the agent was planned
	 */
	@Override
	public void addPlannedPath(@NotNull Agent agent, @NotNull List<Integer> path, long step) {
		for (int i = 0; i < path.size(); i++) {
			stepOccupiedVertices.putIfAbsent(step + i, new HashMap<>());
			stepOccupiedVertices.get(step + i).put(path.get(i), agent);
		}
	}

	/**
	 * Tell the algorithm it should stop as soon as possible.
	 * This call does not guarantee immediate termination or invalid path finding result for all agents.
	 */
	@Override
	public void stop() {
		stopped = true;
	}

	/**
	 * Check if a path is valid.
	 *
	 * @param step           Starting step of the traver
	 * @param path           Path containing IDs of vertices
	 * @param agentPerimeter Perimeter of planned agent
	 * @return True if the path is valid for agent, otherwise false
	 */
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

	/**
	 * Check if agent can transfer between specified vertices without getting too near any other planned agent.
	 *
	 * @param step           Step of start of the transfer
	 * @param vertexID       ID of starting vertex
	 * @param nextVertexID   ID of vertex where agent should end up
	 * @param agentPerimeter Perimeter of the transferring agent
	 * @return True if the transfer is valid for agent, otherwise false
	 */
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

	/**
	 * Check if agent can transfer between specified vertices without getting too near any other agent.
	 *
	 * <p>Similar to {@link #safeStepTo(long, int, int, double)},
	 * except custom map of planned agents is used instead of {@link #stepOccupiedVertices}.
	 * </p>
	 *
	 * @param illegalMovesTable Map containing steps and for those steps IDs of vertices and agents being at those vertices at step
	 * @param step              Step of start of the transfer
	 * @param vertexID          ID of starting vertex
	 * @param nextVertexID      ID of vertex where agent should end up
	 * @param agentPerimeter    Perimeter of the transferring agent
	 * @return True if transfer is valid for agent, otherwise false
	 */
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

	/**
	 * {@link #stepOccupiedVertices} is map containing for step map of vertices and agents.
	 * If agent is in the map under a key {@code id} it means
	 * the agent is at the step at vertex with that {@code id}.
	 *
	 * @return Map containing entries for steps and vertices with agents
	 */
	public @NotNull Map<Long, Map<Integer, Agent>> getStepOccupiedVertices() {
		return stepOccupiedVertices;
	}

	/**
	 * Remove all entries from {@link #stepOccupiedVertices} that are older than specified step.
	 *
	 * @param step Step to which entries should be removed
	 */
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

	/**
	 * Check if any agent in first collection is in collision with any agent from second collection.
	 *
	 * @param agents0 Collection of agents
	 * @param agents1 Second collection of agents
	 * @return True if any two agents from different collections end up in collision, otherwise false
	 */
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

	/**
	 * Check if agents are in collision.
	 *
	 * @param agent0 First agent
	 * @param agent1 Second agent
	 * @return True if agents end uo in collision, otherwise false
	 */
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

	/**
	 * Check if two paths lead to agents getting closer then {@code safePerimeter}.
	 * Paths should have same starting step.
	 * Agents perimeters should be included in {@code safePerimeter}.
	 *
	 * @param step          Step common to start of both paths
	 * @param itP1          Iterator over IDs of vertices in first path
	 * @param itP0          Iterator over IDs of vertices in second path
	 * @param safePerimeter Closest allowed distance between agents
	 * @return True if agents get too near each other, otherwise false
	 */
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

	/**
	 * Find out the distance between two vertices specified by their IDs.
	 *
	 * @param u First vertex ID
	 * @param v Second vertex ID
	 * @return Distance between {@code u} and {@code v}
	 */
	protected double verticesDistance(int u, int v) {
		return graph.getTravelDistances()[u].get(u)[v].get(v);
	}

	/**
	 * Check if there is at least one pair of agents that are in collision.
	 *
	 * @param agents Collection of agents to check
	 * @return True if any two agents are in collision, otherwise false
	 */
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

	/**
	 * Find all vertices too close to specified vertex.
	 *
	 * @param vertexID Vertex, around which to look around
	 * @param distance Distance how far to look
	 * @return Array of IDs of vertices that are too close
	 */
	protected int @NotNull [] conflictVertices(int vertexID, double distance) {
		return graph.getSortedVertexVerticesDistances()[vertexID].stream()
			.takeWhile(v -> v.distance() <= distance)
			.mapToInt(SimulationGraph.VertexDistance::vertexID)
			.toArray();
	}

	/**
	 * Find all vertices too close to specified vertex.
	 *
	 * @param vertexID Vertex, around which to look around
	 * @param distance Distance how far to look
	 * @return Set of IDs of vertices that are too close
	 */
	protected @NotNull Set<Integer> conflictVerticesSet(int vertexID, double distance) {
		return graph.getSortedVertexVerticesDistances()[vertexID].stream()
			.takeWhile(v -> v.distance() <= distance)
			.map(SimulationGraph.VertexDistance::vertexID)
			.collect(Collectors.toSet());
	}
}
