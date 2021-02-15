package cz.cuni.mff.kotal.simulation.graph;


import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class Graph {

	private final boolean oriented;
	private final Set<Vertex> vertices;
	private final Set<Edge> edges;

	/**
	 * Create graph with provided vertices and edges. If not oriented, add edges with reversed orientation too.
	 *
	 * @param oriented If graph edges are oriented.
	 * @param vertices Set of vertices.
	 * @param edges    Set of edges.
	 */
	public Graph(boolean oriented, Set<Vertex> vertices, Set<Edge> edges) {
		this.oriented = oriented;
		this.vertices = vertices;
		if (oriented) {
			this.edges = edges;
		} else {
			this.edges = new HashSet<>(edges);
			this.edges.addAll(edges.parallelStream().map(Edge::reverse).collect(Collectors.toSet())); // add reversed edges
		}
	}

	/**
	 * @return Set of vertices of the graph.
	 */
	public Set<Vertex> getVertices() {
		return vertices;
	}

	/**
	 * @return Set of edges of the graph.
	 */
	public Set<Edge> getEdges() {
		return edges;
	}
}