/* File: GraphColor.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
import java.util.ArrayList;
import java.util.Stack;
import java.util.HashSet;

public class GraphColor {
  /* GraphColor implements the graph coloring method of register allocation.
   */
   InterferenceGraph ifg;      // The interference graph.
   IntermedRepr program;       // The program to allocate registers for.
   ArrayList<ThreeAddrInstr> output; // The final result, ready for code generation.

   static final int numPhysRegs = 8;  // The number of physical registers available. This is fixed.
   int numVirRegs; // The number of virtual registers. This can increase.

   public GraphColor(IntermedRepr program) {
     this.program = program;
     this.ifg = new InterferenceGraph(program.instrs.size(), numPhysRegs);
     // this.outputProgram = null;
     this.numVirRegs    = 0;
     this.output = new ArrayList<ThreeAddrInstr>();
   }

   // Wrapper for all functionality.
   // Call this to do everything in a sensible order.
   public void allocateRegisters() {
     createInterferenceGraph();
     clusterPhi();
     color();
     resolvePhi();
     packageOutput();
     return; // Return final output?
   }

   // Create the interference graph.
   private void createInterferenceGraph() {
     // The set of instructions that are currently live.
     HashSet<Instruction> liveSet = new HashSet<Instruction>();
     Instruction[]        args;

     // Create IFG nodes for every instruction in the program.
     for (Instruction instr : program.instrs) {
       ifg.addNode(instr);
     }

     // Discover interference relationships block by block.
     Block current = program.currentBlock; // Last block in program.
     handleBlock(current, liveSet);
   }

   private void handleBlock(Block block, HashSet<Instruction> start) {
     HashSet<Instruction> prevStart = blockIfg(block, start);
     Block prev = block.in1;
     if (prev != null) {
       switch (prev.type) {
         case Block.WHILE_FOLLOW:
          handleWhile(prev, prevStart);
          break;
         case Block.IF_JOIN:
          handleIf(prev, prevStart);
         default:
          handleBlock(block.in1, end);
          break;
       }
     }
   }

   private HashSet<Instruction> handleWhile(Block follow, HashSet<Instruction> start) {
     // TODO
     // Handle follow block
     // Handle enter block, skipping phi
     // Handle body block
     // Second pass on enter block, not skipping phi
     return null;
   }

   private HashSet<Instruction> handleIf(Block join, HashSet<Instruction> start) {
     // TODO
     return null;
   }

   // Given start (the liveSet at the bottom of the block), return the liveSet
   // at the top of the block. In the meantime, determine the interference
   // relationships for the instructions in the block.
   private HashSet<Instruction> blockIfg(Block block, HashSet<Instruction> start) {
     // For each instr i : op j k
     //     1. live = live - {i}
     //     2. for all x in live: add edge i <--> x
     //     3. live = live + {j, k}
    HashSet<Instruction> liveSet = new HashSet<Instruction>(start);
    Instruction[] args;
    Instruction current = block.end;
    while (current != null) {
      args    = current.getInstrArgs();
      liveSet.remove(current);
      for (Instruction live : liveSet) {
        ifg.addEdge(current, live);
      }
      for (Instruction arg : args) {
        liveSet.add(arg);
      }
      current = current.prev;
    }
    return liveSet;
   }

   // Cluster any non-interfering phi instructions.
   private void clusterPhi() {
     //  for all phi instructions x : phi (y1, y2, ...) do:
     //  cluster = {x}
     //  for all yi that are not constants:
     //     if yi does not interfere with cluster:
     //         add yi to the cluster
     //         remove yi from the interference graph
     //  Replace x with cluster.
    ArrayList<Instruction> phiInstrs = program.getPhiInstrs();
    Node cluster, otherNode;
    Instruction[] args;
    for (Instruction phiInstr : phiInstrs) {
      cluster = ifg.getNode(phiInstr);
      args    = phiInstr.getInstrArgs();
      for (Instruction arg : args) {
        otherNode = ifg.getNode(arg);
        if (!ifg.interferes(cluster, otherNode)) {
          ifg.merge(cluster, otherNode); // Creates one "supernode".
        }
      }
    }
   }

   // Color the interference graph.
   private void color() {
     Node current = null;

     // Loop over nodes x
     // Keep track of min-cost node
     // If x has fewer than numRegisters neighbors
     for (Node node : ifg.nodes) {
       if (node != null && !node.removed) {
         if (node.numNeighbors < numPhysRegs) {
           current = node;
           break;
         } else if (current == null || node.lowerCost(current)) {
           current = node;
         }
       }
     }

     if (current != null) {
      // Remove x and its edges from graph.
      ifg.remove(current);
      // Color remaining graph
      color();
      // Add x and its neighbors back
      ifg.restore(current);

      // Choose color for x different from all neighbors, if one exists
      // Else spill x.
      int reg = ifg.getValidReg(current);
      if (reg != -1) {
        current.setReg(reg);
      } else {
        spill(current);
      }
    }
   }

   private void spill(Node node) {
     // Right now, each spilled instruction gets its own virtual register.
     // TODO: make this smarter.
     node.setReg(numVirRegs);
     numVirRegs++;
   }

   // Resolve phi instructions if necessary.
   private void resolvePhi() {
    //  Recall that phi is a constraint on register allocation. For c : phi(a,b) : if a,b,c don't interfere with each other, then assign them all to the same register. Otherwise, resolve by introducing register moves after graph coloring:
    //
    // a lives in Rx.
    // b lives in Ry.
    // c lives in Rz.
    // Introduce:
    // move Rx -> Rz (along edge from a's block)
    // move Ry -> Rz (along edge from b's block)
    // Note that these move instructions occur on the edges, rather than in the blocks.

    ArrayList<Instruction> phiInstrs = program.getPhiInstrs();
    Instruction[] args;
    for (Instruction phiInstr : phiInstrs) {
      args =  phiInstr.getInstrArgs();
      for (Instruction arg : args) {
        if ( ifg.interferes(ifg.getNode(phiInstr), ifg.getNode(arg)) ) {
          // Introduce move instruction along edge.
          program.addMoveOnEdge(arg, phiInstr);
        }
        // Otherwise, we have already clustered.
      }
    }

   }

   // Package the output into a format readable by
   // the code generator.
   private void packageOutput() {
     Block block, fallThrough;
     Stack<Block> workList = new Stack<Block>();
     // Instructions with jumps that need to be fixed.
     ArrayList<ThreeAddrInstr> jumps = new ArrayList<ThreeAddrInstr>();

     // Loop over functions.
     for (Function function : program.functions) {
      // Start at the first block, and output code favoring fall-through blocks.
      block = function.enter; // TODO
      fallThrough = block.fallThrough;
      while (fallThrough != null) {
        // Handle block
        packageBlock(block);
        // Add branch to queue if not visited
        if (block.branch != null && !block.branch.visited) {
          workList.push(block.branch);
        }
        // Reset fallThrough
        block       = fallThrough;
        fallThrough = block.fallThrough;
       }
     }
   }

   private void packageBlock(Block block) {
     // For each instruction in the block, add one or more three address
     // instructions.
     Instruction current = block.begin;
     while (current != null) {
       packageInstruction(current);
       current = current.next;
     }
   }

   private void packageInstruction(Instruction instr) {
     int op;
     // If instruction is a computation
     // SSA codes:
     // if (op >= Instruction.neg && op )

     // If instruction is I/O
     // SSA codes

     // If instruction is a function call
     // SSA codes:
   }
}
