# Midterm Progress Report
Date:    2/4/2015  
Authors: Tatiana Bradley and Wai Man Chan  
Course:  CS 241 - Advanced Compiler Construction  

We are roughly half-way done with Step 1 of the project. Here's what we 
have implemented so far:
* File Reader - reads a file into a stream of characters.
* Tokenizer   - reads a file into a stream of PL241 tokens (using file reader), and
			    stores unique identifiers in a string table.
* Parser      - recursively parses PL241 statements.

The parser currently parses tokens recursively based on the given grammar,
and breaks the program into basic blocks for straight-line code, if-statements,
and while loops. (It does not yet handle procedures or functions). The parser can
also print out VCG code for the CFG.

We have also created a data structure to represent SSA values, and another to store
the intermediate representation of the program (including the CFG).

The parser does not yet create SSA values/instructions, but that is our immediate next step.

Our project is available on Github at https://github.com/tatianab/compiler-w16/.
