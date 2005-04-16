/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Log.java,v 1.1 2005/04/16 22:51:22 evos Exp $
 * 
 * Created on 16-Apr-2005 by Erik Vos
 * 
 * Change Log:
 */
package game;

/**
 * Class to write a log, and also to maintain a log message stack
 * for writing to the UI.
 * @author Erik
 */
public final class Log {
	
	/** This class is not instantiated */
	private Log() {}
	
	private static StringBuffer logbuf = new StringBuffer();
	
	/** Log a message, and add it to the buffer */
	public static void write (String message) {
		System.out.println(message); // Will become a log file later
		logbuf.append (message).append("\n");
	}
	
	/** Get the current buffer, and clear it */
	public static String getBuffer () { 
		String result = logbuf.toString();
		logbuf = new StringBuffer();
		return result;
	}

}
