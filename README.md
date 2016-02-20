# compiler-w16

An optimizing compiler for the "PL241" language, a simple procedural language with assignments, operations (+,-,*,/), if-else statements, while loops, functions, integers and integer arrays.

Written by Tatiana Bradley and Wai Man Chan for "CS 241 - Advanced Compiler Construction", a graduate course at University of California, Irvine.

### To-do list

#### Step 1
* Handle arrays
* Handle functions/procedures (including recognize built-in functions)
* Insert phi functions
* Create dominator tree
* Test Step 1

#### Step 2
* Common subexpression elimination
* Constant / Copy propagation
* Dead code elimination
* Create trace mode to show process
* Test Step 2

#### Step 3
* Global register allocation
	* Track live ranges
	* Create interference graph
	* Color interference graph
* Test Step 3

#### Step 4
* Code generator for DLX processor
* Test Step 4

#### Overall testing