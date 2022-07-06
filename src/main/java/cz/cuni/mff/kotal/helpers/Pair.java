package cz.cuni.mff.kotal.helpers;

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

	public Pair<E, F> setVal0(E val0) {
		this.val0 = val0;
		return this;
	}

	public F getVal1() {
		return val1;
	}

	public Pair<E, F> setVal1(F val1) {
		this.val1 = val1;
		return this;
	}
}
