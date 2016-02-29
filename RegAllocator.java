/* File: RegAllocator.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.ArrayList;

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
    public ArrayList<VariableAllocation> allocation() {
        int numberOfRegisterAvailable = numberOfRegister-2;
        Variable registers[] = new Register[numberOfRegisterAvailable];
        for (int i = 0; i < numberOfRegisterAvailable; i++)
            registers[i] = null;
        //TODO: pick the variable that is last to use in the upcoming instructions (first use first priority), for all variables
        //Schedule method: get all the upcoming instuctions in the block, add dependency, add at much instruction as possible. When a variable free up, import another
    }

	/* Elimination of phi instructions (replace with move instructions).

	 */

}