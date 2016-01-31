/* File: IntermedRepr.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.io.IOException;

public class IntermedRepr {
	// Encapsulates intermediate representation of a PL241
	// program in SSA form.

	// Code
	public Instruction firstInstr;
	public Instruction currentInstr; // ?

	// Control Flow Graph (CFG)
	public Block firstBlock;

	// Symbol table
	public SymbolTable table;

	public IntermedRepr() {
		// Nothing for now.
	}


}