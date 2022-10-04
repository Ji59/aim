package cz.cuni.mff.kotal.backend.algorithm.sat;

import cz.cuni.mff.kotal.backend.algorithm.AlgorithmAll;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;
import org.sat4j.core.VecInt;
import org.sat4j.maxsat.WeightedPartialMaxsat;
import org.sat4j.specs.ContradictionException;

import java.util.*;

public class SATAll extends SATSingleGrouped {

	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(SATSingleGrouped.PARAMETERS);

	static {
		PARAMETERS.putAll(AlgorithmAll.PARAMETERS);
	}

	protected final Map<Agent, Pair<Agent, Long>> notFinishedAgents = new HashMap<>();
	protected final int maximumPlannedAgents;
	protected final int replanSteps;

	public SATAll(@NotNull SimulationGraph graph) {
		super(graph);
		maximumPlannedAgents = AlgorithmMenuTab2.getIntegerParameter(AlgorithmAll.MAXIMUM_PLANNED_AGENTS_NAME, AlgorithmAll.MAXIMUM_PLANNED_AGENTS_DEF);
		replanSteps = AlgorithmMenuTab2.getIntegerParameter(AlgorithmAll.REPLAN_STEPS_NAME, AlgorithmAll.REPLAN_STEPS_DEF);
	}

	@Override
	public @NotNull Collection<Agent> planAgents(@NotNull Collection<Agent> agents, long step) {
		if (agents.isEmpty()) {
			return agents;
		}
		filterStepOccupiedVertices(step);

		final @NotNull Collection<Agent> validNotFinishedAgents = AlgorithmAll.filterNotFinishedAgents(notFinishedAgents, stepOccupiedVertices, step, maximumPlannedAgents - agents.size(), replanSteps);
		assert stepOccupiedVertices.values().stream().flatMap(s -> s.values().stream()).distinct().noneMatch(validNotFinishedAgents::contains);

		@NotNull Map<Agent, Pair<Integer, Set<Integer>>> allAgents = new LinkedHashMap<>(agents.size() + notFinishedAgents.size());

		AlgorithmAll.addAgentsEntriesExits(this, step, validNotFinishedAgents, allAgents);

		agents.forEach(agent -> allAgents.put(agent, new Pair<>(agent.getEntry(), getExits(agent))));

		Collection<Agent> plannedAgents = super.planAgents(allAgents, step);

		assert plannedAgents.containsAll(validNotFinishedAgents);
		AlgorithmAll.processPlannedAgents(notFinishedAgents, plannedAgents, step);

		return plannedAgents;
	}

	@Override
	protected void filterStepOccupiedVertices(long step) {
		AlgorithmAll.filterStepOccupiedVertices(step, stepOccupiedVertices);
	}

	@Override
	protected boolean addNoValidRouteClause(final @NotNull WeightedPartialMaxsat solver, final int startingVertexOffset, final Agent agent) throws ContradictionException {
		if (!notFinishedAgents.containsKey(agent)) {
			return super.addNoValidRouteClause(solver, startingVertexOffset, agent);
		} else {
			solver.addHardClause(VecInt.of(startingVertexOffset));
			return false;
		}
	}
}
