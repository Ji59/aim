package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.backend.algorithm.simple.SafeLines;
import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;

import java.util.*;

public interface AlgorithmAll {
	String MAXIMUM_REPLANNED_AGENTS_NAME = "Maximum replanned agents";
	int MAXIMUM_REPLANNED_AGENTS_DEF = Integer.MAX_VALUE;
	String REPLAN_STEPS_NAME = "Replan last steps";
	int REPLAN_STEPS_DEF = Simulation.GENERATED_MINIMUM_STEP_AHEAD;

	Map<String, Object> PARAMETERS = Map.of(
		MAXIMUM_REPLANNED_AGENTS_NAME, MAXIMUM_REPLANNED_AGENTS_DEF,
		REPLAN_STEPS_NAME, REPLAN_STEPS_DEF
	);

	static void processPlannedAgents(Map<Agent, Pair<Agent, Long>> notFinishedAgents, Collection<Agent> plannedAgents, long step) {
		final Iterator<Agent> iterator = plannedAgents.iterator();
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

	static Collection<Agent> filterNotFinishedAgents(final Map<Agent, Pair<Agent, Long>> notFinishedAgents, final Map<Long, Map<Integer, Agent>> stepOccupiedVertices, final long step, final int maximumReplannedAgents, final int replanSteps) {
		List<Agent> validNotFinishedAgents = new LinkedList<>();

		for (Iterator<Map.Entry<Agent, Pair<Agent, Long>>> iterator = notFinishedAgents.entrySet().iterator(); iterator.hasNext(); ) {
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

		if (validNotFinishedAgents.size() > maximumReplannedAgents) {
			removeEarliestAgents(stepOccupiedVertices, step, maximumReplannedAgents, validNotFinishedAgents);
		}

		assert validNotFinishedAgents.size() <= maximumReplannedAgents;
		return validNotFinishedAgents;
	}

	private static void removeEarliestAgents(Map<Long, Map<Integer, Agent>> stepOccupiedVertices, long step, int maximumReplannedAgents, List<Agent> validNotFinishedAgents) {
		validNotFinishedAgents.sort((a0, a1) -> Long.compare(a1.getPlannedTime(), a0.getPlannedTime()));

		for (Iterator<Agent> it = validNotFinishedAgents.listIterator(maximumReplannedAgents); it.hasNext(); ) {
			final Agent agent = it.next();
			it.remove();
			addAgentToStepOccupiedVertices(stepOccupiedVertices, agent, step);
		}
	}

	private static void addAgentToStepOccupiedVertices(final Map<Long, Map<Integer, Agent>> stepOccupiedVertices, final Agent agent, final long step) {
		long pathStep = step;
		for (final Iterator<Integer> it = agent.getPath().listIterator((int) (step - agent.getPlannedTime())); it.hasNext(); pathStep++) {
			final int vertex = it.next();
			final Map<Integer, Agent> verticesMap = stepOccupiedVertices.computeIfAbsent(pathStep, k -> new HashMap<>());
			verticesMap.put(vertex, agent);
		}
	}

	static void filterStepOccupiedVertices(long step, Map<Long, Map<Integer, Agent>> stepOccupiedVertices) {
		final Iterator<Map.Entry<Long, Map<Integer, Agent>>> it = stepOccupiedVertices.entrySet().iterator();
		while (it.hasNext()) {
			final Map.Entry<Long, Map<Integer, Agent>> stepVertices = it.next();
			final long occupiedStep = stepVertices.getKey();
			if (occupiedStep < step) {
				it.remove();
			} else {
				stepVertices.getValue().clear();
			}
		}
	}
}
