package cz.cuni.mff.kotal.simulation.graph;


import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;

import static cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0.Parameters.Models.OCTAGONAL;


public class OctagonalGraph extends SquareGraph {

	/**
	 * Create octagonal graph for simulation.
	 *
	 * @param granularity Granularity of the graph
	 * @param entries     Number of entries from each direction
	 * @param exits       Number of exits from each direction
	 */
	public OctagonalGraph(long granularity, long entries, long exits) {
		super(OCTAGONAL, granularity, entries, exits, 1. / (granularity + 2), true);
		cellSize = 1. / (granularity + 2);

		createOctagonalGraphInBetweenVertices(cellSize);

	}

	/**
	 * Create vertices connecting grid vertices width diagonal.
	 *
	 * @param shift Distance between 2 vertices in main grid
	 */
	protected void createOctagonalGraphInBetweenVertices(double shift) {
		long id = vertices.size();

		// create first column
		for (long j = 0; j < granularity - 1; j++, id++) {
			GraphicalVertex v = new GraphicalVertex(id, 2 * shift, (j + 2) * shift);

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
				GraphicalVertex v = new GraphicalVertex(id, (i + 2) * shift, (j + 2) * shift);

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
		long lastColumnIDStartMinusTwo = (granularity - 1) * granularity - 2;
		long lastButOneColumnIDStartMinusTwo = lastColumnIDStartMinusTwo - granularity;
		for (long j = 0; j < granularity - 1; j++, id++) {
			GraphicalVertex v = new GraphicalVertex(id, granularity * shift, (j + 2) * shift);

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

	@Override
	public double getCellSize() {
		return cellSize;
	}

	/**
	 * @return Model Type of octagonal graph
	 */
	@Override
	public IntersectionMenuTab0.Parameters.Models getModel() {
		return OCTAGONAL;
	}
}