package cz.cuni.mff.kotal.simulation.graph;


import javafx.scene.paint.Color;

import java.util.Objects;


public class Vertex {
	private static final Color ENTRY_COLOR = Color.LIGHTSLATEGRAY;
	private static final Color EXIT_COLOR = Color.GRAY;

	private final long id;
	private final Type type;

	/**
	 * Create vertex with special ID.
	 *
	 * @param id Desired ID of the vertex.
	 */
	public Vertex(long id, Type type) {
		this.id = id;
		this.type = type;
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

	public enum Type {
		ROAD(Color.LIGHTGREY),
		ENTRY0(ENTRY_COLOR, 0), // top / top left entry
		ENTRY1(ENTRY_COLOR, 1), // left / left entry
		ENTRY2(ENTRY_COLOR, 2), // bottom / bottom left entry
		ENTRY3(ENTRY_COLOR, 3), // right / bottom right entry
		ENTRY4(ENTRY_COLOR, 4), // X / right entry
		ENTRY5(ENTRY_COLOR, 5), // X / top right entry
		EXIT0(EXIT_COLOR, 0), // top / top left exit
		EXIT1(EXIT_COLOR, 1), // left / left exit
		EXIT2(EXIT_COLOR, 2), // bottom / bottom left exit
		EXIT3(EXIT_COLOR, 3), // right / bottom right exit
		EXIT4(EXIT_COLOR, 4), // X / right exit
		EXIT5(EXIT_COLOR, 5), // X / top right exit
		;

		private final Color color;
		private final int direction;

		Type(Color color) {
			this.color = color;
			direction = -1;
		}

		Type(Color color, int direction) {
			this.color = color;
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
	}
}