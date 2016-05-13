/* File: Array.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
import java.util.ArrayList;

public class Array extends Value {
	int id;
	String ident;
	int numDims; // Number of dimensions.
	int[] dims;  // Dimensions.
	int totalSize = 1;

	RegAllocator.memorySpace.memoryPosition backstorePos;

	Value[] currentIndices;

	ArrayList<Integer> collectDims; // Stores dimensions before we know how many dimensions there are.

	// Constructor
	public Array() {
		// TODO
		collectDims = new ArrayList<Integer>();
	}

	public void addDim(int dim) {
		collectDims.add(dim);
	}

	public void commitDims() {
		int i = 0;
		dims = new int[collectDims.size()];
		for (Integer dim : collectDims) {
			dims[i] = dim;
			totalSize *= dim;
			i++;
		}
		numDims = i;
		collectDims = null;
	}

	public void initFromMemorySpace(RegAllocator.memorySpace space) {
		backstorePos = space.reserveArray(totalSize);
	}

	private ArrayList<InstructionSchedule.outputInstruction> addrCalcInstr(ArrayList<Value> indices) {
		//Just keep multiply
		ArrayList<InstructionSchedule.outputInstruction> ois = new ArrayList<>();
		//TODO implment index calculation
		return ois;
	}

	public ArrayList<InstructionSchedule.outputInstruction> loadInstr(RegAllocator.registerContext ctx, int regLoadPos, ArrayList<Value> indices) {
		ArrayList<InstructionSchedule.outputInstruction> ois = new ArrayList<InstructionSchedule.outputInstruction>();
		InstructionSchedule is = new InstructionSchedule();
		//Load original index
		{
			InstructionSchedule.outputInstruction loadInit = is.new outputInstruction();
			loadInit.op = Instruction.move;
			loadInit.constant1 = 1;
			loadInit.outputReg = ctx.memoryOpRegister().registerID;
			ois.add(loadInit);
		}
		//Calculate address
		ois.addAll(addrCalcInstr(indices));
		{
			InstructionSchedule.outputInstruction subInstr = is.new outputInstruction();
			subInstr.op = Instruction.sub;
			subInstr.arg1 = ctx.memoryOpRegister().registerID;
			subInstr.constant2 = totalSize;
			subInstr.outputReg = ctx.memoryOpRegister().registerID;
			ois.add(subInstr);
			//It will become a negative value, which added to the heap
		}
		//Read value
		{
			InstructionSchedule.outputInstruction loadInstr = is.new outputInstruction();
			loadInstr.op = Instruction.load;
			loadInstr.arg1 = ctx.heapPtrRegister().registerID;
			loadInstr.arg2 = ctx.memoryOpRegister().registerID;
			loadInstr.outputReg = ctx.memoryOpRegister().registerID;
			ois.add(loadInstr);
		}
		return ois;
	}

	public ArrayList<InstructionSchedule.outputInstruction> storeInstr(RegAllocator.registerContext ctx, int regStorePos, ArrayList<Value> indices) {
		ArrayList<InstructionSchedule.outputInstruction> ois = new ArrayList<InstructionSchedule.outputInstruction>();
		InstructionSchedule is = new InstructionSchedule();
		//Load original index
		{
			InstructionSchedule.outputInstruction loadInit = is.new outputInstruction();
			loadInit.op = Instruction.move;
			loadInit.constant1 = 1;
			loadInit.outputReg = ctx.memoryOpRegister().registerID;
			ois.add(loadInit);
		}
		//Calculate address
		ois.addAll(addrCalcInstr(indices));
		{
			InstructionSchedule.outputInstruction subInstr = is.new outputInstruction();
			subInstr.op = Instruction.sub;
			subInstr.arg1 = ctx.memoryOpRegister().registerID;
			subInstr.constant2 = totalSize;
			subInstr.outputReg = ctx.memoryOpRegister().registerID;
			ois.add(subInstr);
			//It will become a negative value, which added to the heap
		}
		//Read value
		{
			InstructionSchedule.outputInstruction loadInstr = is.new outputInstruction();
			loadInstr.op = Instruction.store;
			loadInstr.arg1 = ctx.heapPtrRegister().registerID;
			loadInstr.arg2 = ctx.memoryOpRegister().registerID;
			loadInstr.outputReg = ctx.memoryOpRegister().registerID;
			ois.add(loadInstr);
		}
		return ois;
	}

	public void setCurrentIndices(Value[] indices) {
		if (indices.length == dims.length) {
			currentIndices = indices;
		} else {
			Compiler.error("Array indices and dimensions mismatch");
		}
	}

	public static String indicesToString(Value[] indices) {
		String result = "";
		for (Value index : indices) {
			result += "[" + index.shortRepr() + "]";
		}
		return result;
	}

	@Override
	public String shortRepr() {
		String result = ident;
		for (int i : dims) {
			result += "[" + i+ "]";
		}
		return result;
	}

}
