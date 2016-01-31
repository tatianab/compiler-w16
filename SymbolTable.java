/* File: SymbolTable.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.Hashtable;
import java.util.LinkedList;

public class SymbolTable {
	// Class representing one-way lookup symbol table for
	// variables and constants

	public class Data {
		public int id;        				 // Do we need this?
		public int type;					 // Array, var, constant. Procedure or function too?
		public LinkedList<Integer> current;  // For SSA form.
		public int value;

		// Need more stuff for arrays.
	}

	private Hashtable<String, Data> hashtable;

	public SymbolTable() {
		hashtable = new Hashtable();
	}

	public void add(String name) {
		// TODO
	}

	public int lookup(String name) {
		// TODO
		return 0;
	}

}