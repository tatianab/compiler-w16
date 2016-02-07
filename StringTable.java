/* File: StringTable.java
 * Author: Tatiana Bradley
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.Hashtable;
import java.util.ArrayList;

public class StringTable {
	// Class representing two-way lookup symbol table for
	// identifiers and ID's.

	private Hashtable<String, Integer> strings;
	private ArrayList<StringData>      ids; 
	private int nextOpenID;

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

	private class StringData {
		public int id;
		public String name;
		public int token;
		public int current;   // Increments every time a variable is reassigned.
		public Variable last;
		// Do we need type here too?

		public StringData(int id, String name, int token) {
			this.id = id;
			this.name = name;
			this.token = token;
			this.current = 0;
			this.last    = new Variable(this.id, this.name, this.current);
		}

		public Variable reassign() {
			Variable newVar = new Variable(id, name, current);
			current++;
			last = newVar;
			return newVar;
		}


	}

	/* Reserved words. */
	private static final String[] reserved = new String[]{
		"*", "/", "+", "-", "==", "!=", "<", ">=", "<=", ">", ".",
		",", "[", "]", ")", "<-", "then", "do", "(", ";", "{", "}", "od",
		"fi", "else", "let", "call", "if", "while", "return", "var",
		"array", "function", "procedure", "main"
	};

	private static final int[] reservedID = new int[]{
		timesToken, divToken, plusToken, minusToken, eqlToken, neqToken, lssToken, geqToken, leqToken, 
		gtrToken, periodToken, commaToken, openbracketToken, closebracketToken, closeparenToken,
		becomesToken, thenToken, doToken, openparenToken, semiToken, beginToken, endToken, odToken,
		fiToken, elseToken, letToken, callToken, ifToken, whileToken, returnToken, varToken,
		arrToken, funcToken, procToken, mainToken
	};

	public StringTable() {
		nextOpenID = 0;
		strings = new Hashtable(reserved.length);
		ids 	= new ArrayList(reserved.length);

		String name;
		int id, token;

		// Add reserved words.
		for (int i = 0; i < reserved.length; i++) {
			name = reserved[i];
			id    = nextOpenID;
			token = reservedID[i];
			strings.put(name, id);
			ids.add(id, new StringData(id, name, token));
			nextOpenID++;
		}
	}

	public int getSym(String name) {
		int id = strings.get(name);
		return ids.get(id).token;
	}

	public int getID(String name) {
		return strings.get(name);
	}

	public String getName(int id) {
		return ids.get(id).name;
	}

	public boolean member(String name) {
		return strings.containsKey(name);
	}

	public void add(String name) {
		int id = nextOpenID;
		nextOpenID++;
		strings.put(name, id);
		ids.add(id, new StringData(id, name, ident));
	}

	public Variable get(int id) {
		return ids.get(id).last;
	}

	public Variable reassign(int id) {
		return ids.get(id).reassign();
	}

	// // Only used for error messages.
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


}