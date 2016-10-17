/* File: Addr.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class Addr extends Value {
	public int id;                       // ID for this address
	public HashSet<Instruction> uses;    // The instructions that use this address.

	private static int nextAvailableId = 0;

	public Addr() {
		this.id = nextAvailableId;
		nextAvailableId++;
	}

	// Print out [Addr id]
	@Override
	public String shortRepr() {
		return "[Addr " + id + "]";
	}

}