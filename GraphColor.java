/* File: GraphColor.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GraphColor {
  /* GraphColor implements the graph coloring method of register allocation.
   */
   InterferenceGraph ifg;      // The interference graph.
   IntermedRepr program;       // The program to allocate registers for.
   // OutputRepr   outputProgram; // The final result, ready for code generation.

   static final int numPhysRegs = 8;  // The number of physical registers available. This is fixed.
   int numVirRegs; // The number of virtual registers. This can increase.

   public GraphColor(IntermedRepr program) {
     this.program = program;
     this.ifg = new InterferenceGraph(program.instrs.size());
     // this.outputProgram = null;
     this.numVirRegs    = 0;
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

     for (Instruction instr : program.instrs) {
       ifg.addNode(instr);
     }

     Instruction current;
     for (int i = program.instrs.size() - 1; i >= 0; i--) {
       current = program.instrs.get(i);
       liveSet.remove(current);
       for (Instruction live : liveSet) {
         ifg.addEdge(current, live);
       }
       if (current.arg1 != null && current.arg1 instanceof Instruction ) {
          liveSet.add((Instruction) current.arg1);
       }
       if (current.arg2 != null && current.arg2 instanceof Instruction) {
          liveSet.add((Instruction) current.arg2);
       }
     }
    // Go from the end of the program to the beginning.
    // Operands of instructions must be alive.
    // For each instr i : op j k
    //     1. live = live - {i}
    //     2. for all x in live: add edge i <--> x
    //     3. live = live + {j, k}
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
     // TODO: make this smarter.
     node.setReg(numVirRegs);
     numVirRegs++;
   }

   // Resolve any remaining phi functions, i.e., eliminate them.
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

   }

   // Package the output into a format readable by
   // the code generator.
   private void packageOutput() {

   }

   class InterferenceGraph {
     // Encapsulates interference relationships between nodes.
     int numNodes;  // The number of non-removed nodes in the graph.
     int size;      // The number of nodes originally in the graph.
     Node[] nodes;  // All the nodes in the graph.
     int[][] edges; // A square matrix of relationships.

     int nextOpenID; // Next open node ID.

     HashMap<Instruction,Node> instrsToNodes;

     // Relationships.
     static final int EDGE    = 1;
     static final int NO_EDGE = 0;
     static final int REMOVED = -1;
     static final int DELETED = -2;

     public InterferenceGraph(int numInstrs) {
       size     = numInstrs;
       numNodes = size;
       nodes    = new Node[size];
       edges    = new int[size][size];
       nextOpenID = 0;
       instrsToNodes = new HashMap<Instruction,Node>();
     }

     /* Methods for building the interference graph. */

     Node addNode(Instruction instr) {
       Node node = new Node(instr, nextOpenID);
       nodes[nextOpenID] = node;
       nextOpenID++;
       instrsToNodes.put(instr, node);
       return node;
     }

     void addEdge(Instruction instr1, Instruction instr2) {
       Node node1, node2;
       node1 = instrsToNodes.get(instr1);
       node2 = instrsToNodes.get(instr2);
       int id1 = node1.id;
       int id2 = node2.id;
       modifyEdge(id1,id2,EDGE);
     }

     // Delete a node. (Hard delete)
     void delete(Node node) {
       int id1 = node.id;

       for (int id2 = 0; id2 < size; id2++) {
         modifyEdge(id1,id2,DELETED);
       }

       numNodes--;
       nodes[id1] = null;
     }

     /* Methods for coloring the interference graph. */

     boolean interferes(Node node1, Node node2) {
       int id1 = node1.id;
       int id2 = node2.id;
       return relationship(id1,id2) == EDGE;
     }

     // Remove a node such that it can be re-introduced later.
     void remove(Node node) {
       int id1 = node.id;

       for (int id2 = 0; id2 < size; id2++) {
         if (relationship(id1, id2) == EDGE) {
           modifyEdge(id1, id2, REMOVED);
         }
       }

       numNodes--;
       node.removed = true;
     }

     // Restore a node that has been removed.
     void restore(Node node) {
       int id1 = node.id;

       for (int id2 = 0; id2 < size; id2++) {
         if (relationship(id1, id2) == REMOVED) {
           modifyEdge(id1, id2, EDGE);
         }
       }

       numNodes++;
       node.removed = false;
     }

     private void modifyEdge(int id1, int id2, int relationship) {
       edges[id1][id2] = relationship;
       edges[id2][id1] = relationship;

       // Update neighbor counts.
       if (relationship == EDGE) {
         nodes[id1].numNeighbors++;
         nodes[id2].numNeighbors++;
       } else if (relationship == REMOVED || relationship == DELETED) {
         nodes[id1].numNeighbors--;
         nodes[id2].numNeighbors--;
       }
     }

     private int relationship(int id1, int id2) {
       return edges[id1][id2];
     }

     // Find a register for this node such that no
     // neighbors use the same register.
     // If none exists, return -1.
     int getValidReg(Node node) {
       int numRegs = numPhysRegs; // this may change
       int id1     = node.id;
       boolean[] registersUsed = new boolean[numRegs];
       // Determine which registers have been used.
       for (int id2 = 0; id2 < size; id2++) {
         if (relationship(id1,id2) == EDGE) {
           Node other = nodes[id2];
           assert other.reg != -1; // Other node must have assigned register.
           if (other.reg >= 0 && other.reg < numRegs) {
             registersUsed[other.reg] = true;
           }
         }
       }
       // Scan for free register.
       for (int reg = 0; reg < numRegs; reg++) {
         // If the register is not a neighbor, use it.
         if (!registersUsed[reg]) {
           return reg;
         }
       }
       // No available registers.
       return -1;
     }

     @Override
     public String toString() {
       String result = "";
       // Header
       result += VCG.header("Interference Graph");
       // Nodes
       Node current;
       for (int i = 0; i < size; i++) {
         current = nodes[i];
         if (current != null && !current.removed) {
           result += current.toString();
         }
       }
       // Edges
       for (int i = 0; i < size; i++) {
         for (int j = 0; j < i; j++) {
           if (relationship(i,j) == EDGE) {
             result += VCG.twoWayEdge(i,j, "black");
           }
         }
       }
       // Footer
       result += VCG.footer();
       return result;
     }

   }

   class Node {
     // A node in an interference graph.

     int id;               // Unique ID for this node.
     int numNeighbors;     // The number of neighbors this node has.
     boolean removed;      // True if this node has been removed.
     Instruction[] instrs; // The instructions in this node.
     int reg;              // The register assigned to this node.

     // Constructor.
     public Node(Instruction instr, int id) {
       this.instrs = new Instruction[2];
       this.id   = id;
       instrs[0] = instr;
       this.reg = -1; // Indicates not yet assigned.
     }

     void addInstr(Instruction instr) {
       instrs[1] = instr;
     }

     // Determines whether or not the "cost" of this node is less than
     // the cost of the other node.
     // Lower cost --> less important --> better to spill to registers.
     boolean lowerCost(Node other) {
       return this.cost() < other.cost();
     }

     int cost() {
       return 0;
     }

     void setReg(int reg) {
       this.reg = reg;
       if (instrs[0] != null) {
         instrs[0].assignReg(reg);
       }
       if (instrs[1] != null) {
         instrs[0].assignReg(reg);
       }
     }

     @Override
     public String toString() {
       String contents = instrs[0].toString() + "\n";
       if (instrs[1] != null) {
         contents += instrs[1].toString() + "\n";
       }
       contents += "Register: " + reg + "\n";
       return VCG.node(id, "IFG Node", contents);
     }

   }
}
