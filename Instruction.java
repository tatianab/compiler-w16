/* File: Instruction.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class Instruction extends Value {
	// Class representing an instruction in intermediate form.

	private boolean deleted;  // True if this instruction has been deleted.

	public int op;            // Operation code.
	public Value arg1;  	  // First argument, or only argument.
	public Value arg2;  	  // Second argument.

	public Instruction prev;  // Previous instruction, or null if first in block.
	public Instruction next;  // Next instruction, or null if end of block.

	public Block block;       // The block where this instruction lives.
	public int id;            // This instruction's ID number. Represents time of
							  // creation rather than position in program.

	public Variable[] varsUsed; // Variables used in this instruction, up to two.
	public Variable   varDefd;  // Variables defined in this instrcution, up to 1.

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
	/* End operation codes. */

	private static String[] ops = new String[]{null, "neg","add","sub","mul","div",
												"cmp","adda","load","store","move","phi","end","bra",
												"read","write","writeNL", null, null, null,
												"bne","beq","bge","blt","bgt","ble"};
	/* End operation codes. */

	/* Constructor. */
	public Instruction(int id) {
		this.id = id;
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

	/* Setters. */

	public void setOp(int op) {
		this.op = op;
	}

	public void setArgs(Value arg1, Value arg2) {
		this.arg1 = arg1;
		this.arg2 = arg2;

		if (arg1 instanceof Variable) {
			((Variable) arg1).usedIn(this);
			this.uses( (Variable) arg1);
		}
		if (arg2 instanceof Variable) {
			((Variable) arg2).usedIn(this);
			this.uses( (Variable) arg2);
		}
	}

	public void setArgs(Value arg) {
		if (arg1 != null) {
			this.arg2 = arg;
		}
		else {
			this.arg1 = arg;
			this.arg2 = null;
		}
		
		if (arg instanceof Variable) {
			((Variable) arg).usedIn(this);
			this.uses( (Variable) arg);
		}
	}

	public void setDefn(Variable var, Value expr) {
		this.op = move;
		this.arg1 = expr;
		this.arg2 = var;
		this.varDefd = var;
		var.def = this;
		if (expr instanceof Variable) {
			this.uses((Variable) expr);
		}
        
        //Add instruction to the map
        if (block != null) {
            block.addReturnValue(var);
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

	@Override
	public String toString() {
		if (deleted) {
			return id + " deleted.";
		} else if (arg1 != null && arg2 != null) {
			return id + " : " + ops[op] + " " + arg1.shortRepr() 
				   + " " + arg2.shortRepr() + "\n";
		} else if (arg1 != null) {
			return id + " : " + ops[op] + " " + arg1.shortRepr() + "\n";
		} else {
			return id + " : " + ops[op] + "\n";
		}
	}

	@Override
	public String shortRepr() {
		if (deleted) {
			return "(" + id + "-- del).";
		} else {
			return "(" + id + ")";
		}
	}

}