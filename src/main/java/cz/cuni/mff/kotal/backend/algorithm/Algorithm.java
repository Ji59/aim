package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.simulation.Agent;

import java.util.Set;


public interface Algorithm {

	/**
	 * Try to plan all provided agents using an algorithm.
	 *
	 * @param agents Set of agents to be planned
	 * @return Set of agents which had been successfully planned
	 */
	Set<Agent> planAgents(Set<Agent> agents);

	/**
	 * Try to plan agent with an algorithm.
	 * @param agent Agent to be planned
	 * @return Agent if planning was successful, otherwise null
	 */
	Agent planAgent(Agent agent);
}
