/* File: RegAllocator.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
import java.util.ArrayList;

public class CodeGenerator {
	/* The code generator's responsibility is to emit native programs in DLX format
	   after optimizations and register allocation.

	   Information about DLX processor

	   Registers:
	   32 general purpose 32-bit registers R0 - R31.
	   R0 always contains 0.
	   Branch instructions to subroutinges implicitly use R31 to store return address.

	   Program loader:
	   Object file has no header or footer.
	   End of program signaled by RET 0.

	 */
	IntermedRepr       program;
	ArrayList<Integer> byteCode;
	int[]              finalByteCode;
	boolean            debug;

	public static final int ZERO     = 0;  // R0 always stores the value 0.
	public static final int SCRATCH  = 1;  // R1 is the scratch register to hold intermediate values.
	public static final int FP       = 29; // Frame pointer is R29.
	public static final int SP       = 28; // Stack pointer is R28.
	public static final int GLOBALS  = 30; // R30 stores a pointer to global vars.
	public static final int RET_ADDR = 31; // R31 is used to store the return address of subroutines. 

	public CodeGenerator(IntermedRepr program, boolean debug) {
		this.program  = program;
		this.byteCode = new ArrayList<Integer>();
		this.debug    = debug;
	}

	public void generateCode() {
		// Generate some code!
		setUpMemory();
		ArrayList<Instruction> instrs = program.instrs; // This should be changed to whatever the interface ends up being.
		for (Instruction instr : instrs) {
			addInstruction(instr);
		}
		// // Print 42 for now.
		// addInstruction(DLX.ADDI, 3, 0, 42);
		// addInstruction(DLX.WRD, 3);
		// endProgram();
	}

	public void runGeneratedCode() {
		try {
			DLX.load(finalByteCode);
			DLX.execute();
			System.out.println();
		} catch (Exception e) {
			Compiler.error("Program could not be run.");
		}
	}

	// Allocate space for global variables and
	// set the stack pointer and frame pointer for
	// the main function.
	public void setUpMemory() {
		// Allocate space for global variables.
		addInstruction(DLX.ADDI, GLOBALS, ZERO, (DLX.MemSize / 4) - 4);
		// Set up stack pointer and frame pointer.
		addInstruction(DLX.SUBI, SP, GLOBALS, program.getNumGlobals() * 4);
		addInstruction(DLX.ADDI, FP, SP, 0); // Set SP == FP.
	}

	// Signal the end of the program.
	public void endProgram() {
		addInstruction(DLX.RET, 0);
		finalByteCode = new int[byteCode.size()];
		int i = 0;
		for (int instr : byteCode) {
			finalByteCode[i] = instr;
			i++;
		}
	}

	public class DlxOp {
		int op;
		Integer a;
		Integer b;
		Integer c;
		boolean fixUp;

		// We need a way to branch to the proper place.
	}

	// Add instruction to the program.
	public void addInstruction(int op, int a, int b, int c) {
		byteCode.add(DLX.assemble(op, a, b, c));
	}

	public void addInstruction(int op, int a, int c) {
		byteCode.add(DLX.assemble(op, a, c));
	}

	public void addInstruction(int op, int c) {
		byteCode.add(DLX.assemble(op, c));
	}

	public void addInstruction(int op) {
		byteCode.add(DLX.assemble(op));
	}

	public void addInstruction(Instruction instr) {
		if (debug) { System.out.println("Translating instruction " + instr); }
		switch(instr.op) {
			case Instruction.phi:
				Compiler.error("No phis allowed!");
				return;
			case Instruction.end:
				endProgram();
				return;
			case Instruction.call:
				generateFunction(instr);
				return;
			default:
				break;
		}

		if (generateComputation(instr) || generateBranch(instr) || generateLoadStore(instr) || generateIO(instr)) {
			// We compiled the given instruction.
		} else {
			Compiler.error("Invalid instruction " + instr);
		}
	}

	// Deal with intructions like add, mul, sub, div etc.
	// Return true if successful.
	public boolean generateComputation(Instruction instr) {
		// Add 16 to opcode to get immediate instruction.
		int dlxOp, immOp;
		int dstReg = instr.getReg(); // Where the result goes.
		Value arg1 = instr.arg1;
		Value arg2 = instr.arg2;

		// TODO: what if arg1 or arg2 are stored in memory rather than registers?

		switch(instr.op) {
			case Instruction.neg:
				dlxOp = DLX.SUB;
				arg2  = arg1;
				arg1  = new Constant(0);
				break;
			case Instruction.add:
				dlxOp = DLX.ADD;
				break;
			case Instruction.adda:
				dlxOp = DLX.ADD;
				// TODO: handle address adding
				break;
			case Instruction.sub:
				dlxOp = DLX.SUB;
				break;
			case Instruction.mul:
				dlxOp = DLX.MUL;
				break;
			case Instruction.div:
				dlxOp = DLX.DIV;
				break;
			case Instruction.cmp:
				dlxOp = DLX.CMP;
				break;
			default: 
				return false;
		}

		immOp = dlxOp + 16; // The immediate operation.

		if (arg1.getReg() >= 0 && arg2.getReg() >= 0) {                    // Both args have registers.
			if (debug) { System.out.println("Computation: Neither arg constant.");}
			addInstruction(dlxOp, dstReg, arg1.getReg(), arg2.getReg());
		} else if (arg1 instanceof Constant && arg2 instanceof Constant) { // Neither have registers.
			if (debug) { System.out.println("Computation: Both args constant.");}
			addInstruction(DLX.ADDI, SCRATCH, ZERO, arg1.getVal());
			addInstruction(immOp, dstReg, SCRATCH, arg2.getVal());
		} else if (arg1 instanceof Constant) {                             // Just first has a register.
			if (debug) { System.out.println("Computation: 1st arg constant.");}
			addInstruction(DLX.ADDI, SCRATCH, ZERO, arg1.getVal());
			addInstruction(dlxOp, dstReg, SCRATCH, arg2.getReg());
		} else if (arg2 instanceof Constant) {                             // Just second has a register.
			if (debug) { System.out.println("Computation: 2nd arg constant.");}
			addInstruction(immOp, dstReg, arg1.getReg(), arg2.getVal());
		} else {
			Compiler.error("Invalid arguments to instruction " + instr);
		}

		return true;
	}

	public boolean generateBranch(Instruction instr) {
		int dlxOp;
		Value compare = instr.arg1;
		Value jumpTo  = instr.arg2;

		switch (instr.op) {
			case Instruction.bne:
				dlxOp = DLX.BNE;
				break;
			case Instruction.beq:
				dlxOp = DLX.BEQ;
				break;
			case Instruction.bge:
				dlxOp = DLX.BGE;
				break;
			case Instruction.blt:
				dlxOp = DLX.BLT;
				break;
			case Instruction.bgt:
				dlxOp = DLX.BGT;
				break;
			case Instruction.ble:
				dlxOp = DLX.BLE;
				break;
			case Instruction.bra:
				dlxOp = DLX.RET; // ?
				compare = null;
				jumpTo = instr.arg1;
				break;
			default:
				return false;
		}

		if (compare != null) {
			if (compare instanceof Constant) {

			} else {

			}
		} else {
			// Unconditional branch
		}
		return true;

	}

	public boolean generateLoadStore(Instruction instr) {
		// This currently only works for move instructions.
		// And it has a bug of some kind
		int dlxOp;
		int dstReg = instr.getReg();
		int srcReg = -1;
		switch(instr.op) {
			case Instruction.load :
				// DLX.LDW R.a = Mem[R.b + c]
				// DLX.LDX R.a = Mem[R.b + R.c]
				break;
			case Instruction.store:
				// DLX.STW, DLX.STX
				break;
			case Instruction.move :
				// load then store
				// or add dst 0 src
			    srcReg = instr.arg1.getReg();
				break;
			default:
				return false;
		}

		if (instr.arg1 instanceof Constant) {
			addInstruction(DLX.ADDI, SCRATCH, ZERO, instr.arg1.getVal());
			srcReg = SCRATCH;
		}
		addInstruction(DLX.ADD, dstReg, ZERO, srcReg);
		return true;
	}

	public boolean generateIO(Instruction instr) {
		int dlxOp;
		Value arg = null;
		switch(instr.op) {
			case Instruction.read   :
				dlxOp = DLX.RDI;
				arg   = instr;
				break;
			case Instruction.write  :
				dlxOp = DLX.WRD;
				arg   = instr.arg1;
				break;
			case Instruction.writeNL:
				dlxOp = DLX.WRL;
				addInstruction(dlxOp);
				return true;
			default:
				return false;
		}

		int reg = arg.getReg();

		if (arg instanceof Constant) {
			addInstruction(DLX.ADDI, SCRATCH, ZERO, arg.getVal());
			reg = SCRATCH;
		}
		addInstruction(dlxOp, reg);
		return true;
	}
	// Deal with function and procedure calls.
	public void generateFunction(Instruction instr) {
		// // Function function = instr.getFunction();
		// int n = 0;      // Needs to be set to the amount of space needed for locals.
		// int params = function.getNumParams() * OFFSET; // Space for params.

		// // Procedure prologue
		// // PSH R31 SP -4 : Return address
		// addInstruction(DLX.PSH, 31, SP, -4);
		// // PSH FP SP -4  : Old FP
		// addInstruction(DLX.PSH, FP, SP, -4);
		// // ADD FP R0 -4  : FP = SP
		// addInstruction(DLX.ADD, FP, 0, -4);
		// // SUBI SP SP n  : Reserve space for locals.
		// addInstruction(DLX.SUBI, SP, SP, n);

		// // Function call (store RP in R31)
		// addInstruction(DLX.JSR, function.firstInstructionAddr());

		// // Need to deal with globals, locals, parameters etc...

		// // Need to store return value at some point.

		// // Procedure epilogue
		// // ADD SP 0 FP
		// addInstruction(DLX.ADD, SP, 0, FP);
		// // POP FP SP 4
		// addInstruction(DLX.POP, FP, SP, 4);
		// // POP R31 SP 4+params
		// addInstruction(DLX.POP, 31, SP, 4 + params);
		// // RET 0 0 31
		// addInstruction(DLX.RET, 0, 0, 31);
	}

	// Generate native program string.
	// Mostly just to look cool because nobody can read it!
	public String byteCodeToString() {
		String result = "";
		for (int instr : byteCode) {
			result += Integer.toBinaryString(instr) + "\n";
		}
		return result;
	}

	// Generate assembly program string.
	public String assemblyToString() {
		String result = "";
		int i = 0;
		for (int instr : byteCode) {
			result += i + " : " + DLX.disassemble(instr);
			i += 4;
		}
		return result;
	}

	public String memoryToString() {
		return memoryToString(DLX.MemSize / 4);
	}

	public String memoryToString(int truncate) {
		String result = "Memory \n";
		if (truncate > (DLX.MemSize / 4)) { truncate = (DLX.MemSize / 4); }
		for (int i = 0; i < finalByteCode.length; i++) {
			result += (i * 4) + " : " + DLX.disassemble(DLX.M[i]);
		}
		for (int i = finalByteCode.length * 4; i < truncate; i += 4) {
			result += i + " : " + DLX.M[i];
			if (i == DLX.R[GLOBALS]) {
				result += " // Begin global variables";
			}
			if (i == DLX.R[FP]) {
				result += " // FP";
			} 
			if (i == DLX.R[SP]) {
				result += " // SP";
			}
			result += "\n";
		}
		return result;
	}

	public String registersToString() {
		String result = "Registers \n";
		for (int i = 0; i < 32; i++) {
			result += "R" + i + " : " + DLX.R[i];
			if (i == ZERO) {
				result += " // Always 0";
			} else if (i == SCRATCH) {
				result += " // Scratch register"; 
			} else if (i == FP) {
				result += " // FP";
			} else if (i == SP) {
				result += " // SP";
			} else if (i == GLOBALS) {
				result += " // Pointer to globals";
			} else if (i == RET_ADDR) {
				result += " // Pointer to return address";
			}
			result += "\n";
		}
		return result;
	}
}