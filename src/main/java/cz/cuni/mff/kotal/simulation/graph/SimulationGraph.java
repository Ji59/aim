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
	protected final transient Map<GraphicalVertex, Map<GraphicalVertex, Edge>> verticesDistances = new HashMap<>();
	private final transient Lock verticesDistancesLock = new ReentrantLock(false);
	protected transient Map<Integer, Map<Integer, Double>[]>[] travelDistances;
	protected transient @NotNull Map<Integer, List<VertexDistance>>[] sortedVertexVerticesDistances;
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
	protected SimulationGraph(Parameters.@NotNull GraphType model, int granularity, int entries, int exits, int vertices) {
		super(false, vertices, model.getDirections().size(), granularity, entries, exits);


		for (int i = 0; i < model.getDirections().size(); i++) {
			entryExitVertices.put(i, new ArrayList<>());
		}
	}

	protected SimulationGraph(boolean oriented, @NotNull Graph graph) {
		super(oriented, graph);
	}

	protected static double getTransferDistance(GraphicalVertex u, GraphicalVertex v, GraphicalVertex p, GraphicalVertex q) {
		if (u.equals(p) || v.equals(q)) {
			return 0;
		}

		final double cx = p.getX() - u.getX();
		final double cy = p.getY() - u.getY();
		final double dx = v.getX() - q.getX() + cx;
		final double dy = v.getY() - q.getY() + cy;

		double t = 0;
		if (dx != 0 || dy != 0) {
			t = (cx * dx + cy * dy) / (dx * dx + dy * dy);
			t = Math.min(1, Math.max(0, t));
		}

		double closestX;
		double closestY;
		if (t == 0) {
			closestX = u.getX() - p.getX();
			closestY = u.getY() - p.getY();
		} else if (t == 1) {
			closestX = v.getX() - q.getX();
			closestY = v.getY() - q.getY();
		} else {
			closestX = t * dx - cx;
			closestY = t * dy - cy;
		}
		closestX *= closestX;
		closestY *= closestY;
		final double distanceSquared = closestX + closestY;
		return Math.sqrt(distanceSquared);
	}


	// TODO

	/**
	 * Add vertices and edges from other graph.
	 *
	 * @param graph Graph to be cloned
	 */
	public void addGraphVertices(@NotNull Graph graph) {
		this.vertices = graph.getVertices();
		this.entryExitVertices = graph.getEntryExitVertices();
		this.edges = graph.getEdges();
	}

	/**
	 * TODO
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public List<Integer> shortestPath(GraphicalVertex from, @NotNull GraphicalVertex to) {
		return shortestPath(from, to, 0);
	}

	public @NotNull List<Integer> shortestPath(GraphicalVertex from, @NotNull GraphicalVertex to, double startingAngle) {
		@NotNull PriorityQueue<VertexWithDirectionParent> queue = new PriorityQueue<>();
		@NotNull HashSet<Integer> visitedIDs = new HashSet<>();
		queue.add(new VertexWithDirectionParent(from, startingAngle));
		while (!queue.isEmpty()) {
			VertexWithDirectionParent vertex = queue.poll();
			if (vertex.getID() == to.getID()) {
				@NotNull LinkedList<Integer> path = new LinkedList<>();
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
	private void dfs(@NotNull Map<Integer, VertexWithDirectionParent> vertices, @NotNull VertexWithDirectionParent vertex) {
		for (Integer neighbourID : vertex.getVertex().getNeighbourIDs()) {
			GraphicalVertex neighbourVertex;
			neighbourVertex = (GraphicalVertex) this.vertices[neighbourID];
			if (vertices.containsKey(neighbourID)) {
				double distance = VertexWithDirectionParent.getDistance(vertex, neighbourVertex);
				if (distance >= vertices.get(neighbourID).getDistance()) {
					continue;
				}
			}
			@NotNull VertexWithDirectionParent neighbourVertexWithDirection = new VertexWithDirectionParent(vertex, neighbourVertex, getCellSize());
			vertices.put(neighbourID, neighbourVertexWithDirection);
			dfs(vertices, neighbourVertexWithDirection);
		}
	}

	/**
	 *
	 */
	@Override
	protected void initializeDistances() {
		@NotNull final Thread thread = new Thread(this::createTravelDistances);
		thread.start();
		super.initializeDistances();
		try {
			thread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void createTravelDistances() {
		assert vertices != null;
		travelDistances = new Map[getVertices().length];
		sortedVertexVerticesDistances = new Map[getVertices().length];

		Arrays.stream(getVertices()).map(GraphicalVertex.class::cast).forEach(u -> {
			final int vertexID = u.getID();
			int capacity = u.getNeighbourIDs().size() + 1;
			final Map<Integer, Map<Integer, Double>[]> uDistances = new HashMap<>(capacity);
			synchronized (travelDistances) {
				travelDistances[vertexID] = uDistances;
			}
			Map<Integer, List<VertexDistance>> sortedUVerticesDistance = new HashMap<>(capacity);
			synchronized (sortedVertexVerticesDistances) {
				sortedVertexVerticesDistances[vertexID] = sortedUVerticesDistance;
			}

			for (final int vNeighbourID : u.getNeighbourIDs()) {
				@NotNull final GraphicalVertex v = getVertex(vNeighbourID);
				createNeighbourTransferDistances(u, v, uDistances, sortedUVerticesDistance);
			}
			createNeighbourTransferDistances(u, u, uDistances, sortedUVerticesDistance);
		});
	}

	private void createNeighbourTransferDistances(@NotNull GraphicalVertex u, @NotNull GraphicalVertex v, @NotNull Map<Integer, @NotNull Map<Integer, Double>[]> uDistances, Map<Integer, List<VertexDistance>> sortedUVerticesDistances) {
		final Map[] uvDistances = new Map[getVertices().length];
		uDistances.put(v.getID(), uvDistances);
		final List<VertexDistance> sortedUVDistances = new LinkedList<>();
		sortedUVerticesDistances.put(v.getID(), sortedUVDistances);

		Arrays.stream(getVertices()).map(GraphicalVertex.class::cast).forEach(p -> {
			final Map<Integer, Double> uvpDistances = new HashMap<>(p.getNeighbourIDs().size() + 1);
			uvDistances[p.getID()] = uvpDistances;

			double closest = Double.MAX_VALUE;
			for (final int pNeighbourID : p.getNeighbourIDs()) {
				@NotNull final GraphicalVertex q = getVertex(pNeighbourID);
				double distance = getTransferDistance(u, v, p, q);
				uvpDistances.put(pNeighbourID, distance);
				closest = Math.min(closest, distance);
			}

			double distance = getTransferDistance(u, v, p, p);
			uvpDistances.put(p.getID(), distance);
			closest = Math.min(closest, distance);
			synchronized (sortedUVDistances) {
				sortedUVDistances.add(new VertexDistance(p.getID(), closest));
			}
		});

		sortedUVDistances.sort(VertexDistance::compareTo);
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
		return oriented == graph.oriented && getGranularity() == graph.getGranularity() && getEntries() == graph.getEntries() && getExits() == graph.getExits() && getModel() == graph.getModel();
	}

	@Override
	public int hashCode() {
		return Objects.hash(oriented, getGranularity(), getEntries(), getExits(), getModel());
	}

	/**
	 * @return Set of vertices of the graph
	 */
	public @NotNull Collection<GraphicalVertex> getVerticesSet() {
		return Stream.of(vertices).map(GraphicalVertex.class::cast).collect(Collectors.toSet());
	}

	/**
	 * TODO
	 *
	 * @param id ID of desired vertex
	 * @return
	 */
	@Override
	public @NotNull GraphicalVertex getVertex(int id) {
		return (GraphicalVertex) vertices[id];
	}


	public @NotNull Map<GraphicalVertex, Map<GraphicalVertex, Edge>> getVerticesDistances() {
		verticesDistancesLock.lock();
		if (verticesDistances.isEmpty()) {
			initializeVerticesDistances();
		}
		verticesDistancesLock.unlock();
		return verticesDistances;
	}

	public @NotNull Map<Integer, Map<Integer, Double>[]>[] getTravelDistances() {
		assert travelDistances != null;
		return travelDistances;
	}

	public @NotNull Map<Integer, List<VertexDistance>>[] getSortedVertexVerticesDistances() {
		assert sortedVertexVerticesDistances != null;
		return sortedVertexVerticesDistances;
	}

	protected void initializeVerticesDistances() {
		verticesDistancesLock.lock();
		for (Vertex vertex : vertices) {
			verticesDistances.put((GraphicalVertex) vertex, new HashMap<>());
		}
		for (@NotNull Edge edge : this.edges) {
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

	public record VertexDistance(int vertexID, double distance) implements Comparable<VertexDistance> {

		@Override
		public int compareTo(@NotNull VertexDistance o) {
			return Double.compare(distance, o.distance);
		}
	}
}