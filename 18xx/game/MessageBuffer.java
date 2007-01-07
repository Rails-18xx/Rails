package game;

import util.*;

/**
 * Class to write a log, and also to maintain a log message stack for writing to
 * the UI.
 */
public final class MessageBuffer
{

	/** This class is not instantiated */
	private MessageBuffer()
	{
	}

	/** A buffer for displaying messages in a popup window after any user action.
	 * These include error messages and other notifications of immediate interest
	 * to players. */
	private static StringBuffer messageBuffer = new StringBuffer();

	/** Add a message to the message (display) buffer (and display it on the console) */
	public static void add(String message)
	{
		if (Util.hasValue(message))
		{
			System.out.println("ADD: "+message);
			messageBuffer.append(message);
		}
	}

	/** Get the current message buffer, and clear it */
	public static String get()
	{
		String message = messageBuffer.toString();
		System.out.println("GET: "+message);
		messageBuffer = new StringBuffer();
		return message;
	}

}
