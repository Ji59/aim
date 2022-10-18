package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.backend.algorithm.AlgorithmAll;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Triplet;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class AStarAll extends AStarSingleGrouped {
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(AStarSingleGrouped.PARAMETERS);

	static {
		PARAMETERS.putAll(AlgorithmAll.PARAMETERS);
	}

	protected final Map<Agent, Pair<Agent, Long>> notFinishedAgents = new HashMap<>();
	protected final int maximumPlannedAgents;
	protected final int replanSteps;
	protected final Map<Agent, Map<Integer, Integer>> agentVerticesVisits = new HashMap<>();

	public AStarAll(SimulationGraph graph) {
		super(graph);
		maximumPlannedAgents = AlgorithmMenuTab2.getIntegerParameter(AlgorithmAll.MAXIMUM_PLANNED_AGENTS_NAME, AlgorithmAll.MAXIMUM_PLANNED_AGENTS_DEF);
		replanSteps = AlgorithmMenuTab2.getIntegerParameter(AlgorithmAll.REPLAN_STEPS_NAME, AlgorithmAll.REPLAN_STEPS_DEF);
	}

	private static void divideGroupToPlannedAndNew(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> group0, @NotNull Set<Agent> group0PlannedAgents, @NotNull Map<Agent, Pair<Integer, Set<Integer>>> combinedPlannedAgents, @NotNull Map<Agent, Pair<Integer, Set<Integer>>> combinedNewAgents) {
		group0.forEach((agent, entryExits) -> {
			if (group0PlannedAgents.contains(agent)) {
				combinedPlannedAgents.put(agent, entryExits);
			} else {
				combinedNewAgents.put(agent, entryExits);
			}
		});
	}

	@Override
	public @NotNull Collection<Agent> planAgents(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		stepOccupiedVertices.putIfAbsent(step, new HashMap<>());
		agentVerticesVisits.clear();

		final @NotNull Collection<Agent> validNotFinishedAgents = AlgorithmAll.filterNotFinishedAgents(notFinishedAgents, stepOccupiedVertices, step, maximumPlannedAgents - agentsEntriesExits.size(), replanSteps);
		final @NotNull Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups = new HashSet<>(validNotFinishedAgents.size());
		final @NotNull Map<Long, Map<Integer, Set<Agent>>> plannedConflictAvoidanceTable = new HashMap<>();

		for (final @NotNull Agent agent : validNotFinishedAgents) {
			if (stopped) {
				return Collections.emptySet();
			}

			final long plannedTime = agent.getPlannedTime();
			final int travelTime = (int) (step - plannedTime);
			final List<Integer> path = agent.getPath();
			assert travelTime < path.size() - 1;

			final int startingVertexID = path.get(travelTime);
			final @NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentMap = new HashMap<>(1);
			final Agent agentsCopy = new Agent(agent).setPath(path, plannedTime);
			agentMap.put(agentsCopy, new Pair<>(startingVertexID, getExits(agent)));
			final @NotNull Map<Long, Map<Integer, Agent>> conflictAvoidanceTable = getConflictAvoidanceTable(agentMap, step);
			mergeConflictAvoidanceTables(plannedConflictAvoidanceTable, conflictAvoidanceTable);
			agentsGroups.add(new Triplet<>(agentMap, conflictAvoidanceTable, Collections.emptySet()));

			Map<Integer, Integer> vertexVisitsMap = AlgorithmAll.createVertexVisitsMap(path, travelTime);
			agentVerticesVisits.put(agent, vertexVisitsMap);
		}

		final Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> initialPaths = createInitialPaths(agentsEntriesExits, step, plannedConflictAvoidanceTable);
		if (initialPaths.isEmpty() || stopped) {
			return Collections.emptySet();
		}

		agentsGroups.addAll(initialPaths);

		solveAgentsCollisions(step, agentsGroups);

		if (stopped) {
			return Collections.emptySet();
		}

		@NotNull Collection<Agent> plannedAgents = agentsGroups.stream().flatMap(group -> group.getVal0().keySet().stream()).collect(Collectors.toList());
		assert plannedAgents.containsAll(validNotFinishedAgents);
		AlgorithmAll.processPlannedAgents(notFinishedAgents, plannedAgents, step);

		assert !inCollision(notFinishedAgents.keySet());

		return plannedAgents;
	}

	@Override
	protected void tryMergeGroups(
		long step, @NotNull Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups,
		@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet,
		@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet,
		@NotNull Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable, @NotNull Map<Long, Map<Integer, Set<Agent>>> group1IllegalMovesTable
	) {
		boolean groupsMerged = mergeGroups(step, agentsGroups, group0CollisionTriplet, group1CollisionTriplet, restGroupsConflictAvoidanceTable);
		if (groupsMerged) {
			final @NotNull Set<Agent> group0Agents = group0CollisionTriplet.getVal0().keySet();
			final @NotNull Set<Agent> group1Agents = group1CollisionTriplet.getVal0().keySet();
			System.out.println("Merged 0: " + group0Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", "))
				+ " with 1: " + group1Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", "))
				+ " creating " + agentsGroups.stream().filter(g -> !Collections.disjoint(g.getVal0().keySet(), group0Agents) || !Collections.disjoint(g.getVal0().keySet(), group1Agents)).findAny().orElse(new Triplet<>(Collections.emptyMap(), null, null)).getVal0().keySet().stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")));
		}
		assert groupsMerged || stopped;
	}

	/**
	 * @param agents
	 * @param state
	 * @param neighbours
	 * @param neighboursVisited
	 */
	@Override
	protected void filterVisitedNeighbours(Agent @NotNull [] agents, CompositeState state, Set<Integer>[] neighbours, Map<Integer, Integer>[] neighboursVisited) {
		if (state.getParent() == null && !allowAgentReturn) {
			for (int i = 0, agentsLength = agents.length; i < agentsLength; i++) {
				final Agent agent = agents[i];
				if (notFinishedAgents.containsKey(agent)) {
					final Pair<Agent, Long> nonFinishedAgent = notFinishedAgents.get(agent);
					final int pathLastIndex = (int) (state.getStep() - nonFinishedAgent.getVal1() - 1);
					final int lastVertex = nonFinishedAgent.getVal0().getPath().get(pathLastIndex);
					neighbours[i].remove(lastVertex);
				}
			}
		}
		super.filterVisitedNeighbours(agents, state, neighbours, neighboursVisited);
	}

	/**
	 * @param agent
	 * @param neighbours
	 * @param neighboursVisited
	 * @param i
	 * @param predecessorVertexID
	 */
	@Override
	protected void modifyNeighbours(Agent agent, Set<Integer>[] neighbours, Map<Integer, Integer>[] neighboursVisited, int i, int predecessorVertexID) {
		int maximumAgentVertexVisits = maximumVertexVisits;
		if (agentVerticesVisits.containsKey(agent) && agentVerticesVisits.get(agent).containsKey(predecessorVertexID)) {
			maximumAgentVertexVisits -= agentVerticesVisits.get(agent).getOrDefault(predecessorVertexID, 0);
		}

		final int vertexVisits = neighboursVisited[i].get(predecessorVertexID);
		if (vertexVisits >= maximumAgentVertexVisits) {
			neighbours[i].remove(predecessorVertexID);
		} else {
			neighboursVisited[i].put(predecessorVertexID, vertexVisits + 1);
		}
	}

	@Override
	protected boolean switchGroups(@NotNull Set<Agent> group0Agents, @NotNull Set<Agent> group1Agents) {
		return group0Agents.size() < group1Agents.size() ||
			(
				group0Agents.size() == group1Agents.size() &&
					group0Agents.stream().filter(notFinishedAgents.keySet()::contains).count() < group1Agents.stream().filter(notFinishedAgents.keySet()::contains).count()
			);
	}

	private boolean mergeGroups(
		long step, @NotNull Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups,
		@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet,
		@NotNull Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet,
		Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable) {


		final Map<Agent, Pair<Integer, Set<Integer>>> group0 = group0CollisionTriplet.getVal0();
		final Map<Agent, Pair<Integer, Set<Integer>>> group1 = group1CollisionTriplet.getVal0();

		@NotNull Set<Agent> group0PlannedAgents = group0.keySet().stream().filter(notFinishedAgents.keySet()::contains).collect(Collectors.toSet());
		@NotNull Set<Agent> group1PlannedAgents = group1.keySet().stream().filter(notFinishedAgents.keySet()::contains).collect(Collectors.toSet());

		final @NotNull Map<Agent, Pair<Integer, Set<Integer>>> combinedPlannedAgents = new HashMap<>(group0PlannedAgents.size() + group1PlannedAgents.size());
		final @NotNull Map<Agent, Pair<Integer, Set<Integer>>> combinedNewAgents = new HashMap<>(group0.size() - group0PlannedAgents.size() + group1.size() - group1PlannedAgents.size());
		divideGroupToPlannedAndNew(group0, group0PlannedAgents, combinedPlannedAgents, combinedNewAgents);
		divideGroupToPlannedAndNew(group1, group1PlannedAgents, combinedPlannedAgents, combinedNewAgents);

		agentsGroups.remove(group0CollisionTriplet);
		agentsGroups.remove(group1CollisionTriplet);
		filterReplannedCollisionAgents(agentsGroups, group0.keySet());
		filterReplannedCollisionAgents(agentsGroups, group1.keySet());

		simpleLock.lock();
		final boolean notSimple = !simple;
		simpleLock.unlock();

		for (int i = notSimple ? combinedNewAgents.size() : 0; i > 0; i--) {
			for (@NotNull Collection<Map.Entry<Agent, Pair<Integer, Set<Integer>>>> combination : MyNumberOperations.combinations(combinedNewAgents.entrySet(), i)) {

				if (stopped) {
					return false;
				}

				@NotNull Map<Agent, Pair<Integer, Set<Integer>>> combinedAgents = new HashMap<>(combinedPlannedAgents);
				combination.forEach(agentEntryExits -> combinedAgents.put(agentEntryExits.getKey(), agentEntryExits.getValue()));

				@Nullable Set<Agent> collisionAgents = findPaths(combinedAgents, step, mapStepOccupiedVertices(), restGroupsConflictAvoidanceTable);
				if (collisionAgents != null) {
					agentsGroups.add(new Triplet<>(combinedAgents, getConflictAvoidanceTable(combinedAgents, step), collisionAgents));
					return true;
				}
			}
		}

		if (combinedPlannedAgents.size() != 0) {
			// set paths from previous step
			combinedPlannedAgents.keySet().forEach(a -> {
				final Pair<Agent, Long> lastStepAgent = notFinishedAgents.get(a);
				a.setPath(lastStepAgent.getVal0().getPath(), lastStepAgent.getVal1());
			});

			Set<Agent> collisionAgents = agentsGroups.stream().parallel()
				.map(g -> g.getVal0().keySet())
				.filter(a -> inCollision(a, combinedPlannedAgents.keySet()))
				.flatMap(Set::stream)
				.collect(Collectors.toSet());

			agentsGroups.add(new Triplet<>(combinedPlannedAgents, getConflictAvoidanceTable(combinedPlannedAgents, step), collisionAgents));
		}

		return true;
	}

	/**
	 * @param agent
	 * @param exitsIDs
	 * @param step
	 * @return
	 */
	@Override
	protected long getLastStep(@NotNull Agent agent, @NotNull Set<Integer> exitsIDs, long step) {
		long startingStep;
		if (notFinishedAgents.containsKey(agent)) {
			startingStep = notFinishedAgents.get(agent).getVal1();
		} else {
			startingStep = step;
		}
		return startingStep + getMaximumTravelTime(agent, exitsIDs);
	}

	@Override
	protected void filterStepOccupiedVertices(long step) {
		AlgorithmAll.filterStepOccupiedVertices(step, stepOccupiedVertices);
	}

	@Override
	public void addPlannedAgent(@NotNull Agent agent) {
		setLargestAgentPerimeter(agent);
	}

	@Override
	public void addPlannedPath(Agent agent, List<Integer> path, long step) {
		// stepOccupiedVertices should be empty
	}
}
