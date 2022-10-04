package cz.cuni.mff.kotal.backend.algorithm.cbs;

import cz.cuni.mff.kotal.backend.algorithm.AlgorithmAll;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.helpers.Quaternion;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

public class CBSAll extends CBSSingleGrouped {
	public static final Map<String, Object> PARAMETERS = new LinkedHashMap<>(CBSSingleGrouped.PARAMETERS);


	static {
		PARAMETERS.putAll(AlgorithmAll.PARAMETERS);
	}

	protected final Map<Agent, Pair<Agent, Long>> notFinishedAgents = new HashMap<>();
	protected final int maximumPlannedAgents;
	protected final int replanSteps;

	public CBSAll(SimulationGraph graph) {
		super(graph);
		maximumPlannedAgents = AlgorithmMenuTab2.getIntegerParameter(AlgorithmAll.MAXIMUM_PLANNED_AGENTS_NAME, AlgorithmAll.MAXIMUM_PLANNED_AGENTS_DEF);
		replanSteps = AlgorithmMenuTab2.getIntegerParameter(AlgorithmAll.REPLAN_STEPS_NAME, AlgorithmAll.REPLAN_STEPS_DEF);
	}

	@TestOnly
	protected CBSAll(SimulationGraph graph, double safeDistance, int maximumVertexVisits, boolean allowAgentStop, int maximumPathDelay, boolean allowAgentReturn, int maximumPlannedAgents, int replanSteps) {
		super(graph, safeDistance, maximumVertexVisits, allowAgentStop, maximumPathDelay, allowAgentReturn);
		this.maximumPlannedAgents = maximumPlannedAgents;
		this.replanSteps = replanSteps;
	}

	@Override
	public @NotNull Collection<Agent> planAgents(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		final Collection<Agent> validNotFinishedAgents = AlgorithmAll.filterNotFinishedAgents(notFinishedAgents, stepOccupiedVertices, step, maximumPlannedAgents - agentsEntriesExits.size(), replanSteps);

		assert stepOccupiedVertices.values().stream().flatMap(s -> s.values().stream()).distinct().noneMatch(validNotFinishedAgents::contains);

		findInitialPaths(agentsEntriesExits, step);

		final Map<Agent, Pair<Integer, Set<Integer>>> allAgents = new LinkedHashMap<>(agentsEntriesExits.size() + validNotFinishedAgents.size());
		allAgents.putAll(agentsEntriesExits);

		AlgorithmAll.addAgentsEntriesExits(this, step, validNotFinishedAgents, allAgents);

		final Node node = bestValidNode(allAgents, step);

		final Collection<Agent> plannedAgents = node.getAgents();
		assert stopped || plannedAgents.containsAll(validNotFinishedAgents);
		AlgorithmAll.processPlannedAgents(notFinishedAgents, plannedAgents, step);

		return plannedAgents;
	}

	@Override
	protected void assignNewPath(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step, @NotNull PriorityQueue<Node> queue, @NotNull Node node, @NotNull Agent agent, Quaternion<Agent, Agent, Long, Boolean> collision, @NotNull Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints, @Nullable LinkedList<Integer> path) {
		Collection<Agent> agents;

		if (path != null) {
			agents = copyAgents(step, node, agent, path);
		} else if (notFinishedAgents.containsKey(agent)) {
			return;
		} else {
			removeAgentFromConstraints(node, constraints, agent);
			agents = replanAgents(agentsEntriesExits, step, constraints, node.getAgents(), agent);
		}

		if (stopped) {
			return;
		}

		queue.add(new Node(agents, constraints, node, collision));
	}

	/**
	 * @param agent
	 * @param exitsIDs
	 * @param step
	 *
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
	public void addPlannedAgent(@NotNull Agent agent) {
		setLargestAgentPerimeter(agent);
	}

	@Override
	public void addPlannedPath(Agent agent, List<Integer> path, long step) {
		// stepOccupiedVertices should be empty
	}

	@Override
	protected void filterStepOccupiedVertices(long step) {
		AlgorithmAll.filterStepOccupiedVertices(step, stepOccupiedVertices);
	}
}
