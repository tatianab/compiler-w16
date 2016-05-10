/* File: FinalInstruction.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

 public class FinalInstruction {
   /* Minimal instruction with register allocated. */
   int op;       // The operation. -1 if constant.
   int value;    // The constant value, if this is a constant.
   int reg;      // Register. -1 if no register.
   FinalInstruction[] args; // Arguments. Null if constant.

   static final int CONSTANT = -1;

   public FinalInstruction(int constant) {
     op    = CONSTANT;
     reg   = CONSTANT;
     value = constant;
     args  = null;
   }

   public FinalInstruction(Instruction instr) {

   }

   public isConstant() {
     return op == CONSTANT;
   }

 }
