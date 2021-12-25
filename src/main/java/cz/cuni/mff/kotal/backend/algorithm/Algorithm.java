package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.util.Collection;
import java.util.List;


public interface Algorithm {

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
}
