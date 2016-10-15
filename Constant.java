/* File: Constant.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
public class Constant extends Value {
	// For now, just a wrapper for an int.
	private final int value;

	public Constant(int value) {
		this.value = value;
	}

	@Override
	public int getVal() {
		return value;
	}

	@Override
	public int getReg() {
		if (value == 0) {
			return 0;
		} else {
			return -1;
		}
	}

	@Override
	public String shortRepr() {
		return "#" + value;
	}

	@Override
	public boolean equals(Value other) {
		if (other instanceof Constant) {
			return (value == other.getVal());
		}
		return false;
	}
}