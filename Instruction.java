/* File: Instruction.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.HashSet;

public class Instruction {
	// Class representing an instruction in intermediate form.

	public int op;            // Operation code.
	public int numArgs;       // Number of arguments expected by this operation.
	// public Value arg1;  	  // First argument, or only argument.
	// public Value arg2;  	  // Second argument.
    public Instruction arg1;  // First argument, or only argument.
	public Instruction arg2;  // Second argument.
	// public Value[] params;    // For call and array instructions only.

	public int id;            // This instruction's ID number. Represents time of
							  // creation rather than position in program.

	private Instruction prev;  // Previous instruction, or null if first in block.
	private Instruction next;  // Next instruction, or null if end of block.

	public Block block;       // The block where this instruction lives.
	public Block branch;      // The block to branch to, if this is a branching instruction.

	private boolean deleted;  // True if this instruction has been deleted.
	public boolean visited;   // Used for searching algorithms.

	// public Variable[] varsUsed; // Variables used in this instruction, up to two.
	// public Variable   varDefd;  // Variables defined in this instruction, up to 1.

	public Instruction[] instrsUsed; // The instructions used by this one.
	HashSet<Instruction> uses;       // The instructions that use the result of this instruction.

	public Instruction sameOpDominator; // The most immediate dominating instruction with the
										// same op code as this instruction.

	public int register; // The register that the value of this instruction is assigned to.

	public InstructionState state;

	/* End operation codes. */

	/* Constructor. */
	public Instruction(int id) {
		this.id = id;
		this.block    = null;
		// this.varDefd  = null;
		// this.varsUsed = null;
		this.prev     = null;
		this.next     = null;
		// this.instrsUsed = new Instruction[2];
		this.uses     = new HashSet<Instruction>();
		this.register = -1;
	}

	public void delete() {
		if (instrsUsed != null)
			for (int i = 0; i < 2; i++) {
				if (instrsUsed[i] != null) {
					instrsUsed[i].uses.remove(this);
				}
			}
		if (Compiler.debug) { System.out.println("Deleting instruction " + this); }
		if (this.prev == null && this.next == null) { 
			block.begin = null;
			block.end   = null;
		} else if (this.prev == null) {
			block.begin = this.next;
			this.next.prev = null;
		} else if (this.next == null) {
			block.end = this.prev;
			this.prev.next = null;
		} else {
			this.next.prev = this.prev;
			this.prev.next = this.next;
		}
		this.deleted = true;
	}

	public boolean deleted() {
		return deleted;
	}

	public void uses(Variable var) {
		if (this.varsUsed == null) {
			this.varsUsed = new Variable[]{var, null};
		} else {
			this.varsUsed[1] = var;
		}
	}

	public void uses(Instruction instr) {
		if (this.instrsUsed == null) {
			this.instrsUsed = new Instruction[]{instr, null};
		} else {
			this.instrsUsed[1] = instr;
		}
	}

	public void deleteUse(Instruction instr) {
		if (instrsUsed != null) {
			if (instrsUsed[0] == instr) {
				instrsUsed[0] = null;
			} else if (instrsUsed[1] == instr) {
				instrsUsed[1] = null;
			}
		}
	}

	public void usedIn(Instruction instr) {
		uses.add(instr);
	}

	public void defines(Variable var) {
		this.varDefd = var;
	}

	/* Setters. */

	public void setOp(int op) {
		this.op = op;
	}

	public void setArgs(Value arg1, Value arg2) {
		this.arg1 = arg1;
		this.arg2 = arg2;

		setUsage(arg1);
		setUsage(arg2);
	}

	public void setArgs(Value arg) {
		if (arg1 != null) {
			this.arg2 = arg;
		}
		else {
			this.arg1 = arg;
			this.arg2 = null;
		}
		setUsage(arg);
	}

	// Should only be called for call instructions.
	public void setParams(Value[] params) {
		this.params = params;
		for (Value param : params) {
			setUsage(param);
		}
	}
    
    public void updateArg(Value original, Value updated) {
		if (updated instanceof Variable && varsUsed != null && varsUsed[0] == original)
			varsUsed[0] = (Variable)updated;

		if (updated instanceof Variable && varsUsed != null && varsUsed[1] == original)
			varsUsed[1] = (Variable)updated;

        if (this.arg1 == original) {
            this.arg1 = updated;
            if (updated instanceof Instruction) {
            	((Instruction)updated).uses.add(this);
            } else if (updated instanceof Variable) {
				((Variable)updated).uses.add(this);
				((Variable)updated).def.uses.add(this);
            }
            if (original instanceof Instruction) {
            	((Instruction)original).uses.remove(this);
            } else if (original instanceof Variable) {
				((Variable)original).uses.remove(this);
				((Variable)original).def.uses.remove(this);
            }
        }
        if (this.arg2 == original && op != move) {
            this.arg2 = updated;
            if (updated instanceof Instruction) {
            	((Instruction)updated).uses.add(this);
            } else if (updated instanceof Variable) {
				((Variable)updated).uses.add(this);
            	((Variable)updated).def.uses.add(this);
            }
            if (original instanceof Instruction) {
            	((Instruction)original).uses.remove(this);
            } else if (original instanceof Variable) {
				((Variable)original).uses.remove(this);
				((Variable)original).def.uses.remove(this);
            }
        }
        if (instrsUsed != null) {
        	if (instrsUsed.length >= 1 && instrsUsed[0] == original)
        		instrsUsed[0] = (Instruction)updated;
        	if (instrsUsed.length >= 2 && instrsUsed[1] == original)
        		instrsUsed[1] = (Instruction)updated;
        }
    }

	public void setUsage(Value arg) {
		if (arg == arg2 && op == move) {
			// Do nothing.
		} else if (arg instanceof Variable) {
			((Variable) arg).usedIn(this);
			this.uses( (Variable) arg);
		} else if (arg instanceof Instruction) {
			((Instruction) arg).usedIn(this);
			this.uses( (Instruction) arg);
		}
	}

	public void setPrev(Instruction instr) {
		this.prev = instr;
	}

	public void setNext(Instruction instr) {
		this.next = instr;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	/* End setters. */

	// Return instruction that immediately dominates this instruction.
	// If none exists, return null.
	public Instruction dominatingInstr() {
		if (prev != null) {
			return prev; // Return the previous instruction.
		} else {         // Go up to the previous block and grab the last block.
			if (block.dominator != null) {
				return block.dominator.end;
			}
		}
		return null;
	}

	// True if this instruction is in a block that dominates the block
	// of the other instruction.
	public boolean isDominatorOf(Instruction other) {
		return block.isDominatorOf(other.block);
	}

	// True if the other instruction is in a block that dominates the block
	// of this instruction.
	public boolean isDominatedBy(Instruction other) {
		return block.isDominatedBy(other.block);
	}

	// Return the most immediately dominating instruction that is equivalent
	// to this instruction. If none exists, return null.
	public Instruction equivDominatingInstr() {
		Instruction current  = this;
		Instruction previous = this.sameOpDominator;
		while (previous != null) {
			if (current.equivalent(previous)) {
				return previous;
			}
			current = previous;
			previous = previous.sameOpDominator;
		}
		return null;
	}

	// Two instructions are equivalent if they have the same op code and
	// arguments. Exceptions: phi functions, deleted instructions.
	public boolean equivalent(Instruction other) {
		if (other == null) {
			return false;
		} else if (op != phi) {
			if (this.op == other.op && !this.deleted() && !other.deleted()) {
				if (this.arg1 == other.arg1 && this.arg2 == other.arg2) {
					return true;
				}
			}
		}
		return false;
	}

	// Convert all variables related to this instruction into 
	// pure instructions.
	public void varsToInstrs() {
		if (varDefd != null) {
			uses = varDefd.uses;
		}
		if (arg1 != null) {
			if (arg1 instanceof Variable) {
				arg1 = ((Variable) arg1).def;	
			}
		}
		if (arg2 != null) {
			if (arg2 instanceof Variable) {
				arg2 = ((Variable) arg2).def;
			}
		}
		if (params != null) {
			Value param;
			for (int i = 0; i < params.length; i++) {
				param = params[i];
				if (param instanceof Variable) {
					params[i] = ((Variable) param).def;
				}
			}
		}
		if (varsUsed != null) {
			if (instrsUsed == null) {
				instrsUsed = new Instruction[2];
			}
			for (int i = 0; i < 2; i++) {
				if (varsUsed[i] != null && instrsUsed[i] == null) {
					instrsUsed[i] = varsUsed[i].def;
				}
			}
		}
		//Sanity proof
		if (instrsUsed != null) {
			if (instrsUsed.length >= 1 && instrsUsed[0] != null)
				instrsUsed[0].uses.add(this);
			if (instrsUsed.length >= 2 && instrsUsed[1] != null)
				instrsUsed[1].uses.add(this);
		}
	}

	// Replace all instances of oldInstr seen by this instruction
	// with newInstr.
	public void replace(Instruction oldInstr, Value newValue) {
		if (arg1 != null) {
			if (arg1 == oldInstr) {
				arg1 = newValue;	
			}
		}
		if (arg2 != null) {
			if (arg2 == oldInstr) {
				arg2 = newValue;
			}
		}
		if (instrsUsed != null && newValue instanceof Instruction) {
			for (int i = 0; i < 2; i++) {
				if (instrsUsed[i] == oldInstr) {
					instrsUsed[i] = (Instruction) newValue;
				}
			}
		} else if (instrsUsed != null) {
			for (int i = 0; i < 2; i++) {
				if (instrsUsed[i] == oldInstr) {
					instrsUsed[i] = null;
				}
			}
		}
		((Instruction)oldInstr).uses.remove(this);
		if (newValue instanceof Instruction) {
			((Instruction)newValue).uses.add(this);
		}
	}

	public boolean usedInPhi() {
		for (Instruction usedIn : uses) {
			if (usedIn.op == phi) {
				return true;
			}
		}
		return false;
	}

	public void assignReg(int reg) {
		this.register = reg;
	}

	// Get the register assigned to this instruction.
	@Override
	public int getReg() {
		return register;
	}

	// Get the function associated with this instruction.
	// Only for call instructions.
	public Function getFunction() {
		if (op == call && arg1 instanceof Function) {
			return (Function) arg1;
		} else {
			Compiler.warning("No function associated with " + this);
			return null;
		}
	}

	/* Methods related to string representation of the instruction. */

	/* Output data about the instruction:
	 *		Containing block, variables used, variables defined.
	 */ 		
	public String dataToString() {
		String result = "";
		if (block != null) {
			result += "In block " + block.id + ".\n";
		} else {
			result += "No containing block.\n";
		}
		if (varDefd != null) {
			result += "Variable defined: " + varDefd.shortRepr() + ".\n";
		} else {
			result += "No variables defined.\n";
		}
		if (varsUsed != null) {
			result += "Variables used: ";
			for (Variable var : varsUsed) {
				if (var != null) {
					result += var.shortRepr() + " ";
				}
			}
			result += ".\n";
		} else {
			result += "No variables used.\n";
		}
		if (instrsUsed != null) {
			result += "Instructions used: ";
			for (Instruction instr : instrsUsed) {
				if (instr != null) {
					result += instr.shortRepr() + " ";
				}
			}
			result += ".\n";
		} else {
			result += "No instructions used.\n";
		}
		if (uses != null) {
			result += "Use sites: ";
			for (Instruction useSite: uses) {
				result += useSite.shortRepr() + " ";
			}
			result += ".\n";
		} else {
			result += "Never used by another instruction.";
		}
		return result;
	}


	@Override
	public String toString() {
		if (deleted) {
			return id + " deleted.";
		// } else if (op == call) {
		// 	return id + " : call " + arg1.shortRepr() + " on input " + Function.paramsToString(params);
		// } else if (op == phi) {
		// 	return id + " : PHI " + varDefd.shortRepr() + " := " + arg1.shortRepr() + " " + arg2.shortRepr();
		} else if (op == arrayStore) {
			return id + " : " + ops[op] + " " + arg1.shortRepr() + " " + arg2.shortRepr() + " " + Array.indicesToString(params);
		} else if (op == arrayLoad) {
			return id + " : " + ops[op] + " " + arg1.shortRepr() + " " + Array.indicesToString(params);
		} else if (numArgs == 2) {
			return id + " : " + ops[op] + " " + arg1.shortRepr() 
				   + " " + arg2.shortRepr();
		} else if (numArgs == 1) {
			return id + " : " + SSA.opToString(op) + " " + arg1.shortRepr();
		} else {
			return id + " : " + SSA.opToString(op);
		}
	}

	@Override
	public String shortRepr() {
		if (deleted) {
			return "(" + id + " del)";
		} else {
			return "(" + id + ")";
		}
	}

}