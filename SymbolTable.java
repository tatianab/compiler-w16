/* File: SymbolTable.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.Hashtable;
import java.util.LinkedList;

public class SymbolTable {
	// Class representing one-way lookup symbol table for
	// variables and constants

	public enum SymbolType {
		Constant, Variable, Array
	}

	public class Data {
		public SymbolType type;					 // Array, var, constant. Procedure or function too?
		public ValueInstance current;  // For SSA form.
		public Bool readonly () { return type == Constant; }
		// Need more stuff for arrays.
		public newRevision(VariableInstance newInstance) {
			newInstance.previous = current;
			current.next = newInstance;
			newInstance.next = NULL;
			current = newInstance;
		}
	}

	public class ValueInstance {
		public int value;
	}

	public class VariableInstance extends ValueInstance {
		public int id;
		public ValueInstance previous;
		public ValueInstance next;
	}

	public class ConstantInstance extends ValueInstance {
	}

	public class Array extends VariableInstance {
		//The Array is stored in jounal fashion
		public int size;
	}

	private Hashtable<String, Data> hashtable;

	public SymbolTable() {
		hashtable = new Hashtable();
	}

	public void add(String name, SymbolType type) {
		Data newRecord = new Data;
		newRecord.type = type;
		if (type == Constant) {
			if (isInteger(name)) {
				//This is a constant, so initial the value during the add phrase
				int value = Integer.parseInt(name);
				ConstantInstance instance = new ConstantInstance;
				instance.value = value;
				newRecord.current = instance;
			} else {
				//Remind the constant is not a constant
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
		if (d == NULL) {
			if (isInteger(name)) {
				//This is a constant that has not been encounter before
				add(name, Constant);
				d = hashtable.get(name);
			} else {
				//Remind the variable has not been declare, or it's a constant with typo
			}
		}

		ValueInstance instance = d.current;
		if (instance != NULL)
			return instance;
		else {
			//The variable has not been initialized yet
		}
	}

}