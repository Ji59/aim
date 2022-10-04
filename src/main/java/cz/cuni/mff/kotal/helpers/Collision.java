package cz.cuni.mff.kotal.helpers;


import org.jetbrains.annotations.NotNull;

import java.util.Objects;


/**
 * Class for collision records.
 */
public class Collision {
	private final long id0;
	private final long id1;
	private double time;

	/**
	 * Create new collision between two vertices.
	 *
	 * @param id0 ID of the first agent
	 * @param id1 ID of the second agent
	 */
	public Collision(long id0, long id1) {
		this.id0 = id0;
		this.id1 = id1;
	}

	/**
	 * Create new collision between two vertices.
	 *
	 * @param id0  ID of the first agent
	 * @param id1  ID of the second agent
	 * @param time Step time of collision
	 */
	public Collision(long id0, long id1, double time) {
		if (id0 < id1) {
			this.id0 = id0;
			this.id1 = id1;
		} else {
			this.id0 = id1;
			this.id1 = id0;
		}
		this.time = time;
	}

	/**
	 * @return ID of the first agent
	 */
	public long getId0() {
		return id0;
	}

	/**
	 * @return ID of the second agent
	 */
	public long getId1() {
		return id1;
	}

	/**
	 * @return Time of collision in steps
	 */
	public double getTime() {
		return time;
	}

	/**
	 * Set time of collision.
	 *
	 * @param time Time of collision in steps
	 * @return This Collision object
	 */
	public @NotNull Collision setTime(double time) {
		this.time = time;
		return this;
	}

	/**
	 * Two collisions are equal if they are between the same agents.
	 *
	 * @param o Comparing collision
	 * @return True if and only if the collisions are between the same agents
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Collision collision)) {
			return false;
		}
		boolean same = id0 == collision.id0 && id1 == collision.id1;
		if (same && time < collision.time) {  // TODO
			collision.time = time;
		}
		return same;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id0, id1);
	}
}
