/* File: Block.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Map.Entry;

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
    
    public HashMap<String, Variable> createdValue;
    
	// Constructors for a block.
	public Block(int id) {
		this.id = id;
		this.description = "";
        
        createdValue = new HashMap<String, Variable>();
	}

	public Block(int id, String description) {
		this.id = id;
        this.description = description;
        
        createdValue = new HashMap<String, Variable>();
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
			instr.prev   = current;
		}
		instr.setBlock(this);
		current = instr;
	}

    public void addReturnValue(Variable resultVar) {
        //Add instruction to the map
        if (resultVar != null) {
            createdValue.put(resultVar.ident, resultVar);
        }
    }
    
	/* Methods dealing with previous blocks. */
	public void addPrev(Block in) {
		if (in1 == null) {
			in1 = in;
		} else {
			in2 = in;
            addPrev(in1, in2);
		}
	}
    
    public Variable fetchLastDefinedInstance(String variableName) {
        Variable result = createdValue.get(variableName);
        if (result == null && in1 != null) {
            //If there is no definition, and it has a parent block, search up stream
            return in1.fetchLastDefinedInstance(variableName);
        }
        else return result;
    }

	public void addPrev(Block in1, Block in2) {
		in1 = in1;
		in2 = in2;
        
        HashSet<String> changeVar = new HashSet<String>();
        if (in1.in1 == in2) {
            //If there is no else block, and in1 is the then block
            changeVar.addAll(in1.createdValue.keySet());
        } else if (in2.in1 == in1) {
            //If there is no else block, and in2 is the then block
            changeVar.addAll(in2.createdValue.keySet());
        } else {
            //Either a bi-directional if, or a loop
            changeVar.addAll(in1.createdValue.keySet());
            changeVar.addAll(in2.createdValue.keySet());
        }
        
        IntermedRepr inpr = IntermedRepr.currentRespersenation;
        
        //Generate Phi function
        for (String varianceName: changeVar) {
            Variable var1 = in1.fetchLastDefinedInstance(varianceName);
            Variable var2 = in2.fetchLastDefinedInstance(varianceName);
            if (var1 != null && var2 != null) {
                Instruction instr = inpr.addInstr();
                instr.setArgs(var1, var2);
                instr.setOp(Instruction.phi);
                
                //instr.prev   = begin;
                //begin = instr;
            
                instr.setBlock(this);
                
                System.out.println("phi "+var1.shortRepr()+" "+var2.shortRepr());
            }
        }
        
	}
	/* End previous block methods. */

	/* Methods dealing with next blocks. */
	public void addNext(Block next, boolean jump) {
		if (jump) {
			branch = next;
		} else {
			fallThrough = next;
		}
		next.addPrev(this);
	}

	public void addNext(Block fallThrough, Block branch) {
		this.fallThrough = fallThrough;
		this.branch = branch;
		fallThrough.addPrev(this);
		branch.addPrev(this);

	}

	// Fix a block by adding a jump to the given block from
	// the last instruction of this block.
	// Must only be called when the last instruction of this block
	// is a conditional branch.
	public void fix(Block next) {
		this.end.setArgs(next);
	}

	public void fix() {
		this.end.delete();
	}
	/* End next block methods. */

	// Create VCG representation of block with CFG edges.
	public String cfg() {
		String result = nodes(); // Get the basic blocks.

		// Add outgoing edges.
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
 
	// Create string representation of basic block
	// and instructions, without edges.
	public String nodes() {
		String result = "node: { \n" +
						"title: \"" + id + "\" \n" +
						"label: \"" + id + " " + description + " [\n";
		// Instructions.
		Instruction instr = begin;
		while (instr != end) {
			result += instr.toString();
			instr = instr.next;
		}
		if (end != null) {
			result += end.toString();
		}
        
        result += "createdValue: \n";
        for (Map.Entry<String, Variable> entry: createdValue.entrySet()) {
            result += entry.getKey() + ": " + entry.getValue().shortRepr() + "\n";
        }
        
		result += "]\" \n} \n";
		return result;
	}

	@Override
	public String shortRepr() {
		return "[" + id + "]";
	}


}