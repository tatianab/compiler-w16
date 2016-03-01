/* File: Array.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
import java.util.ArrayList;

public class Array extends Value {
	int id;
	String ident;
	int numDims; // Number of dimensions.
	int[] dims;  // Dimensions.

	ArrayList<Integer> collectDims; // Stores dimensions before we know how many dimensions there are.

	// Constructor
	public Array() {
		// TODO
		collectDims = new ArrayList<Integer>();
	}

	public void addDim(int dim) {
		collectDims.add(dim);
	}

	public void commitDims() {
		int i = 0;
		dims = new int[collectDims.size()];
		for (Integer dim : collectDims) {
			dims[i] = dim;
			i++;
		}
		numDims = i;
		collectDims = null;
	}

}