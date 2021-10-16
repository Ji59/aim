package cz.cuni.mff.kotal.simulation.graph;


import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.graph.Vertex.Type;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters;


/**
 * Graph with added attributes for graphical usage.
 */
public abstract class SimulationGraph extends Graph {
	protected final long granularity;
	protected final long entries;
	protected final long exits;

	/**
	 * Create new simulation graph.
	 *
	 * @param granularity       Granularity of the graph
	 * @param entries           Number of entries from each direction
	 * @param exits             Number of exits from each direction
	 * @param oriented          If this graph is oriented or not
	 * @param vertices          Set of vertices in this graph
	 * @param entryExitVertices Map of vertices in each entry / exit direction
	 * @param edges             Graph edges
	 */
	protected SimulationGraph(long granularity, long entries, long exits, boolean oriented, Set<GraphicalVertex> vertices, Map<Integer, List<Vertex>> entryExitVertices, Set<Edge> edges) {
		super(oriented, vertices, entryExitVertices, edges);
		this.granularity = granularity;
		this.entries = entries;
		this.exits = exits;
	}

	/**
	 * Create graph for simulation.
	 *
	 * @param granularity Granularity of the graph
	 * @param model       Intersection model type
	 * @param entries     Number of entries from each direction
	 * @param exits       Number of exits from each direction
	 */
	protected SimulationGraph(Parameters.Models model, long granularity, long entries, long exits) {
		super(false, model.getDirections().size());
		this.granularity = granularity;
		this.entries = entries;
		this.exits = exits;


		for (int i = 0; i < model.getDirections().size(); i++) {
			entryExitVertices.put(i, new ArrayList<>());
		}
	}


	/**
	 * Add vertices and edges from other graph.
	 *
	 * @param graph Graph to be cloned
	 */
	public void addGraphVertices(Graph graph) {
		this.vertices = graph.getVerticesWithIDs();
		this.entryExitVertices = graph.getEntryExitVertices();
		this.edges = graph.getEdges();
	}

	/**
	 * Check if the graph is same as another graph.
	 * That means they are same model, have same granularity, size and orientation and same number of entries and exits.
	 *
	 * @param o Compared object
	 * @return True if the object is graph and has same key features
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SimulationGraph graph)) return false;
		return oriented == graph.oriented && granularity == graph.granularity && entries == graph.entries && exits == graph.exits && getModel() == graph.getModel();
	}

	@Override
	public int hashCode() {
		return Objects.hash(oriented, granularity, entries, exits, getModel());
	}


	/**
	 * @return Set of vertices of the graph
	 */
	@Override
	public Collection<GraphicalVertex> getVertices() {
		return vertices.values().stream().map(GraphicalVertex.class::cast).collect(Collectors.toSet());
	}

	public GraphicalVertex getVertex(long id) {
		return (GraphicalVertex) vertices.get(id);
	}

	/**
	 * @return Set of edges of the graph
	 */
	@Override
	public Set<Edge> getEdges() {
		return edges;
	}

	/**
	 * @return Granularity of the graph
	 */
	public long getGranularity() {
		return granularity;
	}

	/**
	 * @return Entries to this graph
	 */
	public long getEntries() {
		return entries;
	}

	/**
	 * @return Exits to this graph
	 */
	public long getExits() {
		return exits;
	}


	/**
	 * @return Model Type of the graph
	 */
	public abstract Parameters.Models getModel();

	/**
	 * @return @return Column / row shift based on granularity
	 */
	public abstract double getCellSize();
}