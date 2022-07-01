package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;

import java.util.List;


public record Lines(SimulationGraph graph) implements Algorithm {

	/**
	 * TODO
	 *
	 * @param agent Agent to be planned
	 * @param step  Number of step in which agents should be planned
	 * @return
	 */
	@Override
	public Agent planAgent(Agent agent, long step) {
		final int exit;
		if (agent.getExit() < 0) {
			List<Vertex> directionExits = graph.getEntryExitVertices().get(agent.getExitDirection()).stream().filter(vertex -> vertex.getType().isExit()).toList();
			List<Vertex> directionEntries = graph.getEntryExitVertices().get(agent.getEntryDirection()).stream().filter(vertex -> vertex.getType().isEntry()).toList();
			int entryIndex = directionEntries.indexOf(graph.getVertex(agent.getEntry()));
			entryIndex = directionEntries.size() - entryIndex - 1; // invert index
			exit = directionExits.get(entryIndex * directionExits.size() / directionEntries.size()).getID();
		} else {
			exit = agent.getExit();
		}

		List<Integer> path = graph.getLines().get(agent.getEntry()).get(exit);
		if (path == null) {
			return null;
		}
		agent.setPath(path, step);
		return agent;
	}

	@Override
	public Agent planAgent(Agent agent, int vertexID, long step) {
		// FIXME
		throw new UnsupportedOperationException();
	}

	// TODO
	private boolean agentGoingStraight(Agent agent) {
		return Math.abs(agent.getEntryDirection() - agent.getExitDirection()) % (graph.getModel().getDirections().size() / 2) == 0;
	}
}
