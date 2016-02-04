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

	boolean debug;		         // Debugging flag.

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

	/* Operation codes for intermediate representation. */
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

	/* Main function for accepting PL241 files.
	 * Usage: java Parser <filename> [-d]
	 * The -d flag creates debugging output.
	 */
	public static void main(String[] args) {
		// Capture command line arguments and parse the given file.
		String filename = "";
		boolean debug = false;
    	try {
     		filename = args[0];
     		if (args.length > 1) {  // Check for debugging flag.
     			if (args[1].equals("-d")) {
     				debug = true;
     			}
     		} 
     	} catch (Exception e) {
      		System.out.println("Usage: java Parser <filename> [-d]");
    	}
    	Parser parser = new Parser(filename, debug);

    	// Print out the VCG representation of the parsed file.
    	if (debug) {
    		parser.printVCG();
    	}
    	
    }
	/* End main function. */

	/* Constructor. */
	Parser(String filename, boolean debug) {
		this.debug = debug;
		scanner = new Tokenizer(filename);
		program = new IntermedRepr();
		computation(); // Begin recursive descent parsing.
		// Next, create the interference graph...
	}

	/* Recursive descent methods. */

	/* Computation, i.e., program.
	 * computation = "main" { varDecl } { funcDecl } "{" statSequence "}" "." .
	 * Parses a PL241 program and converts it into SSA form.
	 */
	private void computation() {
		if (debug) { System.out.print("(Computation "); };

		expect(mainToken);

		// Variable declarations.
		while (check(arrToken) || check(varToken)) {
			varDecl();
		}
		// Function declarations.
		while (check(procToken) || check(funcToken)) {
			funcDecl();
		}
		
		expect(beginToken);
		// Signal beginning of program.
		program.addBlock("Program begins.");
		statSequence(); // The program.

		expect(endToken);    
		expect(periodToken);
		expect(eofToken);
		// Signal end of program.
		// program.add(end); 

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
	 * Parameters in function/procedure definitions.
	 */
	private void formalParam() {
		if (debug) { System.out.print("(FormalParam "); };
		expect(openparenToken);

		// Find 0 or more identifiers separated by commas.
		if (!check(closeparenToken)) {
			ident();
			while (accept(commaToken)) { 
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
	 	// Add function/procedure block pointer to string table.
	}

	/* varDecl - variable declaration.
	 * varDecl = typeDecl ident { "," ident } ";".
	 * Not quite sure how to handle this yet.
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
	 * Mostly pass on work to other functions.
	 */
	private Block statSequence() {
		if (debug) { System.out.print("(StatSequence "); };
		statement();		// First statement.
		while (accept(semiToken)) {
			statement();    // More statements.
		}
		if (debug) { System.out.print(")"); };
		return program.currentBlock; // Return the last block seen.
	}

	/* statement.
	 * statement = assignment | funcCall | ifStatement | whileStatement | returnStatement.
	 * For now, we pass on the work to other functions.
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
	 * Function call related. Handle this later.
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
	 * Handle all things while-loop: create new basic blocks, create compare/branch
	 * instructions.
	 */
	private void whileStatement() {
		Block previous, join, whileBlock, endWhileBlock, afterWhile;
		if (debug) { System.out.print("(WhileStatement "); };
		expect(whileToken);
		previous = program.currentBlock;
		// Create basic block with compare and branch -- join block.
		join = program.addBlock("While join block.");
		relation();         // Test.
		expect(doToken);
		// New basic block (fall-through block).
		whileBlock = program.addBlock("While inner block.");
		endWhileBlock = statSequence();     // Instructions in while loop.
		expect(odToken);
		// New basic block (after while) -- branch.
		afterWhile = program.addBlock("After while.");

		// Connect blocks as appropriate.
		previous.addNext(join, false);  // Fall-through.
		join.addNext(whileBlock, afterWhile);
		whileBlock.addNext(join, true); // Jump

		if (debug) { System.out.print(")"); };
	}

	/* ifStatement.
	 * ifStatement = "if" relation "then" statSequence ["else" statSequence] "od".
	 * Handle all things if-statement: create new basic blocks, create compare/branch
	 * instructions.
	 */
	private void ifStatement() {
		Block previous, compare, trueBlock, falseBlock, 
			  endTrueBlock, endFalseBlock, join;
		boolean falseBranch = false;
		falseBlock          = null; // So the compiler doesn't complain.
		endFalseBlock       = null; // 
		if (debug) { System.out.print("(IfStatement "); };
		expect(ifToken);
		previous = program.currentBlock;
		// New basic block with compare and branch.
		compare = program.addBlock("If compare.");
		relation();        // Test.
		expect(thenToken);
		// New basic block -- fallthrough (true).
		trueBlock    = program.addBlock("If true block.");
		endTrueBlock = statSequence();       // Instructions in true block.
		if (accept(elseToken)) {
			// New basic block -- branch (false).
			falseBlock    = program.addBlock("If false block.");
			endFalseBlock = statSequence();  // Instructions in false block.
			falseBranch = true;
		}
		expect(fiToken);
		// New basic block -- join.
		join = program.addBlock("If join block.");

		// Connect blocks as appropriate.
		previous.addNext(compare, false);        // Fallthrough
		compare.addNext(trueBlock, falseBlock);  // Ok if false block is null.
		endTrueBlock.addNext(join, true);        // Jump

		// Block connections for false branch.
		if (falseBranch) { 
			endFalseBlock.addNext(join, false); // Fall through
		}

		if (debug) { System.out.print(")"); };
	}

	/* funcCall. 
	 * funcCall = "call" ident [ "(" [expression { "," expression } ] ")"].
	 * We will deal with function calls later...
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
	 * assignment = "let" "<-" expression.
	 */
	private void assignment() {
		if (debug) { System.out.print("(Assignment "); };
		// Emit move instruction.
		expect(letToken);
		// Return an identifier id.
		designator();			// Name of variable.
		expect(becomesToken);
		// Return an instruction.
		expression();			// Value of variable.
		if (debug) { System.out.print(")"); };
		// Create new Variable.
		// Create new move instruction for this Variable.
		// Make sure as many pointers/parameters as possible are set.
	}

	/* relation.
	 * relation = expression relOp expression.
	 * Creates compare instruction and branch instruction.
	 * Branch instruction must be fixed by caller.
	 */
	private void relation() {
		//Instruction compare, left, right;
		if (debug) { System.out.print("(Relation "); };
		/*left   = */ expression(); 		// 1st expression. 
		int branchCode = opposite(relOp());	// Comparison operator.
		/* right  = */ expression();		// 2nd expression.
		if (debug) { System.out.print(")"); };
		// Emit cmp left right
		// Emit conditional jump based on operator.
	}

	// Helper for relation. Return op code that represents
	// the opposite of a relation operator.
	private int opposite(int relOp) {
		// TODO
		return 0;
	}

	/* expression.
	 * expression = term { ("+" | "-") term }.
	 * Create instructions to evaluate the expression.
	 */
	private void expression() {
		// Instruction acc, next;
		if (debug) { System.out.print("(Expression "); };
		/* acc = */ term();        
		while (check(plusToken) || check(minusToken) ) {
			if (accept(plusToken)) {
				/* next = */ term();   
				// Emit add acc next
			} else if (accept(minusToken)) {
				/* next = */ term();   
				// Emit sub acc next
			}
		}
		if (debug) { System.out.print(")"); };
		// return acc;
	}

	/* term.
	 * term = factor { ("*" | "/") factor }.
	 * Create instructions to evaluate the term.
	 */
	private void term() {
		// Instruction acc, next;
		if (debug) { System.out.print("(Term "); };
		/* acc = */ factor();      // Factor w/o * or /.
		while (check(timesToken) || check(divToken)) {
			if (accept(timesToken)) {
				/* next = */ factor();
				// Emit mul acc next
			} else if (accept(divToken)) {
				/* next = */ factor();
				// Emit div acc next
			}
		}
		if (debug) { System.out.print(")"); };
		// return acc;
	}

	/* factor.
	 * factor = designator | number | "(" expression ")" | funcCall.
	 * Create instructions to evaluate the factor.
	 * We may need to return a Value here.
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
	}

	/* designator.
	 * designator = ident { "[" expression "]" }.
	 * Return the integer id corresponding to the identifier found.
	 * Also need to handle arrays. (Not sure how right now).
	 */
	private void designator() {
		int id;
		if (debug) { System.out.print("(Designator "); };
		id = ident();          			 // Identifier name. Stop here if a variable.
		while (accept(openbracketToken)) {
			expression(); 	             // (If array) array indices.
			expect(closebracketToken);
		}
		if (debug) { System.out.print(")"); };
		// For now, pretend we don't have arrays.
		// return id
	}

	/* number.
	 * number = digit { digit }
	 * Returns the integer value of the number.
	 */
	private int number() {
		int result;
		if (debug) { System.out.print("(Number " + scanner.currentToken()); };
		result = expect(number);  // The number.
		if (debug) { System.out.print(")"); };
		return result;
		// Should this be wrapped in a constant?
	}

	/* ident.
	 * ident = letter { letter | digit }
	 * Returns the integer id of the identifier.
	 */
	private int ident() {
		int result;
		if (debug) { System.out.print("(Ident " + scanner.currentToken()); };
		result = expect(ident);  // The identifier.
		if (debug) { System.out.print(")"); };
		return result;
	}

	/* relOp - relation operator.
	 * Returns the token value of the relation operator found.
	 */
	private int relOp() {
		int result = errorToken;
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

	/* Helper functions for checking validity of tokens. */

	// Checks that the current token is the expected one 
	// and advances the stream. Throws error if not.
	private int expect(int token) {
		int result = errorToken;
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
			error("Expect failed. Saw " + scanner.currentToken() + ", expected "
			 	  + scanner.tokenToString(token) + ".");
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

	/* End token validity checks. */

	// Print an error message and exit the program.
	private void error(String message) {
		scanner.error(message);
		System.exit(0);
	}

	// Print out the CFG in vcg format.
	// Should usually only be called after parsing is complete.
	public void printVCG() {
		System.out.println(program);
	}
	

}