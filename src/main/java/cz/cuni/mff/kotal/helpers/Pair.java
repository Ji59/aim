package cz.cuni.mff.kotal.helpers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Pair<E, F> {
	E val0;
	F val1;

	public Pair(E val0, F val1) {
		this.val0 = val0;
		this.val1 = val1;
	}

	public E getVal0() {
		return val0;
	}

	public @NotNull Pair<E, F> setVal0(E val0) {
		this.val0 = val0;
		return this;
	}

	public F getVal1() {
		return val1;
	}

	public @NotNull Pair<E, F> setVal1(F val1) {
		this.val1 = val1;
		return this;
	}

	@Override
	public @NotNull String toString() {
		return String.join(" : ", getTexts(val0, val1));
	}

	public static String @NotNull [] getTexts(Object @NotNull ... values) {
		String @NotNull [] texts = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			Object value = values[i];
			texts[i] = value == null ? "null" : value.toString();
		}
		return texts;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		@NotNull Pair<?, ?> pair = (Pair<?, ?>) o;

		if (!Objects.equals(val0, pair.val0)) return false;
		return Objects.equals(val1, pair.val1);
	}

	@Override
	public int hashCode() {
		int result = val0 != null ? val0.hashCode() : 0;
		result = 31 * result + (val1 != null ? val1.hashCode() : 0);
		return result;
	}
}
