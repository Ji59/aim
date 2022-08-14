package cz.cuni.mff.kotal.backend.algorithm.sat;

import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.util.*;

public class SATAll extends SATSingleGrouped {

	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(SATSingleGrouped.PARAMETERS);

	protected final Map<Agent, Pair<Long, List<Integer>>> notFinishedAgents = new HashMap<>();

	public SATAll(SimulationGraph graph) {
		super(graph);
	}

	@Override
	public Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		if (agents.isEmpty()) {
			return agents;
		}
		filterStepOccupiedVertices(step);

		Map<Agent, Pair<Integer, Set<Integer>>> allAgents = new LinkedHashMap<>(agents.size() + notFinishedAgents.size());

		Iterator<Agent> it = notFinishedAgents.keySet().iterator();
		while (it.hasNext()) {
			Agent agent = it.next();

			long plannedTime = agent.getPlannedTime();
			int travelTime = (int) (step - plannedTime);
			if (travelTime >= agent.getPath().size() - 1) {
				it.remove();
			} else {
				Integer startingVertexID = agent.getPath().get(travelTime);
				allAgents.put(agent, new Pair<>(startingVertexID, getExits(agent)));
			}
		}
		agents.forEach(agent -> allAgents.put(agent, new Pair<>(agent.getEntry(), getExits(agent))));

		Collection<Agent> plannedAgents = super.planAgents(allAgents, step);
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

		return plannedAgents;
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
}
