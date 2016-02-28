/* File: Parser.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
package compiler-w16;
import java.io.IOException;

public class Parser {
	/* Parser for PL241 language.
	   The parser's job is to convert a PL241 into an 
	   intermediate representation of the progam in SSA form.
	 */

	// Data members.
	public Tokenizer scanner;    // Reads file and breaks it into tokens.
	public IntermedRepr program; // Intermediate representation of the program in SSA form, after parsing.

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
	public static int neg     = Instruction.neg;
	public static int add     = Instruction.add;
	public static int sub     = Instruction.sub;
	public static int mul     = Instruction.mul;
	public static int div     = Instruction.div;
	public static int cmp     = Instruction.cmp;

	public static int adda    = Instruction.adda;
	public static int load    = Instruction.load;
	public static int store   = Instruction.store;
	public static int move    = Instruction.move;
	public static int phi     = Instruction.phi;

	public static int end     = Instruction.end;

	public static int read    = Instruction.read;
	public static int write   = Instruction.write;
	public static int writeNL = Instruction.writeNL;

	public static int bra     = Instruction.bra;
	public static int bne     = Instruction.bne;
	public static int beq     = Instruction.beq;
	public static int bge     = Instruction.bge;
	public static int blt     = Instruction.blt;
	public static int bgt     = Instruction.bgt;
	public static int ble     = Instruction.ble;
	/* End operation codes. */

	/* Constructor. */
	Parser(String filename, boolean debug) {
		this.debug = debug;
		scanner = new Tokenizer(filename);
		program = new IntermedRepr(debug);
	}

	/* Return the program in SSA form. */
	public IntermedRepr parse() {
		if (debug) { System.out.println("Parsing program..."); }
		computation();   // Parse the program recursively.
		return program;
	}

	/* Recursive descent methods. */

	/* Computation, i.e., program.
	 * computation = "main" { varDecl } { funcDecl } "{" statSequence "}" "." .
	 * Parses a PL241 program and converts it into SSA form.
	 */
	private void computation() {

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
		program.begin();
		statSequence(); // The program.

		expect(endToken);    
		expect(periodToken);
		expect(eofToken);
		// Signal end of program.
		program.end();

	}

	/* funcBody - function body.
	 * funcBody = { varDecl } "{" [statSequence] "}".
	 */
	private void funcBody() {

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

	}

	/* formalParam - formal parameter.
	 * formalParam = "(" [ident { "," ident }] ")".
	 * Parameters in function/procedure definitions.
	 */
	private void formalParam() {
		expect(openparenToken);

		// Find 0 or more identifiers separated by commas.
		if (!check(closeparenToken)) {
			ident();
			while (accept(commaToken)) { 
				ident();
			}
		}

		expect(closeparenToken);

	}

	/* funcDecl - function declaration.
	 * funcDecl = ("function" | "procedure") ident [formalParam] ";" funcBody ";".
	 */
	private void funcDecl() {


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

	 	// Add function/procedure block pointer to string table.
	}

	/* varDecl - variable declaration.
	 * varDecl = typeDecl ident { "," ident } ";".
	 * Not quite sure how to handle this yet.
	 */
	private void varDecl() {

		/* Type type = */ typeDecl();   // Type of variable.
		/* int id = */ ident();
		// Add variable to symbol table.
		while (accept(commaToken)) {
			ident();  // Variable name(s).
			// Add variable to symbol table.
		}
		expect(semiToken);

	}

	/* typeDecl - type declaration.
	 * typeDecl = "var" | "array" "[" number "]" { "[" number "]" }.
	 */
	private void typeDecl() {

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

		// Return structure representing type and dimensions.
	}

	/* statSequence - sequence of statements.
	 * statSequence = statement { ";" statement }.
	 * Mostly pass on work to other functions.
	 */
	private Block statSequence() {

		statement();		// First statement.
		while (accept(semiToken)) {
			statement();    // More statements.
		}

		return program.currentBlock(); // Return the last block seen.
	}

	/* statement.
	 * statement = assignment | funcCall | ifStatement | whileStatement | returnStatement.
	 * For now, we pass on the work to other functions.
	 */
	private void statement() {

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

	}

	/* returnStatement.
	 * returnStatement= "return" [expression].
	 * Function call related. Handle this later.
	 */
	private void returnStatement() {

		expect(returnToken);
		if (!check(semiToken)) {
			expression();       // Expression to return.
		}

	}

	/* whileStatement.
	 * whileStatement = "while" relation "do" statSequence "od".
	 * Handle all things while-loop: create new basic blocks, create compare/branch
	 * instructions.
	 */
	private void whileStatement() {
		Block previous, join, whileBlock, endWhileBlock, follow;

		expect(whileToken);
		previous = program.currentBlock(); // Grab previous block.
		program.endBlock();				   // End previous block.

		// Create basic block with compare and branch -- join block.
		join = program.addBlock("While join/compare block.");
		relation();                        // Grab the while condition.
		program.endBlock();                // End compare block.
		expect(doToken);

		// New basic block -- fall-through block (true).
		whileBlock = program.addBlock("While inner block.");
		endWhileBlock = statSequence();    // Fill in instructions in while loop.
		program.addInstr(bra, join);       // Branch to join.
		program.endBlock();				   // End inner block.
		expect(odToken);

		// New basic block (follow block) -- branch.
		follow = program.addBlock("Follow (while).");

		// Connect blocks as appropriate.
		previous.addNext(join, false);     // Fall-through to join/compare from previous.
		join.addNext(whileBlock, follow);  // Join/compare falls through to inner block, or jumps to follow.
		join.fix(follow);                  // Branch to follow.
		endWhileBlock.addNext(join, true); // Jump from inner block to join/compare.

		// Add dominance.
		previous.dominates(join);
		join.dominates(whileBlock);
		join.dominates(follow);


	}

	/* ifStatement.
	 * ifStatement = "if" relation "then" statSequence ["else" statSequence] "od".
	 * Handle all things if-statement: create new basic blocks, create compare/branch
	 * instructions.
	 */
	private void ifStatement() {
		Block previous, compare, trueBlock, falseBlock, 
			  endTrueBlock, endFalseBlock, join;
		

		expect(ifToken);
		previous = program.currentBlock();   // Grab the previous block.
		program.endBlock();					 // End the previous block.

		// New basic block with compare and branch.
		compare = program.addBlock("If compare.");
		relation();                          // Create cmp and branch instructions. (Must be fixed).
		program.endBlock();                  // End the compare block.
		previous.addNext(compare, false);    // Fall through to compare from previous.
		expect(thenToken);

		// New basic block -- fall through (true).
		trueBlock    = program.addBlock("If true block.");
		endTrueBlock = statSequence();       // Instructions in true block.
		program.addInstr(bra);				 // Unconditional branch. (Must be fixed.)
		program.endBlock();                  // End true block.

		if (accept(elseToken)) {
			// New basic block -- branch (false).
			falseBlock    = program.addBlock("If false block.");
			endFalseBlock = statSequence();  // Instructions in false block.
			program.endBlock();              // End false block.
		} else {
			falseBlock          = null;      // Set false blocks to null
			endFalseBlock       = null;      // so the compiler doesn't complain.
		}
		expect(fiToken);

		// New basic block -- join.
		join = program.addBlock("If join block.");
		
		// Block connections for false branch, or no false branch.
		if (falseBlock != null) {               // If there is a false branch:
			// Fix CFG.
			compare.fix(falseBlock);            // Fix jump instruction.
			compare.addNext(trueBlock, falseBlock);   
			endFalseBlock.addNext(join, false); // Fall through to join from false.
			endTrueBlock.addNext(join, true);   // Jump to join from true.
			endTrueBlock.fix(join);             // Fix branch from true.

			// Add dominance.
			compare.dominates(falseBlock);
		} else {								// If there's just a true branch:
			// Fix CFG.
		    compare.fix(join);                  // Fix jump instruction.
			compare.addNext(trueBlock, join);  
			endTrueBlock.addNext(join, false);  // Fall through to join from true.
	        endTrueBlock.fix();                 // Delete branch instruction from true.
		}

		// Add dominance.
		previous.dominates(compare);
		compare.dominates(trueBlock);
		compare.dominates(join);


	}

	/* funcCall. 
	 * funcCall = "call" ident [ "(" [expression { "," expression } ] ")"].
	 * We will deal with function calls later...
	 */
	private Instruction funcCall() {

		Value expr = null;
		expect(callToken);

		// Capture function call details.
		/* Function func = scanner.getFunction( */ int id = ident(); // );   // Function name.
		// if (func == null) {
		// 	error("Function not defined.");
		// }

		// Check for correct # of parameters and store them.
		if (accept(openparenToken)) {
			if (!check(closeparenToken)) {
				expr = expression();         // Parameters.
				while (accept(commaToken)) {
					expression();     // More parameters.
				}
			}
			expect(closeparenToken);
		}

		if (scanner.isBuiltIn(id)) {
			// Handle built-in function.
			return generateBuiltInFunc(id, expr);
		} else {
			// Handle user-defined function.
			// program.addBlock("Enter function.");
			// Set up stack.
			// Unconditional branch to function.
			// program.endBlock();
			// program.addBlock("Exit function.");
			// Clean up stack?
			// Link to end of function.
			// program.endBlock();
			// program.addBlock("After function.")
		}

		return null;
	}

	// Generate code for the given build in function.
	private Instruction generateBuiltInFunc(int id, Value param) {
		String funcName = scanner.idToString(id);

		if (funcName.equals("InputNum")) {
			return program.addInstr(read);
		} else if (funcName.equals("OutputNum")) {
			return program.addInstr(write, param);
		} else if (funcName.equals("OutputNewLine")) {
			return program.addInstr(writeNL);
		} else {
			error("Built in function " + funcName + " not found.");
		}

		return null;
	}

	/* assignment.
	 * assignment = "let" "<-" expression.
	 */
	private void assignment() {

		expect(letToken);

		Variable var = designator(true); // Name of variable.
		expect(becomesToken);
		Value expr = expression();	     // Value of variable.


		// Create new move instruction for this Variable.
		// var = scanner.reassign(var);    // Set variable name and instance.
		scanner.updateVar(var);
		program.addAssignment(var, expr);
		// moveInstr.setDefn(var,expr);    // move var expr
	}

	/* relation.
	 * relation = expression relOp expression.
	 * Creates compare instruction and branch instruction.
	 * Branch instruction must be fixed by caller.
	 */
	private void relation() {
		Value compare, left, right;

		left = expression(); 		           // 1st expression. 
		int branchCode = opposite(relOp());	   // Comparison operator.
		right = expression();		           // 2nd expression.

		compare = program.addInstr(cmp, left, right);
		program.addInstr(branchCode, compare); // Needs to be fixed by caller.
	}

	// Helper for relation. Return branch op code that represents
	// the opposite of a relation operator.
	private int opposite(int relOp) {
		return relOp; // For now, op codes are cleverly set so this works.
	}

	/* expression.
	 * expression = term { ("+" | "-") term }.
	 * Create instructions to evaluate the expression.
	 */
	private Value expression() {
		Value expr, next;

		expr = term();        
		while (check(plusToken) || check(minusToken) ) {
			if (accept(plusToken)) {
				next = term();   
				expr = program.addInstr(add, expr, next);
			} else if (accept(minusToken)) {
				next = term();   
				expr = program.addInstr(sub, expr, next);
			}
		}

		return expr;
	}

	/* term.
	 * term = factor { ("*" | "/") factor }.
	 * Create instructions to evaluate the term.
	 */
	private Value term() {
		Value term, next;

		term = factor();      
		while (check(timesToken) || check(divToken)) {
			if (accept(timesToken)) {
				next = factor();
				term = program.addInstr(mul, term, next);
			} else if (accept(divToken)) {
				next = factor();
				term = program.addInstr(div, term, next);
			}
		}
		return term;
	}

	/* factor.
	 * factor = designator | number | "(" expression ")" | funcCall.
	 * Create instructions to evaluate the factor.
	 * We may need to return a Value here.
	 */
	private Value factor() {
		Value factor = null;

		if (check(ident)) {
			factor = designator(false);   // Identifier.
		} else if (check(number)) {
			factor = number();       // Number
		} else if (accept(openparenToken)) {
			factor = expression();   // Parenthetical expression.
			expect(closeparenToken);
		} else if (check(callToken)) {
			factor = funcCall();     // Function call.
		} else {
			error("Invalid factor.");
		}

		return factor;
	}

	/* designator.
	 * designator = ident { "[" expression "]" }.
	 * Return the integer id corresponding to the identifier found.
	 * Also need to handle arrays. (Not sure how right now).
	 * newVar flag should be false if called from a non-assignment context.
	 */
	private Variable designator(boolean newVar) {
		int id;

		id = ident();          			 // Identifier name. Stop here if a variable.
		while (accept(openbracketToken)) {
			expression(); 	             // (If array) array indices.
			expect(closebracketToken);
		}

		// For now, pretend we don't have arrays.
		if (newVar) {
			return scanner.newVar(id);
		} else if (scanner.getVar(id) == null) {
			error("Uninitialized variable."); 
		} else {
			return scanner.getVar(id);
		}
		return null;
	}

	/* number.
	 * number = digit { digit }
	 * Returns the integer value of the number.
	 */
	private Constant number() {
		int value = 0;
		value = expect(number);  // The number.
		return new Constant(value);
	}

	/* ident.
	 * ident = letter { letter | digit }
	 * Returns the integer id of the identifier.
	 */
	private int ident() {
		int result;
		result = expect(ident);  // The identifier.
		return result;
	}

	/* relOp - relation operator.
	 * Returns the token value of the relation operator found.
	 */
	private int relOp() {
		int relOp = errorToken;

		if (scanner.sym >= eqlToken && scanner.sym <= gtrToken) {
			relOp = scanner.sym;
			scanner.next();
		} else {
	    	error("Invalid relation operator.");
	    }

	    return relOp;
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
	// Should only be called after parsing is complete.
	// public void printCFG() {
	// 	System.out.println(program.cfg());
	// }
	

}