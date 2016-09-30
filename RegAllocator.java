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

    public class phiRequest {
        Instruction value1, value2;
        InstructionSchedule.InstructionValue actualStmt;
    }

    public class phiMergerResult {
        registerContext resultContext;
        ArrayList<InstructionSchedule.outputInstruction> edge1;
        ArrayList<InstructionSchedule.outputInstruction> edge2;
    }

    //While style merge
    public static void phiPassover(Block senderBlock, ArrayList<phiRequest> requests) {
        //Just update all the elements into the same thing
        //Update the value of the register to the phi functions, and drop all the variable that is no longer needed
        for (phiRequest request : requests) {

            Instruction instr1 = request.value1;
            Instruction instr2 = request.value2;
						if (Compiler.debug) {
            	System.out.println("Request Value: "+request.actualStmt.basedInstr);
					  }
            RegAllocator r = new RegAllocator();
            if (instr1.block.id <= senderBlock.id) {
                //It is from sender block 1 side
                //Just move the value
                if (instr1.state.storage.currentRegister != null) {
                    //There is a register, so update it
                    int regID = instr1.state.storage.currentRegister.registerID;
                    senderBlock.schedule.context.registers[regID].phiValue(request.actualStmt);
                    request.actualStmt.basedInstr.state.storage.currentRegister = instr1.state.storage.currentRegister;
                }
                if (instr1.state.storage.backstore != null) {
                    //There is a memory position, so update it
                    instr1.state.storage.backstore.identity = r.new memoryID(request.actualStmt.basedInstr);
                    request.actualStmt.basedInstr.state.storage.backstore = instr1.state.storage.backstore;
                }
            } else {
                //It is from sender block 2 side
                //Just move the value
                if (instr2.state.storage.currentRegister != null) {
                    //There is a register, so update it
                    int regID = instr2.state.storage.currentRegister.registerID;
                    senderBlock.schedule.context.registers[regID].phiValue(request.actualStmt);
                    request.actualStmt.basedInstr.state.storage.currentRegister = instr2.state.storage.currentRegister;
                }
                if (instr2.state.storage.backstore != null) {
                    //There is a position, so update it
                    instr2.state.storage.backstore.identity = r.new memoryID(request.actualStmt.basedInstr);
                    request.actualStmt.basedInstr.state.storage.backstore = instr2.state.storage.backstore;
                }
            }
            //Phi has been handle, so just release
            for (Instruction child: request.actualStmt.basedInstr.uses) {
                //A depended value is calculated->add it back
                child.state.unresolveArgument--;
            }
        }

    }

    public static void freezePhiForReg(registerContext ctx, ArrayList<phiRequest> requests) {
        for (Register r: ctx.registers) {
            if (r.currentValue != null && requestForValue(requests, r.currentValue) == null) {
                //It has a value, not in
                r.currentValue.phiCounter++;
            }
        }
    }

    static phiRequest requestForValue(ArrayList<phiRequest>requests, InstructionSchedule.InstructionValue value) {
        for (phiRequest request:requests) {
            if (request.value1 == value.basedInstr || request.value2 == value.basedInstr || request.actualStmt == value)
                return request;
        }
        return null;
    }

    static int regIDForValue(registerContext ctx, InstructionSchedule.InstructionValue value) {
        for (Register reg:ctx.registers) {
            if (reg.currentValue == value)
                return reg.registerID;
        }
        return -1;
    }



    //a2: inner
    //b: join, after phi transform
    //requests: phi
    //Only edge 1 will has instruction, result context would be the same
    //Transfer a2 back to b
    public static phiMergerResult phiTransform(registerContext a2, registerContext b, memorySpace space, ArrayList<phiRequest> requests) {
        //For while loop
        //We have to transform register context a2 back into b, with 100% correctness

        ArrayList allInstr = new ArrayList();
        for (Register reg: b.registers) {
            if (reg.currentValue != null)
                allInstr.add(reg.currentValue.basedInstr);
        }

        InstructionSchedule insch = new InstructionSchedule();
        InstructionSchedule.clearContext(allInstr);
        InstructionSchedule .setContext(a2);

        RegAllocator r = new RegAllocator();
        phiMergerResult merger = r.new phiMergerResult();
        merger.edge1 = new ArrayList<InstructionSchedule.outputInstruction>();

        //First stage: make all the register value into phi-ed version, in case the phi function changed it
        for (Register reg:a2.registers) {
            if (reg.currentValue != null && b.registers[reg.registerID].currentValue != null) {
                phiRequest req = requestForValue(requests, reg.currentValue);
                if (req != null) {
                    //Transform back to phi
                    reg.currentValue = req.actualStmt;
                }
            }
        }

        //Second stage: swap register until all existed value aligned
        for (Register reg:b.registers) {
            int actualID = regIDForValue(a2, reg.currentValue);
            //If a2 contains the value, aID > 0
            //If aID is not the register ID in reg->need to swap
            if (reg.currentValue != null && actualID > 0 && reg.registerID != actualID) {
                //Need to move to actual ID
                ArrayList<InstructionSchedule.outputInstruction> ois = a2.swapRegister(actualID, reg.registerID);
                merger.edge1.addAll(ois);
            }
        }

        //Third stage: if it does not existed in b, but in a2, load it
        for (Register reg: b.registers) {
            //Load value that is not there
            if (a2.registers[reg.registerID].currentValue != reg.currentValue){
                if (reg.currentValue != null) {
                    //The inner loop has the higher block id
                /*phiRequest request = requestForValue(requests, reg.currentValue);
                Instruction instrVal = (request.value1.block.id > request.value2.block.id)?request.value1:request.value2;
                InstructionSchedule.outputInstruction oi = a2.registers[reg.registerID].updateValue(instrVal.state.valueRepr);*/
                    Register src = reg;
                    Register dst = a2.registers[reg.registerID];
                    ArrayList<InstructionSchedule.outputInstruction> oi = src.currentValue.basedInstr.state.storage.load(dst, space);
                    if (oi != null) {
                        merger.edge1.addAll(oi);
                    }
                } else {
                    //Register in a2 is empty, but it's not null in a2, so preserve
                    InstructionSchedule.outputInstruction oi = a2.registers[reg.registerID].preserveMemory();
                    if (oi != null) {
                        merger.edge1.add(oi);
                    }
                }
            }

        }
        for (Register reg:a2.registers) {
            if (reg.currentValue != null && b.registers[reg.registerID].currentValue != null) {
                phiRequest req = requestForValue(requests, reg.currentValue);
                if (req != null) {
                    //Transform back to phi
                    System.out.println(req);
                    reg.currentValue = req.actualStmt;
                }
            }
        }

        merger.resultContext = a2;

        //Cancel phi state counter
        for (Register reg: b.registers) {
            if (reg.currentValue != null) {
                reg.currentValue.phiCounter--;
            }
        }

        return merger;
    }

    //If style merge
    public static phiMergerResult phiMerger(registerContext a1, registerContext a2, ArrayList<phiRequest> requests) {
        InstructionSchedule insch = new InstructionSchedule();

        RegAllocator r = new RegAllocator();
        phiMergerResult merger = r.new phiMergerResult();
        registerContext mergedCtx = r.new registerContext(a1.space);

        ArrayList<InstructionSchedule.outputInstruction> edge1Instr = new ArrayList<InstructionSchedule.outputInstruction>();
        ArrayList<InstructionSchedule.outputInstruction> edge2Instr = new ArrayList<InstructionSchedule.outputInstruction>();

        for (phiRequest request : requests) {
            //Handle case
            Instruction instr1 = request.value1;
            Instruction instr2 = request.value2;
            Register reg1 = instr1.state.storage.currentRegister;
            Register reg2 = instr2.state.storage.currentRegister;
            if (reg1 != null && reg2 != null) {
                //Both values are loaded in the register
                if (reg1.registerID == reg2.registerID) {
                    //If the two values are loaded in the same register, just register to the returning context
                    if (mergedCtx.registers[reg1.registerID].isAvailable()) {
                        mergedCtx.registers[reg1.registerID].updateValue(request.actualStmt);
                    } else {
                        //No space, so reallocate
                        int regID = mergedCtx.emptyRegister();
                        mergedCtx.registers[regID].updateValue(request.actualStmt);

                        InstructionSchedule.outputInstruction instr = insch.new outputInstruction();
                        instr.op = Instruction.move;
                        instr.arg1 = reg1.registerID;
                        instr.outputReg = regID;
                        edge1Instr.add(instr);

                        instr = insch.new outputInstruction();
                        instr.op = Instruction.move;
                        instr.arg1 = reg1.registerID;
                        instr.outputReg = regID;
                        edge2Instr.add(instr);
                    }
                } else {
                    if (mergedCtx.registers[reg1.registerID].isAvailable()) {
                        //Test if register 1 is available
                        InstructionSchedule.outputInstruction instr = insch.new outputInstruction();
                        instr.op = Instruction.move;
                        instr.arg1 = reg2.registerID;
                        instr.outputReg = reg1.registerID;
                        edge2Instr.add(instr);
                        mergedCtx.registers[reg1.registerID].updateValue(request.actualStmt);
                    } else if (mergedCtx.registers[reg2.registerID].isAvailable()) {
                        //If not, try register 2
                        InstructionSchedule.outputInstruction instr = insch.new outputInstruction();
                        instr.op = Instruction.move;
                        instr.arg1 = reg1.registerID;
                        instr.outputReg = reg2.registerID;
                        edge1Instr.add(instr);
                        mergedCtx.registers[reg2.registerID].updateValue(request.actualStmt);
                    } else {
                        //If not, allocate a space
                        //No space, so reallocate
                        int regID = mergedCtx.emptyRegister();
                        mergedCtx.registers[regID].updateValue(request.actualStmt);
                        InstructionSchedule.outputInstruction instr = insch.new outputInstruction();
                        instr.op = Instruction.move;
                        instr.arg1 = reg1.registerID;
                        instr.outputReg = regID;
                        edge1Instr.add(instr);
                        edge2Instr.add(instr);
                    }
                }
            } else if (reg1 == null && reg2 == null) {
                //Both value are loaded in the memory, so assert the memory address is the
                assert(instr1.state.storage.backstore.address == instr2.state.storage.backstore.address);
            } else {
                //One in, one's not
                //So just store
                if (reg1 != null) {
                    //It is not store from register 1
                    //So just store
                    //TODO
                }
            }
            //Phi has been handle, so just release
            for (Instruction child: request.actualStmt.basedInstr.uses) {
                //A depended value is calculated->add it back
                child.state.unresolveArgument--;
            }
            request.actualStmt.basedInstr.state.scheduled();

            if (request.value1 != null)
                request.value1.state.valueRepr.instructionCalled(request.actualStmt.basedInstr);

            if (request.value2 != null)
                request.value2.state.valueRepr.instructionCalled(request.actualStmt.basedInstr);
        }
        merger.resultContext = mergedCtx;
        merger.edge1 = edge1Instr;
        merger.edge2 = edge2Instr;
        return merger;
    }

    public static int numberOfRegister = 9;
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
        for (Instruction instr: program.instrs) {
            instr.phiLinkage(new ArrayList());
        }
	}

	public InstructionSchedule allocateRegisters() {
        //Set up states
        createStates();
        // dumbRegAlloc();
		// this.interferenceGraph = program.createInterferenceGraph();
		// colorInterferenceGraph();
		// program.elimiatePhi();
        InstructionSchedule schedule = new InstructionSchedule(program);
        //Patch bra instruction
        for (Instruction instr : program.instrs) {
            if (instr.op == Instruction.bra) {
                //It's a branch
                Block destinationBlock = (Block)instr.arg1;
                Block sourceBlock = instr.block;
                sourceBlock.schedule.next1 = destinationBlock.schedule;
            }
        }
		return schedule;
	}

    public void createStates() {
        for (Instruction instr : program.instrs) {
            instr.state = new InstructionState(instr);

        }
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

    public class memoryID {
        public String name;
        public Block identityBlock;
        memoryID(Instruction instr) {
            name = instr.varDefd.ident;
            identityBlock = instr.block.rootBlock;
        }
    }

    public class memorySpace {
        //This is relative to the stack and frame pointer
        public class memoryPosition {
            public int address;
            public int size;
            public int count;
            public memoryID identity;
            public InstructionSchedule.InstructionValue actualValue;
        }
        private int memoryHead;
        private int memoryTail;
        private int dataHead;
        private int dataTail;
        public memorySpace upperSpace;
        public memoryPosition reserveArray(int count) {
            int size = 32;
            int beginAddr = dataTail - size;
            memoryPosition pos = new memoryPosition();
            pos.address = beginAddr;
            pos.size = size;
            pos.count = 1;
            return pos;
        }

        public HashMap<memoryPosition, InstructionSchedule.InstructionValue> preserve;

        public InstructionSchedule.InstructionValue fetchEqualvalent(InstructionSchedule.InstructionValue value) {
            for (InstructionSchedule.InstructionValue val: preserve.values()) {
                if (val.basedInstr.memorySpaceEqual(value.basedInstr)) { return val; }
            }
            return null;
        }

        //Memory listing management
        public InstructionSchedule.InstructionValue fetchExistedValue(InstructionSchedule.InstructionValue val) {
            for (InstructionSchedule.InstructionValue _val: preserve.values()) {
                if (_val.basedInstr.memorySpaceEqual(val.basedInstr))
                    return _val;
            }
            if (upperSpace == null) return null;
            else return upperSpace.fetchExistedValue(val);
        }

        public memoryPosition fetchMemorySpace(InstructionSchedule.InstructionValue val) {
            InstructionSchedule.InstructionValue baseValue = fetchExistedValue(val);
            if (baseValue == null) {
                //Not registered, so create a space
                memoryPosition pos = reserve();
                store(val, pos);
                return pos;
            }
            for (memoryPosition pos: preserve.keySet()) {
                if (preserve.get(pos) == baseValue)
                    return pos;
            }
            if (upperSpace == null) return null;
            else return upperSpace.fetchMemorySpace(val);
        }

        public InstructionSchedule.InstructionValue fetchValue(memoryPosition pos) {
            InstructionSchedule.InstructionValue val = preserve.get(pos);
            if (val == null && upperSpace != null) val = upperSpace.fetchValue(pos);
            return val;
        }

        public void releaseMemorySpace(InstructionSchedule.InstructionValue val) {
            memoryPosition pos = fetchMemorySpace(val);
            preserve.remove(pos);
            if (upperSpace != null) upperSpace.releaseMemorySpace(val);
        }

        memorySpace() {
            preserve = new HashMap<memoryPosition, InstructionSchedule.InstructionValue>();
        }
        memorySpace(memorySpace previousSpace) {
            preserve = new HashMap<memoryPosition, InstructionSchedule.InstructionValue>();
            upperSpace = previousSpace;
            //Sub space creation
            memoryHead = upperSpace.dataHead;
            memoryTail = upperSpace.dataTail;
            dataHead = memoryHead;
            dataTail = memoryTail;
        }

        public void store(InstructionSchedule.InstructionValue value, memoryPosition position) {
            preserve.put(position, value);
        }

        public memoryPosition reserve() {
            int size = 4;
            int beginAddr = dataHead;
            dataHead += size;
            memoryPosition pos = new memoryPosition();
            pos.address = beginAddr;
            pos.size = size;
            pos.count = 1;
            return pos;
        }
        public void release(memoryPosition pos) {
            //Relase from the pool, that's it. The position will not be reused for debug purpose
            preserve.remove(pos);
            //The position will be reused once the stack is set back to frame
            //Assuming infinite (or very large memory space)
        }

        //Load and save of the value
        public ArrayList<InstructionSchedule.outputInstruction> load(Register reg, memoryPosition position) {
            int regNo = reg.registerID;
            InstructionSchedule.InstructionValue val = fetchValue(position);
            //Update the value to the register
            InstructionSchedule.outputInstruction saveOi = reg.updateValue(val);
            reg.updateValue(position);

            //Update the instruction
            val.basedInstr.state.storage.currentRegister = reg;

            //Generate corresponding instructions
            InstructionSchedule c = new InstructionSchedule();
            InstructionSchedule.outputInstruction loadInstr = c.new outputInstruction();
            loadInstr.op = Instruction.load;
            loadInstr.arg1 = numberOfRegister-2;
            loadInstr.constant2 = position.address;
            loadInstr.outputReg = regNo;
            ArrayList<InstructionSchedule.outputInstruction>instrs = new ArrayList<>();
            if (saveOi != null) instrs.add(saveOi);
            if (loadInstr != null) instrs.add(loadInstr);
            return instrs;
        }
        public InstructionSchedule.outputInstruction save(int regNo, memoryPosition position, InstructionSchedule.InstructionValue currentValue) {
            position.actualValue = currentValue;
            InstructionSchedule c = new InstructionSchedule();
            InstructionSchedule.outputInstruction storeInstr = c.new outputInstruction();
            storeInstr.op = Instruction.store;
            storeInstr.arg1 = numberOfRegister-2;
            storeInstr.constant2 = position.address;
            storeInstr.outputReg = regNo;

            return storeInstr;
        }
    }
    static int regWriteCount = 0;
    public int fetchWriteCount() { return regWriteCount++; }

    public class Register {
        public final int registerID;
        public InstructionSchedule.InstructionValue currentValue;
        public memorySpace.memoryPosition backendPosition;
        private memorySpace memSpace;
        int versioning = -1;
        Register(int regID, memorySpace memSpace) {
            registerID = regID;
            this.memSpace = memSpace;

        }
        Register(Register copy) {
            registerID = copy.registerID;
            this.memSpace = copy.memSpace;
            this.currentValue = copy.currentValue;
            backendPosition = copy.backendPosition;
            versioning = copy.versioning;
        }
        public boolean isAvailable() {
            return currentValue == null || currentValue.usageCount <= 0;
        }
        public InstructionSchedule.outputInstruction phiValue(InstructionSchedule.InstructionValue instr) {
            //Store the instruction dependency
            InstructionSchedule.outputInstruction release = null;

            Register instrReg = instr.basedInstr.state.storage.currentRegister;
            memorySpace.memoryPosition pos = instr.basedInstr.state.storage.backstore;

            if (instrReg != null) {
                //Check if the register is the same, only move if not
                if (instrReg.registerID != this.registerID) {
                    //Move
                    InstructionSchedule i = new InstructionSchedule();
                    release = i.new outputInstruction();
                    release.op = Instruction.move;
                    release.arg1 = instrReg.registerID;
                    release.outputReg = this.registerID;
                }
            }
            currentValue = instr;
            backendPosition = null;
            //Update the instruction value record
            instr.basedInstr.state.storage.currentRegister = this;
						if (Compiler.debug) {
            	System.out.println("Update register: "+registerID+" with value "+currentValue);
						}
            return release;
        }
        //Read from memory
        public void updateValue(memorySpace.memoryPosition valueMem) {
            backendPosition = valueMem;
        }
        //Read from instruction / Save from onstruction
        public InstructionSchedule.outputInstruction updateValue(InstructionSchedule.InstructionValue instr) {
            versioning = fetchWriteCount();
            //Store the instruction dependency
            InstructionSchedule.outputInstruction release = preserveMemory();
            currentValue = instr;
            backendPosition = null;
            //Update the instruction value record
            if (currentValue != null) {
                instr.basedInstr.state.storage.currentRegister = this;
                if (Compiler.debug) {
                    System.out.println("Update register: " + registerID + " with value " + currentValue);
                }
            }
            return release;
        }
        public InstructionSchedule.outputInstruction preserveMemory() {
            //4 case:
            //Memory being used?
            //value still needed?
            if (currentValue == null) {
                //No instruction, so do nothing
                return null;
            }
            boolean needToStore = false;
            if (currentValue.stillNeeded() ) {
                //if (backendPosition == null)
                //    backendPosition = memSpace.reserve();
                //memSpace.store(currentValue, backendPosition);
                backendPosition = memSpace.fetchMemorySpace(currentValue);
                if (backendPosition.actualValue != currentValue) {
                    needToStore = true;
                }
                //Else, the value is in the memory
            } else if (backendPosition != null) {
                //No longer needed, stored backend position
                memSpace.releaseMemorySpace(currentValue);
                backendPosition = null;
                //memSpace.release(backendPosition);
            }
            InstructionSchedule.outputInstruction storeInstr = null;
            if (needToStore) {
                storeInstr = memSpace.save(registerID, backendPosition, currentValue);
            }
            Instruction instr = currentValue.basedInstr;
            instr.state.storage.backstore = backendPosition;
            instr.state.storage.currentRegister = null;
            //After preserve, it's nothing
            currentValue = null;
            backendPosition = null;
            return storeInstr;
        }
        public int releaseRate() {
            return versioning;//currentValue.score();
        }
        @Override public String toString() {
            String result = registerID + ": ";
            if (currentValue != null) result += currentValue.basedInstr;
            else result += "null";
            return result+"\n";
        }
    }

    public class registerContext {
        public Register registers[];
        public memorySpace space;
        registerContext(memorySpace _space) {
            space = _space;
            registers = new Register[numberOfRegister];
            for (int i = 0; i < numberOfRegister; i++) {
                registers[i] = new Register(i, space);
            }
        }
        registerContext(registerContext copy) {
            registers = new Register[numberOfRegister];
            for(int i = 0; i < numberOfRegister; i++) {
                registers[i] = new Register(copy.registers[i]);
            }
            space = copy.space;
        }
        //One for return address
        //One for stack pointer
        //One for frame pointer
        //One for heap pointer
        //One for memory address calculation
        public Register memoryOpRegister() {
            return registers[numberOfRegister-numberOfReverse];
        }
        public Register heapPtrRegister() {
            return registers[numberOfRegister-numberOfReverse+1];
        }
        public Register framePtrRegister() {
            return registers[numberOfRegister-numberOfReverse+2];
        }
        public Register stackPtrRegister() {
            return registers[numberOfRegister-numberOfReverse+3];
        }
        public Register returnAddrRegister() {
            return registers[numberOfRegister-numberOfReverse+4];
        }
        ArrayList<InstructionSchedule.outputInstruction> swapRegister(int regA, int regB) {
            ArrayList<InstructionSchedule.outputInstruction>tmp = new ArrayList();
            InstructionSchedule is = new InstructionSchedule();
            if (registers[regB].isAvailable()) {
                //The register is free
                InstructionSchedule.outputInstruction instr = is.new outputInstruction();
                instr.op = Instruction.move;
                instr.arg1 = regA;
                instr.outputReg = regB;
                tmp.add(instr);
            } else {
                //Get the first reverse register
                int swapReg = numberOfRegister-numberOfReverse;
                //The register is not free, so swap
                InstructionSchedule.outputInstruction instr1 = is.new outputInstruction();
                instr1.op = Instruction.move;
                instr1.arg1 = regA;
                instr1.outputReg = swapReg;
                tmp.add(instr1);
                InstructionSchedule.outputInstruction instr2 = is.new outputInstruction();
                instr2.op = Instruction.move;
                instr2.arg1 = regB;
                instr2.outputReg = regA;
                tmp.add(instr2);
                InstructionSchedule.outputInstruction instr3 = is.new outputInstruction();
                instr3.op = Instruction.move;
                instr3.arg1 = swapReg;
                instr3.outputReg = regB;
                tmp.add(instr3);
            }
            InstructionSchedule.InstructionValue valA = registers[regA].currentValue;
            InstructionSchedule.InstructionValue valB = registers[regB].currentValue;
            registers[regA].updateValue(valB);
            registers[regB].updateValue(valA);
            return tmp;
        }
        boolean hasSpace() {
            return emptyRegister() > 0;
        }
        int checkRegister(int testA, int testB) {
            if (testA > 0 && registers[testA].isAvailable()) { return testA; }
            if (testB > 0 && registers[testB].isAvailable()) { return testB; }
            return -1;
        }
        int emptyRegister() {
            for (int i = 1; i < numberOfRegister-numberOfReverse; i++) {
                if (registers[i].isAvailable())
                    return i;
            }
            return -1;
        }
        void trimRegister() {
            for (int i = 1; i < numberOfRegister-numberOfReverse; i++) {
                if (registers[i].isAvailable())
                    registers[i].preserveMemory();
            }
        }
        ArrayList<Integer> registerToFlushTo() {
            //If there is no space anymore, flush the register to the memory in the following order:
            //1. All register that won't be used in the current block
            //2. All register that has a high write amplification: more register will be used in this block to store the result before the register is released (Any instruction with a score higher than 1)
            Integer firstChoice = -1;       Integer secondChoice = -1;      Integer thirdChoice = -1;
            Integer firstChoiceRate = Integer.MAX_VALUE;   Integer secondChoiceRate = Integer.MAX_VALUE;  Integer thirdChoiceRate = Integer.MAX_VALUE;
            for (int i = 1; i < numberOfRegister-numberOfReverse; i++) {
                Register reg = registers[i];
                if (reg.releaseRate() > firstChoiceRate) {
                    if (reg.releaseRate() < secondChoiceRate) {
                        //Swap out the second
                        thirdChoice = secondChoice;
                        thirdChoiceRate = secondChoiceRate;

                        secondChoice = i;
                        secondChoiceRate = reg.releaseRate();
                    } else if (reg.releaseRate() < thirdChoiceRate)  {
                        //Swap out the third
                        thirdChoice = i;
                        thirdChoiceRate = reg.releaseRate();
                    }
                } else  {
                    //Swap out the first
                    thirdChoice = secondChoice;
                    thirdChoiceRate = secondChoiceRate;

                    secondChoice = firstChoice;
                    secondChoiceRate = firstChoiceRate;

                    firstChoice = i;
                    firstChoiceRate = reg.releaseRate();
                }
            }
            ArrayList<Integer> list = new ArrayList<Integer>();
            list.add(firstChoice);
            list.add(secondChoice);
            list.add(thirdChoice);
						if (Compiler.debug) {
                System.out.println("Release: "+list);
						}
            return list;
        }
        @Override public String toString() {
            String result = "Reg: \n";
            for (int i = 0; i < numberOfRegister; i++) {
                Register reg = registers[i];
                result += reg;
            }
            return result;
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
