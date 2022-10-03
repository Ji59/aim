package cz.cuni.mff.kotal.backend.algorithm.sat;

import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.util.*;

public class SATSingle extends SATSingleGrouped {

	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(SATSingleGrouped.PARAMETERS);

	public SATSingle(SimulationGraph graph) {
		super(graph);
	}

	@Override
	public Collection<Agent> planAgents(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		return agentsEntriesExits.entrySet().stream()
			.map(e -> {
				Agent agent = e.getKey();
				final int entry = e.getValue().getVal0();
				final Set<Integer> exitsID = e.getValue().getVal1();
				agent = super.planAgent(agent, entry, exitsID, step);
				assert agent == null || (agent.getPlannedTime() == step && !agent.getPath().isEmpty());
				return agent;
			})
			.filter(Objects::nonNull)
			.toList();
	}
}
