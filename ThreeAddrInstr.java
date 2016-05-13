/* File: ThreeAddrInstr.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
public class ThreeAddrInstr {
  int pos; // Position in code.
  int op;  // SSA operation.

  // Registers:
  // Non-negative register indicates physical or virtual register.
  int dstReg;
  int reg1;
  int reg2;

  static int NO_REG   = -1; // Parameter doesn't exist.
  static int CONSTANT = -2; // Parameter is a constant value.

  // Constant values.
  int const1; // Doubles as jump value if appropriate.
  int const2;

  // For branching instructions.
  Block jumpTo;

  public ThreeAddrInstr(int pos, int op) {
    this.pos = pos;
    this.op  = op;
  }

  public void setRegs(int dst, int arg1, int arg2) {
    dstReg  = dst;
    reg1    = arg1;
    reg2    = arg2;
  }

  public void setConstants(int c1, int c2) {
    const1 = c1;
    const2 = c2;
  }

  public void commitJump() {
    // If this is a branching instruction,
    // give the relative offset.
    if (true) {
      const1 = jumpTo.pos - pos;
    }
    // If this is an absolute jump, give the absolute offset.
    else {
      const1 = jumpTo.pos;
    }
  }

}
