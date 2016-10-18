/* File: Global.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
import java.util.ArrayList;

public class Global extends Value {
	public int id;                       // ID for this global
	public String ident;
	public ArrayList<FunctionData> functions;  

	boolean modified;
	Value lastDef;

	private static int nextAvailableId = 0;

	public Global(int id, String ident) {
		this.id       = id;
		this.ident    = ident;
		this.modified = false;
		this.lastDef  = null;
	}

	@Override
	public String shortRepr() {
		return "[Global " + ident + "]";
	}

	private class FunctionData {

		Function function;
		boolean used;
		boolean modified;

		Value lastDef; 
		ArrayList<Instruction> uses;

		public FunctionData() {

		}
	}

}