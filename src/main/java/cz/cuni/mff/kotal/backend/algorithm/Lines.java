package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Lines implements Algorithm {
	private final SimulationGraph graph;

	public Lines(SimulationGraph graph) {
		this.graph = graph;
	}

	@Override
	public Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		return agents.stream().map(agent -> planAgent(agent, step)).filter(Objects::nonNull).collect(Collectors.toList());
	}

	@Override
	public Agent planAgent(Agent agent, long step) {
		List<Long> path = graph.getLines().get(agent.getStart()).get(agent.getEnd());
		if (path == null) {
			return null;
		}
		agent.setPath(path);
		return agent;
	}
}
