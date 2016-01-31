/* File: StringTable.java
 * Author: Tatiana Bradley
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.util.Hashtable;
import java.util.ArrayList;

public class StringTable {
	// Class representing two-way lookup symbol table for
	// identifiers and ID's.

	private Hashtable<String, Integer> hashtable;
	private ArrayList<String>          list; // Is this needed?
	private int nextOpenID;

	/* Reserved words. */
	private static final String[] reserved = new String[]{
		"*", "/", "+", "-", "==", "!=", "<", ">=", "<=", ">", ".",
		",", "[", "]", ")", "<-", "then", "do", "(", ";", "{", "}", "od",
		"fi", "else", "let", "call", "if", "while", "return", "var",
		"array", "function", "procedure", "}", "main"
	};

	public StringTable() {
		nextOpenID = 0;
		hashtable = new Hashtable(reserved.length);
		list = new ArrayList(reserved.length);

		// Add reserved words.
		for (int i = 0; i < reserved.length; i++) {
			hashtable.put(reserved[i], i);
			list.add(i, reserved[i]);
			nextOpenID++;
		}
	}

	public int stringToId(String name) {
		return hashtable.get(name);
	}

	public String idToString(int id) {
		return list.get(id);
	}

	public int addOrLookup(String name) {
		int id;
		if (!hashtable.containsKey(name)) {
			id = nextOpenID;
			hashtable.put(name, id);
			list.add(id, name);
			nextOpenID++;
		} else {
			id = stringToId(name);
		}
		return id;
	}
}