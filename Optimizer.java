/* File: Optimizer.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class Optimizer {
	/* The optimizer's job is to perform the following optimizations
	   on a program in SSA form:
	   - Dead code elimination.
	   - Common subexpression elimination.
	   - Copy propagation.
	 */
	IntermedRepr program;

	public Optimizer(IntermedRepr program) {
		this.program = program;
	}

	public IntermedRepr optimize() {
		deadCodeElim();
		commonSubexprElim();
		copyPropagation();
		return program;
	}

	/* Dead code elimination. Deletes variables (and corresponding instructions)
	   that are defined but not used. */
	public void deadCodeElim() {
		ArrayList<Variable> workList = new ArrayList<Variable>(program.instrs);
		while (workList.size() != 0) {
			Instruction instr;
			Variable var = instr.varDefd;
			if (var != null) {
				if (var.uses == null) { 
					program.delete(instr);
				}
				for (Variable varUsed : instr.varsUsed) {
					varUsed.deleteUse(instr);
					workList.add(varUsed);
				}
			}

		}
	}

	/* Common subexpression elimination.
	 */
	public void commonSubexprElim() {
		// Loop over all rvalue operations and create linked list
		// of pointers to dominating instructions with same operation.

		// If instruction Y dominates instruction X and Y = X, replace all
		// occurrences of X with Y and delete X.
	}

	/* Copy propagation.
	 */
	public void copyPropagation() {
		// Delete move instructions 
		// 		move y x
		// and replace all occurrences of x with y.
	}
}