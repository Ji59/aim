package cz.cuni.mff.kotal.helpers;

import org.jetbrains.annotations.NotNull;

import static cz.cuni.mff.kotal.helpers.Pair.getTexts;

public class Triplet<E, F, G> {
	E val0;
	F val1;
	G val2;

	public Triplet(E val0, F val1, G val2) {
		this.val0 = val0;
		this.val1 = val1;
		this.val2 = val2;
	}

	public E getVal0() {
		return val0;
	}

	public @NotNull Triplet<E, F, G> setVal0(E val0) {
		this.val0 = val0;
		return this;
	}

	public F getVal1() {
		return val1;
	}

	public @NotNull Triplet<E, F, G> setVal1(F val1) {
		this.val1 = val1;
		return this;
	}

	public G getVal2() {
		return val2;
	}

	public @NotNull Triplet<E, F, G> setVal2(G val2) {
		this.val2 = val2;
		return this;
	}

	@Override
	public @NotNull String toString() {
		return String.join(" : ", getTexts(val0, val1, val2));
	}
}
