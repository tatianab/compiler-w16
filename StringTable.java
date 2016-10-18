/* File: StringTable.java
 * Author: Tatiana Bradley
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.Hashtable;
import java.util.ArrayList;

public class StringTable {
	/* Class representing two-way lookup symbol table for string identifiers
	   and ids. 
	 */
    
	private Hashtable<String, Integer> strings; // Lookup string --> id.
	private ArrayList<StringData>      ids;     // Lookup id --> string info.
	private int nextOpenID;
	private Function currentScope; 

	/* Constructor.
	 * Adds all reserved words to the string table.
	 */
	public StringTable() {
		nextOpenID = 0;
		strings = new Hashtable<String, Integer>(reserved.length);
		ids 	= new ArrayList<StringData>(reserved.length);

		String name;
		int id, token;

		// Add reserved words.
		for (int i = 0; i < reserved.length; i++) {
			name = reserved[i];
			id    = nextOpenID;
			token = reservedID[i];
			strings.put(name, id);
			ids.add(id, new StringData(id, name, token, null));
			nextOpenID++;
		}

		// Register built-in functions.
		Function func;
		int funcId;
		for (String funcName : builtInFunctions) {
			funcId = getID(funcName);
			func = new Function(funcId, funcName, false);
			currentScope = func;
			if (funcName.equals("OutputNum")) {
				func.setNumParams(1);
			}
			func.builtIn = true;
			declare(func, funcId);
		}

	}

	public StringTable(StringTable copy) {
		strings = new Hashtable<>(copy.strings);
		ids =  new ArrayList<>(copy.ids);
		nextOpenID = copy.nextOpenID;
	}

	// Add a new string to the string table.
	public void add(String name) {
		int id = nextOpenID;
		nextOpenID++;
		strings.put(name, id);
		ids.add(id, new StringData(id, name, ident, currentScope));
	}

	/* Methods related to variables, functions and arrays. */

	/* Get the variable currently associated with the given id.
	   Null if none exists. */
	public Variable getVar(int id) {
		return get(id).lastVar;
	}

	/* Get the array currently associated with the given id.
	   Null if none exists. */
	public Array getArr(int id) {
		return get(id).lastArr;
	}

	/* Get the (user-defined) function currently associated with the given id.
	   Null if none exists. */
	public Function getFunc(int id) {
		return get(id).lastFunc;
	}

	// Reset the "lastVar" data on the given id.
	// Used when there is a new assignment to a variable.
	public Global reassignVar(int id, Variable var) {
		get(id).lastVar = var;
		return get(id).global;
	}

	public void setScope(Function scope) {
		this.currentScope = scope;
	}

	// Declare a variable, array, or function.
	public Global declare(Value value, int id) {
		StringData data = get(id);
		if (value instanceof Variable) {
			data.lastVar = (Variable) value;
		    ((Variable) value).instance = -1;
		    ((Variable) value).ident = data.name;

		    if (currentScope.isMain()) {
				Global g = new Global(id, data.name);
				data.global = g;	
				return g;
			}		
		} else if (value instanceof Array) {
			data.lastArr = (Array) value;
			// ((Array) value).instance = -1;
			((Array) value).ident = data.name;
		} else if (value instanceof Function) {
			data.lastFunc = (Function) value;
		}
		return null;
	}

	public void declareFormalParam(int id) {
		Variable variable = new Variable(id);
		StringData data = get(id);
		data.lastVar = variable;
		variable.instance = 0;
		variable.ident = data.name;
	}

	/* StringData class to encapsulate information about each string in the
	   program. */
	private class StringData {
		/* Stores information about a string in the program. */
		public int id;           // ID number for this string.
		public String name;      // Name used in original program for this string.
		public int token;        // Token value of this string.

		public Variable lastVar;  // The last variable associated with this string.
		public Array    lastArr;  // The last array associated with this string.
		public Function lastFunc; // The last function associated with this string.

		public Function scope;    // The scope of the variable.

		public Global global;     // If a global variable.

		public StringData(int id, String name, int token, Function scope) {
			this.id       = id;
			this.name     = name;
			this.token    = token;
			this.scope    = scope;
			this.lastVar  = null;
			this.lastArr  = null;
			this.lastFunc = null;
			this.global   = null;
		}
	}

	/* Methods related to low-level conversions. */

	// Get the StringData object associated with the given id.
	public StringData get(int id) {
		return ids.get(id);
	}

	// Get the token associated with the given string.
	public int getSym(String name) {
		int id = strings.get(name);
		return get(id).token;
	}

	// Get the id associated with the given string.
	public int getID(String name) {
		return strings.get(name);
	}

	// Get the string associated with the given id.
	public String getName(int id) {
		return get(id).name;
	}

	// Check if the given string is already in the string table.
	public boolean member(String name) {
		return strings.containsKey(name);
	}

	// Only used for error messages.
	public String symToString(int sym) {
		if (sym == number) {
			return "Number";
		} else if (sym == ident) {
			return "Ident";
		} else {
			for (int i = 0; i < reserved.length; i++) {
				if (reservedID[i] == sym) {
					return reserved[i];
				}
			}
		}
		return "Symbol not found.";
	}

	/* Token values. */
	private static final int errorToken 	   = 0;
	private static final int timesToken 	   = 1;
	private static final int divToken   	   = 2;
	private static final int plusToken  	   = 11;
	private static final int minusToken 	   = 12;
	private static final int eqlToken		   = 20;
	private static final int neqToken		   = 21;
	private static final int lssToken		   = 22;
	private static final int geqToken		   = 23;
	private static final int leqToken		   = 24;
	private static final int gtrToken		   = 25;
	private static final int periodToken       = 30;
	private static final int commaToken		   = 31;
	private static final int openbracketToken  = 32;
	private static final int closebracketToken = 34;
	private static final int closeparenToken   = 35;
	private static final int becomesToken      = 40;
	private static final int thenToken		   = 41;
	private static final int doToken           = 42;
	private static final int openparenToken	   = 50;          
	private static final int number		       = 60;
	private static final int ident		       = 61;
	private static final int semiToken		   = 70;       
	private static final int endToken		   = 80;       
	private static final int odToken		   = 81;       
	private static final int fiToken		   = 82;       
	private static final int elseToken		   = 90;       
	private static final int letToken		   = 100;    
	private static final int callToken		   = 101;   
	private static final int ifToken		   = 102;   
	private static final int whileToken		   = 103;   
	private static final int returnToken	   = 104; 
	private static final int varToken		   = 110;   
	private static final int arrToken		   = 111;   
	private static final int funcToken		   = 112;   
	private static final int procToken		   = 113;     
	private static final int beginToken		   = 150;   
	private static final int mainToken		   = 200;   
	private static final int eofToken		   = 255;   
	/* End token values. */ 

	/* Reserved words. */
	private static final String[] reserved = new String[]{
		"*", "/", "+", "-", "==", "!=", "<", ">=", "<=", ">", ".",
		",", "[", "]", ")", "<-", "then", "do", "(", ";", "{", "}", "od",
		"fi", "else", "let", "call", "if", "while", "return", "var",
		"array", "function", "procedure", "main","InputNum", "OutputNum",
		"OutputNewLine"
	};

	private static final String[] builtInFunctions = new String[]{"InputNum", "OutputNum",
		"OutputNewLine"};

	private static final int[] reservedID = new int[]{
		timesToken, divToken, plusToken, minusToken, eqlToken, neqToken, 
		lssToken, geqToken, leqToken, 
		gtrToken, periodToken, commaToken, openbracketToken, closebracketToken,
		closeparenToken,
		becomesToken, thenToken, doToken, openparenToken, semiToken, beginToken,
		endToken, odToken,
		fiToken, elseToken, letToken, callToken, ifToken, whileToken, 
		returnToken, varToken,
		arrToken, funcToken, procToken, mainToken, ident, ident, ident
	};
    /* End reserved words. */
    
    public static StringTable sharedTable = new StringTable();


}