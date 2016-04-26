/* File: Compiler.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class Compiler {
	/* Class representing the entire compiler.
	   Options: -d        : Print debugging output.
	   			-cfg      : Print control flow graph (in VCG format).
	   			-dt       : Print dominator tree.
	   			-ifg      : Print interference graph (in VCG format).
	   			-instr    : Print SSA instructions.
	   			-O        : Perform optimizations.
	   			-regAlloc : Allocate registers.
	   			-assem    : Print assembly code.
	   			-o        : Print object code.
	   			-vtoi     : Convert variables to instructions after parsing.
	   			-run      : Run the program and display its output (if any).
	   			-mem 	  : Print out the state of memory and registers after execution.
	   			-all      : Do optimizations, register allocation and code generation.
	   Usage: java Compiler <filename> [-d] [-cfg] [-dt] [-ifg] [-instr] [-O] [-regAlloc] [-assem] [-o] [-vtoi] [-run] [-mem] [-all]
	 */

	// Output flags.
	static boolean debug;   // Debugging.
	final boolean cfg;      // Control flow graph.
	final boolean dt;       // Dominator tree.
	final boolean ifg;      // Interference graph.
	final boolean instr;    // SSA instructions.
	final boolean optimize; // Optimizations.
	final boolean regAlloc; // Register allocation.
	final boolean byteCode; // Byte code.
	final boolean assembly; // Assembly code.
	final boolean vtoi;     // Convert vars to instrs after parsing.
	final boolean run;      // Run compiled code with DLX simulator.
	final boolean memory;   // Print out the final state of memory.
	final boolean all;      // Do everything without any output.

	// Filename data.
	final String filename;    // The file to compile.
	final String filePrefix;  // The filename without its suffix e.g w/o ".txt".

	// To be used if we want to automatically write to files.
	public static final String CFG_SUFFIX = "-cfg.vcg";
	public static final String IFG_SUFFIX = "-ifg.vcg";

	/* Takes in file to compile, parses options and runs the compiler. */
	public static void main(String[] args) {

		// Capture command line arguments.
		String filename = "";
		boolean debug = false;
		boolean cfg   = false;
		boolean ifg   = false;
		boolean dt    = false;
		boolean instr = false;
		boolean optimize = false;
		boolean regAlloc = false;
		boolean byteCode = false;
		boolean assembly = false;
		boolean vtoi     = false;
		boolean run      = false;
		boolean memory   = false;
		boolean all      = false;

    	try {
     		filename = args[0];     // Get the filename.
     		if (args.length > 1) {  // Check for flags.
     			if (contains(args, "-d")) {
     				debug = true;
     			}
     			if (contains(args, "-cfg")) {
     				cfg = true;
     			}
     			if (contains(args, "-dt")) {
     				dt  = true;
     			}
     			if (contains(args, "-ifg")) {
     				ifg = true;
     			}
     			if (contains(args, "-instr")) {
     				instr = true;
     			}
     			if (contains(args, "-O")) {
     				optimize = true;
     			}
     			if (contains(args, "-regAlloc")) {
     				regAlloc = true;
     			}
     			if (contains(args, "-o")) {
     				byteCode = true;
     			}
     			if (contains(args, "-assem")) {
     				assembly = true;
     			}
     			if (contains(args, "-vtoi")) {
     				vtoi     = true;
     			}
     			if (contains(args, "-run")) {
     				run      = true;
     			}
     			if (contains(args, "-mem")) {
     				memory   = true;
     			}
     			if (contains(args, "-all")) {
     				all      = true;
     			}
     		}
     	} catch (Exception e) {
      		System.out.println("Usage: java Compiler <filename> [-d] [-cfg] [-dt] [-ifg] [-instr] [-O] [-regAlloc] [-assem] [-o] [-vtoi] [-run] [-mem] [-all]");
					System.exit(0);
    	}

    	// Compile the file.
    	Compiler compiler = new Compiler(filename, debug, cfg, dt, ifg, instr, optimize, regAlloc, byteCode, assembly, vtoi, run, memory, all);
    	compiler.compile();

	}

	// Compile the file.
	public void compile() {
		Parser parser        = new Parser(filename, debug);
		IntermedRepr program = parser.parse();

		Optimizer optimizer  = new Optimizer(program, debug);
		if (vtoi || optimize || regAlloc) {
			program = optimizer.preprocess();
		}
		if (optimize || all) {
			program = optimizer.optimize();
		}
		InstructionSchedule schedule;
		if (regAlloc) {
			if (debug) { System.out.println("Cleaning up deleted instructions..."); }
			program.clean();
			// if (debug) { System.out.println("Dumbly allocating registers..."); }
			// program.dumbRegAlloc();
			RegAllocator allocator = new RegAllocator(program);
		 	// program = allocator.allocate();
			schedule = allocator.allocateRegisters();
			System.out.println("Allocated registers:\n" + schedule);
		} else {
			schedule = null;
		}
		if (assembly || byteCode || run || all) {
			if (debug) { System.out.println("Generating native code..."); }
			CodeGenerator generator = new CodeGenerator(program, schedule, debug);
		    generator.generateCode();
		    if (assembly) {
		    	System.out.println(generator.assemblyToString());
		    } else if (byteCode) {
		    	System.out.println(generator.byteCodeToString());
		    }
		    if (run) {
		    	generator.runGeneratedCode();
		    }
		    if (memory) {
		    	System.out.println(generator.registersToString());
		    	System.out.println(generator.memoryToString());
		    }
		}

		// Auxillary data.
		if (cfg) {
			System.out.println(program.cfgToString());
		}
		if (dt) {
			System.out.println(program.domTreeToString());
		}
		if (ifg) {
			// Print out interference graph.
		}
		if (instr) {
			System.out.println(program.instrsToString());
		}

	}

	// Constructor.
	public Compiler(String filename, boolean debug, boolean cfg, boolean dt, boolean ifg,
					boolean instr, boolean optimize, boolean regAlloc, boolean byteCode, boolean assembly,
					boolean vtoi, boolean run, boolean memory, boolean all) {
		this.filename   = filename;
		this.filePrefix = getFilePrefix();
		this.debug    = debug;
		this.cfg      = cfg;
		this.dt       = dt;
		this.ifg      = ifg;
		this.instr    = instr;
		this.optimize = optimize;
		this.byteCode = byteCode;
		this.assembly = assembly;
		this.run      = run;
		this.memory   = memory;
		this.all      = all;

		if (ifg || byteCode || assembly || run || memory || all ) {
			this.regAlloc = true;
			this.vtoi     = true;
		} else {
			this.regAlloc = regAlloc;
			this.vtoi     = vtoi;
		}
	}

	// Get the filename prefix, i.e., cut off the extension.
	public String getFilePrefix() {
		String result = "";
		for (int i = 0; i < filename.length(); i++) {
			if (filename.charAt(i) == '.') {
				return result;
			} else {
				result += filename.charAt(i);
			}
		}
		return result;
	}

	// Helper for parsing command line arguments.
	public static boolean contains(String[] array, String element) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(element)) {
				return true;
			}
		}
		return false;
	}

	// Generic error message that does not show location in program.
	public static void error(String message) {
		System.out.println("ERROR: " + message);
		System.exit(0);
	}

	// Warning does not kill program.
	public static void warning(String message) {
		System.out.println("WARNING: " + message);
	}

}
