package cz.cuni.mff.kotal.simulation.graph;

import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class extending {@link VertexWithDirection} containing also reference to last vertex.
 */
public class VertexWithDirectionParent extends VertexWithDirection {
	private final @Nullable VertexWithDirectionParent parent;

	/**
	 * Create new object with specified values, zero distance, angle and estimate and null parent.
	 *
	 * @param vertex Vertex this object is linked to
	 */
	public VertexWithDirectionParent(GraphicalVertex vertex) {
		this(vertex, 0);
	}

	/**
	 * Create new object with specified values, zero distance and estimate and null parent.
	 *
	 * @param vertex Vertex this object is linked to
	 * @param angle  Starting angle of a path
	 */
	public VertexWithDirectionParent(GraphicalVertex vertex, double angle) {
		this(vertex, angle, 0);
	}

	/**
	 * Create new object with specified values, zero distance and null parent.
	 *
	 * @param vertex   Vertex this object is linked to
	 * @param angle    Starting angle of a path
	 * @param estimate Estimate to goal
	 */
	public VertexWithDirectionParent(GraphicalVertex vertex, double angle, double estimate) {
		super(vertex, angle, estimate);
		parent = null;
	}

	/**
	 * Create new object according to arguments.
	 * Distance is taken from parent and then add distance between vertices from argument.
	 * This distance is computed from edge and cell distance.
	 * Then compute angle from previous vertex to actual.
	 * Finally, set parent to previous vertex.
	 * <p>
	 * Estimate is set to zero.
	 *
	 * @param previous     Last Vertex with safe distance and direction
	 * @param actual       New vertex on path
	 * @param edge         Edge between those vertices
	 * @param cellDistance Base distance between two vertices defined by graph, edge size is scaled with this value
	 */
	public VertexWithDirectionParent(@NotNull VertexWithDirectionParent previous, GraphicalVertex actual, Edge edge, double cellDistance) {
		this(previous, actual, edge, cellDistance, 0);
	}

	/**
	 * Create new object according to arguments.
	 * Distance is taken from parent and then add distance between vertices from argument.
	 * This distance is computed from edge and cell distance.
	 * Then compute angle from previous vertex to actual.
	 * Finally, set parent to previous vertex.
	 *
	 * @param previous     Last Vertex with safe distance and direction
	 * @param actual       New vertex on path
	 * @param edge         Edge between those vertices
	 * @param cellDistance Base distance between two vertices defined by graph, edge size is scaled with this value
	 * @param estimate     Estimate to goal
	 */
	public VertexWithDirectionParent(@NotNull VertexWithDirectionParent previous, GraphicalVertex actual, Edge edge, double cellDistance, double estimate) {
		super(previous, actual, edge, cellDistance, estimate);
		this.parent = previous;
	}

	/**
	 * Create new object according to arguments.
	 * Distance is taken from parent and then add distance between vertices from argument.
	 * Then compute angle from previous vertex to actual.
	 * Finally, set parent to previous vertex.
	 *
	 * @param previous Last Vertex with safe distance and direction
	 * @param actual   New vertex on path
	 * @param distance Distance between those vertices
	 * @param estimate Estimate to goal
	 */
	public VertexWithDirectionParent(@NotNull VertexWithDirectionParent previous, GraphicalVertex actual, double distance, double estimate) {
		super(previous, actual, distance, estimate);

		this.parent = previous;
	}

	public @Nullable VertexWithDirectionParent getParent() {
		return parent;
	}

	/**
	 * Compare two vertices for A* usage according to their sum of distance and heuristic.
	 * If the sum is equal, compare distance objects.
	 * If this sum is also equal and comparison of the vertex IDs, return vertex which parent vertex ID is equal to current ID.
	 *
	 * @param o the object to be compared
	 * @return Value < 0 if path to this vertex is better than the other
	 */
	@Override
	public int compareTo(@NotNull VertexWithDirection o) {
		final int superComparison = super.compareTo(o);
		if (superComparison == 0 && o instanceof VertexWithDirectionParent op) {
			assert op.parent != null;  // this method should not be called on starting vertex
			assert parent != null;
			boolean parentEquals = parent.getID() == getID();
			boolean oParentEquals = op.parent.getID() == op.getID();
			if (parentEquals ^ oParentEquals) {
				return parentEquals ? -1 : 1;
			}
		}
		return superComparison;
	}
}
