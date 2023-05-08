package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Triplet;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.VertexWithDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.backend.algorithm.cbs.CBSSingleGrouped.SIMPLE_STRATEGY_DEF;
import static cz.cuni.mff.kotal.backend.algorithm.cbs.CBSSingleGrouped.SIMPLE_STRATEGY_NAME;

public class AStarSingleGrouped extends AStarSingle {
	/**
	 * Parameters names and default values
	 */
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(AStarSingle.PARAMETERS);

	static {
		PARAMETERS.put(SIMPLE_STRATEGY_NAME, SIMPLE_STRATEGY_DEF);
	}

	protected final long simpleStrategyAfter;
	protected final ReentrantLock simpleLock = new ReentrantLock();
	protected boolean simple = false;

	public AStarSingleGrouped(@NotNull SimulationGraph graph) {
		super(graph);

		simpleStrategyAfter = AlgorithmMenuTab2.getLongParameter(SIMPLE_STRATEGY_NAME, SIMPLE_STRATEGY_DEF);
	}

	@TestOnly
	protected AStarSingleGrouped(@NotNull SimulationGraph graph, double safeDistance, int maximumVertexVisits, boolean allowAgentStop, int maximumPathDelay, boolean allowAgentReturn, long simpleStrategyAfter) {
		super(graph, safeDistance, maximumVertexVisits, allowAgentStop, maximumPathDelay, allowAgentReturn);
		this.simpleStrategyAfter = simpleStrategyAfter;
	}

	@Override
	public Collection<Agent> planAgents(@NotNull Collection<Agent> agents, long step) {
		filterStepOccupiedVertices(step);
		return planAgents(agents.stream().collect(Collectors.toMap(Function.identity(), a -> new Pair<>(a.getEntry(), getExitIDs(a)))), step);
	}

	@Override
	public @NotNull Collection<Agent> planAgents(final @NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		if (agentsEntriesExits.isEmpty()) {
			return Collections.emptySet();
		}

		final Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups = createInitialPaths(agentsEntriesExits, step);

		if (agentsEntriesExits.isEmpty()) {
			return Collections.emptySet();
		}

		simple = false;

		Thread timer = new Thread(() -> {
			try {
				Thread.sleep(simpleStrategyAfter);
				simpleLock.lock();
				simple = true;
			} catch (InterruptedException ignored) {
			} finally {
				if (simpleLock.isHeldByCurrentThread()) {
					simpleLock.unlock();
				}
			}
		});
		timer.start();

		solveAgentsCollisions(step, agentsGroups);
		timer.interrupt();

		if (stopped) {
			return Collections.emptySet();
		}

		@NotNull Set<Agent> plannedAgents = new HashSet<>();
		for (@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group : agentsGroups) {
			@NotNull Set<Agent> groupAgents = group.getVal0().keySet();

			plannedAgents.addAll(groupAgents);
			groupAgents.forEach(this::addPlannedAgent);
		}

		return plannedAgents;
	}

	protected void solveAgentsCollisions(long step, @NotNull Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups) {
		@NotNull Map<Collection<Agent>, Collection<Collection<Agent>>> resolvedPairs = new HashMap<>();

		Optional<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> collisionTripletOptional;
		while (!stopped && (collisionTripletOptional = inCollision(agentsGroups)).isPresent()) {
			@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet = collisionTripletOptional.get();

			Map<Agent, Pair<Integer, Set<Integer>>> group0 = group0CollisionTriplet.getVal0();
			@NotNull Set<Agent> group0Agents = new HashSet<>(group0.keySet());
			resolvedPairs.computeIfAbsent(group0Agents, k -> new HashSet<>());

			final Set<Agent> conflictAgents = group0CollisionTriplet.getVal2();

			@NotNull Map<Long, Map<Integer, Set<Agent>>> group0IllegalMovesTable = new HashMap<>();
			@NotNull Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable = new HashMap<>();

			@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet = findCollisionGroup(agentsGroups, resolvedPairs, group0Agents, conflictAgents, group0IllegalMovesTable, restGroupsConflictAvoidanceTable);

			Map<Agent, Pair<Integer, Set<Integer>>> group1;
			Set<Agent> group1Agents;
			@Nullable Map<Long, Map<Integer, Set<Agent>>> group1IllegalMovesTable = null;
			if (switchGroups(group0Agents, group1CollisionTriplet.getVal0().keySet())) {
				group1 = group1CollisionTriplet.getVal0();
				group1Agents = new HashSet<>(group1.keySet());
			} else {
				group1 = group0;
				group0 = group1CollisionTriplet.getVal0();
				final @NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> temp = group0CollisionTriplet;
				group0CollisionTriplet = group1CollisionTriplet;
				group1CollisionTriplet = temp;

				group1Agents = group0Agents;
				group0Agents = new HashSet<>(group0.keySet());
				resolvedPairs.computeIfAbsent(group0Agents, k -> new HashSet<>());

				mergeIllegalMovesTables(restGroupsConflictAvoidanceTable, group0IllegalMovesTable);
				group1IllegalMovesTable = group0IllegalMovesTable;
				group0IllegalMovesTable = getIllegalMovesTable(agentsGroups, resolvedPairs, group1CollisionTriplet, group0Agents);
			}

			@NotNull Map<Long, Map<Integer, Set<Agent>>> group0IllegalMovesTableComplete = copyMap(group0IllegalMovesTable);
			mergeConflictAvoidanceTables(group0IllegalMovesTableComplete, stepOccupiedVertices);

			if (replanGroup(step, agentsGroups, group0CollisionTriplet, group1CollisionTriplet, group0, group0IllegalMovesTableComplete, restGroupsConflictAvoidanceTable)) {
				resolvedPairs.get(group0Agents).add(group1Agents);
				resolvedPairs.computeIfAbsent(group1Agents, k -> new HashSet<>());
				resolvedPairs.get(group1Agents).add(group0Agents);
				continue;
			}

			if (group1IllegalMovesTable == null) {
				mergeIllegalMovesTables(restGroupsConflictAvoidanceTable, group0IllegalMovesTable);
				group1IllegalMovesTable = getIllegalMovesTable(agentsGroups, resolvedPairs, group0CollisionTriplet, group1Agents);
			}

			mergeConflictAvoidanceTables(group1IllegalMovesTable, stepOccupiedVertices);

			if (replanGroup(step, agentsGroups, group1CollisionTriplet, group0CollisionTriplet, group1, group1IllegalMovesTable, restGroupsConflictAvoidanceTable)) {
				resolvedPairs.get(group1Agents).add(group0Agents);
				resolvedPairs.get(group0Agents).add(group1Agents);
				continue;
			}

			tryMergeGroups(step, agentsGroups, group0CollisionTriplet, group1CollisionTriplet, restGroupsConflictAvoidanceTable, group1IllegalMovesTable);
		}
	}

	protected void tryMergeGroups(
		long step, @NotNull Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups,
		@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet,
		@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet,
		@NotNull Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable, @NotNull Map<Long, Map<Integer, Set<Agent>>> group1IllegalMovesTable
	) {
		Map<Agent, Pair<Integer, Set<Integer>>> group1 = group1CollisionTriplet.getVal0();

		@NotNull Map<Agent, Pair<Integer, Set<Integer>>> combinedAgents = new HashMap<>(group0CollisionTriplet.getVal0());
		combinedAgents.putAll(group1);
		@Nullable Set<Agent> collisionAgents = null;

		agentsGroups.remove(group0CollisionTriplet);
		filterReplannedCollisionAgents(agentsGroups, group0CollisionTriplet.getVal0().keySet());
		filterReplannedCollisionAgents(agentsGroups, group1CollisionTriplet.getVal0().keySet());

		agentsSize:
		for (int i = group0CollisionTriplet.getVal0().size() + group1.size(); i > group1.size(); i--) {
			for (@NotNull Collection<Map.Entry<Agent, Pair<Integer, Set<Integer>>>> combination : MyNumberOperations.combinations(combinedAgents.entrySet(), i)) {
				simpleLock.lock();
				final boolean simpleCopy = simple;
				simpleLock.unlock();

				if (simpleCopy) {
					break agentsSize;
				}

				if (stopped) {
					return;
				}

				@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agents = new HashMap<>();
				combination.forEach(agentEntryExits -> agents.put(agentEntryExits.getKey(), agentEntryExits.getValue()));

				collisionAgents = findPaths(agents, step, mapStepOccupiedVertices(), restGroupsConflictAvoidanceTable);
				if (collisionAgents != null) {
					combinedAgents = agents;
					break agentsSize;
				}
			}
		}

		if (collisionAgents != null) {
			agentsGroups.remove(group1CollisionTriplet);

			agentsGroups.add(new Triplet<>(combinedAgents, getConflictAvoidanceTable(combinedAgents, step), collisionAgents));
		} else {
			final @Nullable Set<Agent> group1Collisions = findPaths(group1, step, group1IllegalMovesTable, restGroupsConflictAvoidanceTable);
			assert group1Collisions != null || stopped;
			if (group1Collisions != null) {
				updateAgentsGroup(step, group1CollisionTriplet, group1Collisions);
			}
		}
	}

	protected boolean switchGroups(@NotNull Set<Agent> group0Agents, @NotNull Set<Agent> group1Agents) {
		return group0Agents.size() <= group1Agents.size();
	}

	protected @NotNull Map<Long, Map<Integer, Set<Agent>>> getIllegalMovesTable(@NotNull Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups, @NotNull Map<Collection<Agent>, Collection<Collection<Agent>>> resolvedPairs, Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> conflictGroupCollisionTriplet, Set<Agent> groupAgents) {
		Map<Long, Map<Integer, Set<Agent>>> illegalMovesTable;
		illegalMovesTable = new HashMap<>();
		resolvedPairs.computeIfAbsent(groupAgents, k -> new HashSet<>());
		if (resolvedPairs.containsKey(groupAgents) && !resolvedPairs.get(groupAgents).isEmpty()) {
			for (@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group : agentsGroups) {
				if (group != conflictGroupCollisionTriplet && resolvedPairs.get(groupAgents).contains(group.getVal0().keySet())) {
					mergeConflictAvoidanceTables(illegalMovesTable, group.getVal1());
				}
			}
		}
		return illegalMovesTable;
	}

	protected boolean replanGroup(long step, @NotNull Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups,
																@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet,
																@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet,
																@NotNull Map<Agent, Pair<Integer, Set<Integer>>> group0, @NotNull Map<Long, Map<Integer, Set<Agent>>> invalidMovesMap,
																@NotNull Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable) {
		@NotNull Map<Long, Map<Integer, Set<Agent>>> group0IllegalMovesTable = copyMap(invalidMovesMap);
		mergeConflictAvoidanceTables(group0IllegalMovesTable, group1CollisionTriplet.getVal1());

		if (stopped) {
			return false;
		}

		@Nullable final Set<Agent> group0Collisions = findPaths(group0, step, group0IllegalMovesTable, restGroupsConflictAvoidanceTable);
		if (group0Collisions != null) {
			updateAgentsGroup(step, group0CollisionTriplet, group0Collisions);
			filterReplannedCollisionAgents(agentsGroups, group0.keySet());
			return true;
		}
		return false;
	}

	protected Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> createInitialPaths(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		return createInitialPaths(agentsEntriesExits, step, new HashMap<>());
	}

	protected @NotNull Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> createInitialPaths(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, final long step, final @NotNull Map<Long, Map<Integer, Set<Agent>>> conflictAvoidanceTable) {
		final @NotNull Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups = new HashSet<>(agentsEntriesExits.size());

		for (@NotNull Iterator<Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator(); it.hasNext(); ) {
			final Entry<Agent, Pair<Integer, Set<Integer>>> agentEntryExitsEntry = it.next();
			final Agent agent = agentEntryExitsEntry.getKey();
			final int agentEntry = agentEntryExitsEntry.getValue().getVal0();
			if (stepOccupiedVertices.get(step).containsKey(agentEntry)) {
				it.remove();
				continue;
			}
			final @NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentEntryExitsMap = new HashMap<>(1);
			agentEntryExitsMap.put(agent, agentEntryExitsEntry.getValue());

			final @Nullable Set<Agent> collisionAgents = findPaths(agentEntryExitsMap, step, mapStepOccupiedVertices(), conflictAvoidanceTable);
			if (collisionAgents == null) {
				it.remove();
			} else {
				final @NotNull Map<Long, Map<Integer, Agent>> agentConflictAvoidanceTable = getConflictAvoidanceTable(agentEntryExitsMap, step);
				agentsGroups.add(new Triplet<>(agentEntryExitsMap, agentConflictAvoidanceTable, collisionAgents));
				mergeConflictAvoidanceTables(conflictAvoidanceTable, agentConflictAvoidanceTable);
			}
		}

		return agentsGroups;
	}

	private void filterAgentsWithNoEntry(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		for (@NotNull Iterator<Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator(); it.hasNext(); ) {
			final Entry<Agent, Pair<Integer, Set<Integer>>> entry = it.next();
			final Agent agent = entry.getKey();
			final int agentEntry = entry.getValue().getVal0();
			if (stepOccupiedVertices.get(step).containsKey(agentEntry)) {
				it.remove();
			}
		}
	}

	protected void filterReplannedCollisionAgents(@NotNull Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups, @NotNull Set<Agent> replannedAgents) {
		for (@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> agentGroup : agentsGroups) {
			final Set<Agent> collisionAgents = agentGroup.getVal2();
			collisionAgents.removeAll(replannedAgents);
		}
	}

	protected @NotNull Map<Long, Map<Integer, Set<Agent>>> copyMap(@NotNull Map<Long, Map<Integer, Set<Agent>>> sourceMap) {
		@NotNull Map<Long, Map<Integer, Set<Agent>>> copy = new HashMap<>(sourceMap.size());
		for (@NotNull Entry<Long, Map<Integer, Set<Agent>>> entry : sourceMap.entrySet()) {
			@NotNull Map<Integer, Set<Agent>> verticesMap = new HashMap<>(entry.getValue().size());
			copy.put(entry.getKey(), verticesMap);
			for (@NotNull Entry<Integer, Set<Agent>> vertexEntry : entry.getValue().entrySet()) {
				verticesMap.put(vertexEntry.getKey(), new HashSet<>(vertexEntry.getValue()));
			}
		}

		return copy;
	}

	protected @NotNull Map<Long, Map<Integer, Set<Agent>>> mapStepOccupiedVertices() {
		final @NotNull Map<Long, Map<Integer, Set<Agent>>> newMap = new HashMap<>(stepOccupiedVertices.size());
		for (@NotNull Entry<Long, Map<Integer, Agent>> stepEntry : stepOccupiedVertices.entrySet()) {
			final @NotNull HashMap<Integer, Set<Agent>> stepMap = new HashMap<>(stepEntry.getValue().size());
			newMap.put(stepEntry.getKey(), stepMap);
			for (@NotNull Entry<Integer, Agent> vertexEntry : stepEntry.getValue().entrySet()) {
				stepMap.put(vertexEntry.getKey(), Set.of(vertexEntry.getValue()));
			}
		}

		return newMap;
	}

	protected void updateAgentsGroup(long step, @NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> collisionTriplet, @Nullable Set<Agent> collisionAgents) {
		collisionTriplet.setVal1(getConflictAvoidanceTable(collisionTriplet.getVal0(), step));
		collisionTriplet.setVal2(collisionAgents);
	}

	protected @NotNull Optional<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> inCollision(@NotNull Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups) {
		return agentsGroups.stream().filter(triplet -> !triplet.getVal2().isEmpty()).min(Comparator.comparingInt(g -> g.getVal0().size()));
	}

	protected @Nullable Set<Agent> findPaths(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step, final @NotNull Map<Long, Map<Integer, Set<Agent>>> illegalMovesTable, final @NotNull Map<Long, Map<Integer, Set<Agent>>> conflictAvoidanceTable) {
		assert stepOccupiedVertices.entrySet().stream().allMatch(s -> s.getValue().entrySet().stream().allMatch(v -> illegalMovesTable.get(s.getKey()).get(v.getKey()).contains(v.getValue())));

		final int agentsCount = agentsEntriesExits.size();
		final Set<Integer> @NotNull [] exitsIDs = new Set[agentsCount];
		final Agent @NotNull [] agents = new Agent[agentsCount];
		final double[] @NotNull [] heuristics = new double[agentsCount][];
		final int @NotNull [] maximumDelays = new int[agentsCount];

		final @NotNull Set<CompositeState> visitedStates = new HashSet<>();
		final Map<Integer, Set<Integer>>[] @NotNull [] collisionPairs = new Map[agentsCount][];
		for (int i = 0; i < agentsCount; i++) {
			collisionPairs[i] = new Map[i];
			for (int j = 0; j < i; j++) {
				collisionPairs[i][j] = new HashMap<>();
			}
		}

		final @NotNull CompositeState initialState = generateInitialState(agentsEntriesExits, exitsIDs, agents, heuristics, maximumDelays, step);
		final @NotNull PriorityQueue<CompositeState> queue = new PriorityQueue<>();
		queue.add(initialState);

		while (!queue.isEmpty() && !stopped) {
			final CompositeState state = queue.poll();

			if (state.isFinal(exitsIDs)) {
				constructPaths(step, agentsCount, agents, state);
				return state.collisionAgents;
			}

			long stateStep = state.getStep();
			final long nextStep = stateStep + 1;

			// check if all non-finished agents can continue their journey
			if (invalidState(step, agentsCount, exitsIDs, maximumDelays, state, nextStep)) {
				continue;
			}

			final Set<Integer> @NotNull [] neighbours = new Set[agentsCount];
			final Map<Integer, Integer> @NotNull [] neighboursVisited = new Map[agentsCount];
			generateIndividualNeighbours(step, agentsCount, exitsIDs, heuristics, maximumDelays, state, nextStep, neighbours, neighboursVisited);

			// Remove neighbours visited too many times
			filterVisitedNeighbours(agents, state, neighbours, neighboursVisited);


			// remove colliding neighbours
			final Map<Integer, Set<Agent>> @NotNull [] neighboursCollisions = filterCollidingNeighbours(illegalMovesTable, conflictAvoidanceTable, agents, state, stateStep, nextStep, neighbours);

			// if any traveling agent does not have any valid neighbours, skip generating neighbours states
			if (existsNoneValidNeighbour(agentsCount, exitsIDs, state, neighbours)) {
				continue;
			}

			// for each valid neighbour create vertex object
			final VertexWithDirection[] @NotNull [] neighboursVertices = generateVertexObjects(agentsCount, heuristics, state, neighbours);

			// add cartesian product of agents position vertex neighbours to queue
			@NotNull Set<VertexWithDirection[]> cartesianNeighbours = cartesianProductWithCheck(agents, exitsIDs, collisionPairs, state, neighboursVertices);
			for (VertexWithDirection @NotNull [] neighboursCombination : cartesianNeighbours) {
				@NotNull CompositeState nextState = new CompositeState(neighboursCombination, state, collisionCount(neighboursCombination, neighboursCollisions));
				if (!visitedStates.contains(nextState)) {
					visitedStates.add(nextState);
					queue.add(nextState);
				}
			}
		}

		return null;
	}

	@NotNull
	private VertexWithDirection[] @NotNull [] generateVertexObjects(int agentsCount, double[][] heuristics, @NotNull CompositeState state, Set<Integer>[] neighbours) {
		final VertexWithDirection[] @NotNull [] neighboursVertices = new VertexWithDirection[agentsCount][];
		for (int i = 0; i < agentsCount; i++) {
			final VertexWithDirection stateVertex = state.getStates()[i];
			final Set<Integer> vertexNeighbours = neighbours[i];
			if (vertexNeighbours.isEmpty()) {
				neighboursVertices[i] = new VertexWithDirection[]{stateVertex};
			} else {
				VertexWithDirection @NotNull [] neighboursWithVisit = new VertexWithDirection[vertexNeighbours.size()];
				neighboursVertices[i] = neighboursWithVisit;
				int j = 0;
				for (int id : vertexNeighbours) {
					final double distance = graph.getDistance(stateVertex.getID(), id);
					neighboursWithVisit[j++] = new VertexWithDirection(stateVertex, graph.getVertex(id), distance, heuristics[i][id]);
				}
			}
		}
		return neighboursVertices;
	}

	private Map<Integer, Set<Agent>> @NotNull [] filterCollidingNeighbours(final @NotNull Map<Long, Map<Integer, Set<Agent>>> illegalMovesTable, final @NotNull Map<Long, Map<Integer, Set<Agent>>> conflictAvoidanceTable, final Agent @NotNull [] agents, final @NotNull CompositeState state, long stateStep, long nextStep, Set<Integer> @NotNull [] neighbours) {
		final Map<Integer, Set<Agent>> @NotNull [] neighboursCollisions = new Map[neighbours.length];

		for (int i = 0; i < agents.length; i++) {
			final int stateID = state.getStates()[i].getID();
			final @NotNull Map<Integer, Set<Agent>> agentNeighboursCollisions = new HashMap<>();
			neighboursCollisions[i] = agentNeighboursCollisions;
			for (final @NotNull Iterator<Integer> iterator = neighbours[i].iterator(); iterator.hasNext(); ) {
				final int neighbourID = iterator.next();
				final double agentPerimeter = agents[i].getAgentPerimeter();
				if (safeStepTo(illegalMovesTable, stateStep, stateID, neighbourID, agentPerimeter)) {
					assert !agentNeighboursCollisions.containsKey(neighbourID);
					agentNeighboursCollisions.put(neighbourID, new HashSet<>());
					agentNeighboursCollisions.get(neighbourID).addAll(collisionsOnWayTo(conflictAvoidanceTable, stateStep, stateID, neighbourID, agentPerimeter));
				} else {
					iterator.remove();
				}
			}
		}

		return neighboursCollisions;
	}

	@NotNull
	public Set<Agent> collisionsOnWayTo(@NotNull Map<Long, Map<Integer, Set<Agent>>> conflictAvoidanceTable, long step, int vertexID, int nextVertexID, double agentPerimeter) {
		if (!(conflictAvoidanceTable.containsKey(step) && conflictAvoidanceTable.containsKey(step + 1)) ||
			conflictAvoidanceTable.get(step).isEmpty() || conflictAvoidanceTable.get(step + 1).isEmpty()
		) {
			return Collections.emptySet();
		}

		final Set<Agent> neighbours = new HashSet<>();

		final Map<Integer, Set<Agent>> conflictVertices = conflictAvoidanceTable.get(step);
		@NotNull final Map<Integer, Double>[] transferMap = graph.getTravelDistances()[vertexID].get(nextVertexID);
		final double agentSafeDistance = agentPerimeter + safeDistance;
		final double largestSafePerimeter = agentSafeDistance + largestAgentPerimeter;
		for (SimulationGraph.VertexDistance vertexDistance : graph.getSortedVertexVerticesTravelDistances()[vertexID].get(nextVertexID)) {
			if (vertexDistance.distance() > largestSafePerimeter) {
				break;
			}

			final int agentVertex = vertexDistance.vertexID();
			if (!conflictVertices.containsKey(agentVertex)) {
				continue;
			}
			if (agentVertex == vertexID) {
				neighbours.addAll(conflictVertices.get(agentVertex));
				continue;
			}

			for (@NotNull final Agent plannedAgent : conflictVertices.get(agentVertex)) {

				final int plannedAgentTime = (int) (step - plannedAgent.getPlannedStep() + 1);
				@NotNull List<Integer> path = plannedAgent.getPath();
				if (plannedAgentTime >= path.size()) {
					continue;
				}

				final int agentNextVertex = path.get(plannedAgentTime);
				final double perimeters = agentSafeDistance + plannedAgent.getAgentPerimeter();
				if (transferMap[agentVertex].get(agentNextVertex) <= perimeters) {
					neighbours.add(plannedAgent);
				}
			}
		}

		return neighbours;
	}

	protected void filterVisitedNeighbours(Agent @NotNull [] agents, CompositeState state, Set<Integer>[] neighbours, Map<Integer, Integer>[] neighboursVisited) {
		CompositeState predecessorState = state;
		while (predecessorState != null) {
			for (int i = 0; i < agents.length; i++) {
				int predecessorVertexID = predecessorState.getStates()[i].getID();
				if (neighbours[i].contains(predecessorVertexID)) {
					modifyNeighbours(agents[i], neighbours, neighboursVisited, i, predecessorVertexID);
				}
			}
			predecessorState = predecessorState.getParent();
		}
	}

	protected void modifyNeighbours(Agent agent, Set<Integer>[] neighbours, Map<Integer, Integer>[] neighboursVisited, int i, int predecessorVertexID) {
		final int vertexVisits = neighboursVisited[i].get(predecessorVertexID);
		if (vertexVisits >= maximumVertexVisits) {
			neighbours[i].remove(predecessorVertexID);
		} else {
			neighboursVisited[i].put(predecessorVertexID, vertexVisits + 1);
		}
	}

	private void generateIndividualNeighbours(long step, int agentsCount, Set<Integer>[] exitsIDs, double[][] heuristics, int[] maximumDelays, @NotNull CompositeState state, long nextStep, Set<Integer>[] neighbours, Map<Integer, Integer>[] neighboursVisited) {
		for (int i = 0; i < agentsCount; i++) {
			@NotNull Set<Integer> vertexNeighbours = new HashSet<>(state.getStates()[i].getVertex().getNeighbourIDs());
			neighbours[i] = vertexNeighbours;
			if (allowAgentStop && state.getParent() != null) {
				vertexNeighbours.add(state.getStates()[i].getID());
			}

			if (!allowAgentReturn && state.getParent() != null) {
				vertexNeighbours.remove(state.getParent().getStates()[i].getID());
			}

			neighboursVisited[i] = new HashMap<>(neighbours[i].size());
			for (@NotNull Iterator<Integer> iterator = vertexNeighbours.iterator(); iterator.hasNext(); ) {
				final int id = iterator.next();
				if (heuristics[i][id] == 0) {
					heuristics[i][id] = getHeuristic(exitsIDs[i], id);
				}
				double vertexHeuristic = heuristics[i][id];
				if (Double.isFinite(vertexHeuristic) && vertexHeuristic <= step + maximumDelays[i] - nextStep) {
					neighboursVisited[i].put(id, 1);
				} else {
					iterator.remove();
				}
			}
		}
	}

	private void constructPaths(long step, int agentsCount, Agent[] agents, @NotNull CompositeState state) {
		final List<Integer> @NotNull [] paths = constructPaths(state);
		for (int i = 0; i < agentsCount; i++) {
			Agent agent = agents[i];
			agent.setPath(paths[i], step);
		}
	}

	private @NotNull Pair<Integer, Set<Agent>> collisionCount(VertexWithDirection @NotNull [] vertices, Map<Integer, Set<Agent>>[] collidingVertices) {
		int collisions = 0;
		@NotNull Set<Agent> collisionAgents = new HashSet<>();
		for (int i = 0; i < vertices.length; i++) {
			final int vertexID = vertices[i].getID();
			if (collidingVertices[i].containsKey(vertexID)) {
				final Set<Agent> vertexCollisionAgents = collidingVertices[i].get(vertexID);
				collisions += vertexCollisionAgents.size();
				collisionAgents.addAll(vertexCollisionAgents);
			}
		}

		return new Pair<>(collisions, collisionAgents);
	}

	private @NotNull Set<VertexWithDirection[]> cartesianProductWithCheck(final Agent @NotNull [] agents, Set<Integer>[] exitsIDs, final Map<Integer, Set<Integer>>[][] collisionPairs, final @NotNull CompositeState previousState, final VertexWithDirection[] @NotNull [] neighbours) {
		return cartesianProductWithCheck(agents, exitsIDs, collisionPairs, previousState, neighbours, new VertexWithDirection[agents.length], 0);
	}

	private @NotNull Set<VertexWithDirection[]> cartesianProductWithCheck(final Agent @NotNull [] agents, final Set<Integer>[] exitsIDs, final Map<Integer, Set<Integer>>[][] collisionPairs, final @NotNull CompositeState previousState, final VertexWithDirection[] @NotNull [] neighbours, final VertexWithDirection @NotNull [] constructed, final int addingIndex) {
		@NotNull Set<VertexWithDirection[]> cartesian = new HashSet<>();
		final int nextIndex = addingIndex + 1;

		for (@NotNull VertexWithDirection vertex : neighbours[addingIndex]) {
			int vertexID = vertex.getID();
			if (!exitsIDs[addingIndex].contains(vertexID) && collisionVertexPair(agents, collisionPairs, previousState, constructed, vertexID, addingIndex)) {
				continue;
			}

			VertexWithDirection[] clonedArray = constructed.clone();
			clonedArray[addingIndex] = vertex;
			if (nextIndex >= agents.length) {
				cartesian.add(clonedArray);
			} else {
				cartesian.addAll(cartesianProductWithCheck(agents, exitsIDs, collisionPairs, previousState, neighbours, clonedArray, nextIndex));
			}
		}

		return cartesian;
	}

	private boolean collisionVertexPair(final Agent @NotNull [] agents, @NotNull final Map<Integer, Set<Integer>>[][] collisionPairs,
																			final @NotNull CompositeState previousState, final VertexWithDirection[] neighboursCombination,
																			final int vertex, final int agentIndex) {
		final double agentPerimeter = agents[agentIndex].getAgentPerimeter();
		final int predecessorI = previousState.getStates()[agentIndex].getID();

		for (int j = 0; j < agentIndex; j++) {
			final int vertex1 = neighboursCombination[j].getID();
			final double neighbourPerimeter = agents[j].getAgentPerimeter();
			final double safePerimeter = agentPerimeter + neighbourPerimeter + safeDistance;

			@NotNull final Map<Integer, Set<Integer>> collisionPairIJ = collisionPairs[agentIndex][j];
			if (!collisionPairIJ.containsKey(vertex)) {
				@NotNull final Set<Integer> vertex0CollisionVertices = conflictVerticesSet(vertex, safePerimeter);
				collisionPairIJ.put(vertex, vertex0CollisionVertices);
			}

			if (collisionPairIJ.get(vertex).contains(vertex1)) {
				return true;
			}

			final int predecessorJ = previousState.getStates()[j].getID();
			if (safePerimeter >= graph.getTravelDistances()[predecessorI].get(vertex)[predecessorJ].get(vertex1)) {
				return true;
			}
		}
		return false;
	}

	private List<Integer> @NotNull [] constructPaths(@NotNull CompositeState state) {
		final int agents = state.getStates().length;
		final LinkedList<Integer> @NotNull [] paths = new LinkedList[agents];
		for (int i = 0; i < agents; i++) {
			paths[i] = new LinkedList<>();
			paths[i].add(state.getStates()[i].getID());
		}
		final boolean @NotNull [] notExitVertices = new boolean[agents];

		while (state != null) {
			for (int i = 0; i < agents; i++) {
				if (notExitVertices[i] || paths[i].get(0) != state.getStates()[i].getID()) {
					paths[i].addFirst(state.getStates()[i].getID());
					notExitVertices[i] = true;
				}
			}
			state = state.getParent();
		}

		ArrayList<Integer> @NotNull [] arrays = new ArrayList[agents];
		for (int i = 0; i < agents; i++) {
			arrays[i] = new ArrayList<>(paths[i]);
		}
		return arrays;
	}

	private @NotNull CompositeState generateInitialState(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, Set<Integer>[] exitsIDs, Agent @NotNull [] agents, double[][] heuristics, int[] maximumDelays, long step) {
		final VertexWithDirection @NotNull [] initialStates = new VertexWithDirection[agents.length];
		final @NotNull Iterator<Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator();
		for (int i = 0; it.hasNext(); i++) {
			final Entry<Agent, Pair<Integer, Set<Integer>>> agentPairEntry = it.next();
			final Agent agent = agentPairEntry.getKey();
			agents[i] = agent;

			final Set<Integer> exits = agentPairEntry.getValue().getVal1();
			exitsIDs[i] = exits;

			final int entryID = agentPairEntry.getValue().getVal0();
			final double startEstimate = getHeuristic(exits, entryID);
			final @NotNull VertexWithDirection initialState = new VertexWithDirection(graph.getVertex(entryID), 0, startEstimate);
			initialStates[i] = initialState;

			heuristics[i] = new double[graph.getVertices().length];
			heuristics[i][entryID] = startEstimate;

			maximumDelays[i] = exits.stream().mapToInt(exitID -> (int) (Math.ceil(graph.getDistance(agent.getEntry(), exitID)) + super.maximumPathDelay)).min().getAsInt();
		}

		return new CompositeState(initialStates, step);
	}

	@NotNull
	protected static Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> findCollisionGroup(
		@NotNull Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups,
		@NotNull Map<Collection<Agent>, Collection<Collection<Agent>>> resolvedPairs, Set<Agent> group0Agents, @NotNull Set<Agent> conflictAgents,
		@NotNull Map<Long, Map<Integer, Set<Agent>>> group0IllegalMovesTable, @NotNull Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable
	) {
		@Nullable Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet = null;

		for (final @NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group : agentsGroups) {
			final @NotNull Set<Agent> groupAgents = group.getVal0().keySet();
			if (groupAgents.equals(group0Agents)) {
				continue;
			}

			final Map<Long, Map<Integer, Agent>> groupConflictAvoidanceTable = group.getVal1();
			if (group1CollisionTriplet == null && !Collections.disjoint(groupAgents, conflictAgents)) {
				group1CollisionTriplet = group;
			} else if (resolvedPairs.get(group0Agents).contains(groupAgents)) {
				mergeConflictAvoidanceTables(group0IllegalMovesTable, groupConflictAvoidanceTable);
			} else {
				mergeConflictAvoidanceTables(restGroupsConflictAvoidanceTable, groupConflictAvoidanceTable);
			}
		}
		assert group1CollisionTriplet != null;
		return group1CollisionTriplet;
	}

	protected static void mergeIllegalMovesTables(@NotNull Map<Long, Map<Integer, Set<Agent>>> destinationMap, final @NotNull Map<Long, Map<Integer, Set<Agent>>> sourceMap) {
		for (@NotNull Entry<Long, Map<Integer, Set<Agent>>> sourceMapEntry : sourceMap.entrySet()) {
			final long step = sourceMapEntry.getKey();
			@NotNull Map<Integer, Set<Agent>> stepMap = destinationMap.computeIfAbsent(step, k -> new HashMap<>());
			for (@NotNull Entry<Integer, Set<Agent>> vertexEntry : sourceMapEntry.getValue().entrySet()) {
				Integer vertexID = vertexEntry.getKey();
				@NotNull Set<Agent> vertexSet = stepMap.computeIfAbsent(vertexID, k -> new HashSet<>());
				vertexSet.addAll(vertexEntry.getValue());
			}
		}
	}

	protected static void mergeConflictAvoidanceTables(@NotNull Map<Long, Map<Integer, Set<Agent>>> destinationMap, final @NotNull Map<Long, Map<Integer, Agent>> sourceMap) {
		for (@NotNull Entry<Long, Map<Integer, Agent>> sourceMapEntry : sourceMap.entrySet()) {
			final long step = sourceMapEntry.getKey();
			destinationMap.computeIfAbsent(step, k -> new HashMap<>());
			Map<Integer, Set<Agent>> destinationStepMap = destinationMap.get(step);

			for (@NotNull Entry<Integer, Agent> sourceEntry : sourceMapEntry.getValue().entrySet()) {
				final int vertex = sourceEntry.getKey();
				@NotNull Set<Agent> vertexSet = destinationStepMap.computeIfAbsent(vertex, k -> new HashSet<>());
				vertexSet.add(sourceEntry.getValue());
			}
		}
	}

	protected static @NotNull Map<Long, Map<Integer, Agent>> getConflictAvoidanceTable(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agents, long step) {
		@NotNull Map<Long, Map<Integer, Agent>> conflictAvoidanceTable = new HashMap<>();
		for (@NotNull Agent agent : agents.keySet()) {
			@NotNull ListIterator<Integer> it = agent.getPath().listIterator((int) (step - agent.getPlannedStep()));
			for (long i = step; it.hasNext(); i++) {
				int vertexID = it.next();
				conflictAvoidanceTable.computeIfAbsent(i, k -> new HashMap<>());
				conflictAvoidanceTable.get(i).put(vertexID, agent);
			}
		}

		return conflictAvoidanceTable;
	}

	private static boolean existsNoneValidNeighbour(int agentsCount, Set<Integer>[] exitsIDs, @NotNull CompositeState state, Set<Integer>[] neighbours) {
		for (int i = 0; i < agentsCount; i++) {
			if (neighbours[i].isEmpty() && !exitsIDs[i].contains(state.getStates()[i].getID())) {
				return true;
			}
		}
		return false;
	}

	private static boolean invalidState(long step, int agentsCount, Set<Integer>[] exitsIDs, int[] maximumDelays, @NotNull CompositeState state, long nextStep) {
		for (int i = 0; i < agentsCount; i++) {
			if (nextStep - step > maximumDelays[i] && !exitsIDs[i].contains(state.getStates()[i].getID())) {
				return true;
			}
		}
		return false;
	}

	protected class CompositeState implements Comparable<CompositeState> {
		private final VertexWithDirection[] states;
		private final long step;
		private final @Nullable CompositeState parent;
		private final int collisions;
		private final @NotNull Set<Agent> collisionAgents;

		protected CompositeState(VertexWithDirection[] states, long step) {
			this.states = states;
			this.step = step;
			this.collisions = 0;
			this.parent = null;
			this.collisionAgents = new HashSet<>();
		}

		protected CompositeState(VertexWithDirection[] states, @NotNull CompositeState parent, @NotNull Pair<Integer, Set<Agent>> collisionsAgentPair) {
			this.states = states;
			this.step = parent.step + 1;
			this.parent = parent;
			this.collisions = collisionsAgentPair.getVal0() + parent.collisions;
			Set<Agent> collisionAgentsSet = collisionsAgentPair.getVal1();
			collisionAgentsSet.addAll(parent.collisionAgents);
			this.collisionAgents = collisionAgentsSet;
		}

		protected boolean isFinal(Set<Integer>[] exitsIDs) {
			for (int i = 0; i < states.length; i++) {
				if (!exitsIDs[i].contains(states[i].getID())) {
					return false;
				}
			}

			return true;
		}

		public VertexWithDirection[] getStates() {
			return states;
		}

		public long getStep() {
			return step;
		}

		public CompositeState getParent() {
			return parent;
		}

		@Override
		public int compareTo(@NotNull final CompositeState compositeState) {
			final int heuristicsComparison = Double.compare(getHeuristics(), compositeState.getHeuristics());
			if (heuristicsComparison == 0) {
				final int turnPenaltyComparison = Double.compare(getTurnPenalty(), compositeState.getTurnPenalty());
				if (turnPenaltyComparison == 0) {
					final int turnsComparison = Integer.compare(compositeState.getTurns(), getTurns());  // prefer higher number of turns
					if (turnsComparison == 0) {
						final int collisionsComparison = Integer.compare(collisionAgents.size(), compositeState.collisionAgents.size());
						if (collisionsComparison == 0) {
							final int collisionsCountComparison = Integer.compare(collisions, compositeState.collisions);
							if (collisionsCountComparison == 0) {
								final int distanceComparison = Double.compare(compositeState.getDistance(), getDistance());   // prefer vertex with greater traveled distance
								if (distanceComparison == 0) {
									for (int i = 0; i < states.length; i++) {
										final int idComparison = Integer.compare(states[i].getID(), compositeState.states[i].getID());
										if (idComparison != 0) {
											return idComparison;
										}
									}
									return 0;
								}
								return distanceComparison;
							}
							return collisionsCountComparison;
						}
						return collisionsComparison;
					}
					return turnsComparison;
				}
				return turnPenaltyComparison;
			}
			return heuristicsComparison;
		}

		protected double getDistance() {
			double distance = 0;
			for (@Nullable VertexWithDirection vertex : states) {
				if (vertex != null) {
					distance += vertex.getDistance();
				}
			}

			return distance;
		}

		protected double getTurnPenalty() {
			double turnPenalty = 0;
			for (@Nullable VertexWithDirection vertex : states) {
				if (vertex != null) {
					turnPenalty += vertex.getTurnPenalty();
				}
			}

			return turnPenalty;
		}

		protected int getTurns() {
			int turns = 0;
			for (@Nullable VertexWithDirection vertex : states) {
				if (vertex != null) {
					turns += vertex.getTurns();
				}
			}

			return turns;
		}

		protected double getHeuristics() {
			double estimate = 0;
			for (@Nullable VertexWithDirection vertex : states) {
				if (vertex != null) {
					estimate += vertex.getEstimate() + vertex.getDistance();
				}
			}

			return estimate;
		}

		@Override
		public int hashCode() {
			int result = Arrays.hashCode(states);
			result = 31 * result + (int) (step ^ (step >>> 32));
			return result;
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			@NotNull CompositeState compositeState = (CompositeState) o;

			if (step != compositeState.step) {
				return false;
			}

			// Probably incorrect - comparing Object[] arrays with Arrays.equals
			final boolean statesEqual = Arrays.equals(states, compositeState.states);
//			if (!allowAgentReturn && statesEqual && parent != null) {  FIXME
//				assert compositeState.parent != null;
//				return Arrays.equals(parent.states, compositeState.parent.states);
//			}

			return statesEqual;
		}
	}
}
