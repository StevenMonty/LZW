/*************************************************************************************************************
 *  Compilation:  javac LZWmod.java
 *  Execution:    java LZWmod - MODE < input.txt > out.txt  (compress)
 *  Execution:    java LZWmod + < input.txt > out.txt  (expand, auto detects which compression mode was used)
 *  Mode Options: n = none, r = reset the codebook when full, m = reset mode + monitor compression ratio
 *  Dependencies: BinaryStdIn.java BinaryStdOut.java
 *
 *  Compress or expand binary input from standard input using LZW.
 *************************************************************************************************************/

public class LZWmod {

	/* TODO
	 * method-ize the reset and up size functions
	 * 
	 * Extra Credit:
	 * 	Implement the monitor mode and compression ratios
	 * 
	 */	

	private static int R = 256;       // number of input chars in the ASCII table, EOF codeword
	private static int L = 512;       // max number of codewords = 2^W
	private static int W = 9;         // codeword width in bits range (9-16)

	public static void compress(CodeBookMode mode) {
		TSTmod<Integer> st = new TSTmod<Integer>();	// The codebook uses a TernarySymbolTable for compression
		for (int i = 0; i < R; i++)
			st.put(new StringBuilder("" + (char) i), i);	// Fill the codebook with the ASCII chars
		int code = R+1;  // R is codeword for EOF

		if (mode == CodeBookMode.RESET || mode == CodeBookMode.MONITOR)
			BinaryStdOut.write(1, 1);	// One bit flag to store the codebook mode so that the expand
		else							// algorithm can detect whether the codebook should be reset 
			BinaryStdOut.write(0, 1);	// or not upon filling to the max 16-bit width

		StringBuilder current = new StringBuilder();
		char c = BinaryStdIn.readChar();		//read and append the first char
		current.append(c);
		Integer codeword = st.get(current);
		
		while (!BinaryStdIn.isEmpty()) {
			codeword = st.get(current);
			char next = BinaryStdIn.readChar();
			current.append(next);
			if(!st.contains(current)){
				BinaryStdOut.write(codeword, W);	// Write the codeword to the compressed file

				if (code < L)    // Add to symbol table if not full
					st.put(current, code++);
				else	
				{
					if(W < 16) {	// if the codeword width is < max width

						W++;
						L <<= 1;	// A single left bit shift is the same as (L *= 2)
						st.put(current, code++);

					} else if(W == 16 && mode == CodeBookMode.RESET) {	// If at max codeword width and in reset mode

						//TODO re-implement resetCodeBook()

						st = new TSTmod<Integer>();	// Reinitialize the codebook

						for (int i = 0; i < R; i++)
							st.put(new StringBuilder("" + (char) i), i);	// Fill the codebook with the ASCII chars

						W = 9;		//	Reset back to inital values
						L = 512;						
						code = R+1;

						st.put(current, code++);
					}
				}
				current = current.delete(0, current.length());
				current.append(next);
			}
		}
		codeword = st.get(current);
		BinaryStdOut.write(codeword, W);
		BinaryStdOut.write(R, W); //Write EOF
		BinaryStdOut.close();
	}

	public static void expand() {
		String[] st = new String[L];	//Uses a String[] as the codebook symbol table for expansion
		int i; // next available codeword value

		// initialize symbol table with all 1-character strings
		for (i = 0; i < R; i++)
			st[i] = "" + (char)i;
		st[i++] = "";                        // unused codeword for EOF, st[R] is the EOF char

		boolean resetFlag = BinaryStdIn.readBoolean();		// First bit of the file will be the reset flag to determine 
		// which CodeBookMode was used during compression

		CodeBookMode mode = (resetFlag) ? CodeBookMode.RESET : CodeBookMode.NONE;

		int codeword = BinaryStdIn.readInt(W);
		String val = st[codeword];

		while (true) {

			if (W < 16 && i >= L) {	// if not at max codeword width and current codeword >= L which == the st length

				W++;				// Increment the bit width
				L <<= 1;			// A single left bit shift is the same as (L *= 2)
				st = upsizeArr(st);	// Once i becomes >= L, the codebook.length will need to be able to fit up to L elements

			} else if(W == 16 && i >= L && mode == CodeBookMode.RESET) {	// If at max codeword width and in reset mode

				st = new String[L];	// Reinitialize the codebook

				for (i = 0; i < R; i++)		// Fill the codebook with the ASCII chars
					st[i] = "" + (char) i;
				st[i++] = "";  				

				W = 9;			//	Reset back to initial values
				L = 512;		
				i = R+1;
			}

			BinaryStdOut.write(val);
			codeword = BinaryStdIn.readInt(W);
			if (codeword == R) break;	// Read in EOF, break from loop
			String s = st[codeword];
			if (i == codeword) 
				s = val + val.charAt(0);   // special case hack

			if (i < L) 
				st[i++] = val + s.charAt(0);
			val = s;

		}	// end while

		BinaryStdOut.close();
	}

	private static String[] upsizeArr(String[] arr) {

		String[] tmpArr = new String[arr.length * 2];	

		for (int i = 0; i < arr.length; i++) 
			tmpArr[i] = arr[i];

		return tmpArr;
	}

	public static void main(String[] args) {

		if(args.length < 1) { 
			System.err.println("USAGE: java LZWmod -/+ MODE < input.txt > output.lzw");
			System.err.printf ("MODE OPTIONS:\n\t r : reset codebook once filled \n"
					+ "\t n : leave codebook unaltered once filled \n" );
			//					+ "\t m : monitor the compression ratio while in reset mode \n");
			System.exit(1);
		}

		CodeBookMode mode = CodeBookMode.NONE;	// default compression mode

		if (args.length == 2) {	// if a flag is entered then select the compression mode

			switch(args[1]) {

			case "r":
				mode = CodeBookMode.RESET;
				break;
				//			case "m": 
				//				mode = CodeBookMode.MONITOR;
				//				break;
			case "n":
			default:
				mode = CodeBookMode.NONE;
				break;
			}
		}

		if      (args[0].equals("-")) compress(mode);
		else if (args[0].equals("+")) expand();
		else throw new RuntimeException("Illegal command line argument");
	}

	/**
	 * Enum type to represent the different modes of how to handle the codebook
	 * filling up.
	 * 
	 * None 	 = codebook will remain unaltered once filling to capacity, may result in lower compression ratio
	 * Reset	 = codebook will reset once filled to capacity (16-bit width)
	 * Monitor 	 = codebook will be in reset mode while monitoring the compression ratio of the file
	 * 
	 */
	private static enum CodeBookMode{
		NONE, 
		RESET, 
		MONITOR	//TODO extra credit
	}

}
