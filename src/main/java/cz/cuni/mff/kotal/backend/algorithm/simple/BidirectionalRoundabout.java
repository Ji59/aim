package cz.cuni.mff.kotal.backend.algorithm.simple;

import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static cz.cuni.mff.kotal.helpers.MyNumberOperations.myModulo;

public class BidirectionalRoundabout extends Roundabout {

	public BidirectionalRoundabout(@NotNull SimulationGraph graph) {
		this(graph, true);
	}

	public BidirectionalRoundabout(@NotNull SimulationGraph graph, boolean addPlannedAgent) {
		super(graph, addPlannedAgent);
	}

	@Override
	public Agent planAgent(@NotNull Agent agent, long step) {
		double agentPerimeter = agent.getAgentPerimeter();
		int entryNeighbour = graph.getVertex(agent.getEntry()).getNeighbourIDs().stream().findFirst().orElse(0);
		int firstExitNeighbour;
		int lastExitNeighbour;
		int firstExitNeighbourIndex = -1;
		int lastExitNeighbourIndex = -1;
		int entryNeighbourIndex = roundTrip.indexOf(entryNeighbour);

		if (agent.getExit() < 0) {
			@NotNull List<Integer> exitNeighbourIndexes = directionExits.get(agent.getExitDirection()).stream()
				.map(exit -> getExitsNeighboursMapping().get(exit))
				.toList();
			for (int i = 0; i < roundTrip.size(); i++) {
				int roundID = roundTrip.get(i);
				if (exitNeighbourIndexes.contains(roundID)) {
					if (firstExitNeighbourIndex < 0) {
						firstExitNeighbourIndex = i;
					}
					lastExitNeighbourIndex = i;
				} else if (firstExitNeighbourIndex >= 0) {
					break;
				}
			}

			firstExitNeighbour = roundTrip.get(firstExitNeighbourIndex);
			lastExitNeighbour = roundTrip.get(lastExitNeighbourIndex);
		} else {
			lastExitNeighbour = firstExitNeighbour = getExitsNeighboursMapping().get(agent.getExit());
			lastExitNeighbourIndex = firstExitNeighbourIndex = roundTrip.indexOf(firstExitNeighbour);
		}

//		boolean increasing = exitNeighbourIndex > entryNeighbourIndex ^ Math.abs(exitNeighbourIndex - entryNeighbourIndex) > roundTrip.size() / 2;
		boolean increasing = getDistance(entryNeighbourIndex, firstExitNeighbourIndex, true) <= getDistance(entryNeighbourIndex, lastExitNeighbourIndex, false);


		int exitNeighbour = increasing ? firstExitNeighbour : lastExitNeighbour;
		@NotNull List<Integer> path = getPath(agent, exitNeighbour, entryNeighbourIndex, increasing);
		if (!validPath(step, path, agentPerimeter)) {
			increasing = !increasing;
			exitNeighbour = increasing ? firstExitNeighbour : lastExitNeighbour;
			path = getPath(agent, exitNeighbour, entryNeighbourIndex, !increasing);
			if (!validPath(step, path, agentPerimeter)) {
				return null;
			}
		}

		agent.setPath(path, step);
		addPlannedAgent(agent);

		return agent;
	}

	private @NotNull List<Integer> getPath(@NotNull Agent agent, Integer exitNeighbour, int entryNeighbourIndex, boolean increasing) {
		@NotNull List<Integer> path = new ArrayList<>(roundTrip.size());
		path.add(agent.getEntry());
		int index = entryNeighbourIndex;
		int vertexID;
		while ((vertexID = roundTrip.get(index)) != exitNeighbour) {
			path.add(vertexID);
			index = myModulo(index + (increasing ? 1 : -1), roundTrip.size());
		}
		path.add(exitNeighbour);
		path.add(getExitsNeighboursMapping().get(exitNeighbour));
		return path;
	}

	private int getRoundDistance(int entryNeighbourIndex, int exitNeighbourIndex) {
		return Math.min(Math.abs(exitNeighbourIndex - entryNeighbourIndex), Math.abs(exitNeighbourIndex + roundTrip.size() - entryNeighbourIndex));
	}

	private int getDistance(int entryNeighbourIndex, int exitNeighbourIndex, boolean increasing) {
		if (increasing ^ entryNeighbourIndex >= exitNeighbourIndex) {
			return Math.abs(exitNeighbourIndex - entryNeighbourIndex);
		} else {
			return roundTrip.size() - Math.abs(entryNeighbourIndex - exitNeighbourIndex);
		}
	}
}
