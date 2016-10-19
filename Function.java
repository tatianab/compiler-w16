/* File: Function.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Function extends Value {

	public int id;
	public String ident;

	public int numParams;
	public String[] formalParams;

	public boolean builtIn;

	public Block enter;
	public Block exit;
	public IntermedRepr program; // The program that contains this function.

	// public Instruction returnInstr;

	public  ArrayList<Variable> paras = new ArrayList<>();
	public ArrayList<Instruction> paraLoad = new ArrayList<>();

	public ArrayList<Instruction> instrs;

	public ArrayList<Array> arrays; // Arrays in this function.

	public ArrayList<Global> globalsUsed;
	public ArrayList<Global> globalsModified;

	public boolean isProc;

	public Value returnValue;

	
	public Function(int id, String ident, boolean isProc, IntermedRepr program) {
		this.id = id;
		this.ident = ident;
		numParams = 0;
		this.instrs = new ArrayList<Instruction>();
		this.arrays = new ArrayList<Array>();
		this.isProc = isProc;
		this.globalsUsed     = new ArrayList<Global>();
		this.globalsModified = new ArrayList<Global>();
		this.program = program;
	}

	public Function(int id, String ident, int numParams, IntermedRepr program) {
		this.id = id;
		this.ident = ident;
		this.numParams = numParams;
		this.instrs = new ArrayList<Instruction>();
		this.arrays = new ArrayList<Array>();
		this.globalsUsed     = new ArrayList<Global>();
		this.globalsModified = new ArrayList<Global>();
		this.program = program;
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

	public void end() {
		// this.exit = exit;

		// // Load globals.
		// for (Variable global : globalsUsed) {
	 //        Instruction instr = program.createInstr();
		// 	instr.setOp(Instruction.load);
		// 	instr.setArgs(global);
		// 	enter.addToEnd(instr);
		// 	// global.replaceUses(this, instr);
		// }
		
        // Store globals.
        for (Global g : globalsModified) {
	        Instruction instr = program.createInstr();
	        Value lastMod = g.getLastDef();
			instr.setOp(Instruction.store);
			instr.setArgs(lastMod, g);
			exit.addToEnd(instr);
        }

        // Store return value.
        if (returnValue != null) {
            Instruction instr = program.createInstr();
			instr.setOp(Instruction.store);
			instr.setArgs(returnValue, this);
			exit.addToEnd(instr);
        }
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
	public void addGlobalModification(Global g, Value v) {
		if (!globalsModified.contains(g)) {
			globalsModified.add(g);
		}
		g.lastDef = v;
	}

	public HashMap<Global, Instruction> globalLoad = new HashMap<>();

	// Handle globals that are USED by this function.
	public Instruction addGlobalUse(Global g) {
		// TODO

		if (globalsUsed.add(g)) {//!g.modified) {
			if (true) {
			// Add a load instruction and return it
			// Instruction instr = new Instruction(200, null);
			Instruction instr = program.createInstr();
			instr.setOp(Instruction.load);
			instr.setArgs(g);
			enter.addToEnd(instr);
				globalLoad.put(g, instr);
			return instr;
			}
		}
		return globalLoad.get(g);

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
