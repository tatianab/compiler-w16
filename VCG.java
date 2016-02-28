/* File: VCG.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
package compiler-w16;
public class VCG {
	/* A collection of static methods to aid in
	   creation of VCG code to visualize intermediate
	   code in the compiler. 
	*/

	public static String header(String title) {
		return "graph: { title: \"" + title +"\" \n" 
						// + "layoutalgorithm: dfs \n" 
						+ "manhattan_edges: yes \n" 
						+ "smanhattan_edges: yes \n"
						+ "orientation: top_to_bottom \n";
	}

	public static String node(int id, String description, String contents) {
		return  "node: { \n" +
			    "title: \"" + id + "\" \n" +
				"label: \"" + description + " [\n" +
				contents + "]\" \n} \n";
	}

	public static String edge(int src, int dest, String color) {
		return  "edge: { sourcename: \"" + src + "\" \n" +
			    "targetname: \"" + dest + "\" \n" +
			    "color: " + color + "\n } \n";
	}

	public static String footer() {
		return "}";
	}
}