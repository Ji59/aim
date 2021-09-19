package cz.cuni.mff.kotal.helpers;

import java.util.Objects;

public class Collision {
	private final long id0;
	private final long id1;
	private double time;

	public Collision(long id0, long id1) {
		this.id0 = id0;
		this.id1 = id1;
	}

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

	public long getId0() {
		return id0;
	}

	public long getId1() {
		return id1;
	}

	public double getTime() {
		return time;
	}

	public Collision setTime(double time) {
		this.time = time;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Collision collision = (Collision) o;
		return id0 == collision.id0 && id1 == collision.id1;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id0, id1);
	}
}
