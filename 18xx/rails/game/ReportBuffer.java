/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/ReportBuffer.java,v 1.10 2010/03/23 18:45:15 stefanfrey Exp $ */
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
 *
 * Each gameManager has one unique ReportBuffer, which is used by the public static methods.
 * Messages before the creation of that buffer are kept in an internal initial queue.
 *
 * Also used for regression testing comparing the output of the report buffer.
 *
 */
public final class ReportBuffer {
    /**
     * A stack for displaying messages in the Log Window. Such messages are
     * intended to record the progress of the rails.game and can be used as a
     * rails.game report.
     */
    private List<String> reportQueue = new ArrayList<String>();

    /** Another stack for messages that must "wait" for other messages */
    private List<String> waitQueue = new ArrayList<String> ();

    private String reportPathname = null;
    private PrintWriter report = null;

    /** Initial queue for all messages, before the ReportBuffer for the GameManager is created */
    private static List<String> initialQueue = new ArrayList<String>();

    private static boolean wantReport = false;
    private static String reportDirectory = null;
    private static final String DEFAULT_DTS_PATTERN = "yyyyMMdd_HHmm";
    private static final String DEFAULT_REPORT_EXTENSION = "txt";

    static {
        reportDirectory = Config.get("report.directory").trim();
        wantReport = Util.hasValue(reportDirectory);
    }

    private static Logger log =
        Logger.getLogger(ReportBuffer.class.getPackage().getName());


    public ReportBuffer() {
        if (!initialQueue.isEmpty()) {
            for (String s : initialQueue) {
                addMessage (s);
            }
            initialQueue.clear();
        }
    }


    private List<String> getReportQueue() {
        return reportQueue;
    }

    private void clearReportQueue() {
        reportQueue.clear();
    }

    private void addMessage (String message) {
        if (message != null) {
            if (message.equals(""))
                message = "---"; // workaround for testing
            reportQueue.add(message);
            /* Also log the message */
            if (message.length() > 0) log.info(message);
            /* Also write it to the report file, if requested */
            if (wantReport) writeToReport(message);
        }
    }

    private void writeToReport(String message) {

        /* Get out if we don't want a report */
        if (!wantReport) return;

        if (report == null) openReportFile();

        if (wantReport) {
            report.println(message);
            report.flush();
        }
    }

    private void openReportFile() {

        /* Get out if we don't want a report */
        if (!wantReport) return;

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


    /** Get the current log buffer, and clear it */
    public static String get() {
        ReportBuffer instance = getInstance();

        // convert to String
        StringBuffer result = new StringBuffer();
        for (String msg:instance.getReportQueue())
          result.append(msg).append("\n");

        // clear current queue
        instance.clearReportQueue();

        return result.toString();
    }

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

   /** return the current buffer as list */
    public static List<String> getAsList() {
        ReportBuffer instance = getInstance();

        if (instance == null)
            return initialQueue;
        else
            return instance.getReportQueue();
    }

    /** clear the current buffer */
    public static void clear() {
        ReportBuffer instance = getInstance();

        if (instance == null)
            initialQueue.clear();
        else
            instance.clearReportQueue();
    }

    private static ReportBuffer getInstance() {
        return GameManager.getInstance().getReportBuffer();
    }



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
