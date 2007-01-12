package game;

import org.apache.log4j.Logger;

import util.*;

/**
 * Class to write a log, and also to maintain a log message stack for writing to
 * the UI.
 */
public final class ReportBuffer
{
	protected static Logger log = Logger.getLogger(ReportBuffer.class.getPackage().getName());

	/** This class is not instantiated */
	private ReportBuffer()
	{
	}

	/** A buffer for displaying messages in the Log Window.
	 * Such messages are intended to record the progress of the game
	 * and can be used as a game report. */
	private static StringBuffer reportBuffer = new StringBuffer();

	/** Add a message to the log buffer (and display it on the console) */
	public static void add(String message)
	{
		if (Util.hasValue(message))
		{
			reportBuffer.append(message).append("\n");
			/* Also log the message */
			log.info(message);
		}
	}


	/** Get the current log buffer, and clear it */
	public static String get()
	{
		String result = reportBuffer.toString();
		reportBuffer = new StringBuffer();
		return result;
	}

}
