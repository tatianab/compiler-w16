# compiler-w16

An optimizing compiler for the "PL241" language, a simple procedural language with assignments, operations (+,-,\*,/), if-else statements, while loops, functions, integers and integer arrays.

Written by **Tatiana Bradley** and **Wai Man Chan** for "CS 241 - Advanced Compiler Construction" (Winter 2016), a graduate course taught by Michael Franz at University of California, Irvine.

See the [CS 241 course website](http://www.michaelfranz.com/w16cs241.html) for more info on requirements and the PL 241 grammar.

### Usage
To compile `<filename>` to assembly with optimizations, use:

To compile and run `<filename>` with optimizations, use:

To view unoptimized SSA:
(Use a VCG viewer to see graphical representation.)

To view optimized SSA:
(Use a VCG viewer to see graphical representation.)

To view allocated registers:


All flags:
```
[-d] - debugging output
```


### Design Road Map
The main program `Compiler.java` parses command line arguments and runs the various elements of the compiler in a sensible order.

#### Scanning/Parsing
*Scanning* is breaking up an input program into tokens. *Parsing* is ensuring that the tokens fit the grammar and converting them into an intermediate representation.

`Reader.java` reads a PL241 file character by character, `Tokenizer.java` reads a PL241 file token by token, and `Parser.java` uses the token stream to parse the file according to the grammar, converting it into *basic blocks* (units of control flow) made up of *SSA instructions*. Control flow information (i.e., edges between basic blocks) is also recorded.

`IntermedRepr.java` stores the result.

#### SSA Form
The intermediate representation we use is *SSA (Static Single Assignment)* form. It requires that each variable be defined only once.

A key feature of SSA is *phi functions*, which indicate that a variable may take on multiple values depending on what control flow path is taken.

#### Optimizations
`Optimizer.java` performs the following optimizations:
* *Copy/Constant Propagation* - elimination of instructions of the form `move x y` where `x` is a variable or constant. All later instances of `y` are replaced with `x`.
* *Common Subexpression Elimination* - elimination of instructions whose result has already been evaluated.  
* *Constant Folding* - elimination of instructions of the form `op c1 c2` where `c1` and `c2` are constants. The instruction is evaluated and all references to it are replaced by the result of evaluation.  
* *Dead Code Elimination* - eliminate instructions whose result is never used.

The `-O` flag turns on optimization.

#### Register Allocation

#### Code Generation
The final step is converting the optimized code with registers allocated into machine-readable code. Our compiler outputs byte code readable by the *DLX Processor* with the `-o` flag, or human-readable assembly with the `-assem` flag. Code execution can be simulated with the `-run` flag.

Code generation occurs in `CodeGenerator.java` with help from `DLX.java`.
