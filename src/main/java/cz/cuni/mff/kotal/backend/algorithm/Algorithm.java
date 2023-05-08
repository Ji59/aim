package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.helpers.Pair;
import cz.cuni.mff.kotal.simulation.Agent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Interface with all basic methods.
 * Some methods are predefined for easier usage.
 *
 * <p>Every algorithm should have public static map {@link #PARAMETERS},
 * which contains for each parameter name and default value.</p>
 *
 * <p>{@link #planAgent(Agent, int, Set, long)} is only planning method that needs implementation.
 * It should try to find path for agent with desired properties.
 * {@link #planAgents} methods call by default {@link #planAgent(Agent, int, Set, long)} for each agent individually.
 * Those algorithms should return only successfully planned agents.
 * </p>
 */
public interface Algorithm {
	/**
	 * Parameters names and default values
	 */
	Map<String, Object> PARAMETERS = Collections.emptyMap();  // floating point parameters in form of parameter name, default value

	/**
	 * Try to plan all provided agents using an algorithm.
	 * By default, plan agents in sequential order by calling planAgent().
	 *
	 * @param agents Set of agents to be planned
	 * @param step   Number of step in which agents should be planned
	 * @return Set of agents which had been successfully planned
	 */
	default Collection<Agent> planAgents(@NotNull Collection<Agent> agents, long step) {
		return planAgents(agents.stream().collect(Collectors.toMap(Function.identity(), a -> new Pair<>(a.getEntry(), a.getExit() >= 0 ? Collections.singleton(a.getExit()) : Collections.emptySet()))), step);
	}

	/**
	 * Try to plan all provided agents using an algorithm.
	 * By default, plan agents in sequential order by calling {@link #planAgent} method.
	 *
	 * @param agentsEntriesExits Map of agents to be planned with their entries and exits
	 * @param step               Number of step in which agents should be planned
	 * @return Set of agents which had been successfully planned
	 */
	default Collection<Agent> planAgents(@NotNull Map<Agent, Pair<Integer, Set<Integer>>> agentsEntriesExits, long step) {
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
	@Deprecated
	default @Nullable Agent planAgent(@NotNull Agent agent, long step) {
		return planAgent(agent, agent.getEntry(), Collections.singleton(agent.getExit()), step);
	}

	/**
	 * Try to plan agent with an algorithm from specified vertex to any exit.
	 *
	 * @param agent   Agent to be planned
	 * @param entryID ID of starting vertex for algorithm
	 * @param exitsID Set of IDs of target vertices for algorithm
	 * @param step    Number of step in which agents should be planned
	 * @return Agent if planning was successful, otherwise null
	 */
	@Nullable Agent planAgent(@NotNull Agent agent, int entryID, @NotNull Set<Integer> exitsID, long step);

	/**
	 * Inform algorithm about new agent it should avoid while planning.
	 * Agent should have a valid path.
	 * <p>By default this method calls {@link #addPlannedPath(Agent, List, long)} with path and planned step taken from agent fields.</p>
	 *
	 * @param agent Agent to be avoided by future agents
	 */
	default void addPlannedAgent(Agent agent) {
		addPlannedPath(agent, agent.getPath(), agent.getPlannedStep());
	}

	/**
	 * Inform algorithm about new agent it should avoid while planning.
	 * Similar to {@link #addPlannedAgent(Agent)}, but path details should be used from parameters instead of agent fields.
	 * <p>By default this method is empty</p>
	 *
	 * @param agent Agent to be avoided by future agents
	 * @param path  Path of the agent
	 * @param step  Step in which the agent was planned
	 */
	default void addPlannedPath(Agent agent, List<Integer> path, long step) {
	}

	/**
	 * Tell the algorithm it should stop as soon as possible.
	 * This call does not guarantee immediate termination or invalid path finding result for all agents.
	 */
	void stop();
}
