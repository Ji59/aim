package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.backend.algorithm.simple.SafeLines;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public interface AlgorithmAll {
	String MAXIMUM_PLANNED_AGENTS_NAME = "Maximum planned agents";
	int MAXIMUM_PLANNED_AGENTS_DEF = Integer.MAX_VALUE;
	String REPLAN_STEPS_NAME = "Replan last steps";
	int REPLAN_STEPS_DEF = Simulation.GENERATED_MINIMUM_STEP_AHEAD;

	Map<String, Object> PARAMETERS = Map.of(
		MAXIMUM_PLANNED_AGENTS_NAME, MAXIMUM_PLANNED_AGENTS_DEF,
		REPLAN_STEPS_NAME, REPLAN_STEPS_DEF
	);

	static @NotNull Collection<Agent> filterNotFinishedAgents(final @NotNull Map<Agent, Pair<Agent, Long>> notFinishedAgents, final @NotNull Map<Long, Map<Integer, Agent>> stepOccupiedVertices, final long step, int maximumPlannedAgents, final int replanSteps) {
		maximumPlannedAgents = Math.max(maximumPlannedAgents, 0);

		@NotNull List<Agent> validNotFinishedAgents = new LinkedList<>();

		for (@NotNull Iterator<Map.Entry<Agent, Pair<Agent, Long>>> iterator = notFinishedAgents.entrySet().iterator(); iterator.hasNext(); ) {
			final Map.Entry<Agent, Pair<Agent, Long>> entry = iterator.next();
			final Agent agent = entry.getKey();
			if (step >= agent.getPlannedTime() + agent.getPath().size() - 1) {
				iterator.remove();
			} else if (step - agent.getPlannedTime() <= replanSteps) {
				validNotFinishedAgents.add(agent);
			} else {
				addAgentToStepOccupiedVertices(stepOccupiedVertices, agent, step);
			}
		}

		if (validNotFinishedAgents.size() > maximumPlannedAgents) {
			removeEarliestAgents(stepOccupiedVertices, step, maximumPlannedAgents, validNotFinishedAgents);
		}

		assert validNotFinishedAgents.size() <= maximumPlannedAgents;
		return validNotFinishedAgents.stream().map(a -> new Agent(a).setPath(a.getPath(), a.getPlannedTime())).collect(Collectors.toSet());
	}

	private static void removeEarliestAgents(@NotNull Map<Long, Map<Integer, Agent>> stepOccupiedVertices, long step, int maximumPlannedAgents, @NotNull List<Agent> validNotFinishedAgents) {
		validNotFinishedAgents.sort((a0, a1) -> Long.compare(a1.getPlannedTime(), a0.getPlannedTime()));

		for (@NotNull Iterator<Agent> it = validNotFinishedAgents.listIterator(maximumPlannedAgents); it.hasNext(); ) {
			final Agent agent = it.next();
			it.remove();
			addAgentToStepOccupiedVertices(stepOccupiedVertices, agent, step);
		}
	}

	private static void addAgentToStepOccupiedVertices(final @NotNull Map<Long, Map<Integer, Agent>> stepOccupiedVertices, final @NotNull Agent agent, final long step) {
		long pathStep = step;
		for (final @NotNull Iterator<Integer> it = agent.getPath().listIterator((int) (step - agent.getPlannedTime())); it.hasNext(); pathStep++) {
			final int vertex = it.next();
			final @NotNull Map<Integer, Agent> verticesMap = stepOccupiedVertices.computeIfAbsent(pathStep, k -> new HashMap<>());
			verticesMap.put(vertex, agent);
		}
	}

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

				if (agent.getPlannedTime() == plannedTime) {
					continue;
				}

				final List<Integer> lastPath = originalAgent.getPath();
				for (int i = 0; i < step - plannedTime; i++) {
					pathEnd.add(i, lastPath.get(i));
				}
				originalAgent.setPath(pathEnd);
			} else {
				notFinishedAgents.put(agent, new Pair<>(agent, agent.getPlannedTime()));
			}
		}
	}

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

	static void addAgentsEntriesExits(final @NotNull SafeLines algorithm, final long step, final @NotNull Collection<Agent> validNotFinishedAgents, final @NotNull Map<Agent, Pair<Integer, Set<Integer>>> allAgents) {
		for (@NotNull Agent agent : validNotFinishedAgents) {
			final long plannedTime = agent.getPlannedTime();
			final int travelTime = (int) (step - plannedTime);
			assert travelTime < agent.getPath().size() - 1;
			final int startingVertexID = agent.getPath().get(travelTime);
			allAgents.put(agent, new Pair<>(startingVertexID, algorithm.getExits(agent)));
		}
	}

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
