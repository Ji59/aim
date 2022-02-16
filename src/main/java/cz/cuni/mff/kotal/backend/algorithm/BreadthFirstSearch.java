package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.helpers.MyGenerator.generateRandomInt;


public class BreadthFirstSearch implements Algorithm {
	private final Map<Long, VertexWithVisit> vertices;

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
		vertices = graph.getVertices()
			.stream()
			.map(vertex -> new VertexWithVisit((GraphicalVertex) vertex))
			.collect(Collectors.toMap(Vertex::getID, Function.identity()));
	}

	/**
	 * Plan agent using BFS on saved graph.
	 *
	 * @param agent Agent to be planned
	 * @param step  Actual step of simulation, ignored
	 * @return Agent if successfully planned otherwise null
	 */
	@Override
	public Agent planAgent(Agent agent, long step) {
		final long exit;
		if (agent.getExit() < 0) {
			List<VertexWithVisit> directionExits = vertices.values().stream().filter(vertex -> vertex.getType().isExit() && vertex.getType().getDirection() == agent.getExitDirection()).collect(Collectors.toList());
			exit = directionExits.get(generateRandomInt(directionExits.size() - 1)).getID();
		} else {
			exit = agent.getExit();
		}
		try {
			agent.setPath(bfs(agent.getEntry(), exit), step);
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
	 * @return Found path as list
	 * @throws RuntimeException If no path was found.
	 */
	private List<Long> bfs(long startID, long endID) {
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
				List<Long> path = first.getPath();
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

	/**
	 * Vertex with saved path to it.
	 */
	private static class VertexWithVisit extends GraphicalVertex {
		private boolean visited = false;
		private List<Long> path;

		/**
		 * Create new vertex with provided parameters.
		 *
		 * @param id           ID of the vertex
		 * @param type         Type of the vertex
		 * @param neighbourIDs Set of neighbour vertices of the vertex
		 */
		public VertexWithVisit(long id, double x, double y, Type type, Set<Long> neighbourIDs) {
			super(id, x, y, type, neighbourIDs);
		}

		/**
		 * Create new vertex with provided parameters.
		 *
		 * @param id   ID of the vertex
		 * @param type Type of the vertex
		 */
		public VertexWithVisit(long id, double x, double y, Type type) {
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
		public List<Long> getPath() {
			return path;
		}

		/**
		 * Set path to this vertex to provided one and add itself in the end.
		 *
		 * @param path Path to append itself to
		 */
		public void setPathAndAddSelf(List<Long> path) {
			this.path = path;
			path.add(id);
		}
	}
}
