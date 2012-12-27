package rails.game;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import rails.game.state.ChangeActionOwner;
import rails.game.state.ChangeReporter;
import rails.game.state.ChangeSet;
import rails.game.state.ChangeStack;
import rails.ui.swing.AbstractReportWindow;
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
public class ReportBuffer implements ChangeReporter {

    private static final Logger log =
            LoggerFactory.getLogger(ReportBuffer.class);

    /** Indicator string to find the active message position in the parsed html document */
    public static final String ACTIVE_MESSAGE_INDICATOR = "(**)";
    
    /** Newline string
     * &#10; is the linefeed character to induce line feed on copy & paste
     */
    private static final String NEWLINE_STRING = "<br>&#10;";
    
    /** defines the collection of data that is stored in the report buffer */
    private class ReportItem {
        private final ChangeSet changeSet;
        private final List<String> messages;

        private ReportItem(ChangeSet changeSet, List<String> messages) {
            this.changeSet = changeSet;
            this.messages = messages;
        }
        
        private String getMessages(boolean html) {
            StringBuffer s = new StringBuffer();
            for (String message:messages) {
                if (html) {
                    s.append(Util.convertToHtml(message)); 
                } else {
                    s.append(message);
                }
            }
            return s.toString();
        }
        
        private int getIndex() {
            return changeSet.getIndex();
        }
        
        private ChangeActionOwner getOwner() {
            return changeSet.getOwner();
        }
        
        /**
         * converts messages to html string
         * @param activeMessage if true, adds indicator and highlighting for active message
         */
        
        private String toHtml(boolean activeMessage) {
            if (messages.isEmpty()) {
                if (activeMessage) {
                    return ("<span bgcolor=Yellow>" + ACTIVE_MESSAGE_INDICATOR + "</span>"
                            + NEWLINE_STRING);
                } else { 
                    return null;
                }
            }

            StringBuffer s = new StringBuffer();
            boolean init = true;
            for (String message:messages) {
                message = Util.convertToHtml(message);
                if (init) {
                    if (activeMessage) {
                        s.append("<span bgcolor=Yellow>" + ACTIVE_MESSAGE_INDICATOR) ;
                    }
                    s.append("<a href=http://rails:"  + changeSet.getIndex() + ">");
                    s.append(message);
                    s.append("</a>"); 
                    if (activeMessage) {
                        s.append("</span>");
                    }
                    s.append(NEWLINE_STRING);
                    init = false;
                } else {
                    s.append(message + NEWLINE_STRING); // see above
                }
            }
            return s.toString();
        }
        
        public String toText() {
            StringBuffer s = new StringBuffer();
            for (String message:messages) {
                s.append(message + "\n");
            }
            return s.toString();
        }
        
        
        public String toString() {
            StringBuffer s = new StringBuffer();
            s.append("ReportItem for ChangeSet = " + changeSet);
            s.append(", messages = "); s.append(getMessages(false));
            return s.toString();
        }

    }

    private final List<String> waitQueue = Lists.newArrayList();
    private final List<ReportItem> reportItems = Lists.newArrayList();

    private ChangeStack changeStack;
    private ImmutableList.Builder<String> current = ImmutableList.builder();
    
    private AbstractReportWindow reportWindow;
    
    public ReportBuffer() {}
    
    public void setChangeStack(ChangeStack changeStack) {
        this.changeStack = changeStack;
    }

    public int getCurrentIndex() {
        return changeStack.getCurrentIndex();
    }
    
    public int getLastIndex() {
        return changeStack.getMaximumIndex();
    }
    
    /** Creates a new report item */
    public void close(ChangeSet changeSet) {
        ReportItem newItem = new ReportItem(changeSet, current.build());
        log.debug("Creation of reportItem: " + newItem);
        reportItems.add(changeSet.getIndex(), newItem);
        current = ImmutableList.builder();
   }

    public void addMessage(String message) {
        if (message == null || message.equals("")) return;
        // log message
        if (message.length() > 0) log.info(message);
        current.add(message);
    }

    public List<String> getReportQueue() {
        ImmutableList.Builder<String> queue = ImmutableList.builder();
        for (ReportItem item:reportItems) {
            queue.addAll(item.messages);
        }
        return queue.build();
    }
    
    
    /**
     * returns the latest report items
     */
    public String getLatestReportItems(){
        // search for a change of the player
        ChangeActionOwner currentPlayer = null;
        int firstActionIndex = 0;
        for (ReportItem item:reportItems) {
            if (item.getOwner() != currentPlayer) {
                currentPlayer = item.getOwner();
                firstActionIndex = item.getIndex();
            }
        }
        
        // start with that index and connect data
        StringBuffer s = new StringBuffer();
        for(ReportItem item:reportItems.subList(firstActionIndex, getCurrentIndex())) {
            String text = item.toText();
            // text afterwards
            if (text != null) s.append(text);
        } 
        return s.toString();
    }
    
    public String getReportItems() {
        
        StringBuffer s = new StringBuffer();
        s.append("<html>");
        for (int i=0; i < changeStack.getMaximumIndex(); i++) {
            ReportItem item = reportItems.get(i);
            // active Index is the one before the current
            String text = item.toHtml(i == changeStack.getCurrentIndex() - 1 );
            if (text == null) continue;
            s.append("<p>").append(text).append("</p>");
        }
        s.append("</html>");
        
        return s.toString();
    }

    public void registerReportWindow(AbstractReportWindow reportWindow) {
        this.reportWindow = reportWindow; 
        update();
    }
 
    public void update() {
        if (reportWindow != null) {
            reportWindow.update(getReportItems());
        }
    }

    /** Add a message to the log buffer (and display it on the console) */
    public static void add(RailsItem item, String message) {
//        // ignore undo and redo for the new reportItems
//        if (!message.equals(LocalText.getText("UNDO")) && !message.equals(LocalText.getText("REDO"))) {
//            instance.addMessage(message);
//        }
        item.getRoot().getReportBuffer().addMessage(message);
    }

    // TODO: Is it possible to remove the only usecase for 1856 escrow money?
    @Deprecated
    public static void addWaiting (RailsItem item, String message) {
        item.getRoot().getReportBuffer().waitQueue.add(message);
    }

    @Deprecated
    public static void getAllWaiting (RailsItem item) {
        ReportBuffer reportBuffer = item.getRoot().getReportBuffer();
        for (String message : reportBuffer.waitQueue) {
            reportBuffer.addMessage(message);
        }
        reportBuffer.waitQueue.clear();
    }

}
