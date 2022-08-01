package cz.cuni.mff.kotal.helpers;

public class Quadruple<E, F, G, H> {
	E val0;
	F val1;
	G val2;
	H val3;

	public Quadruple(E val0, F val1, G val2, H val3) {
		this.val0 = val0;
		this.val1 = val1;
		this.val2 = val2;
		this.val3 = val3;
	}

	public E getVal0() {
		return val0;
	}

	public Quadruple<E, F, G, H> setVal0(E val0) {
		this.val0 = val0;
		return this;
	}

	public F getVal1() {
		return val1;
	}

	public Quadruple<E, F, G, H> setVal1(F val1) {
		this.val1 = val1;
		return this;
	}

	public G getVal2() {
		return val2;
	}

	public Quadruple<E, F, G, H> setVal2(G val2) {
		this.val2 = val2;
		return this;
	}

	public H getVal3() {
		return val3;
	}

	public Quadruple<E, F, G, H> setVal3(H val3) {
		this.val3 = val3;
		return this;
	}

	@Override
	public String toString() {
		return val0.toString() + " + " + val1.toString() + " + " + val2.toString() + " + " + val3.toString();
	}
}
