package game;

import util.*;

/**
 * Class to write a log, and also to maintain a log message stack for writing to
 * the UI.
 */
public final class LogBuffer
{

	/** This class is not instantiated */
	private LogBuffer()
	{
	}

	/** A buffer for displaying messages in the Log Window.
	 * Such messages are intended to record the progress of the game
	 * and can be used as a game report. */
	private static StringBuffer logBuffer = new StringBuffer();

	/** Add a message to the log buffer (and display it on the console) */
	public static void add(String message)
	{
		if (Util.hasValue(message))
		{
			System.out.println(message); // Will become a log file later
			logBuffer.append(message).append("\n");
		}
	}


	/** Get the current log buffer, and clear it */
	public static String get()
	{
		String result = logBuffer.toString();
		logBuffer = new StringBuffer();
		return result;
	}

}
