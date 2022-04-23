package cz.cuni.mff.kotal.simulation.graph;


import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;

import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters.Models.*;


public class SquareGraph extends SimulationGraph {

	/**
	 * Create square graph for simulation.
	 *
	 * @param granularity Granularity of the graph
	 * @param entries     Number of entries from each direction
	 * @param exits       Number of exits from each direction
	 */
	public SquareGraph(int granularity, int entries, int exits) {
		super(SQUARE, granularity, entries, exits);

		cellSize = 1. / (granularity + 2);

		createSquareGraphVertices(cellSize, false);

		createSquareGraphEntryVertices(entries, exits, cellSize, false);
	}

	/**
	 * Create graph based on square graph for simulation.
	 *
	 * @param granularity    Granularity of the graph
	 * @param entries        Number of entries from each direction
	 * @param exits          Number of exits from each direction
	 * @param withoutCorners Denotes, if The graph is with corner vertices or not
	 */
	protected SquareGraph(IntersectionMenuTab0.Parameters.Models model, int granularity, int entries, int exits, double shift, boolean withoutCorners) {
		super(model, granularity, entries, exits);

		createSquareGraphVertices(shift, withoutCorners);

		createSquareGraphEntryVertices(entries, exits, shift, withoutCorners);
	}

	/**
	 * Create intersection vertices with edges between them.
	 *
	 * @param shift          Distance between 2 vertices
	 * @param withoutCorners Denotes, if The graph is with corner vertices or not
	 */
	protected void createSquareGraphVertices(double shift, boolean withoutCorners) {
		int g = granularity;
		int id = vertices.size();
		for (int i = 0; i < granularity; i++) {
			for (int j = 0; j < granularity; j++) {
				// check if in corner
				if (withoutCorners &&
					(i == 0 || i == g - 1) &&
					(j == 0 || j == g - 1)
				) {
					continue;
				}

				// create vertex
				GraphicalVertex vertex = new GraphicalVertex(id, (i + 1.5) * shift, (j + 1.5) * shift);
				vertices.put(id, vertex);

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
	 * @param vertex         Vertex whose neighbours are computed
	 * @param withoutCorners Denotes, if The graph is with corner vertices or not
	 */
	private void createSquareGraphAddNeighbours(GraphicalVertex vertex, boolean withoutCorners) {
		int g = granularity;
		int id = vertex.getID();
		int i = id / granularity;
		int j = id % granularity;

		if (withoutCorners) {
			int idOffset;
			if (id < g - 2) {
				idOffset = 1;
			} else {
				if (id >= g * (g - 1) - 2) idOffset = 3;
				else idOffset = 2;
			}
			i = (id + idOffset) / g;
			j = (j + idOffset) % g;
		}
		if (i == 0 || i == g - 1) {
			vertex.addNeighbourID(id + (i == 0 ? 1 : -1) * (g - (withoutCorners ? 1 : 0)));
			if (j > (withoutCorners ? 1 : 0)) {
				vertex.addNeighbourID(id - 1);
			}
			if (j < (withoutCorners ? g - 2 : g - 1)) {
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
	 * @param entries        Number of entries
	 * @param exits          Number of exits
	 * @param shift          distance between 2 neighbour vertices
	 * @param withoutCorners Denotes, if The graph is with corner vertices or not
	 */
	protected void createSquareGraphEntryVertices(int entries, int exits, double shift, boolean withoutCorners) {
		assert (entries + exits <= granularity - (withoutCorners ? 2 : 0));

		// compute outer empty spaces
		int empty = granularity - entries - exits;
		int padding = empty / 3;
		int index = empty % 3 == 2 ? ++padding + 1 : padding + 1;

		// draw all the entries
		createSquareGraphEntries(entries, shift, index, withoutCorners, true);
		index += entries;

		// skip middle empty space
		index += empty - 2 * padding;

		// draw all the exits
		createSquareGraphEntries(exits, shift, index, withoutCorners, false);
	}


	/**
	 * Create entries / exits from all sides at specified location.
	 *
	 * @param entries        Number of entries on one side
	 * @param shift          Distance between 2 vertices
	 * @param index          Number of square from side where entries should be created from
	 * @param withoutCorners Denotes, if The graph is with corner vertices or not
	 * @param entry          Define if creating entries or exits
	 */
	private void createSquareGraphEntries(int entries, double shift, int index, boolean withoutCorners, boolean entry) {
		double topX = (index + 0.5) * shift;
		double topY = shift * 0.5;
		double botX = (granularity - index + 1.5) * shift;
		double botY = (granularity + 1 + 0.5) * shift;

		// find first unused id
		int id = vertices.size();

		for (int i = 0; i < entries; i++) {

			// create vertices
			GraphicalVertex topE = new GraphicalVertex(id++, topX, topY, entry ? Vertex.Type.ENTRY0 : Vertex.Type.EXIT0);
			GraphicalVertex rightE = new GraphicalVertex(id++, botY, topX, entry ? Vertex.Type.ENTRY1 : Vertex.Type.EXIT1);
			GraphicalVertex bottomE = new GraphicalVertex(id++, botX, botY, entry ? Vertex.Type.ENTRY2 : Vertex.Type.EXIT2);
			GraphicalVertex leftE = new GraphicalVertex(id++, topY, botX, entry ? Vertex.Type.ENTRY3 : Vertex.Type.EXIT3);

			// add graph vertices
			vertices.put(topE.getID(), topE);
			vertices.put(bottomE.getID(), bottomE);
			vertices.put(leftE.getID(), leftE);
			vertices.put(rightE.getID(), rightE);

			entryExitVertices.get(topE.getType().getDirection()).add(topE);
			entryExitVertices.get(rightE.getType().getDirection()).add(rightE);
			entryExitVertices.get(bottomE.getType().getDirection()).add(bottomE);
			entryExitVertices.get(leftE.getType().getDirection()).add(leftE);


			// create edges
			cz.cuni.mff.kotal.simulation.graph.Vertex topN = vertices.get((index - 1) * granularity - (withoutCorners ? 2 : 0));
			cz.cuni.mff.kotal.simulation.graph.Vertex bottomN = vertices.get((granularity + 1 - index) * granularity - (withoutCorners ? 3 : 1));
			cz.cuni.mff.kotal.simulation.graph.Vertex leftN = vertices.get(granularity - index - (withoutCorners ? 1 : 0));
			cz.cuni.mff.kotal.simulation.graph.Vertex rightN = vertices.get((granularity - 1) * granularity + index - (withoutCorners ? 4 : 1));
			assert (topN != null && bottomN != null && leftN != null && rightN != null);
			if (entry) {
				topE.getNeighbourIDs().add(topN.getID());
				bottomE.getNeighbourIDs().add(bottomN.getID());
				leftE.getNeighbourIDs().add(leftN.getID());
				rightE.getNeighbourIDs().add(rightN.getID());

				edges.add(new Edge(topE, topN));
				edges.add(new Edge(bottomE, bottomN));
				edges.add(new Edge(leftE, leftN));
				edges.add(new Edge(rightE, rightN));
			} else {
				topN.getNeighbourIDs().add(topE.getID());
				bottomN.getNeighbourIDs().add(bottomE.getID());
				leftN.getNeighbourIDs().add(leftE.getID());
				rightN.getNeighbourIDs().add(rightE.getID());

				edges.add(new Edge(topN, topE));
				edges.add(new Edge(bottomN, bottomE));
				edges.add(new Edge(leftN, leftE));
				edges.add(new Edge(rightN, rightE));
			}

			index++;
			topX += shift;
			botX -= shift;
		}
	}

	/**
	 * @return Model Type of square graph
	 */
	@Override
	public IntersectionMenuTab0.Parameters.Models getModel() {
		return SQUARE;
	}

	@Override
	public double getCellSize() {
		return cellSize;
	}
}
