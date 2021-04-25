package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class BreadthFirstSearch implements Algorithm {
	private final Set<VertexWithVisit> vertices;

	public BreadthFirstSearch(Simulation simulation) {
		this.vertices = simulation.getIntersectionGraph().getVertices()
			.stream()
			.map(vertex -> new VertexWithVisit(vertex.getID(), vertex.getType(), vertex.getNeighbourIDs()))
			.collect(Collectors.toSet());
	}

	public BreadthFirstSearch(Graph graph) {
		this.vertices = graph.getVertices()
			.stream()
			.map(vertex -> new VertexWithVisit(vertex.getID(), vertex.getType(), vertex.getNeighbourIDs()))
			.collect(Collectors.toSet());
	}

	@Override
	public void planAgents(Set<Agent> agents) {
		agents.forEach(a -> a.setPath(bfs(a.getStart(), a.getEnd())));
	}

	private List<Long> bfs(long startID, long endID) {
		Map<Long, VertexWithVisit> vertices = this.vertices.stream().map(vertex -> new VertexWithVisit(vertex.getID(), vertex.getType(), vertex.getNeighbourIDs())).collect(Collectors.toMap(Vertex::getID, Function.identity()));
		VertexWithVisit start = vertices.get(startID);
		assert (start != null);
		start.setPathAndAddSelf(new ArrayList<>());
		Queue<VertexWithVisit> queue = new ArrayDeque<>();
		queue.add(start);
		start.setVisited(true);

		while (!queue.isEmpty()) {
			VertexWithVisit first = queue.poll();
			if (first.getNeighbourIDs().contains(endID)) {
				List<Long> path = first.getPath();
				path.add(endID);
				return path;
			}
			first.getNeighbourIDs()
				.stream()
				.map(vertices::get).filter(v -> !v.isVisited()).forEach(neighbour -> {
				neighbour.setPathAndAddSelf(new ArrayList<>(first.getPath()));
				queue.add(neighbour);
				neighbour.setVisited(true);
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

		public void setPathAndAddSelf(List<Long> path) {
			this.path = path;
			path.add(id);
		}
	}
}
