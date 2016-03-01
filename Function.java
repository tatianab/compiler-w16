/* File: Function.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

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
	
	public Function(int id, String ident) {
		this.id = id;
		this.ident = ident;
		numParams = 0;
	}

	public Function(int id, String ident, int numParams) {
		this.id = id;
		this.ident = ident;
		this.numParams = numParams;
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
		for (Value val : params) {
			result += val.shortRepr();
		}
		result += ")";
		return result;
	}

}