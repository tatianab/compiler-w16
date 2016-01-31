/* File: Reader.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Reader {
	// Encapsulates streams of characters.

	public char sym; // The current character. 0x00 = error, 0xff = EOF.
	private BufferedReader buffer;

	// Constructor. Opens filename and scans the first character into sym.
	public Reader(String filename) {
		try {
			buffer = new BufferedReader(new FileReader(filename));
		}
		catch (Exception e) {
			System.out.println("Could not read from file " + filename);
		}
		next();
	}

	// Advance to the next character, unless EOF has been reached.
	public void next() {
		if (sym != 0xff) { 
			int c = 0; // Default is error state.
			// Read character.
			try {
				c = buffer.read();
				if (c == -1) { // EOF reached.
					sym = 0xff;
					return;
				}
			} catch (Exception e) {
				System.out.println("Could not read from buffer.");
			}
			// Store character.
			sym = (char) c;
		}
	}

	// Signal an error with current file position.
	public void error(String errorMsg) {
		System.out.println(errorMsg);
	}

}