/* File: RegAllocator.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.ArrayList;
import java.util.HashMap;

public class RegAllocator {
	/* The register allocators's job is allocate registers for the compiler.
	   In particular, it:
	   - Tracks the live ranges of individual values and creates an interference graph.
	   - Colors the interference graph (assuming 8 general purpose registers).
	   - Eliminates phi instructions and inserts move instructions.
	   - Displays the final result using VCG.
	 */
    
    public static int numberOfRegister = 8;
    
	IntermedRepr program;
	InterferenceGraph interferenceGraph;

	public RegAllocator(IntermedRepr program) {
		this.program = program;
	}

	public IntermedRepr allocateRegisters() {
		this.interferenceGraph = program.createInterferenceGraph();
		colorInterferenceGraph();
		program.elimiatePhi();
		return program;
	}

	/* Tracking live ranges / interference graph.

	 */
    public class LiveRange {
        public Instruction definition;
        public ArrayList<Instruction> usage;
        public LiveRange() {
            usage = new ArrayList<Instruction>();
        }
        public void setDefinition(Instruction def) {
            definition = def;
        }
        public void addUsage(ArrayList<Instruction> instr) {
            usage.addAll(instr);
        }
    }
    public LiveRange variableLiveRange(Variable value) {
        LiveRange range = new LiveRange();
        for (Block block: program.blocks) {
            if (range.definition == null) {
                //Not found definition, look for the instruction
                Instruction def = block.instructionsWithDefVariable(value);
                if (def != null) {
                    range.setDefinition(def);
                }
            }
            if (range.definition != null) {
                ArrayList<Instruction> usages = block.instructionsWithUsageOfVariable(value);
                range.addUsage(usages);
            }
        }
        return range;
    }

	/* Interference graph coloring. 

	 */
    public class RegisterConf {
        public int registerNumber;
        public Instruction begin;
        public Instruction end;
    }
    public class VariableAllocation {
        public Variable variable;
        public ArrayList<RegisterConf> registers;
    }
    
    public class memorySpace {
        //This is relative to the stack and frame pointer
        public class memoryPosition {
            public int address;
            public int size;
            public int count;
            public Value value;
        }
        private int memoryHead;
        private int memoryTail;
        private int dataHead;
        private int dataTail;
        public memoryPosition reserveArray(int count) {
            int size = 32;
            int beginAddr = dataTail - size;
            memoryPosition pos = new memoryPosition;
            pos.address = beginAddr;
            pos.size = size;
            pos.count = 1;
            return pos;
        }

        public HashMap<instructionValue, memoryPosition> preserve;

        memorySpace() {
            preserve = new HashMap<instructionValue, memoryPosition>();
        }

        public void store(instructionValue value, memoryPosition position) {
            preserve.put(value, position);
        }

        public memoryPosition reserve() {
            int size = 32;
            int beginAddr = dataHead + size;
            memoryPosition pos = new memoryPosition;
            pos.address = beginAddr;
            pos.size = size;
            pos.count = 1;
            return pos;
        }
        public void release(memoryPosition) {
            //Doing nothing
        }
    }

    public class instructionValue {
        Instruction basedInstr;
        //Usage count means how many instructions use this value
        int usageCount;
        //Reference count means how many times the instruction being used as an argument
        int referenceCount;
        //Usage count means how many instructions use this value
        int upcomingUsageCount;
        //Reference count means how many times the instruction being used as an argument
        int upcomingReferenceCount;

        public void instructionCalled(Instruction instr) {
            boolean use = false;
            if (instr.arg1 == basedInstr) {
                use = true;
                referenceCount--;
                upcomingReferenceCount--;
            }
            if (instr.arg2 == basedInstr) {
                use = true;
                referenceCount--;
                upcomingReferenceCount--;
            }
            if (use) {
                usageCount--;
                upcomingUsageCount--;
            }
        }
        public void instructionAddedToBuffer(Instruction instr) {
            boolean use = false;
            if (instr.arg1 == basedInstr) {
                use = true;
                upcomingReferenceCount++;
            }
            if (instr.arg2 == basedInstr) {
                use = true;
                upcomingReferenceCount++;
            }
            if (use) {
                upcomingUsageCount++;
            }
        }
        public boolean stillNeeded() {
            return referenceCount>0;
        }
        public boolean flushable() {
            return upcomingReferenceCount == 0;
        }
    }

    public class Register {
        public instructionValue currentValue;
        public memoryPosition backendPosition;
        private memorySpace memSpace;
        Register(memSpace) {
            this.memSpace = memSpace;
        }
        public void updateValue(memoryPosition valueMem) {
            preserveMemory();
            backendPosition = valueMem;
        }
        public void updateValue(instructionValue instr) {
            //Store the instruction dependency
            preserveMemory();
            currentValue = instr;
            backendPosition = null;
        }
        public preserveMemory() {
            //4 case: 
            //Memory being used? 
            //value still needed? 
            if (currentValue.stillNeeded() ) {
                if (backendPosition == null)
                    backendPosition = memSpace.reserve
                memSpace.store(currentValue, backendPosition);
            } else if (backendPosition != null) {
                memSpace.release(backendPosition);
            }
        }
    }

    public class registerContext {
        public Register registers[];
        public ArrayList<instructionValue> availableContents;
        registerContext() {
            registers = new Register[numberOfRegisterAvailable];
        }
    }

    public class dependencyGraph {
        public class dependencyNode {
            public Instruction instr;
            public ArrayList<dependencyNode> child;
            public int dependCount;
            dependencyNode(registerContext ctx, Instruction instr) {
                this.instr = instr;
                dependCount = 0;
                if (Instruction.getClass().isInstance(instr.arg1) && ) 
                    dependCount++;
            }
        }
        public ArrayList<Instruction> opening;
        public ArrayList
        public void addInstruction
    }
    
    public ArrayList<VariableAllocation> allocation() {
        int numberOfRegisterAvailable = numberOfRegister-2;
        memorySpace memSpace = new memorySpace;
        Variable registers[] = new Register[numberOfRegisterAvailable];
        for (int i = 0; i < numberOfRegisterAvailable; i++)
            registers[i] = null;
        //TODO: pick the variable that is last to use in the upcoming instructions (first use first priority), for all variables
        //Schedule method: get all the upcoming instuctions in the block, add dependency, add at much instruction as possible. When a variable free up, import another
        while (true) {

        }
    }

    public class AssemblyBlock {
        public ArrayList<Instruction> instructions;
    }

    public AssemblyBlock allocateBlock(Block block, registerContext context) {
        Block pointer = block.begin;

        while (true) {
            if (availableInstructions)
        }
    }

	/* Elimination of phi instructions (replace with move instructions).

	 */

}