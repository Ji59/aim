package cz.cuni.mff.kotal.simulation.graph;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Graph of vertices and edges.
 */
public class Graph {

	protected final boolean oriented;
	protected final int granularity;
	protected final int entries;
	protected final int exits;
	protected Vertex[] vertices;
	protected Map<Integer, List<Vertex>> entryExitVertices;
	protected Set<Edge> edges;
	protected double[][] distances;

	/**
	 * Create graph with provided vertices and edges. If not oriented, add edges with reversed orientation too.
	 *
	 * @param oriented          If graph edges are oriented
	 * @param vertices          Set of vertices
	 * @param entryExitVertices Map of entry and exit vertices from different sides. Indexed by index of entry side
	 * @param edges             Set of edges
	 */
	public Graph(boolean oriented, Set<? extends Vertex> vertices, Map<Integer, List<Vertex>> entryExitVertices, Set<Edge> edges, int granularity, int entries, int exits) {
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
	protected void addGraphEdges(Integer id) {
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

	@Deprecated
	public Map<Integer, Vertex> getVerticesWithIDs() {
		return Stream.of(vertices).collect(Collectors.toMap(Vertex::getID, Function.identity()));
	}

	/**
	 * TODO
	 *
	 * @return
	 */
	public double[][] getDistances() {
		if (distances == null) {
			System.out.println("Initializing distances.");
			initializeDistances();
			System.out.println("Initializing distances finished.");

			computeDistances();
		}

		return distances;
	}

	private void computeDistances() {

		int directions = entryExitVertices.size();
		int entriesVertices = directions * entries;
		int exitsVertices = directions * exits;

		Integer[] verticesEdgesTo = new Integer[vertices.length - entriesVertices];
		int to = 0;
		Integer[] verticesEdgesFrom = new Integer[vertices.length - exitsVertices];
		int from = 0;
		Integer[] verticesBoth = new Integer[vertices.length - entriesVertices - exitsVertices];
		int both = 0;

		for (Vertex vertex : vertices) {
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
				Double distIK;
				if (i == k || (distIK = distances[i][k]).isInfinite()) {
					continue;
				}

				/**
				 Arrays.stream(verticesEdgesTo).parallel().forEach(j -> {
				 Double distKJ;
				 if (j == i || j == k || (distKJ = distances.get(k).get(j)).isInfinite()) {
				 return;
				 }

				 double distIJ = distances.get(i).get(j);
				 if (distIJ > distIK + distKJ) {
				 distances.get(i).replace(j, distKJ);
				 }
				 });
				 /*/
				double distKJ;
				for (Integer j : verticesEdgesTo) {
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

	private void initializeDistances() {
		int verticesSize = vertices.length;
		distances = new double[verticesSize][verticesSize];
		for (int i = 0; i < verticesSize; i++) {
			for (int j = 0; j < verticesSize; j++) {
				double initialValue = i == j ? 0 : Double.POSITIVE_INFINITY;
				distances[i][j] = initialValue;
			}
		}

		for (Edge edge : edges) {
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

}