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
    public static int numberOfReverse = 5;
    //One for return address
    //One for stack pointer
    //One for frame pointer
    //One for heap pointer
    //One for memory address calculation
    
	IntermedRepr program;
	InterferenceGraph interferenceGraph;

    public RegAllocator() {
    }

	public RegAllocator(IntermedRepr program) {
		this.program = program;
	}

	public IntermedRepr allocateRegisters() {
        dumbRegAlloc();
		// this.interferenceGraph = program.createInterferenceGraph();
		// colorInterferenceGraph();
		// program.elimiatePhi();
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
                Instruction def = null;// = block.instructionsWithDefVariable(value);
                if (def != null) {
                    range.setDefinition(def);
                }
            }
            if (range.definition != null) {
                //ArrayList<Instruction> usages;// = block.instructionsWithUsageOfVariable(value);
                //range.addUsage(usages);
            }
        }
        return range;
    }

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
            memoryPosition pos = new memoryPosition();
            pos.address = beginAddr;
            pos.size = size;
            pos.count = 1;
            return pos;
        }

        public HashMap<InstructionSchedule.InstructionValue, memoryPosition> preserve;

        memorySpace() {
            preserve = new HashMap<InstructionSchedule.InstructionValue, memoryPosition>();
        }

        public void store(InstructionSchedule.InstructionValue value, memoryPosition position) {
            preserve.put(value, position);
        }

        public memoryPosition reserve() {
            int size = 32;
            int beginAddr = dataHead + size;
            memoryPosition pos = new memoryPosition();
            pos.address = beginAddr;
            pos.size = size;
            pos.count = 1;
            return pos;
        }
        public void release(memoryPosition pos) {
            //Doing nothing
        }

        //Load and save of the value
        public InstructionSchedule.outputInstruction load(int regNo, memoryPosition position) {
            InstructionSchedule c = new InstructionSchedule();
            InstructionSchedule.outputInstruction loadInstr = c.new outputInstruction();
            loadInstr.op = Instruction.load;
            loadInstr.arg1 = numberOfRegister-2;
            loadInstr.constant2 = position.address;
            loadInstr.outputReg = regNo;
            return loadInstr;
        }
        public InstructionSchedule.outputInstruction save(int regNo, memoryPosition position) {
            InstructionSchedule c = new InstructionSchedule();
            InstructionSchedule.outputInstruction storeInstr = c.new outputInstruction();
            storeInstr.op = Instruction.store;
            storeInstr.arg1 = numberOfRegister-2;
            storeInstr.constant2 = position.address;
            storeInstr.outputReg = regNo;
            return storeInstr;
        }
    }

    public class Register {
        public final int registerID;
        public InstructionSchedule.InstructionValue currentValue;
        public memorySpace.memoryPosition backendPosition;
        private memorySpace memSpace;
        Register(int regID, memorySpace memSpace) {
            registerID = regID;
            this.memSpace = memSpace;
        }
        public void updateValue(memorySpace.memoryPosition valueMem) {
            preserveMemory();
            backendPosition = valueMem;
        }
        public void updateValue(InstructionSchedule.InstructionValue instr) {
            //Store the instruction dependency
            preserveMemory();
            currentValue = instr;
            backendPosition = null;
        }
        public InstructionSchedule.outputInstruction preserveMemory() {
            //4 case: 
            //Memory being used? 
            //value still needed? 
            if (currentValue.stillNeeded() ) {
                if (backendPosition == null)
                    backendPosition = memSpace.reserve();
                memSpace.store(currentValue, backendPosition);
            } else if (backendPosition != null) {
                memSpace.release(backendPosition);
            }
            InstructionSchedule.outputInstruction storeInstr = null;
            if (memSpace != null) {
                storeInstr = memSpace.save(registerID, backendPosition);
            }
            return storeInstr;
        }
        public int releaseRate() {
            return currentValue.score();
        }
    }

    public class registerContext {
        public Register registers[];
        public ArrayList<InstructionSchedule.InstructionValue> availableContents;
        registerContext() {
            registers = new Register[numberOfRegister];
            for (int i = 1; i < numberOfRegister-numberOfReverse; i++) {
                registers[i] = new Register(i, null);
            }
        }
        registerContext(registerContext copy) {
            registers = new Register[numberOfRegister];
            for(int i = 1; i < numberOfRegister; i++) {
                registers[i] = copy.registers[i];
            }
        }
        boolean hasSpace() {
            return emptyRegister() > 0;
        }
        int emptyRegister() {
            for (int i = 1; i < numberOfRegister-numberOfReverse; i++) {
                if (registers[i] == null)
                    return i;
            }
            return -1;
        }
        ArrayList<Integer> registerToFlushTo() {
            //If there is no space anymore, flush the register to the memory in the following order: 
            //1. All register that won't be used in the current block
            //2. All register that has a high write amplification: more register will be used in this block to store the result before the register is released (Any instruction with a score higher than 1)
            Integer firstChoice = -1;       Integer secondChoice = -1;
            Integer firstChoiceRate = -1;   Integer secondChoiceRate = -1;
            for (int i = 1; i < numberOfRegister-numberOfReverse; i++) {
                Register reg = registers[i];
                if (reg.releaseRate() < firstChoiceRate) {
                    if (reg.releaseRate() > secondChoiceRate) {
                        secondChoice = i;
                        secondChoiceRate = reg.releaseRate();
                    }
                } else  {
                    firstChoice = i;
                    firstChoiceRate = reg.releaseRate();
                }
            }
            ArrayList<Integer> list = new ArrayList<Integer>();
            list.add(firstChoice);
            list.add(secondChoice);
            return list;
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
                //if (Instruction.getClass().isInstance(instr.arg1) && ) 
                    //dependCount++;
            }
        }
        public ArrayList<Instruction> opening;
        //public ArrayList
        //public void addInstruction
    }

	/* Elimination of phi instructions (replace with move instructions).

	 */

}