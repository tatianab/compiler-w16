/* File: Optimizer.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.ArrayList;
import java.util.LinkedList;

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

	public IntermedRepr optimize() {
		if (debug) { System.out.println("Eliminating dead code..."); }
		// deadCodeElim();
		if (debug) { System.out.println("Eliminating common subexpressions..."); }
		commonSubexprElim();
		if (debug) { System.out.println("Copy propagation..."); }
		copyPropagation();
		return program;
	}

	/* Dead code elimination. Deletes instructions that have no side effects
	   and are never used. */
	public void deadCodeElim() {
		// ArrayList<Instruction> workList = new ArrayList<Instruction>(program.instrs);
		// Instruction current;
		// for (Instruction instr : program.instrs) {
		// 	instr.visited = false;
		// }
		// while (workList.size() != 0) {
		// 	current = workList.pop();
		// 	current.visited == true;
		// 	if (deletable(current.op)) {
		// 		if (current.uses == null || current.uses.size() == 0) { 
		// 			current.delete();
		// 		}
		// 		for (Instr instrUsed : instr.instrsUsed) {
		// 			instrUsed.deleteUse(instr);
		// 			workList.add(instrUsed);
		// 		}
		// 	}
		// }
	}

	// True if it is OK to delete an instruction of this type
	// that has no uses.
	public boolean deletable(int op) {
		if (op >= write || op == phi || op == end) {
			return false;
		} else { return true; }
	}

	/* Common subexpression elimination.
	 */
	public void commonSubexprElim() {
		program.setInstrDominators();
		Instruction currentInstr, equivInstr;
		// If instruction Y dominates instruction X and Y = X, replace all
		// occurrences of X with Y and delete X.
		for (int i = program.instrs.size() - 1; i >= 0; i--) {
			currentInstr = program.instrs.get(i);
			equivInstr   = currentInstr.equivDominatingInstr();
			if (equivInstr != null) {
				for (Instruction useSite : currentInstr.uses) {
					useSite.replace(currentInstr, equivInstr);
				}
				currentInstr.delete();
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
				newValue = instr.arg1;
				oldInstr = (Instruction) instr.arg2;
				for (Instruction useSite : oldInstr.uses) {
					if (newValue instanceof Instruction) {
						useSite.replace(oldInstr, (Instruction) newValue);
					} else if (newValue instanceof Constant) {
						useSite.replace(oldInstr, (Constant) newValue);
					}
				}
				oldInstr.delete();
			}
		}
	}

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