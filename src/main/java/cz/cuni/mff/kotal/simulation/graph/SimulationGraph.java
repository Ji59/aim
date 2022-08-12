package cz.cuni.mff.kotal.simulation.graph;


import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters;


/**
 * Graph with added attributes for graphical usage.
 */
public abstract class SimulationGraph extends Graph {
	public static final double EPSILON = 0.0625;  // 2 ^ -4
	protected transient final Map<GraphicalVertex, Map<GraphicalVertex, Edge>> verticesDistances = new HashMap<>();
	private transient final Lock verticesDistancesLock = new ReentrantLock(false);
	protected transient Map<Integer, Map<Integer, List<Integer>>> shortestPaths;

	protected transient double cellSize;

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
	protected SimulationGraph(int granularity, int entries, int exits, boolean oriented, Set<GraphicalVertex> vertices, Map<Integer, List<Vertex>> entryExitVertices, Set<Edge> edges) {
		super(oriented, vertices, entryExitVertices, edges, granularity, entries, exits);
	}

	/**
	 * Create graph for simulation.
	 *
	 * @param granularity Granularity of the graph
	 * @param model       Intersection model type
	 * @param entries     Number of entries from each direction
	 * @param exits       Number of exits from each direction
	 */
	protected SimulationGraph(Parameters.GraphType model, int granularity, int entries, int exits, int vertices) {
		super(false, vertices, model.getDirections().size(), granularity, entries, exits);


		for (int i = 0; i < model.getDirections().size(); i++) {
			entryExitVertices.put(i, new ArrayList<>());
		}
	}

	protected SimulationGraph(boolean oriented, Graph graph) {
		super(oriented, graph);
	}


	/**
	 * Add vertices and edges from other graph.
	 *
	 * @param graph Graph to be cloned
	 */
	public void addGraphVertices(Graph graph) {
		this.vertices = graph.getVertices();
		this.entryExitVertices = graph.getEntryExitVertices();
		this.edges = graph.getEdges();
	}

	// TODO create exception if path does not exists
	public Map<Integer, Map<Integer, List<Integer>>> getLines() {
		if (shortestPaths == null) {
			shortestPaths = new HashMap<>(entries * getModel().getDirections().size());
			initializeVerticesDistances();

			entryExitVertices.values().parallelStream()
				.flatMap(Collection::stream)
				.filter(entry -> entry.getType().isEntry())
				.map(GraphicalVertex.class::cast)
				.forEach(entry -> shortestPaths.put(entry.getID(), shortestPaths(entry)));
		}
		return shortestPaths;
	}

	private Map<Integer, List<Integer>> shortestPaths(GraphicalVertex start) {

		VertexWithDirectionParent startWithDirection = new VertexWithDirectionParent(start);
		Map<Integer, List<Integer>> allPaths = ucs(startWithDirection);

		Map<Integer, List<Integer>> paths = new HashMap<>();
		entryExitVertices.values().stream()
			.flatMap(Collection::stream)
			.filter(exit -> exit.getType().isExit())
			.map(Vertex::getID)
			.forEach(vertexID -> paths.put(vertexID, allPaths.get(vertexID)));

		return paths;
	}

	private Map<Integer, List<Integer>> ucs(VertexWithDirectionParent startingVertex) {
		Map<Integer, List<Integer>> vertexDistances = new HashMap<>(vertices.length);

		PriorityQueue<VertexWithDirectionParent> queue = new PriorityQueue<>();
		queue.add(startingVertex);

		while (!queue.isEmpty()) {
			VertexWithDirectionParent vertex = queue.poll();
			if (!vertexDistances.containsKey(vertex.getID())) {
				if (vertex.getParent() == null) {
					vertexDistances.put(vertex.getID(), Collections.singletonList(vertex.getID()));
				} else {
					List<Integer> vertexPath = new ArrayList<>(vertexDistances.get(vertex.getParent().getID()));
					vertexPath.add(vertex.getID());
					vertexDistances.put(vertex.getID(), vertexPath);
				}

				getVerticesDistances().get(vertex).forEach((neighbour, edge) -> {
					if (!vertexDistances.containsKey(neighbour.getID())) {
						queue.add(new VertexWithDirectionParent(vertex, neighbour, edge, getCellSize())); // TODO refactor
					}
				});
			}
		}
		return vertexDistances;
	}

	/**
	 * TODO
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public List<Integer> shortestPath(GraphicalVertex from, GraphicalVertex to) {
		return shortestPath(from, to, 0);
	}

	// TODO

	public List<Integer> shortestPath(GraphicalVertex from, GraphicalVertex to, double startingAngle) {
				PriorityQueue<VertexWithDirectionParent> queue = new PriorityQueue<>();
		HashSet<Integer> visitedIDs = new HashSet<>();
		queue.add(new VertexWithDirectionParent(from, startingAngle));
		while (!queue.isEmpty()) {
			VertexWithDirectionParent vertex = queue.poll();
			if (vertex.getID() == to.getID()) {
				LinkedList<Integer> path = new LinkedList<>();
				VertexWithDirectionParent vertexPath = vertex;
				while (vertexPath != null) {
					path.addFirst(vertexPath.getID());
					vertexPath = vertexPath.getParent();
				}
				return path;
			}
			visitedIDs.add(vertex.getID());
			for (int neighbourID : vertex.getVertex().getNeighbourIDs()) {
				double distance = getDistance(vertex.getID(), neighbourID);
				double heuristic = getDistance(neighbourID, to.getID());
				queue.add(new VertexWithDirectionParent(vertex, getVertex(neighbourID), distance, heuristic));
			}
//			verticesDistances.get(vertex).forEach((neighbour, edge) -> {
//				if (!visitedIDs.contains(neighbour.getID())) {
//					double distance = distances[neighbour.getID()][to.getID()];
//					queue.add(new VertexWithDirection(vertex, neighbour, edge, getCellSize(), distance));
//				}
//			});
		}
		return new ArrayList<>();
	}

	@Deprecated
	private void dfs(Map<Integer, VertexWithDirectionParent> vertices, VertexWithDirectionParent vertex) {
		for (Integer neighbourID : vertex.getVertex().getNeighbourIDs()) {
			GraphicalVertex neighbourVertex;
			neighbourVertex = (GraphicalVertex) this.vertices[neighbourID];
			if (vertices.containsKey(neighbourID)) {
				double distance = VertexWithDirectionParent.getDistance(vertex, neighbourVertex);
				if (distance >= vertices.get(neighbourID).getDistance()) {
					continue;
				}
			}
			VertexWithDirectionParent neighbourVertexWithDirection = new VertexWithDirectionParent(vertex, neighbourVertex, getCellSize());
			vertices.put(neighbourID, neighbourVertexWithDirection);
			dfs(vertices, neighbourVertexWithDirection);
		}
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
	public Collection<GraphicalVertex> getVerticesSet() {
		return Stream.of(vertices).map(GraphicalVertex.class::cast).collect(Collectors.toSet());
	}

	/**
	 * TODO
	 *
	 * @param id ID of desired vertex
	 * @return
	 */
	@Override
	public GraphicalVertex getVertex(int id) {
		return (GraphicalVertex) vertices[id];
	}

	/**
	 * @return Granularity of the graph
	 */
	public int getGranularity() {
		return granularity;
	}

	/**
	 * @return Entries to this graph
	 */
	public int getEntries() {
		return entries;
	}

	/**
	 * @return Exits to this graph
	 */
	public int getExits() {
		return exits;
	}


	/**
	 * @return Model Type of the graph
	 */
	public abstract Parameters.GraphType getModel();

	/**
	 * @return @return Column / row shift based on granularity
	 */
	public abstract double getCellSize();

	protected Map<GraphicalVertex, Map<GraphicalVertex, Edge>> getVerticesDistances() {
		verticesDistancesLock.lock();
		if (verticesDistances.isEmpty()) {
			initializeVerticesDistances();
		}
		verticesDistancesLock.unlock();
		return verticesDistances;
	}
	protected void initializeVerticesDistances() {
		verticesDistancesLock.lock();
		for (Vertex vertex : vertices) {
			verticesDistances.put((GraphicalVertex) vertex, new HashMap<>());
		}
		for (Edge edge : this.edges) {
			verticesDistances.get(edge.getU()).put((GraphicalVertex) edge.getV(), edge);
		}
		verticesDistancesLock.unlock();
	}

	/**
	 * TODO
	 *
	 * @param v0
	 * @param v1
	 * @return
	 */
	public Edge getEdge(Vertex v0, Vertex v1) {
		return getVerticesDistances().get(v0).get(v1);
	}

}