package cz.cuni.mff.kotal.simulation.graph;


import javafx.scene.paint.Color;

import java.util.Objects;


public class Vertex {

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
		ENTRY(Color.LIGHTSLATEGRAY),
		EXIT(Color.GRAY),
		;

		private final Color color;

		Type(Color color) {
			this.color = color;
		}

		public Color getColor() {
			return color;
		}
	}
}