/* File: Variable.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.LinkedList;

public class Variable extends Value {
	String ident;                 // Variable identifier - original name in program.
	int instance;                 // The instance of this variable. e.g, a_1, a_2 etc.
	Instruction def;              // The instruction that defines this variable.
	LinkedList<Instruction> uses; // The instructions that use this variable, but don't re-define it.

	// Print out ident_id, e.g, a_1, a_2 etc.
	@Override
	public String shortRepr() {
		return ident + "_" + instance;
	}

}