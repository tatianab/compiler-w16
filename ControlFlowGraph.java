/* File: ControlFlowGraph.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class ControlFlowGraph {

	public Block firstBlock;
	Function function;                        // The function that contains this CFG.
	public ArrayList<Block> blocks;           // All of the blocks in the CFG.
	public ArrayList<Instruction> instrs;     // All instructions in the CFG.

	// Intermediate data related to state.
	public Block currentBlock;
	public Block currentJoinBlock;

	// Insert an existing block into the CFG.
	public void insertBlock(Block block) {
		blocks.add(block);  		// Add block to block array.
		if (firstBlock == null) {
			firstBlock = block;
		} else {
			// End current block.
		}
		currentBlock = block;
	}

	// Insert an existing instruction into the current block.
	public void insertInstr(Instruction instr) {
		if (currentBlock != null) {
			currentBlock.addInstr(instr); // Add instruction to current block.
			instrs.add(instr);            // Add instruction to list of instructions.
		} else {
			if (Compiler.debug) { System.out.println("Current block is null; instruction not added."); }
		}
	}
	

}
