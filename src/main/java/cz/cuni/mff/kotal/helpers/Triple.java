package cz.cuni.mff.kotal.helpers;

public class Triple<E, F, G> {
	E val0;
	F val1;
	G val2;

	public Triple(E val0, F val1, G val2) {
		this.val0 = val0;
		this.val1 = val1;
		this.val2 = val2;
	}

	public E getVal0() {
		return val0;
	}

	public Triple<E, F, G> setVal0(E val0) {
		this.val0 = val0;
		return this;
	}

	public F getVal1() {
		return val1;
	}

	public Triple<E, F, G> setVal1(F val1) {
		this.val1 = val1;
		return this;
	}

	public G getVal2() {
		return val2;
	}

	public Triple<E, F, G> setVal2(G val2) {
		this.val2 = val2;
		return this;
	}

	@Override
	public String toString() {
		return val0.toString() + " + " + val1.toString() + " + " + val2.toString();
	}
}
