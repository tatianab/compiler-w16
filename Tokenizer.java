/* File: Tokenizer.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
//
import java.io.IOException;

public class Tokenizer {
	// Encapsulates streams of tokens.

	public int sym; 		   // The current token. 0 = error, 255 = EOF.
	public int val; 		   // The value of the last number encountered.
	public int id;             // The id of the last identifier encountered.
	private StringTable table; // Table of identifiers.
	private Reader reader;     // File reader.

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

	// Constructor. Opens filename and scans the first token into sym.
	public Tokenizer(String filename) {
		reader = new Reader(filename);
        table  = StringTable.sharedTable;
		next(); // Read first token.
	}

	// Advance to the next token and return its sym value.
	public int next() {
		skipSpaces();
		char c = reader.sym;
		String token;

		if (c == 0xff) { // If EOF is found.
			sym = eofToken;
			return sym; 
		} else if (isPunctuation(c)) { // If token is a punctuation mark.
			token = toString(c);
			if (c == '<' || c == '>') { 	   // < and > are ambiguous.
				reader.next();	   
				c = reader.sym;
				if (c == '=' || c == '-') {    // Check for <=, <- and >=.
					token += toString(c);
				    reader.next();
				}
			} else if (c == '=' || c == '!') { // Check for != and ==.
				reader.next();
				c = reader.sym;
				token += toString(c);
				reader.next();
			} else { reader.next(); } 		   // All other tokens.

			if (!table.member(token)) {
				error("Invalid punctuation token."); 
			}

			// Store token information.
			sym = table.getSym(token);
			id  = table.getID(token);
		}
		else if (isAlpha(c)) {         // If token is an identifier.
			token = toString(c);
			reader.next();
		    c = reader.sym;

		    // Capture token.
			while (isAlphaNumeric(c)) {
				  token += c;
		          reader.next();
		          c = reader.sym;
			}

			// Add token to string table and update params.
			if (!table.member(token)) { 
				table.add(token); 
			}
			sym = table.getSym(token);
			id  = table.getID(token);

		} else if (isDigit(c)) { 	  // If token is a number.
			// Capture token as integer.
			int value = toInt(c);
			reader.next();
			c = reader.sym;
			while (isDigit(c)) {
				value = value * 10 + toInt(c);
				reader.next();
				c = reader.sym;
			}

			// Store token information.
			sym = number;
			val = value;
		} else {
			error("Invalid token beginning with : " + c + ".");
		}
		return sym;
	}

	// Skip over any space characters. These will be ignored.
	private void skipSpaces() {
		char c = reader.sym;
   		while (Character.isSpaceChar(c) || c == '\n' || c == '\r' || c == '\t') {
   			reader.next();
   			c = reader.sym;
   		}
	}

	// Signal an error with current file position.
	public void error(String errorMsg) {
		System.out.println("\nERROR: " + errorMsg);
		System.out.println("Last ident: " + idToString(id) + 
						   ", last val = " + val + ", current symbol = " + sym + ".");
		System.exit(0);
	}

	/* String table methods.*/
	// public Variable reassign(Variable var) {
	// 	return table.reassign(var.id);
	// }

	public Variable getVar(int id) {
		return table.get(id);
	}

	public Variable newVar(int id) {
		Variable newVar = new Variable(id, table.getName(id));
		return newVar;
	}

	public void updateVar(Variable var) {
		table.update(var.id, var);
	}

	// Convert an id number to its String representation.
	public String idToString(int id) {
		return table.getName(id);
	}

	// Convert an identifier name to its integer id.
	public int stringToId(String name) {
		return table.getID(name);
	}

	// Returns the String representation of the current token.
	public String currentToken() {
		if (sym != number) {
			return idToString(id);
		} else { return Integer.toString(val); }
	}

	// Return the String representation of the given token.
	// Input token is a token code, not an id.
	public String tokenToString(int token) {
		return table.symToString(token);
	}

	// Check if the given identifier is a built-in function.
	public boolean isBuiltIn(int id) {
		String name = table.getName(id);
		return ( name.equals("InputNum") || name.equals("OutputNum")|| name.equals("OutputNewLine") );
	}
	/* End identifier table methods. */

	/* Helper functions to check the type of characters
	   and do type conversions. */
	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}

	private boolean isAlpha(char c) {
		return Character.isLetter(c);
	}

	private boolean isPunctuation(char c) {
		String punctuation = "*/+-=!<>.,[](){};";
		return punctuation.contains(toString(c));
	}

	private boolean isDigit(char c) {
		return Character.isDigit(c);
	}

	private int toInt(char c) {
		return Character.getNumericValue(c);
	}

	private String toString(char c) {
		return Character.toString(c);
	}
	/* End helper functions. */

}