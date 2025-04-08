package com.solace.psg.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Simple utility to dump a throwable to a string. 
 *
 */
public class StackTracePrinter {
	public static String toString(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		String sStackTrace = sw.toString();
		return sStackTrace;
	}
}
