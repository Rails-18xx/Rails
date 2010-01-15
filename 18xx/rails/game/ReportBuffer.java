/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/ReportBuffer.java,v 1.8 2010/01/15 19:55:59 evos Exp $ */
package rails.game;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;

import rails.util.Config;
import rails.util.Util;

/**
 * Class to write a log, and also to maintain a log message stack for writing to
 * the UI.
 */
public final class ReportBuffer {
    protected static String reportDirectory = null;
    protected String reportPathname = null;
    protected PrintWriter report = null;
    protected static boolean wantReport = false;
    protected static List<String> initialQueue = new ArrayList<String>();

    private static final String DEFAULT_DTS_PATTERN = "yyyyMMdd_HHmm";
    private static final String DEFAULT_REPORT_EXTENSION = "txt";
    protected static Logger log =
        Logger.getLogger(ReportBuffer.class.getPackage().getName());

    static {
        reportDirectory = Config.get("report.directory");
        wantReport = Util.hasValue(reportDirectory);
    }

    public ReportBuffer() {
    	if (!initialQueue.isEmpty()) {
    		for (String s : initialQueue) {
    			addMessage (s);
    		}
    		initialQueue.clear();
    	}
    }

    /**
     * A buffer for displaying messages in the Log Window. Such messages are
     * intended to record the progress of the rails.game and can be used as a
     * rails.game report.
     */
    private StringBuffer reportBuffer = new StringBuffer();

    /** Add a message to the log buffer (and display it on the console) */
    public static void add(String message) {
    	GameManagerI gm = GameManager.getInstance();
    	ReportBuffer instance = null;
    	if (gm != null) instance = gm.getReportBuffer();
    	if (gm == null || instance == null) {
    		// Queue in a static buffer until the instance is created
    		initialQueue.add(message);
    	} else {
    		instance.addMessage(message);
    	}
    }

    private void addMessage (String message) {
        if (message != null) {
            reportBuffer.append(message).append("\n");
            /* Also log the message */
            if (message.length() > 0) log.info(message);
            /* Also write it to the report file, if requested */
            if (wantReport) writeToReport(message);
        }
    }

    /** Get the current log buffer, and clear it */
    public static String get() {
    	ReportBuffer instance = getInstance();
        String result = instance.reportBuffer.toString();
        instance.reportBuffer = new StringBuffer();
        return result;
    }

    private static ReportBuffer getInstance() {
    	return GameManager.getInstance().getReportBuffer();
    }

    private void writeToReport(String message) {

        /* Get out if we don't want a report */
        if (!Util.hasValue(reportDirectory) || !wantReport) return;

        if (report == null) openReportFile();

        if (wantReport) {
            report.println(message);
            report.flush();
        }
    }

    private void openReportFile() {

        /* Get any configured date/time pattern, or else set the default */
        String reportFilenamePattern =
                Config.get("report.filename.date_time_pattern");
        if (!Util.hasValue(reportFilenamePattern)) {
            reportFilenamePattern = ReportBuffer.DEFAULT_DTS_PATTERN;
        }
        /* Get any configured extension, or else set the default */
        String reportFilenameExtension =
                Config.get("report.filename.extension");
        if (!Util.hasValue(reportFilenameExtension)) {
            reportFilenameExtension = ReportBuffer.DEFAULT_REPORT_EXTENSION;
        }
        /* Create a date formatter */
        DateFormat dateFormat = new SimpleDateFormat(reportFilenamePattern);
        /* Create the pathname */
        reportPathname =
                reportDirectory + "/" + GameManager.getInstance().getGameName() + "_"
                		+ GameManager.getInstance().getGMKey() + "_"
                        + dateFormat.format(new Date()) + "."
                        + reportFilenameExtension;
        log.debug("Report pathname is " + reportPathname);
        /* Open the file */
        /* TODO: check if the directory exists, and if not, create it */
        try {
            report = new PrintWriter(new FileWriter(new File(reportPathname)));
        } catch (IOException e) {
            log.error("Cannot open file " + reportPathname, e);
            wantReport = false;
        }
    }

    /* A stack for messages that must "wait" for other messages */
    private List<String> waitQueue = new ArrayList<String> ();

    public static void addWaiting (String string) {
        getInstance().waitQueue.add (string);
    }

    public static void getAllWaiting () {
    	ReportBuffer instance = getInstance();
        for (String message : instance.waitQueue) {
            instance.addMessage (message);
        }
        instance.waitQueue.clear();
    }

}
