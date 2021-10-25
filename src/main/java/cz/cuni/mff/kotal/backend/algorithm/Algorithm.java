package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.simulation.Agent;

import java.util.Collection;
import java.util.List;


public interface Algorithm {

	/**
	 * Try to plan all provided agents using an algorithm.
	 *
	 * @param agents Set of agents to be planned
	 * @param step   Number of step in which agents should be planned
	 * @return Set of agents which had been successfully planned
	 */
	Collection<Agent> planAgents(Collection<Agent> agents, long step);

	/**
	 * Try to plan agent with an algorithm.
	 *
	 * @param agent Agent to be planned
	 * @param step  Number of step in which agents should be planned
	 * @return Agent if planning was successful, otherwise null
	 */
	Agent planAgent(Agent agent, long step);
}
