package cz.cuni.mff.kotal.simulation.graph;

import cz.cuni.mff.kotal.frontend.simulation.GraphicalVertex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VertexWithDirectionParent extends VertexWithDirection {
	private final @Nullable VertexWithDirectionParent parent;

	public VertexWithDirectionParent(GraphicalVertex vertex) {
		this(vertex, 0);
	}

	public VertexWithDirectionParent(GraphicalVertex vertex, double angle) {
		this(vertex, angle, 0);
	}

	public VertexWithDirectionParent(GraphicalVertex vertex, double angle, double estimate) {
		super(vertex, angle, estimate);
		parent = null;
	}

	public VertexWithDirectionParent(@NotNull VertexWithDirectionParent previous, GraphicalVertex actual, Edge edge, double cellDistance) {
		this(previous, actual, edge, cellDistance, 0);
	}

	// TODO refactor
	public VertexWithDirectionParent(@NotNull VertexWithDirectionParent previous, GraphicalVertex actual, Edge edge, double cellDistance, double estimate) {
		super(previous, actual, edge, cellDistance, estimate);
		this.parent = previous;
	}

	public VertexWithDirectionParent(@NotNull VertexWithDirectionParent previous, GraphicalVertex actual, double distance, double estimate) {
		super(previous, actual, distance, estimate);

		this.parent = previous;
	}

	public VertexWithDirectionParent(@NotNull VertexWithDirectionParent previous, GraphicalVertex actual, double cellDistance) {
		this(previous, actual, null, cellDistance, 0);
	}

	public VertexWithDirectionParent getParent() {
		return parent;
	}

	/**
	 * @param o the object to be compared.
	 * @return
	 */
	@Override
	public int compareTo(@NotNull VertexWithDirection o) {
		final int superComparison = super.compareTo(o);
		if (superComparison == 0 && o instanceof VertexWithDirectionParent op) {
			assert op.parent != null;
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
