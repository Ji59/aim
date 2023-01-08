package cz.cuni.mff.kotal.simulation.graph;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters.GraphType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Graph of vertices and edges.
 */
public abstract class Graph {
	private static final Lock DISTANCES_CACHING_LOCK = new ReentrantLock(false);

	protected transient final boolean oriented;
	protected final int granularity;
	protected final int entries;
	protected final int exits;
	private transient final Lock distancesLock = new ReentrantLock(false);
	protected transient Vertex @Nullable [] vertices;
	protected transient Map<Integer, List<Vertex>> entryExitVertices;
	protected transient Set<Edge> edges;
	protected transient double[][] distances;

	/**
	 * Create graph with provided vertices and edges. If not oriented, add edges with reversed orientation too.
	 *
	 * @param oriented          If graph edges are oriented
	 * @param vertices          Set of vertices
	 * @param entryExitVertices Map of entry and exit vertices from different sides. Indexed by index of entry side
	 * @param edges             Set of edges
	 */
	public Graph(boolean oriented, @Nullable Set<? extends Vertex> vertices, Map<Integer, List<Vertex>> entryExitVertices, @Nullable Set<Edge> edges, int granularity, int entries, int exits) {
		this.oriented = oriented;
		this.entryExitVertices = entryExitVertices;
		if (vertices != null) {
			this.vertices = new Vertex[vertices.size()];
			vertices.forEach(vertex -> this.vertices[vertex.getID()] = vertex);
		} else {
			this.vertices = null;
		}
		if (oriented || edges == null) {
			this.edges = edges;
		} else {
			this.edges = new HashSet<>(edges);
			this.edges.addAll(edges.parallelStream().map(Edge::reverse).collect(Collectors.toSet())); // add reversed edges
		}
		this.granularity = granularity;
		this.entries = entries;
		this.exits = exits;
	}

	public Graph(boolean oriented, @NotNull Graph graph) {
		this.oriented = oriented;
		granularity = graph.granularity;
		entries = graph.entries;
		exits = graph.exits;

		entryExitVertices = graph.getEntryExitVertices();
		edges = new HashSet<>();
	}

	/**
	 * Create empty graph with specified attributes.
	 *
	 * @param oriented   True if and only if this graph is oriented
	 * @param entrySides Number of entry directions
	 */
	public Graph(boolean oriented, int vertices, int entrySides, int granularity, int entries, int exits) {
		this.oriented = oriented;
		this.vertices = new Vertex[vertices];
		edges = new HashSet<>();

		entryExitVertices = new HashMap<>();
		for (int i = 0; i < entrySides; i++) {
			entryExitVertices.put(i, new ArrayList<>());
		}
		this.granularity = granularity;
		this.entries = entries;
		this.exits = exits;
	}

	/**
	 * Create edges to neighbours with lower IDs.
	 *
	 * @param id ID of the vertex
	 */
	protected void addGraphEdges(@NotNull Integer id) {
		Vertex vertex = vertices[id];
		assert (vertex != null);
		for (Integer neighbourID : vertex.getNeighbourIDs()) {
			if (neighbourID < id) {
				Vertex neighbour = vertices[neighbourID];

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
	public Vertex[] getVertices() {
		return vertices;
	}

	public @NotNull Graph setVertices(Vertex[] vertices) {
		this.vertices = vertices;
		return this;
	}

	public @NotNull Graph setVertices(@NotNull Collection<? extends Vertex> vertices) {
		this.vertices = vertices.toArray(new Vertex[0]);
		return this;
	}

	@Deprecated
	public @NotNull Map<Integer, Vertex> getVerticesWithIDs() {
		return Stream.of(vertices).collect(Collectors.toMap(Vertex::getID, Function.identity()));
	}

	/**
	 * TODO
	 *
	 * @return
	 */
	public double[][] getDistances() {
		createDistances(false);

		return distances;
	}

	public void createDistances(boolean caching) {
		if (caching) {
			DISTANCES_CACHING_LOCK.lock();
		}

		distancesLock.lock();
		if (distances == null && (!caching || this.equals(IntersectionModel.getGraph()))) {
			System.out.println("Initializing distances.");
			initializeDistances();
			System.out.println("Initializing distances finished.");

			computeDistances();
		}
		distancesLock.unlock();
		if (caching) {
			DISTANCES_CACHING_LOCK.unlock();
		}
	}

	private void computeDistances() {

		int directions = entryExitVertices.size();
		int entriesVertices = directions * entries;
		int exitsVertices = directions * exits;

		int @NotNull [] verticesEdgesTo = new int[vertices.length - entriesVertices];
		int to = 0;
		int @NotNull [] verticesEdgesFrom = new int[vertices.length - exitsVertices];
		int from = 0;
		int @NotNull [] verticesBoth = new int[vertices.length - entriesVertices - exitsVertices];
		int both = 0;

		for (@NotNull Vertex vertex : vertices) {
			int id = vertex.getID();
			if (vertex.getType().isEntry()) {
				verticesEdgesFrom[from++] = id;
			} else if (vertex.getType().isExit()) {
				verticesEdgesTo[to++] = id;
			} else {
				verticesEdgesFrom[from++] = id;
				verticesBoth[both++] = id;
				verticesEdgesTo[to++] = id;
			}
		}

		System.out.println("Computing distances.");
		long time = System.nanoTime();

		// Perform Floyd–Warshall update algorithm
		for (int k : verticesBoth) {
			for (int i : verticesEdgesFrom) {
				double distIK;
				if (i == k || Double.isInfinite(distIK = distances[i][k])) {
					continue;
				}

				/**/
				// 31 120 695 987 ns
				Arrays.stream(verticesEdgesTo).parallel().forEach(j -> {
					double distKJ;
					if (j == i || j == k || Double.isInfinite(distKJ = distances[k][j])) {
						return;
					}

					double distIJ = distances[i][j];
					if (distIJ > distIK + distKJ) {
						distances[i][j] = distIK + distKJ;
					}
				});
				 /*/
				// 20 956 137 142 ns
				double distKJ;
				for (int j : verticesEdgesTo) {
					if (j == i || j == k || Double.isInfinite(distKJ = distances[k][j])) {
						continue;
					}

					double distIJ = distances[i][j];
					if (distIJ > distIK + distKJ) {
						distances[i][j] = distIK + distKJ;
					}
				}
				/**/
			}
		}
		System.out.println("Computing distances finished after " + (System.nanoTime() - time) + " ns.");
	}

	protected void initializeDistances() {
		int verticesSize = vertices.length;
		distances = new double[verticesSize][verticesSize];
		for (int i = 0; i < verticesSize; i++) {
			for (int j = 0; j < verticesSize; j++) {
				double initialValue = i == j ? 0 : Double.POSITIVE_INFINITY;
				distances[i][j] = initialValue;
			}
		}

		for (@NotNull Edge edge : edges) {
			int id0 = edge.getU().getID();
			int id1 = edge.getV().getID();
			double distance = edge.getDistance();
			distances[id0][id1] = distance;
		}
	}

	/**
	 * TODO
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public double getDistance(int from, int to) {
		return getDistances()[from][to];
	}

	/**
	 * @param id ID of desired vertex
	 * @return Vertex from this graph with specified ID
	 */
	public Vertex getVertex(int id) {
		return vertices[id];
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
	public abstract GraphType getModel();

	/**
	 * @return @return Column / row shift based on granularity
	 */
	public abstract double getCellSize();
}