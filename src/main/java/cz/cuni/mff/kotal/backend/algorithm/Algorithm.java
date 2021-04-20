package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.simulation.Agent;

import java.util.Set;


public interface Algorithm {
	void planAgents(Set<Agent> agents);
}
