/* File: Parser.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.io.IOException;

public class Parser {
	// Parser for PL241 language.

	// Data members.
	public Tokenizer scanner;    // Reads file and breaks it into tokens.
	public IntermedRepr program; // Intermediate representation of the program, after parsing.

	boolean debug = false;        // Debugging flag.

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

	/* Operation codes. */
	public static int neg     = 1;
	public static int add     = 2;
	public static int sub     = 3;
	public static int mul     = 4;
	public static int div     = 5;
	public static int cmp     = 6;
   
	public static int adda    = 7;
	public static int load    = 8;
	public static int store   = 9;
	public static int move    = 10;
	public static int phi     = 11;
   
	public static int end     = 12;
	public static int bra     = 13;
	public static int bne     = 14;
	public static int beq     = 15;
	public static int ble     = 16;
	public static int blt     = 17;
	public static int bge     = 18;
	public static int bgt     = 19;
   
	public static int read    = 20;
	public static int write   = 21;
	public static int writeNL = 22;
	/* End operation codes. */

	/* Main function for testing. */
	public static void main(String[] args) {
		String filename = "";
    	try {
     		filename = args[0];
     	} catch (Exception e) {
      		System.out.println("Usage: java Parser <filename>");
    	}
    	Parser parser = new Parser(filename);
    	parser.computation();
    }
	/* End main function. */

	/* Constructor. */
	Parser(String filename) {
		scanner = new Tokenizer(filename);
	}

	/* Recursive descent methods. */

	/* Computation, i.e., program.
	 * computation = "main" { varDecl } { funcDecl } "{" statSequence "}" "." .
	 */
	private void computation() {
		if (debug) { System.out.print("(Computation "); };

		// Main keyword.
		expect(mainToken);

		// Variable declarations.
		while (check(arrToken) || check(varToken)) {
			varDecl();
		}
		// Function declarations.
		while (check(procToken) || check(funcToken)) {
			funcDecl();
		}
		// Main body.
		expect(beginToken);
		statSequence();

		// Ending keywords.
		expect(endToken);    
		expect(periodToken);
		expect(eofToken);
		// program.add(end); // Add end instruction to program.

		if (debug) { 
			System.out.print(")"); 
			System.out.print("\nProgram parsed successfully.");
		};
	}

	/* funcBody - function body.
	 * funcBody = { varDecl } "{" [statSequence] "}".
	 */
	private void funcBody() {
		if (debug) { System.out.print("(FuncBody "); };

		// Variable declarations.
		while (check(arrToken) || check(varToken)) {
			varDecl();
		}

		// Function body.
		expect(beginToken);
		if (!check(endToken)) {
			statSequence();
		} 
		expect(endToken);

		if (debug) { System.out.print(")"); };
	}

	/* formalParam - formal parameter.
	 * formalParam = "(" [ident { "," ident }] ")".
	 */
	private void formalParam() {
		if (debug) { System.out.print("(FormalParam "); };
		expect(openparenToken);

		// Find 0 or more identifiers separated by commas.
		if (!check(closeparenToken)) {
			ident();
			while (accept(commaToken)) { // ,
				ident();
			}
		}

		expect(closeparenToken);
		if (debug) { System.out.print(")"); };
	}

	/* funcDecl - function declaration.
	 * funcDecl = ("function" | "procedure") ident [formalParam] ";" funcBody ";".
	 */
	private void funcDecl() {
		if (debug) { System.out.print("(FuncDecl "); };

		if (accept(funcToken) || accept(procToken)) {
			ident();             // Procedure/function name.
			if (!check(semiToken)) {
				formalParam();   // Formal parameters.
			}
			expect(semiToken);
			funcBody();          // Body of function/procedure.
			expect(semiToken);
		} else { 
			error("Invalid function declaration.");
	 	}
	 	if (debug) { System.out.print(")"); };
	 	// Add function/procedure to symbol table.
	}

	/* varDecl - variable declaration.
	 * varDecl = typeDecl ident { "," ident } ";".
	 */
	private void varDecl() {
		if (debug) { System.out.print("(VarDecl "); };
		/* Type type = */ typeDecl();   // Type of variable.
		/* int id = */ ident();
		// Add variable to symbol table.
		while (accept(commaToken)) {
			ident();  // Variable name(s).
			// Add variable to symbol table.
		}
		expect(semiToken);
		if (debug) { System.out.print(")"); };
	}

	/* typeDecl - type declaration.
	 * typeDecl = "var" | "array" "[" number "]" { "[" number "]" }.
	 */
	private void typeDecl() {
		if (debug) { System.out.print("(TypeDecl "); };
		int type = -1;
		if (accept(varToken)) {        // If of variable type.
			// type = var
		} else if (accept(arrToken)) { // If of array type.
			// type = array
			expect(openbracketToken);
			number();                  // Array dimensions.
			expect(closebracketToken);
			while (accept(openbracketToken)) {
				number();			   // More array dimensions.
				expect(closebracketToken);
			}
		} else {
			error("Invalid type declaration.");
		}
		if (debug) { System.out.print(")"); };
		// Return structure representing type and dimensions.
	}

	/* statSequence - sequence of statements.
	 * statSequence = statement { ";" statement }.
	 */
	private void statSequence() {
		if (debug) { System.out.print("(StatSequence "); };
		// Start new basic block.
		statement();		// First statement.
		while (accept(semiToken)) {
			statement();    // More statements.
		}
		if (debug) { System.out.print(")"); };
	}

	/* statement.
	 * statement = assignment | funcCall | ifStatement | whileStatement | returnStatement.
	 */
	private void statement() {
		if (debug) { System.out.print("(Statement "); };
		if (check(letToken)) {
			assignment();			// If assigment statement.
		} else if (check(callToken)) {
			funcCall();				// If function call.
		} else if (check(ifToken)) {
			ifStatement();			// If if statement.
		} else if (check(whileToken)) {
			whileStatement();       // If while statement.
		} else if (check(returnToken)) {
			returnStatement();      // If return statement.
		} else {
			error("Invalid statement.");
		}
		if (debug) { System.out.print(")"); };
	}

	/* returnStatement.
	 * returnStatement= "return" [expression].
	 */
	private void returnStatement() {
		if (debug) { System.out.print("(ReturnStatement "); };
		expect(returnToken);
		if (!check(semiToken)) {
			expression();       // Expression to return.
		}
		if (debug) { System.out.print(")"); };
	}

	/* whileStatement.
	 * whileStatement = "while" relation "do" statSequence "od".
	 */
	private void whileStatement() {
		if (debug) { System.out.print("(WhileStatement "); };
		expect(whileToken);
		relation();         // Test.
		expect(doToken);
		// New basic block.
		statSequence();     // Instructions in while loop.
		expect(odToken);
		if (debug) { System.out.print(")"); };
	}

	/* ifStatement.
	 * ifStatement = "if" relation "then" statSequence ["else" statSequence] "od".
	 */
	private void ifStatement() {
		if (debug) { System.out.print("(IfStatement "); };
		expect(ifToken);
		relation();        // Test.
		expect(thenToken);
		// Branch if not true.
		// New basic block.
		statSequence();      // Instructions in true block.
		if (accept(elseToken)) {
			// New basic block.
			statSequence();  // Instructions in false block.
		}
		expect(fiToken);
		// Join block begins. (signal to fix)
		if (debug) { System.out.print(")"); };
	}

	/* funcCall. 
	 * funcCall = "call" ident [ "(" [expression { "," expression } ] ")"].
	 */
	private void funcCall() {
		if (debug) { System.out.print("(FuncCall "); };
		expect(callToken);
		ident();             	   // Function name.
		if (accept(openparenToken)) {
			if (!check(closeparenToken)) {
				expression();      // Parameters.
				while (accept(commaToken)) {
					expression();  // More parameters.
				}
			}
			expect(closeparenToken);
		}
		if (debug) { System.out.print(")"); };
	}

	/* assignment.
	 * assignment = "let" designator "<-" expression.
	 */
	private void assignment() {
		if (debug) { System.out.print("(Assignment "); };
		// Emit move instruction.
		expect(letToken);
		designator();			// Name of variable.
		expect(becomesToken);
		expression();			// Value of variable.
		if (debug) { System.out.print(")"); };
	}

	/* relation.
	 * relation = expression relOp expression.
	 */
	private void relation() {
		//Instruction compare, left, right;
		if (debug) { System.out.print("(Relation "); };
		// Emit branch/compare instruction?
		/*left   = */ expression(); 		// 1st expression. 
		/* branch = */ opposite(relOp());	// Comparison operator.
		/* right  = */ expression();		// 2nd expression.
		// compare = new Instruction(cmp, left, right);
		if (debug) { System.out.print(")"); };
		// return compare;
	}

	// Helper for relation. Find opposite of a relation operator.
	private int opposite(int relOp) {
		// TODO
		return 0;
	}

	/* expression.
	 * expression = term { ("+" | "-") term }.
	 */
	private void expression() {
		// Instruction acc, next;
		if (debug) { System.out.print("(Expression "); };
		/* acc = */ term();        // Term w/o + or -.
		while (check(plusToken) || check(minusToken) ) {
			if (accept(plusToken)) {
				/* next = */ term();   // Term(s) w/o + or -.
				// acc = new Instruction(add, acc, next, acc);
			} else if (accept(minusToken)) {
				/* next = */ term();   // Term(s) w/o + or -.
				// acc = new Instruction(sub, acc, next, acc);
			}
		}
		if (debug) { System.out.print(")"); };
		// return acc;
	}

	/* term.
	 * term = factor { ("*" | "/") factor }.
	 */
	private void term() {
		// Instruction acc, next;
		if (debug) { System.out.print("(Term "); };
		/* acc = */ factor();      // Factor w/o * or /.
		while (check(timesToken) || check(divToken)) {
			if (accept(timesToken)) {
				/* next = */ factor();
				// /* acc = */ new Instruction(mul, acc, next, acc);
			} else if (accept(divToken)) {
				/* next = */ factor();
				// /* acc = */ new Instruction(div, acc, next, acc);
			}
		}
		if (debug) { System.out.print(")"); };
		// return acc;
	}

	/* factor.
	 * factor = designator | number | "(" expression ")" | funcCall.
	 */
	private void factor() {
		if (debug) { System.out.print("(Factor "); };
		if (check(ident)) {
			designator();   // Identifier.
		} else if (check(number)) {
			number();       // Number
		} else if (accept(openparenToken)) {
			expression();   // Parenthetical expression.
			expect(closeparenToken);
		} else if (check(callToken)) {
			funcCall();     // Function call.
		} else {
			error("Invalid factor.");
		}
		if (debug) { System.out.print(")"); };
		// What to return here?
	}

	/* designator.
	 * designator = ident { "[" expression "]" }.
	 * This is for arrays.
	 */
	private void designator() {
		int id;
		if (debug) { System.out.print("(Designator "); };
		/* id = */ ident();          // Identifier name. Stop here if a variable.
		while (accept(openbracketToken)) {
			expression(); 	         // (If array) array indices.
			expect(closebracketToken);
		}
		if (debug) { System.out.print(")"); };
		// Add to symbol table here?
	}

	/* number.
	 * number = digit { digit }
	 * Returns the value of the number.
	 */
	private int number() {
		int result;
		if (debug) { System.out.print("(Number " + scanner.currentToken()); };
		result = expect(number);  // The number.
		if (debug) { System.out.print(")"); };
		return result;
	}

	/* ident.
	 * ident = letter { letter | digit }
	 * Returns the id of the identifier.
	 */
	private int ident() {
		int result;
		if (debug) { System.out.print("(Ident " + scanner.currentToken()); };
		result = expect(ident);  // The identifier.
		if (debug) { System.out.print(")"); };
		return result;
	}

	/* relOp - relation operator.
	 * Returns the relation operator in question.
	 */
	private int relOp() {
		int result = -1;
		if (debug) { System.out.print("(Relop "); };
		if (scanner.sym >= eqlToken && scanner.sym <= gtrToken) {
			result = scanner.sym;
			scanner.next();
		} else {
	    	error("Invalid relation operator.");
	    }
	    if (debug) { System.out.print(")"); };
	    return result;
	}

	// Helper functions for checking tokens.

	// Checks that the current token is the expected one 
	// and advances the stream. Throws error if not.
	private int expect(int token) {
		int result = -1;
		if (scanner.sym == token) {
			if (scanner.sym == number) {
				result = scanner.val;
				scanner.next();
			} else if (scanner.sym == ident) {
				result = scanner.id;
				scanner.next();
			} else {
				result = scanner.sym;
				scanner.next();
			}
		}
		else {
			error("Expect failed. Saw " + scanner.symString() + ", expected " + scanner.symString(token) + ".");
		}
		return result; 
	}

	// Checks that the current token is the expected one 
	// and advances the stream if so.
	private boolean accept(int token) {
		if (scanner.sym == token) {
			scanner.next();
			return true;
		}
		return false;
	}

    // Checks whether or not the current token is the expected one.
	private boolean check(int token) {
		return (scanner.sym == token);
	}

	private void error(String message) {
		scanner.error(message);
		System.exit(0);
	}
	

}