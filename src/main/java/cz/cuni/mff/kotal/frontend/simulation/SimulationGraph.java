package cz.cuni.mff.kotal.frontend.simulation;


import cz.cuni.mff.kotal.simulation.graph.Edge;
import cz.cuni.mff.kotal.simulation.graph.Vertex.Type;

import java.util.*;

import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters;


public class SimulationGraph {
	private final boolean oriented;
	private final Map<Long, Vertex> vertices;
	private final Set<Edge> edges;
	private final long granularity, entries, exits;
	private final double size;
	private final Parameters.Models model;
	private final boolean abstractGraph;

	public SimulationGraph(long granularity, long entries, long exits, Parameters.Models model, boolean oriented, Set<Vertex> vertices, Set<Edge> edges, double size, boolean abstractGraph) {
		this.oriented = oriented;
		this.size = size;
		this.vertices = new HashMap<>();
		this.edges = edges;
		this.granularity = granularity;
		this.entries = entries;
		this.exits = exits;
		this.model = model;
		this.abstractGraph = abstractGraph;

		if (vertices != null) {
			vertices.forEach(vertex -> this.vertices.put(vertex.getID(), vertex));
		}
	}

	/**
	 * Create graph for simulation.
	 *
	 * @param granularity   Granularity of the graph.
	 * @param model         Model type.
	 * @param entries       Number of entries.
	 * @param exits         Number of exits.
	 * @param size
	 * @param abstractGraph
	 */
	public SimulationGraph(long granularity, Parameters.Models model, long entries, long exits, double size, boolean abstractGraph) {
		this.size = size;
		this.oriented = false;
		this.vertices = new HashMap<>();
		this.edges = new HashSet<>();
		this.granularity = granularity;
		this.entries = entries;
		this.exits = exits;
		this.model = model;
		this.abstractGraph = abstractGraph;

		switch (model) {
			case SQUARE -> createSquareGraph(entries, exits);
			case OCTAGONAL -> createOctagonalGraph(entries, exits);
			case HEXAGONAL -> createHexagonalGraph(entries, exits);
		}
	}

	/**
	 * Create graph for square model.
	 *
	 * @param entries Number of entries.
	 * @param exits   Number of exits.
	 */
	private void createSquareGraph(long entries, long exits) {
		double shift = size / (granularity + 2);

		createSquareGraphVertices(shift, false);

		createSquareGraphEntryVertices(entries, exits, shift, false);
	}

	/**
	 * Create graph for hexagonal model.
	 * First of all crate center vertex, then all the other from inside to outside.
	 *
	 * @param entries Number of entries.
	 * @param exits   Number of exits.
	 */
	private void createHexagonalGraph(long entries, long exits) {
		double shift = size / (2 * granularity + 1);

		createHexagonalGraphRoadVertices(shift);

		createHexagonalGraphEntriesExits(entries, exits, shift);
	}

	/**
	 * Create graph for octagonal model.
	 * First of all create grid vertices (like in square model), then the in between vertices and finally entries and exits.
	 *
	 * @param entries Number of entries.
	 * @param exits   Number of exits.
	 */
	private void createOctagonalGraph(long entries, long exits) {
		double shift = size / (granularity + 2);

		// create main grid
		createSquareGraphVertices(shift, true);

		createOctagonalGraphInBetweenVertices(shift);

		createSquareGraphEntryVertices(entries, exits, shift, true);
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
				Vertex vertex = new Vertex(id, (i + 1.5) * shift, (j + 1.5) * shift);
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
			vertex.addNeighbourID(id + (i == 0 ? 1 : -1) * (g - (withoutCorners ? 1 : 0)));
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
			Vertex topE = new Vertex(id++, topX, topY, entry ? Type.ENTRY0 : Type.EXIT0),
				bottomE = new Vertex(id++, botX, botY, entry ? Type.ENTRY1 : Type.EXIT1),
				leftE = new Vertex(id++, topY, botX, entry ? Type.ENTRY2 : Type.EXIT2),
				rightE = new Vertex(id++, botY, topX, entry ? Type.ENTRY3 : Type.EXIT3);

			vertices.put(topE.getID(), topE);
			vertices.put(bottomE.getID(), bottomE);
			vertices.put(leftE.getID(), leftE);
			vertices.put(rightE.getID(), rightE);

			// create edges
			Vertex topN = vertices.get((index - 1) * granularity - (withoutCorners ? 2 : 0)),
				bottomN = vertices.get((granularity + 1 - index) * granularity - (withoutCorners ? 3 : 1)),
				leftN = vertices.get(granularity - index - (withoutCorners ? 1 : 0)),
				rightN = vertices.get((granularity - 1) * granularity + index - (withoutCorners ? 4 : 1));
			assert (topN != null && bottomN != null && leftN != null && rightN != null);
			topN.getNeighbourIDs().add(topE.getID());
			bottomN.getNeighbourIDs().add(bottomE.getID());
			leftN.getNeighbourIDs().add(leftE.getID());
			rightN.getNeighbourIDs().add(rightE.getID());

			edges.add(new Edge(topE, topN));
			edges.add(new Edge(bottomE, bottomN));
			edges.add(new Edge(leftE, leftN));
			edges.add(new Edge(rightE, rightN));

			index++;
			topX += shift;
			botX -= shift;
		}
	}

	/**
	 * Create all roads for hexagonal model.
	 *
	 * @param shift Distance between 2 vertices.
	 */
	private void createHexagonalGraphRoadVertices(double shift) {
		double centerX = size / 2,
			centerY = size / 2;

		// create center vertex
		long id = vertices.size();
		Vertex v = new Vertex(id++, centerX, centerY);
		v.addNeighbourID(1L, 2L, 3L, 4L, 5L, 6L);
		vertices.put(0L, v);


		long id0 = id;
		double sin60Shift = shift * Math.sqrt(3) / 2,
			halfShift = shift / 2;

		for (long i = 1; i < granularity; i++, id0 += (i - 1) * 5) {
			// compute initial positions
			double x1 = centerX, y1 = centerY - i * shift,
				x2 = centerX + i * sin60Shift, y2 = centerY - i * halfShift,
				x3 = x2, y3 = centerY + i * shift / 2,
				x4 = centerX, y4 = centerY + i * shift,
				x5 = centerX - i * sin60Shift, y5 = y3,
				x0 = x5, y0 = y2;

			boolean notLast = i < granularity - 1;

			for (long j = 0; j < i; j++, id0++,
				x1 += sin60Shift, y1 += halfShift,
				y2 += shift,
				x3 -= sin60Shift, y3 += halfShift,
				x4 -= sin60Shift, y4 -= halfShift,
				y5 -= shift,
				x0 += sin60Shift, y0 -= halfShift
			) {
				createLayerVertices(i, j, notLast, id0, x0, y0, x1, y1, x2, y2, x3, y3, x4, y4, x5, y5);
			}
		}

		vertices.keySet().forEach(this::addGraphEdges);
	}

	/**
	 * Create single vertices of one layer.
	 *
	 * @param i       Number of layer, counted from 0.
	 * @param j       Number of vertex on hexagon edge. Vertex of hexagon has 0, vertex next to him clockwise has 1, etc. to next hexagon vertex excluded.
	 * @param notLast Indicator if it is the last layer.
	 * @param id0     ID of top left hexagon edge vertex.
	 * @param x0      Coordination X of top left hexagon edge vertex.
	 * @param y0      Coordination Y of top left hexagon edge vertex.
	 * @param x1      Coordination X of top right hexagon edge vertex.
	 * @param y1      Coordination Y of top right hexagon edge vertex.
	 * @param x2      Coordination X of right hexagon edge vertex.
	 * @param y2      Coordination Y of right hexagon edge vertex.
	 * @param x3      Coordination X of bottom right hexagon edge vertex.
	 * @param y3      Coordination Y of bottom right hexagon edge vertex.
	 * @param x4      Coordination X of bottom left hexagon edge vertex.
	 * @param y4      Coordination Y of bottom left hexagon edge vertex.
	 * @param x5      Coordination X of left hexagon edge vertex.
	 * @param y5      Coordination Y of left hexagon edge vertex.
	 */
	private void createLayerVertices(long i, long j, boolean notLast, long id0, double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, double x5, double y5) {
		createHexagonalGraphEdgeVertices(i, j, notLast, id0, x0, y0, 0);
		createHexagonalGraphEdgeVertices(i, j, notLast, id0 + i, x1, y1, 1);
		createHexagonalGraphEdgeVertices(i, j, notLast, id0 + 2 * i, x2, y2, 2);
		createHexagonalGraphEdgeVertices(i, j, notLast, id0 + 3 * i, x3, y3, 3);
		createHexagonalGraphEdgeVertices(i, j, notLast, id0 + 4 * i, x4, y4, 4);
		createHexagonalGraphEdgeVertices(i, j, notLast, id0 + 5 * i, x5, y5, 5);
	}

	/**
	 * Create vertex at desired location and compute and add neighbour IDs.
	 *
	 * @param i       Number of layer, counted from 0.
	 * @param j       Number of vertex on hexagon edge.
	 * @param notLast Indicator if it is the last layer.
	 * @param id      ID of the vertex.
	 * @param x       Coordination X of the vertex.
	 * @param y       Coordination Y of the vertex.
	 * @param side    Number of edge of the hexagon. 0 is top left, then incrementally clockwise.
	 */
	private void createHexagonalGraphEdgeVertices(long i, long j, boolean notLast, long id, double x, double y, int side) {
		// create vertex
		Vertex v = new Vertex(id, x, y);

		// add neighbour IDs
		long previousLayerNeighbour = id - (i - 1) * 6 - side;
		v.addNeighbourID(i == 1 ? 0 : previousLayerNeighbour);
		if (side > 0 || j > 0) {
			v.addNeighbourID(id - 1);
		}
		if (side < 5 || j < i - 1) {
			v.addNeighbourID(id + 1);
		} else {
			v.addNeighbourID(id - (2 * i - 1) * 6 + 1);
		}

		if (notLast) {
			long nextLayerNeighbour = id + i * 6 + side;
			v.addNeighbourID(nextLayerNeighbour, nextLayerNeighbour + 1);
			if (j == 0) {
				v.addNeighbourID(nextLayerNeighbour - 1);
				if (side == 0) {
					v.addNeighbourID(id + (2 * i + 1) * 6 - 1);
				}
			}
		}

		if (j > 0) {
			v.addNeighbourID(previousLayerNeighbour - 1);
		} else if (side == 0) {
			v.addNeighbourID(id + i * 6 - 1);
		}

		// add vertex to vertices map
		Vertex last1 = vertices.put(id, v);
		assert (last1 == null);
	}

	/**
	 * Create entries and exits for hexagonal graph.
	 *
	 * @param entries Number of entries.
	 * @param exits   Number of exits.
	 * @param shift   Distance between 2 vertices.
	 */
	private void createHexagonalGraphEntriesExits(long entries, long exits, double shift) {
		// compute outer empty spaces
		long empty = granularity - entries - exits,
			padding = empty / 3,
			index = empty % 3 == 2 ? ++padding : padding;

		double edgeLength = Math.sqrt(3) * shift / 3;
		long id = vertices.size();

		// draw all of the entries
		double e0x = shift * (granularity + Math.sqrt(3) * (index - granularity + 1) / 2),
			e0y = (granularity + 2 - index - Math.sqrt(3)) * shift / 2,     // top left road center
			e1x = size / 2 + shift * (index * Math.sqrt(3) / 2 + 0.5),
			e1y = shift * (index / 2. + 1.5 - Math.sqrt(3) / 2),                     // top right road center
			e5x = shift * (granularity - Math.sqrt(3) * (granularity - 1) / 2 - 0.5),
			e2y = ((granularity) / 2. + index + 1) * shift;
		createHexagonalGraphEntries(entries, size, shift, index, id, e0x, e0y, e1x, e1y, e5x, e2y, true);

		// skip middle empty space
		long middleEmpty = empty % 3 == 2 ? empty - 2 * padding : padding,
			indexShift = middleEmpty + entries;
		index += indexShift;

		// draw all of the exits
		id = vertices.size();
		e0x += indexShift * Math.sqrt(3) * shift / 2;
		e0y -= indexShift * shift / 2;
		e1x += indexShift * Math.sqrt(3) * shift / 2;
		e1y += indexShift * shift / 2;
		e2y += indexShift * shift;
		createHexagonalGraphEntries(exits, size, shift, index, id, e0x, e0y, e1x, e1y, e5x, e2y, false);
	}

	/**
	 * Create entries / exits for hexagonal graph.
	 *
	 * @param entries Number of entries / exits.
	 * @param height  Height of the space.
	 * @param shift   Distance between 2 vertices.
	 * @param index   Index of the first entries.
	 * @param id      ID of the first entry.
	 * @param e0x     Coordination X of the first top left entry.
	 * @param e0y     Coordination Y of the first top left entry.
	 * @param e1x     Coordination X of the first top right entry.
	 * @param e1y     Coordination Y of the first top right entry.
	 * @param e5x     Coordination X of the first left entry.
	 * @param e2y     Coordination Y of the first right entry.
	 */
	private void createHexagonalGraphEntries(long entries, double height, double shift, long index, long id, double e0x, double e0y, double e1x, double e1y, double e5x, double e2y, boolean entry) {
		while (entries-- > 0) {
			long id1 = id + 1,
				id2 = id + 2,
				id3 = id + 3,
				id4 = id + 4,
				id5 = id + 5;

			Vertex v0 = new Vertex(id, e0x, e0y, entry ? Type.ENTRY0 : Type.EXIT0),                     // top left
				v1 = new Vertex(id1, e1x, e1y, entry ? Type.ENTRY1 : Type.EXIT1),                         // top right
				v2 = new Vertex(id2, height - e5x, e2y, entry ? Type.ENTRY2 : Type.EXIT2),             // right
				v3 = new Vertex(id3, height - e0x, height - e0y, entry ? Type.ENTRY3 : Type.EXIT3), // bottom right
				v4 = new Vertex(id4, height - e1x, height - e1y, entry ? Type.ENTRY4 : Type.EXIT4), // bottom left
				v5 = new Vertex(id5, e5x, height - e2y, entry ? Type.ENTRY5 : Type.EXIT5);             // left
			vertices.put(id, v0);
			vertices.put(id1, v1);
			vertices.put(id2, v2);
			vertices.put(id3, v3);
			vertices.put(id4, v4);
			vertices.put(id5, v5);

			addHexagonalEntriesEdges(index, id);

			id += 6;
			index++;
			e0x += Math.sqrt(3) * shift / 2;
			e0y -= shift / 2;
			e1x += Math.sqrt(3) * shift / 2;
			e1y += shift / 2;
			e2y += shift;
		}
	}

	/**
	 * Add edges to all entries from all sides.
	 *
	 * @param index Index of entries.
	 * @param id    ID of the top left entry.
	 */
	private void addHexagonalEntriesEdges(long index, long id) {
		long start = 6 * (granularity - 1) * (granularity - 2) / 2 + 1 + index;
		addHexagonalEntryEdge(id++, start);
		start += granularity - 1;
		addHexagonalEntryEdge(id++, start);
		start += granularity - 1;
		addHexagonalEntryEdge(id++, start);
		start += granularity - 1;
		addHexagonalEntryEdge(id++, start);
		start += granularity - 1;
		addHexagonalEntryEdge(id++, start);
		start += granularity - 1;
		addHexagonalEntryEdge(id, start);
	}

	/**
	 * Add neighbour to entry / exit vertex and vice versa and then add edge.
	 *
	 * @param id  ID of the entry / exit vertex.
	 * @param idn ID of the neighbour.
	 */
	private void addHexagonalEntryEdge(long id, long idn) {
		vertices.get(id).addNeighbourID(idn);
		vertices.get(idn).addNeighbourID(id);
		addGraphEdges(id);
	}

	/**
	 * Create vertices connecting grid vertices width diagonal.
	 *
	 * @param shift Distance between 2 vertices in main grid.
	 */
	private void createOctagonalGraphInBetweenVertices(double shift) {
		long id = vertices.size();

		// create first column
		for (long j = 0; j < granularity - 1; j++, id++) {
			Vertex v = new Vertex(id, 2 * shift, (j + 2) * shift);

			long rightTopNeighbourID = granularity - 2 + j;
			v.addNeighbourID(rightTopNeighbourID, rightTopNeighbourID + 1);
			vertices.get(rightTopNeighbourID).addNeighbourID(id);
			vertices.get(rightTopNeighbourID + 1).addNeighbourID(id);
			if (j > 0) {
				long leftTopNeighbourID = j - 1;
				v.addNeighbourID(leftTopNeighbourID);
				vertices.get(leftTopNeighbourID).addNeighbourID(id);
			}
			if (j < granularity - 2) {
				long leftTopNeighbourID = j;
				v.addNeighbourID(leftTopNeighbourID);
				vertices.get(leftTopNeighbourID).addNeighbourID(id);
			}

			vertices.put(id, v);
			addGraphEdges(id);
		}

		// create vertices in second, ..., last but one column
		for (long i = 1; i < granularity - 2; i++) {
			for (long j = 0; j < granularity - 1; j++, id++) {
				Vertex v = new Vertex(id, (i + 2) * shift, (j + 2) * shift);

				long leftTopNeighbourID = i * granularity - 2 + j;
				long rightTopNeighbourID = leftTopNeighbourID + granularity;
				v.addNeighbourID(leftTopNeighbourID, leftTopNeighbourID + 1, rightTopNeighbourID, rightTopNeighbourID + 1);
				vertices.get(leftTopNeighbourID).addNeighbourID(id);
				vertices.get(leftTopNeighbourID + 1).addNeighbourID(id);
				vertices.get(rightTopNeighbourID).addNeighbourID(id);
				vertices.get(rightTopNeighbourID + 1).addNeighbourID(id);

				vertices.put(id, v);
				addGraphEdges(id);
			}
		}

		// create last column
		long lastColumnIDStartMinusTwo = (granularity - 1) * granularity - 2,
			lastButOneColumnIDStartMinusTwo = lastColumnIDStartMinusTwo - granularity;
		for (long j = 0; j < granularity - 1; j++, id++) {
			Vertex v = new Vertex(id, granularity * shift, (j + 2) * shift);

			long leftTopNeighbourID = lastButOneColumnIDStartMinusTwo + j;
			v.addNeighbourID(leftTopNeighbourID, leftTopNeighbourID + 1);
			vertices.get(leftTopNeighbourID).addNeighbourID(id);
			vertices.get(leftTopNeighbourID + 1).addNeighbourID(id);
			if (j > 0) {
				long rightTopNeighbourID = lastColumnIDStartMinusTwo + j - 1;
				v.addNeighbourID(rightTopNeighbourID);
				vertices.get(rightTopNeighbourID).addNeighbourID(id);
			}
			if (j < granularity - 2) {
				long rightBottomNeighbourID = lastColumnIDStartMinusTwo + j;
				v.addNeighbourID(rightBottomNeighbourID);
				vertices.get(rightBottomNeighbourID).addNeighbourID(id);
			}

			vertices.put(id, v);
			addGraphEdges(id);
		}
	}

	/**
	 * Create edges to neighbours with lower IDs.
	 *
	 * @param id ID of the vertex.
	 */
	private void addGraphEdges(long id) {
		Vertex vertex = vertices.get(id);
		assert (vertex != null);
		for (Long neighbourID : vertex.getNeighbourIDs()) {
			if (neighbourID < id) {
				Vertex neighbour = vertices.get(neighbourID);
//				if (neighbour == null) {
//					System.out.println(id);
//					for (long id_ : vertex.getNeighbourIDs()) {
//
//						System.out.println("\t" + id_);
//					}
//					System.out.println("That's all");
//				}
				assert (neighbour != null);

//				if (!neighbour.getNeighbourIDs().contains(id)) {
//					System.out.println(id + " - " + neighbourID);
//					for (long id_ : vertex.getNeighbourIDs()) {
//						System.out.println("\t" + id_);
//					}
//					System.out.println("That's all for " + id);
//
//					System.out.println(neighbourID);
//					for (long id_ : neighbour.getNeighbourIDs()) {
//						System.out.println("\t" + id_);
//					}
//					System.out.println("That's all for " + neighbourID);
//				}
				assert (neighbour.getNeighbourIDs().contains(id));

				boolean notContainEdge = edges.add(new Edge(vertex, neighbour));
				assert (notContainEdge);
			}
		}
	}

	/**
	 * Check if the graph is same as another graph.
	 * That means they are same model, have same granularity, size and orientation and same number of entries and exits.
	 *
	 * @param o Compared object.
	 * @return True if the object is graph and has same key features.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SimulationGraph graph = (SimulationGraph) o;
		return oriented == graph.oriented && granularity == graph.granularity && entries == graph.entries && exits == graph.exits && Double.compare(graph.size, size) == 0 && abstractGraph == graph.abstractGraph && model == graph.model;
	}

	@Override
	public int hashCode() {
		return Objects.hash(oriented, granularity, entries, exits, size, model, abstractGraph);
	}


	public boolean isOriented() {
		return oriented;
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

	public long getEntries() {
		return entries;
	}

	public long getExits() {
		return exits;
	}

	public double getSize() {
		return size;
	}

	/**
	 * @return Model Type of the graph.
	 */
	public Parameters.Models getModel() {
		return model;
	}

	public boolean isAbstractGraph() {
		return abstractGraph;
	}
}