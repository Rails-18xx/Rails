/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/ReportBuffer.java,v 1.2 2007/10/05 22:02:27 evos Exp $ */
package rails.game;

import java.io.*;
import java.text.*;
import java.util.Date;

import org.apache.log4j.Logger;

import rails.util.*;


/**
 * Class to write a log, and also to maintain a log message stack for writing to
 * the UI.
 */
public final class ReportBuffer
{
	protected static Logger log = Logger.getLogger(ReportBuffer.class.getPackage().getName());
	protected static String reportDirectory = null;
	protected static String reportPathname = null;
	protected static PrintWriter report = null;
	protected static boolean wantReport = false;
	
	static {
		reportDirectory = Config.get("report.directory");
		wantReport = Util.hasValue(reportDirectory);
	}

	/** This class is not instantiated */
	private ReportBuffer()
	{
	}

	/** A buffer for displaying messages in the Log Window.
	 * Such messages are intended to record the progress of the rails.game
	 * and can be used as a rails.game report. */
	private static StringBuffer reportBuffer = new StringBuffer();

	/** Add a message to the log buffer (and display it on the console) */
	public static void add(String message)
	{
		if (Util.hasValue(message))
		{
			reportBuffer.append(message).append("\n");
			/* Also log the message */
			log.info(message);
			/* Also write it to the report file, if requested */
			if (wantReport) writeToReport (message);
		}
	}


	/** Get the current log buffer, and clear it */
	public static String get()
	{
		String result = reportBuffer.toString();
		reportBuffer = new StringBuffer();
		return result;
	}
	
	private static void writeToReport (String message) {
		
		/* Get out if we don't want a report */
		if (!Util.hasValue(reportDirectory) || !wantReport) return;
		
		if (report == null) openReportFile();
		
		if (wantReport) {
			report.println(message);
			report.flush();
		}
	}
	
	private static void openReportFile () {

	    /* Get any configured date/time pattern, or else set the default */
	    String reportFilenamePattern = Config.get("report.filename.date_time_pattern");
	    if (!Util.hasValue(reportFilenamePattern)) {
	        reportFilenamePattern = "yyyyMMdd_HHmm";
	    }
	    /* Get any configured extension, or else set the default */
	    String reportFilenameExtension = Config.get("report.filename.extension");
	    if (!Util.hasValue(reportFilenameExtension)) {
	        reportFilenameExtension = "txt";
	    }
	    /* Create a date formatter */
		DateFormat dateFormat = new SimpleDateFormat (reportFilenamePattern);
		/* Create the pathname */
		reportPathname = reportDirectory + "/"
			+ Game.getName() + "_"
			+ dateFormat.format(new Date()) +"."+ reportFilenameExtension;
		log.debug("Report pathname is "+reportPathname);
		/* Open the file */
		/* TODO: check if the directory exists, and if not, create it */
		try {
			report = new PrintWriter (new FileWriter (new File (reportPathname)));
		} catch (IOException e) {
			log.error ("Cannot open file "+reportPathname, e);
			wantReport = false;
		}
	}

}
