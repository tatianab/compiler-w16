/* File: Function.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
import java.util.ArrayList;
public class Function extends Value {

	public int id;
	public String ident;

	public int numParams;
	public String[] formalParams;

	public boolean builtIn;

	public Block enter;
	public Block exit;
	public IntermedRepr program; // The program that contains this function.

	public Instruction returnInstr;

	public ArrayList<Instruction> instrs;

	public ArrayList<Array> arrays; // Arrays in this function.

	public ArrayList<Variable> globalsUsed;
	public ArrayList<Variable> globalsModified;

	
	public Function(int id, String ident) {
		this.id = id;
		this.ident = ident;
		numParams = 0;
		this.instrs = new ArrayList<Instruction>();
		this.arrays = new ArrayList<Array>();
	}

	public Function(int id, String ident, int numParams) {
		this.id = id;
		this.ident = ident;
		this.numParams = numParams;
		this.instrs = new ArrayList<Instruction>();
		this.arrays = new ArrayList<Array>();
	}

	public boolean isMain() {
		return id == -1;
	}

	public int getNumParams() {
		return numParams;
	}

	public void setNumParams(int numParams) {
		this.numParams    = numParams;
		this.formalParams = new String[numParams];
		for (int i = 0; i < numParams; i++) {
			formalParams[i] = "var" + Integer.toString(i); 
		}
	}

	public void setFormalParams(String[] formalParams) {
		this.numParams = formalParams.length;
		this.formalParams = formalParams;
	}

	public void begin(Block enter) {
		this.enter = enter;
	}

	public void end(Block exit) {
		this.exit = exit;
	}

	// Generate a call to this function.
	public void generateCall(Instruction instr, Value[] params) {
		if (builtIn) {
			if (ident.equals("InputNum")) {
			    instr.setOp(Instruction.read);
			} else if (ident.equals("OutputNum")) {
				instr.setOp(Instruction.write);
				instr.setArgs(params[0]);
			} else if (ident.equals("OutputNewLine")) {
				instr.setOp(Instruction.writeNL);
			} else {
				Compiler.error("Non-existent built in function " + shortRepr() + ".");
			}
		} else {
			if (numParams == params.length) {
				instr.setOp(Instruction.call);
				instr.setArgs(this);
				instr.setParams(params);
			} else {
				Compiler.error("Too many or too few parameters given to function " + shortRepr() + ".");
			}
		}
	}

	public void addInstr(Instruction instr) {
		instrs.add(instr);
	}

	public void addArray(Array array) {
		arrays.add(array);
	}

	// Handle globals that are MODIFIED by this function.
	public void addGlobalModification(Variable var, Instruction instr) {
		// If this global has already been modified,
		// change the store entry.
		if (false) {
			// TODO
		}
		// If this global has not already been modified,
		// store it back in the EXIT block of the function.
		else {
			// TODO
		}
	}

	// Handle globals that are USED by this function.
	public void addGlobalUse(Variable var) {
		// If this variable has already been loaded,
		// do nothing.
		if (false) {
			// TODO
		}
		// Otherwise, load it into the ENTER block of the
		// function.
		else {
			// Instruction instr = program.createInstr();
			// instr.setOp(Instruction.move);
			// instr.setArgs(var, instr);
			// enter.addToEnd(instr);
		}
		
	}

	@Override
	public String shortRepr() {
		String result = ident + "("; // + numParams + ")";
		int i;
		for (i = 0; i < numParams - 1; i++) {
			result += formalParams[i] + ",";
		}
		if (numParams > 0) {
			result += formalParams[i];
		}
		result += ")";
		return result;
	}

	public static String paramsToString(Value[] params) {
		String result = "(";
		Value val;
		for (int i = 0; i < params.length - 1; i++) {
			val     = params[i];
			result += val.shortRepr() + ", ";
		}
		if (params.length != 0) {
			result += params[params.length - 1].shortRepr();
		}
		result += ")";
		return result;
	}

}