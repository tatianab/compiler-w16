/* File: Constant.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */
public class Constant extends Argument {
	int value;
	String label;

	@Override
	public String shortRepr() {
		return label;
	}
}