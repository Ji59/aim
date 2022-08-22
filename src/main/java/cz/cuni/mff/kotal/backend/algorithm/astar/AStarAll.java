package cz.cuni.mff.kotal.backend.algorithm.astar;

import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.util.*;

public class AStarAll extends AStarSingleGrouped {
	public static final Map<String, Object> PARAMETERS = AStarSingleGrouped.PARAMETERS;

	protected final Map<Agent, Pair<Long, List<Integer>>> notFinishedAgents = new HashMap<>();

	public AStarAll(SimulationGraph graph) {
		super(graph);
	}

	@Override
	public Collection<Agent> planAgents(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		stepOccupiedVertices.putIfAbsent(step, new HashMap<>());

		Iterator<Map.Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator();

		// filter agents that cannot enter intersection
		while (it.hasNext()) {
			Map.Entry<Agent, Pair<Integer, Set<Integer>>> entry = it.next();
			Agent agent = entry.getKey();
			int agentEntry = entry.getValue().getVal0();
			if (!safeVertex(step, agentEntry, agent.getAgentPerimeter())) {
				it.remove();
			}
		}

		if (agentsEntriesExits.isEmpty()) {
			return Collections.emptySet();
		}

		Map<Agent, Pair<Integer, Set<Integer>>> nonFinishedAgentsMap = new LinkedHashMap<>(agentsEntriesExits.size() + notFinishedAgents.size());

		Iterator<Agent> itNonFinished = notFinishedAgents.keySet().iterator();
		while (itNonFinished.hasNext()) {
			Agent agent = itNonFinished.next();

			long plannedTime = agent.getPlannedTime();
			int travelTime = (int) (step - plannedTime);
			if (travelTime >= agent.getPath().size() - 1) {
				itNonFinished.remove();
			} else {
				Integer startingVertexID = agent.getPath().get(travelTime);
				nonFinishedAgentsMap.put(agent, new Pair<>(startingVertexID, getExits(agent)));
			}
		}

		Collection<Agent> agents = agentsEntriesExits.keySet();

		for (int i = agentsEntriesExits.size(); i > 0; i--) {
			for (Collection<Agent> agentsCombination : MyNumberOperations.combinations(agents, i)) {
				final Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExitsSubset = new HashMap<>(nonFinishedAgentsMap);
				for (Agent agent : agentsCombination) {
					agentsEntriesExitsSubset.put(agent, agentsEntriesExits.get(agent));
				}
//				Collection<Agent> plannedAgents = findPaths(agentsEntriesExitsSubset, step);
//				if (!plannedAgents.isEmpty()) {
//					processPlannedAgents(step, plannedAgents);
//					return plannedAgents;
//				}
			}

		}
		return Collections.emptySet();
	}

	private void processPlannedAgents(long step, Collection<Agent> plannedAgents) {
		if (!plannedAgents.containsAll(notFinishedAgents.keySet())) throw new AssertionError();

		Iterator<Agent> iterator = plannedAgents.iterator();
		while (iterator.hasNext()) {
			Agent agent = iterator.next();
			if (notFinishedAgents.containsKey(agent)) {
				Pair<Long, List<Integer>> plannedPathPair = notFinishedAgents.get(agent);
				long plannedTime = plannedPathPair.getVal0();
				List<Integer> lastPath = plannedPathPair.getVal1();
				List<Integer> pathEnd = agent.getPath();
				for (int i = 0; i < step - plannedTime; i++) {
					pathEnd.add(i, lastPath.get(i));
				}
				notFinishedAgents.get(agent).setVal1(pathEnd);
				agent.setPlannedTime(plannedTime);

				iterator.remove();
			} else {
				notFinishedAgents.put(agent, new Pair<>(agent.getPlannedTime(), agent.getPath()));
			}

			if (!notFinishedAgents.get(agent).getVal1().equals(agent.getPath())) {
				throw new AssertionError();
			}
		}
	}

	@Override
	protected void filterStepOccupiedVertices(long step) {
		Iterator<Map.Entry<Long, Map<Integer, Agent>>> it = stepOccupiedVertices.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, Map<Integer, Agent>> stepVertices = it.next();
			long occupiedStep = stepVertices.getKey();
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
	}
}
