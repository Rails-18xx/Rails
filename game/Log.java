/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Log.java,v 1.3 2005/05/12 22:22:28 evos Exp $
 * 
 * Created on 16-Apr-2005 by Erik Vos
 * 
 * Change Log:
 */
package game;

import util.*;

/**
 * Class to write a log, and also to maintain a log message stack
 * for writing to the UI.
 * @author Erik
 */
public final class Log {
	
	/** This class is not instantiated */
	private Log() {}
	
	/** A buffer for displaying messages in the UI */
	private static StringBuffer messageBuffer = new StringBuffer();
	
	/** A buffer for displaying errors in the UI */
	private static StringBuffer errorBuffer = new StringBuffer();
	
	/** Log a message, and add it to the display buffer */
	public static void write (String message) {
	    if (XmlUtils.hasValue(message)) {
	        System.out.println(message); // Will become a log file later
	        messageBuffer.append (message).append("\n");
	    }
	}
	
	/** Add it to the error buffer */
	public static void error (String message) {
	    if (XmlUtils.hasValue(message)) {
		    System.out.println(message);
	        errorBuffer.append(message);
	    }
	}
	
	/** Get the current message buffer, and clear it */
	public static String getMessageBuffer () { 
		String result = messageBuffer.toString();
		messageBuffer = new StringBuffer();
		return result;
	}

	/** Get the current error buffer, and clear it */
	public static String getErrorBuffer () { 
		String result = errorBuffer.toString();
		errorBuffer = new StringBuffer();
		return result;
	}

}
