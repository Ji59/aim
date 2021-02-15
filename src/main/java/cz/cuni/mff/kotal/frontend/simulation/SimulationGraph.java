package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionGraph;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionMenu;
import cz.cuni.mff.kotal.simulation.graph.Edge;
import cz.cuni.mff.kotal.simulation.graph.Vertex.Type;
import javafx.scene.paint.Color;

import java.util.*;

import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters;


public class SimulationGraph {
	private final boolean oriented;
	private final Map<Long, Vertex> vertices;
	private final Set<Edge> edges;
	private final long granularity;
	private final Parameters.Models model;

	public SimulationGraph(long granularity, Parameters.Models model, boolean oriented, Set<Vertex> vertices, Set<Edge> edges) {
		this.oriented = oriented;
		this.vertices = new HashMap<>();
		this.edges = edges;
		this.granularity = granularity;
		this.model = model;

		vertices.forEach(vertex -> this.vertices.put(vertex.getID(), vertex));
	}

	public SimulationGraph(long granularity, Parameters.Models model, long entries, long exits) {
		this.oriented = false;
		this.vertices = new HashMap<>();
		this.edges = new HashSet<>();
		this.granularity = granularity;
		this.model = model;

		switch (model) {
			case SQUARE -> createSquareGraph(entries, exits);
		}
	}

	private void createSquareGraph(long entries, long exits) {
		double height = IntersectionGraph.getPreferredHeight(),
			shift = height / (granularity + 2);

		long g = granularity;

		for (long i = 0; i < granularity; i++) {
			for (long j = 0; j < granularity; j++) {
				long id = i * granularity + j;
				// create vertex
				Vertex vertex = new Vertex(id, (i + 1.5) * shift, (j + 1.5) * shift, Type.ROAD);
				vertices.put(id, vertex);

				long gs = g * g;

				// add neighbours ids
				vertex.addNeighbourID((id - 1) % gs, (id + 1) % gs, (id - g) % g, (id + 1) % g);

				// create edges
				squareGraphCreateEdges(i, j, id);
			}
		}

		// compute outer empty spaces
		long empty = granularity - entries - exits,
			padding = empty / 3,
			index = empty % 3 == 2 ? ++padding + 1 : padding + 1,
			id = granularity * granularity;

		// draw all of the entries
		double topX = (index + 0.5) * shift,
			topY = shift * 0.5,
			botX = (granularity - index + 1.5) * shift,
			botY = (granularity + 1 + 0.5) * shift;

		squareGraphCreateEntries(entries, shift, topX, topY, botX, botY, index, Type.ENTRY);
		index += entries;

		// skip middle empty space
		index += empty - 2 * padding;

		// draw all of the exits
		topX = (index + 0.5) * shift;
		botX = (granularity - index + 1.5) * shift;
		squareGraphCreateEntries(exits, shift, topX, topY, botX, botY, index, Type.EXIT);
	}

	/**
	 * Create edges to neighbours with lower IDs for vertex at specified coordinations.
	 *
	 * @param i  Number of vertex in row (X).
	 * @param j  Number of vertex in column (Y).
	 * @param id ID of the vertex.
	 */
	private void squareGraphCreateEdges(long i, long j, long id) {
		Vertex vertex = vertices.get(id);
		if (i > 0) {
			edges.add(new Edge(vertex, vertices.get(id - granularity)));
			// if last row
			if (i == this.granularity - 1) {
				edges.add(new Edge(vertex, vertices.get(j)));
			}
		}
		if (j > 0) {
			edges.add(new Edge(vertex, vertices.get(id - 1)));
			if (j == this.granularity - 1) {
				edges.add(new Edge(vertex, vertices.get(i * this.granularity)));
			}
		}
	}

	private void squareGraphCreateEntries(long entries, double shift, double topX, double topY, double botX, double botY, long index, Type type) {
		long id = vertices.size();

		for (int i = 0; i < entries; i++) {
			Vertex vTop = new Vertex(id++, topX, topY, type),
				vBot = new Vertex(id++, botX, botY, type),
				vLeft = new Vertex(id++, topY, botX, type),
				vRight = new Vertex(id++, botY, topX, type);

			vertices.put(vTop.getID(), vTop);
			vertices.put(vBot.getID(), vBot);
			vertices.put(vLeft.getID(), vLeft);
			vertices.put(vRight.getID(), vRight);

			edges.add(new Edge(vTop, vertices.get((index - 1) * granularity)));
			edges.add(new Edge(vBot, vertices.get((granularity + 1 - index) * granularity - 1)));
			edges.add(new Edge(vLeft, vertices.get(granularity - index)));
			edges.add(new Edge(vRight, vertices.get((granularity - 1) * granularity + index - 1)));

			index++;
			topX += shift;
			botX -= shift;
		}
	}

	/**
	 * @return Set of vertices of the graph.
	 */
	public Collection<Vertex> getVertices() {
		return vertices.values();
	}

	/**
	 * @return Set of edges of the graph.
	 */
	public Set<Edge> getEdges() {
		return edges;
	}

	public long getGranularity() {
		return granularity;
	}

	public Parameters.Models getModel() {
		return model;
	}
}
