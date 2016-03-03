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

	public static final int ZERO     = 0;  // R0 always stores the value 0.
	public static final int SCRATCH  = 1;  // R1 is the scratch register to hold intermediate values.
	public static final int FP       = 29; // Frame pointer is R29.
	public static final int SP       = 28; // Stack pointer is R28.
	public static final int GLOBALS  = 30; // R30 stores a pointer to global vars.
	public static final int RET_ADDR = 31; // R31 is used to store the return address of subroutines. 

	public CodeGenerator(IntermedRepr program) {
		this.program = program;
		this.byteCode = new ArrayList<Integer>();
	}

	public void generateCode() {
		// Generate some code!

		// Print 42 for now.
		addInstruction(DLX.ADDI, 3, 0, 42);
		addInstruction(DLX.WRD, 3);
		endProgram();
	}

	public void runGeneratedCode() {
		try {
			DLX.load(finalByteCode);
			DLX.execute();
		} catch (Exception e) {
			Compiler.error("Program could not be run.");
		}
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

	public void addInstruction(Instruction instr) {
		int ssaOp, dlxOp, a, b, c;
		ssaOp = instr.op;

		switch(ssaOp) {
			case Instruction.adda    :
				break;
			case Instruction.phi     :
				Compiler.error("No phis allowed!");
				break;
			case Instruction.end     :
				endProgram();
				break;
			case Instruction.call    :
				generateFunction(instr);
				break;
			default:
				break;
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

		if (arg1.getReg() >= 0 && arg1.getReg() >= 0) {                    // Both args have registers.
			addInstruction(dlxOp, dstReg, arg1.getReg(), arg2.getReg());
		} else if (arg1 instanceof Constant && arg2 instanceof Constant) { // Neither have registers.
			addInstruction(immOp, SCRATCH, arg1.getVal());
			addInstruction(immOp, dstReg, SCRATCH, arg2.getVal());
		} else if (arg1 instanceof Constant) {                             // Just first has a register.
			addInstruction(DLX.ADDI, SCRATCH, ZERO, arg2.getVal());
			addInstruction(dlxOp, dstReg, SCRATCH, arg2.getReg());
		} else if (arg2 instanceof Constant) {                             // Just second has a register.
			addInstruction(immOp, dstReg, arg1.getReg(), arg2.getVal());
		} else {
			Compiler.error("Invalid arguments to instruction " + instr);
		}

		return true;
	}

	public void generateBranch(Instruction instr) {

		switch (instr.op) {
			case Instruction.bne:
				// DLX.BNE
				break;
			case Instruction.beq:
				// DLX.BEQ
				break;
			case Instruction.bge:
				// DLX.BGE
				break;
			case Instruction.blt:
				// DLX.BLT
				break;
			case Instruction.bgt:
				// DLX.BGT
				break;
			case Instruction.ble:
				// DLX.BLE
				break;
			case Instruction.bra:
				// DLX.RET ?
				break;
			default:
				break;
		}

	}

	public void generateLoadStore(Instruction instr) {
		switch(instr.op) {
			case Instruction.load :
				// DLX.LDW, DLX.LDX
				break;
			case Instruction.store:
				// DLX.STW, DLX.STX
				break;
			case Instruction.move :
				break;
		}
	}

	public void generateIO(Instruction instr) {
		switch(instr.op) {
			case Instruction.read   :
				// DLX.RDD
				break;
			case Instruction.write  :
				// DLX.WRD
				break;
			case Instruction.writeNL:
				// DLX.WRL
				break;
			default:
				break;
		}
	}

	// Deal with function and procedure calls.
	public void generateFunction(Instruction instr) {
		int n = 0;      // Needs to be set to the amount of space needed for locals.
		int params = 0; // Space for params.

		// Procedure prologue
		// PSH R31 SP -4 : Return address
		addInstruction(DLX.PSH, 31, SP, -4);
		// PSH FP SP -4  : Old FP
		addInstruction(DLX.PSH, FP, SP, -4);
		// ADD FP R0 -4  : FP = SP
		addInstruction(DLX.ADD, FP, 0, -4);
		// SUBI SP SP n  : Reserve space for locals.
		addInstruction(DLX.SUBI, SP, SP, n);

		// Need to deal with globals, locals, parameters etc...

		// Need to store return value at some point.

		// Procedure epilogue
		// ADD SP 0 FP
		addInstruction(DLX.ADD, SP, 0, FP);
		// POP FP SP 4
		addInstruction(DLX.POP, FP, SP, 4);
		// POP R31 SP 4+params
		addInstruction(DLX.POP, 31, SP, 4 + params);
		// RET 0 0 31
		addInstruction(DLX.RET, 0, 0, 31);
	}

	// Generate native program string.
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
		for (int instr : byteCode) {
			result += DLX.disassemble(instr);
		}
		return result;
	}
}