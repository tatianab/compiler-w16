/* File: Block.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class Block extends Value {
	// Class representing a basic block in the control flow graph.

	public Instruction begin;   // First instruction in block.
	public Instruction end;     // Last instruction in block.
	public Instruction current; // Current instruction in block.

	private final int id; 	    // Id of the block.

	public Block in1;
	public Block in2;   	    // If a join block.

	public Block fallThrough;   // Fall through block, (i.e., true branch).
	public Block branch;        // Explicit branch block, (i.e, false branch or unconditional branch).

	public String description;  // Optional description for block for debugging purposes.

	// Constructors for a block.
	public Block(int id) {
		this.id = id;
		this.description = "";
	}

	public Block(int id, String description) {
		this.id = id;
		this.description = description;
	}

	// Signify the end of a basic block.
	public void endBlock() {
		end = current;
	}

	// Add an instruction to the block and update current.
	public void addInstr(Instruction instr) {
		if (begin == null) {
			begin = instr;
		} else {
			current.next = instr;
		}
		instr.setBlock(this);
		current = instr;
	}

	/* Methods dealing with previous blocks. */
	public void addPrev(Block in) {
		in1 = in;
	}

	public void addPrev(Block in1, Block in2) {
		in1 = in1;
		in2 = in2;
	}
	/* End previous block methods. */

	/* Methods dealing with next blocks. */
	public void addNext(Block next, boolean jump) {
		if (jump) {
			branch = next;
		} else {
			fallThrough = next;
		}
	}

	public void addNext(Block fallThrough, Block branch) {
		this.fallThrough = fallThrough;
		this.branch = branch;
	}
	/* End next block methods. */

	// Methods for creating VCG representation.
	@Override
	public String toString() {
		String result = "node: { \n" +
						"title: \"" + id + "\" \n" +
						"label: \"" + id + " " + description + " [\n";
		// Instructions
		Instruction instr = begin;
		while (instr != end) {
			result += instr.toString();
			instr = instr.next;
		}
		result += "]\" \n} \n";
		// Outgoing edges
		if (fallThrough != null) {
			result += "edge: { sourcename: \"" + id + "\" \n" +
					  "targetname: \"" + fallThrough.id + "\" \n" +
					  "color: blue \n } \n";
		}
		if (branch != null) {
			result += "edge: { sourcename: \"" + id + "\" \n" +
					  "targetname: \"" + branch.id + "\" \n" +
					  "color: red \n } \n";
		}
		return result;
	}

	@Override
	public String shortRepr() {
		return "[" + id + "]";
	}


}