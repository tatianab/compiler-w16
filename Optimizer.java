/* File: Optimizer.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

public class Optimizer {
	/* The optimizer's job is to perform the following optimizations
	   on a program in SSA form:
	   - Dead code elimination.
	   - Common subexpression elimination.
	   - Copy propagation.
	 */
	IntermedRepr program;
	boolean      debug;

	public Optimizer(IntermedRepr program, boolean debug) {
		this.program = program;
		this.debug   = debug;
	}

	public IntermedRepr preprocess() {
		if (debug) { System.out.println("Converting variables to instructions..."); }
		program.varsToInstrs();
		if (debug) { System.out.println("Setting instruction dominators..."); }
		program.setInstrDominators();
		return program;
	}

	public IntermedRepr optimize() {
		if (debug) { System.out.println("Copy propagation..."); }
		copyPropagation();
		if (debug) { System.out.println("Eliminating common subexpressions..."); }
		commonSubexprElim();
		if (debug) { System.out.println("Precomputing constant values..."); }
		constantPrecompuation();
		if (debug) { System.out.println("Eliminating dead code..."); }
		deadCodeElim();
		// if (debug) { System.out.println("Collapsing empty blocks..."); }
		return program;
	}

	/* Dead code elimination. 
	 * Deletes instructions that have no side effects
	 * and are never used. 
	 */
	public void deadCodeElim() {
		Stack<Instruction> workList = new Stack<Instruction>();
		Instruction current;

		// Set up work list.
		for (Instruction instr : program.instrs) {
			instr.visited = false;
			workList.push(instr);
		}

		// Handle work list.
		while (workList.size() != 0) {
			current = workList.pop();
			if (!current.visited) {
				current.visited = true;

				// Delete any deletable instructions that are not used,
				// and add any instructions that they use to the work list
				// for possible deletion.
				if (deletable(current)) {
					if (current.uses == null || current.uses.size() == 0) { 
						current.delete();
					}
					if (current.instrsUsed != null) {
						for (Instruction instrUsed : current.instrsUsed) {
							if (instrUsed != null) {
								instrUsed.deleteUse(current);
								workList.push(instrUsed);
							}
						}
					}
				}
			}
		}
	}

	/* True if it is OK to delete an instruction of this type.
	 */
	public boolean deletable(Instruction instr) {
		if (instr.op >= write || instr.op == phi || instr.op == end || instr.op == bra) {
			return false;
		} else if (instr.op == move && instr.arg1 instanceof Constant && instr.usedInPhi()) {
			return false;
		} else if (instr.isLinked()) { // Don't delete array instrs for now.
			return false;
		} else { return true; }
	}

	/* Common subexpression elimination.
	 * Avoid unnecessary re-computation of values.
	 */
	public void commonSubexprElim() {
		Instruction equivInstr;
		// If instruction Y dominates instruction X and Y = X, replace all
		// occurrences of X with Y and delete X.
		for (Instruction currentInstr : program.instrs) {
			// if (deletable(currentInstr)) {
				equivInstr   = currentInstr.equivDominatingInstr();
				if (equivInstr != null) {
					for (Instruction useSite : currentInstr.uses) {
						useSite.replace(currentInstr, equivInstr);
					}
					//currentInstr.delete();
			//	}
			}
		}
	}

	/* Copy propagation.
	 * Delete move instructions 
     *		move y x
	 * and replace all occurrences of x with y.
	 */
	public void copyPropagation() {
		Instruction oldInstr;
		Value newValue;
		for (Instruction instr : program.instrs) {
			if (instr.op == move) {
				if (debug) {System.out.println("CP: " + instr);} 
				if (instr.usedInPhi() && instr.arg1 instanceof Constant) {
					if (debug) {System.out.println("Don't delete me: " + instr);} 
				} else {
					newValue = instr.arg1;
					oldInstr = (Instruction) instr.arg2;
					if (oldInstr.op == phi) { continue; }

					ArrayList<Instruction> useSites = new ArrayList<>();

					for (Instruction useSite : oldInstr.uses) {
						useSites.add(useSite);
					}
					for (int i = 0; i < useSites.size(); i++) {
						Instruction useSite = useSites.get(i);
						if (newValue instanceof Instruction) {
							useSite.replace(oldInstr, (Instruction) newValue);
						} else if (newValue instanceof Constant) {
							useSite.replace(oldInstr, (Constant) newValue);
						}
						if (debug) { System.out.println("	Replacing value in instruction " + useSite); }
					}
					// oldInstr.delete();
				}
			}
		}
	}

	/* Get rid of instructions like add #1 #3 that can be precomputed. */
	public void constantPrecompuation() {
		for (Instruction instr : program.instrs) {
			if (instr.op >= add && instr.op <= div) {
				if (debug) { System.out.print("Cons. Precomp: testing " + instr); }
				Value arg1 = instr.arg1;
				Value arg2 = instr.arg2;
				if (instr.arg1 instanceof Constant && instr.arg2 instanceof Constant && !instr.usedInPhi()) {
					if (debug) { System.out.print("args = " + arg1.getVal() +  ", " + arg2.getVal() + ";"); }
					int value   = 0;
					int arg1Val = arg1.getVal();
					int arg2Val = arg2.getVal();
					switch(instr.op) {
						case Instruction.add:
							value = arg1Val + arg2Val;
							break;
						case Instruction.sub:
							value = arg1.getVal() - arg2.getVal();
							break;
						case Instruction.mul:
							value = arg1.getVal() * arg2.getVal();
							break;
						case Instruction.div:
							value = arg1.getVal() / arg2.getVal();
							break;
						default:
							break;
					}
					Constant constant = new Constant(value);
					if (debug) { System.out.print("value = " + value + ", " + constant.shortRepr() + "\n"); }
					ArrayList<Instruction> useSites = new ArrayList<Instruction>(instr.uses);
					for (Instruction useSite : useSites) {
						// TODO: fix concurrent modification error
						useSite.replace(instr, constant);
					}
				}
			}
		}
	}

	// // Get rid of any empty blocks while maintaining structure.
	// public void collapseEmptyBlocks() {
	// 	for (Block block : program.blocks) {
	// 		if (block.isEmpty()) {
	// 			block.remove();
	// 		}
	// 	}
	// }

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

	public static final int[] rValues = new int[]{neg, add, sub, mul, div, cmp};
	/* End operation codes. */
}