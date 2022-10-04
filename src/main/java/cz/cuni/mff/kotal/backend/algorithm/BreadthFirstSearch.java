package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class BreadthFirstSearch implements Algorithm {
	private final Map<Integer, VertexWithVisit> vertices;

	/**
	 * Create new instance working with graph from provided simulation.
	 * FIXME
	 *
	 * @param simulation
	 */
	public BreadthFirstSearch(Simulation simulation) {
		this(simulation.getIntersectionGraph());
	}

	/**
	 * Create new instance working with provided graph.
	 *
	 * @param graph Graph to search on
	 */
	public BreadthFirstSearch(SimulationGraph graph) {
		vertices = graph.getVerticesSet()
			.stream()
			.map(VertexWithVisit::new)
			.collect(Collectors.toMap(Vertex::getID, Function.identity()));
	}

	@Override
	public Agent planAgent(Agent agent, long step) {
		Set<Integer> exits;
		if (agent.getExit() > 0) {
			exits = vertices.values().stream()
				.filter(vertex -> vertex.getType().isExit() && vertex.getType().getDirection() == agent.getExitDirection())
				.map(Vertex::getID)
				.collect(Collectors.toSet());
		} else {
			exits = Collections.singleton(agent.getExit());
		}
		return planAgent(agent, agent.getEntry(), exits, step);
	}

	/**
	 * Plan agent using BFS on saved graph.
	 *
	 * @param agent    Agent to be planned
	 * @param entryID  TODO
	 * @param exitsIDs
	 * @param step     Actual step of simulation, ignored
	 *
	 * @return Agent if successfully planned otherwise null
	 */
	@Override
	public Agent planAgent(Agent agent, int entryID, Set<Integer> exitsIDs, long step) {
		final int exit = exitsIDs.stream().findFirst().orElse(agent.getExit());
		try {
			agent.setPath(bfs(entryID, exit), step);
		} catch (Exception e) {
			return null;
		}
		return agent;
	}

	/**
	 * Perform BFS on `this.graph` searching path from start to end.
	 *
	 * @param startID ID of starting vertex
	 * @param endID   ID of ending vertex
	 *
	 * @return Found path as list
	 *
	 * @throws RuntimeException If no path was found.
	 */
	private List<Integer> bfs(int startID, int endID) {
		VertexWithVisit start = vertices.get(startID);

		// TODO add excepion
		assert (start != null);

		start.setPathAndAddSelf(new ArrayList<>());
		Queue<VertexWithVisit> queue = new ArrayDeque<>();
		queue.add(start);
		start.setVisited();

		while (!queue.isEmpty()) {
			VertexWithVisit first = queue.poll();
			if (first.getNeighbourIDs().contains(endID)) {
				List<Integer> path = first.getPath();
				path.add(endID);

				resetVisitedVertices();
				return path;
			}
			first.getNeighbourIDs()
				.stream()
				.map(vertices::get).filter(v -> !v.isVisited()).forEach(neighbour -> {
					neighbour.setPathAndAddSelf(new ArrayList<>(first.getPath()));
					queue.add(neighbour);
					neighbour.setVisited();
				});
		}
		// TODO create exception
		resetVisitedVertices();
		throw new RuntimeException("Path not found");
	}

	private void resetVisitedVertices() {
		vertices.values().forEach(VertexWithVisit::setNotVisited);
	}

	@Override
	public void stop() {
		// TODO
	}

	/**
	 * Vertex with saved path to it.
	 */
	private static class VertexWithVisit extends GraphicalVertex {
		private boolean visited = false;
		private List<Integer> path;

		/**
		 * Create new vertex with provided parameters.
		 *
		 * @param id           ID of the vertex
		 * @param type         Type of the vertex
		 * @param neighbourIDs Set of neighbour vertices of the vertex
		 */
		public VertexWithVisit(int id, double x, double y, Type type, Set<Integer> neighbourIDs) {
			super(id, x, y, type, neighbourIDs);
		}

		/**
		 * Create new vertex with provided parameters.
		 *
		 * @param id   ID of the vertex
		 * @param type Type of the vertex
		 */
		public VertexWithVisit(int id, double x, double y, Type type) {
			super(id, x, y, type);
		}

		public VertexWithVisit(GraphicalVertex vertex) {
			super(vertex);
		}

		/**
		 * @return True if this vertex was visited on path otherwise False
		 */
		public boolean isVisited() {
			return visited;
		}

		/**
		 * Set visited to True.
		 */
		public void setVisited() {
			visited = true;
		}

		/**
		 * Set visited to False to reset state.
		 */
		public void setNotVisited() {
			visited = false;
		}

		/**
		 * @return Path to this vertex
		 */
		public List<Integer> getPath() {
			return path;
		}

		/**
		 * Set path to this vertex to provided one and add itself in the end.
		 *
		 * @param path Path to append itself to
		 */
		public void setPathAndAddSelf(List<Integer> path) {
			this.path = path;
			path.add(id);
		}
	}
}
