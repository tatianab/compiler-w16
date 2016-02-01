/* File: Variable.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
public class Variable extends Argument {
	String identifier;
	int value;
	int current;
	LinkedList useChain;

	@Override
	public String shortRepr() {
		return identifier + "_" + current;
	}

}