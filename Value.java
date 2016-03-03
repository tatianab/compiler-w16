/* File: Value.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class Value {
	// Basically do nothing for now.
	// Represents an SSA value.
	
	// This should be overridden.
	public String shortRepr() {
		return "Value of some kind.";
	}

	// This should also be overridden.
	public int getReg() {
		return -1;
	}

	// This too!
	public int getVal() {
		Compiler.error("No value exists");
		return Integer.MIN_VALUE;
	}
}