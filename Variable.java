/* File: Variable.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.HashSet;

public class Variable extends Value {
	public String ident;                 // Variable identifier - original name in program.
	public int instance;                 // The instance of this variable. e.g, a_1, a_2 etc.
	public int id;                       // Variable's id in the string table. Won't be unique.
	public int uid;                      // Variable's unique id.
	public Instruction def;              // The instruction that defines this variable.
	public HashSet<Instruction> uses;    // The instructions that use this variable, but don't re-define it.
	private Global globalVar; 

	public RegAllocator.memorySpace.memoryPosition position;

	private static int nextAvailableId = 0;

	public Variable() {
		this.uses = new HashSet<Instruction>();

		this.uid = nextAvailableId;
		nextAvailableId++;
	}

	public Variable(int id) {
		this.id = id;
		this.uses = new HashSet<Instruction>();

		this.uid = nextAvailableId;
		nextAvailableId++;
	}

	public Variable(int id, String ident) {
		this.id = id;
		this.uses = new HashSet<Instruction>();
		this.ident = ident;

		this.uid = nextAvailableId;
		nextAvailableId++;
	}

	public Variable(int id, String ident, int instance) {
		this.id       = id;
		this.ident    = ident;
		this.instance = instance;
		this.uses     = new HashSet<Instruction>();

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
		this.uses.add(use);
	}

	// This variable is declared but uninitialized if the
	// instance is -1.
	public boolean uninit() {
		return (instance == -1);
	}

	public void setGlobalVar(Global g) {
		this.globalVar = g;
	}

	@Override
	public Global getGlobalVar() {
		if (globalVar == null && global) {
			Compiler.error("Global var not assigned to " + shortRepr() + ".");
		}
		return globalVar;
	}

	// Print out ident_id, e.g, a_1, a_2 etc.
	@Override
	public String shortRepr() {
		return ident + "_" + instance;
	}

}