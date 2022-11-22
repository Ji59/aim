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
	protected final Map<Integer, Pair<Agent, Map<Integer, Integer>>> agentVerticesVisits = new HashMap<>();

	public CBSAll(SimulationGraph graph) {
		super(graph);
		maximumPlannedAgents = AlgorithmMenuTab2.getIntegerParameter(AlgorithmAll.MAXIMUM_PLANNED_AGENTS_NAME, AlgorithmAll.MAXIMUM_PLANNED_AGENTS_DEF);
		replanSteps = AlgorithmMenuTab2.getIntegerParameter(AlgorithmAll.REPLAN_STEPS_NAME, AlgorithmAll.REPLAN_STEPS_DEF);
	}

	@TestOnly
	protected CBSAll(SimulationGraph graph, double safeDistance, int maximumVertexVisits, boolean allowAgentStop, int maximumPathDelay, boolean allowAgentReturn, long simpleStrategyAfter, int maximumPlannedAgents, int replanSteps) {
		super(graph, safeDistance, maximumVertexVisits, allowAgentStop, maximumPathDelay, allowAgentReturn, simpleStrategyAfter);
		this.maximumPlannedAgents = maximumPlannedAgents;
		this.replanSteps = replanSteps;
	}

	/**
	 * @param step
	 * @param queue
	 * @param node
	 * @param collision
	 * @param agent
	 */
	@Override
	protected void applySimpleAlgorithm(long step, @NotNull PriorityQueue<Node> queue, @NotNull Node node, @NotNull Quaternion<Agent, Agent, Long, Boolean> collision, Agent agent) {
		if (!notFinishedAgents.containsKey(agent)) {
			super.applySimpleAlgorithm(step, queue, node, collision, agent);
		}
	}

	@Override
	public @NotNull Collection<Agent> planAgents(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		final @NotNull Collection<Agent> validNotFinishedAgents = AlgorithmAll.filterNotFinishedAgents(notFinishedAgents, stepOccupiedVertices, step, maximumPlannedAgents - agentsEntriesExits.size(), replanSteps);

		assert stepOccupiedVertices.values().stream().flatMap(s -> s.values().stream()).distinct().noneMatch(validNotFinishedAgents::contains);

		agentVerticesVisits.clear();
		validNotFinishedAgents.forEach(agent -> {
			final List<Integer> path = agent.getPath();
			final int travelTime = (int) (step - agent.getPlannedTime());
			final Map<Integer, Integer> vertexVisitsMap = AlgorithmAll.createVertexVisitsMap(path, travelTime);
			agentVerticesVisits.put(path.get(travelTime), new Pair<>(agent, vertexVisitsMap));
		});

		initialPaths.clear();
		validNotFinishedAgents.forEach(a -> initialPaths.put(a, a.getPath()));
		findInitialPaths(agentsEntriesExits, step);

		final @NotNull Map<Agent, Pair<Integer, Set<Integer>>> allAgents = new LinkedHashMap<>(agentsEntriesExits.size() + validNotFinishedAgents.size());
		allAgents.putAll(agentsEntriesExits);

		AlgorithmAll.addAgentsEntriesExits(this, step, validNotFinishedAgents, allAgents);

		final @NotNull Node node = bestValidNode(allAgents, step);

		final Collection<Agent> plannedAgents = node.getAgents();
		assert stopped || plannedAgents.containsAll(validNotFinishedAgents);
		AlgorithmAll.processPlannedAgents(notFinishedAgents, plannedAgents, step);

		return plannedAgents;
	}

	/**
	 * @param state
	 * @param entryID
	 * @param vertexID
	 * @param constraints
	 * @return
	 */
	@Override
	protected boolean canVisitVertex(@NotNull State state, int entryID, int vertexID, @Nullable Collection<Pair<Integer, Integer>> constraints) {
		int maximumVertexVisits = this.maximumVertexVisits;
		if (agentVerticesVisits.containsKey(entryID)) {
			final Pair<Agent, Map<Integer, Integer>> agentVertexVisits = agentVerticesVisits.get(entryID);
			if (!allowAgentReturn) {
				final int lastVertex;
				if (state.getParent() == null) {
					final Agent agent = agentVertexVisits.getVal0();
					final int lastFixTime = (int) (state.getStep() - agent.getPlannedTime() - 1);
					lastVertex = agent.getPath().get(lastFixTime);
				} else {
					lastVertex = state.getParent().getID();
				}
				if (lastVertex == vertexID) {
					return false;
				}
			}
			final Map<Integer, Integer> vertexVisitsMap = agentVertexVisits.getVal1();
			if (vertexVisitsMap.containsKey(vertexID)) {
				maximumVertexVisits -= vertexVisitsMap.get(vertexID);
			}
		} else if (state.getParent() != null && state.getParent().getID() == vertexID) {
			return false;
		}

		return maximumVertexVisits > 0 && safeConstraints(state, vertexID, constraints) && validVisitCount(state, vertexID, maximumVertexVisits);
	}

	/**
	 * @param agent
	 * @param step
	 * @return
	 */
	@Override
	protected long getInitialPlannedStep(Agent agent, long step) {
		if (notFinishedAgents.containsKey(agent)) {
			return notFinishedAgents.get(agent).getVal1();
		}
		return step;
	}

	@Override
	protected void assignNewPath(long step, @NotNull PriorityQueue<Node> queue, @NotNull Node node, @NotNull Agent agent, Quaternion<Agent, Agent, Long, Boolean> collision, @NotNull Map<Agent, Map<Long, Collection<Pair<Integer, Integer>>>> constraints, @Nullable LinkedList<Integer> path) {
		Collection<Agent> agents;

		if (path != null) {
			agents = copyAgents(step, node, agent, path);
		} else if (notFinishedAgents.containsKey(agent)) {
			return;
		} else {
			removeAgentFromConstraints(node, constraints, agent);
			agents = replanAgents(step, node.getAgents(), agent);
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
	}

	@Override
	public void addPlannedPath(@NotNull Agent agent, @NotNull List<Integer> path, long step) {
		// stepOccupiedVertices should be empty
	}

	@Override
	protected void filterStepOccupiedVertices(long step) {
		AlgorithmAll.filterStepOccupiedVertices(step, stepOccupiedVertices);
	}
}
