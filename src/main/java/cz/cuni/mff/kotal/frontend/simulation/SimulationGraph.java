package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionGraph;
import cz.cuni.mff.kotal.simulation.graph.Edge;
import cz.cuni.mff.kotal.simulation.graph.Vertex.Type;

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
			case OCTAGONAL -> createOctagonalGraph(entries, exits);
		}
	}

	/**
	 * Create graph for square model.
	 *
	 * @param entries Number of entries.
	 * @param exits   Number of exits.
	 */
	private void createSquareGraph(long entries, long exits) {
		double height = IntersectionGraph.getPreferredHeight(),
			shift = height / (granularity + 2);

		createSquareGraphVertices(shift, false);

		createSquareGraphEntryVertices(entries, exits, shift, false);
	}

	/**
	 * Create intersection verticies with edges between them.
	 *
	 * @param shift Distance between 2 vertices.
	 */
	private void createSquareGraphVertices(double shift, boolean withoutCorners) {
		long g = granularity;
		long id = vertices.size();
		for (long i = 0; i < granularity; i++) {
			for (long j = 0; j < granularity; j++) {
				// check if in corner
				if ((i == 0 || i == g - 1) &&
					((j == 0) || (j == granularity - 1)) &&
					withoutCorners) {
					continue;
				}

				// create vertex
				Vertex vertex = new Vertex(id, (i + 1.5) * shift, (j + 1.5) * shift, Type.ROAD);
				vertices.put(id, vertex);

				long gs = g * g;

				// add neighbours ids
				createSquareGraphAddNeighbours(vertex, withoutCorners);

				// create edges
				addGraphEdges(id++);
			}
		}
	}

	/**
	 * Compute neighbour IDs.
	 *
	 * @param vertex         Vertex whose neighbours are computed.
	 * @param withoutCorners Denotes, if The graph is with corners or not.
	 */
	private void createSquareGraphAddNeighbours(Vertex vertex, boolean withoutCorners) {
		long g = granularity,
			id = vertex.getID(),
			i = id / granularity,
			j = id % granularity;

		if (withoutCorners) {
			int id_offset = id < g - 2 ? 1 : id >= g * (g - 1) - 2 ? 3 : 2;
			i = (id + id_offset) / g;
			j = (j + id_offset) % g;
		}
		if (i == 0 || i == g - 1) {
			vertex.addNeighbourID(id + (i == 0 ? g : -g) + (withoutCorners ? 1 : 0));
			if (j > (withoutCorners ? 1 : 0)) {
				vertex.addNeighbourID(id - 1);
			}
			if (j < (withoutCorners ? g - 1 : g)) {
				vertex.addNeighbourID(id + 1);
			}
		} else if (j == 0 || j == g - 1) {
			vertex.addNeighbourID(id + (j == 0 ? 1 : -1));
			if (!withoutCorners) {
				vertex.addNeighbourID(id - g, id + g);
			} else {
				if (i > 1) {
					vertex.addNeighbourID(id - g);
				}
				if (i < g - 2) {
					vertex.addNeighbourID(id + g);
				}
			}
		} else {
			if (withoutCorners) {
				vertex.addNeighbourID(id - 1, id + 1, id - g + (i == 1 ? 1 : 0), id + g - (i == g - 2 ? 1 : 0));
			} else {
				vertex.addNeighbourID(id - 1, id + 1, id - g, id + g);
			}
		}
	}

	/**
	 * Create entries and exits vertices with edges connecting them to graph.
	 *
	 * @param entries        Number of entries.
	 * @param exits          Number of exits.
	 * @param shift          distance between 2 neighbour vertices.
	 * @param withoutCorners Denotes, if The graph is with corners or not.
	 */
	private void createSquareGraphEntryVertices(long entries, long exits, double shift, boolean withoutCorners) {
		assert (entries + exits <= granularity - (withoutCorners ? 2 : 0));

		// compute outer empty spaces
		long empty = granularity - entries - exits,
			padding = empty / 3,
			index = empty % 3 == 2 ? ++padding + 1 : padding + 1,
			id = vertices.size();

		// draw all of the entries
		createSquareGraphEntries(entries, shift, index, withoutCorners, true);
		index += entries;

		// skip middle empty space
		index += empty - 2 * padding;

		// draw all of the exits
		createSquareGraphEntries(exits, shift, index, withoutCorners, false);
	}

	/**
	 * Create entries / exits from all sides at specified location.
	 *
	 * @param entries        Number of entries on one side.
	 * @param shift          Distance between 2 vertices.
	 * @param index          Number of square from side where entries should be created from.
	 * @param withoutCorners Denotes, if The graph is with corners or not.
	 * @param entry          Define if creating entries or exits.
	 */
	private void createSquareGraphEntries(long entries, double shift, long index, boolean withoutCorners, boolean entry) {
		double topX = (index + 0.5) * shift,
			topY = shift * 0.5,
			botX = (granularity - index + 1.5) * shift,
			botY = (granularity + 1 + 0.5) * shift;

		// find first unused id
		long id = vertices.size();

		for (int i = 0; i < entries; i++) {
			// create vertices
			Vertex vTop = new Vertex(id++, topX, topY, entry ? Type.ENTRY0 : Type.EXIT0),
				vBot = new Vertex(id++, botX, botY, entry ? Type.ENTRY1 : Type.EXIT1),
				vLeft = new Vertex(id++, topY, botX, entry ? Type.ENTRY2 : Type.EXIT2),
				vRight = new Vertex(id++, botY, topX, entry ? Type.ENTRY3 : Type.EXIT3);

			vertices.put(vTop.getID(), vTop);
			vertices.put(vBot.getID(), vBot);
			vertices.put(vLeft.getID(), vLeft);
			vertices.put(vRight.getID(), vRight);

			// create edges
			Vertex topN = vertices.get((index - 1) * granularity - (withoutCorners ? 2 : 0)),
				botN = vertices.get((granularity + 1 - index) * granularity - (withoutCorners ? 3 : 1)),
				leftN = vertices.get(granularity - index - (withoutCorners ? 1 : 0)),
				rightN = vertices.get((granularity - 1) * granularity + index - (withoutCorners ? 4 : 1));
			assert (topN != null && botN != null && leftN != null && rightN != null);
			edges.add(new Edge(vTop, topN));
			edges.add(new Edge(vBot, botN));
			edges.add(new Edge(vLeft, leftN));
			edges.add(new Edge(vRight, rightN));

			index++;
			topX += shift;
			botX -= shift;
		}
	}

	/**
	 * Create graph for octagonal model.
	 * First of all create grid vertices (like in square model), then the in between vertices and finally entries and exits.
	 *
	 * @param entries Number of entries.
	 * @param exits   Number of exits.
	 */
	private void createOctagonalGraph(long entries, long exits) {
		double height = IntersectionGraph.getPreferredHeight(),
			shift = height / (granularity + 2);

		// create main grid
		createSquareGraphVertices(shift, true);

		long id = vertices.size();

		// create first column
		for (long j = 0; j < granularity - 1; j++, id++) {
			Vertex v = new Vertex(id, 2 * shift, (j + 2) * shift, Type.ROAD);

			long rightTopNeighbourID = granularity - 2 + j;
			v.addNeighbourID(rightTopNeighbourID, rightTopNeighbourID + 1);
			if (j > 0) {
				v.addNeighbourID(j - 1);
			}
			if (j < granularity - 2) {
				v.addNeighbourID(j);
			}

			vertices.put(id, v);
			addGraphEdges(id);
		}

		// create vertices in second, ..., last but one column
		for (long i = 1; i < granularity - 2; i++) {
			for (long j = 0; j < granularity - 1; j++, id++) {
				Vertex v = new Vertex(id, (i + 2) * shift, (j + 2) * shift, Type.ROAD);

				long leftTopNeighbourID = i * granularity - 2 + j;
				long rightTopNeighbourID = leftTopNeighbourID + granularity;
				v.addNeighbourID(leftTopNeighbourID, leftTopNeighbourID + 1, rightTopNeighbourID, rightTopNeighbourID + 1);

				vertices.put(id, v);
				addGraphEdges(id);
			}
		}

		// create last column
		long lastColumnIDStartMinusTwo = (granularity - 1) * granularity - 2,
			lastButOneColumnIDStartMinusTwo = lastColumnIDStartMinusTwo - granularity;
		for (long j = 0; j < granularity - 1; j++, id++) {
			Vertex v = new Vertex(id, granularity * shift, (j + 2) * shift, Type.ROAD);

			long leftTopNeighbourID = lastButOneColumnIDStartMinusTwo + j;
			v.addNeighbourID(leftTopNeighbourID, leftTopNeighbourID + 1);
			if (j > 0) {
				v.addNeighbourID(lastColumnIDStartMinusTwo + j - 1);
			}
			if (j < granularity - 2) {
				v.addNeighbourID(lastColumnIDStartMinusTwo + j);
			}

			vertices.put(id, v);
			addGraphEdges(id);
		}

		createSquareGraphEntryVertices(entries, exits, shift, true);
	}

	/**
	 * Create edges to neighbours with lower IDs.
	 *
	 * @param id ID of the vertex.
	 */
	private void addGraphEdges(long id) {
		Vertex vertex = vertices.get(id);
		for (Long neighbourID : vertex.getNeighbour_ids()) {
			if (neighbourID < id) {
				Vertex neighbour = vertices.get(neighbourID);
				assert (neighbour != null);
				edges.add(new Edge(vertex, neighbour));
			}
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

	/**
	 * @return Granularity of the graph.
	 */
	public long getGranularity() {
		return granularity;
	}

	/**
	 * @return Model Type of the graph.
	 */
	public Parameters.Models getModel() {
		return model;
	}
}
