/* File: SSA.java
 * Authors: Tatiana Bradley and Wai Man Chan
 * Winter 2016
 * CS 241 - Advanced Compiler Design
 */

public class SSA {
	/* A collection of static data and methods to aid
	   in the generation of intermediate code in SSA form.
	*/

	/* Operation codes. */
	public static final int neg     = 1;
	public static final int add     = 2;
	public static final int sub     = 3;
	public static final int mul     = 4;
	public static final int div     = 5;
	public static final int cmp     = 6;
   				  
	public static final int adda    = 7;
	public static final int load    = 8;
	public static final int store   = 9;
	public static final int move    = 10;
	public static final int phi     = 11;
                  
	public static final int end     = 12;
	public static final int bra     = 13;
	              
	public static final int read    = 14;
	public static final int write   = 15;
	public static final int writeNL = 16;
                   
	public static final int bne     = 20;
	public static final int beq     = 21;
	public static final int bge     = 22;
	public static final int blt     = 23;
	public static final int bgt     = 24;
	public static final int ble     = 25;
                  
	public static final int call    = 30;

	public static final int arrayStore = 35;
	public static final int arrayLoad  = 36;
	/* End operation codes. */

	private static final String[] ops = new String[]{null, "neg","add","sub","mul","div",
											  "cmp","adda","load","store","move","phi","end","bra",
											  "read","write","writeNL", null, null, null,
											  "bne","beq","bge","blt","bgt","ble", null, null, null, null,
											  "call", null, null, null, null, "arrayStore", "arrayLoad"};
    
    // Return the string representation of the given operation code.
    public static String opToString(int opCode) {
    	return ops[opCode];
    }

    // Return the number of arguments that this operation expects.
    public static String numArgs(int opCode) {
    	if (opCode == neg) {

    	} else if (opCode > )
    	return 0;
    }
}