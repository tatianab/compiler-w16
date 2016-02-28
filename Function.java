/* File: Function.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class Function {
	public int id;
	public String ident;
	public int numParams;
	public Variable[] formalParams;
	public Block start;
	public Instruction returnInstr;
}