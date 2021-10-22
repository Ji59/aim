package cz.cuni.mff.kotal.simulation.graph;


import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters;


/**
 * Graph with added attributes for graphical usage.
 */
public abstract class SimulationGraph extends Graph {
	public static final double EPSILON = 1e-7;
	protected final long granularity;
	protected final long entries;
	protected final long exits;
	protected Map<Long, Map<Long, List<Long>>> shortestPaths;

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

	public Map<Long, Map<Long, List<Long>>> getLines() {
		if (shortestPaths == null) {
			shortestPaths = new HashMap<>((int) (entries * getModel().getDirections().size()));
			for (GraphicalVertex entry : entryExitVertices.values().stream()
				.flatMap(Collection::stream)
				.map(vertex -> (GraphicalVertex) vertex)
				.filter(entry -> entry.getType().isEntry())
				.collect(Collectors.toSet())) {

				int entryDirection = entry.getType().getDirection();
				shortestPaths.put(entry.getID(), shortestPaths(entry));
			}
		}
		return shortestPaths;
	}

	private Map<Long, List<Long>> shortestPaths(GraphicalVertex start) {

		VertexWithDirection startWithDirection = new VertexWithDirection(start);
		HashMap<Long, VertexWithDirection> allPaths = new HashMap<>();
		dfs(allPaths, startWithDirection);

		Map<Long, List<Long>> paths = new HashMap<>();
		entryExitVertices.values().stream()
			.flatMap(Collection::stream)
			.filter(exit -> !exit.getType().isEntry() && exit.getType().getDirection() != start.getType().getDirection())
			.map(Vertex::getID)
			.forEach(vertexID -> paths.put(vertexID, allPaths.get(vertexID).getPath()));

		return paths;
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
			VertexWithDirection neighbourVertexWithDirection = new VertexWithDirection(vertex, neighbourVertex);
			vertices.put(neighbourID, neighbourVertexWithDirection);
			dfs(vertices, neighbourVertexWithDirection);
		}
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

	private static class VertexWithDirection extends GraphicalVertex {
		private final double angle;
		private final double distance;
		private final List<Long> path;

		public VertexWithDirection(GraphicalVertex vertex) {
			super(vertex);
			this.angle = Math.atan2(vertex.getX(), vertex.getY());
			this.distance = 0;
			path = Collections.singletonList(vertex.getID());
		}

		public VertexWithDirection(VertexWithDirection previous, GraphicalVertex actual) {
			super(actual);

			double xDiff = actual.getX() - previous.getX();
			double yDiff = actual.getY() - previous.getY();

			double verticesDistance = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
			this.angle = Math.atan2(yDiff, xDiff);
			double angleDiff = Math.abs(this.angle - previous.getAngle());
			if (angleDiff > Math.PI) {
				angleDiff -= Math.PI;
			}
			this.distance = previous.getDistance() + verticesDistance + angleDiff * EPSILON;

			this.path = new ArrayList<>(previous.getPath());
			this.path.add(this.getID());
		}

		public double getAngle() {
			return angle;
		}

		public double getDistance() {
			return distance;
		}

		public List<Long> getPath() {
			return path;
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
	}
}