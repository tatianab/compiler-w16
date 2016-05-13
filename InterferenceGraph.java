/* File: InterferenceGraph.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 *
 * File includes Node class.
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;

public class InterferenceGraph {
  // Encapsulates interference relationships between nodes.
  int numNodes;  // The number of non-removed nodes in the graph.
  int numRegs;   // The number of physical registers available.
  int size;      // The number of nodes originally in the graph.
  Node[] nodes;  // All the nodes in the graph.
  int[][] edges; // A square matrix of relationships.

  int nextOpenID; // Next open node ID.

  HashMap<Instruction, Node> instrsToNodes; // Maps instructions to nodes.

  // Relationships.
  static final int EDGE    = 1;
  static final int NO_EDGE = 0;
  static final int REMOVED = -1;
  static final int DELETED = -2;

  public InterferenceGraph(int numInstrs, int numPhysRegs) {
    size     = numInstrs;
    numRegs  = numPhysRegs;
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

  void merge(Node node1, Node node2) {
    node1.merge(node2);
    delete(node2);
  }

  Node getNode(Instruction instr) {
    return instrsToNodes.get(instr);
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

  int id;                                // Unique ID for this node.
  int numNeighbors;                      // The number of neighbors this node has.
  boolean removed;                       // True if this node has been removed.
  private ArrayList<Instruction> instrs; // The instructions in this node.
  int reg;                               // The register assigned to this node.

  // Constructor.
  public Node(Instruction instr, int id) {
    this.instrs = new ArrayList<Instruction>(3);
    instrs.add(instr);
    this.id   = id;
    this.reg = -1; // Indicates not yet assigned.
  }

  void addInstr(Instruction instr) {
    instrs.add(instr);
  }

  // Determines whether or not the "cost" of this node is less than
  // the cost of the other node.
  // Lower cost --> less important --> better to spill to registers.
  boolean lowerCost(Node other) {
    return this.cost() < other.cost();
  }

  int cost() {
    int result = 0;
    for (Instruction instr : instrs) {
      result += instr.cost;
    }
    return result;
  }

  // Merge the instruction lists.
  void merge(Node other) {
    instrs.addAll(other.instrs);
  }

  void setReg(int reg) {
    this.reg = reg;
    for (Instruction instr : instrs) {
      instr.assignReg(reg);
    }
  }

  @Override
  public String toString() {
    String contents = "";
    for (Instruction instr : instrs) {
      contents += instr.toString() + "\n";
    }
    contents += "Register: " + reg + "\n";
    return VCG.node(id, "IFG Node", contents);
  }

}
