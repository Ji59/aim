package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.simulation.Agent;
import javafx.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;


public interface Algorithm {

	Map<String, Integer> PARAMETERS = Collections.emptyMap();  // parameters in form of parameter name, default value

	/**
	 * Try to plan all provided agents using an algorithm.
	 * By default, plan agents in sequential order by calling planAgent().
	 *
	 * @param agents Set of agents to be planned
	 * @param step   Number of step in which agents should be planned
	 * @return Set of agents which had been successfully planned
	 */
	default Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		return agents.stream().filter(agent -> planAgent(agent, step) != null).toList();
	}

	/**
	 * Try to plan agent with an algorithm.
	 *
	 * @param agent Agent to be planned
	 * @param step  Number of step in which agents should be planned
	 * @return Agent if planning was successful, otherwise null
	 */
	Agent planAgent(Agent agent, long step);

	/**
	 * TODO
	 *
	 * @return
	 */
	static Map<String, Object> getParameters() {
		return Collections.emptyMap();
	}
}
