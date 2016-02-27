/* File: Variable.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.LinkedList;

public class Variable extends Value {
	String ident;                 // Variable identifier - original name in program.
	int instance;                 // The instance of this variable. e.g, a_1, a_2 etc.
	int id;                       // Variable's id in the string table. Won't be unique.
	int uid;                      // Variable's unique id.
	Instruction def;              // The instruction that defines this variable.
	LinkedList<Instruction> uses; // The instructions that use this variable, but don't re-define it.

	private static int nextAvailableId = 0;

	public Variable(int id) {
		this.id = id;
		this.uses = new LinkedList<Instruction>();

		this.uid = nextAvailableId;
		nextAvailableId++;
	}

	public Variable(int id, String ident) {
		this.id = id;
		this.uses = new LinkedList<Instruction>();
		this.ident = ident;

		this.uid = nextAvailableId;
		nextAvailableId++;
	}

	public Variable(int id, String ident, int instance) {
		this.id       = id;
		this.ident    = ident;
		this.instance = instance;
		this.uses     = new LinkedList<Instruction>();

		this.uid = nextAvailableId;
		nextAvailableId++;
	}

	public int getUid() {
		return uid;
	}

	// Set the instruction that defines this variable.
	public void definedAt(Instruction def) {
		this.def = def;
		this.instance = def.id;
	}

	// Set the next instruction that uses this variable.
	public void usedIn(Instruction use) {
		this.uses.push(use);
	}

	// Print out ident_id, e.g, a_1, a_2 etc.
	@Override
	public String shortRepr() {
		return ident + "_" + instance;
	}

}