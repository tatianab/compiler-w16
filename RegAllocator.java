/* File: RegAllocator.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
package compiler-w16;
public class RegAllocator {
	/* The register allocators's job is allocate registers for the compiler.
	   In particular, it:
	   - Tracks the live ranges of individual values and creates an interference graph.
	   - Colors the interference graph (assuming 8 general purpose registers).
	   - Eliminates phi instructions and inserts move instructions.
	   - Displays the final result using VCG.
	 */
	IntermedRepr program;
	InterferenceGraph interferenceGraph;

	public RegAllocator(IntermedRepr program) {
		this.program = program;
	}

	public IntermedRepr allocateRegisters() {
		this.interferenceGraph = program.createInterferenceGraph();
		colorInterferenceGraph();
		program.elimiatePhi();
		return program;
	}

	/* Tracking live ranges / interference graph.

	 */

	/* Interference graph coloring. 

	 */

	/* Elimination of phi instructions (replace with move instructions).

	 */

}