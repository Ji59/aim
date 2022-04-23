package cz.cuni.mff.kotal.backend.algorithm;

import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph.VertexWithDirection;
import cz.cuni.mff.kotal.simulation.graph.Vertex;
import javafx.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Roundabout extends SafeLines {
	List<Integer> roundTrip = new ArrayList<>();
	private final Map<Integer, Integer> exitsNeighboursMapping = new HashMap<>();

	public Roundabout(SimulationGraph graph) {
		super(graph);

		Collection<List<Vertex>> entriesExits = graph.getEntryExitVertices().values();
		if (entriesExits.isEmpty()) {
			return;
		} else if (entriesExits.size() == 1) {
			entriesExits.forEach(e -> roundTrip.addAll(sortEntriesExits(e, graph)));
		}
		List<List<Integer>> directionParts = entriesExits.parallelStream().map(directionList -> sortEntriesExits(directionList, graph)).toList();

		Map<Integer, Map<Integer, List<Integer>>> distances = new HashMap<>(directionParts.size());
		int from = 0;
		int to = 0;
		int shortest = Integer.MAX_VALUE;
		for (int i = 0; i < directionParts.size(); i++) {
			int iDirectionPartsSize = directionParts.get(i).size();
			GraphicalVertex iDirectionLast = graph.getVertex(directionParts.get(i).get(iDirectionPartsSize - 1));
			double angle = 0;
			if (iDirectionPartsSize >= 2) {
				GraphicalVertex iDirectionOneButLast = graph.getVertex(directionParts.get(i).get(iDirectionPartsSize - 2));
				angle = VertexWithDirection.computeAngle(iDirectionOneButLast, iDirectionLast);
			}
			Map<Integer, List<Integer>> iDistances = new HashMap<>();
			for (int j = 0; j < directionParts.size(); j++) {
				if (i == j) {
					continue;
				}
				GraphicalVertex jDirectionFirst = graph.getVertex(directionParts.get(j).get(0));
				List<Integer> path = graph.shortestPath(iDirectionLast, jDirectionFirst, angle);
				if (path.size() < shortest) {
					from = i;
					to = j;
					shortest = path.size();
				}
				iDistances.put(j, path);
			}
			distances.put(i, iDistances);
		}

		int start = from;
		while (!distances.isEmpty()) {
			List<Integer> directionVertices = directionParts.get(from);
			if (!roundTrip.isEmpty() && roundTrip.get(roundTrip.size() - 1).equals(directionVertices.get(0))) {
				directionVertices.remove(0);
			}
			roundTrip.addAll(directionVertices);
			List<Integer> pathIDs = distances.get(from).get(to);
			if (pathIDs.size() > 2) {
				pathIDs.remove(0);
				pathIDs.remove(pathIDs.size() - 1);
				roundTrip.addAll(pathIDs);
			}
			distances.remove(from);
			from = to;
			if (distances.size() >= 2) {
				to = distances.get(from).entrySet().stream()
					.filter(e -> distances.containsKey(e.getKey()))
					.min(Comparator.comparingInt(e -> e.getValue().size()))
					.map(Map.Entry::getKey).orElse(0);
			} else {
				to = start;
			}
		}
		if (roundTrip.size() > 1 && roundTrip.get(0).equals(roundTrip.get(roundTrip.size() - 1))) {
			roundTrip.remove(0);
		}
	}

	@Override
	public Agent planAgent(Agent agent, long step) {
		int exitNeighbour;
		final Integer agentExit;
		if (agent.getExit() < 0) {
			int exitNeighbourIndex = directionExits.get(agent.getExitDirection()).stream()
				.mapToInt(exit -> roundTrip.indexOf(exitsNeighboursMapping.get(exit.getID())))
				.min().orElse(0);
			exitNeighbour = roundTrip.get(exitNeighbourIndex);
			agentExit = exitsNeighboursMapping.entrySet().stream().filter(exitPair -> exitPair.getValue() == exitNeighbour).mapToInt(Map.Entry::getKey).findFirst().orElse(0);
		} else {
			exitNeighbour = exitsNeighboursMapping.get(agent.getExit());
			agentExit = agent.getExit();
		}

		int entryNeighbour = graph.getVertex(agent.getEntry()).getNeighbourIDs().stream().findFirst().orElse(0);
		List<Integer> path = new ArrayList<>(roundTrip.size());
		path.add(agent.getEntry());
		int startIndex;
		for (startIndex = 0; startIndex < roundTrip.size(); startIndex++) {
			if (roundTrip.get(startIndex) == entryNeighbour) {
				break;
			}
		}
		int index = startIndex;
		int vertexID;
		while ((vertexID = roundTrip.get(index)) != exitNeighbour) {
			path.add(vertexID);
			index = (index + 1) % roundTrip.size();
		}
		path.add(exitNeighbour);
		path.add(agentExit);

		if (!validPath(step, path, agent.getAgentPerimeter())) {
			return null;
		}
		agent.setPath(path, step);
		for (int i = 0; i < path.size(); i++) {
			stepOccupiedVertices.get(step + i).put(path.get(i), agent);
		}
		return agent;
	}

	private List<Integer> sortEntriesExits(List<Vertex> directionEntriesExits, SimulationGraph graph) {
		List<GraphicalVertex> entriesNeighboursVertices = new ArrayList<>();
		List<GraphicalVertex> exitsNeighboursVertices = new ArrayList<>();
		directionEntriesExits.forEach(v -> {
			if (v.getType().isEntry()) {
				Integer neighbour = v.getNeighbourIDs().stream().findFirst().orElse(null);
				if (neighbour == null) {
					// TODO
					throw new RuntimeException("Neighbour of entry " + v.getID() + " not found.");
				}
				entriesNeighboursVertices.add(graph.getVertex(neighbour));
			} else {
				GraphicalVertex neighbour = graph.getVertices().stream().filter(n -> n.getNeighbourIDs().contains(v.getID())).findFirst().orElse(null);
				if (neighbour == null) {
					// TODO
					throw new RuntimeException("Neighbour of exit " + v.getID() + " not found.");
				}
				exitsNeighboursVertices.add(neighbour);
				exitsNeighboursMapping.put(v.getID(), neighbour.getID());
				exitsNeighboursMapping.put(neighbour.getID(), v.getID());
			}
		});

		Map<GraphicalVertex, Map<GraphicalVertex, List<Integer>>> distances = new HashMap<>();
		exitsNeighboursVertices.parallelStream().forEach(exit -> {
			Map<GraphicalVertex, List<Integer>> exitMap = entriesNeighboursVertices.stream()
				.collect(Collectors.toMap(Function.identity(), entry -> graph.shortestPath(exit, entry)));
			distances.put(exit, exitMap); // TODO check thread safety
		});

		Pair<GraphicalVertex, GraphicalVertex> closest = null;
		int closestDistance = Integer.MAX_VALUE;
		for (Map.Entry<GraphicalVertex, Map<GraphicalVertex, List<Integer>>> exitDistances : distances.entrySet()) {
			GraphicalVertex exit = exitDistances.getKey();
			for (Map.Entry<GraphicalVertex, List<Integer>> entryDistances : exitDistances.getValue().entrySet()) {
				GraphicalVertex entry = entryDistances.getKey();
				int distance = entryDistances.getValue().size();
				if (distance < closestDistance) {
					closest = new Pair<>(exit, entry);
					closestDistance = distance;
				}
			}
		}

		if (closest == null) {
			throw new RuntimeException("Closest entry and exit not found.");
			// TODO
		}
		GraphicalVertex exit = closest.getKey();
		GraphicalVertex entry = closest.getValue();

		LinkedList<GraphicalVertex> pathPoints = new LinkedList<>();
		pathPoints.add(exit);
		pathPoints.addLast(entry);
//		exitsNeighboursVertices.remove(exit);
//		entriesNeighboursVertices.remove(entry);

		distances.entrySet().stream()
			.filter(e -> e.getKey() != exit)
			.map(e -> new Pair<>(e.getKey(), e.getValue().get(entry).size()))
			.sorted(Comparator.comparingInt(Pair::getValue))
			.forEach(p -> pathPoints.addFirst(p.getKey()));

		distances.get(exit).entrySet().stream()
			.filter(e -> e.getKey() != entry)
			.sorted(Comparator.comparingInt(e -> e.getValue().size()))
			.map(Map.Entry::getKey)
			.forEach(pathPoints::addLast);

		List<Integer> path = new ArrayList<>();
		GraphicalVertex previous = null;
		for (GraphicalVertex vertex : pathPoints) {
			if (previous != null) {
				List<Integer> nextPath = graph.shortestPath(previous, vertex);
				assert !nextPath.isEmpty();
				nextPath.remove(0);
				path.addAll(nextPath);
			} else {
				path.add(vertex.getID());
			}
			previous = vertex;
		}
		return path;
	}

	/**
	 * @return map containing mapping between exits IDs and their neighbours IDs both ways
	 */
	protected Map<Integer, Integer> getExitsNeighboursMapping() {
		return exitsNeighboursMapping;
	}
}
