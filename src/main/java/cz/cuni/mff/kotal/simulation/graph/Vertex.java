package cz.cuni.mff.kotal.simulation.graph;


import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


public class Vertex {
	private static final Color ROAD_COLOR = Color.LIGHTGREY, 
		ENTRY_COLOR = Color.LIGHTSLATEGRAY,
		EXIT_COLOR = Color.GRAY;

	protected final long id;
	protected final Type type;
	protected final Set<Long> neighbour_ids = new HashSet<>();

	/**
	 * Create vertex with specified ID and type.
	 *
	 * @param id   Desired ID of the vertex.
	 * @param type Desired type of the vertex.
	 */
	public Vertex(long id, Type type, Set<Long> neighbourIDs) {
		this.id = id;
		this.type = type;
		this.getNeighbourIDs().addAll(neighbourIDs);
	}

	/**
	 * Create vertex with specified ID and type.
	 *
	 * @param id   Desired ID of the vertex.
	 * @param type Desired type of the vertex.
	 */
	public Vertex(long id, Type type) {
		this.id = id;
		this.type = type;
	}

	/**
	 * Create vertex with specified ID and road type.
	 *
	 * @param id Desired ID of the vertex.
	 */
	public Vertex(long id) {
		this.id = id;
		this.type = Type.ROAD;
	}

	public boolean addNeighbourID(Long... ids) {
		if (ids.length == 1) {
			return neighbour_ids.add(ids[0]);
		}
		return neighbour_ids.addAll(Arrays.asList(ids));
	}

	/**
	 * Compare vertex to another object.
	 *
	 * @param o Object to compare to.
	 * @return True if the object is vertex and has the same ID.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Vertex vertex = (Vertex) o;
		return id == vertex.id;
	}

	/**
	 * @return Hash generated using ID.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	/**
	 * @return ID of the vertex.
	 */
	public long getID() {
		return id;
	}

	/**
	 * @return Type of the vertex.
	 */
	public Type getType() {
		return type;
	}

	public Set<Long> getNeighbourIDs() {
		return neighbour_ids;
	}

	public enum Type {
		ROAD(ROAD_COLOR),
		ENTRY0(ENTRY_COLOR, true, 0), // top / top left entry
		ENTRY1(ENTRY_COLOR, true, 1), // left / left entry
		ENTRY2(ENTRY_COLOR, true, 2), // bottom / bottom left entry
		ENTRY3(ENTRY_COLOR, true, 3), // right / bottom right entry
		ENTRY4(ENTRY_COLOR, true, 4), // X / right entry
		ENTRY5(ENTRY_COLOR, true, 5), // X / top right entry
		EXIT0(EXIT_COLOR, false, 0), // top / top left exit
		EXIT1(EXIT_COLOR, false, 1), // left / left exit
		EXIT2(EXIT_COLOR, false, 2), // bottom / bottom left exit
		EXIT3(EXIT_COLOR, false, 3), // right / bottom right exit
		EXIT4(EXIT_COLOR, false, 4), // X / right exit
		EXIT5(EXIT_COLOR, false, 5), // X / top right exit
		;

		private final Color color;
		private final boolean entry;
		private final int direction;

		Type(Color color) {
			this.color = color;
			entry = false;
			direction = -1;
		}

		Type(Color color, boolean entry, int direction) {
			this.color = color;
			this.entry = entry;
			this.direction = direction;
		}

		/**
		 * @return Color of the vertex type.
		 */
		public Color getColor() {
			return color;
		}

		/**
		 * @return Direction of the vertex entry / exit type.
		 */
		public int getDirection() {
			return direction;
		}

		public boolean isEntry() {
			return entry;
		}
	}
}