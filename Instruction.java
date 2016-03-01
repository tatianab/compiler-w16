/* File: Instruction.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.LinkedList;

public class Instruction extends Value {
	// Class representing an instruction in intermediate form.

	private boolean deleted;  // True if this instruction has been deleted.
	public boolean visited;   // Used for searching algorithms.

	public int op;            // Operation code.
	public Value arg1;  	  // First argument, or only argument.
	public Value arg2;  	  // Second argument.
	public Value[] params;    // For call instructions only.

	public Instruction prev;  // Previous instruction, or null if first in block.
	public Instruction next;  // Next instruction, or null if end of block.

	public Block block;       // The block where this instruction lives.
	public int id;            // This instruction's ID number. Represents time of
							  // creation rather than position in program.

	public Variable[] varsUsed; // Variables used in this instruction, up to two.
	public Variable   varDefd;  // Variables defined in this instruction, up to 1.

	public Instruction sameOpDominator; // The most immediate dominating instruction with the
										// same op code as this instruction.

	public Instruction[] instrsUsed; // The instructions used by this one.
	LinkedList<Instruction> uses;    // The instructions that use the result of this instruction.

	public int register; // The register that the value of this instruction is assigned to.

	/* Operation codes. */
	public static int neg     = 1;
	public static int add     = 2;
	public static int sub     = 3;
	public static int mul     = 4;
	public static int div     = 5;
	public static int cmp     = 6;
   
	public static int adda    = 7;
	public static int load    = 8;
	public static int store   = 9;
	public static int move    = 10;
	public static int phi     = 11;
   
	public static int end     = 12;
	public static int bra     = 13;
	
	public static int read    = 14;
	public static int write   = 15;
	public static int writeNL = 16;

	public static int bne     = 20;
	public static int beq     = 21;
	public static int bge     = 22;
	public static int blt     = 23;
	public static int bgt     = 24;
	public static int ble     = 25;

	public static int call    = 30;
	/* End operation codes. */

	private static String[] ops = new String[]{null, "neg","add","sub","mul","div",
												"cmp","adda","load","store","move","phi","end","bra",
												"read","write","writeNL", null, null, null,
												"bne","beq","bge","blt","bgt","ble", null, null, null, null, "call"};
	/* End operation codes. */

	/* Constructor. */
	public Instruction(int id) {
		this.id = id;
		this.block    = null;
		this.varDefd  = null;
		this.varsUsed = null;
		this.prev     = null;
		this.next     = null;
		// this.instrsUsed = new Instruction[2];
		this.uses     = new LinkedList<Instruction>();
	}

	public void delete() {
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
        if (this.arg1 == original) {
            this.arg1 = updated;
        }
        if (this.arg2 == original && op != move) {
            this.arg2 = updated;
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
		} else if (op == call) {
			return id + " : call " + arg1.shortRepr() + " on input " + Function.paramsToString(params);
		} else if (op == phi) {
			return id + " : PHI " + varDefd.shortRepr() + " := " + arg1.shortRepr() + " " + arg2.shortRepr();
		} else if (arg1 != null && arg2 != null) {
			return id + " : " + ops[op] + " " + arg1.shortRepr() 
				   + " " + arg2.shortRepr();
		} else if (arg1 != null) {
			return id + " : " + ops[op] + " " + arg1.shortRepr();
		} else {
			return id + " : " + ops[op];
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