package cz.cuni.mff.kotal.simulation.graph;


import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.helpers.MyNumberOperations;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters;


/**
 * Graph with added attributes for graphical usage.
 */
public abstract class SimulationGraph extends Graph {
	public static final double EPSILON = 1e-1;
	private final Map<GraphicalVertex, Map<GraphicalVertex, Edge>> verticesDistances = new HashMap<>();
	protected final long granularity;
	protected final long entries;
	protected final long exits;
	protected Map<Long, Map<Long, List<Long>>> shortestPaths;
	protected Map<Long, Map<Long, Double>> distances;

	protected double cellSize;

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

	// TODO create exception if path does not exists
	public Map<Long, Map<Long, List<Long>>> getLines() {
		if (shortestPaths == null) {
			shortestPaths = new HashMap<>((int) (entries * getModel().getDirections().size()));
			initializeVerticesDistances();

			entryExitVertices.values().parallelStream()
				.flatMap(Collection::stream)
				.filter(entry -> entry.getType().isEntry())
				.map(GraphicalVertex.class::cast)
				.forEach(entry -> shortestPaths.put(entry.getID(), shortestPaths(entry)));
		}
		return shortestPaths;
	}

	private void initializeVerticesDistances() {
		vertices.values().forEach(vertex -> verticesDistances.put((GraphicalVertex) vertex, new HashMap<>()));
		for (Edge edge : this.edges) {
			verticesDistances.get((GraphicalVertex) edge.getU()).put((GraphicalVertex) edge.getV(), edge);
		}
	}

	private Map<Long, List<Long>> shortestPaths(GraphicalVertex start) {

		VertexWithDirection startWithDirection = new VertexWithDirection(start);
		Map<Long, List<Long>> allPaths = ucs(startWithDirection);

		Map<Long, List<Long>> paths = new HashMap<>();
		entryExitVertices.values().stream()
			.flatMap(Collection::stream)
			.filter(exit -> exit.getType().isExit())
			.map(Vertex::getID)
			.forEach(vertexID -> paths.put(vertexID, allPaths.get(vertexID)));

		return paths;
	}

	private Map<Long, List<Long>> ucs(VertexWithDirection startingVertex) {
		Map<Long, List<Long>> vertexDistances = new HashMap<>(vertices.size());

		PriorityQueue<VertexWithDirection> queue = new PriorityQueue<>();
		queue.add(startingVertex);

		while (!queue.isEmpty()) {
			VertexWithDirection vertex = queue.poll();
			if (!vertexDistances.containsKey(vertex.getID())) {
				if (vertex.getParent() == null) {
					vertexDistances.put(vertex.getID(), Collections.singletonList(vertex.getID()));
				} else {
					List<Long> vertexPath = new ArrayList<>(vertexDistances.get(vertex.getParent().getID()));
					vertexPath.add(vertex.getID());
					vertexDistances.put(vertex.getID(), vertexPath);
				}

				verticesDistances.get(vertex).forEach((neighbour, edge) -> {
					if (!vertexDistances.containsKey(neighbour.getID())) {
						queue.add(new VertexWithDirection(vertex, neighbour, edge, getCellSize())); // TODO refactor
					}
				});
			}
		}
		return vertexDistances;
	}

	/**
	 * TODO
	 *
	 * @return
	 */
	public Map<Long, Map<Long, Double>> getDistances() {
		if (distances == null) {
			System.out.println("Initializing distances.");
			initializeDistancesMap();
			System.out.println("Initializing distances finished.");

			computeDistances();
		}

		return distances;
	}

	private void computeDistances() {

		int directions = entryExitVertices.size();
		int entriesVertices = (int) (directions * entries);
		int exitsVertices = (int) (directions * exits);

		long[] verticesEdgesTo = new long[vertices.size() - entriesVertices];
		int to = 0;
		long[] verticesEdgesFrom = new long[vertices.size() - exitsVertices];
		int from = 0;
		long[] verticesBoth = new long[vertices.size() - entriesVertices - exitsVertices];
		int both = 0;

		for (Vertex vertex : vertices.values()) {
			long id = vertex.getID();
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
		for (long k : verticesBoth) {
			for (long i : verticesEdgesFrom) {
				Double distIK;
				if (i == k || (distIK = distances.get(i).get(k)).isInfinite()) {
					continue;
				}

				/**/
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
				Double distKJ;
				for (long j : verticesEdgesTo) {
					if (j == i || j == k || (distKJ = distances.get(k).get(j)).isInfinite()) {
						continue;
					}

					double distIJ = distances.get(i).get(j);
					if (distIJ > distIK + distKJ) {
						distances.get(i).replace(j, distKJ);
					}
				}
				/**/
			}
		}
		System.out.println("Computing distances finished after " + (System.nanoTime() - time) + " ns.");
	}

	private void initializeDistancesMap() {
		int capacity = 8 * vertices.size(); // TODO
		distances = new HashMap<>(capacity);
		for (Vertex vertex : vertices.values()) {
			HashMap<Long, Double> vertexDistances = new HashMap<>(capacity);
			for (Vertex u : vertices.values()) {
				Edge edge = getEdge(vertex, u);
				double initialDistance;
				if (edge != null) {
					initialDistance = edge.getDistance();
				} else if (vertex.equals(u)) {
					initialDistance = 0.;
				} else {
					initialDistance = Double.POSITIVE_INFINITY;
				}
				vertexDistances.put(u.getID(), initialDistance);
			}
			distances.put(vertex.getID(), vertexDistances);
		}
	}

	/**
	 * TODO
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public double getDistance(long from, long to) {
		return getDistances().get(from).get(to);
	}

	/**
	 * TODO
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public List<Long> shortestPath(GraphicalVertex from, GraphicalVertex to) {
		return shortestPath(from, to, 0);
	}

	// TODO

	public List<Long> shortestPath(GraphicalVertex from, GraphicalVertex to, double startingAngle) {
		if (verticesDistances.isEmpty()) {
			initializeVerticesDistances();
		}
		PriorityQueue<VertexWithDirection> queue = new PriorityQueue<>();
		HashSet<Long> visitedIDs = new HashSet<>();
		queue.add(new VertexWithDirection(from, startingAngle));
		while (!queue.isEmpty()) {
			VertexWithDirection vertex = queue.poll();
			if (vertex.getID() == to.getID()) {
				LinkedList<Long> path = new LinkedList<>();
				VertexWithDirection vertexPath = vertex;
				while (vertexPath != null) {
					path.addFirst(vertexPath.getID());
					vertexPath = vertexPath.getParent();
				}
				return path;
			}
			visitedIDs.add(vertex.getID());
			verticesDistances.get(vertex).forEach((neighbour, edge) -> {
				if (!visitedIDs.contains(neighbour.getID())) {
					double distance = MyNumberOperations.distance(neighbour.getX(), neighbour.getY(), to.getX(), to.getY());
					queue.add(new VertexWithDirection(vertex, neighbour, edge, getCellSize(), distance)); // TODO refactor
				}
			});
		}
		return new ArrayList<>();
	}

	private void dfs(Map<Long, VertexWithDirection> vertices, VertexWithDirection vertex) {
		for (long neighbourID : vertex.getNeighbourIDs()) {
			GraphicalVertex neighbourVertex;
			neighbourVertex = (GraphicalVertex) this.vertices.get(neighbourID);
			if (vertices.containsKey(neighbourID)) {
				double distance = VertexWithDirection.getDistance(vertex, neighbourVertex);
				if (distance >= vertices.get(neighbourID).getDistance()) {
					continue;
				}
			}
			VertexWithDirection neighbourVertexWithDirection = new VertexWithDirection(vertex, neighbourVertex, getCellSize());
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
	@Override
	public Collection<GraphicalVertex> getVertices() {
		return vertices.values().stream().map(GraphicalVertex.class::cast).collect(Collectors.toSet());
	}

	/**
	 * TODO
	 *
	 * @param id ID of desired vertex
	 * @return
	 */
	public GraphicalVertex getVertex(long id) {
		return (GraphicalVertex) vertices.get(id);
	}

	/**
	 * TODO
	 *
	 * @param v0
	 * @param v1
	 * @return
	 */
	public Edge getEdge(Vertex v0, Vertex v1) {
		if (verticesDistances.isEmpty()) {
			initializeVerticesDistances();
		}
		return verticesDistances.get((GraphicalVertex) v0).get((GraphicalVertex) v1);
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

	public static class VertexWithDirection extends GraphicalVertex implements Comparable<VertexWithDirection> {
		private final double angle;
		private final double distance;
		private final double estimate;
		private final VertexWithDirection parent;

		public VertexWithDirection(GraphicalVertex vertex) {
			this(vertex, 0);
		}

		public VertexWithDirection(GraphicalVertex vertex, double angle) {
			super(vertex);
			this.angle = angle;
			this.distance = 0;
			parent = null;
			estimate = 0;
		}

		public VertexWithDirection(VertexWithDirection previous, GraphicalVertex actual, Edge edge, double cellDistance) {
			this(previous, actual, edge, cellDistance, 0);
		}

		// TODO refactor
		public VertexWithDirection(VertexWithDirection previous, GraphicalVertex actual, Edge edge, double cellDistance, double estimate) {
			super(actual);

			double xDiff = previous.getX() - getX();
			double yDiff = previous.getY() - getY();
			this.angle = computeAngle(xDiff, yDiff);

			double verticesDistance = edge == null ? Math.sqrt(xDiff * xDiff + yDiff * yDiff) / cellDistance : edge.getDistance();

			double angleDiff = Math.abs(this.angle - previous.getAngle());
			if (angleDiff > Math.PI) {
				angleDiff = 2 * Math.PI - angleDiff;
			}

//			double middleDistanceX = getX() - 0.5;
//			double middleDistanceY = getY() - 0.5;
//			double middleDistance = Math.sqrt(middleDistanceX * middleDistanceX + middleDistanceY * middleDistanceY);  // TODO

			this.distance = previous.getDistance() + verticesDistance + angleDiff * EPSILON; // + middleDistance * EPSILON * EPSILON;
			this.parent = previous;
			this.estimate = estimate;
		}

		public VertexWithDirection(VertexWithDirection previous, GraphicalVertex actual, double distance, double estimate) {
			super(actual);
			double xDiff = previous.getX() - getX();
			double yDiff = previous.getY() - getY();
			this.angle = computeAngle(xDiff, yDiff);

			double angleDiff = Math.abs(this.angle - previous.getAngle());
			if (angleDiff > Math.PI) {
				angleDiff = 2 * Math.PI - angleDiff;
			}

			this.distance = previous.getDistance() + distance + angleDiff * EPSILON; // + middleDistance * EPSILON * EPSILON;
			this.parent = previous;
			this.estimate = estimate;
		}

		public VertexWithDirection(VertexWithDirection previous, GraphicalVertex actual, double cellDistance) {
			this(previous, actual, null, cellDistance, 0);
		}

		public double getAngle() {
			return angle;
		}

		public double getDistance() {
			return distance;
		}

		public double getEstimate() {
			return estimate;
		}

		public VertexWithDirection getParent() {
			return parent;
		}

		public static double computeAngle(GraphicalVertex start, GraphicalVertex end) {
			double xDiff = start.getX() - end.getX();
			double yDiff = start.getY() - end.getY();
			return computeAngle(xDiff, yDiff);
		}

		public static double computeAngle(double xDiff, double yDiff) {
			return Math.atan2(yDiff, xDiff);
		}

		public static double getDistance(VertexWithDirection start, GraphicalVertex end) {
			double x0 = start.getX();
			double y0 = start.getY();
			double x1 = end.getX();
			double y1 = end.getY();

			double xDiff = x1 - x0;
			double yDiff = y1 - y0;

			double verticesDistance = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
			double angle = Math.atan2(yDiff, xDiff);
			double angleDiff = Math.abs(angle - start.getAngle());
			if (angleDiff > Math.PI) {
				angleDiff -= Math.PI;
			}
			return start.getDistance() + verticesDistance + angleDiff * EPSILON;
		}

		@Override
		public int compareTo(@NotNull SimulationGraph.VertexWithDirection o) {
			return Double.compare(distance + estimate, o.getDistance() + o.estimate);
		}

// FIXME
//		@Override
//		public boolean equals(Object o) {
//			if (o instanceof Vertex vertex) {
//				return id == vertex.getID();
//			}
//			return false;
//		}
	}
}