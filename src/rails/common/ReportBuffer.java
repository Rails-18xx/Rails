package rails.common;


import java.util.List;

import com.google.common.collect.ImmutableList;

import rails.game.RailsItem;
import rails.game.model.RailsModel;
import rails.game.state.ArrayListState;
import rails.game.state.ChangeSet;
import rails.game.state.ChangeStack;
import rails.util.Util;

/**
 * ReportBuffer stores messages of the game progress.
 *
 * Also used for regression testing comparing the output of the report buffer.
 */
// FIXME (Rails2.0): Is an automatic reportFile still used?
// Check in previous commits for an implementation
// FIXME (Rails2.0): Replace the CommentItems by an action based implementation

public class ReportBuffer extends RailsModel implements ChangeStack.Observer {
    
    /** Indicator string to find the active message position in the parsed html document */
    public static final String ACTIVE_MESSAGE_INDICATOR = "(**)";
    
    private final ArrayListState<ReportSet> reportSets = 
            ArrayListState.create(this, "reportSets");
    
    private ReportSet currentSet;

    private ReportBuffer(ReportManager parent, String id) {
        super(parent, id);
    }
    
    public static ReportBuffer create(ReportManager parent, String id) {
        ReportBuffer buffer = new ReportBuffer(parent, id);
        ChangeSet initialCS = buffer.getRoot().getStateManager().getChangeStack().getCurrentChangeSet();
        buffer.nextReportSet(initialCS);
        return buffer;
    }
    
    private void nextReportSet(ChangeSet changeSet) {
        currentSet = new ReportSet(changeSet);
        reportSets.add(currentSet);
    }
    
    /**
     * Returns a full list of all messages
     * @return list of messages
     */
    public List<String> getAsList() {
        ImmutableList.Builder<String> list = ImmutableList.builder();
        for (ReportSet rs:reportSets) {
            list.addAll(rs.getAsList());
        }
        return list.build();
    }
    
    /**
     * Returns the current ReportSet as PlainText
     * @return full text
     */
    public String getCurrent() {
        return currentSet.toText();
    }
    
    /**
     * Returns the full text of ReportBuffer as plain text
     */
    public String getAsText() {
        StringBuilder text = new StringBuilder();
        for (ReportSet rs:reportSets) {
                text.append(rs.toText());
        }
        return text.toString();
    }
    
    /**
     * Returns the full text of ReportBuffer in html
     * @param active changeSet that is highlighted as active
     */
    public String getAsHtml(ChangeSet active) {
        StringBuilder s = new StringBuilder();
        s.append("<html>");
        for (ReportSet rs:reportSets) {
            String text = rs.toHtml(rs.getChangeSet() == active);
            if (text == null) continue;
            s.append("<p>");
            // FIXME (Rails2.0): Add commments back
            //     s.append("<span style='color:green;font-size:80%;font-style:italic;'>");
            if (text != null) s.append(text);
            s.append("</p>");
        }
        s.append("</html>");
        
        return s.toString();
    }
    
    /**
     * Returns all messages for the recent active player
     * @return full text
     */
    // FIXME (Rails2.0): Add implementation for this
    public String getRecentPlayer() {
        return null;
    }

    private void addMessage(String message) {
        if (!Util.hasValue(message)) return;
        currentSet.addMessage(message);
    }
    
    // ChangeStack observer methods
    public void changeSetCreated(ChangeSet createdCS) {
        nextReportSet(createdCS);
    }
    
    @Override
    public String toText() {
        ChangeSet active = getRoot().getStateManager().getChangeStack().getCurrentChangeSet();
        return getAsHtml(active);
    }
    

    /**
     * Shortcut to add a message to DisplayBuffer
     */
    public static void add(RailsItem item, String message) {
        item.getRoot().getReportManager().getReportBuffer().addMessage(message);
    }
    
}
