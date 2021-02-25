package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.backend.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.event.Action;
import cz.cuni.mff.kotal.simulation.event.EventArrivalLeaving;
import cz.cuni.mff.kotal.simulation.event.EventOnWay;
import cz.cuni.mff.kotal.simulation.graph.Vertex;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class BreadthFirstSearch {
	private final Simulation simulation;
	private final Map<Long, VertexWithVisit> vertices;

	public BreadthFirstSearch(Simulation simulation) {
		this.simulation = simulation;
		this.vertices = simulation.getIntersectionGraph().getVertices()
			.stream()
			.map(vertex -> new VertexWithVisit(vertex.getID(), vertex.getType(), vertex.getNeighbourIDs()))
			.collect(Collectors.toMap(Vertex::getID, Function.identity()));
	}

	public void start() {
		for (Agent a : simulation.getAgents()) {
			vertices.values().forEach(vertex -> vertex.setVisited(false));
			List<Long> path = bfs(a.getStart(), a.getEnd());
			assert (path != null);

			EventArrivalLeaving arriving = new EventArrivalLeaving(Action.ARRIVAL, 0, a, a.getStart());
			simulation.getHeap().add(arriving);
			for (int i = 0; i < path.size() - 1; i++) {
				EventOnWay e = new EventOnWay(i, a, path.get(i), path.get(i + 1));
				simulation.getHeap().add(e);
			}
			EventArrivalLeaving leaving = new EventArrivalLeaving(Action.LEAVING, path.size() - 1, a, a.getEnd());
			simulation.getHeap().add(leaving);
		}
	}

	private List<Long> bfs(long startID, long endID) {
		VertexWithVisit start = vertices.get(startID);
		assert (start != null);
		start.setPath(new ArrayList<>());
		Queue<VertexWithVisit> queue = new PriorityQueue<>();
		queue.add(start);

		while (!queue.isEmpty()) {
			VertexWithVisit first = queue.poll();
			first.setVisited(true);
			if (first.getID() == endID) {
				return first.getPath();
			}
			first.getNeighbourIDs()
				.stream()
				.map(vertices::get).filter(v -> !v.isVisited()).forEach(neighbour -> {
				neighbour.setPath(first.getPath());
				queue.add(neighbour);
			});
		}
		// TODO create exception
		return null;
	}

	private static class VertexWithVisit extends Vertex {
		private boolean visited = false;
		private List<Long> path;

		public VertexWithVisit(long id, Type type, Set<Long> neighbourIDs) {
			super(id, type, neighbourIDs);
		}

		public VertexWithVisit(long id, Type type) {
			super(id, type);
		}

		public boolean isVisited() {
			return visited;
		}

		public void setVisited(boolean visited) {
			this.visited = visited;
		}

		public List<Long> getPath() {
			return path;
		}

		public void setPath(List<Long> path) {
			this.path = path;
			path.add(id);
		}
	}
}
