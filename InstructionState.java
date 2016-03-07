public class InstructionState{
	public Instruction instr;
	public int unresolveArgument = 0;
	public float remainingAvailableChildSize = 0;
	public class storageState {
		public RegAllocator.Register currentRegister;
		public RegAllocator.memorySpace.memoryPosition backstore;
		public boolean loaded() { return currentRegister!=null; }
		public InstructionSchedule.outputInstruction load(int regNo, RegAllocator.memorySpace space) {
			if (loaded()) return null;
			else {
				return space.load(regNo, backstore);
			}
		}
	}
	public storageState storage;
	//How fast could the parent value be released
	public float parentReleaseScore() {
		Value a1 = instr.arg1;
		Instruction instrA = null;
		if (a1 instanceof Instruction) {
			instrA = a1;
		}
		Instruction instrB;
		if (instr.op != move) {
			Value a2 = instr.arg2;
			if (a2 instanceof Instruction) {
				instrB = a2;
			}
		} else {
			instrB = null;
			if (instrA == null) {
				//Move a contant into register, do it now
				return 0;
			} else {
				return instrA.remainingAvailableChildSize;
			}
		}
		if (a2 == null) {
			return instrA.remainingAvailableChildSize;
		}
		float min = Math.min(instrA.remainingAvailableChildSize, instrB.remainingAvailableChildSize);
		float mul = instrA.remainingAvailableChildSize * instrB.remainingAvailableChildSize;
		return Math.min(min, mul);
	}
}