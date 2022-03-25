package cz.cuni.mff.kotal.simulation.graph;


import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;

import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters.Models.*;


public class HexagonalGraph extends SimulationGraph {

	/**
	 * Create graph for hexagonal model.
	 * First crate center vertex, then all the other from inside to outside.
	 *
	 * @param granularity Granularity of the graph
	 * @param entries     Number of entries from each direction
	 * @param exits       Number of exits from each direction
	 */
	public HexagonalGraph(long granularity, long entries, long exits) {
		super(HEXAGONAL, granularity, entries, exits);

		cellSize = 1. / (2 * granularity + 1);

		createHexagonalGraphRoadVertices(cellSize);

		createHexagonalGraphEntriesExits(entries, exits, cellSize);
	}

	/**
	 * Create all roads for hexagonal model.
	 *
	 * @param shift Distance between 2 vertices
	 */
	private void createHexagonalGraphRoadVertices(double shift) {
		double centerX = 0.5;
		double centerY = 0.5;

		// create center vertex
		long id = vertices.size();
		GraphicalVertex v = new GraphicalVertex(id++, centerX, centerY);
		v.addNeighbourID(1L, 2L, 3L, 4L, 5L, 6L);
		vertices.put(0L, v);


		long id0 = id;
		double sin60Shift = shift * Math.sqrt(3) / 2;
		double halfShift = shift / 2;

		for (long i = 1; i < granularity; i++, id0 += (i - 1) * 5) {
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
	 * @param i       Number of layer, counted from 0
	 * @param j       Number of vertex on hexagon edge. Vertex of hexagon has 0, vertex next to him clockwise has 1, etc. to next hexagon vertex excluded
	 * @param notLast Indicator if it is the last layer
	 * @param id0     ID of top left hexagon edge vertex
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
	 * @param i       Number of layer, counted from 0
	 * @param j       Number of vertex on hexagon edge
	 * @param notLast Indicator if it is the last layer
	 * @param id      ID of the vertex
	 * @param x       Coordinate X of the vertex
	 * @param y       Coordinate Y of the vertex
	 * @param side    Number of edge of the hexagon. 0 is top left, then incrementally clockwise
	 */
	private void createHexagonalGraphEdgeVertices(long i, long j, boolean notLast, long id, double x, double y, int side) {
		// create vertex
		GraphicalVertex v = new GraphicalVertex(id, x, y);

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
		cz.cuni.mff.kotal.simulation.graph.Vertex last1 = vertices.put(id, v);
		assert (last1 == null);
	}

	/**
	 * Create entries and exits for hexagonal graph.
	 *
	 * @param entries Number of entries
	 * @param exits   Number of exits
	 * @param shift   Distance between 2 vertices
	 */
	private void createHexagonalGraphEntriesExits(long entries, long exits, double shift) {
		// compute outer empty spaces
		long empty = granularity - entries - exits;
		long padding = empty / 3;
		long index = empty % 3 == 2 ? ++padding : padding;

		long id = vertices.size();

		// draw all the entries
		double e0x = shift * (granularity + Math.sqrt(3) * (index - granularity + 1) / 2);
		double e0y = (granularity + 2 - index - Math.sqrt(3)) * shift / 2; // top left road center
		double e1x = 1. / 2 + shift * (index * Math.sqrt(3) / 2 + 0.5);
		double e1y = shift * (index / 2. + 1.5 - Math.sqrt(3) / 2);
		double e5x = shift * (granularity - Math.sqrt(3) * (granularity - 1) / 2 - 0.5); // top right road center
		double e2y = ((granularity) / 2. + index + 1) * shift;
		createHexagonalGraphEntries(entries, shift, index, id, e0x, e0y, e1x, e1y, e5x, e2y, true);

		// skip middle empty space
		long middleEmpty = empty % 3 == 2 ? empty - 2 * padding : padding;
		long indexShift = middleEmpty + entries;
		index += indexShift;

		// draw all the exits
		id = vertices.size();
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
	private void createHexagonalGraphEntries(long entries, double shift, long index, long id, double e0x, double e0y, double e1x, double e1y, double e5x, double e2y, boolean entry) {
		while (entries-- > 0) {
			long id1 = id + 1;
			long id2 = id + 2;
			long id3 = id + 3;
			long id4 = id + 4;
			long id5 = id + 5;

			GraphicalVertex v0 = new GraphicalVertex(id, e0x, e0y, entry ? Vertex.Type.ENTRY0 : Vertex.Type.EXIT0);                // top left
			GraphicalVertex v1 = new GraphicalVertex(id1, e1x, e1y, entry ? Vertex.Type.ENTRY1 : Vertex.Type.EXIT1);               // top right
			GraphicalVertex v2 = new GraphicalVertex(id2, 1 - e5x, e2y, entry ? Vertex.Type.ENTRY2 : Vertex.Type.EXIT2);        // right
			GraphicalVertex v3 = new GraphicalVertex(id3, 1 - e0x, 1 - e0y, entry ? Vertex.Type.ENTRY3 : Vertex.Type.EXIT3); // bottom right
			GraphicalVertex v4 = new GraphicalVertex(id4, 1 - e1x, 1 - e1y, entry ? Vertex.Type.ENTRY4 : Vertex.Type.EXIT4); // bottom left
			GraphicalVertex v5 = new GraphicalVertex(id5, e5x, 1 - e2y, entry ? Vertex.Type.ENTRY5 : Vertex.Type.EXIT5);        // left
			vertices.put(id, v0);
			vertices.put(id1, v1);
			vertices.put(id2, v2);
			vertices.put(id3, v3);
			vertices.put(id4, v4);
			vertices.put(id5, v5);

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
	}

	/**
	 * Add edges to all entries from all sides.
	 *
	 * @param index Index of entries
	 * @param id    ID of the top left entry
	 */
	private void addHexagonalEntriesEdges(long index, long id, boolean entry) {
		long start = 6 * (granularity - 1) * (granularity - 2) / 2 + 1 + index;
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
	private void addHexagonalEntryEdge(long id, long idn, boolean entry) {
		Vertex v = vertices.get(id);
		Vertex vn = vertices.get(idn);
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