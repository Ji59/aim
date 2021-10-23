package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.util.*;
import java.util.stream.Collectors;

public class Semaphore implements Algorithm {
	private final SimulationGraph graph;
	private final Map<Long, Map<Long, Agent>> stepOccupiedVertices = new HashMap<>();
	private final int directions;
	private final long greenTime;
	private final long directionTime; // TODO

	public Semaphore(SimulationGraph graph) {
		this.graph = graph;

		Integer longestPath = graph
			.getLines().values().stream()
			.flatMap(
				map -> map.values().stream()
					.map(List::size)
			).max(Long::compare)
			.orElse(0);

		directions = graph.getModel().getDirections().size() / 2;
		directionTime =( graph.getGranularity() * graph.getEntryExitVertices().size()) / (directions - 1); // TODO zero division TODO extract method
		greenTime = Math.max(1, directionTime - longestPath);
	}

	@Override
	public List<Agent> planAgents(List<Agent> agents, long step) {
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
		plannedAgents.addAll(turningAgents.stream().filter(agent -> planAgent(agent, step) != null).collect(Collectors.toList()));

		return plannedAgents;
	}

	private boolean isFromCurrentDirection(Agent agent, long step) {
		return graph.getVertex(agent.getStart()).getType().getDirection() % directions == (step / directionTime) % directions;
	}

	private boolean agentGoingStraight(Agent agent) {
		return Math.abs(
			(
				graph.getVertex(agent.getStart()).getType().getDirection() -
					graph.getVertex(agent.getEnd()).getType().getDirection()
			)
		) == graph.getEntryExitVertices().size();
	}

	@Override
	public Agent planAgent(Agent agent, long step) {
		List<Long> path = graph.getLines().get(agent.getStart()).get(agent.getEnd());
		for (int i = 0; i < path.size(); i++) {
			if (stepOccupiedVertices.containsKey(step + i)) {
				if (stepOccupiedVertices.get(step + i).containsKey(path.get(i))) {
					return null;
				}
			} else {
				stepOccupiedVertices.put(step + i, new HashMap<>());
			}
		}

		agent.setPath(path);
		for (int i = 0; i < path.size(); i++) {
			stepOccupiedVertices.get(step + i).put(path.get(i), agent);
		}
		return agent;
	}
}
