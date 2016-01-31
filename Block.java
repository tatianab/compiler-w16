/* File: Block.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class Block {
	// Class representing a basic block in the control flow graph.

	public Instruction begin;  // First instruction in block.
	public Instruction end;    // Last instruction in block.

	public int id; 			   // Id of the block.

	public Block in1;
	public Block in2;   	   // If a join block.

	public Block fallThrough;  // Fall through block, (i.e., true branch).
	public Block branch;       // Explicit branch block, (i.e, false branch or unconditional branch).

	public Block() {

	}

}