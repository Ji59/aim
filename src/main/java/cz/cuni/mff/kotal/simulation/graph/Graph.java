package cz.cuni.mff.kotal.simulation.graph;


import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Graph of vertices and edges.
 */
public class Graph {

	protected final boolean oriented;
	protected Map<Long, Vertex> vertices;
	protected Map<Integer, List<Vertex>> entryExitVertices;
	protected Set<Edge> edges;

	/**
	 * Create graph with provided vertices and edges. If not oriented, add edges with reversed orientation too.
	 *
	 * @param oriented          If graph edges are oriented
	 * @param vertices          Set of vertices
	 * @param entryExitVertices Map of entry and exit vertices from different sides. Indexed by index of entry side
	 * @param edges             Set of edges
	 */
	public Graph(boolean oriented, Set<? extends Vertex> vertices, Map<Integer, List<Vertex>> entryExitVertices, Set<Edge> edges) {
		this.oriented = oriented;
		this.entryExitVertices = entryExitVertices;
		if (vertices != null) {
			this.vertices = vertices.stream().collect(Collectors.toMap(Vertex::getID, Function.identity()));
		} else {
			this.vertices = null;
		}
		if (oriented || edges == null) {
			this.edges = edges;
		} else {
			this.edges = new HashSet<>(edges);
			this.edges.addAll(edges.parallelStream().map(Edge::reverse).collect(Collectors.toSet())); // add reversed edges
		}
	}

	/**
	 * Create empty graph with specified attributes.
	 *
	 * @param oriented   True if and only if this graph is oriented
	 * @param entrySides Number of entry directions
	 */
	public Graph(boolean oriented, int entrySides) {
		this.oriented = oriented;
		vertices = new HashMap<>();
		edges = new HashSet<>();

		entryExitVertices = new HashMap<>();
		for (int i = 0; i < entrySides; i++) {
			entryExitVertices.put(i, new ArrayList<>());
		}
	}

	/**
	 * Create edges to neighbours with lower IDs.
	 *
	 * @param id ID of the vertex
	 */
	protected void addGraphEdges(long id) {
		cz.cuni.mff.kotal.simulation.graph.Vertex vertex = vertices.get(id);
		assert (vertex != null);
		for (Long neighbourID : vertex.getNeighbourIDs()) {
			if (neighbourID < id) {
				GraphicalVertex neighbour = (GraphicalVertex) vertices.get(neighbourID);

				assert (neighbour != null);
				assert (neighbour.getNeighbourIDs().contains(id));

				boolean notContainEdge = edges.add(new Edge(vertex, neighbour));
				assert (notContainEdge);

				notContainEdge = edges.add(new Edge(neighbour, vertex));
				assert (notContainEdge);
			}
		}
	}

	/**
	 * @return True if the graph is oriented, otherwise false
	 */
	public boolean isOriented() {
		return oriented;
	}

	/**
	 * @return Set of vertices of the graph
	 */
	public Collection<? extends Vertex> getVertices() {
		return vertices.values();
	}

	public Map<Long, Vertex> getVerticesWithIDs() {
		return vertices;
	}

	/**
	 * @return Map of entries and exits in different directions
	 */
	public Map<Integer, List<Vertex>> getEntryExitVertices() {
		return entryExitVertices;
	}

	/**
	 * @return Set of edges of the graph
	 */
	public Set<Edge> getEdges() {
		return edges;
	}
}