package rails.game;

import org.apache.log4j.Logger;

import rails.util.*;


/**
 * Class to write a log, and also to maintain a log message stack for writing to
 * the UI.
 */
public final class DisplayBuffer
{

	protected static Logger log = Logger.getLogger(DisplayBuffer.class.getPackage().getName());

	/** This class is not instantiated */
	private DisplayBuffer()
	{
	}

	/** A buffer for displaying messages in a popup window after any user action.
	 * These include error messages and other notifications of immediate interest
	 * to players. */
	private static StringBuffer displayBuffer = new StringBuffer();

	/** Add a message to the message (display) buffer (and display it on the console) */
	public static void add(String message)
	{
		if (Util.hasValue(message))
		{
			displayBuffer.append(message);
			/* Also log the message */
			log.info("Displayed: "+message);
		}
	}

	/** Get the current message buffer, and clear it */
	public static String get()
	{
		String message = displayBuffer.toString();
		displayBuffer = new StringBuffer();
		return message;
	}

}
