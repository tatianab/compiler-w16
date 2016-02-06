/* File: IntermedRepr.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

public class IntermedRepr {
	// Encapsulates intermediate representation of a PL241
	// program in SSA form.

	// Code
	public int nextOpenInstr;  // Next available instruction ID.

	// Control Flow Graph (CFG)
	// Maybe this should be its own class?
	public Block firstBlock;
	public Stack<Block> currentBlocks;
	public Block currentBlock;

	//Block ID: Always increment
	public int nextOpenBlock;  // Next available block ID.

	// Interference graph.
	// TODO.
	// This may work best as an adjacency list.

	// Symbol table
	public SymbolTable table;  // Table of constants, variables, and arrays.
							   // We may not actually need this.

	// Block array (for easy printing).
	public ArrayList<Block> blocks;


	public Block currentBlock() {
		if (currentBlocks.empty()) {
			return null;
		}
		return currentBlocks.peek();
	}

	// Constructor.
	public IntermedRepr() {
		nextOpenInstr = 0;
		nextOpenBlock = 0;
		table  = new SymbolTable();
		blocks = new ArrayList();
		currentBlocks = new Stack();
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
		currentBlocks.push(block);
	}

	// Add a new instruction to the current block.
	public Instruction addInstr() {
		Instruction instr = new Instruction(nextOpenInstr);
		nextOpenInstr++;
		currentBlock().addInstr(instr);
		return instr;
	}

	// Symbol table methods.

	// Add a new symbol to the symbol table.
	/*public void addSymbol(Symbol symbol) {
		table.add(symbol.name, symbol);
	}*/

	// Look up an existing symbol from the symbol table.
	/*public Symbol lookupSymbol(String name) {
		return table.lookup(name);
	}*/

	// Signal that the current block is finished.
	// This may not be necessary.
	public void endBlock() {
		Block current = currentBlock();
		if (current != null) {
			current.endBlock();
			currentBlocks.pop();
		}
	}

	// Create interference graph.
	// Must be called only AFTER program is in SSA form.
	public void interferenceGraph() {
		// TODO.
	}

	// Print out VCG code for the Control Flow Graph.
	// We will need more of these...
	@Override
	public String toString() {
		String result = "graph: { title: \"Control Flow Graph\" \n" 
						// + "layoutalgorithm: dfs \n" 
						+ "manhattan_edges: yes \n" 
						+ "smanhattan_edges: yes \n"
						+ "orientation: \"top_to_bottom\" \n";
		// Print blocks.
		Block block;
		for (int i = 0; i < nextOpenBlock; i++) {
			block = blocks.get(i);
			result += block.toString();
		}
		result += "}";
		return result;
	}

}