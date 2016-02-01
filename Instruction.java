/* File: Instruction.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class Instruction {
	// Class representing an instruction in intermediate form.

	public int op;            // Operation code.
	public Argument arg1;  	  // First argument.
	public Argument arg2;  	  // Second argument.
	public Instruction next;  // Next instruction.
	public int id;            // This instruction's ID number.

	private static int nextID = 0; // The next free ID number.

	public class Argument {
		// Argument can either be an instruction or a variable.
		// This hasn't been fully thought out.
		public int instrCode = 0;
		public int valueCode = 1;

		public int type;
		public Instruction instr;
		public int value;

		public Argument(int value) {
			this.value = value;
			this.type = valueCode;
		}

		public Argument(Instruction instr) {
			this.instr = instr;
			this.type = instrCode;
		}
	}

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
	public static int bne     = 14;
	public static int beq     = 15;
	public static int ble     = 16;
	public static int blt     = 17;
	public static int bge     = 18;
	public static int bgt     = 19;
   
	public static int read    = 20;
	public static int write   = 21;
	public static int writeNL = 22;
	/* End operation codes. */

	/* Different constructors for convenience. Maybe don't use these? */
	// public Instruction(int op, Instruction arg1, 
	// 	Instruction arg2, Instruction previous) {
	// 	initialize(op, new Argument(arg))
	// }

	// public Instruction(int op, Instruction arg1, Instruction arg2, Instruction previous) {
	// 	this(op, new Argument(arg1), new Argument(arg2), previous);
	// }

	// public Instruction(int op, int arg1, Instruction arg2, Instruction previous) {
	// 	this(op, new Argument(arg1), new Argument(arg2), previous);
	// }

	// public Instruction(int op, Instruction arg1, int arg2, Instruction previous) {
	// 	this(op, new Argument(arg1), new Argument(arg2), previous);
	// }

	// public Instruction(int op, int arg1, int arg2, Instruction previous) {
	// 	this(op, new Argument(arg1), new Argument(arg2), previous);
	// }

	public Instruction(int op, Argument arg1, Argument arg2, Instruction previous) {
		// Set op code and argument pointers.
		this.op = op;
		this.arg1 = arg1;
		this.arg2 = arg2;
		// Set id number.
		this.id = nextID;
		nextID++;
		// Set pointer to this instruction from previous instruction.
		previous.next = this;
	}
	/* End constructors. */

}