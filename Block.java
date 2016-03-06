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

    public boolean visited;     // For use in topological sorting.

    public Integer dlxPos;      // The index where this block begins in the dlx code.

    // Dominator relationships.
    public ArrayList<Block> dominees;  // The blocks that this block dominates.
    public Block            dominator; // Dominator of this block.
    
	// Constructors for a block.
	public Block(int id) {
		this.id = id;
		this.description = "";
        
        createdValue = new HashMap<String, Variable>();
        dominees     = new ArrayList<Block>();
        this.visited = false;
        dlxPos = null;
	}

	public Block(int id, String description) {
		this.id = id;
        this.description = description;
        
        createdValue = new HashMap<String, Variable>();
        dominees     = new ArrayList<Block>();
        this.visited = false;
        dlxPos = null;
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
			if (current != null) {
				current.next = instr;
				instr.prev   = current;
			} else {
				Compiler.error("(In Block " + this + instrsToString() + "\n" +
					"No current instruction set.");
			}
		}
		instr.setBlock(this);
		current = instr;
	}

    public void addReturnValue(Variable resultVar) {
        // Add instruction to the map
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
        
        //Got the last block of inner loop
        Block inner = null;
        if (in1.id > id)
            inner = in1;
        if (in2.id > id)
            inner = in2;
        
        Instruction phiBegin = null;
        Instruction phiEnd = null;
        Instruction instr;
        
        // Generate Phi function
        for (String varianceName: changeVar) {
            Variable var1 = in1.fetchLastDefinedInstance(varianceName);
            Variable var2 = in2.fetchLastDefinedInstance(varianceName);

            if (var1 != null && var2 != null && var1 != var2) {
                instr = inpr.createInstr();
                
                //this.addInstr(instr); // Add instruction to current block.
                
                instr.setArgs(var1, var2);
                instr.setOp(Instruction.phi);
            
                instr.setBlock(this);
                this.current = instr;
                
                // Reassign for this Variable.
                Variable var = new Variable(var1.id, table.getName(var1.id));
                table.reassignVar(var1.id, var);

                instr.defines(var);
        		var.definedAt(instr);
        		inpr.currentBlock().addReturnValue(var);
                
                if (phiBegin == null) {
                    phiBegin = instr;
                } else {
                    phiEnd.next = instr;
                }
                
                phiEnd = instr;
                
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
        
        if (phiEnd != null) {
        	phiEnd.next = begin;
        	begin = phiBegin;
        }
        
	}
	/* End previous block methods. */

	public void smartlyCopy(HashMap<String, Variable> source, HashMap<String, Variable> target) {
		for (String key: source.keySet()) {
			if (target.containsKey(key) == false) {
				target.put(key, source.get(key));
			}
		}
	}

	/* Methods dealing with next blocks. */
	public void addNext(Block next, boolean jump) {
		smartlyCopy(createdValue, next.createdValue);
		if (jump) {
			branch = next;
		} else {
			fallThrough = next;
		}
		next.addPrev(this);
	}

	public void addNext(Block fallThrough, Block branch) {
		smartlyCopy(createdValue, fallThrough.createdValue);
		smartlyCopy(createdValue, branch.createdValue);

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

	// This block immediately dominates the other block.
	public void dominates(Block other) {
		dominees.add(other);
		other.dominator = this;
	}

	// Is this block an immediate dominator of the other block?
	public boolean isIDominatorOf(Block other) {
		return (other.dominator == this);
	}

	// Is the other block an immediate dominator of this block?
	public boolean isIDominatedBy(Block other) {
		return (this.dominator == other);
	}

	// Is this block a dominator of the other block?
	public boolean isDominatorOf(Block other) {
		// True if the blocks are the same.
		if (this == other) {
			return true;
		}

		// Otherwise, recursively check for dominance.
		if (this.dominees.size() == 0) {
			return false;
		} else if (this.isIDominatorOf(other)) {
			return true;
		} else {
			for (Block dominee : dominees) {
				if (dominee.isDominatorOf(other)) {
					return true;
				}
			}
		}
		return false;
	}

	// Is the other block a dominator of this block?
	public boolean isDominatedBy(Block other) {
		return other.isDominatorOf(this);
	}

	/* End methods related to dominance. */

	/* Methods related to string representation (in VCG form). */

	// String representation of the instructions in this block.
	public String instrsToString() {
		String result = "";
		Instruction instr = begin;
		while (instr != end) {
			result += instr.toString() + "\n";
			instr = instr.next;
		}
		if (end != null) {
			result += end.toString();
		}
		return result;
	}
 
 	// String representation of this block with id and description.
	@Override
	public String toString() {
		return id + ": " + description;
	}

	@Override
	public String shortRepr() {
		return "[" + id + "]";
	}


}