/* File: Block.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.ArrayList;

public class Block extends Value {
	// Class representing a basic block in the control flow graph.

	public Instruction begin;   // First instruction in block.
	public Instruction end;     // Last instruction in block.
	public Instruction current; // Current instruction in block.

	public final int id; 	    // Id of the block.

	public Block in1;
	public Block in2;   	    // If a join block.

	public Block fallThrough;   // Fall through block, (i.e., true branch).
	public Block branch;        // Explicit branch block, 
								//  (i.e, false branch or unconditional branch).

	public String description;  // Description of block.
    
    public HashMap<String, Variable> createdValue;

    // Dominator relationships.
    public ArrayList<Block> dominees;  // The blocks that this block dominates.
    public Block            dominator; // Dominator of this block.
    
	// Constructors for a block.
	public Block(int id) {
		this.id = id;
		this.description = "";
        
        createdValue = new HashMap<String, Variable>();
        dominees     = new ArrayList<Block>();
	}

	public Block(int id, String description) {
		this.id = id;
        this.description = description;
        
        createdValue = new HashMap<String, Variable>();
        dominees     = new ArrayList<Block>();
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
            // If there is no definition, and it has a parent block, 
            // search up stream
            return in1.fetchLastDefinedInstance(variableName);
        }
        else return result;
    }

	public void addPrev(Block in1, Block in2) {
		in1 = in1;
		in2 = in2;
        
        HashSet<String> changeVar = new HashSet<String>();
        if (in1.in1 == in2) {
            // If there is no else block, and in1 is the then block
            changeVar.addAll(in1.createdValue.keySet());
        } else if (in2.in1 == in1) {
            // If there is no else block, and in2 is the then block
            changeVar.addAll(in2.createdValue.keySet());
        } else {
            // Either a bi-directional if, or a loop
            changeVar.addAll(in1.createdValue.keySet());
            changeVar.addAll(in2.createdValue.keySet());
        }
        
        IntermedRepr inpr = IntermedRepr.currentRepresentation;
        
        StringTable table  = StringTable.sharedTable;
        
        //TODO:
        //Got the last block of inner loop
        Block inner = null;
        if (in1.id > id)
            inner = in1;
        if (in2.id > id)
            inner = in2;
        
        Instruction phiBegin = null;
        Instruction phiEnd = null;
        
        // Generate Phi function
        for (String varianceName: changeVar) {
            Variable var1 = in1.fetchLastDefinedInstance(varianceName);
            Variable var2 = in2.fetchLastDefinedInstance(varianceName);
            if (var1 != null && var2 != null) {
                Instruction instr = inpr.createInstr();
                
                //this.addInstr(instr); // Add instruction to current block.
                
                instr.setArgs(var1, var2);
                instr.setOp(Instruction.phi);
            
                instr.setBlock(this);
                
                // Create new move instruction for this Variable.
                Variable var = table.reassign(var1.id);    // Set variable name and instance.
                Instruction moveInstr = inpr.createInstr();
                //this.addInstr(moveInstr); // Add instruction to current block.
                moveInstr.setDefn(var,instr);    // move var expr
                
                
                instr.next = moveInstr;
                
                if (phiBegin == null) {
                    phiBegin = instr;
                } else {
                    phiEnd.next = instr;
                }
                
                phiEnd = moveInstr;
                
                if (inner != null) {
                    //Go back to the inside of loop, replace the the old value with the phi version, such that the value
                    //Example: a3 = phi(a1, a2), if a[x] is the upstream, replace a[x] with a3 ([x] = 1 or 2)
                    this.fixLoopingPhi(var1, var);
                    this.fixLoopingPhi(var2, var);
                    inner.fixLoopingPhi(var1, var);
                    inner.fixLoopingPhi(var2, var);
                }
                
            }
        }
        
        phiEnd.next = begin;
        begin = phiBegin;
        
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

    public void fixLoopingPhi(Variable originalVar, Variable replacementVar) {
        //TODO:
        //Look for all instruction that use originalVar, and replace it with replacement
        Instruction instr = begin;
        while (instr != end) {
            instr.updateArg(originalVar, replacementVar);
            instr = instr.next;
        }
    }

	/* Methods related to dominance. */

	// This block dominates the other block.
	public void dominates(Block other) {
		dominees.add(other);
		other.dominator = this;
	}

	// Is this block a dominator of the other block?
	public boolean isDominatorOf(Block other) {
		return (other.dominator == this);
	}

	// Is the other block a dominator of this block?
	public boolean isDominatedBy(Block other) {
		return (this.dominator == other);
	}

	/* End methods related to dominance. */

	/* Methods related to string representation (in VCG form). */

	// Create VCG representation of block with CFG edges.
	public String cfg() {
		String result = blockToString(); // Get the basic block.

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

	// Create VCG representation of block with dominance edges.
	public String dominanceString() {
		String result = blockToString(); // Get the basic block.

		// Add outgoing edges.
		for (int i = 0; i < dominees.size(); i++) {
			result += "edge: { sourcename: \"" + id + "\" \n" +
					  "targetname: \"" + dominees.get(i).id + "\" \n" +
					  "color: black \n } \n";
		}
		return result;
	}
 
	// Create string representation of basic block
	// and instructions, without edges.
	public String blockToString() {
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
            result += entry.getKey() + ": " + entry.getValue().shortRepr() 
            + "\n";
        }
        
		result += "]\" \n} \n";
		return result;
	}

	@Override
	public String shortRepr() {
		return "[" + id + "]";
	}

    //Search for instruction with variable
    public Instruction instructionsWithDefVariable(Variable var) {
        
    }
    
    public ArrayList<Instruction> instructionsWithUsageOfVariable(Variable var) {
        
    }

}