/* File: VCG.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class VCG {
	/* A collection of static methods to aid in
	   creation of VCG code to visualize intermediate
	   code in the compiler. 
	*/

	// Create a VCG header with the given title.
	public static String header(String title) {
		return "graph: { title: \"" + title +"\" \n" 
						// + "layoutalgorithm: dfs \n" 
						+ "manhattan_edges: yes \n" 
						+ "smanhattan_edges: yes \n"
						+ "orientation: top_to_bottom \n";
	}

	// Create a VCG node with the given id, description, and contents.
	public static String node(int id, String description, String contents) {
		return  "node: { \n" +
			    "title: \"" + id + "\" \n" +
				"label: \"" + description + " [\n" +
				contents + "]\" \n} \n";
	}

	// Create a VCG edge with the given source, destination (ids of nodes) and color.
	public static String edge(int src, int dest, String color) {
		return  "edge: { sourcename: \"" + src + "\" \n" +
			    "targetname: \"" + dest + "\" \n" +
			    "color: " + color + "\n } \n";
	}

	// Create a VCG footer.
	public static String footer() {
		return "}";
	}
}