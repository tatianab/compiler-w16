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
  
    public static IntermedRepr currentRepresentation = null;

    public boolean debug;
    
	public int nextOpenInstr;  // Next available instruction ID.
	public int nextOpenBlock;  // Next available block ID.

	// Control Flow Graph (CFG)
	public Block firstBlock;
	public Stack<Block> currentBlocks;
	public Block currentBlock;

	// public DominatorTree dominatorTree; // Dominator tree.
	public InterferenceGraph ifg;          // Interference graph.

	// Function compilation.
	public Function currentFunction;

	public ArrayList<Block> blocks;       // Block array.
	public ArrayList<Instruction> instrs; // Instruction array.

	// Constructor.
	public IntermedRepr(boolean debug) {
		this.debug    = debug;
		nextOpenInstr = 0;
		nextOpenBlock = 0;
		blocks        = new ArrayList<Block>();
		instrs        = new ArrayList<Instruction>();
		currentBlocks = new Stack<Block>();
        currentRepresentation = this;
	}

	public Block begin() {
		Block block = addBlock("Program begins.");
		// dominatorTree = new DominatorTree(block);
		return block;
	}

	// Return the current block.
	public Block currentBlock() {
		if (currentBlocks.empty()) {
			return null;
		}
		return currentBlocks.peek();
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
    public Instruction createInstr() {
        try {
            Instruction instr = new Instruction(nextOpenInstr);
            nextOpenInstr++;
            //instrs.add(instr);              // Add instruction to list of instructions.
            return instr;
        } catch (Exception e) {
            error("Possible null pointer in addInstr.");
            return null;
        }
    }
    
	public Instruction addInstr() {
		try {
			Instruction instr = new Instruction(nextOpenInstr);
			nextOpenInstr++;
			currentBlock().addInstr(instr); // Add instruction to current block.
			instrs.add(instr);              // Add instruction to list of instructions.
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

	public Instruction addAssignment(Variable var, Value expr) {
		Instruction moveInstr = addInstr(move, expr, var);
		moveInstr.defines(var);
		var.definedAt(moveInstr);
		return moveInstr;
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

    /* Convert all variables into instructions.
	 */
	public void varsToInstrs() {
		// Loop over all instructions and convert any 
		// variables to instructions.
		for (Instruction instr : instrs) {
			instr.varsToInstrs();
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

	// Generate VCG code for the Control Flow Graph.
	public String cfgToString() {
		String result = VCG.header("Control Flow Graph");
		// Generate string for each block.
		for (Block block : blocks) {
			// Generate string for the block's instructions.
			result += VCG.node(block.id, block.toString(), 
							   block.instrsToString());

			// For now, don't show previous edges.
			// if (block.in1 != null) { // Previous block 1.
			// 	result += VCG.edge(block.id, block.in1.id, "red");
			// }
			// if (block.in2 != null) { // Previous block 2.
			// 	result += VCG.edge(block.id, block.in2.id, "red");
			// }

			// Generate control flow edges.
			if (block.fallThrough != null) {  // Fall through.
				result += VCG.edge(block.id, block.fallThrough.id, "black");
			}
			if (block.branch != null) {       // Explicit branch.
				result += VCG.edge(block.id, block.branch.id, "blue");
			}
		}
		result += VCG.footer();
		return result;
	}

	// Generate VCG code for the Dominator Tree.
	public String domTreeToString() {
		String description, contents;
		String result = VCG.header("Dominator Tree");
		// Generate string for each block.
		for (Block block : blocks) {
			// Generate node for block with description and instructions.
			result += VCG.node(block.id, block.toString(), 
							   block.instrsToString());
			// Generate dominance related edges.
			if (block.dominator != null) { // Dominators.
				result += VCG.edge(block.id, block.dominator.id, "red");
			}
			if (block.dominees != null) {  // Dominees.
				for (Block dominee : block.dominees) {
					result += VCG.edge(block.id, dominee.id, "blue"); 
				}
			}
		}
		result += VCG.footer();
		return result;
	}

	// Generate VCG code for instruction relationships.
	public String instrsToString() {
		String result = VCG.header("SSA Instructions");
		for (Instruction instr : instrs) {
			// Generate the instruction.
			result += VCG.node(instr.id, instr.toString(), 
					  instr.dataToString());
			// Generate edges.
			if (instr.prev != null) {
				result += VCG.edge(instr.id, instr.prev.id, "red");  // previous
			}
			if (instr.next != null) {
				result += VCG.edge(instr.id, instr.next.id, "blue"); // next
			}
		}
		result += VCG.footer();
		return result;
	}

	public void error(String message) {
		System.out.println("ERROR (IntermedRepr): " + message);
		System.exit(0);
	}

	/** Methods related to OPTIMIZATION. **/

	// // Topologically sort blocks based on dominator
	// // relationships. 
	// public ArrayList<Block> topoSort() {
	// 	ArrayList<Block> sortedBlocks = new ArrayList<Block>();

	// 	// Set all visited flags to false.
	// 	for (Block current : blocks) {
	// 		current.visited = false;
	// 	}

	// 	// Visit every block.
	// 	for (Block current : blocks) {
	// 		visit(current, sortedBlocks);
	// 	}

	// 	blocks = sortedBlocks;
	// }

	// // Helper for topological sort.
	// public void visit(Block block, ArrayList<Block> sortedBlocks) {
	// 	block.visited = true;
	// 	for (Block dominee : block.dominees) {
	// 		visit(dominee);
	// 	}
	// 	sortedBlocks.pushFront(current);
	// }

	public void setInstrDominators() {
		for (Instruction instr : instrs) {
			if (!instr.visited) {
				visit(instr);
			}
		}
	}

	public void visit(Instruction instr) {
		instr.visited = true;
		Instruction current = instr;
		Instruction prev    = instr.dominatingInstr();
		while (prev != null) {
			if (current.op == prev.op) {
				current.sameOpDominator = prev;
				if (!prev.visited) {
					visit(prev);
				}
				return;
			} else {
				prev = prev.dominatingInstr();
			}
			
		}
	}

	/** Methods related to REGISTER ALLOCATION. **/

	/* Create interference graph. */
	public void createInterferenceGraph() {
		// Loop over all SSA values.
		// for (Instruction instr : instrs) {
		// 	current = instr.varDef;
		// 	if (current != null) {
		// 		for (Instruction use : current.uses) {
		// 			if (use.op == phi ) { // && something else
		// 				// Do something.
		// 			} else {
		// 				// beginLiveRange(use, current);
		// 			}
		// 		}
		// 	}
		// }
	}

	/** Methods related to CODE GENERATION. **/

	/** Other data. **/

	/* Operation codes for intermediate representation. */
	public static final int neg     = Instruction.neg;
	public static final int add     = Instruction.add;
	public static final int sub     = Instruction.sub;
	public static final int mul     = Instruction.mul;
	public static final int div     = Instruction.div;
	public static final int cmp     = Instruction.cmp;

	public static final int adda    = Instruction.adda;
	public static final int load    = Instruction.load;
	public static final int store   = Instruction.store;
	public static final int move    = Instruction.move;
	public static final int phi     = Instruction.phi;

	public static final int end     = Instruction.end;

	public static final int read    = Instruction.read;
	public static final int write   = Instruction.write;
	public static final int writeNL = Instruction.writeNL;

	public static final int bra     = Instruction.bra;
	public static final int bne     = Instruction.bne;
	public static final int beq     = Instruction.beq;
	public static final int bge     = Instruction.bge;
	public static final int blt     = Instruction.blt;
	public static final int bgt     = Instruction.bgt;
	public static final int ble     = Instruction.ble;

	public static final int[] opCodes = new int[]{neg, add, sub, mul, div, cmp,    
 												  adda, load, store, move, phi,  
 												  end, read, write, writeNL, 
 												  bra, bne, beq, bge, blt, bgt,    
 												  ble};
	/* End operation codes. */


}