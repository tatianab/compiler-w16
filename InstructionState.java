import java.util.ArrayList;

public class InstructionState{

	public boolean schedule = false;

	public Instruction instr;
	public int unresolveArgument = 0;
	public float remainingAvailableChildSize = 0;
	public InstructionSchedule.InstructionValue valueRepr = null;
	InstructionState(Instruction _instr) {
		instr = _instr;
		if (instr.instrsUsed != null) {
			for (int i = 0; i < 2; i++) {
				if (instr.instrsUsed[i] != null && (i == 0 || (_instr.op != Instruction.move && _instr.op != Instruction.write))) {
					unresolveArgument++;
				}
			}
		}
		for (Instruction child: _instr.uses) {
			for (Instruction child_Arg: child.instrsUsed) {
				if (child_Arg == _instr)
					remainingAvailableChildSize += 0.5;
			}
		}
		storage = new storageState();
	}
	public class storageState {
		public RegAllocator.Register currentRegister = null;
		public RegAllocator.memorySpace.memoryPosition backstore = null;
		public boolean loaded() { return currentRegister!=null; }
		public ArrayList<InstructionSchedule.outputInstruction> load(RegAllocator.Register reg, RegAllocator.memorySpace space) {
			if (loaded()) return null;
			else {
				return space.load(reg, backstore);
			}
		}
	}
	public storageState storage;
	//How fast could the parent value be released
	public float parentReleaseScore() {
		Value a1 = instr.arg1;
		Instruction instrA = null;
		if (a1 instanceof Instruction) {
			instrA = (Instruction)a1;
		}
		Instruction instrB = null;
		if (instr.op != Instruction.move) {
			Value a2 = instr.arg2;
			if (a2 instanceof Instruction) {
				instrB = (Instruction)a2;
			}
		} else {
			if (instrA == null) {
				//Move a constant into register, do it at the last minute
				//As the constant will jsut occupy one register, while freeing non
				return 100000000;
			} else {
				return instrA.state.remainingAvailableChildSize;
			}
		}
		if (instrB == null) {
			return instrA.state.remainingAvailableChildSize;
		}
		float min = Math.min(instrA.state.remainingAvailableChildSize, instrB.state.remainingAvailableChildSize);
		float mul = instrA.state.remainingAvailableChildSize * instrB.state.remainingAvailableChildSize;
		return Math.min(min, mul);
	}
	public void scheduled() {
		if (instr.arg1 instanceof Instruction) {
			//Argument 1 is an instruction
			((Instruction)instr.arg1).state.remainingAvailableChildSize -= 0.5;
			((Instruction)instr.arg1).state.valueRepr.usageCount -= 1;
			((Instruction)instr.arg1).state.valueRepr.referenceCount -= 1;
			((Instruction)instr.arg1).state.valueRepr.upcomingUsageCount -= 1;
			((Instruction)instr.arg1).state.valueRepr.upcomingReferenceCount -= 1;
		}
		if (instr.arg2 instanceof Instruction && instr.op != Instruction.move) {
			//Argument 1 is an instruction
			((Instruction)instr.arg2).state.remainingAvailableChildSize -= 0.5;
			((Instruction)instr.arg2).state.valueRepr.usageCount -= 1;
			((Instruction)instr.arg2).state.valueRepr.referenceCount -= 1;
			((Instruction)instr.arg2).state.valueRepr.upcomingUsageCount -= 1;
			((Instruction)instr.arg2).state.valueRepr.upcomingReferenceCount -= 1;
		}
		if (instr.arg1 == instr.arg2 && instr.arg1 != null) {
			((Instruction)instr.arg2).state.valueRepr.referenceCount += 1;
			((Instruction)instr.arg2).state.valueRepr.upcomingReferenceCount += 1;

		}
		schedule = true;
	}
}