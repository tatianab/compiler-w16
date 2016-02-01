/* File: IntermedRepr.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.io.IOException;
import java.util.ArrayList;

public class IntermedRepr {
	// Encapsulates intermediate representation of a PL241
	// program in SSA form.

	// Code
	public int nextOpenInstr;  // Next available instruction ID.

	// Control Flow Graph (CFG)
	public Block firstBlock;
	public Block currentBlock;
	public int nextOpenBlock;  // Next available block ID.

	// Symbol table
	public SymbolTable table;  // Table of constants, variables, and arrays.

	// Block array (for easy printing).
	public ArrayList<Block> blocks;

	public IntermedRepr() {
		nextOpenInstr = 0;
		nextOpenBlock = 0;
		table = new SymbolTable();
		blocks = new ArrayList();
	}

	// Create a new block and insert it.
	public Block addBlock() {
		Block block = new Block(nextOpenBlock);
		nextOpenBlock++;
		insertBlock(block);
		return block;
	}

	public Block addBlock(String description) {
		Block block = new Block(nextOpenBlock, description);
		nextOpenBlock++;
		insertBlock(block);
		return block;
	}

	// Insert an existing block.
	public void insertBlock(Block block) {
		blocks.add(block);
		if (firstBlock == null) {
			firstBlock = block;
		} else {
			endBlock();
		}
		currentBlock = block;
	}

	// Do not add an instruction before there are any blocks.
	public Instruction addInstr() {
		Instruction instr = new Instruction(nextOpenInstr);
		nextOpenInstr++;
		currentBlock.addInstr(instr);
		return instr;
	}

	public void setCurrentBlock(Block block) {
		currentBlock = block;
	}

	public Block getCurrentBlock(Block block) {
		return currentBlock;
	}

	public void endBlock() {
		currentBlock.endBlock();
	}

	// Print out VCG code for this compiler graph.
	@Override
	public String toString() {
		String result = "graph: { title: \"Control Flow Graph\" \n" 
						// + "layoutalgorithm: dfs \n" 
						+ "manhattan_edges: yes \n" 
						+ "smanhattan_edges: yes \n";
		// Add blocks.
		Block block;
		for (int i = 0; i < nextOpenBlock; i++) {
			block = blocks.get(i);
			result += block.toString();
		}
		result += "}";
		return result;
	}

	// public static void main(String[] args) {
	// 	IntermedRepr program = new IntermedRepr();
	// 	program.addBlock();
	// 	program.addInstr();
	// 	program.endBlock();
	// 	Block oldBlock = program.currentBlock;
	// 	program.addBlock();
	// 	oldBlock.addNext(program.currentBlock, true);
	// 	program.addInstr();
	// 	program.endBlock();
	// 	System.out.println(program);

	// }


}