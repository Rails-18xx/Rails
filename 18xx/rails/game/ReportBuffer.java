/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/ReportBuffer.java,v 1.10 2010/03/23 18:45:15 stefanfrey Exp $ */
package rails.game;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;

import rails.util.Config;
import rails.util.LocalText;
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
    
    /** defines the collection of data that is stored in the report buffer */
    private class ReportItem {
        private List<String> messages = new ArrayList<String>();
        private int index = 0;
        private Player player = null;
        private RoundI round = null;
      
        private void addMessage(String message) {
            // ignore undos and redos
            messages.add(message);
        }
        
        private String getMessages() {
            StringBuffer s = new StringBuffer();
            for (String message:messages) {
                s.append(message);
            }
            return s.toString();
        }
        
        private String toHtml() {
            StringBuffer s = new StringBuffer();
            boolean init = true;
            for (String message:messages) {
                if (init) {
                    s.append("<a href=http://rails:"  + index + ">");
                    s.append(message);
                    s.append("</a><br>");
                    init = false;
                } else {
                    s.append(message + "<br>");
                }
            }
            return s.toString();
        }
        
        public String toString() {
            StringBuffer s = new StringBuffer();
            s.append("ReportItem for MoveStackIndex = " + index);
            s.append(", player = " + player);
            s.append(", round = " + round);
            s.append(", messages = "); s.append(getMessages());
            return s.toString();
        }
    }

    
    /**
     * A stack for displaying messages in the Log Window. Such messages are
     * intended to record the progress of the rails.game and can be used as a
     * rails.game report.
     */
    private List<String> reportQueue = new ArrayList<String> ();

    /** Another stack for messages that must "wait" for other messages */
    private List<String> waitQueue = new ArrayList<String> ();

    /** Archive stack, the integer index corresponds with the moveset items */
    private SortedMap<Integer, ReportItem> reportItems = new TreeMap<Integer, ReportItem>();
    /** Indicator string to find the active message position in the parsed html document */
    public static final String ACTIVE_MESSAGE_INDICATOR = "(**)"; 
    
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
        reportItems.put(0, new ReportItem());
        if (!initialQueue.isEmpty()) {
            for (String s : initialQueue) {
                addMessage(s, -1); // start of the game
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

    private void addMessage(String message, int moveStackIndex) {
        if (message != null) {
            if (message.equals("")) {
                message = "---"; // workaround for testing
            }
            // legacy report queue
            reportQueue.add(message);
            // new queue
            reportItems.get(moveStackIndex).addMessage(message);
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
    
    private void addReportItem(int index, Player player, RoundI round) {
        ReportItem newItem = new ReportItem();
        newItem.index = index;
        newItem.player = player;
        newItem.round = round;
        reportItems.put(index, newItem);
        Set<Integer> deleteIndices = new HashSet<Integer>
            (reportItems.tailMap(index + 1).keySet());
        for (Integer i:deleteIndices) {
            reportItems.remove(i);
        }
    }

    /** Movestack calls the report item to update */
    public static void createNewReportItem(int index) {
        // check availablity
        GameManagerI gm = GameManager.getInstance();
        ReportBuffer instance = null;
        if (gm != null) {
            instance = gm.getReportBuffer();
        }
        if (gm == null || instance == null) {
            return;
        }
        // all there, add new report item
        Player player = gm.getCurrentPlayer();
        RoundI round = gm.getCurrentRound();
        instance.addReportItem(index, player, round);
    }

    
    public static String getReportItems() {
        int index = GameManager.getInstance().getMoveStack().getIndex();
        ReportBuffer instance = getInstance();
        
        StringBuffer s = new StringBuffer();
        s.append("<html>");
        for (ReportItem item:instance.reportItems.values()) {
            if (item.index == index-1) {
                s.append("<p bgcolor=Yellow>" + ACTIVE_MESSAGE_INDICATOR) ;
            }
            s.append(item.toHtml());
            if (item.index == (index-1)) {
                s.append("</p><");
            }
        }
        s.append("</html>");
        
        return s.toString();
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
        if (gm != null) {
            instance = gm.getReportBuffer();
        }
        if (instance == null) {
            // Queue in a static buffer until the instance is created
            initialQueue.add(message);
        } else {
            // ignore undo and redo for the new reportItems
            if (message.equals(LocalText.getText("UNDO")) || message.equals(LocalText.getText("REDO"))) {
                instance.reportQueue.add(message);
                return;
            }
            int moveStackIndex = gm.getMoveStack().getIndex();
            instance.addMessage(message, moveStackIndex);
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

    public static void addWaiting (String message) {
        getInstance().waitQueue.add(message);
    }

    public static void getAllWaiting () {
        ReportBuffer instance = getInstance();
        for (String message : instance.waitQueue) {
            add(message);
        }
        instance.waitQueue.clear();
    }


}
