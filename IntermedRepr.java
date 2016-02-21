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
    
    public static IntermedRepr currentRespersenation;
    
	public int nextOpenInstr;  // Next available instruction ID.

	// Control Flow Graph (CFG)
	// Maybe this should be its own class?
	public Block firstBlock;
	public Stack<Block> currentBlocks;
	public Block currentBlock;

	// Function compilation.
	public Function currentFunction;

	// Block ID: Always increment
	public int nextOpenBlock;  // Next available block ID.

	// Interference graph.
	// TODO.
	// This may work best as an adjacency list.

	// Block array (for easy printing).
	public ArrayList<Block> blocks;

	/* Operation codes for intermediate representation. */
	public static int neg     = Instruction.neg;
	public static int add     = Instruction.add;
	public static int sub     = Instruction.sub;
	public static int mul     = Instruction.mul;
	public static int div     = Instruction.div;
	public static int cmp     = Instruction.cmp;

	public static int adda    = Instruction.adda;
	public static int load    = Instruction.load;
	public static int store   = Instruction.store;
	public static int move    = Instruction.move;
	public static int phi     = Instruction.phi;

	public static int end     = Instruction.end;

	public static int read    = Instruction.read;
	public static int write   = Instruction.write;
	public static int writeNL = Instruction.writeNL;

	public static int bra     = Instruction.bra;
	public static int bne     = Instruction.bne;
	public static int beq     = Instruction.beq;
	public static int bge     = Instruction.bge;
	public static int blt     = Instruction.blt;
	public static int bgt     = Instruction.bgt;
	public static int ble     = Instruction.ble;
	/* End operation codes. */


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
		blocks = new ArrayList<Block>();
		currentBlocks = new Stack<Block>();
        currentRespersenation = this;
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
		blocks.add(block);  		// Add block to block array.
		if (firstBlock == null) {
			firstBlock = block;
		} else {
			endBlock();
		}
		currentBlocks.push(block); // Add block to block stack.
	}

	// Add a new instruction to the current block.
	public Instruction addInstr() {
		try {
			Instruction instr = new Instruction(nextOpenInstr);
			nextOpenInstr++;
			currentBlock().addInstr(instr);
			return instr;
		} catch (Exception e) { 
			error("Possible null pointer in addInstr.");
			return null;
		}
	}

	public Instruction addInstr(int op) {
		Instruction instr = addInstr();
		instr.setOp(op);
		return instr;
	}

	public Instruction addInstr(int op, Value arg) {
		Instruction instr = addInstr(op);
		instr.setArgs(arg);
		return instr;
	}

	public Instruction addInstr(int op, Value arg1, Value arg2) {
		Instruction instr = addInstr(op);
		instr.setArgs(arg1, arg2);
		return instr;
	}

	public void insertFunc(Function func) {
		// Loop over blocks in function and add them.
		// TODO.
	}

	// Signal that the current block is finished.
	public void endBlock() {
		Block current = currentBlock();
		if (current != null) {
			current.endBlock();
			currentBlocks.pop();
		}
	}

	// Signals the end of a program.
	public void end() {
		try {
			addInstr(end);
			endBlock();
		} catch (Exception e) { 
			error("Possible null pointer in end.");
		}
	}

	// Create interference graph.
	// Must be called only AFTER program is in SSA form.
	public void createInterferenceGraph() {
		// TODO.
	}

	// Print out VCG code for the Control Flow Graph.
	public String cfg() {
		String result = "graph: { title: \"Control Flow Graph\" \n" 
						// + "layoutalgorithm: dfs \n" 
						+ "manhattan_edges: yes \n" 
						+ "smanhattan_edges: yes \n"
						+ "orientation: top_to_bottom \n";
		// Print blocks.
		Block block;
		for (int i = 0; i < nextOpenBlock; i++) {
			block = blocks.get(i);
			result += block.cfg();
		}
		result += "}";
		return result;
	}

	public void error(String message) {
		System.out.println("ERROR (IntermedRepr): " + message);
		System.exit(0);
	}

}