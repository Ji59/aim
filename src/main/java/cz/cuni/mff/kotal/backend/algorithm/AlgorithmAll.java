package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.backend.algorithm.simple.SafeLanes;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Interface used in strategies replanning travelling agents.
 * Contains only static methods for operation with these agents.
 */
public interface AlgorithmAll {
	/**
	 * Parameters names and default values
	 */
	String MAXIMUM_PLANNED_AGENTS_NAME = "Maximum planned agents";
	int MAXIMUM_PLANNED_AGENTS_DEF = Integer.MAX_VALUE;
	String REPLAN_STEPS_NAME = "Replan last steps";
	int REPLAN_STEPS_DEF = Simulation.GENERATED_MINIMUM_STEP_AHEAD;

	Map<String, Object> PARAMETERS = Map.of(
		MAXIMUM_PLANNED_AGENTS_NAME, MAXIMUM_PLANNED_AGENTS_DEF,
		REPLAN_STEPS_NAME, REPLAN_STEPS_DEF
	);

	/**
	 * Choose agents to be replanned from travelling agents in {@code notFinishedAgents}.
	 * First of all remove all agents travelling longer than {@code replanSteps} steps.
	 * Then remove the longest travelling agents until the collection of agents has size at most {@code maximumPlannedAgents}.
	 * <p>Removed agents are added to {@code stepOccupiedVertices} so planning agents can avid them.</p>
	 *
	 * @param notFinishedAgents    Collection of travelling agents
	 * @param stepOccupiedVertices Map from steps to map of vertexIDs and agents occupying the vertex at the step
	 * @param step                 Actually planning step
	 * @param maximumPlannedAgents Maximum number of agents that are allowed to be replanned
	 * @param replanSteps          Maximum number of steps agent can travel to be valid for replanning
	 * @return Collection of agents chosen to be replanned
	 */
	static @NotNull Collection<Agent> filterNotFinishedAgents(final @NotNull Map<Agent, Pair<Agent, Long>> notFinishedAgents, final @NotNull Map<Long, Map<Integer, Agent>> stepOccupiedVertices, final long step, int maximumPlannedAgents, final int replanSteps) {
		maximumPlannedAgents = Math.max(maximumPlannedAgents, 0);

		@NotNull List<Agent> validNotFinishedAgents = new LinkedList<>();

		for (@NotNull Iterator<Map.Entry<Agent, Pair<Agent, Long>>> iterator = notFinishedAgents.entrySet().iterator(); iterator.hasNext(); ) {
			final Map.Entry<Agent, Pair<Agent, Long>> entry = iterator.next();
			final Agent agent = entry.getKey();
			if (step >= agent.getPlannedStep() + agent.getPath().size() - 1) {
				iterator.remove();
			} else if (step - agent.getPlannedStep() <= replanSteps) {
				validNotFinishedAgents.add(agent);
			} else {
				addAgentToStepOccupiedVertices(stepOccupiedVertices, agent, step);
			}
		}

		if (validNotFinishedAgents.size() > maximumPlannedAgents) {
			removeEarliestAgents(stepOccupiedVertices, step, maximumPlannedAgents, validNotFinishedAgents);
		}

		assert validNotFinishedAgents.size() <= maximumPlannedAgents;
		return validNotFinishedAgents.stream().map(a -> new Agent(a).setPath(a.getPath(), a.getPlannedStep())).collect(Collectors.toSet());
	}

	/**
	 * Remove agents that are planned the earliest, until the size of {@code validNotFinishedAgents} is not smaller than {@code maximumPlannedAgents}.
	 * Removed agents are added to {@code stepOccupiedVertices}.
	 *
	 * @param stepOccupiedVertices   Map from steps to map of vertexIDs and agents occupying the vertex at the step
	 * @param step                   Actually planning step
	 * @param maximumPlannedAgents   Maximum number of agents that are allowed to be replanned
	 * @param validNotFinishedAgents Collection of candidates for replanning
	 */
	private static void removeEarliestAgents(@NotNull Map<Long, Map<Integer, Agent>> stepOccupiedVertices, long step, int maximumPlannedAgents, @NotNull List<Agent> validNotFinishedAgents) {
		validNotFinishedAgents.sort((a0, a1) -> Long.compare(a1.getPlannedStep(), a0.getPlannedStep()));

		for (@NotNull Iterator<Agent> it = validNotFinishedAgents.listIterator(maximumPlannedAgents); it.hasNext(); ) {
			final Agent agent = it.next();
			it.remove();
			addAgentToStepOccupiedVertices(stepOccupiedVertices, agent, step);
		}
	}

	/**
	 * Add specified agent to {@code stepOccupiedVertices} by iterating over its path and adding entry with visited vertex for every future step.
	 *
	 * @param stepOccupiedVertices Map from steps to map of vertexIDs and agents occupying the vertex at the step
	 * @param agent                Agent to be added
	 * @param step                 First step to add entry in
	 */
	private static void addAgentToStepOccupiedVertices(final @NotNull Map<Long, Map<Integer, Agent>> stepOccupiedVertices, final @NotNull Agent agent, final long step) {
		long pathStep = step;
		for (final @NotNull Iterator<Integer> it = agent.getPath().listIterator((int) (step - agent.getPlannedStep())); it.hasNext(); pathStep++) {
			final int vertex = it.next();
			final @NotNull Map<Integer, Agent> verticesMap = stepOccupiedVertices.computeIfAbsent(pathStep, k -> new HashMap<>());
			verticesMap.put(vertex, agent);
		}
	}

	/**
	 * Add newly planned agents to {@code notFinishedAgents}, otherwise update travelling agents paths.
	 *
	 * @param notFinishedAgents Collection of travelling agents
	 * @param plannedAgents     Collection of successfully planned agents
	 * @param step              Step in which were agents planned
	 */
	static void processPlannedAgents(@NotNull Map<Agent, Pair<Agent, Long>> notFinishedAgents, @NotNull Collection<Agent> plannedAgents, long step) {
		final @NotNull Iterator<Agent> iterator = plannedAgents.iterator();
		while (iterator.hasNext()) {
			final Agent agent = iterator.next();
			if (notFinishedAgents.containsKey(agent)) {
				iterator.remove();

				final List<Integer> pathEnd = agent.getPath();

				final Pair<Agent, Long> plannedPathPair = notFinishedAgents.get(agent);
				final Agent originalAgent = plannedPathPair.getVal0();
				final long plannedTime = plannedPathPair.getVal1();

				if (agent.getPlannedStep() == plannedTime) {
					continue;
				}

				final List<Integer> lastPath = originalAgent.getPath();
				for (int i = 0; i < step - plannedTime; i++) {
					pathEnd.add(i, lastPath.get(i));
				}
				originalAgent.setPath(pathEnd);
			} else {
				notFinishedAgents.put(agent, new Pair<>(agent, agent.getPlannedStep()));
			}
		}
	}

	/**
	 * Remove all  old entries from {@code stepOccupiedVertices}, clear the rest of it.
	 *
	 * @param step                 remove all entries until {@code step}
	 * @param stepOccupiedVertices Map containing entries for steps, where at vertex with vertexID as key there is agent
	 */
	static void filterStepOccupiedVertices(long step, @NotNull Map<Long, Map<Integer, Agent>> stepOccupiedVertices) {
		final @NotNull Iterator<Map.Entry<Long, Map<Integer, Agent>>> it = stepOccupiedVertices.entrySet().iterator();
		while (it.hasNext()) {
			final Map.Entry<Long, Map<Integer, Agent>> stepVertices = it.next();
			final long occupiedStep = stepVertices.getKey();
			if (occupiedStep < step) {
				it.remove();
			} else {
				stepVertices.getValue().clear();
			}
		}
		stepOccupiedVertices.computeIfAbsent(step, k -> new HashMap<>());
	}

	/**
	 * Add replanning agents to collection of agents waiting to be planned.
	 *
	 * @param algorithm              Planning algorithm
	 * @param step                   Planning step
	 * @param validNotFinishedAgents Planned agents to be replanned
	 * @param allAgents              Set of agents meant to be planned
	 */
	static void addAgentsEntriesExits(final @NotNull SafeLanes algorithm, final long step, final @NotNull Collection<Agent> validNotFinishedAgents, final @NotNull Map<Agent, Pair<Integer, Set<Integer>>> allAgents) {
		for (@NotNull Agent agent : validNotFinishedAgents) {
			final long plannedTime = agent.getPlannedStep();
			final int travelTime = (int) (step - plannedTime);
			assert travelTime < agent.getPath().size() - 1;
			final int startingVertexID = agent.getPath().get(travelTime);
			allAgents.put(agent, new Pair<>(startingVertexID, algorithm.getExits(agent)));
		}
	}

	/**
	 * Create map with number of vertex visits along a path until specified step.
	 *
	 * @param path       List of vertexIDs
	 * @param travelTime Step until when check for visited vertices, exclusively
	 * @return Map from vertexIDs to number of visits in path
	 */
	static Map<Integer, Integer> createVertexVisitsMap(List<Integer> path, int travelTime) {
		final Map<Integer, Integer> agentVertexVisits = new HashMap<>();
		for (int i = 0; i < travelTime; i++) {
			final int vertex = path.get(i);
			int visits = agentVertexVisits.getOrDefault(vertex, 0);
			agentVertexVisits.put(vertex, visits + 1);
		}

		return agentVertexVisits;
	}
}
