package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cz.cuni.mff.kotal.helpers.MyNumberOperations.myModulo;
import static cz.cuni.mff.kotal.helpers.MyNumberOperations.perimeter;

public class BidirectionalRoundabout extends Roundabout {

	public BidirectionalRoundabout(SimulationGraph graph) {
		super(graph);
	}

	@Override
	public Agent planAgent(Agent agent, long step) {
		double agentPerimeter = perimeter(agent.getL(), agent.getW()) * graph.getCellSize();
		long entryNeighbour = graph.getVertex(agent.getEntry()).getNeighbourIDs().stream().findFirst().orElse(0L);
		long firstExitNeighbour;
		long lastExitNeighbour;
		int firstExitNeighbourIndex = -1;
		int lastExitNeighbourIndex = -1;
		int entryNeighbourIndex = roundTrip.indexOf(entryNeighbour);

		if (agent.getExit() < 0) {
			List<Long> exitNeighbourIndexes = directionExits.get((int) agent.getExitDirection()).stream()
				.map(exit -> exitsNeighbours.get(exit.getID()))
				.toList();
			for (int i = 0; i < roundTrip.size(); i++) {
				Long roundID = roundTrip.get(i);
				if (exitNeighbourIndexes.contains(roundID)) {
					if (firstExitNeighbourIndex < 0) {
						firstExitNeighbourIndex = i;
					}
					lastExitNeighbourIndex = i;
				}
			}

			firstExitNeighbour = roundTrip.get(firstExitNeighbourIndex);
			lastExitNeighbour = roundTrip.get(lastExitNeighbourIndex);
		} else {
			lastExitNeighbour = firstExitNeighbour = exitsNeighbours.get(agent.getExit());
			lastExitNeighbourIndex = firstExitNeighbourIndex = roundTrip.indexOf(firstExitNeighbour);
		}

//		boolean increasing = exitNeighbourIndex > entryNeighbourIndex ^ Math.abs(exitNeighbourIndex - entryNeighbourIndex) > roundTrip.size() / 2;
		boolean increasing = getDistance(entryNeighbourIndex, firstExitNeighbourIndex, true) <= getDistance(entryNeighbourIndex, lastExitNeighbourIndex, false);


		long exitNeighbour = increasing ? lastExitNeighbour : firstExitNeighbour;
		List<Long> path = getPath(agent, exitNeighbour, entryNeighbourIndex, increasing);
		if (!validPath(step, path, agentPerimeter)) {
			increasing = !increasing;
			exitNeighbour = increasing ? lastExitNeighbour : firstExitNeighbour;
			path = getPath(agent, lastExitNeighbour, entryNeighbourIndex, !increasing);
			if (!validPath(step, path, agentPerimeter)) {
				return null;
			}
		}

		agent.setExit(exitsNeighbours.entrySet().stream().filter(exitPair -> exitPair.getValue() == exitNeighbour).mapToLong(Map.Entry::getKey).findFirst().orElse(0L));
		agent.setPath(path);
		for (
			int i = 0; i < path.size(); i++) {
			stepOccupiedVertices.get(step + i).put(path.get(i), agent);
		}
		return agent;
	}

	@NotNull
	private List<Long> getPath(Agent agent, long exitNeighbour, int entryNeighbourIndex, boolean increasing) {
		List<Long> path = new ArrayList<>(roundTrip.size());
		path.add(agent.getEntry());
		int index = entryNeighbourIndex;
		long vertexID;
		while ((vertexID = roundTrip.get(index)) != exitNeighbour) {
			path.add(vertexID);
			index = (int) myModulo(index + (increasing ? 1 : -1), roundTrip.size());
		}
		path.add(exitNeighbour);
		path.add(agent.getExit());
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
