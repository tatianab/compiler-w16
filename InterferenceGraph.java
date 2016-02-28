/* File: IntermedRepr.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
package compiler-w16;
public class InterferenceGraph {
	/* Shows which variables interfere with each other.
	   This can be deleted or changed completely. */

	private int numVars;        // The number of variables.
	private boolean[][] matrix; // The interference matrix.

	// Constructor.
	public InterferenceGraph(int numVars) {
		this.numVars = numVars;
		this.matrix  = new boolean[numVars][numVars];
	}

	// Add two interfering variables to the interference graph.
	public void add(Variable a, Variable b) {
		int aIndex = a.getUid();
		int bIndex = b.getUid();
		matrix[aIndex][bIndex] = true;
		matrix[aIndex][bIndex] = true;
	}

	// Check if two variables interfere with one another.
	public boolean check(Variable a, Variable b) {
		int aIndex = a.getUid();
		int bIndex = b.getUid();
		return matrix[aIndex][bIndex];
	}
}