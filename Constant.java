/* File: Constant.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
public class Constant extends Value {
	// For now, just a wrapper for an int.
	int value;

	public Constant(int value) {
		this.value = value;
	}

	@Override
	public String shortRepr() {
		return "#" + value;
	}
}