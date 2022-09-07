package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Triplet;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class AStarAll extends AStarSingleGrouped {
	public static final Map<String, Object> PARAMETERS = AStarSingleGrouped.PARAMETERS;
	protected final Map<Agent, Pair<Agent, Long>> notFinishedAgents = new HashMap<>();


	public AStarAll(SimulationGraph graph) {
		super(graph);
	}

	@Override
	public Collection<Agent> planAgents(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		stepOccupiedVertices.putIfAbsent(step, new HashMap<>());

		final Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups = new HashSet<>(notFinishedAgents.size());
		final Map<Long, Map<Integer, Set<Agent>>> plannedConflictAvoidanceTable = new HashMap<>();

		final Iterator<Agent> itNonFinished = notFinishedAgents.keySet().iterator();
		while (itNonFinished.hasNext()) {
			final Agent agent = itNonFinished.next();

			final long plannedTime = agent.getPlannedTime();
			final int travelTime = (int) (step - plannedTime);
			if (travelTime >= agent.getPath().size() - 1) {
				itNonFinished.remove();
			} else {
				final int startingVertexID = agent.getPath().get(travelTime);
				final Map<Agent, Pair<Integer, Set<Integer>>> agentMap = new HashMap<>(1);
				final Agent agentsCopy = new Agent(agent).setPath(agent.getPath(), plannedTime);
				agentMap.put(agentsCopy, new Pair<>(startingVertexID, getExits(agent)));
				final Map<Long, Map<Integer, Agent>> conflictAvoidanceTable = getConflictAvoidanceTable(agentMap, step);
				mergeConflictAvoidanceTables(plannedConflictAvoidanceTable, conflictAvoidanceTable);
				agentsGroups.add(new Triplet<>(agentMap, conflictAvoidanceTable, Collections.emptySet()));
			}
		}

		Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> initialPaths = createInitialPaths(agentsEntriesExits, step, plannedConflictAvoidanceTable);
		if (initialPaths.isEmpty()) {
			return Collections.emptySet();
		}

		agentsGroups.addAll(initialPaths);

		solveAgentsCollisions(step, agentsGroups);

		List<Agent> plannedAgents = agentsGroups.stream().flatMap(group -> group.getVal0().keySet().stream()).collect(Collectors.toList());
		processPlannedAgents(step, plannedAgents);
		return plannedAgents;
	}

	@Override
	protected void solveAgentsCollisions(long step, Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups) {
		Map<Collection<Agent>, Collection<Collection<Agent>>> resolvedPairs = new HashMap<>();
		Collection<Agent> plannedAgents = notFinishedAgents.keySet();

		Optional<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> collisionTripletOptional;
		while ((collisionTripletOptional = inCollision(agentsGroups)).isPresent()) {
			Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet = collisionTripletOptional.get();

			Map<Agent, Pair<Integer, Set<Integer>>> group0 = group0CollisionTriplet.getVal0();
			Set<Agent> group0Agents = new HashSet<>(group0.keySet());
			resolvedPairs.computeIfAbsent(group0Agents, k -> new HashSet<>());

			final Set<Agent> conflictAgents = group0CollisionTriplet.getVal2();

			Map<Long, Map<Integer, Set<Agent>>> group0IllegalMovesTable = new HashMap<>();
			Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable = new HashMap<>();

			Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet = findCollisionGroup(agentsGroups, resolvedPairs, group0Agents, conflictAgents, group0IllegalMovesTable, restGroupsConflictAvoidanceTable);

			Set<Agent> group0PlannedAgents = group0Agents.stream().filter(plannedAgents::contains).collect(Collectors.toSet());
			Set<Agent> group1PlannedAgents = group1CollisionTriplet.getVal0().keySet().stream().filter(plannedAgents::contains).collect(Collectors.toSet());

			Map<Agent, Pair<Integer, Set<Integer>>> group1;
			Set<Agent> group1Agents;
			Map<Long, Map<Integer, Set<Agent>>> group1IllegalMovesTable = null;
			if (group0Agents.size() < group1CollisionTriplet.getVal0().size() || (group0Agents.size() == group1CollisionTriplet.getVal0().size() && group0PlannedAgents.size() < group1PlannedAgents.size())) {
				group1 = group1CollisionTriplet.getVal0();
				group1Agents = new HashSet<>(group1.keySet());
			} else {
				group1 = group0;
				group0 = group1CollisionTriplet.getVal0();
				final Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> temp = group0CollisionTriplet;
				group0CollisionTriplet = group1CollisionTriplet;
				group1CollisionTriplet = temp;

				Set<Agent> temp1 = group0PlannedAgents;
				group0PlannedAgents = group1PlannedAgents;
				group1PlannedAgents = temp1;

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


			boolean groupsMerged = mergeGroups(step, agentsGroups, group0CollisionTriplet, group1CollisionTriplet, restGroupsConflictAvoidanceTable, group0PlannedAgents, group1PlannedAgents);
			if (!notFinishedAgents.keySet().stream().allMatch(a -> agentsGroups.stream().anyMatch(g -> g.getVal0().containsKey(a)))) {
				throw new AssertionError();
			}
			if (groupsMerged) {
				final Set<Agent> finalGroup0Agents = group0Agents;
				System.out.println("Merged 0: " + group0Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", "))
								+ " with 1: " + group1Agents.stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", "))
								+ " creating " + agentsGroups.stream().filter(g -> !Collections.disjoint(g.getVal0().keySet(), finalGroup0Agents) || !Collections.disjoint(g.getVal0().keySet(), group1Agents)).findAny().orElse(new Triplet<>(Collections.emptyMap(), null, null)).getVal0().keySet().stream().map(a -> String.valueOf(a.getId())).collect(Collectors.joining(", ")));
				continue;
			}
			assert false;
		}
	}

	private boolean mergeGroups(
					long step, Set<Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>>> agentsGroups,
					Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group0CollisionTriplet,
					Triplet<Map<Agent, Pair<Integer, Set<Integer>>>, Map<Long, Map<Integer, Agent>>, Set<Agent>> group1CollisionTriplet,
					Map<Long, Map<Integer, Set<Agent>>> restGroupsConflictAvoidanceTable, Set<Agent> group0PlannedAgents, Set<Agent> group1PlannedAgents) {

		final Map<Agent, Pair<Integer, Set<Integer>>> group0 = group0CollisionTriplet.getVal0();
		final Map<Agent, Pair<Integer, Set<Integer>>> group1 = group1CollisionTriplet.getVal0();
		final Map<Agent, Pair<Integer, Set<Integer>>> combinedPlannedAgents = new HashMap<>(group0PlannedAgents.size() + group1PlannedAgents.size());
		final Map<Agent, Pair<Integer, Set<Integer>>> combinedNewAgents = new HashMap<>(group0.size() - group0PlannedAgents.size() + group1.size() - group1PlannedAgents.size());
		divideGroupToPlannedAndNew(group0, group0PlannedAgents, combinedPlannedAgents, combinedNewAgents);
		divideGroupToPlannedAndNew(group1, group1PlannedAgents, combinedPlannedAgents, combinedNewAgents);

		agentsGroups.remove(group0CollisionTriplet);
		agentsGroups.remove(group1CollisionTriplet);
		filterReplannedCollisionAgents(agentsGroups, group0.keySet());
		filterReplannedCollisionAgents(agentsGroups, group1.keySet());

		for (int i = combinedNewAgents.size(); i >= 0; i--) {
			for (Collection<Map.Entry<Agent, Pair<Integer, Set<Integer>>>> combination : MyNumberOperations.combinations(combinedNewAgents.entrySet(), i)) {

				Map<Agent, Pair<Integer, Set<Integer>>> combinedAgents = new HashMap<>(combinedPlannedAgents);
				combination.forEach(agentEntryExits -> combinedAgents.put(agentEntryExits.getKey(), agentEntryExits.getValue()));

				if (combinedAgents.size() == 0) {
					return true;
				}

				@Nullable Set<Agent> collisionAgents = findPaths(combinedAgents, step, Collections.emptyMap(), restGroupsConflictAvoidanceTable);
				if (collisionAgents != null) {
					agentsGroups.add(new Triplet<>(combinedAgents, getConflictAvoidanceTable(combinedAgents, step), collisionAgents));
					if (!notFinishedAgents.keySet().stream().allMatch(a -> agentsGroups.stream().anyMatch(g -> g.getVal0().containsKey(a)))) {
						throw new AssertionError();
					}
					return true;
				}
			}
		}
		return false;
	}

	private static void divideGroupToPlannedAndNew(Map<Agent, Pair<Integer, Set<Integer>>> group0, Set<Agent> group0PlannedAgents, Map<Agent, Pair<Integer, Set<Integer>>> combinedPlannedAgents, Map<Agent, Pair<Integer, Set<Integer>>> combinedNewAgents) {
		group0.forEach((agent, entryExits) -> {
			if (group0PlannedAgents.contains(agent)) {
				combinedPlannedAgents.put(agent, entryExits);
			} else {
				combinedNewAgents.put(agent, entryExits);
			}
		});
	}

	@Override
	protected Map<Long, Map<Integer, Set<Agent>>> mapStepOccupiedVertices() {
		return Collections.emptyMap();
	}

	protected void processPlannedAgents(long step, Collection<Agent> plannedAgents) {
		if (!plannedAgents.containsAll(notFinishedAgents.keySet())) {
			throw new AssertionError();
		}

		Iterator<Agent> iterator = plannedAgents.iterator();
		while (iterator.hasNext()) {
			final Agent agent = iterator.next();
			if (notFinishedAgents.containsKey(agent)) {
				iterator.remove();

				List<Integer> pathEnd = agent.getPath();

				final Pair<Agent, Long> plannedPathTriplet = notFinishedAgents.get(agent);
				final Agent originalAgent = plannedPathTriplet.getVal0();
				final long plannedTime = plannedPathTriplet.getVal1();

				if (agent.getPlannedTime() == plannedTime) {
					continue;
				}

				final List<Integer> lastPath = originalAgent.getPath();
				for (int i = 0; i < step - plannedTime; i++) {
					pathEnd.add(i, lastPath.get(i));
				}
				originalAgent.setPath(pathEnd);
			} else {
				notFinishedAgents.put(agent, new Pair<>(agent, agent.getPlannedTime()));
			}
		}
	}

	@Override
	protected void filterStepOccupiedVertices(long step) {
		final Iterator<Map.Entry<Long, Map<Integer, Agent>>> it = stepOccupiedVertices.entrySet().iterator();
		while (it.hasNext()) {
			final Map.Entry<Long, Map<Integer, Agent>> stepVertices = it.next();
			final long occupiedStep = stepVertices.getKey();
			if (occupiedStep < step) {
				it.remove();
			} else {
				stepVertices.getValue().clear();
			}
		}
	}

	@Override
	public void addPlannedAgent(Agent agent) {
		setLargestAgentPerimeter(agent);
	}

	@Override
	public void addPlannedPath(Agent agent, List<Integer> path, long step) {
		// stepOccupiedVertices should be empty
	}
}
