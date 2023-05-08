package cz.cuni.mff.kotal.backend.algorithm.sat;

import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SATSingle extends SATSingleGrouped {
	/**
	 * Parameters names and default values
	 */
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(SATSingleGrouped.PARAMETERS);

	public SATSingle(@NotNull SimulationGraph graph) {
		super(graph);
	}

	@Override
	public @NotNull Collection<Agent> planAgents(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		return agentsEntriesExits.entrySet().stream()
			.map(e -> {
				@NotNull Agent agent = e.getKey();
				final int entry = e.getValue().getVal0();
				final Set<Integer> exitsID = e.getValue().getVal1();
				@Nullable Agent plannedAgent = super.planAgent(agent, entry, exitsID, step);
				assert plannedAgent == null || (plannedAgent.getPlannedStep() == step && !plannedAgent.getPath().isEmpty());
				return plannedAgent;
			})
			.filter(Objects::nonNull)
			.toList();
	}
}
