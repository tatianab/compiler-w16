/* File: Optimizer.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class Optimizer {
	/* The optimizer's job is to perform the following optimizations
	   on a program in SSA form:
	   - Common subexpression elimination.
	   - Copy propagation.
	 */
	IntermedRepr program;

	public Optimizer(IntermedRepr program) {
		this.program = program;
	}

	public IntermedRepr optimize() {
		program.commonSubexprElim();
		program.copyPropagation();
		return program;
	}
}