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

		InstructionValue() {}

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
		public int constant1;
		public int constant2;
		public Block jumpBlock;
		outputInstruction() {
			op = -1;
			outputReg = -1;
			arg1 = -1;
			arg2 = -1;
		}
		outputInstruction(Instruction instr, int outputRegistetNo) {
        	//Search for arg1
			int reg1 = -1;
			if (instr.arg1 instanceof Instruction) {
				//It is instruction, it must have an register
				Instruction instr1 = (Instruction)instr.arg1;
				reg1 = instr1.state.storage.currentRegister.registerID;
			} else if (instr.arg1 instanceof Constant) {
				Constant c = (Constant)instr.arg1;
				constant1 = c.getVal();
			}
        	//Search for arg2
			int reg2 = -1;

			if (instr.op != Instruction.move) {
				//If it's not move instruction, it should be have the second argument
				if (instr.arg2 instanceof Instruction) {
					//It is instruction, it must have an register
					Instruction instr2 = (Instruction)instr.arg2;
					System.out.println("Probe: ");
					System.out.println(instr);
					System.out.println(instr2);
					reg2 = instr2.state.storage.currentRegister.registerID;
				} else if (instr.arg2 instanceof Constant) {
					Constant c = (Constant)instr.arg2;
					constant2 = c.getVal();
				} else if (instr.arg2 instanceof Block) {
					Block b = (Block)instr.arg2;
					jumpBlock = b;
				}

			}

        	//Search for output register, register its present
			int outReg = outputRegistetNo;

			op = instr.op;
			outputReg = outReg;
			arg1 = reg1;
			arg2 = reg2;
		}

		@Override
		public String toString() {
			String result = Instruction.ops[op] + " ";
			result += "R" + outputReg + " ";
			if (arg1 >= 0)
				result += "R" + arg1 + " ";
			else result += "C" + constant1 + " ";
			if (arg2 >= 0)
				result += "R" + arg2;
			else result += "C" + constant2;
			return result;
		}

	}

	public class ScheduledBlock {
		public ScheduledBlock next1, next2;
		public ArrayList<outputInstruction> instructions = new ArrayList<outputInstruction>();
		public ArrayList<Instruction> directDependent;
		public Block referenceBlock;
		public RegAllocator.registerContext context;

		@Override
		public String toString() {
			String output = "Block:"+referenceBlock.id+" \n";
			output += "#instructions: " + instructions.size() + "\n";
			for (outputInstruction oi : instructions) {
				output += oi+"\n";
			}
			//Next
			if (next1 != null) {
				output += "Next 1: \n";
				output += next1 + "\n";
			}
			//Next
			if (next2 != null) {
				output += "Next 2: \n";
				output += next2 + "\n";
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
				if (currentInstr.op == Instruction.bra || (currentInstr.op >= Instruction.bne && currentInstr.op <= Instruction.ble) ) {
					//Make sure branch is the last to go
					currentScore = -1;
				}
				if (score == -1 || score > currentScore) {
					//There is a better instruction
					score = currentScore;
					instr = currentInstr;
				}
			}
			return instr;
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

		public outputInstruction scheduleInstruction(Instruction instr, RegAllocator.registerContext context) {
			//Get register, set the value to the register
			System.out.println(instr);
			int nextRegID = -1;

			InstructionValue value = new InstructionValue(instr, referenceBlock);
			System.out.print("Instr: "+instr+"\n");
			instr.state.valueRepr = value;

			if (instructionWouldReturn(instr)) {
				nextRegID = context.emptyRegister();
				RegAllocator.Register nextReg =  context.registers[nextRegID];
				System.out.println("Instruction: "+instr+" Register ID: "+nextRegID);
				//The register is empty, so just write the value
				outputInstruction release = nextReg.updateValue(value);
				assert(release == null);//Should be null, meaning no overwriting the variable
				
			}
			outputInstruction oi = new outputInstruction(instr, nextRegID);
			//Tell the instrcution depended on that it's releasing
			instr.state.scheduled();
			return oi;
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
			for (Instruction instr: instrs) {
				//First loop, get an instruction that require one object
				int dependCount = 0;	Instruction depend = null;
				for (Instruction parent : instr.instrsUsed) {
					System.out.println("Parent Instr: "+parent);
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

		ScheduledBlock(RegAllocator.registerContext previousContext, Block inputBlock, RegAllocator.memorySpace space) {

			inputBlock.schedule = this;

			instructions = new ArrayList<InstructionSchedule.outputInstruction>();

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
								if (instr != null) dependencyList.add(instr);
					}
				} else if (currentInstr.op == Instruction.phi) {
					RegAllocator.phiRequest req = rc.new phiRequest();
					req.value1 = ((Instruction)currentInstr.arg1);
					req.value2 = ((Instruction)currentInstr.arg2);
					req.actualStmt = new InstructionValue(currentInstr, inputBlock);
					currentInstr.state.valueRepr = req.actualStmt;

					phiInstructions.add(req);
				} else {
					//Remaining: end instruction
					endOfBlock = true;
				}
				currentInstr = currentInstr.next;
			}
			boolean ifJoin = true;
			if (phiInstructions.size() > 0) {
				//There is phi instruction
				//Check phi status
				//There should have two input
				System.out.println("PHI");
				ifJoin = (inputBlock.id > inputBlock.in1.id) && (inputBlock.id > inputBlock.in2.id);
				if (ifJoin) {
					RegAllocator.phiMergerResult result = RegAllocator.phiMerger(inputBlock.in1.schedule.context, inputBlock.in2.schedule.context, phiInstructions);
					//TODO: Do something to the blocks
				} else {
					if (inputBlock.in1.schedule != null && inputBlock.in2.schedule != null) {
						//Both input block is scheduled, so just patch the remain
					} else {
						//Only one input block is done, so just do a passover
						System.out.println("Patching");
						RegAllocator.phiPassover(inputBlock, phiInstructions);
					}
				}
				//Phi-ed
				System.out.println(context);
			}
			afterPhiCtx= rc.new registerContext(context);

			//Newly Calculated: Calculated value that has not been used to add more instruction into the system
			ArrayList<Instruction> newlyCalculated = new ArrayList<Instruction>();

			//As long as there is instruction un-scheduled
			while (unprocessBlock.size() > 0) {

				//Load buffer: the elements that should be scheduled to load
				ArrayList<outputInstruction> loadInstrBuffer = new ArrayList<outputInstruction>();
				//Save buffer: the elements that should be scheduled to load
				ArrayList<outputInstruction> saveInstrBuffer = new ArrayList<outputInstruction>();
				//Instruction Buffer: the instruction that could carried out immediately
				ArrayList<outputInstruction> instrBuffer = new ArrayList<outputInstruction>();

				for (Instruction child: unprocessBlock) {
					System.out.print("child: "+child+" "+child.state.unresolveArgument+"\n");
					if (child.state.unresolveArgument == 0) {
						//If the instruction is available to schedule, it promopt to available
						boolean cached = true;
						if (child.instrsUsed != null)
							for (Instruction parent: child.instrsUsed) {
								if (parent != null)
									System.out.print("Parent: "+parent+"\n");
								if (parent != null && ( !parent.deleted() && !parent.state.storage.loaded()) )
									cached = false;
							}
						if (cached) {
							availableInstruction.remove(child);
							cachedInstruction.remove(child);
							cachedInstruction.add(child);
						} else {
							availableInstruction.remove(child);
							availableInstruction.add(child);
							cachedInstruction.remove(child);
						}
						//unprocessBlock.remove(child);
					}
				}

				System.out.println("# of unscheduled instruction: "+unprocessBlock.size()+"\n"+unprocessBlock);
				System.out.println("# of cached instruction: "+cachedInstruction.size());
				System.out.println("Scheduled: "+instructions);

				boolean allInstrNeedSpace = needSpace(cachedInstruction);

				while (context.hasSpace() && cachedInstruction.size() > 0) {
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
					for (Instruction _instr : instr.uses) {
						if (_instr.op != Instruction.phi &&
								!(_instr.op == Instruction.bra || (_instr.op >= Instruction.bne && _instr.op <= Instruction.ble))
						&& _instr.block == inputBlock) {
							cachedInstruction.remove(_instr);
							cachedInstruction.add(_instr);
						}
					}

					System.out.print("Schedule instruction: "+instr+"\n");
					System.out.print("Register Context: " + context + "\n");
				}

				for (Instruction processed: newlyCalculated) {
					for (Instruction child: processed.uses) {
						//A depended value is calculated->add it back
						child.state.unresolveArgument--;	
					}
				}

				//Finally, no instruction available, or no space available
				//See which problem it is
				if (unprocessBlock.size() > 0) {
					//If there is futhur unrelease stuff, load and unload stuff
					if (context.hasSpace() == false) {
						System.out.print(context + "\n");
						//If there is no space, flush
						ArrayList<Integer> nextFlush = context.registerToFlushTo();
						if (nextFlush.get(0) > 0) {
							int i = nextFlush.get(0);
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
						//Pick one register
						int nextRegID = context.emptyRegister();
						RegAllocator.Register r = context.registers[nextRegID];
				
						//No instruction, pull
						//Choose the stored variable with the lowest usage rate -> reduce the chance of requiring more register, and free up quickly
						//Then add the instruction from the available pool
						Instruction instr = pickNextCache(availableInstruction);
						if (instr != null) {
							outputInstruction oi = instr.state.storage.load(r, space);

							for (Instruction child : instr.uses) {
								if (availableInstruction.contains(child)) {
									//If the value is in cached, move from cached to available
									cachedInstruction.remove(child);
									cachedInstruction.add(child);
									availableInstruction.remove(child);
								}
							}

							if (oi != null) {
								loadInstrBuffer.add(oi);
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

				System.out.print("Schedule Stat: \n"
					+ "Load: " + loadInstrBuffer.size() + "\n"
					+ "Save: " + saveInstrBuffer.size() + "\n"
					+ "Instr: " + instrBuffer.size() + "\n"
					+ "Total: " + instructions.size() + "\n");
			}

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
				outputInstruction oi = scheduleInstruction(branch, context);
				instructions.add(oi);
			}

			//Get the set of the instructions dependent
			//This is used to for the previous block to have component scheduled
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
			if (inputBlock.fallThrough != null) {
				if (inputBlock.fallThrough.id > inputBlock.id)
					inputBlock.fallThrough.dependent--;
				if (inputBlock.fallThrough.dependent == 0 && inputBlock.fallThrough.schedule == null)
					next1 = new ScheduledBlock(context, inputBlock.fallThrough, space);
				if (phiInstructions.size() > 0 && !ifJoin) {
					//Has phi, not if join->while join
					RegAllocator.phiMergerResult result = RegAllocator.phiTransform(next1.context, afterPhiCtx, phiInstructions);
					next1.insertPhiTransfer(result.edge1);
				}
			} else next1 = null;

			if (inputBlock.branch != null) {
				if (inputBlock.branch.id > inputBlock.id)
					inputBlock.branch.dependent--;
				if (inputBlock.branch.dependent == 0 && inputBlock.branch.schedule == null)
					next2 = new ScheduledBlock(context, inputBlock.branch, space);
			} else next2 = null;

			if (endOfBlock) {
				//Add back the end instruction
				outputInstruction endInstr = new outputInstruction();
				endInstr.op = Instruction.end;
				instructions.add(endInstr);
			}
			System.out.print("Total: " + instructions.size() + "\n");
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
		RegAllocator.memorySpace space = rac.new memorySpace();
		RegAllocator.registerContext regCtx = rac.new registerContext(space);

		mainBlock = new ScheduledBlock(regCtx, repr.firstBlock, space);
		System.out.print("Main Size: " + mainBlock.instructions.size() + "\n");	
		System.out.print(this);	
	}
	@Override
	public String toString() {
		return "Main: \n"+mainBlock;
	}
}