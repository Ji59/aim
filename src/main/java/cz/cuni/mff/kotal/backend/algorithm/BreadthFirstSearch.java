package cz.cuni.mff.kotal.backend.algorithm;


import cz.cuni.mff.kotal.simulation.Agent;
import cz.cuni.mff.kotal.simulation.Simulation;
import cz.cuni.mff.kotal.simulation.graph.Graph;
import cz.cuni.mff.kotal.simulation.graph.Vertex;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class BreadthFirstSearch implements Algorithm {
	private final Map<Long, VertexWithVisit> vertices;

	/**
	 * Create new instance working with graph from provided simulation.
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
	public BreadthFirstSearch(Graph graph) {
		vertices = graph.getVertices()
			.stream()
			.map(vertex -> new VertexWithVisit(vertex.getID(), vertex.getType(), vertex.getNeighbourIDs()))
			.collect(Collectors.toMap(Vertex::getID, Function.identity()));
	}

	/**
	 * Plan all agents using BFS without any collision check.
	 *
	 * @param agents Set of agents to be planned
	 * @param step   Number of step in which agents should be planned
	 * @return Set of successfully planned agents
	 */
	@Override
	public Collection<Agent> planAgents(Collection<Agent> agents, long step) {
		return agents.stream().filter(agent -> planAgent(agent, step) != null).collect(Collectors.toList());
	}

	/**
	 * Plan agent using BFS on saved graph.
	 *
	 * @param agent Agent to be planned
	 * @param step
	 * @return Agent if successfully planned otherwise null
	 */
	@Override
	public Agent planAgent(Agent agent, long step) {
		try {
			agent.setPath(bfs(agent.getStart(), agent.getEnd()));
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
	private static class VertexWithVisit extends Vertex {
		private boolean visited = false;
		private List<Long> path;

		/**
		 * Create new vertex with provided parameters.
		 *
		 * @param id           ID of the vertex
		 * @param type         Type of the vertex
		 * @param neighbourIDs Set of neighbour vertices of the vertex
		 */
		public VertexWithVisit(long id, Type type, Set<Long> neighbourIDs) {
			super(id, type, neighbourIDs);
		}

		/**
		 * Create new vertex with provided parameters.
		 *
		 * @param id   ID of the vertex
		 * @param type Type of the vertex
		 */
		public VertexWithVisit(long id, Type type) {
			super(id, type);
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
