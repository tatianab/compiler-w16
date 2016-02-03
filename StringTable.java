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

	private Hashtable<String, StringData> strings;
	private ArrayList<String>             ids; // Is this needed?
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
		int id;
		int token;
		int current; // Increments every time a variable is reassigned.
		// Do we need type here too?

		public StringData(int id, int token) {
			this.id = id;
			this.token = token;
			this.current = 0;
		}

		public int updateCurrent() {
			int old = current;
			current++;
			return old;
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

		// Add reserved words.
		for (int i = 0; i < reserved.length; i++) {
			strings.put(reserved[i], new StringData(nextOpenID, reservedID[i]));
			ids.add(nextOpenID, reserved[i]);
			nextOpenID++;
		}
	}

	public int getSym(String name) {
		return strings.get(name).token;
	}

	public int getID(String name) {
		return strings.get(name).id;
	}

	public String getName(int id) {
		return ids.get(id);
	}

	public boolean member(String name) {
		return strings.containsKey(name);
	}

	public void add(String name) {
		strings.put(name, new StringData(nextOpenID, ident));
		ids.add(nextOpenID, name);
		nextOpenID++;
	}

	// Use this when a variable is assigned or reassigned.
	// Updates the # of assignments of the variable and returns
	// the (pre-update) value.
	public int update(String name) {
		return strings.get(name).updateCurrent();

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


}