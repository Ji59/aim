package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Triplet;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.VertexWithDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AStarSingleGrouped extends AStarSingle {

	public static final Map<String, Object> PARAMETERS = AStarSingle.PARAMETERS;

	public AStarSingleGrouped(SimulationGraph graph) {
		super(graph);
	}

	@Override
	public Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		filterStepOccupiedVertices(step);
		return planAgents(agents.stream().collect(Collectors.toMap(Function.identity(), a -> new Pair<>(a.getEntry(), getExitIDs(a)))), step);
	}

	@Override
	public Collection<Agent> planAgents(final Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		// filter agents that cannot enter intersection
		filterAgentsWithNoEntry(agentsEntriesExits, step);

		if (agentsEntriesExits.isEmpty()) {
			return Collections.emptySet();
		}

		final Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups = new HashSet<>(agentsEntriesExits.size());

		createInitialPaths(agentsEntriesExits, step, agentsGroups);

		if (agentsEntriesExits.isEmpty()) {
			return Collections.emptySet();
		}

		Set<Pair<long[], long[]>> agentsCollisionsPairs = agentsGroups.stream().map(triplet -> new Pair<>(triplet.getVal0().keySet().stream().mapToLong(Agent::getId).toArray(), triplet.getVal2().stream().mapToLong(Agent::getId).toArray())).collect(Collectors.toSet());

		solveAgentsCollisions(step, agentsGroups);

		Set<Agent> plannedAgents = new HashSet<>();
		for (Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group : agentsGroups) {
			Set<Agent> groupAgents = group.getVal0().keySet();

//			assert !inCollision(groupAgents, plannedAgents);

			plannedAgents.addAll(groupAgents);
			groupAgents.forEach(this::addPlannedAgent);
		}

		return plannedAgents;
	}

	private void solveAgentsCollisions(long step, Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups) {
		Map<Collection<Agent>, Collection<Collection<Agent>>> resolvedPairs = new HashMap<>();

		Optional<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> collisionTripletOptional;
		while ((collisionTripletOptional = inCollision(agentsGroups)).isPresent()) {
			Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet = collisionTripletOptional.get();
			Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet = null;

			Map<Agent, Pair<Integer, Set<Integer>>> group0 = group0CollisionTriplet.getVal0();
			Set<Agent> group0Agents = new HashSet<>(group0.keySet());
			resolvedPairs.computeIfAbsent(group0Agents, k -> new HashSet<>());

			final Set<Agent> conflictAgents = group0CollisionTriplet.getVal2();

			Map<Long, Map<Integer, Agent>> group0IllegalMovesTable = new HashMap<>();
			Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable = new HashMap<>();

			for (Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group : agentsGroups) {
				Set<Agent> groupAgents = group.getVal0().keySet();
				if (groupAgents.equals(group0Agents)) {
					continue;
				}

				Map<Long, Map<Integer, Agent>> groupConflictAvoidanceTable = group.getVal1();
				if (group1CollisionTriplet == null && !Collections.disjoint(groupAgents, conflictAgents)) {
					group1CollisionTriplet = group;
				} else if (resolvedPairs.get(group0Agents).contains(groupAgents)) {
					mergeIllegalMovesTables(group0IllegalMovesTable, groupConflictAvoidanceTable);
				} else {
					mergeConflictAvoidanceTables(restGroupsConflictAvoidanceTable, groupConflictAvoidanceTable);
				}
			}

			assert group1CollisionTriplet != null;

			final Map<Agent, Pair<Integer, Set<Integer>>> group1;
			final Set<Agent> group1Agents;
			if (group0.size() <= group1CollisionTriplet.getVal0().size()) {
				group1 = group1CollisionTriplet.getVal0();
				group1Agents = new HashSet<>(group1.keySet());
			} else {
				group1 = group0;
				group0 = group1CollisionTriplet.getVal0();
				Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> temp = group0CollisionTriplet;
				group0CollisionTriplet = group1CollisionTriplet;
				group1CollisionTriplet = temp;

				group1Agents = group0Agents;
				group0Agents = new HashSet<>(group0.keySet());
				resolvedPairs.computeIfAbsent(group0Agents, k -> new HashSet<>());
			}

			Map<Long, Map<Integer, Agent>> group0IllegalMovesTableComplete = copyMap(stepOccupiedVertices);
			mergeIllegalMovesTables(group0IllegalMovesTableComplete, group0IllegalMovesTable);

			if (replanGroup(step, agentsGroups, group0CollisionTriplet, group1CollisionTriplet, group0, group0IllegalMovesTableComplete, restGroupsConflictAvoidanceTable)) {
				resolvedPairs.get(group0Agents).add(group1Agents);
//				System.out.println("Replanned 0: [" + group0Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")) + "]; in collision with 1: [" + group1Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")) + "]");
				continue;
			}

			mergeConflictAvoidanceTables(restGroupsConflictAvoidanceTable, group0IllegalMovesTable);

			final Map<Long, Map<Integer, Agent>> group1IllegalMovesTable = copyMap(stepOccupiedVertices);
			resolvedPairs.computeIfAbsent(group1Agents, k -> new HashSet<>());
			if (resolvedPairs.containsKey(group1Agents) && !resolvedPairs.get(group1Agents).isEmpty()) {
				for (Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group : agentsGroups) {
					if (group != group0CollisionTriplet && resolvedPairs.get(group1Agents).contains(group.getVal0().keySet())) {
						mergeIllegalMovesTables(group1IllegalMovesTable, group.getVal1());
					}
				}
			}

			if (replanGroup(step, agentsGroups, group1CollisionTriplet, group0CollisionTriplet, group1, group1IllegalMovesTable, restGroupsConflictAvoidanceTable)) {
				resolvedPairs.computeIfAbsent(group1Agents, k -> new HashSet<>());
				resolvedPairs.get(group1Agents).add(group0Agents);
//				System.out.println("Replanned 1: [" + group1Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")) + "]; in collision with 0: [" + group0Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")) + "]");
				continue;
			}

			HashMap<Agent, Pair<Integer, Set<Integer>>> combinedAgents = new HashMap<>(group0);
			combinedAgents.putAll(group1);
			@Nullable Set<Agent> collisionAgents = findPaths(combinedAgents, step, stepOccupiedVertices, restGroupsConflictAvoidanceTable);
			agentsGroups.remove(group0CollisionTriplet);
			filterReplannedCollisionAgents(agentsGroups, group0Agents);
			filterReplannedCollisionAgents(agentsGroups, group1Agents);
			if (collisionAgents != null) {
				agentsGroups.remove(group1CollisionTriplet);

				agentsGroups.add(new Triplet<>(combinedAgents, getConflictAvoidanceTable(combinedAgents, step), collisionAgents));
//				System.out.println("Merged 0: [" + group0Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")) + "] with 1: [" + group1Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")) + "] and replanned");
			} else {
				Set<Agent> group1Collisions = findPaths(group1, step, group1IllegalMovesTable, restGroupsConflictAvoidanceTable);
				assert group1Collisions != null;
				updateAgentsGroup(step, group1CollisionTriplet, group1Collisions);
//				System.out.println("Replanned 1: [" + group1Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")) + "]; removed 0: [" + group0Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")) + "]");
			}
		}

		// TODO remove
		int maxGroupSize = agentsGroups.stream().mapToInt(g -> g.getVal0().size()).max().orElse(0);
		if (maxGroupSize > 1) {
			System.out.println(maxGroupSize);
		}
	}

	private boolean replanGroup(long step, Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups,
															Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet,
															Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet,
															Map<Agent, Pair<Integer, Set<Integer>>> group0, Map<Long, Map<Integer, Agent>> invalidMovesMap,
															Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable) {
		Map<Long, Map<Integer, Agent>> group1IllegalMovesTable = copyMap(invalidMovesMap);
		mergeIllegalMovesTables(group1IllegalMovesTable, group1CollisionTriplet.getVal1());

		@Nullable Set<Agent> group0Collisions = findPaths(group0, step, group1IllegalMovesTable, restGroupsConflictAvoidanceTable);
		if (group0Collisions != null) {
			updateAgentsGroup(step, group0CollisionTriplet, group0Collisions);
			filterReplannedCollisionAgents(agentsGroups, group0.keySet());
			return true;
		}
		return false;
	}

	private void createInitialPaths(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step, Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups) {
		final Map<Long, Map<Integer, Set<Agent>>> conflictAvoidanceTable = new HashMap<>();

		for (Iterator<Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator(); it.hasNext(); ) {
			final Entry<Agent, Pair<Integer, Set<Integer>>> agentEntryExitsEntry = it.next();
			final Map<Agent, Pair<Integer, Set<Integer>>> agentEntryExitsMap = new HashMap<>(1);
			agentEntryExitsMap.put(agentEntryExitsEntry.getKey(), agentEntryExitsEntry.getValue());

			final @Nullable Set<Agent> collisionAgents = findPaths(agentEntryExitsMap, step, stepOccupiedVertices, conflictAvoidanceTable);
			if (collisionAgents == null) {
				it.remove();
			} else {
				final Map<Long, Map<Integer, Agent>> agentConflictAvoidanceTable = getConflictAvoidanceTable(agentEntryExitsMap, step);
				agentsGroups.add(new Triplet<>(agentEntryExitsMap, agentConflictAvoidanceTable, collisionAgents));
				mergeConflictAvoidanceTables(conflictAvoidanceTable, agentConflictAvoidanceTable);
			}
		}
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

	protected Map<Long, Map<Integer, Agent>> copyMap(Map<Long, Map<Integer, Agent>> sourceMap) {
		Map<Long, Map<Integer, Agent>> copy = new HashMap<>(sourceMap.size());
		for (Entry<Long, Map<Integer, Agent>> entry : sourceMap.entrySet()) {
			copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
		}

		return copy;
	}

	protected void mergeIllegalMovesTables(Map<Long, Map<Integer, Agent>> destinationMap, final Map<Long, Map<Integer, Agent>> sourceMap) {
		for (Entry<Long, Map<Integer, Agent>> stepOccupiedVerticesEntry : sourceMap.entrySet()) {
			long step = stepOccupiedVerticesEntry.getKey();
			destinationMap.computeIfAbsent(step, k -> new HashMap<>());
			destinationMap.get(step).putAll(stepOccupiedVerticesEntry.getValue());
		}
	}

	protected void mergeConflictAvoidanceTables(Map<Long, Map<Integer, Set<Agent>>> destinationMap, final Map<Long, Map<Integer, Agent>> sourceMap) {
		for (Entry<Long, Map<Integer, Agent>> stepOccupiedVerticesEntry : sourceMap.entrySet()) {
			final long step = stepOccupiedVerticesEntry.getKey();
			destinationMap.computeIfAbsent(step, k -> new HashMap<>());
			Map<Integer, Set<Agent>> destinationStepMap = destinationMap.get(step);

			for (Entry<Integer, Agent> sourceEntry : stepOccupiedVerticesEntry.getValue().entrySet()) {
				final int vertex = sourceEntry.getKey();
				destinationStepMap.computeIfAbsent(vertex, k -> new HashSet<>());
				destinationStepMap.get(vertex).add(sourceEntry.getValue());
			}
		}
	}

	protected void updateAgentsGroup(long step, Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> collisionTriplet, @Nullable Set<Agent> collisionAgents) {
		collisionTriplet.setVal1(getConflictAvoidanceTable(collisionTriplet.getVal0(), step));
		collisionTriplet.setVal2(collisionAgents);
	}

	protected Optional<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> inCollision(Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups) {
		return agentsGroups.stream().filter(triplet -> !triplet.getVal2().isEmpty()).findAny();
	}

	protected Map<Long, Map<Integer, Agent>> getConflictAvoidanceTable(Map<Agent, Pair<Integer, Set<Integer>>> agents, long step) {
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

	protected @Nullable Set<Agent> findPaths(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step, final Map<Long, Map<Integer, Agent>> illegalMovesTable, final Map<Long, Map<Integer, Set<Agent>>> conflictAvoidanceTable) {
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

		while (!queue.isEmpty()) {
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
			if (validState(step, agentsCount, exitsIDs, maximumDelays, state, nextStep)) {
				continue;
			}

			final Set<Integer>[] neighbours = new Set[agentsCount];
			final Map<Integer, Integer>[] neighboursVisited = new Map[agentsCount];
			generateIndividualNeighbours(step, agentsCount, exitsIDs, heuristics, maximumDelays, state, nextStep, neighbours, neighboursVisited);

			// Remove neighbours visited too many times
			filterVisitedNeighbours(agentsCount, state, neighbours, neighboursVisited);


			// remove colliding neighbours
			final Map<Integer, Set<Agent>>[] neighboursCollisions = filterCollidingNeighbours(illegalMovesTable, conflictAvoidanceTable, agentsCount, agents, state, stateStep, nextStep, neighbours);

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

	private static boolean existsNoneValidNeighbour(int agentsCount, Set<Integer>[] exitsIDs, CompositeState state, Set<Integer>[] neighbours) {
		for (int i = 0; i < agentsCount; i++) {
			if (neighbours[i].isEmpty() && !exitsIDs[i].contains(state.getStates()[i].getID())) {
				return true;
			}
		}
		return false;
	}

	private Map<Integer, Set<Agent>>[] filterCollidingNeighbours(Map<Long, Map<Integer, Agent>> illegalMoveTable, Map<Long, Map<Integer, Set<Agent>>> conflictAvoidanceTable, int agentsCount, Agent[] agents, CompositeState state, long stateStep, long nextStep, Set<Integer>[] neighbours) {
		final Map<Integer, Set<Agent>>[] neighboursCollisions = new Map[neighbours.length];

		for (int i = 0; i < agentsCount; i++) {
			final int stateID = state.getStates()[i].getID();
			final Map<Integer, Set<Agent>> agentNeighboursCollisions = new HashMap<>();
			neighboursCollisions[i] = agentNeighboursCollisions;
			for (final Iterator<Integer> iterator = neighbours[i].iterator(); iterator.hasNext(); ) {
				final int neighbourID = iterator.next();
				final double agentPerimeter = agents[i].getAgentPerimeter();
				if (safeVertex(illegalMoveTable, nextStep, neighbourID, agentPerimeter) &&
					safeVertexTo(illegalMoveTable, nextStep, neighbourID, stateID, agentPerimeter) &&
					safeStepFrom(illegalMoveTable, stateStep, stateID, neighbourID, agentPerimeter)
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
			if (allowAgentStop) {
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

	private static boolean validState(long step, int agentsCount, Set<Integer>[] exitsIDs, int[] maximumDelays, CompositeState state, long nextStep) {
		for (int i = 0; i < agentsCount; i++) {
			if (nextStep - step > maximumDelays[i] && !exitsIDs[i].contains(state.getStates()[i].getID())) {
				return true;
			}
		}
		return false;
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

	protected static class CompositeState implements Comparable<CompositeState> {
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
						final int collisionsComparison = Integer.compare(collisions, collectiveState.collisions);
						if (collisionsComparison == 0) {
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
						return collisionsComparison;
					}
					return turnsComparison;
				}
				return turnPenaltyComparison;
			}
			return heuristicsComparison;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CompositeState state = (CompositeState) o;

			if (step != state.step) return false;
			// Probably incorrect - comparing Object[] arrays with Arrays.equals
			return Arrays.equals(states, state.states);
		}

		@Override
		public int hashCode() {
			int result = Arrays.hashCode(states);
			result = 31 * result + (int) (step ^ (step >>> 32));
			return result;
		}
	}
}
