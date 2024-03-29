package cz.cuni.mff.kotal.simulation.graph;


import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * Class representing graph vertex.
 */
public class Vertex {
	private static final Color ROAD_COLOR = Color.color(0.75, 0.75, 0.75);
	private static final Color ENTRY_COLOR = Color.color(0.875, 0.75, 0.625);
	private static final Color EXIT_COLOR = Color.color(0.625, 0.75, 0.875);

	protected final int id;
	protected final Type type;
	protected final Set<Integer> neighbourIds = new HashSet<>();

	/**
	 * Create vertex with specified ID and type.
	 *
	 * @param id   Desired ID of the vertex
	 * @param type Desired type of the vertex
	 */
	public Vertex(int id, Type type, @NotNull Set<Integer> neighbourIDs) {
		this.id = id;
		this.type = type;
		this.getNeighbourIDs().addAll(neighbourIDs);
	}

	/**
	 * Create vertex with specified ID and type.
	 *
	 * @param id   Desired ID of the vertex
	 * @param type Desired type of the vertex
	 */
	public Vertex(int id, Type type) {
		this.id = id;
		this.type = type;
	}

	/**
	 * Create vertex with specified ID and road type.
	 *
	 * @param id Desired ID of the vertex
	 */
	public Vertex(int id) {
		this.id = id;
		this.type = Type.ROAD;
	}

	public Vertex(@NotNull Vertex vertex) {
		this(vertex, true);
	}

	public Vertex(@NotNull Vertex vertex, boolean copyNeighbours) {
		this.id = vertex.getID();
		this.type = vertex.getType();
		if (copyNeighbours) {
			this.neighbourIds.addAll(vertex.getNeighbourIDs());
		}
	}

	/**
	 * Add IDs to neighbours set.
	 *
	 * @param ids IDs of neighbour vertices
	 * @return True if neighbour IDs set changed
	 */
	public boolean addNeighbourID(Integer @NotNull ... ids) {
		if (ids.length == 1) {
			return neighbourIds.add(ids[0]);
		}
		return neighbourIds.addAll(Arrays.asList(ids));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vertex vertex)) return false;
		return id == vertex.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	/**
	 * @return ID of the vertex
	 */
	public int getID() {
		return id;
	}

	/**
	 * @return Type of the vertex
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @return IDs of neighbours
	 */
	public @NotNull Set<Integer> getNeighbourIDs() {
		return neighbourIds;
	}

	/**
	 * Enum of vertex types.
	 */
	public enum Type {
		ROAD(ROAD_COLOR),
		// TODO check if comments are right
		ENTRY0(ENTRY_COLOR, true, 0), // top / top left entry
		ENTRY1(ENTRY_COLOR, true, 1), // right / top right entry
		ENTRY2(ENTRY_COLOR, true, 2), // bottom / right entry
		ENTRY3(ENTRY_COLOR, true, 3), // left / bottom right entry
		ENTRY4(ENTRY_COLOR, true, 4), // X / bottom left entry
		ENTRY5(ENTRY_COLOR, true, 5), // X / left entry
		EXIT0(EXIT_COLOR, false, 0), // top / top left exit
		EXIT1(EXIT_COLOR, false, 1), // right / top right exit
		EXIT2(EXIT_COLOR, false, 2), // bottom / right exit
		EXIT3(EXIT_COLOR, false, 3), // left / bottom right exit
		EXIT4(EXIT_COLOR, false, 4), // X / bottom left exit
		EXIT5(EXIT_COLOR, false, 5), // X / left exit
		;

		private final Color color;
		private final boolean entry;
		private final boolean exit;
		private final int direction;

		Type(Color color) {
			this.color = color;
			entry = false;
			exit = false;
			direction = -1;
		}

		Type(Color color, boolean entry, int direction) {
			this.color = color;
			this.entry = entry;
			this.exit = !entry;
			this.direction = direction;
		}

		/**
		 * @return Color of the vertex type
		 */
		public Color getColor() {
			return color;
		}


		/**
		 * TODO
		 *
		 * @return
		 */
		public double getBaseColorGreenValue() {
			return color.getGreen();
		}

		/**
		 * @return Direction of the vertex entry / exit type
		 */
		public int getDirection() {
			return direction;
		}

		/**
		 * @return True if this vertex is entry one, otherwise False
		 */
		public boolean isEntry() {
			return entry;
		}

		/**
		 * @return True if this vertex is exit one, otherwise False
		 */
		public boolean isExit() {
			return exit;
		}
	}
}