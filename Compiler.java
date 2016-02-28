/* File: Compiler.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class Compiler {
	/* Class representing the entire compiler.
	   Options: -d        : Print debugging output
	   			-cfg      : Print control flow graph (in VCG format).
	   			-dt       : Print dominator tree.
	   			-ifg      : Print interference graph (in VCG format). 
	   			-instr    : Print SSA instructions.
	   			-O        : Perform optimizations.
	   			-regAlloc : Allocate registers.
	   			-assem    : Print assembly code.
	   			-o        : Print object code.
	   Usage: java Compiler <filename> [-d] [-cfg] [-dt] [-ifg] [-instr] [-O] [-regAlloc] [-assem] [-o]
	 */

	// Output flags.
	final boolean debug; // Debugging.
	final boolean cfg;   // Control flow graph.
	final boolean dt;    // Dominator tree.
	final boolean ifg;   // Interference graph.
	final boolean instr; // SSA instructions.
	final boolean optimize; // Optimizations.
	final boolean regAlloc; // Register allocation.
	final boolean byteCode; // Byte code.
	final boolean assembly; // Assembly code.

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
		boolean assembly = 	false;

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
     		} 
     	} catch (Exception e) {
      		System.out.println("Usage: java Compiler <filename> [-d] [-cfg] [-dt] [-ifg] [-instr] [-O]");
    	}

    	// Compile the file.
    	Compiler compiler = new Compiler(filename, debug, cfg, dt, ifg, instr, optimize, regAlloc, byteCode, assembly);
    	compiler.compile();
		
	}

	// Compile the file.
	public void compile() {
		Parser parser        = new Parser(filename, debug);
		IntermedRepr program = parser.parse();

		if (optimize) {
			Optimizer optimizer  = new Optimizer(program, debug);
			program = optimizer.optimize();
		}
		if (regAlloc) {
			// RegAllocator allocator = new RegAllocator(program);
		    // program = allocator.allocate();
		}
		if (assem || byteCode) {
			// CodeGenerator generator = new CodeGenerator(program);
		    // generator.generateCode();
		    if (assem) {
		    	// System.out.println(generator.assemblyToString());
		    } else {
		    	// System.out.println(generator.byteCodeToString());
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
					boolean instr, boolean optimize, boolean regAlloc, boolean byteCode, boolean assem) {
		this.filename   = filename;
		this.filePrefix = getFilePrefix();
		this.debug    = debug;
		this.cfg      = cfg;
		this.dt       = dt;
		this.ifg      = ifg;
		this.instr    = instr;
		this.optimize = optimize;
		this.regAlloc = regAlloc;
		this.byteCode = byteCode;
		this.assem    = assem;

		if (ifg) {
			this.regAlloc = true;
		}
		if (byteCode || assem) {
			this.regAlloc = true;
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

}