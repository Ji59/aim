package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public interface Algorithm {

	Map<String, Object> PARAMETERS = Collections.emptyMap();  // floating point parameters in form of parameter name, default value

	/**
	 * Try to plan all provided agents using an algorithm.
	 * By default, plan agents in sequential order by calling planAgent().
	 *
	 * @param agents Set of agents to be planned
	 * @param step   Number of step in which agents should be planned
	 * @return Set of agents which had been successfully planned
	 */
	default Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		return planAgents(agents.stream().collect(Collectors.toMap(Function.identity(), a -> new Pair<>(a.getEntry(), a.getExit() >= 0 ? Collections.singleton(a.getExit()) : Collections.emptySet()))), step);
	}

	/**
	 * TODO
	 * Try to plan all provided agents using an algorithm.
	 * By default, plan agents in sequential order by calling planAgent().
	 *
	 * @param agentsEntriesExits Set of agents to be planned TODO
	 * @param step               Number of step in which agents should be planned
	 * @return Set of agents which had been successfully planned
	 */
	default Collection<Agent> planAgents(Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
		return agentsEntriesExits.entrySet().stream()
			.map(agentEntry -> planAgent(agentEntry.getKey(), agentEntry.getValue().getVal0(), agentEntry.getValue().getVal1(), step))
			.filter(Objects::nonNull)
			.toList();
	}

	/**
	 * Try to plan agent with an algorithm.
	 *
	 * @param agent Agent to be planned
	 * @param step  Number of step in which agents should be planned
	 * @return Agent if planning was successful, otherwise null
	 */
	default Agent planAgent(Agent agent, long step) {
		return planAgent(agent, agent.getEntry(), Collections.singleton(agent.getExit()), step);
	}

	/**
	 * Try to plan agent with an algorithm from specified vertex.
	 *
	 * @param agent   Agent to be planned
	 * @param entryID Starting vertex for algorithm
	 * @param exitsID TODO
	 * @param step    Number of step in which agents should be planned
	 * @return Agent if planning was successful, otherwise null
	 */
	Agent planAgent(Agent agent, int entryID, Set<Integer> exitsID, long step);

	default void addPlannedAgent(Agent agent) {
	}

	default void addPlannedPath(Agent agent, List<Integer> path, long step) {
	}

	/**
	 *
	 */
	void stop();
}
