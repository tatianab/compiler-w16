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
	// public Block currentBlock;

	public InterferenceGraph ifg;          // Interference graph.

	public final Function MAIN = new Function(-1, "MAIN", true, this);

	// Function compilation.
	public Function currentFunction;

	public ArrayList<Block>       blocks;     // All of the blocks in the program.
	public ArrayList<Instruction> instrs;     // All instructions in the program.
	public ArrayList<Instruction> mainInstrs; // Just the instructions in the main program.
	public ArrayList<Function>    functions;  // All compiled user-defined functions.

	public ArrayList<Global> globals;
	public ArrayList<Array>    globalArrays;  // All global arrays.

	// Constructor.
	public IntermedRepr(boolean debug) {
		this.debug    = debug;
		nextOpenInstr = 0;
		nextOpenBlock = 0;
		blocks        = new ArrayList<Block>();
		instrs        = new ArrayList<Instruction>();
		currentBlocks = new Stack<Block>();
        currentRepresentation = this;
        functions     = new ArrayList<Function>();
        currentFunction = MAIN;
        mainInstrs    = new ArrayList<Instruction>();
        globals       = new ArrayList<Global>();
        globalArrays  = new ArrayList<Array>();
	}

	public Block begin() {
		Block block = addBlock("Program begins.");
		MAIN.enter = block;
		return block;
	}

	public boolean inMainFunction() {
		return (currentFunction == MAIN);
	}

	public void setScope(Function function) {
		this.currentFunction = function;
	}

	// Begin compiling a function.
	public void beginFunction(Function function) {
		setScope(function);
		Block enter = addBlock("Enter function " + function.shortRepr());
		Block exit  = createBlock("Exit function " + function.shortRepr());
		function.exit = exit;
		endBlock();
		currentBlocks.add(enter);
		function.begin(enter);
	}

	// Clean up after compiling a function.
	public void endFunction() {
		Block exit = currentFunction.exit;
		insertBlock(exit);
		currentFunction.end();
		endBlock();
		functions.add(currentFunction);
		currentFunction = MAIN;
		currentBlocks.removeAllElements();
	}

	// Return the current block.
	public Block currentBlock() {
		if (currentBlocks.empty()) {
			return null;
		}
		return currentBlocks.peek();
	}

	// Create a new block without inserting it.
	public Block createBlock() {
		Block block = new Block(nextOpenBlock);
		nextOpenBlock++;
		return block;
	}

	public Block createBlock(String description) {
		Block block = new Block(nextOpenBlock, description);
		nextOpenBlock++;
		return block;
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

	// Create a new instruction without adding it to the current block.
    public Instruction createInstr() {
        try {
            Instruction instr = new Instruction(nextOpenInstr, currentFunction);
            nextOpenInstr++;
            addToInstructionList(instr); // Add instruction to list of instructions.
            return instr;
        } catch (Exception e) {
            error("Possible null pointer in createInstr.");
            return null;
        }
    }

    public void addToInstructionList(Instruction instr) {
    	if (currentFunction == MAIN) {
    		mainInstrs.add(instr);
    	} else {
    		currentFunction.addInstr(instr);
    	}

    	instrs.add(instr);
    }

	// Add a new instruction to the current block.
	public Instruction addInstr() {
		Instruction instr = new Instruction(nextOpenInstr, currentFunction);
		nextOpenInstr++;
		instr.function = currentFunction;
		insertInstr(instr);
		return instr;
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

	// Insert an existing instruction into the current block.
	public void insertInstr(Instruction instr) {
		Block current = currentBlock(); 
		if (current != null) {
			currentBlock().addInstr(instr); // Add instruction to current block.
			addToInstructionList(instr);    // Add instruction to list of instructions.
		} else {
			if (debug) { System.out.println("Instruction could not be added to current block."); }
		}
	}

	// Get an array of all of the instructions (including those in functions)
	// in this program.

	// Add function call instruction.
	public Instruction addFunctionCall(Function function, Value[] params) {
		// TODO: reset modifed marker on all globals
		Instruction instr = addInstr();
		function.generateCall(instr, params);
		return instr;
	}

	// public Instruction addArrayInstr(int op, Array array, Value[] indices, Value expr) {
	// 	Instruction instr = addInstr(op, array, expr);
	// 	addIndexUsage(instr, array, indices);
	// 	return instr;
	// }

	// public Instruction addArrayInstr(int op, Array array, Value[] indices) {
	// 	Instruction instr = addInstr(op, array);
	// 	addIndexUsage(instr, array, indices);
	// 	return instr;
	// }

	// // Update usage information for indices of an array.
	// public void addIndexUsage(Instruction instr, Array array, Value[] indices) {
	// 	// Ensure that all indices are instructions. TODO

	// 	// Associate the instruction with the indices.
	// 	instr.params = indices;

	// 	// Update usage info for indices and the instruction.
	// 	// for (Instruction index : indices) {
	// 	// 	index.usedIn(instr);
	// 	// }
	// }

	// Add assignment (move) instruction.
	public Instruction addAssignment(Variable var, Value expr) {
		Instruction moveInstr = addInstr(move, expr, var);
		moveInstr.defines(var);
        var.definedAt(moveInstr);
        currentBlock().addReturnValue(var);
        checkGlobal(var.getGlobalVar(), moveInstr);
		return moveInstr;
	}

	public void declare(Value val) {
		if (inMainFunction()) {
			if (val instanceof Variable) {
				// globalVars.add( (Variable) val );
			} else if (val instanceof Array) {
				globalArrays.add( (Array) val );
			}
		}
	}

	public void addGlobal(Global g) {
		if (inMainFunction() && g != null) {
			globals.add(g);
		}
	}

	public void updateGlobal(Global g, Value v) {

		if (inMainFunction() && g != null)  {
			g.modified = true;
			g.lastDef  = v;
		} else if (g != null) {
			currentFunction.addGlobalModification(g, v);
		}
	}

	// If the 
	private void checkGlobal(Global g, Instruction instr) {
		if (g != null && !inMainFunction() ) {
			currentFunction.addGlobalModification(g, instr);
		}
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
			if (instr.sameOpDominator != null) {
				result += VCG.edge(instr.id, instr.sameOpDominator.id, "yellow");        // previous instr with same op
			}
			if (instr.equivDominatingInstr() != null) {
				result += VCG.edge(instr.id, instr.equivDominatingInstr().id, "orange"); // immediately dominating instr
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

	public void setInstrDominators() {
		for (Instruction instr : instrs) {
			if (!instr.visited) {
				visit(instr);
			}
		}
	}

	// Helper for setInstrDominators.
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

	public void phify() {
		for (Instruction instr: instrs) {
			if (instr.op == Instruction.phi) {
				if (instr.arg1 instanceof Block.phiPatcher) {
					Block.phiPatcher patcher = (Block.phiPatcher) instr.arg1;
					patcher.patch();
				}
				if (instr.arg2 instanceof Block.phiPatcher) {
					Block.phiPatcher patcher = (Block.phiPatcher) instr.arg2;
					patcher.patch();
				}
			}
		}
	}

	// Get rid of any deleted instructions.
	// TODO: get rid of deleted
	public void clean() {
		Instruction instr;
		int i = 0;
		while (i < instrs.size()) {
			instr = instrs.get(i);
			if (debug) { System.out.println("Testing " + instr); }
			if (instr.deleted()) {
				instrs.remove(instr);
			} else {
				i++;
			}
		}

	}

	/** Methods related to REGISTER ALLOCATION. **/

	/* Assigns first available register to instruction.
       Once all registers are filled it gives up. */
    public void dumbRegAlloc() {
        int FIRST_REG = 3;
        int LAST_REG  = 27;
        int nextAvailReg = FIRST_REG;
        for (Instruction instr : instrs) {
        	if (instr.op != phi) {
            	instr.assignReg(nextAvailReg);
            	nextAvailReg++;
            	if (nextAvailReg > LAST_REG) {
                	Compiler.error("Not enough registers!");
            	}
            }
        }
    }

    public void eliminatePhi() {
    	ArrayList<Instruction> oldInstrs = new ArrayList<Instruction>(instrs);
    	for (Instruction instr : oldInstrs) {
    		if (instr.op == phi) {
    			replacePhi(instr);
    		}
    	}
    }

    public void replacePhi(Instruction instr) {
    	Instruction moveInstr, arg1, arg2;
    	arg1 = (Instruction) instr.arg1;
    	arg2 = (Instruction) instr.arg2;
    	if (arg1 != null) {
    		moveInstr = createInstr();
    		moveInstr.setOp(move);
			moveInstr.setArgs(arg1, instr);
    		arg1.block.addToEnd(moveInstr);
    	}
    	if (arg2 != null) {
    		moveInstr = createInstr();
    		moveInstr.setOp(move);
			moveInstr.setArgs(arg2, instr);
    		arg2.block.addToEnd(moveInstr);
    	}
    	instrs.remove(instr);
    	instr.delete();
    }

    // Topologically sort blocks and instructions based on dominator
	// relationships. 
	public void topoSort() {
		ArrayList<Block> sortedBlocks = new ArrayList<Block>();

		// Set all visited flags to false.
		for (Block current : blocks) {
			current.visited = false;
		}

		// Visit every block.
		for (Block current : blocks) {
			visit(current, sortedBlocks);
		}

		blocks = sortedBlocks;

		ArrayList<Instruction> sortedInstrs = new ArrayList<Instruction>();
		Instruction instr;
		for (Block current : blocks) {
			instr = current.begin;
			if (instr != null) {
				while (instr != current.end) {
					sortedInstrs.add(instr);
					instr = instr.next;
				}
				sortedInstrs.add(instr);
			}
			
		}
		
		instrs = sortedInstrs;
	}

	// Helper for topological sort.
	public void visit(Block block, ArrayList<Block> sortedBlocks) {
		block.visited = true;
		for (Block dominee : block.dominees) {
			visit(dominee, sortedBlocks);
		}
		sortedBlocks.add(0, block);
	}

    // This needs to be fixed.
    public int getNumGlobals() {
    	return MAIN.instrs.size();
    }

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