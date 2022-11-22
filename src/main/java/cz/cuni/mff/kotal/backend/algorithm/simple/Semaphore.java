package cz.cuni.mff.kotal.backend.algorithm.simple;

import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public class Semaphore extends SafeLanes {
	private final int directions;
	private final long greenTime;
	private final long directionTime; // TODO

	public Semaphore(@NotNull SimulationGraph graph) {
		super(graph);

		@NotNull Integer longestPath = lanes.values().stream()
			.flatMap(
				map -> map.values().stream()
					.map(List::size)
			).max(Long::compare)
			.orElse(0);

		directions = graph.getModel().getDirections().size() / 2;
		directionTime = ((long) graph.getGranularity() * graph.getEntryExitVertices().size()) / (directions - 1); // TODO zero division TODO extract method
		greenTime = Math.max(1, directionTime - longestPath);
	}

	@Override
	public @NotNull Collection<Agent> planAgents(@NotNull Collection<Agent> agents, long step) {
		if (step % directionTime >= greenTime) {
			return new ArrayList<>();
		}

		filterStepOccupiedVertices(step);

		@NotNull List<Agent> straightAgents = new ArrayList<>();
		@NotNull List<Agent> turningAgents = new ArrayList<>();

		for (@NotNull Agent agent : agents) {
			if (isFromCurrentDirection(agent, step)) {
				if (agentGoingStraight(agent)) {
					straightAgents.add(agent);
				} else {
					turningAgents.add(agent);
				}
			}
		}

		@NotNull List<Agent> plannedAgents = straightAgents.stream().filter(agent -> planAgent(agent, step) != null).collect(Collectors.toList());
		plannedAgents.addAll(turningAgents.stream().filter(agent -> planAgent(agent, step) != null).toList());

		return plannedAgents;
	}

	private boolean isFromCurrentDirection(@NotNull Agent agent, long step) {
		return graph.getVertex(agent.getEntry()).getType().getDirection() % directions == (step / directionTime) % directions;
	}

	private boolean agentGoingStraight(@NotNull Agent agent) {
		return Math.abs(agent.getEntryDirection() - agent.getExitDirection()) % directions == 0;
	}
}
