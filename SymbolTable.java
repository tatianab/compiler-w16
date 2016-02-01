/* File: SymbolTable.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ArrayList;

public class SymbolTable {
	// Class representing one-way lookup symbol table for
	// variables and constants

	public enum SymbolType {
		Constant, Variable, Array
	}

	public class Data {
		public SymbolType type;					 // Array, var, constant. Procedure or function too?
		public ValueInstance current;  // For SSA form.
		public ArrayList<Integer> size;
		public Data() {

		}
		public boolean readonly () { return type == SymbolType.Constant; }
		// Need more stuff for arrays.
		public void newRevision(VariableInstance newInstance) {
			VariableInstance _current = (VariableInstance)current;
			newInstance.previous = _current;
			_current.next = newInstance;
			newInstance.next = null;
			_current = newInstance;
		}
	}

	public class ValueInstance {
		public ArrayList<Instruction> previousUsed;
	}

	public class VariableInstance extends ValueInstance {
		public int id;
		public ValueInstance previous;
		public ValueInstance next;
		public Instruction assigmentInstruction;
		public int basedMemoryAddress;
	}

	public class ConstantInstance extends ValueInstance {
		public int value;
	}

	public class ArrayIndex {
	}

	public class ConstantArrayIndex: ArrayIndex {
		public int value;
	}

	public class ComputationArrayIndex: ArrayIndex {
		public Instruction instruction;
	}

	public class Array extends VariableInstance {
		//The Array is stored in jounal fashion
		public ArrayList<ArrayIndex> indice;
	}

	private Hashtable<String, Data> hashtable;

	public SymbolTable() {
		hashtable = new Hashtable();
	}

	public void add(String name, SymbolType type) {
		Data newRecord = new Data();
		newRecord.type = type;
		//This part is only being called from the lookup
		if (type == SymbolType.Constant) {
			if (isInteger(name)) {
				//This is a constant, so initial the value during the add phrase
				int value = Integer.parseInt(name);
				ConstantInstance instance = new ConstantInstance();
				instance.value = value;
				newRecord.current = instance;
			} else {
				//Remind the constant is not a constant
				//The variable has not been initialized yet, complaint
				System.out.printf("\"name\" is neither a constant nor a existed variable. \n", name);
				System.exit(1);
			}
		}
		hashtable.put(name, newRecord);
	}

	public boolean isInteger(String s) {
		try { 
			Integer.parseInt(s); 
		} catch(NumberFormatException e) { 
			return false; 
		} catch(NullPointerException e) {
			return false;
		}
    	// only got here if we didn't return false
		return true;
	}


	public ValueInstance lookup(String name) {
		// TODO
		Data d = hashtable.get(name);
		if (d == null) {
			if (isInteger(name)) {
				//This is a constant that has not been encounter before
				add(name, SymbolType.Constant);
				d = hashtable.get(name);
			} else {
				//Remind the variable has not been declare, or it's a constant with typo
				System.out.printf("\"name\" is neither a constant nor a existed variable. \n", name);
				System.exit(1);
			}
		}

		ValueInstance instance = d.current;
		if (instance != null)
			return instance;
		else {
			//The variable has not been initialized yet, complaint
			System.out.printf("Variable %s has not been initialized yet. \n", name);
			System.exit(1);
			return null;
		}
	}

}