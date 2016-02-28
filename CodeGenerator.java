/* File: RegAllocator.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
package compiler-w16;
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

	public generateCode() {
		// Generate some code!
	}

	public void runGeneratedCode() {
		if (finalByteCode != null) {
			DLX.load(finalByteCode);
			DLX.execute();
		} else {
			System.out.println("Program not finished. Cannot be run.")
		}
	}

	// Signal the end of the program.
	public void endProgram() {
		addInstruction(DLX.RET, 0);
		byteCode.toArray(new int[] finalByteCode[byteCode.size()]);
	}

	// Add instruction to the progam.
	public int addInstruction(int op, int a, int b, int c) {
		byteCode.add(DLX.assemble(op, a, b, c));
	}

	public int addInstruction(int op, int a, int c) {
		byteCode.add(DLX.assemble(op, a, c));
	}

	public int addInstruction(int op, int c) {
		byteCode.add(DLX.assemble(op, c));
	}

	// Generate native program string.
	public String byteCodeToString() {
		result = "";
		for (int instr : byteCode) {
			result += instr.toString() + "/n";
		}
		return result;
	}

	// Generate assembly program string.
	public String assemblyToString() {
		result = "";
		for (int instr : byteCode) {
			result += DLX.disassemble(instr) + "/n";
		}
		return result;
	}
}