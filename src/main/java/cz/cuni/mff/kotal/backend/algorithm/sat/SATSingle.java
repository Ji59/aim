package cz.cuni.mff.kotal.backend.algorithm.sat;

import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.util.*;
import java.util.stream.Collectors;

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
				int entry = e.getValue().getVal0();
				Set<Integer> exitsID = e.getValue().getVal1();
				agent = super.planAgent(agent, entry, exitsID, step);
				return agent;
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toSet()
			);
	}
}
