import java.util.ArrayList;

public class InstructionSchedule {


	public class InstructionValue {
		public Instruction basedInstr;
        //Usage count means how many instructions use this value
		public int usageCount;
        //Reference count means how many times the instruction being used as an argument
		public int referenceCount;
        //Usage count means how many instructions in this block use this value
		public int upcomingUsageCount;
        //Reference count means how many times the instruction in this block being used as an argument
		public int upcomingReferenceCount;

		InstructionValue(Instruction instr, Block currentBlock) {
			basedInstr = instr;
			for (Instruction parent : instr.uses) {
				boolean sameBlock = (instr.block == currentBlock);
				usageCount++;
				referenceCount++;
				if (sameBlock) {
					upcomingReferenceCount++;
					upcomingUsageCount++;
				}
			}
		}

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
			return referenceCount == 0;
		}
		public int score() {
			return referenceCount;
		}
	}

	public class outputInstruction {
		public int op;
		public int outputReg;
		public int arg1, arg2;
		public int constant2;
		outputInstruction() {
			op = -1;
			outputReg = -1;
			arg1 = -1;
			arg2 = -1;
		}
		outputInstruction(Instruction instr) {
        	//Search for arg1
			int reg1 = -1;
        	//Search for arg2
			int reg2 = -1;
        	//Search for output register, register its present
			int outReg = -1;

			op = instr.op;
			outputReg = outReg;
			arg1 = reg1;
			arg2 = reg2;
		}

	}

	public class ScheduledBlock {
		public ScheduledBlock next1, next2;
		public ArrayList<outputInstruction> instructions;
		public ArrayList<Instruction> directDependent;
		public Block referenceBlock;

		public Instruction pickInstr(ArrayList<Instruction> instrs) {
			int score = -1;
			Instruction instr = null;
			for (Instruction currentInstr : instrs) {
				int currentScore = 0;
				if (score == -1 || score > currentScore) {
					//There is a better instruction
					score = currentScore;
					instr = currentInstr;
				}
			}
			return instr;
		}

		public outputInstruction scheduleInstruction(Instruction instr, RegAllocator.registerContext context) {
			//Get register, set the value to the register
			int nextRegID = context.emptyRegister();
			RegAllocator.Register nextReg =  context.registers[nextRegID];
			//The register is empty, so just write the value
			InstructionValue value = new InstructionValue(instr, referenceBlock);
			nextReg.updateValue(value);
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
			//Loop for all variable
			int score = 100000;	Instruction ref;
			for (Instruction instr : instrs) {
				//if (score > ref.)
			}
			return ref;
		}

		ScheduledBlock(RegAllocator.registerContext previousContext, Block inputBlock) {
			referenceBlock = inputBlock;
			//Current Context
			RegAllocator rc = new RegAllocator();
			RegAllocator.registerContext context = rc.new registerContext();

			//Unprocess block: not scheduled instructions
			ArrayList<Instruction> unprocessBlock = new ArrayList<Instruction>();
			//Available instructions: value availabled in the system
			ArrayList<Instruction> availableInstruction = new ArrayList<Instruction>();
			//Cached instructions: value availabled in the register
			ArrayList<Instruction> cachedInstruction = new ArrayList<Instruction>();

			ArrayList<Instruction> dependencyList = new ArrayList<Instruction>();

			//Get all the instructions in the block into unprocess
			Instruction currentInstr = inputBlock.begin;
			while (currentInstr != null) {
				unprocessBlock.add(currentInstr);
				for (Instruction instr : currentInstr.instrsUsed) {
					dependencyList.add(instr);
				}
				currentInstr = currentInstr.next;
			}

			//Load buffer: the elements that should be scheduled to load
			ArrayList<RegAllocator.memorySpace.memoryPosition> loadRegBuffer = new ArrayList<RegAllocator.memorySpace.memoryPosition>();
			//Load buffer: the elements that should be scheduled to load
			ArrayList<outputInstruction> loadInstrBuffer = new ArrayList<outputInstruction>();
			//Save buffer: the elements that should be scheduled to load
			ArrayList<outputInstruction> saveInstrBuffer = new ArrayList<outputInstruction>();
			//Instruction Buffer: the instruction that could carried out immediately
			ArrayList<outputInstruction> instrBuffer = new ArrayList<outputInstruction>();

			//Newly Calculated: Calculated value that has not been used to add more instruction into the system
			ArrayList<Instruction> newlyCalculated = new ArrayList<Instruction>();

			//As long as there is instruction un-scheduled
			while (unprocessBlock.size() > 0) {

				for (Instruction child: unprocessBlock) {
					if (child.state.unresolveArgument == 0) {
						//If the instruction is available to schedule, it promopt to available
						boolean cached = true;
						for (Instruction parent: child.instrsUsed) {
							if (!parent.state.storage.loaded())
								cached = false;
						}
						if (cached) {
							cachedInstruction.add(child);
						} else {
							availableInstruction.add(child);
						}
						unprocessBlock.remove(child);
					}
				}
				while (previousContext.hasSpace() && cachedInstruction.size() > 0) {
					//As long as there is instruction available:
					//If there is space in the register, use it to do the instruction that has the largest impact:
					//Free up more register (last hold out data) (Using the heuristic)
					//If there is still space, reverse two for move instruction, which will dynamically schedule N instructions later than the last load instruction
					Instruction instr = pickInstr(cachedInstruction);
					outputInstruction oi = scheduleInstruction(instr, context);
					//Transform it to register based instruction
					//"Solidify" it
					instrBuffer.add(oi);
					newlyCalculated.add(instr);
					unprocessBlock.remove(instr);
					cachedInstruction.remove(instr);
					cachedInstruction.addAll(instr.uses);
				}

				for (Instruction processed: newlyCalculated) {
					for (Instruction child: processed.uses) {
						//A depended value is calculated->
						child.state.unresolveArgument--;			
					}
				}

				//Finally, no instruction available, or no space available
				//See which problem it is
				if (previousContext.hasSpace() == false) {
					//If there is no space, flush
					ArrayList<Integer> nextFlush = context.registerToFlushTo();
					for (Integer i : nextFlush) {
						RegAllocator.Register r = context.registers[i];
						//Set the instruction back to storage
						Instruction storageInstr = r.currentValue.basedInstr;
						for (Instruction child : storageInstr.uses) {
							if (cachedInstruction.contains(child)) {
								//If the value is in cached, move from cached to available
								cachedInstruction.remove(child);
								availableInstruction.add(child);
							}
						}
						outputInstruction oi = r.preserveMemory();
						if (oi != null)
							saveInstrBuffer.add(oi);
					}
				} else if (cachedInstruction.size() == 0) {
					//No instruction, pull
					//Choose the stored variable with the lowest usage rate -> reduce the chance of requiring more register, and free up quickly
					//Then add the instruction from the available pool
					Instruction instr = pickNextCache(dependencyList);
					outputInstruction oi = instr.state.storage.load();

					for (Instruction child : instr.uses) {
						if (availableInstruction.contains(child)) {
							//If the value is in cached, move from cached to available
							cachedInstruction.add(child);
							availableInstruction.remove(child);
						}
					}

					if (oi != null) {
						loadInstrBuffer.add(oi);
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
			}

			//Get the set of the instructions dependent
			//This is used to for the previous block to have component scheduled
			if (inputBlock.block1) {
				next1 = new ScheduledBlock(context, inputBlock.block1);
			} else next1 = null;

			if (inputBlock.block2) {
				next2 = new ScheduledBlock(context, inputBlock.block2);
			} else next2 = null;

			instructions = new ArrayList<InstructionSchedule.outputInstruction>();
		}
	}
	public ScheduledBlock mainBlock;
}