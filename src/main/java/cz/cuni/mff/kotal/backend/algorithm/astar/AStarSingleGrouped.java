package cz.cuni.mff.kotal.backend.algorithm.astar;

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
import java.util.function.Function;
import java.util.stream.Collectors;

public class AStarSingleGrouped extends AStarSingle {

	public static final Map<String, Object> PARAMETERS = AStarSingle.PARAMETERS;

	public AStarSingleGrouped(SimulationGraph graph) {
		super(graph);
	}

	@TestOnly
	protected AStarSingleGrouped(SimulationGraph graph, double safeDistance, int maximumVertexVisits, boolean allowAgentStop, int maximumPathDelay, boolean allowAgentReturn) {
		super(graph, safeDistance, maximumVertexVisits, allowAgentStop, maximumPathDelay, allowAgentReturn);
	}

	@NotNull
	protected static Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> findCollisionGroup(
		Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups,
		Map<Collection<Agent>, Collection<Collection<Agent>>> resolvedPairs, Set<Agent> group0Agents, Set<Agent> conflictAgents,
		Map<Long, Map<Integer, Set<Agent>>> group0IllegalMovesTable, Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable
	) {
		Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet = null;

		for (final Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group : agentsGroups) {
			final Set<Agent> groupAgents = group.getVal0().keySet();
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

	protected static void mergeIllegalMovesTables(Map<Long, Map<Integer, Set<Agent>>> destinationMap, final Map<Long, Map<Integer, Set<Agent>>> sourceMap) {
		for (Entry<Long, Map<Integer, Set<Agent>>> sourceMapEntry : sourceMap.entrySet()) {
			final long step = sourceMapEntry.getKey();
			Map<Integer, Set<Agent>> stepMap = destinationMap.computeIfAbsent(step, k -> new HashMap<>());
			for (Entry<Integer, Set<Agent>> vertexEntry : sourceMapEntry.getValue().entrySet()) {
				Integer vertexID = vertexEntry.getKey();
				Set<Agent> vertexSet = stepMap.computeIfAbsent(vertexID, k -> new HashSet<>());
				vertexSet.addAll(vertexEntry.getValue());
			}
		}
	}

	protected static void mergeConflictAvoidanceTables(Map<Long, Map<Integer, Set<Agent>>> destinationMap, final Map<Long, Map<Integer, Agent>> sourceMap) {
		for (Entry<Long, Map<Integer, Agent>> sourceMapEntry : sourceMap.entrySet()) {
			final long step = sourceMapEntry.getKey();
			destinationMap.computeIfAbsent(step, k -> new HashMap<>());
			Map<Integer, Set<Agent>> destinationStepMap = destinationMap.get(step);

			for (Entry<Integer, Agent> sourceEntry : sourceMapEntry.getValue().entrySet()) {
				final int vertex = sourceEntry.getKey();
				Set<Agent> vertexSet = destinationStepMap.computeIfAbsent(vertex, k -> new HashSet<>());
				vertexSet.add(sourceEntry.getValue());
			}
		}
	}

	protected static Map<Long, Map<Integer, Agent>> getConflictAvoidanceTable(Map<Agent, Pair<Integer, Set<Integer>>> agents, long step) {
		Map<Long, Map<Integer, Agent>> conflictAvoidanceTable = new HashMap<>();
		for (Agent agent : agents.keySet()) {
			ListIterator<Integer> it = agent.getPath().listIterator((int) (step - agent.getPlannedTime()));
			for (long i = step; it.hasNext(); i++) {
				int vertexID = it.next();
				conflictAvoidanceTable.computeIfAbsent(i, k -> new HashMap<>());
				conflictAvoidanceTable.get(i).put(vertexID, agent);
			}
		}

		return conflictAvoidanceTable;
	}

	private static boolean existsNoneValidNeighbour(int agentsCount, Set<Integer>[] exitsIDs, CompositeState state, Set<Integer>[] neighbours) {
		for (int i = 0; i < agentsCount; i++) {
			if (neighbours[i].isEmpty() && !exitsIDs[i].contains(state.getStates()[i].getID())) {
				return true;
			}
		}
		return false;
	}

	private static boolean invalidState(long step, int agentsCount, Set<Integer>[] exitsIDs, int[] maximumDelays, CompositeState state, long nextStep) {
		for (int i = 0; i < agentsCount; i++) {
			if (nextStep - step > maximumDelays[i] && !exitsIDs[i].contains(state.getStates()[i].getID())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		filterStepOccupiedVertices(step);
		return planAgents(agents.stream().collect(Collectors.toMap(Function.identity(), a -> new Pair<>(a.getEntry(), getExitIDs(a)))), step);
	}

	@Override
	public Collection<Agent> planAgents(final Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		// filter agents that cannot enter intersection
//		filterAgentsWithNoEntry(agentsEntriesExits, step);

		if (agentsEntriesExits.isEmpty()) {
			return Collections.emptySet();
		}

		final Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups = createInitialPaths(agentsEntriesExits, step);

		if (agentsEntriesExits.isEmpty()) {
			return Collections.emptySet();
		}

		solveAgentsCollisions(step, agentsGroups);

		if (stopped) {
			return Collections.emptySet();
		}

		Set<Agent> plannedAgents = new HashSet<>();
		for (Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group : agentsGroups) {
			Set<Agent> groupAgents = group.getVal0().keySet();

			plannedAgents.addAll(groupAgents);
			groupAgents.forEach(this::addPlannedAgent);
		}

		return plannedAgents;
	}

	protected void solveAgentsCollisions(long step, Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups) {
		Map<Collection<Agent>, Collection<Collection<Agent>>> resolvedPairs = new HashMap<>();

		Optional<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> collisionTripletOptional;
		while (!stopped && (collisionTripletOptional = inCollision(agentsGroups)).isPresent()) {
			Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet = collisionTripletOptional.get();

			Map<Agent, Pair<Integer, Set<Integer>>> group0 = group0CollisionTriplet.getVal0();
			Set<Agent> group0Agents = new HashSet<>(group0.keySet());
			resolvedPairs.computeIfAbsent(group0Agents, k -> new HashSet<>());

			final Set<Agent> conflictAgents = group0CollisionTriplet.getVal2();

			Map<Long, Map<Integer, Set<Agent>>> group0IllegalMovesTable = new HashMap<>();
			Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable = new HashMap<>();

			Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet = findCollisionGroup(agentsGroups, resolvedPairs, group0Agents, conflictAgents, group0IllegalMovesTable, restGroupsConflictAvoidanceTable);

			Map<Agent, Pair<Integer, Set<Integer>>> group1;
			Set<Agent> group1Agents;
			Map<Long, Map<Integer, Set<Agent>>> group1IllegalMovesTable = null;
			if (switchGroups(group0Agents, group1CollisionTriplet.getVal0().keySet())) {
				group1 = group1CollisionTriplet.getVal0();
				group1Agents = new HashSet<>(group1.keySet());
			} else {
				group1 = group0;
				group0 = group1CollisionTriplet.getVal0();
				final Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> temp = group0CollisionTriplet;
				group0CollisionTriplet = group1CollisionTriplet;
				group1CollisionTriplet = temp;

				group1Agents = group0Agents;
				group0Agents = new HashSet<>(group0.keySet());
				resolvedPairs.computeIfAbsent(group0Agents, k -> new HashSet<>());

				mergeIllegalMovesTables(restGroupsConflictAvoidanceTable, group0IllegalMovesTable);
				group1IllegalMovesTable = group0IllegalMovesTable;
				group0IllegalMovesTable = getIllegalMovesTable(agentsGroups, resolvedPairs, group1CollisionTriplet, group0Agents);
			}

			Map<Long, Map<Integer, Set<Agent>>> group0IllegalMovesTableComplete = copyMap(group0IllegalMovesTable);
			mergeConflictAvoidanceTables(group0IllegalMovesTableComplete, stepOccupiedVertices);

			if (replanGroup(step, agentsGroups, group0CollisionTriplet, group1CollisionTriplet, group0, group0IllegalMovesTableComplete, restGroupsConflictAvoidanceTable)) {
				resolvedPairs.get(group0Agents).add(group1Agents);
				resolvedPairs.computeIfAbsent(group1Agents, k -> new HashSet<>());
				resolvedPairs.get(group1Agents).add(group0Agents);
				System.out.println("Replan 0: " + group0Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")) + "; in collision with 1: " + group1Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")));
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
				System.out.println("Replan 1: " + group1Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")) + "; in collision with 0: " + group0Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")));
				continue;
			}

			tryMergeGroups(step, agentsGroups, group0CollisionTriplet, group1CollisionTriplet, restGroupsConflictAvoidanceTable, group1IllegalMovesTable);
		}
	}

	protected void tryMergeGroups(
		long step, Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups,
		Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet,
		Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet,
		Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable, Map<Long, Map<Integer, Set<Agent>>> group1IllegalMovesTable
	) {
		Map<Agent, Pair<Integer, Set<Integer>>> group1 = group1CollisionTriplet.getVal0();

		final Map<Agent, Pair<Integer, Set<Integer>>> combinedAgents = new HashMap<>(group0CollisionTriplet.getVal0());
		combinedAgents.putAll(group1);
		@Nullable Set<Agent> collisionAgents = findPaths(combinedAgents, step, mapStepOccupiedVertices(), restGroupsConflictAvoidanceTable);
		agentsGroups.remove(group0CollisionTriplet);
		filterReplannedCollisionAgents(agentsGroups, group0CollisionTriplet.getVal0().keySet());
		filterReplannedCollisionAgents(agentsGroups, group1CollisionTriplet.getVal0().keySet());
		if (collisionAgents != null) {
			agentsGroups.remove(group1CollisionTriplet);

			agentsGroups.add(new Triplet<>(combinedAgents, getConflictAvoidanceTable(combinedAgents, step), collisionAgents));
		} else {
			final Set<Agent> group1Collisions = findPaths(group1, step, group1IllegalMovesTable, restGroupsConflictAvoidanceTable);
			if (group1Collisions != null) {
				updateAgentsGroup(step, group1CollisionTriplet, group1Collisions);
			}
		}
	}

	protected boolean switchGroups(Set<Agent> group0Agents, Set<Agent> group1Agents) {
		return group0Agents.size() <= group1Agents.size();
	}

	protected Map<Long, Map<Integer, Set<Agent>>> getIllegalMovesTable(Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups, Map<Collection<Agent>, Collection<Collection<Agent>>> resolvedPairs, Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> conflictGroupCollisionTriplet, Set<Agent> groupAgents) {
		Map<Long, Map<Integer, Set<Agent>>> illegalMovesTable;
		illegalMovesTable = new HashMap<>();
		resolvedPairs.computeIfAbsent(groupAgents, k -> new HashSet<>());
		if (resolvedPairs.containsKey(groupAgents) && !resolvedPairs.get(groupAgents).isEmpty()) {
			for (Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group : agentsGroups) {
				if (group != conflictGroupCollisionTriplet && resolvedPairs.get(groupAgents).contains(group.getVal0().keySet())) {
					mergeConflictAvoidanceTables(illegalMovesTable, group.getVal1());
				}
			}
		}
		return illegalMovesTable;
	}

	protected boolean replanGroup(long step, Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups,
																Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet,
																Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet,
																Map<Agent, Pair<Integer, Set<Integer>>> group0, Map<Long, Map<Integer, Set<Agent>>> invalidMovesMap,
																Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable) {
		Map<Long, Map<Integer, Set<Agent>>> group0IllegalMovesTable = copyMap(invalidMovesMap);
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

	protected Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> createInitialPaths(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		return createInitialPaths(agentsEntriesExits, step, new HashMap<>());
	}

	protected Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> createInitialPaths(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, final long step, final Map<Long, Map<Integer, Set<Agent>>> conflictAvoidanceTable) {
		final Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups = new HashSet<>(agentsEntriesExits.size());

		for (Iterator<Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator(); it.hasNext(); ) {
			final Entry<Agent, Pair<Integer, Set<Integer>>> agentEntryExitsEntry = it.next();
			final Agent agent = agentEntryExitsEntry.getKey();
			final int agentEntry = agentEntryExitsEntry.getValue().getVal0();
			if (!safeVertex(step, agentEntry, agent.getAgentPerimeter())) {
				it.remove();
				continue;
			}
			final Map<Agent, Pair<Integer, Set<Integer>>> agentEntryExitsMap = new HashMap<>(1);
			agentEntryExitsMap.put(agent, agentEntryExitsEntry.getValue());

			final @Nullable Set<Agent> collisionAgents = findPaths(agentEntryExitsMap, step, mapStepOccupiedVertices(), conflictAvoidanceTable);
			if (collisionAgents == null) {
				it.remove();
			} else {
				final Map<Long, Map<Integer, Agent>> agentConflictAvoidanceTable = getConflictAvoidanceTable(agentEntryExitsMap, step);
				agentsGroups.add(new Triplet<>(agentEntryExitsMap, agentConflictAvoidanceTable, collisionAgents));
				mergeConflictAvoidanceTables(conflictAvoidanceTable, agentConflictAvoidanceTable);
			}
		}

		return agentsGroups;
	}

	private void filterAgentsWithNoEntry(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		for (Iterator<Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator(); it.hasNext(); ) {
			final Entry<Agent, Pair<Integer, Set<Integer>>> entry = it.next();
			final Agent agent = entry.getKey();
			final int agentEntry = entry.getValue().getVal0();
			if (!safeVertex(step, agentEntry, agent.getAgentPerimeter())) {
				it.remove();
			}
		}
	}

	protected void filterReplannedCollisionAgents(Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups, Set<Agent> replannedAgents) {
		for (Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> agentGroup : agentsGroups) {
			final Set<Agent> collisionAgents = agentGroup.getVal2();
			collisionAgents.removeAll(replannedAgents);
		}
	}

	protected Map<Long, Map<Integer, Set<Agent>>> copyMap(Map<Long, Map<Integer, Set<Agent>>> sourceMap) {
		Map<Long, Map<Integer, Set<Agent>>> copy = new HashMap<>(sourceMap.size());
		for (Entry<Long, Map<Integer, Set<Agent>>> entry : sourceMap.entrySet()) {
			Map<Integer, Set<Agent>> verticesMap = new HashMap<>(entry.getValue().size());
			copy.put(entry.getKey(), verticesMap);
			for (Entry<Integer, Set<Agent>> vertexEntry : entry.getValue().entrySet()) {
				verticesMap.put(vertexEntry.getKey(), new HashSet<>(vertexEntry.getValue()));
			}
		}

		return copy;
	}

	protected Map<Long, Map<Integer, Set<Agent>>> mapStepOccupiedVertices() {
		final Map<Long, Map<Integer, Set<Agent>>> newMap = new HashMap<>(stepOccupiedVertices.size());
		for (Entry<Long, Map<Integer, Agent>> stepEntry : stepOccupiedVertices.entrySet()) {
			final HashMap<Integer, Set<Agent>> stepMap = new HashMap<>(stepEntry.getValue().size());
			newMap.put(stepEntry.getKey(), stepMap);
			for (Entry<Integer, Agent> vertexEntry : stepEntry.getValue().entrySet()) {
				stepMap.put(vertexEntry.getKey(), Set.of(vertexEntry.getValue()));
			}
		}

		return newMap;
	}

	protected void updateAgentsGroup(long step, Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> collisionTriplet, @Nullable Set<Agent> collisionAgents) {
		collisionTriplet.setVal1(getConflictAvoidanceTable(collisionTriplet.getVal0(), step));
		collisionTriplet.setVal2(collisionAgents);
	}

	protected Optional<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> inCollision(Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups) {
		return agentsGroups.stream().filter(triplet -> !triplet.getVal2().isEmpty()).min(Comparator.comparingInt(g -> g.getVal0().size()));
	}

	protected @Nullable Set<Agent> findPaths(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step, final Map<Long, Map<Integer, Set<Agent>>> illegalMovesTable, final Map<Long, Map<Integer, Set<Agent>>> conflictAvoidanceTable) {
		assert stepOccupiedVertices.entrySet().stream().allMatch(s -> s.getValue().entrySet().stream().allMatch(v -> illegalMovesTable.get(s.getKey()).get(v.getKey()).contains(v.getValue())));

		final int agentsCount = agentsEntriesExits.size();
		final Set<Integer>[] exitsIDs = new Set[agentsCount];
		final Agent[] agents = new Agent[agentsCount];
		final double[][] heuristics = new double[agentsCount][];
		final int[] maximumDelays = new int[agentsCount];

		final Set<CompositeState> visitedStates = new HashSet<>();
		final Map<Integer, Set<Integer>>[][] collisionPairs = new Map[agentsCount][];
		final Map<Integer, Map<Pair<Integer, Integer>, Pair<Boolean, Boolean>>>[][] collisionTransfers = new Map[agentsCount][];
		for (int i = 0; i < agentsCount; i++) {
			collisionPairs[i] = new Map[i];
			collisionTransfers[i] = new Map[i];
			for (int j = 0; j < i; j++) {
				collisionPairs[i][j] = new HashMap<>();
				collisionTransfers[i][j] = new HashMap<>();
			}
		}

		final CompositeState initialState = generateInitialState(agentsEntriesExits, exitsIDs, agents, heuristics, maximumDelays, step);
		final PriorityQueue<CompositeState> queue = new PriorityQueue<>();
		queue.add(initialState);

		while (!queue.isEmpty() && !stopped) {
			final CompositeState state = queue.poll();

			if (state.isFinal(exitsIDs)) {
				constructPaths(step, agentsCount, agents, state);
				return state.collisionAgents;
			}

			if (visitedStates.contains(state)) {
				continue;
			} else {
				visitedStates.add(state);
			}

			long stateStep = state.getStep();
			final long nextStep = stateStep + 1;

			// check if all non-finished agents can continue their journey
			if (invalidState(step, agentsCount, exitsIDs, maximumDelays, state, nextStep)) {
				continue;
			}

			final Set<Integer>[] neighbours = new Set[agentsCount];
			final Map<Integer, Integer>[] neighboursVisited = new Map[agentsCount];
			generateIndividualNeighbours(step, agentsCount, exitsIDs, heuristics, maximumDelays, state, nextStep, neighbours, neighboursVisited);

			// Remove neighbours visited too many times
			filterVisitedNeighbours(agentsCount, state, neighbours, neighboursVisited);


			// remove colliding neighbours
			final Map<Integer, Set<Agent>>[] neighboursCollisions = filterCollidingNeighbours(illegalMovesTable, conflictAvoidanceTable, agents, state, stateStep, nextStep, neighbours);

			// if any traveling agent does not have any valid neighbours, skip generating neighbours states
			if (existsNoneValidNeighbour(agentsCount, exitsIDs, state, neighbours)) {
				continue;
			}

			// for each valid neighbour create vertex object
			final VertexWithDirection[][] neighboursVertices = generateVertexObjects(agentsCount, heuristics, state, neighbours);

			// add cartesian product of agents position vertex neighbours to queue
			Set<VertexWithDirection[]> cartesianNeighbours = cartesianProductWithCheck(agents, exitsIDs, collisionPairs, collisionTransfers, state, neighboursVertices);
			for (VertexWithDirection[] neighboursCombination : cartesianNeighbours) {
				CompositeState nextState = new CompositeState(neighboursCombination, state, collisionCount(neighboursCombination, neighboursCollisions));
				if (visitedStates.contains(nextState)) {
					continue;
				}
				queue.add(nextState);
			}
		}

		return null;
	}

	@NotNull
	private VertexWithDirection[][] generateVertexObjects(int agentsCount, double[][] heuristics, CompositeState state, Set<Integer>[] neighbours) {
		final VertexWithDirection[][] neighboursVertices = new VertexWithDirection[agentsCount][];
		for (int i = 0; i < agentsCount; i++) {
			final VertexWithDirection stateVertex = state.getStates()[i];
			final Set<Integer> vertexNeighbours = neighbours[i];
			if (vertexNeighbours.isEmpty()) {
				neighboursVertices[i] = new VertexWithDirection[]{stateVertex};
			} else {
				VertexWithDirection[] neighboursWithVisit = new VertexWithDirection[vertexNeighbours.size()];
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

	private Map<Integer, Set<Agent>>[] filterCollidingNeighbours(final Map<Long, Map<Integer, Set<Agent>>> illegalMovesTable, final Map<Long, Map<Integer, Set<Agent>>> conflictAvoidanceTable, final Agent[] agents, final CompositeState state, long stateStep, long nextStep, Set<Integer>[] neighbours) {
		final Map<Integer, Set<Agent>>[] neighboursCollisions = new Map[neighbours.length];

		for (int i = 0; i < agents.length; i++) {
			final int stateID = state.getStates()[i].getID();
			final Map<Integer, Set<Agent>> agentNeighboursCollisions = new HashMap<>();
			neighboursCollisions[i] = agentNeighboursCollisions;
			for (final Iterator<Integer> iterator = neighbours[i].iterator(); iterator.hasNext(); ) {
				final int neighbourID = iterator.next();
				final double agentPerimeter = agents[i].getAgentPerimeter();
				if (safeVertex(illegalMovesTable, nextStep, neighbourID, agentPerimeter) &&
					safeStepTo(illegalMovesTable, nextStep, neighbourID, stateID, agentPerimeter) &&
					safeStepFrom(illegalMovesTable, stateStep, stateID, neighbourID, agentPerimeter)
				) {
					assert !agentNeighboursCollisions.containsKey(neighbourID);
					agentNeighboursCollisions.put(neighbourID, new HashSet<>());
					agentNeighboursCollisions.get(neighbourID).addAll(collisionAgentsAtVertex(conflictAvoidanceTable, nextStep, neighbourID, agentPerimeter));
					agentNeighboursCollisions.get(neighbourID).addAll(collisionsOnWayTo(conflictAvoidanceTable, nextStep, neighbourID, stateID, agentPerimeter));
					agentNeighboursCollisions.get(neighbourID).addAll(collisionsOnWayFrom(conflictAvoidanceTable, stateStep, stateID, neighbourID, agentPerimeter));
				} else {
					iterator.remove();
				}
			}
		}

		return neighboursCollisions;
	}

	private void filterVisitedNeighbours(int agentsCount, CompositeState state, Set<Integer>[] neighbours, Map<Integer, Integer>[] neighboursVisited) {
		CompositeState predecessorState = state;
		while (predecessorState != null) {
			for (int i = 0; i < agentsCount; i++) {
				int predecessorVertexID = predecessorState.getStates()[i].getID();
				if (neighbours[i].contains(predecessorVertexID)) {
					int vertexVisits = neighboursVisited[i].get(predecessorVertexID);
					if (vertexVisits >= maximumVertexVisits) {
						neighbours[i].remove(predecessorVertexID);
					} else {
						neighboursVisited[i].put(predecessorVertexID, vertexVisits + 1);
					}
				}
			}
			predecessorState = predecessorState.getParent();
		}
	}

	private void generateIndividualNeighbours(long step, int agentsCount, Set<Integer>[] exitsIDs, double[][] heuristics, int[] maximumDelays, CompositeState state, long nextStep, Set<Integer>[] neighbours, Map<Integer, Integer>[] neighboursVisited) {
		for (int i = 0; i < agentsCount; i++) {
			Set<Integer> vertexNeighbours = new HashSet<>(state.getStates()[i].getVertex().getNeighbourIDs());
			neighbours[i] = vertexNeighbours;
			if (allowAgentStop && state.getParent() != null) {
				vertexNeighbours.add(state.getStates()[i].getID());
			}

			if (!allowAgentReturn && state.getParent() != null) {
				vertexNeighbours.remove(state.getParent().getStates()[i].getID());
			}

			neighboursVisited[i] = new HashMap<>(neighbours[i].size());
			for (Iterator<Integer> iterator = vertexNeighbours.iterator(); iterator.hasNext(); ) {
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

	private void constructPaths(long step, int agentsCount, Agent[] agents, CompositeState state) {
		final List<Integer>[] paths = constructPaths(state);
		for (int i = 0; i < agentsCount; i++) {
			Agent agent = agents[i];
			agent.setPath(paths[i], step);
		}
	}

	private Pair<Integer, Set<Agent>> collisionCount(VertexWithDirection[] vertices, Map<Integer, Set<Agent>>[] collidingVertices) {
		int collisions = 0;
		Set<Agent> collisionAgents = new HashSet<>();
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

	private Set<VertexWithDirection[]> cartesianProductWithCheck(final Agent[] agents, Set<Integer>[] exitsIDs, final Map<Integer, Set<Integer>>[][] collisionPairs, final Map<Integer, Map<Pair<Integer, Integer>, Pair<Boolean, Boolean>>>[][] collisionTransfers, final CompositeState previousState, final VertexWithDirection[][] neighbours) {
		return cartesianProductWithCheck(agents, exitsIDs, collisionPairs, collisionTransfers, previousState, neighbours, new VertexWithDirection[agents.length], 0);
	}

	private Set<VertexWithDirection[]> cartesianProductWithCheck(final Agent[] agents, final Set<Integer>[] exitsIDs, final Map<Integer, Set<Integer>>[][] collisionPairs, final Map<Integer, Map<Pair<Integer, Integer>, Pair<Boolean, Boolean>>>[][] collisionTransfers, final CompositeState previousState, final VertexWithDirection[][] neighbours, final VertexWithDirection[] constructed, final int addingIndex) {
		Set<VertexWithDirection[]> cartesian = new HashSet<>();
		final int nextIndex = addingIndex + 1;

		for (VertexWithDirection vertex : neighbours[addingIndex]) {
			int vertexID = vertex.getID();
			if (!exitsIDs[addingIndex].contains(vertexID) && collisionVertexPair(agents, collisionPairs, collisionTransfers, previousState, constructed, vertexID, addingIndex)) {
				continue;
			}

			VertexWithDirection[] clonedArray = constructed.clone();
			clonedArray[addingIndex] = vertex;
			if (nextIndex >= agents.length) {
				cartesian.add(clonedArray);
			} else {
				cartesian.addAll(cartesianProductWithCheck(agents, exitsIDs, collisionPairs, collisionTransfers, previousState, neighbours, clonedArray, nextIndex));
			}
		}

		return cartesian;
	}

	private boolean collisionVertexPair(final Agent[] agents, final Map<Integer, Set<Integer>>[][] collisionPairs, final Map<Integer, Map<Pair<Integer, Integer>, Pair<Boolean, Boolean>>>[][] collisionTransfers, final CompositeState previousState, final VertexWithDirection[] neighboursCombination, final int vertex, int agentIndex) {
		final double agentPerimeter = agents[agentIndex].getAgentPerimeter();
		for (int j = 0; j < agentIndex; j++) {
			final int vertex1 = neighboursCombination[j].getID();
			final double neighbourPerimeter = agents[j].getAgentPerimeter();
			Map<Integer, Set<Integer>> collisionPairIJ = collisionPairs[agentIndex][j];
			if (!collisionPairIJ.containsKey(vertex)) {
				Set<Integer> vertex0CollisionVertices = conflictVerticesSet(vertex, agentPerimeter + neighbourPerimeter);
				collisionPairIJ.put(vertex, vertex0CollisionVertices);
			}

			if (collisionPairIJ.get(vertex).contains(vertex1)) {
				return true;
			}

			if (vertex == previousState.getStates()[j].getID()) {
				collisionTransfers[agentIndex][j].computeIfAbsent(vertex, k -> new HashMap<>());

				Map<Pair<Integer, Integer>, Pair<Boolean, Boolean>> vertex0CollisionPairs = collisionTransfers[agentIndex][j].get(vertex);
				int predecessorI = previousState.getStates()[agentIndex].getID();
				Pair<Integer, Integer> predINextJPair = new Pair<>(predecessorI, vertex1);
				vertex0CollisionPairs.computeIfAbsent(predINextJPair, k -> new Pair<>(null, null));
				Pair<Boolean, Boolean> transferCollision = vertex0CollisionPairs.get(predINextJPair);
				if (transferCollision.getVal0() == null) {
					transferCollision.setVal0(!checkNeighbour(vertex, predecessorI, vertex1, agentPerimeter, neighbourPerimeter));
				}

				if (transferCollision.getVal0()) {
					return true;
				}
			}

			if (vertex1 == previousState.getStates()[agentIndex].getID()) {
				collisionTransfers[agentIndex][j].computeIfAbsent(vertex, k -> new HashMap<>());

				Map<Pair<Integer, Integer>, Pair<Boolean, Boolean>> vertex0CollisionPairs = collisionTransfers[agentIndex][j].get(vertex);
				int predecessorJ = previousState.getStates()[j].getID();
				Pair<Integer, Integer> nextIPredJPair = new Pair<>(vertex, predecessorJ);
				vertex0CollisionPairs.computeIfAbsent(nextIPredJPair, k -> new Pair<>(null, null));
				Pair<Boolean, Boolean> transferCollision = vertex0CollisionPairs.get(nextIPredJPair);
				if (transferCollision.getVal1() == null) {
					transferCollision.setVal1(!checkNeighbour(vertex1, predecessorJ, vertex, neighbourPerimeter, agentPerimeter));
				}

				if (transferCollision.getVal1()) {
					return true;
				}
			}
		}
		return false;
	}

	private List<Integer>[] constructPaths(CompositeState state) {
		final int agents = state.getStates().length;
		final LinkedList<Integer>[] paths = new LinkedList[agents];
		for (int i = 0; i < agents; i++) {
			paths[i] = new LinkedList<>();
			paths[i].add(state.getStates()[i].getID());
		}
		final boolean[] notExitVertices = new boolean[agents];

		while (state != null) {
			for (int i = 0; i < agents; i++) {
				if (notExitVertices[i] || paths[i].get(0) != state.getStates()[i].getID()) {
					paths[i].addFirst(state.getStates()[i].getID());
					notExitVertices[i] = true;
				}
			}
			state = state.getParent();
		}

		return paths;
	}

	private CompositeState generateInitialState(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, Set<Integer>[] exitsIDs, Agent[] agents, double[][] heuristics, int[] maximumDelays, long step) {
		final VertexWithDirection[] initialStates = new VertexWithDirection[agents.length];
		final Iterator<Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator();
		for (int i = 0; it.hasNext(); i++) {
			final Entry<Agent, Pair<Integer, Set<Integer>>> agentPairEntry = it.next();
			final Agent agent = agentPairEntry.getKey();
			agents[i] = agent;

			final Set<Integer> exits = agentPairEntry.getValue().getVal1();
			exitsIDs[i] = exits;

			final int entryID = agentPairEntry.getValue().getVal0();
			final double startEstimate = getHeuristic(exits, entryID);
			final VertexWithDirection initialState = new VertexWithDirection(graph.getVertex(entryID), 0, startEstimate);
			initialStates[i] = initialState;

			heuristics[i] = new double[graph.getVertices().length];
			heuristics[i][entryID] = startEstimate;

			maximumDelays[i] = exits.stream().mapToInt(exitID -> (int) (Math.ceil(graph.getDistance(entryID, exitID)) + super.maximumPathDelay)).min().getAsInt();
		}

		return new CompositeState(initialStates, step);
	}

	protected class CompositeState implements Comparable<CompositeState> {
		private final VertexWithDirection[] states;
		private final long step;
		private final CompositeState parent;
		private final int collisions;
		private final Set<Agent> collisionAgents;

		protected CompositeState(VertexWithDirection[] states, long step) {
			this.states = states;
			this.step = step;
			this.collisions = 0;
			this.parent = null;
			this.collisionAgents = new HashSet<>();
		}

		protected CompositeState(VertexWithDirection[] states, CompositeState parent, Pair<Integer, Set<Agent>> collisionsAgentPair) {
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
		public int compareTo(@NotNull final CompositeState collectiveState) {
			final int heuristicsComparison = Double.compare(getHeuristics(), collectiveState.getHeuristics());
			if (heuristicsComparison == 0) {
				final int turnPenaltyComparison = Double.compare(getTurnPenalty(), collectiveState.getTurnPenalty());
				if (turnPenaltyComparison == 0) {
					final int turnsComparison = Integer.compare(collectiveState.getTurns(), getTurns());  // prefer higher number of turns
					if (turnsComparison == 0) {
						final int collisionsComparison = Integer.compare(collisionAgents.size(), collectiveState.collisionAgents.size());
						if (collisionsComparison == 0) {
							final int collisionsCountComparison = Integer.compare(collisions, collectiveState.collisions);
							if (collisionsCountComparison == 0) {
								final int distanceComparison = Double.compare(collectiveState.getDistance(), getDistance());   // prefer vertex with greater traveled distance
								if (distanceComparison == 0) {
									for (int i = 0; i < states.length; i++) {
										int idComparison = Integer.compare(states[i].getID(), collectiveState.states[i].getID());
										if (idComparison != 0) {
											return idComparison;
										}
									}
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
			for (VertexWithDirection vertex : states) {
				if (vertex != null) {
					distance += vertex.getDistance();
				}
			}

			return distance;
		}

		protected double getTurnPenalty() {
			double turnPenalty = 0;
			for (VertexWithDirection vertex : states) {
				if (vertex != null) {
					turnPenalty += vertex.getTurnPenalty();
				}
			}

			return turnPenalty;
		}

		protected int getTurns() {
			int turns = 0;
			for (VertexWithDirection vertex : states) {
				if (vertex != null) {
					turns += vertex.getTurns();
				}
			}

			return turns;
		}

		protected double getHeuristics() {
			double estimate = 0;
			for (VertexWithDirection vertex : states) {
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
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CompositeState compositeState = (CompositeState) o;

			if (step != compositeState.step) {
				return false;
			}

			// Probably incorrect - comparing Object[] arrays with Arrays.equals
			final boolean statesEqual = Arrays.equals(states, compositeState.states);
			if (!allowAgentReturn && statesEqual && parent != null) {
				assert compositeState.parent != null;
				return Arrays.equals(parent.states, compositeState.parent.states);
			}

			return statesEqual;
		}
	}
}
