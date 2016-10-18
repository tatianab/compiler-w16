/* File: Value.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class Value {
	// Represents an SSA value.
	protected boolean global;

	public void setGlobal() {
		global = true;
	}

	public void setLocal() {
		global = false;
	}

	public boolean isGlobal() {
		return global;
	}

	public boolean isLocal() {
		return !global;
	}

	public Global getGlobalVar() {
		return null;
	}

	public boolean equals(Value other) {
		return (this == other);
	}
	
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