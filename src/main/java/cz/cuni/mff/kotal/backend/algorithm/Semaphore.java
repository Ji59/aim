package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Semaphore extends SafeLines {
	private final int directions;
	private final long greenTime;
	private final long directionTime; // TODO

	public Semaphore(SimulationGraph graph) {
		super(graph);

		Integer longestPath = graph
			.getLines().values().stream()
			.flatMap(
				map -> map.values().stream()
					.map(List::size)
			).max(Long::compare)
			.orElse(0);

		directions = graph.getModel().getDirections().size() / 2;
		directionTime = (graph.getGranularity() * graph.getEntryExitVertices().size()) / (directions - 1); // TODO zero division TODO extract method
		greenTime = Math.max(1, directionTime - longestPath);
	}

	@Override
	public Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		if (step % directionTime >= greenTime) {
			return new ArrayList<>();
		}

		stepOccupiedVertices.entrySet().removeIf(occupiedVerticesEntry -> occupiedVerticesEntry.getKey() < step);

		List<Agent> straightAgents = new ArrayList<>();
		List<Agent> turningAgents = new ArrayList<>();

		for (Agent agent : agents) {
			if (isFromCurrentDirection(agent, step)) {
				if (agentGoingStraight(agent)) {
					straightAgents.add(agent);
				} else {
					turningAgents.add(agent);
				}
			}
		}

		List<Agent> plannedAgents = straightAgents.stream().filter(agent -> planAgent(agent, step) != null).collect(Collectors.toList());
		plannedAgents.addAll(turningAgents.stream().filter(agent -> planAgent(agent, step) != null).toList());

		return plannedAgents;
	}

	private boolean isFromCurrentDirection(Agent agent, long step) {
		return graph.getVertex(agent.getEntry()).getType().getDirection() % directions == (step / directionTime) % directions;
	}

	private boolean agentGoingStraight(Agent agent) {
		return Math.abs(agent.getEntryDirection() - agent.getExitDirection()) % directions == 0;
	}
}