package cz.cuni.mff.kotal.simulation.graph;


import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import cz.cuni.mff.kotal.simulation.graph.Vertex.Type;

import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters.Models.HEXAGONAL;


public class HexagonalGraph extends SimulationGraph {

	/**
	 * Create graph for hexagonal model.
	 * First crate center vertex, then all the other from inside to outside.
	 *
	 * @param granularity Granularity of the graph
	 * @param entries     Number of entries from each direction
	 * @param exits       Number of exits from each direction
	 */
	public HexagonalGraph(int granularity, int entries, int exits) {
		super(HEXAGONAL, granularity, entries, exits, (granularity * (granularity - 1) / 2 + entries + exits) * 6 + 1);

		cellSize = 1. / (2 * granularity + 1);

		int id = createHexagonalGraphRoadVertices(0, cellSize);

		createHexagonalGraphEntriesExits(entries, exits, id, cellSize);
	}

	/**
	 * Create all roads for hexagonal model.
	 *
	 * @param shift Distance between 2 vertices
	 */
	private int createHexagonalGraphRoadVertices(int id, double shift) {
		double centerX = 0.5;
		double centerY = 0.5;

		// create center vertex
		GraphicalVertex v = new GraphicalVertex(id++, centerX, centerY);
		v.addNeighbourID(1, 2, 3, 4, 5, 6);
		vertices[0] = v;


		double sin60Shift = shift * Math.sqrt(3) / 2;
		double halfShift = shift / 2;

		for (int i = 1; i < granularity; i++, id += (i - 1) * 5) {
			// compute initial positions
			double x1 = centerX;
			double y1 = centerY - i * shift;
			double x2 = centerX + i * sin60Shift;
			double y2 = centerY - i * halfShift;
			double x3 = x2;
			double y3 = centerY + i * shift / 2;
			double x4 = centerX;
			double y4 = centerY + i * shift;
			double x5 = centerX - i * sin60Shift;
			double y5 = y3;
			double x0 = x5;
			double y0 = y2;

			boolean notLast = i < granularity - 1;

			for (int j = 0; j < i; j++, id++,
				x1 += sin60Shift, y1 += halfShift,
				y2 += shift,
				x3 -= sin60Shift, y3 += halfShift,
				x4 -= sin60Shift, y4 -= halfShift,
				y5 -= shift,
				x0 += sin60Shift, y0 -= halfShift
			) {
				createLayerVertices(i, j, notLast, id, x0, y0, x1, y1, x2, y2, x3, y3, x4, y4, x5, y5);
			}
		}

		for (int i = 0; i < id; i++) {
			addGraphEdges(i);
		}
//		vertices.keySet().forEach(this::addGraphEdges);  TODO remove
		return id;
	}

	/**
	 * Create single vertices of one layer.
	 *
	 * @param i       Number of layer, counted from 0
	 * @param j       Number of vertex on hexagon edge. Vertex of hexagon has 0, vertex next to him clockwise has 1, etc. to next hexagon vertex excluded
	 * @param notLast Indicator if it is the last layer
	 * @param id      ID of top left hexagon edge vertex
	 * @param x0      Coordinate X of top left hexagon edge vertex
	 * @param y0      Coordinate Y of top left hexagon edge vertex
	 * @param x1      Coordinate X of top right hexagon edge vertex
	 * @param y1      Coordinate Y of top right hexagon edge vertex
	 * @param x2      Coordinate X of right hexagon edge vertex
	 * @param y2      Coordinate Y of right hexagon edge vertex
	 * @param x3      Coordinate X of bottom right hexagon edge vertex
	 * @param y3      Coordinate Y of bottom right hexagon edge vertex
	 * @param x4      Coordinate X of bottom left hexagon edge vertex
	 * @param y4      Coordinate Y of bottom left hexagon edge vertex
	 * @param x5      Coordinate X of left hexagon edge vertex
	 * @param y5      Coordinate Y of left hexagon edge vertex
	 */
	private void createLayerVertices(int i, int j, boolean notLast, int id, double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, double x5, double y5) {
		createHexagonalGraphEdgeVertices(i, j, notLast, id, x0, y0, 0);
		createHexagonalGraphEdgeVertices(i, j, notLast, id + i, x1, y1, 1);
		createHexagonalGraphEdgeVertices(i, j, notLast, id + 2 * i, x2, y2, 2);
		createHexagonalGraphEdgeVertices(i, j, notLast, id + 3 * i, x3, y3, 3);
		createHexagonalGraphEdgeVertices(i, j, notLast, id + 4 * i, x4, y4, 4);
		createHexagonalGraphEdgeVertices(i, j, notLast, id + 5 * i, x5, y5, 5);
	}

	/**
	 * Create vertex at desired location and compute and add neighbour IDs.
	 *
	 * @param i       Number of layer, counted from 0
	 * @param j       Number of vertex on hexagon edge
	 * @param notLast Indicator if it is the last layer
	 * @param id      ID of the vertex
	 * @param x       Coordinate X of the vertex
	 * @param y       Coordinate Y of the vertex
	 * @param side    Number of edge of the hexagon. 0 is top left, then incrementally clockwise
	 */
	private void createHexagonalGraphEdgeVertices(int i, int j, boolean notLast, int id, double x, double y, int side) {
		// create vertex
		GraphicalVertex v = new GraphicalVertex(id, x, y);

		// add neighbour IDs
		int previousLayerNeighbour = id - (i - 1) * 6 - side;
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
			int nextLayerNeighbour = id + i * 6 + side;
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

		// add vertex to vertices
		assert vertices[id] == null;
		vertices[id] = v;
	}

	/**
	 * Create entries and exits for hexagonal graph.
	 *
	 * @param entries Number of entries
	 * @param exits   Number of exits
	 * @param shift   Distance between 2 vertices
	 */
	private void createHexagonalGraphEntriesExits(int entries, int exits, int id, double shift) {
		// compute outer empty spaces
		int empty = granularity - entries - exits;
		int padding = empty / 3;
		int index = empty % 3 == 2 ? ++padding : padding;

		// draw all the entries
		double e0x = shift * (granularity + Math.sqrt(3) * (index - granularity + 1) / 2);
		double e0y = (granularity + 2 - index - Math.sqrt(3)) * shift / 2; // top left road center
		double e1x = 1. / 2 + shift * (index * Math.sqrt(3) / 2 + 0.5);
		double e1y = shift * (index / 2. + 1.5 - Math.sqrt(3) / 2);
		double e5x = shift * (granularity - Math.sqrt(3) * (granularity - 1) / 2 - 0.5); // top right road center
		double e2y = ((granularity) / 2. + index + 1) * shift;
		id = createHexagonalGraphEntries(entries, shift, index, id, e0x, e0y, e1x, e1y, e5x, e2y, true);

		// skip middle empty space
		int middleEmpty = empty % 3 == 2 ? empty - 2 * padding : padding;
		int indexShift = middleEmpty + entries;
		index += indexShift;

		// draw all the exits
		e0x += indexShift * Math.sqrt(3) * shift / 2;
		e0y -= indexShift * shift / 2;
		e1x += indexShift * Math.sqrt(3) * shift / 2;
		e1y += indexShift * shift / 2;
		e2y += indexShift * shift;
		createHexagonalGraphEntries(exits, shift, index, id, e0x, e0y, e1x, e1y, e5x, e2y, false);
	}

	/**
	 * Create entries / exits for hexagonal graph.
	 *
	 * @param entries Number of entries / exits
	 * @param shift   Distance between 2 vertices
	 * @param index   Index of the first entries
	 * @param id      ID of the first entry
	 * @param e0x     Coordinate X of the first top left entry
	 * @param e0y     Coordinate Y of the first top left entry
	 * @param e1x     Coordinate X of the first top right entry
	 * @param e1y     Coordinate Y of the first top right entry
	 * @param e5x     Coordinate X of the first left entry
	 * @param e2y     Coordinate Y of the first right entry
	 */
	private int createHexagonalGraphEntries(int entries, double shift, int index, int id, double e0x, double e0y, double e1x, double e1y, double e5x, double e2y, boolean entry) {
		while (entries-- > 0) {
			int id1 = id + 1;
			int id2 = id + 2;
			int id3 = id + 3;
			int id4 = id + 4;
			int id5 = id + 5;

			GraphicalVertex v0 = new GraphicalVertex(id, e0x, e0y, entry ? Type.ENTRY0 : Type.EXIT0);                // top left
			GraphicalVertex v1 = new GraphicalVertex(id1, e1x, e1y, entry ? Type.ENTRY1 : Type.EXIT1);               // top right
			GraphicalVertex v2 = new GraphicalVertex(id2, 1 - e5x, e2y, entry ? Type.ENTRY2 : Type.EXIT2);        // right
			GraphicalVertex v3 = new GraphicalVertex(id3, 1 - e0x, 1 - e0y, entry ? Type.ENTRY3 : Type.EXIT3); // bottom right
			GraphicalVertex v4 = new GraphicalVertex(id4, 1 - e1x, 1 - e1y, entry ? Type.ENTRY4 : Type.EXIT4); // bottom left
			GraphicalVertex v5 = new GraphicalVertex(id5, e5x, 1 - e2y, entry ? Type.ENTRY5 : Type.EXIT5);        // left
			vertices[id] = v0;
			vertices[id1] = v1;
			vertices[id2] = v2;
			vertices[id3] = v3;
			vertices[id4] = v4;
			vertices[id5] = v5;

			entryExitVertices.get(0).add(v0);
			entryExitVertices.get(1).add(v1);
			entryExitVertices.get(2).add(v2);
			entryExitVertices.get(3).add(v3);
			entryExitVertices.get(4).add(v4);
			entryExitVertices.get(5).add(v5);


			addHexagonalEntriesEdges(index, id, entry);

			id += 6;
			index++;
			e0x += Math.sqrt(3) * shift / 2;
			e0y -= shift / 2;
			e1x += Math.sqrt(3) * shift / 2;
			e1y += shift / 2;
			e2y += shift;
		}
		return id;
	}

	/**
	 * Add edges to all entries from all sides.
	 *
	 * @param index Index of entries
	 * @param id    ID of the top left entry
	 */
	private void addHexagonalEntriesEdges(int index, int id, boolean entry) {
		int start = 6 * (granularity - 1) * (granularity - 2) / 2 + 1 + index;
		addHexagonalEntryEdge(id++, start, entry);
		start += granularity - 1;
		addHexagonalEntryEdge(id++, start, entry);
		start += granularity - 1;
		addHexagonalEntryEdge(id++, start, entry);
		start += granularity - 1;
		addHexagonalEntryEdge(id++, start, entry);
		start += granularity - 1;
		addHexagonalEntryEdge(id++, start, entry);
		start += granularity - 1;
		addHexagonalEntryEdge(id, start, entry);
	}

	/**
	 * Add neighbour to entry / exit vertex and vice versa and then add edge.
	 *
	 * @param id  ID of the entry / exit vertex
	 * @param idn ID of the neighbour
	 */
	private void addHexagonalEntryEdge(int id, int idn, boolean entry) {
		Vertex v = vertices[id];
		Vertex vn = vertices[idn];
		Vertex from = entry ? v : vn;
		Vertex to = entry ? vn : v;

		from.addNeighbourID(entry ? idn : id);
		edges.add(new Edge(from, to));
	}


	/**
	 * @return Model Type of hexagonal graph
	 */
	@Override
	public IntersectionMenuTab0.Parameters.Models getModel() {
		return HEXAGONAL;
	}

	@Override
	public double getCellSize() {
		return cellSize;
	}
}
