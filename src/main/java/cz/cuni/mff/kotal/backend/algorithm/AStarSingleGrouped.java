package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.util.*;

public class AStarSingleGrouped extends SafeLines {

	public static final Map<String, Object> PARAMETERS = SafeLines.PARAMETERS;

	public AStarSingleGrouped(SimulationGraph graph) {
		super(graph);
	}

	@Override
	public Collection<Agent> planAgents(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		Iterator<Map.Entry<Agent, Pair<Integer, Set<Integer>>>> it = agentsEntriesExits.entrySet().iterator();

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

		return null;
	}
}
