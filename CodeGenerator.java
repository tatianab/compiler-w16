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

	   Memory:
	   Memory is byte-addressed and has wordlength of 32 bits.

	   Runtime environment:

	   Program loader:
	   Object file has no header or footer.
	   End of program signaled by RET 0.

	   Instruction Formats:
	   Instructions are 32 bits and start with a 6-bit opcode.
	   F1 [6 op] [5 a] [5 b] [16 c]
	   F2 [6 op] [5 a] [5 b] [11] [5 c]
	   F3 [6 op] [26 c]
	 */
	IntermedRepr       program;
	ArrayList<Integer> byteCode;
	int[]              finalByteCode;

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

	// Deal with function and procedure calls.
	public void generateFunction() {
		// Procedure prologue
		// PSH R31 SP -4 : Return address
		// PSH FP SP -4  : Old FP
		// ADD FP R0 -4  : FP = SP
		// SUBI SP SP n  : Reserve space for locals.

		// Need to deal with globals, locals, parameters etc...

		// Need to store return value at some point.

		// Procedure epilogue
		// ADD SP 0 FP
		// POP FP SP 4
		// POP R31 SP 4+params
		// RET 0 0 31
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