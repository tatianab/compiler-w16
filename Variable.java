/* File: Variable.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.HashSet;

public class Variable {
	
	private int id;                       // Variable's id in the string table. 
	private Instruction lastDef;          // The last instruction to redefine this variable.
	private Function scope;               // The function where this variable was defined.
	private String ident;                 // Variable identifier - original name in program.

	public Variable() {
		// Nothing.
	}

	public Variable(int id) {
		this.id = id;
	}

	public Variable(int id, String ident, Function scope) {
		this.id = id;
		this.ident = ident;
	}

	// Reset the most recent instruction that defines this variable.
	public void updateLastDef(Instruction instr) {
		lastDef = instr;
	}

	// This variable is declared but uninitialized if the
	// instance is -1.
	public boolean uninit() {
		return (instance == -1);
	}

	// Print out ident_id, e.g, a_1, a_2 etc.
	@Override
	public String shortRepr() {
		return ident + "_" + instance;
	}

}