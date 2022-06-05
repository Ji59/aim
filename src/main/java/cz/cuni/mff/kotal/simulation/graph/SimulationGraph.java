package cz.cuni.mff.kotal.simulation.graph;


import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import org.jetbrains.annotations.NotNull;

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

		VertexWithDirection startWithDirection = new VertexWithDirection(start);
		Map<Integer, List<Integer>> allPaths = ucs(startWithDirection);

		Map<Integer, List<Integer>> paths = new HashMap<>();
		entryExitVertices.values().stream()
			.flatMap(Collection::stream)
			.filter(exit -> exit.getType().isExit())
			.map(Vertex::getID)
			.forEach(vertexID -> paths.put(vertexID, allPaths.get(vertexID)));

		return paths;
	}

	private Map<Integer, List<Integer>> ucs(VertexWithDirection startingVertex) {
		Map<Integer, List<Integer>> vertexDistances = new HashMap<>(vertices.length);

		PriorityQueue<VertexWithDirection> queue = new PriorityQueue<>();
		queue.add(startingVertex);

		while (!queue.isEmpty()) {
			VertexWithDirection vertex = queue.poll();
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
	 * @param from
	 * @param to
	 * @return
	 */
	public List<Integer> shortestPath(GraphicalVertex from, GraphicalVertex to) {
		return shortestPath(from, to, 0);
	}

	// TODO

	public List<Integer> shortestPath(GraphicalVertex from, GraphicalVertex to, double startingAngle) {
				PriorityQueue<VertexWithDirection> queue = new PriorityQueue<>();
		HashSet<Integer> visitedIDs = new HashSet<>();
		queue.add(new VertexWithDirection(from, startingAngle));
		while (!queue.isEmpty()) {
			VertexWithDirection vertex = queue.poll();
			if (vertex.getID() == to.getID()) {
				LinkedList<Integer> path = new LinkedList<>();
				VertexWithDirection vertexPath = vertex;
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
				queue.add(new VertexWithDirection(vertex, getVertex(neighbourID), distance, heuristic));
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
	private void dfs(Map<Integer, VertexWithDirection> vertices, VertexWithDirection vertex) {
		for (Integer neighbourID : vertex.getVertex().getNeighbourIDs()) {
			GraphicalVertex neighbourVertex;
			neighbourVertex = (GraphicalVertex) this.vertices[neighbourID];
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

	public static class VertexWithDirection implements Comparable<VertexWithDirection> {
		private final GraphicalVertex vertex;
		private final double angle;
		private final double distance;
		private final double estimate;
		private final VertexWithDirection parent;

		public VertexWithDirection(GraphicalVertex vertex) {
			this(vertex, 0);
		}

		public VertexWithDirection(GraphicalVertex vertex, double angle) {
			this(vertex, angle, 0);
		}

		public VertexWithDirection(GraphicalVertex vertex, double angle, double estimate) {
			this.vertex = vertex;
			this.angle = angle;
			this.distance = 0;
			parent = null;
			this.estimate = estimate;
		}

		public VertexWithDirection(VertexWithDirection previous, GraphicalVertex actual, Edge edge, double cellDistance) {
			this(previous, actual, edge, cellDistance, 0);
		}

		// TODO refactor
		public VertexWithDirection(VertexWithDirection previous, GraphicalVertex actual, Edge edge, double cellDistance, double estimate) {
			vertex = actual;

			GraphicalVertex previousVertex = previous.getVertex();
			double xDiff = previousVertex.getX() - vertex.getX();
			double yDiff = previousVertex.getY() - vertex.getY();
			this.angle = computeAngle(xDiff, yDiff);

			double verticesDistance = edge == null ? Math.sqrt(xDiff * xDiff + yDiff * yDiff) / cellDistance : edge.getDistance();

			double angleDiff;
			if (previous.getParent() == null || previous.getID() == getID()) {
				angleDiff = 0;
			} else {
				angleDiff = Math.abs(this.angle - previous.getAngle());
				if (angleDiff > Math.PI) {
					angleDiff = 2 * Math.PI - angleDiff;
				}
			}

//			double middleDistanceX = getX() - 0.5;
//			double middleDistanceY = getY() - 0.5;
//			double middleDistance = Math.sqrt(middleDistanceX * middleDistanceX + middleDistanceY * middleDistanceY);  // TODO

			this.distance = previous.getDistance() + verticesDistance + angleDiff * EPSILON; // + middleDistance * EPSILON * EPSILON;
			this.parent = previous;
			this.estimate = estimate;
		}

		public VertexWithDirection(VertexWithDirection previous, GraphicalVertex actual, double distance, double estimate) {
			vertex = actual;
			GraphicalVertex previousVertex = previous.getVertex();
			double xDiff = previousVertex.getX() - vertex.getX();
			double yDiff = previousVertex.getY() - vertex.getY();
			this.angle = computeAngle(xDiff, yDiff);

			double angleDiff;
			if (previous.getID() == getID()) {
				angleDiff = 0;
			} else {
				angleDiff = Math.abs(this.angle - previous.getAngle());
				if (angleDiff > Math.PI) {
					angleDiff = 2 * Math.PI - angleDiff;
				}
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

		public int getID() {
			return vertex.getID();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Vertex vertexObject) {
				return vertex.getID() == vertexObject.getID();
			} else if (o instanceof VertexWithDirection vertexWithDirection) {
				return vertex.getID() == vertexWithDirection.getVertex().getID();
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(vertex.getID());
		}

		public static double computeAngle(GraphicalVertex start, GraphicalVertex end) {
			double xDiff = start.getX() - end.getX();
			double yDiff = start.getY() - end.getY();
			return computeAngle(xDiff, yDiff);
		}

		public static double computeAngle(double xDiff, double yDiff) {
			return Math.atan2(yDiff, xDiff);
		}

		public GraphicalVertex getVertex() {
			return vertex;
		}

		public static double getDistance(VertexWithDirection start, GraphicalVertex end) {
			GraphicalVertex startVertex = start.getVertex();
			double x0 = startVertex.getX();
			double y0 = startVertex.getY();
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
	}
}