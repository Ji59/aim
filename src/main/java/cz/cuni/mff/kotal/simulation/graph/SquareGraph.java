package cz.cuni.mff.kotal.simulation.graph;


import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters.GraphType;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import org.jetbrains.annotations.NotNull;

import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters.GraphType.SQUARE;


public class SquareGraph extends SimulationGraph {

	/**
	 * Create square graph for simulation.
	 *
	 * @param granularity Granularity of the graph
	 * @param entries     Number of entries from each direction
	 * @param exits       Number of exits from each direction
	 */
	public SquareGraph(int granularity, int entries, int exits) {
		super(SQUARE, granularity, entries, exits, granularity * granularity + 4 * (entries + exits));

		cellSize = 1. / (granularity + 2);

		int id = createSquareGraphVertices(0, cellSize, false);

		createSquareGraphEntryVertices(id, entries, exits, cellSize, false);
	}

	/**
	 * Create graph based on square graph for simulation.
	 *
	 * @param granularity    Granularity of the graph
	 * @param entries        Number of entries from each direction
	 * @param exits          Number of exits from each direction
	 * @param withoutCorners Denotes, if The graph is with corner vertices or not
	 */
	protected SquareGraph(@NotNull GraphType model, int granularity, int entries, int exits, int vertices, double shift, boolean withoutCorners) {
		super(model, granularity, entries, exits, vertices);

		int id = createSquareGraphVertices(0, shift, withoutCorners);

		createSquareGraphEntryVertices(id, entries, exits, shift, withoutCorners);
	}

	/**
	 * Create intersection vertices with edges between them.
	 *
	 * @param shift          Distance between 2 vertices
	 * @param withoutCorners Denotes, if The graph is with corner vertices or not
	 * @return TODO
	 */
	protected int createSquareGraphVertices(int id, double shift, boolean withoutCorners) {
		int g = getGranularity();
		for (int i = 0; i < getGranularity(); i++) {
			for (int j = 0; j < getGranularity(); j++) {
				// check if in corner
				if (withoutCorners &&
					(i == 0 || i == g - 1) &&
					(j == 0 || j == g - 1)
				) {
					continue;
				}

				// create vertex
				@NotNull GraphicalVertex vertex = new GraphicalVertex(id, (i + 1.5) * shift, (j + 1.5) * shift);
				vertices[id] = vertex;

				// add neighbours ids
				createSquareGraphAddNeighbours(vertex, withoutCorners);

				// create edges
				addGraphEdges(id++);
			}
		}

		return id;
	}

	/**
	 * Compute neighbour IDs.
	 *
	 * @param vertex         Vertex whose neighbours are computed
	 * @param withoutCorners Denotes, if The graph is with corner vertices or not
	 */
	private void createSquareGraphAddNeighbours(@NotNull GraphicalVertex vertex, boolean withoutCorners) {
		int g = getGranularity();
		int id = vertex.getID();
		int i = id / getGranularity();
		int j = id % getGranularity();

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
	protected int createSquareGraphEntryVertices(int id, int entries, int exits, double shift, boolean withoutCorners) {
		assert (entries + exits <= getGranularity() - (withoutCorners ? 2 : 0));

		// compute outer empty spaces
		int empty = getGranularity() - entries - exits;
		int padding = empty / 3;
		int index = empty % 3 == 2 ? ++padding + 1 : padding + 1;

		// draw all the entries
		id = createSquareGraphEntries(id, entries, shift, index, withoutCorners, true);
		index += entries;

		// skip middle empty space
		index += empty - 2 * padding;

		// draw all the exits
		id = createSquareGraphEntries(id, exits, shift, index, withoutCorners, false);
		return id;
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
	private int createSquareGraphEntries(int id, int entries, double shift, int index, boolean withoutCorners, boolean entry) {
		double topX = (index + 0.5) * shift;
		double topY = shift * 0.5;
		double botX = (getGranularity() - index + 1.5) * shift;
		double botY = (getGranularity() + 1 + 0.5) * shift;

		for (int i = 0; i < entries; i++) {

			// create vertices
			@NotNull GraphicalVertex topE = new GraphicalVertex(id++, topX, topY, entry ? Vertex.Type.ENTRY0 : Vertex.Type.EXIT0);
			@NotNull GraphicalVertex rightE = new GraphicalVertex(id++, botY, topX, entry ? Vertex.Type.ENTRY1 : Vertex.Type.EXIT1);
			@NotNull GraphicalVertex bottomE = new GraphicalVertex(id++, botX, botY, entry ? Vertex.Type.ENTRY2 : Vertex.Type.EXIT2);
			@NotNull GraphicalVertex leftE = new GraphicalVertex(id++, topY, botX, entry ? Vertex.Type.ENTRY3 : Vertex.Type.EXIT3);

			// add graph vertices
			vertices[topE.getID()] = topE;
			vertices[bottomE.getID()] = bottomE;
			vertices[leftE.getID()] = leftE;
			vertices[rightE.getID()] = rightE;

			entryExitVertices.get(topE.getType().getDirection()).add(topE);
			entryExitVertices.get(rightE.getType().getDirection()).add(rightE);
			entryExitVertices.get(bottomE.getType().getDirection()).add(bottomE);
			entryExitVertices.get(leftE.getType().getDirection()).add(leftE);


			// create edges
			Vertex topN = vertices[(index - 1) * getGranularity() - (withoutCorners ? 2 : 0)];
			Vertex bottomN = vertices[(getGranularity() + 1 - index) * getGranularity() - (withoutCorners ? 3 : 1)];
			Vertex leftN = vertices[getGranularity() - index - (withoutCorners ? 1 : 0)];
			Vertex rightN = vertices[(getGranularity() - 1) * getGranularity() + index - (withoutCorners ? 4 : 1)];
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

		return id;
	}

	/**
	 * @return Model Type of square graph
	 */
	@Override
	public GraphType getModel() {
		return SQUARE;
	}

	@Override
	public double getCellSize() {
		return cellSize;
	}
}
