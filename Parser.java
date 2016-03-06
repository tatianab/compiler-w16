/* File: Parser.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
//
import java.io.IOException;
import java.util.ArrayList;

public class Parser {
	/* Parser for PL241 language.
	   The parser's job is to convert a PL241 into an 
	   intermediate representation of the progam in SSA form.
	 */

	// Data members.
	public StringTable  table;   // Table of identifiers.
	public Tokenizer    scanner; // Reads file and breaks it into tokens.
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

	public static int call    = Instruction.call;
	/* End operation codes. */

	/* Constructor. */
	Parser(String filename, boolean debug) {
		this.debug = debug;
		scanner = new Tokenizer(filename);
		program = new IntermedRepr(debug);
		table   = scanner.getTable();
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
	private void funcBody(Function function) {

		// Begin function and set up.
		program.beginFunction(function);
		Block body = program.addBlock("Body of function " + function.shortRepr());
		function.enter.addNext(body, false); // Connect blocks - fall through.

		// Variable declarations.
		while (check(arrToken) || check(varToken)) {
			varDecl();
		}
		
		// Function body.
		expect(beginToken);
		if (!check(endToken)) {
			body = statSequence();
		} 
		expect(endToken);

		// End function and clean up.
		program.endBlock();
		program.endFunction();
		body.addNext(function.exit, false); // Connect blocks - fall through.
	}

	/* formalParam - formal parameter.
	 * formalParam = "(" [ident { "," ident }] ")".
	 * Parameters in function/procedure definitions.
	 */
	private void formalParam(Function function) {
		expect(openparenToken);
		int id;
		ArrayList<String> formalParams = new ArrayList<String>();

		if (debug) { System.out.println("Capturing formal parameters of " + function.shortRepr()); }

		// Find 0 or more formal parameters separated by commas.
		if (!check(closeparenToken)) {
			id = ident();
			table.declareFormalParam(id);
			formalParams.add(table.getName(id));
			while (accept(commaToken)) { 
				id = ident();
				table.declareFormalParam(id);
				formalParams.add(table.getName(id));
			}
		}

		function.setFormalParams(formalParams.toArray(new String[formalParams.size()]));

		if (debug) { System.out.println("Formal parameters: " + function.shortRepr()); }

		expect(closeparenToken);
	}

	/* funcDecl - function declaration.
	 * funcDecl = ("function" | "procedure") ident [formalParam] ";" funcBody ";".
	 */
	private void funcDecl() {
		if (accept(funcToken) || accept(procToken)) {
			int funcId = ident(); // Procedure/function id.

			// Add function/procedure to string table.
	 		String funcName = table.getName(funcId);
	 		Function function = new Function(funcId, funcName);
	 		table.declare(function, funcId);

	 		// Get formal parameters.
			if (!check(semiToken)) {
				formalParam(function);   
			}

	 		// Get body of function or procedure.
			expect(semiToken);
			funcBody(function);         
			expect(semiToken);

		} else { 
			error("Invalid function declaration.");
	 	}
	}

	/* varDecl - variable declaration.
	 * varDecl = typeDecl ident { "," ident } ";".
	 */
	private void varDecl() {
		Value variable = typeDecl();   // Get the variable or array.
		int id = ident();
		table.declare(variable, id);
		if (debug && variable instanceof Variable) { 
			System.out.println("Declared variable " + table.getVar(id).shortRepr()); 
		}
		while (accept(commaToken)) {
		    id = ident();
			table.declare(variable, id);
			if (debug && variable instanceof Variable) { 
				System.out.println("Declared variable " + table.getVar(id).shortRepr()); 
			}
		}
		expect(semiToken);

		if (program.inMainFunction()) {
			variable.setGlobal();
		} else {
			variable.setLocal();
		}
	}

	/* typeDecl - type declaration.
	 * typeDecl = "var" | "array" "[" number "]" { "[" number "]" }.
	 */
	private Value typeDecl() {
		int dim;
		if (accept(varToken)) {        // If of variable type.
			return new Variable();
		} else if (accept(arrToken)) { // If of array type.
			Array array = new Array();
			expect(openbracketToken);
			dim = number().getVal();            // Array dimensions.
			array.addDim(dim);
			expect(closebracketToken);
			while (accept(openbracketToken)) {
				dim = number().getVal();        // More array dimensions.
			    array.addDim(dim);			   
				expect(closebracketToken);
			}
			array.commitDims();                 // Done with array dimensions.
			return array;
		} else {
			error("Invalid type declaration.");
			return null;
		}
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

		if (debug) { System.out.println("Parsing while block."); }

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

		if (debug) { System.out.println("Parsing if statement."); }

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
		int id = ident(); 					   // Function id.
		Function function = table.getFunc(id);
		if (function == null) {
			error("No function " + table.getName(id) + " exists.");
		}
		int numParams     = function.numParams;
		Value[] parameters  = new Value[numParams];
		if (debug) { System.out.println("Function " + function.shortRepr() + " with " + numParams + " params.");}

		// Check for correct # of parameters and store them.
		int i = 0;
		if (accept(openparenToken)) {
			if (!check(closeparenToken)) {
				if (i < numParams) {
					expr = expression();     // First parameter.
					parameters[i] = expr;
					i++;
				}
				while (accept(commaToken)) {
					if (i < numParams) {
						expr = expression();     // More parameters.
						parameters[i] = expr;
						i++;
					}
				}
			}
			expect(closeparenToken);
		}

		// Add function call to program and return the instruction.
        return program.addFunctionCall(function, parameters);
	}

	/* assignment.
	 * assignment = "let" "<-" expression.
	 */
	private void assignment() {
		expect(letToken);

		Value var = designator(true);  // Name of variable.
		expect(becomesToken);
		Value expr = expression();	   // Value of variable.

		if (var instanceof Variable) {
			// Create new move instruction for this Variable.
			Instruction moveInstr = program.addAssignment((Variable) var, expr);
			table.reassignVar(((Variable) var).id, (Variable) var);
			if (debug) { System.out.println("Generated move instruction " + moveInstr); }
		} else if (var instanceof Array) {
			// Create an array store instruction for this array.
			Instruction arrayInstr = program.addArrayInstr(Instruction.arrayStore, (Array) var, ((Array) var).currentIndices, expr);
			if (debug) { System.out.println("Generated array instruction " + arrayInstr); }
		}
	}

    /* designator.
	 * designator = ident { "[" expression "]" }.
	 * Return the Array or Variable corresponding to the identifier found.
	 * assignment flag should be false if called from a non-assignment context.
	 */
	private Value designator(boolean assignment) {
		Value[] indices = null;
		int id = ident();          		    // Identifier name. Stop here if a variable.

		if (table.getArr(id) != null) {
			int i  = 0;
			indices = new Value[table.getArr(id).numDims];
			while (accept(openbracketToken)) {
				indices[i] = expression();  // Get array indices.
				expect(closebracketToken);
				i++;
			}
			if (i > 0) {
				Array array = table.getArr(id);
				if (array == null) {
					error("Undeclared array " + table.getName(id));
					return null;
				} else if (assignment) {
					array.setCurrentIndices(indices);
					return array;
				} else {
					return program.addArrayInstr(Instruction.arrayLoad, array, indices);
				}
			}
		}

		// For now, pretend we don't have arrays.
		Variable var = table.getVar(id);

		// Check for errors and return the variable.
		if (var == null) {
			error("Undeclared variable " + table.getName(id)); 
		} else if (assignment) {
			Variable newVar = new Variable(id, table.getName(id));
			if (var.isGlobal() || (var.isLocal() && program.inMainFunction())) {
				newVar.setGlobal();
			} else {
				newVar.setLocal();
			}
			return newVar;
		} else if (var.uninit() && (program.inMainFunction() || var.isLocal()) ) {
			error("Uninitialized variable " + table.getName(id)); 
		} else {
			return var;
		}

		// Code for arrays...
		return null;
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

		// Check for unary minus.
		if (accept(minusToken)) {
			next = term();   
			expr = program.addInstr(neg, next);
			if (debug) { System.out.println("Generated neg instruction " + expr); }
		} else {
			expr = term();
		}

		while (check(plusToken) || check(minusToken) ) {
			if (accept(plusToken)) {
				next = term();   
				expr = program.addInstr(add, expr, next);
				if (debug) { System.out.println("Generated add instruction " + expr); }
			} else if (accept(minusToken)) {
				next = term();   
				expr = program.addInstr(sub, expr, next);
				if (debug) { System.out.println("Generated sub instruction " + expr); }
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
				if (debug) { System.out.println("Generated mul instruction " + term); }
			} else if (accept(divToken)) {
				next = factor();
				term = program.addInstr(div, term, next);
				if (debug) { System.out.println("Generated div instruction " + term); }
			}
		}
		return term;
	}

	/* factor.
	 * factor = designator | number | "(" expression ")" | funcCall.
	 * Create instructions to evaluate the factor.
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

	/* number.
	 * number = digit { digit }
	 * Returns a Constant that represents the number.
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
			 	  + table.symToString(token) + ".");
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

}