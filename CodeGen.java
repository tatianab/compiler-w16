/* File: CodeGen.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
import java.util.ArrayList;

public class CodeGen {
  /* Reorganized code generator.
	   The code generator's responsibility is to emit native programs in DLX
     format after optimizations and register allocation.

	   Information about DLX processor

	   Registers:
	   32 general purpose 32-bit registers R0 - R31.
	   R0 always contains 0.
	   Branch instructions to subroutines implicitly use R31 to store return address.

	   Program loader:
	   Object file has no header or footer.
	   End of program signaled by RET 0.

	 */
	IntermedRepr        program;
	InstructionSchedule schedule; // The instructions to translate.
	ArrayList<DlxOp>    dlxOps;   // The DLX instructions we have generated so far.
	int[]               byteCode;
	boolean             debug;

	int nextDlxPos;
	Instruction currentInstruction;
	Block       currentBlock;

	// TODO: associate assembly instructions with containing block and function.

  // Reserved registers.
	public static final int ZERO     = 0;  // R0 always stores the value 0.
	public static final int SCRATCH  = 1;  // R1 is the scratch register to hold intermediate values.
	public static final int SCRATCH2 = 2;  // R2 is the second scratch register.
  public static final int SCRATCH3 = 3;  // R3 is the third scratch register.
	public static final int FP       = 29; // Frame pointer is R29.
	public static final int SP       = 28; // Stack pointer is R28.
	public static final int GLOBALS  = 30; // R30 stores a pointer to global vars.
	public static final int RET_ADDR = 31; // R31 is used to store the return address of subroutines.

	// The first and last free general purpose registers.
	public static final int FIRST_FREE_REG = 4;
	public static final int LAST_FREE_REG  = FIRST_FREE_REG + 8;

	public static final int BYTES_IN_WORD = 4;

	public CodeGenerator(IntermedRepr program, InstructionSchedule schedule, boolean debug) {
		this.program  = program;
		this.schedule = schedule;
		this.dlxOps   = new ArrayList<DlxOp>();
		this.debug    = debug;
		this.nextDlxPos = 0;
	}

	public void generateCode() {
		// Generate some code!
		setUpMemory();
		// This should be changed to whatever the interface ends up being.
		ArrayList<Instruction> instrs = program.instrs;
		for (Instruction instr : instrs) {
			currentInstruction = instr;
			if (instr.prev == null) {
				currentBlock = instr.block;
			}
			addInstruction(instr);
		}
		endProgram();
		// // Print 42 for now.
		// addInstruction(DLX.ADDI, 3, 0, 42);
		// addInstruction(DLX.WRD, 3);
		// endProgram();
	}

	public void runGeneratedCode() {
		try {
			DLX.load(byteCode);
			DLX.execute(-1);
			System.out.println();
		} catch (Exception e) {
			Compiler.error("Program could not be run.");
		}
	}

	// Runs the code up to breakPoint, an (assembly) instruction #.
	public void runGeneratedCode(int breakPoint) {
		try {
			DLX.load(byteCode);
			DLX.execute(breakPoint);
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
		addInstruction(DLX.ADDI, GLOBALS, ZERO, (DLX.MemSize / BYTES_IN_WORD) - BYTES_IN_WORD);
		// Set up stack pointer and frame pointer.
		addInstruction(DLX.SUBI, SP, GLOBALS, program.getNumGlobals() * BYTES_IN_WORD);
		addInstruction(DLX.ADDI, FP, SP, 0); // Set SP == FP.
	}

	// Signal the end of the program.
	public void endProgram() {
		fixBranches();
		byteCode = new int[dlxOps.size()];
		int i = 0;
		for (DlxOp instr : dlxOps) {
			byteCode[i] = getNativeInstr(instr);
			i++;
		}
	}

	public void fixBranches() {
		for (DlxOp dlxOp : dlxOps) {
			if (dlxOp.branch != null) {
				if (dlxOp.branch.dlxPos != null) {
					dlxOp.setOffset(dlxOp.branch.dlxPos - dlxOp.pos);
				} else {
					dlxOp.setOffset(0);
					Compiler.warning("No offset available for block " +
						dlxOp.branch.shortRepr());
				}
			}
		}
	}

	public class DlxOp {
		public int op;
		public int pos; // Where in the final program this instr will live.
		public Integer arg1;
		public Integer arg2;
		public Integer arg3;
		public Block branch;

		// Auxillary info for debugging.
		public Instruction instr; // The corresponding SSA instr.
		public Block       block; // If this is the beginning of a block.

		public DlxOp(int op, Integer arg1, Integer arg2, Integer arg3) {
			this.op     = op;
			this.arg1   = arg1;
			this.arg2   = arg2;
			this.arg3   = arg3;
			this.branch = null;
			this.pos    = nextDlxPos;
			nextDlxPos++;
			this.instr  = currentInstruction;
			if (currentBlock != null) {
				this.block = currentBlock;
				currentBlock = null;
			} else {
				this.block = null;
			}
		}

		// This instruction will need to be fixed once it is known where
		// the block to jump to lives.
		public DlxOp(int op, Integer compare, Block branch) {
			this.op = op;
			this.arg1   = compare;
			this.arg2   = null;
			this.arg3   = null;
			this.branch = branch;
			this.pos    = nextDlxPos;
			nextDlxPos++;
			this.instr  = currentInstruction;
			if (currentBlock != null) {
				this.block = currentBlock;
				currentBlock = null;
			} else {
				this.block = null;
			}
		}

		public void setOffset(int offset) {
			if (this.arg1 == null) {
				this.arg1 = offset;
			} else {
				this.arg2 = offset;
			}
		}

		@Override
		public String toString() {
			return DLX.mnemo[op] + " " + arg1 + " " + arg2 + " " + arg3;
		}
	}

	// Add instruction to the program.
	public void addInstruction(int op, int a, int b, int c) {
		dlxOps.add(new DlxOp(op, a, b, c));
	}

	public void addInstruction(int op, int a, int c) {
		dlxOps.add(new DlxOp(op, a, c, null));
	}

	public void addInstruction(int op, int c) {
		dlxOps.add(new DlxOp(op, c, null, null));
	}

	public void addInstruction(int op) {
		dlxOps.add(new DlxOp(op, null, null, null));
	}

	public int getNativeInstr(DlxOp instr) {
		if (debug) { System.out.println("Generating native code for " + instr + "(" + instr.instr + ")"); }
		int op = instr.op;
		Integer arg1, arg2, arg3;
		arg1 = instr.arg1;
		arg2 = instr.arg2;
		arg3 = instr.arg3;

		if (arg1 == null) {
			return DLX.assemble(op);
		} else if (arg2 == null) {
			return DLX.assemble(op, arg1);
		} else if (arg3 == null) {
			return DLX.assemble(op, arg1, arg2);
		} else {
			return DLX.assemble(op, arg1, arg2, arg3);
		}
	}

	public void addInstruction(Instruction instr) {
    // This should be the only function where the type of the given instruction //matters.
		if (debug) { System.out.println("Translating instruction " + instr); }
		// If the instruction is the first in its block, the position
		// of this instruction will mark the beginning of the block's code.
		int op = instr.op;

		if (instr.prev == null) {
			instr.block.dlxPos = nextDlxPos;
		}
		switch(op) {
			case Instruction.phi:
				Compiler.warning("No phis allowed!");
				return;
			case Instruction.end:
				addInstruction(DLX.RET, 0);
				return;
			case Instruction.call:
				generateFunction(instr);
				return;
			default:
				break;
		}

		if (op >= Instruction.neg && op <= Instruction.adda) {
			generateComputation(instr);
		} else if (op >= Instruction.load && op <= Instruction.move) {
			generateLoadStore(instr);
		} else if (op == Instruction.bra || (op >= Instruction.bne && op <= Instruction.ble)) {
			generateBranch(instr);
		} else if (op >= Instruction.read && op <= Instruction.writeNL) {
			generateIO(instr);
		} else if (op == Instruction.arrayStore || op == Instruction.arrayLoad) {
			generateArrayOp(instr);
		} else {
			Compiler.error("Invalid instruction " + instr);
		}
	}

  public int loadValue(int value, int type, int reg) {
    // If the value is a constant
    if (type == CONSTANT) {
      addInstruction(DLX.ADDI, reg, ZERO, value);
      return reg;
    }
    // Else if the value is a virtual register
    else if (isVirtual(value)) {
      addInstruction(DLX.LDW, reg, GLOBALS, getOffset(value));
      return reg;
    }
    // Else: the value is a physical register.
    else {
      return value;
    }
  }

  public void storeValue(int virtual, int physical) {
    // If virtual is the same as physical, do nothing.
    // Otherwise, store value held in physical reg in virtual reg.
    if (virtual != physical) {
      addInstruction(DLX.STW, physical, GLOBALS, getOffset(virtual));
    }
  }

  public int getOffset(int virtual) {

  }

  public boolean isVirtual(int register) {

  }

  public void inputOutput(int ssaOp, int arg, int argType) {
    int dlxOp = getDlxOp(ssaOp);
    int reg;

    // Write new line.
    if (dlxOp == DLX.WRL) {
      addInstruction(dlxOp);
      return;
    }

    // If the argument is a register.

    // If the argument is a constant, load it into scratch.

    // Emit the instruction.

    // Load
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
        addInstruction(dlxOp);
      default:
        // Nothing
    }

    int reg = arg.getReg();

    if (arg instanceof Constant) {
      addInstruction(DLX.ADDI, SCRATCH, ZERO, arg.getVal());
      reg = SCRATCH;
    }
    addInstruction(dlxOp, reg);
    return true;
  }

  public void computation(int ssaOp, int dest, int arg1, int arg1Type, int arg2, int arg2Type) {
    int destReg, arg1Reg, arg2Reg;
    int dlxOp = getDlxOp(ssaOp);

    // If the destination is a virtual register, put the final result in a scratch register which will later be stored to memory.
    if (isVirtual(dest)) {
      destReg = SCRATCH3;
    } else {
      destReg = dest;
    }

    // If the first argument is a constant, load it into a scratch register.
    if (arg1Type == CONSTANT) {
      addInstruction(DLX.ADDI, SCRATCH, ZERO, arg1);
      arg1Reg = SCRATCH;
    }
    // Else if the first argument is in a virtual register, load it into a scratch register.
    else if (arg1Type == REGISTER && isVirtual(arg1)) {
      // Load into SCRATCH
      addInstruction(DLX.LDW, SCRATCH, GLOBALS, getOffset(arg1));
      arg1Reg = SCRATCH;
    }
    // Otherwise, use the physical register.
    else {
      arg1Reg = arg1;
    }

    arg1Reg = loadValue(arg1, arg1Type, SCRATCH);
    arg2Reg = loadValue(arg2, arg2Type, SCRATCH2);
    // TODO: fix logic
    // If the second argument is a constant, use the immediate operator.
    if (arg2Type == CONSTANT) {
      addInstruction(dlxOp + IMM_OFFSET, destReg, arg1Reg, arg2);
      return;
    }
    // Else if the second argument is in a virtual register, load it into (the second) scratch register.
    else if (arg2Type == REGISTER && isVirtual(arg2)) {
      addInstruction(DLX.LDW, SCRATCH2, GLOBALS, getOffset(arg2));
      arg1Reg = SCRATCH2;
    }
    // Otherwise, use the physical register.
    else {
      arg2Reg = arg2;
    }

    // Emit computation instruction.
    addInstruction(dlxOp, destReg, arg1Reg, arg2Reg);

    // Store result to memory if necessary.
    if (isVirtual(dest)) {
      addInstruction(DLX.STW, destReg, GLOBALS, getOffset(dest));
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

		int regArg1, regArg2; // The registers where the first and second arguments live.

		// If arg1 is a constant
			// Load arg1 into a register
		// Otherwise, if arg1 is an instruction
			// Get arg1's register
			// If it is a virtual register
				// Load into a physical register

		// Same process for arg2

		// If arg2 is a constant

		// TODO: what if arg1 or arg2 are stored in memory rather than registers?

		// Determine the operation code.
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

	public void addInstruction(int op, int a, Block branch) {
		dlxOps.add(new DlxOp(op, a, branch));
	}

	public boolean generateBranch(Instruction instr) {
		int dlxOp;
		Value compare = instr.arg1;
		int compareReg = compare.getReg();
		Block jumpTo  = (Block) instr.arg2;

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
				dlxOp = DLX.BEQ; // Unconditional branch.
				compare = (Value) null;
				compareReg = ZERO;
				jumpTo = (Block) instr.arg1;
				break;
			default:
				return false;
		}


		if (compare instanceof Constant) {
				addInstruction(DLX.ADDI, SCRATCH, ZERO, compare.getVal());
				addInstruction(dlxOp, SCRATCH, jumpTo);
		} else {
				addInstruction(dlxOp, compareReg, jumpTo);
		}
		return true;

	}

	public boolean generateLoadStore(Instruction instr) {
		// This currently only works for move instructions.
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

	public void generateArrayOp(Instruction instr) {
		// TODO
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
		Function function = instr.getFunction();
		int n = 0;      // Needs to be set to the amount of space needed for locals.
		int params = function.getNumParams() * BYTES_IN_WORD; // Space for params.

		// Procedure prologue
		addInstruction(DLX.PSH, RET_ADDR, SP, - BYTES_IN_WORD); // Store return address.
		addInstruction(DLX.PSH, FP, SP, - BYTES_IN_WORD);       // Store old frame pointer.
		addInstruction(DLX.SUBI, FP, ZERO, BYTES_IN_WORD);      // Set FP = SP.
		addInstruction(DLX.SUBI, SP, SP, n);                    // Reserve space for local vars.

		// Function call (store RP in R31)
		// addInstruction(DLX.JSR, function.firstInstructionAddr());

		// Need to deal with globals, locals, parameters etc...

		// Need to store return value at some point.

		// Procedure epilogue
		addInstruction(DLX.ADD, SP, ZERO, FP);       		           // Set SP = FP.
		addInstruction(DLX.POP, FP, SP, BYTES_IN_WORD);                // Restore frame pointer.
		addInstruction(DLX.POP, RET_ADDR, SP, BYTES_IN_WORD + params); // Restore return address.
		addInstruction(DLX.RET, RET_ADDR);							   // Jump to return address.
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
		int j = 0;
		for (int instr : byteCode) {
			if (dlxOps.get(j).block != null) {
				result += "\nBlock " + dlxOps.get(j).block.shortRepr() + ":\n";
			}
			result +=  String.format("%0$-20s", i + " : " + DLX.disassemble(instr));
			//result +=  i + " : " + DLX.disassemble(instr);
			if (dlxOps.get(j).instr != null) {
				result += " // " + dlxOps.get(j).instr + "\n";
			} else {
				result += "\n";
			}
			i += BYTES_IN_WORD;
			j++;
		}
		return result;
	}

	public String memoryToString() {
		return memoryToString(DLX.MemSize / BYTES_IN_WORD);
	}

	public String memoryToString(int truncate) {
		String result = "Memory \n";
		if (truncate > (DLX.MemSize / BYTES_IN_WORD)) { truncate = (DLX.MemSize / BYTES_IN_WORD); }
		for (int i = 0; i < byteCode.length; i++) {
			result += (i * BYTES_IN_WORD) + " : " + DLX.disassemble(DLX.M[i]);
		}
		for (int i = byteCode.length * BYTES_IN_WORD; i < truncate; i += BYTES_IN_WORD) {
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
