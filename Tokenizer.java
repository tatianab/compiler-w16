/* File: Tokenizer.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

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
		table  = new StringTable();
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
			if (c == '*') {        // *
				sym = timesToken;
			} else if (c == '/') { // /
				sym = divToken;
			} else if (c == '+') { // +
				sym = plusToken;
			} else if (c == '-') { // -
				sym = minusToken;	
			} else if (c == '=') { // ==
				reader.next();
				c = reader.sym;
				if (c == '=') { 
					token += toString(c);
					sym = eqlToken;	
				} else {        
					error("Expecting ==");
				}
			} else if (c == '!') { // !
				reader.next();
				c = reader.sym;
				if (c == '=') { 
					token += toString(c);
					sym = neqToken;
				} else {        
					error("Expecting !=");
				}
			} else if (c == '<') { // <, <= or <-
				reader.next();
				c = reader.sym;
				if (c == '=') { 
					token += toString(c);
					sym = leqToken;
				} else if (c == '-') {        
					token += toString(c);
					sym = becomesToken;
				} else {
					sym = lssToken;
				}
			} else if (c == '>') { // > or >=
				reader.next();
				c = reader.sym;
				if (c == '=') { 
					token += toString(c);
					sym = geqToken;
				} else {
					sym = gtrToken;
				}
			} else if (c == '.') { // .
				sym = periodToken;
			} else if (c == ',') { // ,
				sym = commaToken;
			} else if (c == '[') { // [
				sym = openbracketToken;
			} else if (c == ']') { // ]
				sym = closebracketToken;
			} else if (c == '(') { // (
				sym = openparenToken;
			} else if (c == ')') { // )
				sym = closeparenToken;
			} else if (c == ';') { // ;
				sym = semiToken;
			} else if (c == '}') { // }
				sym = endToken;
			}  else if (c == '{') { // {
				sym = beginToken;
			} else {
			 error("Invalid token."); 
			}

			// Advance reader and store token ID.
			reader.next();
			id  = stringToId(token);
		}
		else if (isAlpha(c)) {    // If token is an identifier.
			token = toString(c);
			reader.next();
		    c = reader.sym;
		    // Capture token.
			while (isAlphaNumeric(c)) {
				  token += c;
		          reader.next();
		          c = reader.sym;
			}
			// Check if token is a reserved word.
			if (token.equals("then")) { // then
				sym = thenToken;
			} else if (token.equals("do")) { // do
				sym = doToken;
			} else if (token.equals("od")) { // od
				sym = odToken;
			} else if (token.equals("fi")) { // fi
 				sym = fiToken;
			} else if (token.equals("else")) { // else
				sym = elseToken;
			} else if (token.equals("let")) { // let
				sym = letToken;
			} else if (token.equals("call")) { // call
				sym = callToken;
			} else if (token.equals("if")) { // if
				sym = ifToken;
			} else if (token.equals("while")) { // while
				sym = whileToken;
			} else if (token.equals("return")) { // return
				sym = returnToken;
			} else if (token.equals("var")) { // var
				sym = varToken;
			} else if (token.equals("array")) { // array
				sym = arrToken;
			} else if (token.equals("function")) { // function
				sym = funcToken;
			} else if (token.equals("procedure")) { // procedure
				sym = procToken;
			} else if (token.equals("main")) { // main
				sym = mainToken;
			} else {                  // Normal identifier.
				sym = ident;
			}
			// Store token ID.
			id  = table.addOrLookup(token);
		} else if (isDigit(c)) { // If token is a number.
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
			error("Error: invalid token beginning with : " + c + ".");
		}
		return sym;
	}

	// Signal an error with current file position.
	public void error(String errorMsg) {
		String token = idToString(id);
		System.out.println("\nERROR: " + errorMsg + "\nlast token: " + token + ", last val = " + val);
		System.out.println("Current symbol: " + sym);
		System.exit(0);
	}

	// Identifier table methods.
	public String idToString(int id) {
		return table.idToString(id);
	}

	public int stringToId(String name) {
		return table.stringToId(name);
	}

	// Helper functions to check the type of characters
	// and do type conversions.
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

	private void skipSpaces() {
		char c = reader.sym;
   		while (Character.isSpaceChar(c) || c == '\n' || c == '\r' || c == '\t') {
   			reader.next();
   			c = reader.sym;
   		}
	}

	// Returns the string representation of the current token.
	public String currentToken() {
		if (sym != number) {
			return idToString(id);
		} else { return Integer.toString(val); }
	}

}