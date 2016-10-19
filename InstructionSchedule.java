import com.sun.org.apache.bcel.internal.classfile.Code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedList;

public class InstructionSchedule {
    
    
    public class InstructionValue {
        public Instruction basedInstr;
        //Usage count means how many instructions use this value
        public int usageCount;
        //Reference count means how many times the instruction being used as an argument
        public int referenceCount;
        
        //Usage count means how many instructions use this value
        public int flowUsageCount;
        //Reference count means how many times the instruction being used as an argument
        public int flowReferenceCount;
        
        //Usage count means how many instructions in this block use this value
        public int upcomingUsageCount;
        //Reference count means how many times the instruction in this block being used as an argument

        public int upcomingReferenceCount;

        public int phiCounter = 0;

        InstructionValue() {}
        
        InstructionValue(Instruction instr, Block currentBlock) {
            usageCount = 0;
            referenceCount = 0;
            upcomingUsageCount = 0;
            upcomingReferenceCount = 0;
            
            
            basedInstr = instr;
            for (Instruction parent : instr.uses) {
                boolean sameBlock = (parent.block == currentBlock);
                usageCount++;
                if (parent.arg1 == instr)
                    referenceCount++;
                if (parent.arg2 == instr)
                    referenceCount++;
                if (sameBlock) {
                    upcomingReferenceCount++;
                    upcomingUsageCount++;
                }
            }
            
            evaluateBlockFlow(currentBlock);
        }
        
        public void evaluateBlockFlow(Block currentBlock) {
            if (currentBlock.visited) { return; }

            currentBlock.visited = true;

            if (currentBlock == null) {
                flowReferenceCount = 0;
                flowUsageCount = 0;
                return;
            }
            int ftRefCount = 0;
            int ftUsageCount = 0;
            if (currentBlock.fallThrough != null) {
                evaluateBlockFlow(currentBlock.fallThrough);
                ftRefCount = flowReferenceCount;
                ftUsageCount = flowUsageCount;
            }
            int braRefCount = 0;
            int braUsageCount = 0;
            if (currentBlock.branch != null) {
                evaluateBlockFlow(currentBlock.branch);
                braRefCount = flowReferenceCount;
                braUsageCount = flowUsageCount;
            }
            
            flowReferenceCount = Math.max(ftRefCount, braRefCount);
            flowUsageCount = Math.max(ftUsageCount, braUsageCount);
            
            for (Instruction child : basedInstr.uses) {
                boolean sameBlock = (child.block == currentBlock);
                if (sameBlock) {
                    flowReferenceCount++;
                    flowUsageCount++;
                }
            }

            currentBlock.visited = false;
        }
        
        public void instructionCalled(Instruction instr) {
            if (instr == basedInstr) { return; }
            boolean use = false;
            if (instr.arg1 == basedInstr) {
                use = true;
                referenceCount--;
                upcomingReferenceCount--;
                flowReferenceCount--;
            }
            if (instr.arg2 == basedInstr) {
                use = true;
                referenceCount--;
                upcomingReferenceCount--;
                flowReferenceCount--;
            }
            if (use) {
                usageCount--;
                upcomingUsageCount--;
                flowUsageCount--;
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
            return usageCount>0 || phiCounter > 0;
        }
        public boolean flushable() {
            return usageCount <= 0;
        }
        public int score() {
            return referenceCount;
        }
    }
    
    public class outputInstruction {
        public int op;
        public int outputReg;
        public int arg1, arg2;
        public int constant1;
        public int constant2;
        public Block jumpBlock;
        public Function jumpFunc;
        outputInstruction() {
            op = -1;
            outputReg = -1;
            arg1 = -1;
            arg2 = -1;
        }
        outputInstruction(Instruction instr, int outputRegistetNo, ArrayList<outputInstruction>intermediateOp) {
            Block b;

            //Oct 14, 2016 - A bypass for adda instruction
            if (instr.op == Instruction.adda) {
                //This is an adda instruction
                //First, calculate the offset (-ve), using the intermediate op
                if (instr.arg1 instanceof Array) {
                    Array a = ((Array)instr.arg1);
                    arg1 = -1;
                    if (instr.arg1.isGlobal()) {
                        //If the variable is global, it is a subtraction, with the offset being negative
                        constant1 = a.backstorePos.address * -1;
                    }
                } else {
                    assert (false); //What is this suppose to carry out
                }
                if (instr.arg2 instanceof Constant) {
                    arg2 = -1;
                    constant2 = ((Constant) instr.arg2).getVal() * 4;
                }
                else if (instr.arg2 instanceof Instruction) {
                    outputInstruction addressCalc = new outputInstruction();
                    addressCalc.arg1 = ((Instruction)instr.arg2).state.storage.currentRegister.registerID;
                    addressCalc.arg2 = -1;
                    if (instr.arg1.isGlobal()) {
                        //If the variable is global, it is a subtraction, with the offset being negative
                        addressCalc.constant2 = -4;
                    } else { addressCalc.constant2 = 4; }
                    addressCalc.op = Instruction.mul;
                    addressCalc.outputReg = RegAllocator.memoryOpRegisterID();
                    intermediateOp.add(addressCalc);

                    arg2 = RegAllocator.memoryOpRegisterID();
                }
                if (instr.arg1.isGlobal()) {
                    //If the variable is global, it is a subtraction, with the offset being negative
                    op = Instruction.sub;
                } else {
                    op = Instruction.add;
                }
                return;
            }

            //Search for arg1
            int reg1 = -1;
            if (instr.arg1 instanceof Instruction) {
                //It is instruction, it must have an register
                Instruction instr1 = (Instruction)instr.arg1;
                reg1 = instr1.state.storage.currentRegister.registerID;
            } else if (instr.arg1 instanceof Constant) {
                Constant c = (Constant)instr.arg1;
                constant1 = c.getVal();
            } else if (instr.arg1 instanceof Block) {
               b = (Block)instr.arg1;
               jumpBlock = b;

           }
            //Search for arg2
            int reg2 = -1;
            
            if (instr.op != Instruction.move && instr.op != Instruction.store) {
                //If it's not move instruction, it should be have the second argument
                if (instr.arg2 instanceof Instruction) {
                    //It is instruction, it must have an register
                    Instruction instr2 = (Instruction)instr.arg2;
                    if (Compiler.debug) {
                        System.out.println("Probe: ");
                        System.out.println(instr);
                        System.out.println(instr2);
                    }
                    reg2 = instr2.state.storage.currentRegister.registerID;
                } else if (instr.arg2 instanceof Constant) {
                    Constant c = (Constant)instr.arg2;
                    constant2 = c.getVal();
                } else if (instr.arg2 instanceof Block) {
                    b = (Block)instr.arg2;
                    jumpBlock = b;
                } else if (instr.arg2 == null && instr.op == Instruction.add) {
                    //Special patch for array: arg1 is a constant, arg2 is not defined, use to load constnat into the machine
                    constant2 = constant1;
                    reg1 = 0;
                }
                
            }
            
            //Search for output register, register its present
            int outReg = outputRegistetNo;
            
            op = instr.op;

            if (op == Instruction.load) {
                boolean globalVar =  (((instr.arg1 instanceof Global)&&((Global)instr.arg1).isGlobal())||(((instr.arg1 instanceof Instruction) && (((Instruction)instr.arg1).arg1).isGlobal())));
                if (globalVar) {
                    reg2 = RegAllocator.globalPtrRegisterID();
                } else {
                    reg2 = RegAllocator.stackPtrRegisterID();
                }
            }

            outputReg = outReg;
            arg1 = reg1;
            arg2 = reg2;

            if (op == Instruction.call) {
                jumpFunc = ((Function)instr.arg1);
            }

            //Override for array
            if ((op == Instruction.load || op == Instruction.store)&&(instr.isLinked() ||
                    ((instr.arg1 instanceof Global)&&((Global)instr.arg1).isGlobal())   ||
                    ((instr.arg2 instanceof Global)&&((Global)instr.arg2).isGlobal())
            )) {
                if (instr.isLinked()) {
                    outputReg = ((Instruction) instr.arg1).state.storage.currentRegister.registerID;
                    arg1 = instr.getLink().arg1.isGlobal()?RegAllocator.globalPtrRegisterID(): RegAllocator.stackPtrRegisterIndex();
                    arg2 = instr.getLink().state.storage.currentRegister.registerID;
                } else if (op == Instruction.load) {
                    outputReg = outputRegistetNo;
                    arg1 = instr.arg1.isGlobal()?RegAllocator.globalPtrRegisterID(): RegAllocator.stackPtrRegisterIndex();
                    arg2 = -1;
                    constant2 = ((Global)instr.arg1).position.address*-1;
                } else {
                    if (instr.arg1 instanceof Instruction) outputReg = ((Instruction)instr.arg1).state.storage.currentRegister.registerID;
                    else {
                        Constant c = (Constant)instr.arg1;
                        outputInstruction oi = new outputInstruction();
                        oi.arg1 = 0;    oi.arg2 = -1;   oi.constant2 = c.getVal();  oi.op = Instruction.add;    oi.outputReg = RegAllocator.memoryOpRegisterID();
                        intermediateOp.add(oi);
                        outputReg = oi.outputReg;
                    }
                    arg1 = instr.arg2.isGlobal()?RegAllocator.globalPtrRegisterID(): RegAllocator.stackPtrRegisterIndex();
                    arg2 = -1;
                    constant2 = ((Global)instr.arg2).position.address*-1;
                }
            }
        }
        
        @Override
        public String toString() {
            String result = Instruction.ops[op] + " ";
            result += "R" + outputReg + " ";
            if (jumpBlock != null) {
                result += "B" + jumpBlock.id;
                return result;
            }
            if (arg1 >= 0)
                result += "R" + arg1 + " ";
            else result += "C" + constant1 + " ";
            if (arg2 >= 0)
                result += "R" + arg2;
            else result += "C" + constant2;
            return result;
        }
        
    }

    public static void clearContext(ArrayList<Instruction> dependencyList) {
        for (Instruction instr: dependencyList) {
            if (instr.state != null) {
                instr.state.storage.currentRegister = null;
                if (instr.state.storage.backstore != null) instr.state.storage.backstore.actualValue = instr.state.valueRepr;
            }
        }
    }

    public static void setContext(RegAllocator.registerContext ctx) {
        for (RegAllocator.Register reg: ctx.registers) {
            if (reg.currentValue != null && reg.currentValue.basedInstr != null)
                reg.currentValue.basedInstr.state.storage.currentRegister = reg;
        }

    }
    
    public class ScheduledBlock {
        public ScheduledBlock next1, next2;
        public ArrayList<outputInstruction> instructions = new ArrayList<outputInstruction>();
        public ArrayList<Instruction> directDependent;
        public Block referenceBlock;
        public RegAllocator.registerContext context;

        public RegAllocator.memorySpace scheduledSpace;

        public boolean compiled = false;

        public boolean isEmpty() {
            return instructions.size() == 0;
        }
        
        @Override
        public String toString() {
            String output = "Block:"+referenceBlock.id+" \n";
            output += "#instructions: " + instructions.size() + "\n";
            for (outputInstruction oi : instructions) {
                output += oi+"\n";
            }
            //Next
            if (next1 != null) {
                output += "Next 1: ";
                output += next1.referenceBlock.id + "\n";
            }
            //Next
            if (next2 != null) {
                output += "Next 2: ";
                output += next2.referenceBlock.id + "\n";
            }
            
            //Next
            if (next1 != null && next1.referenceBlock.id > referenceBlock.id) {
                output += "\n" + next1 + "\n";
            }
            //Next
            if (next2 != null && next2.referenceBlock.id > referenceBlock.id) {
                output += "\n" + next2 + "\n";
            }
            
            return output;
        }
        
        public Instruction pickInstr(ArrayList<Instruction> instrs) {
            int score = -1;
            Instruction instr = null;
            Instruction branch = null;
            for (Instruction currentInstr : instrs) {
                int currentScore = 0;
                if (instructionWouldReturn(currentInstr) == false)
                    currentScore = 1;
                boolean allowed = true;
                if (currentInstr.arrayVersion > 0 && currentInstr.getLink() != null && currentInstr.getLink().arg1 instanceof Array) {
                    int ver = currentInstr.arrayVersion;
                    Array a = (Array)currentInstr.getLink().arg1;
                    //Check version
                    allowed =
                            a.versionCanCarry(ver, currentInstr.op == Instruction.store);
                }
                if (currentInstr.op == Instruction.bra || (currentInstr.op >= Instruction.bne && currentInstr.op <= Instruction.ble) ) {
                    //Make sure branch is the last to go
                    currentScore = -1;
                }
                if (allowed&&(score == -1 || score < currentScore)) {
                    //There is a better instruction
                    score = currentScore;
                    instr = currentInstr;
                }
            }
            return instr;
        }

        public Instruction pickNonReturnInstr(ArrayList<Instruction> instrs) {
            for (Instruction instr: instrs) {
                if (!instructionWouldReturn(instr)) return instr;
                if (instr.isLinked() && instr.op != Instruction.adda) {
                    //Array op, can replace the value
                    if (((Array)instr.getLink().arg1).versionCanCarry(instr.arrayVersion, instr.op == Instruction.store)) return instr;
                }
                if (instr.instrsUsed != null) {
                    for (Instruction parent : instr.instrsUsed) {
                        if (parent != null && !parent.deleted() && parent.state.storage.loaded() && parent.state.remainingAvailableChildSize == 0.5)
                            return instr;
                    }
                }
            }
            assert (false);
            return null;
        }

        private boolean instructionWouldReturn(Instruction instr) {
            switch (instr.op) {
                case Instruction.write:
                case Instruction.store:
                case Instruction.writeNL:
                case Instruction.arrayStore:
                    return false;
                default:
                    if (instr.op == Instruction.bra || (instr.op >= Instruction.bne && instr.op <= Instruction.ble) ) return false;
                    return true;
            }
        }

        private void phiMemoryDeduction(Instruction instr, RegAllocator.registerContext context) {
            for (RegAllocator.Register reg: context.registers) {
                if (reg.currentValue != null) {
                    Instruction regInstr = reg.currentValue.basedInstr;
                    if (regInstr.op == Instruction.phi && (regInstr.arg1 == instr || regInstr.arg2 == instr)) {
                        reg.currentValue.usageCount *= -1;
                    }
                }
            }
        }
        private void phiMemoryRestore(Instruction instr, RegAllocator.registerContext context) {
            for (RegAllocator.Register reg: context.registers) {
                if (reg.currentValue != null) {
                    Instruction regInstr = reg.currentValue.basedInstr;
                    if (regInstr.op == Instruction.phi && (regInstr.arg1 == instr || regInstr.arg2 == instr)) {
                        reg.currentValue.usageCount *= -1;
                    }
                }
            }
        }

        public int phiableRegister(Instruction instr, RegAllocator.registerContext context) {
            for (RegAllocator.Register reg: context.registers) {
                if (reg.currentValue != null) {
                    for (Instruction childInstr: reg.currentValue.basedInstr.uses) {
                        if (childInstr.op == Instruction.phi && (childInstr.arg1 == instr || childInstr.arg2 == instr))
                            return reg.registerIndex;
                    }
                }
            }
            return -1;
        }

        public ArrayList<outputInstruction> scheduleInstruction(Instruction instr, RegAllocator.registerContext context) {
            ArrayList<outputInstruction> result = new ArrayList<>();
            //Get register, set the value to the register
            if (Compiler.debug) {
                //System.out.println(instr);
            }
            int nextRegID = -1;
            
            InstructionValue value = new InstructionValue(instr, referenceBlock);
            if (Compiler.debug) {
                //System.out.print("Instr: "+instr+"\n");
            }
            instr.state.valueRepr = value;
            
            //Tell the instrcution depended on that it's releasing
            instr.state.scheduled();
            outputInstruction oi = new outputInstruction(instr, nextRegID, result);
            
            if (instructionWouldReturn(instr)) {
                nextRegID = phiableRegister(instr, context);
                phiMemoryDeduction(instr, context);
                if (nextRegID < 0) nextRegID = context.checkRegister(oi.arg1, oi.arg2);
                if (nextRegID < 0) nextRegID = context.emptyRegister();
                phiMemoryRestore(instr, context);
                RegAllocator.Register nextReg =  context.registers[nextRegID];
                if (Compiler.debug) {
                    System.out.println("Instruction: "+instr+" Register ID: "+nextRegID);
                }
                //The register is empty, so just write the value
                InstructionValue oldVal = nextReg.currentValue;
                outputInstruction release = nextReg.updateValue(value);
                if (release != null) {
                    System.out.println("Check Register "+nextRegID);
                    //context.space.release(oldVal.basedInstr.state.storage.backstore);
                }
                //assert release == null;//Should be null, meaning no overwriting the variable
                
                oi.outputReg = nextRegID;
                
            }
            result.add(oi);
            return result;
        }
        
        //Check if the instruction depend on an instruction that has not scheduled yet (O(n))
        private boolean instructionDependOnInstr(Instruction instr, ArrayList<Instruction> unprocessBlock) {
            Value instr1 = instr.arg1;
            if (unprocessBlock.contains(instr1))
                return true;
            Value instr2 = instr.arg2;
            if (instr.op != Instruction.move && unprocessBlock.contains(instr1))
                return true;
            return false;
        }
        
        private Instruction pickNextCache(ArrayList<Instruction> instrs) {
            /* TODO */
            //Solve read write conflict first, if exist
            for (Instruction instr: instrs) {
                //Detect if it's an array write
                if (instr.isLinked() && (instr.op == Instruction.store)||(instr.op == Instruction.load)) {
                    //Check if can be carry out
                    Array a = (Array)instr.getLink().arg1;
                    boolean premission = a.versionCanCarry(instr.arrayVersion, instr.op == Instruction.store);
                    if (premission){
                        //There is a scheuldable I/O, so find the not loaded
                        for (Instruction child: instr.instrsUsed) {
                            if (!child.state.storage.loaded())
                                return child;
                        }
                    }
                }
            }

            //Loop for all variable
            for (Instruction instr: instrs) {
                //First loop, get an instruction that require one object
                int dependCount = 0;	Instruction depend = null;
                for (Instruction parent : instr.instrsUsed) {
                    if (Compiler.debug) {
                        System.out.println("Parent Instr: "+parent);
                    }
                    if (parent != null && parent.state.storage.currentRegister == null) {
                        dependCount++;
                        depend = parent;
                    }
                }
                if (dependCount == 1) {
                    //Only one more arguemnt need, so just load it
                    return depend;
                }
            }
            //Need to load more than one, so just load one randomly
            for (Instruction instr: instrs) {
                for (Instruction parent : instr.instrsUsed) {
                    if (parent.state.storage.currentRegister == null) {
                        return parent;
                    }
                }
            }
            float score = 100000;	Instruction ref = null;
            for (Instruction instr : instrs) {
                //if (score > ref.)
                float currentScore = instr.state.remainingAvailableChildSize;
                if (instr.state.storage.loaded() == false && score > currentScore) {
                    //Not loaded, and current instruction has fewer usage -> load first
                    ref = instr;
                }
            }
            return ref;
        }
        
        public boolean needSpace(ArrayList<Instruction> instrs) {
            for (Instruction instr : instrs) {
                if (instructionWouldReturn(instr) == false)
                    return false;
            }
            return true;
        }
        
        public RegAllocator.registerContext afterPhiCtx;

        ScheduledBlock(RegAllocator.registerContext previousContext, Block inputBlock, RegAllocator.memorySpace space, RegAllocator.registerContext lastPhiReg) {

            boolean ifJoin = false;

            //Repleace the register context with new memory space
            previousContext.space = space;

            //Solve the if memory space split problem
            if (inputBlock.in1!=null && inputBlock.in2!=null && inputBlock.in1.id < inputBlock.id && inputBlock.in2.id < inputBlock.id) {
                //We are at a merge section, so merge
                ifJoin = true;
            }

            boolean ifCmp = false;
            if (inputBlock.in2 == null && inputBlock.branch != null)
                ifCmp = true;

            inputBlock.schedule = this;
            
            instructions = new ArrayList<InstructionSchedule.outputInstruction>();

            ArrayList<Instruction> proccessSeq = new ArrayList<>();

            referenceBlock = inputBlock;
            //Current Context
            RegAllocator rc = new RegAllocator();
            context = rc.new registerContext(previousContext);
            
            //Unprocess block: not scheduled instructions
            ArrayList<Instruction> unprocessBlock = new ArrayList<Instruction>();
            //Available instructions: value availabled in the system
            ArrayList<Instruction> availableInstruction = new ArrayList<Instruction>();
            //Cached instructions: value availabled in the register
            ArrayList<Instruction> cachedInstruction = new ArrayList<Instruction>();
            
            ArrayList<Instruction> dependencyList = new ArrayList<Instruction>();
            
            ArrayList<RegAllocator.phiRequest> phiInstructions = new ArrayList<RegAllocator.phiRequest>();
            boolean endOfBlock = false;
            
            Instruction branch = null;
            
            //Get all the instructions in the block into unprocess
            Instruction currentInstr = inputBlock.begin;
            while (currentInstr != null) {
                if (currentInstr.op != Instruction.end && currentInstr.op != Instruction.phi) {
                    if (currentInstr.op == Instruction.bra || (currentInstr.op >= Instruction.bne && currentInstr.op <= Instruction.ble)) {
                        branch = currentInstr;
                    } else {
                        unprocessBlock.add(currentInstr);
                        if (currentInstr.instrsUsed != null)
                            for (Instruction instr : currentInstr.instrsUsed)
                                if (instr != null && !instr.deleted() && !instr.state.schedule) dependencyList.add(instr);
                    }
                } else if (currentInstr.op == Instruction.phi) {
                    RegAllocator.phiRequest req = rc.new phiRequest();
                    req.value1 = ((Instruction)currentInstr.arg1);
                    req.value2 = ((Instruction)currentInstr.arg2);
                    if (currentInstr.state.valueRepr != null)
                        req.actualStmt = currentInstr.state.valueRepr;
                    else req.actualStmt = new InstructionValue(currentInstr, inputBlock);
                    currentInstr.state.valueRepr = req.actualStmt;
                    
                    phiInstructions.add(req);
                } else {
                    //Remaining: end instruction
                    endOfBlock = true;
                }
                
                //Re-evaluate
                if ((currentInstr.arg1 instanceof Instruction) && ((Instruction) currentInstr.arg1).state != null  && ((Instruction) currentInstr.arg1).state.valueRepr != null )
                    ((Instruction) currentInstr.arg1).state.valueRepr.evaluateBlockFlow(inputBlock);
                if ((currentInstr.arg2 instanceof Instruction) && ((Instruction) currentInstr.arg2).state != null  && ((Instruction) currentInstr.arg2).state.valueRepr != null )
                    ((Instruction) currentInstr.arg2).state.valueRepr.evaluateBlockFlow(inputBlock);
                
                currentInstr = currentInstr.next;
            }
            if (phiInstructions.size() > 0) {
                //There is phi instruction
                //Check phi status
                //There should have two input
                if (Compiler.debug) {
                    System.out.println("PHI");
                }
                if (ifJoin) {
                    //This is a merge block
                    RegAllocator.phiMergerResult result = RegAllocator.phiMerger(inputBlock.in1.schedule.context, inputBlock.in2.schedule.context, phiInstructions);
                    //TODO: Do something to the blocks
                    inputBlock.in1.schedule.insertPhiTransfer(result.edge1);
                    inputBlock.in2.schedule.insertPhiTransfer(result.edge2);
                    context = result.resultContext;

                    //Memory Space merge

                    RegAllocator.memorySpace space1 = inputBlock.in1.schedule.context.space;
                    RegAllocator.memorySpace space2 = inputBlock.in2.schedule.context.space;
                    assert (space1.upperSpace == space2.upperSpace);
                    space = space1.upperSpace;

                    context.space = space;
                } else {
                    //This is the join block (loop header)
                    if (inputBlock.in1.schedule != null && inputBlock.in2.schedule != null) {
                        //Both input block is scheduled, so just patch the remain
                    } else {
                        //Only one input block is done, so just do a passover
                        if (Compiler.debug) {
                            System.out.println("Patching");
                        }
                        RegAllocator.phiPassover(inputBlock, phiInstructions);
                    }
                }
                //Phi-ed
                if (Compiler.debug) {
                    System.out.println(context);
                }
            }
            afterPhiCtx= rc.new registerContext(context);


            //Newly Calculated: Calculated value that has not been used to add more instruction into the system
            ArrayList<Instruction> newlyCalculated = new ArrayList<Instruction>();

            boolean flushedWithoutSuccess = false;
            int lastFlushed = 0;
            InstructionValue lastStoredVal = null;

            clearContext(dependencyList);
            setContext(context);

            //Switch instruction register context


            //As long as there is instruction un-scheduled
            while (unprocessBlock.size() > 0) {

                newlyCalculated.clear();

                //Load buffer: the elements that should be scheduled to load
                ArrayList<outputInstruction> loadInstrBuffer = new ArrayList<outputInstruction>();
                //Save buffer: the elements that should be scheduled to load
                ArrayList<outputInstruction> saveInstrBuffer = new ArrayList<outputInstruction>();
                //Instruction Buffer: the instruction that could carried out immediately
                ArrayList<outputInstruction> instrBuffer = new ArrayList<outputInstruction>();
                
                for (Instruction child: unprocessBlock) {
                    if (Compiler.debug) {
                        System.out.print("child: "+child+" "+child.state.unresolveArgument+"\n");
                    }
                    if (child.state.unresolveArgument == 0) {
                        //If the instruction is available to schedule, it promopt to available
                        boolean cached = true;
                        if (child.instrsUsed != null) {
                            for (Instruction parent : child.instrsUsed) {
                                if (parent != null)
                                    if (Compiler.debug) {
                                        //System.out.print("Parent: "+parent+"\n");
                                    }
                                if (parent != null && (!parent.deleted() && !parent.state.storage.loaded()))
                                    cached = false;
                            }
                        }
                        boolean canCarryOut = true;
                        //On array versioning
                        if (child.getLink() != null && child.getLink().arg1 instanceof Array) {
                            canCarryOut = ((Array)child.getLink().arg1).versionCanCarry(child.arrayVersion, child.op == Instruction.store);
                        }

                        if (cached) {
                            availableInstruction.remove(child);
                            cachedInstruction.remove(child);
                            cachedInstruction.add(child);
                        } else if (canCarryOut) {
                            availableInstruction.remove(child);
                            availableInstruction.add(child);
                            cachedInstruction.remove(child);
                        }
                        //unprocessBlock.remove(child);
                    }
                }
                if (Compiler.debug) {
                    /*System.out.println("# of unscheduled instruction: "+unprocessBlock.size()+"\n"+unprocessBlock);
                    System.out.println("# of cached instruction: "+cachedInstruction.size());*/
                    System.out.println("Scheduled: "+instructions);
                }
                
                boolean allInstrNeedSpace = needSpace(cachedInstruction);

                boolean readWriteConflict = false;
                while (context.hasSpace()||existNonReturnCond(cachedInstruction, context) && cachedInstruction.size() > 0 && !readWriteConflict) {
                    readWriteConflict = false;
                    //As long as there is instruction available:
                    //If there is space in the register, use it to do the instruction that has the largest impact:
                    //Free up more register (last hold out data) (Using the heuristic)
                    //If there is still space, reverse two for move instruction, which will dynamically schedule N instructions later than the last load instruction
                    Instruction instr;
                    if (context.hasSpace()) instr = pickInstr(cachedInstruction);
                    else instr = pickNonReturnInstr(cachedInstruction);
                    if (instr == null) {
                        //All remaining is conflict
                        readWriteConflict = true;
                        break;
                    }

                    proccessSeq.add(instr);

                    ArrayList<outputInstruction> oi = scheduleInstruction(instr, context);
                    //Transform it to register based instruction
                    //"Solidify" it
                    flushedWithoutSuccess = false;
                    instrBuffer.addAll(oi);
                    newlyCalculated.add(instr);
                    unprocessBlock.remove(instr);
                    cachedInstruction.remove(instr);
                    for (Instruction _instr : instr.uses) {
                        _instr.state.unresolveArgument--;
                        if (_instr.op != Instruction.phi && /* Not Phi*/
                            !(_instr.op == Instruction.bra || (_instr.op >= Instruction.bne && _instr.op <= Instruction.ble)) /*Not branch*/
                            && _instr.block == inputBlock) { /*Within scope of block*/
                            if (_instr.state.unresolveArgument == 0) {
                                boolean cached = true;
                                if (_instr.instrsUsed != null)
                                    for (Instruction parent: _instr.instrsUsed) {
                                        if (parent != null)
                                            if (Compiler.debug) {
                                                //System.out.print("Parent: "+parent+"\n");
                                            }
                                        if (parent != null && ( !parent.deleted() && !parent.state.storage.loaded()) )
                                            cached = false;
                                    }
                                if (cached) {
                                    availableInstruction.remove(_instr);
                                    cachedInstruction.remove(_instr);
                                    cachedInstruction.add(_instr);
                                }
                            }
                        }
                    }

                    //For array, add all available child
                    if (instr.isLinked() && instr.op != Instruction.adda) {
                        //I/O, scheduled, so get child
                        ArrayList<Instruction> bachelors = ((Array)instr.getLink().arg1).canSchedule();
                        boolean d;
                        for (Instruction bachelor: bachelors) {
                            if (bachelor.getLink().state.storage.loaded()) {
                                availableInstruction.remove(bachelor);
                                cachedInstruction.remove(bachelor);
                                cachedInstruction.add(bachelor);
                            }
                        }
                    }

                }
                
                //Finally, no instruction available, or no space available
                //See which problem it is
                if (unprocessBlock.size() > 0 || readWriteConflict) {
                    //If there is futhur unrelease stuff, load and unload stuff


                    if (context.hasSpace() == false ) {
                        if (Compiler.debug) {
                            System.out.print(context + "\n");
                        }
                        //If there is no space, flush
                        int flushID = 0;
                        if (cachedInstruction.size() == 0) {
                            ArrayList<Integer> nextFlush = context.registerToFlushTo();
                            int flushIndex;
                            if (flushedWithoutSuccess) flushIndex = 1;
                            else flushIndex = 0;
                            if (nextFlush.get(flushIndex) > 0) {
                                flushID = nextFlush.get(flushIndex);

                            }
                            flushedWithoutSuccess = true;
                        } else {
                            //Since there is an existed cached instruction, just pick the one register that is not depended
                            Instruction instr = cachedInstruction.get(0);
                            Instruction[] dependents = instr.instrsUsed;
                            ArrayList<Integer> nextFlush = context.registerToFlushTo();
                            if (dependents != null) {
                                for (Instruction dep : dependents) {
                                    if (dep != null && !dep.deleted() && dep.state.storage.currentRegister != null) {
                                        //Only consider depenedents that is scheduled
                                        int index = nextFlush.indexOf(dep.state.storage.currentRegister.registerIndex);
                                        if (index >= 0) nextFlush.remove(index);
                                    }
                                }
                            }
                            flushID = nextFlush.get(0);
                            //Flush the oldest one
                            flushedWithoutSuccess = true;
                        }

                        RegAllocator.Register r = context.registers[flushID];
                        //Set the instruction back to storage
                        Instruction storageInstr = r.currentValue.basedInstr;
                        for (Instruction child : storageInstr.uses) {
                            if (cachedInstruction.contains(child)) {
                                //If the value is in cached, move from cached to available
                                cachedInstruction.remove(child);
                                availableInstruction.add(child);
                            }
                        }
                        lastFlushed = flushID;
                        lastStoredVal = r.currentValue;
                        outputInstruction oi = r.preserveMemory();
                        if (oi != null)
                            saveInstrBuffer.add(oi);

                    } else if (cachedInstruction.size() == 0 || readWriteConflict) {
                        //Pick one register
                        int nextRegID = context.emptyRegister();
                        RegAllocator.Register r = context.registers[nextRegID];
                        
                        //No instruction, pull
                        //Choose the stored variable with the lowest usage rate -> reduce the chance of requiring more register, and free up quickly
                        //Then add the instruction from the available pool
                        Instruction instr = pickNextCache(availableInstruction);
                        if (instr != null) {
                            ArrayList<InstructionSchedule.outputInstruction> oi = instr.state.storage.load(r, space);

                            if (r.currentValue.basedInstr != instr) {   //If crash, it means it didn't load
                                //The value is not correct, so check if phi-able
                                if (instr.op == Instruction.phi && (
                                        r.currentValue.basedInstr == instr.arg1 || r.currentValue.basedInstr == instr.arg2 )
                                        ) {
                                    //Phi-able
                                    r.currentValue = instr.state.valueRepr;
                                    instr.state.storage.currentRegister = r;
                                } else { assert false; }
                            }
                            
                            for (Instruction child : instr.uses) {
                                if (availableInstruction.contains(child)) {
                                    //If the value is in cached, move from cached to available
                                    cachedInstruction.remove(child);
                                    cachedInstruction.add(child);
                                    availableInstruction.remove(child);
                                }
                            }
                            
                            if (oi != null) {
                                loadInstrBuffer.addAll(oi);
                            }
                        }
                        
                    }
                }
                
                //Then, schedule the instruction in the buffer to go away
                //First, add each instrcution, if it's the instruction value in the register, and the register is in nextStoredReg, it means it is going to save
                instructions.addAll(instrBuffer);
                //So do the save ASAP
                instructions.addAll(saveInstrBuffer);
                //Then, flush all load instructions
                instructions.addAll(loadInstrBuffer);
                //Worst case: the save is followed by load at the same register
                
                if (Compiler.debug) {
                    /*System.out.print("Schedule Stat: \n"
                                     + "Load: " + loadInstrBuffer.size() + "\n"
                                     + "Save: " + saveInstrBuffer.size() + "\n"
                                     + "Instr: " + instrBuffer.size() + "\n"
                                     + "Total: " + instructions.size() + "\n");*/
                }
            }

            //Popping up the store and load
            //In order to drain the maximum memory bandwidth, all load and store are moved right after the last instruction that used such space
            int cycleBubble = 1;

            LinkedList<outputInstruction> oiCopy = new LinkedList(instructions);

            for (int index = 0; index < instructions.size(); index++) {
                boolean reachConflict = false;
                outputInstruction oi = instructions.get(index);
                int targetReg = oi.outputReg;
                int addrReg = oi.arg2;
                if (oi.op == Instruction.load||oi.op == Instruction.store) {
                    int moveAhead = 0;
                    //Find a memory interaction, so start popping up
                    for (int ptr = index-1; ptr >= 0 && !reachConflict; ptr--) {
                        outputInstruction roadBlock = oiCopy.get(ptr);
                        if ((roadBlock.arg1 != targetReg && roadBlock.arg2 != targetReg && roadBlock.outputReg != targetReg)
                        &&  (roadBlock.arg1 != addrReg && roadBlock.arg2 != addrReg && roadBlock.outputReg != addrReg)) {
                            //No conflict at all, so move
                            moveAhead++;
                        } else {
                            reachConflict = true;
                        }
                    }
                    //The total amount of move ahead = max(0, moveAhead-cycleBubble) (need to reverse the instruction to avoid bubble), or move to the top if reachConflict = false -> add instruction ahead has nothing to do with the register, so move to the top
                    int newIndex = index;
                    if (moveAhead-cycleBubble > 0) newIndex -= (moveAhead - cycleBubble);
                    if (!reachConflict) newIndex = 0;
                    //Adjust
                    oiCopy.remove(index);
                    oiCopy.add(newIndex, oi);
                }
            }

            //Rebuild the list
            instructions = new ArrayList<>(oiCopy);

            scheduledSpace = space;

            if (branch != null) {
                //There is branch statement, so do the branch
                if (branch.instrsUsed != null) {
                    //It use instruction, because it's branch so it must have only one
                    Instruction depend = branch.instrsUsed[0];
                    if (depend != null && depend.state.storage.loaded() == false) {
                        //It dependes on instruction, make sure it would be loaded if it's not
                        //Use memory reverse register to do it
                        int swapReg = RegAllocator.numberOfRegister-RegAllocator.numberOfReverse;
                        RegAllocator.Register reg = context.registers[swapReg];
                        outputInstruction load = reg.updateValue(depend.state.valueRepr);
                        instructions.add(load);
                    }
                }
                //It's ready, so load it
                ArrayList<outputInstruction> oi = scheduleInstruction(branch, context);
                instructions.addAll(oi);
            }

            //Get the set of the instructions dependent
            //This is used to for the previous block to have component scheduled
            //Dependent counter init
            if (inputBlock.fallThrough != null && inputBlock.fallThrough.dependent < 0) {
                //Init the counter
                inputBlock.fallThrough.dependent = 0;
                if (inputBlock.fallThrough.in1 != null && inputBlock.fallThrough.id > inputBlock.fallThrough.in1.id)
                    inputBlock.fallThrough.dependent++;
                if (inputBlock.fallThrough.in2 != null && inputBlock.fallThrough.id > inputBlock.fallThrough.in2.id)
                    inputBlock.fallThrough.dependent++;
            }
            if (inputBlock.branch != null && inputBlock.branch.dependent < 0) {
                //Init the counter
                inputBlock.branch.dependent = 0;
                if (inputBlock.branch.in1 != null && inputBlock.branch.id > inputBlock.branch.in1.id)
                    inputBlock.branch.dependent++;
                if (inputBlock.branch.in2 != null && inputBlock.branch.id > inputBlock.branch.in2.id)
                    inputBlock.branch.dependent++;
            }
            
            //If it is not the end block
            if (inputBlock.fallThrough != null) {
                if (inputBlock.fallThrough != null) {
                    //Count if the input block's the fall through has an un-scheduled in2
                    if (inputBlock.fallThrough.id > inputBlock.id)
                        inputBlock.fallThrough.dependent--;
                    //If the input block of the fall through has been done->has register context to start
                    //So, start scheduling the fall through block
                    if (inputBlock.fallThrough.dependent == 0 && inputBlock.fallThrough.schedule == null) {
                        if (inputBlock.fallThrough != null && inputBlock.branch != null && inputBlock.in2 != null) {
                            //There is a multi-path, so it is either a while, or an if branch (as in2 exist->must not be a if)->while only->has a new phi target
                            rc.freezePhiForReg(previousContext, phiInstructions);
                            next1 = new ScheduledBlock(context, inputBlock.fallThrough, space, previousContext);
                        } else if (ifCmp) {
                            //This is a if branch, scheudling the fall through
                            //So create a sub-memory space
                            RegAllocator.memorySpace subSpace = rc.new memorySpace(space);
                            //And a copy of register
                            RegAllocator.registerContext ctx = rc.new registerContext(context);
                            next1 = new ScheduledBlock(ctx, inputBlock.fallThrough, subSpace, lastPhiReg);
                        } else {
                            next1 = new ScheduledBlock(context, inputBlock.fallThrough, space, lastPhiReg);
                        }
                    }
                }
                //If it is a while loop, and has phi instr->this is a loop header, and the fall through is the inner loop, so patch up the inner loop
                if (phiInstructions.size() > 0 && !ifJoin) {
                    //Has phi, not if join->while join
                    //After the effect in the
                    RegAllocator.phiMergerResult result = RegAllocator.phiTransform(inputBlock.in2.schedule.context, afterPhiCtx, space, phiInstructions);
                    inputBlock.in2.schedule.insertPhiTransfer(result.edge1);
                }
            } else next1 = null;
            
            //If there is a branch
            //Count if the branch is ready to schedule
            //If so, schedule
            if (inputBlock.branch != null) {
                if (inputBlock.branch.id > inputBlock.id)
                    inputBlock.branch.dependent--;
                if (inputBlock.branch.dependent == 0)
                    if (inputBlock.branch.schedule == null) {
                        //It has a branch, a fall through is given, so it must throw a new phi
                        if (inputBlock.in2 != null) {
                            //While branch
                            rc.freezePhiForReg(previousContext, phiInstructions);
                            next2 = new ScheduledBlock(context, inputBlock.branch, space, previousContext);
                        } else if (ifCmp) {
                            //This is a if branch, scheudling the fall through
                            //So create a sub-memory space
                            RegAllocator.memorySpace subSpace = rc.new memorySpace(space);
                            //And a copy of register
                            RegAllocator.registerContext ctx = rc.new registerContext(context);
                            next2 = new ScheduledBlock(ctx, inputBlock.branch, subSpace, lastPhiReg);
                        } else {
                            next2 = new ScheduledBlock(context, inputBlock.branch, space, lastPhiReg);
                        }
                    }
                    else next2 = inputBlock.branch.schedule;
            } else next2 = null;
            
            //End of instruction
            if (endOfBlock) {
                //Add back the end instruction
                outputInstruction endInstr = new outputInstruction();
                endInstr.op = Instruction.end;
                instructions.add(endInstr);
            }
            if (Compiler.debug) {
                System.out.print("Sequence: \n" + proccessSeq);
                System.out.print("Total: " + instructions.size() + "\n");
            }

        }

        private boolean existNonReturnCond(ArrayList<Instruction> cachedInstruction, RegAllocator.registerContext context) {
            for (RegAllocator.Register reg: context.registers) {
                if (reg.currentValue != null) {
                    Instruction instr = reg.currentValue.basedInstr;
                    //First condition: array load at addr space, or array store
                    if (instr.isLinked() && instr.op == Instruction.adda) {
                        Instruction linked = instr.getLink();
                        if (
                                ((Array)instr.arg1).versionCanCarry(linked.arrayVersion, linked.op == Instruction.store)
                                && linked.state.unresolveArgument == 0
                                ) {
                            //Can go on
                            return true;
                        }
                    }
                    //Second: Over-write-able
                    if (instr.state.remainingAvailableChildSize == 0.5)
                        return true;
                }
            }
            //Thrid: no return value
            for (Instruction instr: cachedInstruction) {
                if (!instructionWouldReturn(instr))
                    return true;
            }
            return false;
        }

        public void insertPhiTransfer(ArrayList<outputInstruction> ois) {
            outputInstruction lastOI;
            if (instructions.size() > 0)
                lastOI = instructions.get(instructions.size() - 1);
            else lastOI = null;
            if (lastOI != null && (lastOI.op == Instruction.bra || (lastOI.op >= Instruction.bne && lastOI.op <= Instruction.ble)) ) {
                //It is branch
                instructions.remove(lastOI);
            } else lastOI = null;
            instructions.addAll(ois);
            if (lastOI != null)
                instructions.add(lastOI);
        }
        
    }
    
    public ScheduledBlock mainBlock;
    public HashMap<Function, ScheduledBlock> functionBlocks;
    
    InstructionSchedule() {
        //Null init
    }
    
    InstructionSchedule(IntermedRepr repr) {
        for (Instruction instr : repr.instrs) {
            instr.varsToInstrs();
        }
        //Just init the first block, it will bring the main
        //Init first register set
        RegAllocator rac = new RegAllocator();
        //Init memory set
        //Create global variable memory space
        RegAllocator.memorySpace globalSpace = rac.new memorySpace();
        {
            //Inital all the variable first
            for (Global g: repr.globals) {
                //Every variable is 1 value size
                g.position = globalSpace.reverseVariable(1, g.ident);
            }
            //Then, reserve all array
            for (Array array: repr.globalArrays) {
                array.backstorePos = globalSpace.reverseVariable(array.totalSize, array.ident);
            }
        }

        //Reserve register space


        RegAllocator.memorySpace space = rac.new memorySpace(globalSpace);
        RegAllocator.registerContext regCtx = rac.new registerContext(space);

        space.offset(-4);
        space.reserveForRegister(regCtx.regToPreserve().size());

        mainBlock = new ScheduledBlock(regCtx, repr.MAIN.enter, space, null);
        {
            outputInstruction oi = new outputInstruction();
            oi.arg1 = RegAllocator.framePtrRegisterID();
            oi.constant2 = 4;
            oi.outputReg = RegAllocator.framePtrRegisterID();
            oi.op = Instruction.add;
            mainBlock.instructions.add(0, oi);
        }

        //Init functions
        functionBlocks = new HashMap<Function, ScheduledBlock>();

        for (Function func : repr.functions) {
            //Lame patch for instrsUsed
            for (Instruction f: func.instrs) {
                if (!f.deleted()) {
                    f.state.unresolveArgument = 0;
                    if (f.arg1 instanceof Instruction && f.arg1 != f.instrsUsed[0] && f.arg2 != f.instrsUsed[1]) {
                        if (f.instrsUsed[0] == null) f.instrsUsed[0] = (Instruction)f.arg1;
                        else f.instrsUsed[1] = (Instruction)f.arg1;
                    }
                    if ( f.arg1 instanceof  Instruction && !((Instruction)f.arg1).state.schedule ) f.state.unresolveArgument++;
                    if (f.arg2 instanceof Instruction && f.arg2 != f.instrsUsed[0] && f.arg2 != f.instrsUsed[1]) {
                        if (f.instrsUsed[0] == null) f.instrsUsed[0] = (Instruction)f.arg2;
                        else f.instrsUsed[1] = (Instruction)f.arg2;
                    }
                    if ( f.arg2 instanceof  Instruction && !((Instruction)f.arg2).state.schedule ) f.state.unresolveArgument++;
                }

            }
            RegAllocator.memorySpace _space = rac.new memorySpace(globalSpace);
        	RegAllocator.registerContext _regCtx = rac.new registerContext(space);
            //Frame+RA
            _space.offset(-8);

            //Reserve for variable
            _space.offset(-4 * func.numParams);
            for (Instruction loadI: func.paraLoad) {
                RegAllocator.memorySpace.memoryPosition pos = _space.reserve();
                loadI.arg1 = new Constant(pos.address);
            }

            _space.raPosReserve();
            _space.reserveForRegister(regCtx.regToPreserve().size());

            ScheduledBlock funcBlock = new ScheduledBlock(_regCtx, func.enter, _space, null);

        	functionBlocks.put(func, funcBlock);
        }

        if (Compiler.debug) {
            System.out.print("Main Size: " + mainBlock.instructions.size() + "\n");
            System.out.print(this);
        }
    }

    public ArrayList beforeFlush;

    /*public ArrayList<outputInstruction> functionCall(Function func, RegAllocator.registerContext ctx) {

    }*/

    public ArrayList<outputInstruction>sumbitVariable(ArrayList<Instruction> parameters, RegAllocator.memorySpace space) {
        ArrayList<outputInstruction> oi = new ArrayList<outputInstruction>();
        //Reserve space from register 1 to the end
        for (int i = 1; i < RegAllocator.numberOfRegister; i++) {
            //Reserve memory space for context switch
            space.reserve();
        }
        //Load all value into stack
        for (Instruction parameter: parameters) {
            //Load it to register, then flush it to stack

        }
        return oi;
    }

    public ArrayList<outputInstruction> beforeCall(RegAllocator.registerContext ctx) {
        beforeFlush = new ArrayList();
        ArrayList oi = new ArrayList();
        RegAllocator.Register[] registers = ctx.registers;
        for (int i = 1; i < registers.length; i++) {
            RegAllocator.Register reg = registers[i];
            if (reg.currentValue != null) {
                beforeFlush.add(reg.currentValue);
                oi.add(reg.preserveMemory());
            } else {
                beforeFlush.add("Empty");
            }
        }
        return oi;
    }

    public ArrayList<outputInstruction> priorFunctionCall(RegAllocator.registerContext ctx) {


        ArrayList oi = beforeCall(ctx);
        //Drop the first register for function return
        if (beforeFlush.get(0) != "Empty") {
            beforeFlush.remove(0);
            beforeFlush.add(0, "Empty");
            oi.remove(0);
        }
        return oi;
    }
    public ArrayList<outputInstruction> postFuncCall(RegAllocator.registerContext ctx) {
        ArrayList<outputInstruction> oi = new ArrayList<outputInstruction>();

        for (int i = 1; i < beforeFlush.size(); i++) {
            int registerIndex = i+1;
            if (beforeFlush.get(i) != "Empty") {
                outputInstruction o = ctx.registers[registerIndex].updateValue((InstructionValue) beforeFlush.get(i));
                oi.add(o);
            }
        }
        return oi;
    }

    public ArrayList<outputInstruction> postProdCall(RegAllocator.registerContext ctx) {
        ArrayList<outputInstruction> oi = new ArrayList<outputInstruction>();
        for (int i = 0; i < beforeFlush.size(); i++) {
            int registerIndex = i+1;
            if (beforeFlush.get(i) != "Empty") {
                outputInstruction o = ctx.registers[registerIndex].updateValue((InstructionValue) beforeFlush.get(i));
                oi.add(o);
            }
        }
        return oi;
    }

    @Override
    public String toString() {
        //Functions
        String func = "";
        for (Map.Entry<Function, ScheduledBlock> e: functionBlocks.entrySet()) {
            func += e.getKey().ident + ": \n" + e.getValue() +"\n\n";
        }
        return func+"Main: \n"+mainBlock;
    }
}
