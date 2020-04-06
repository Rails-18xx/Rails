package net.sf.rails.common;

import java.util.Deque;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.game.RailsAbstractItem;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.state.ChangeReporter;
import net.sf.rails.game.state.ChangeSet;
import net.sf.rails.game.state.ChangeStack;
import net.sf.rails.util.Util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;


/**
 * ReportBuffer stores messages of the game progress.
 *
 * Also used for regression testing comparing the output of the report buffer.
 */

public class ReportBuffer extends RailsAbstractItem implements ChangeReporter {

    private static final Logger log = LoggerFactory.getLogger(ReportBuffer.class);

    /** Indicator string to find the active message position in the parsed html document */
    public static final String ACTIVE_MESSAGE_INDICATOR = "(**)";

    // static data
    private final Deque<ReportSet> pastReports = Lists.newLinkedList();
    private final Deque<ReportSet> futureReports = Lists.newLinkedList();

    // TODO: Remove waitQueue, see functions below
    private final Queue<String> waitQueue = Lists.newLinkedList();

    private ChangeStack changeStack; // initialized via init()

    // dynamic data
    private ReportSet.Builder currentReportBuilder;
    private ReportBuffer.Observer observer;


    private ReportBuffer(ReportManager parent, String id) {
        super(parent, id);
        currentReportBuilder = ReportSet.builder();
    }

    public static ReportBuffer create(ReportManager parent, String id) {
        ReportBuffer buffer = new ReportBuffer(parent, id);
        return buffer;
    }

    public void addObserver(ReportBuffer.Observer observer) {
        this.observer = observer;
    }

    public void removeObserver() {
        this.observer = null;
    }

    /**
     * Returns a list of all messages (of the past)
     * @return list of messages
     */
    public ImmutableList<String> getAsList() {
        ImmutableList.Builder<String> list = ImmutableList.builder();
        for (ReportSet rs:pastReports) {
            list.addAll(rs.getAsList());
        }
        return list.build();
    }

    private String getAsHtml(ChangeSet currentChangeSet) {

        // FIXME (Rails2.0): Add comments back
        //     s.append("<span style='color:green;font-size:80%;font-style:italic;'>");

        StringBuilder s = new StringBuilder();
        s.append("<html>");
        for (ReportSet rs:Iterables.concat(pastReports, futureReports)) {
            String text = rs.getAsHtml(currentChangeSet);
            if (text == null) continue;
            s.append("<p>");
            s.append(text);
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

    public String getCurrentText() {
        return getAsHtml(changeStack.getClosedChangeSet());
    }

    private void addMessage(String message) {
        if (!Util.hasValue(message)) return;
        currentReportBuilder.addMessage(message);
        log.debug("ReportBuffer: {}", message);
    }

    private void updateObserver() {
        if (observer != null) {
            observer.update(getCurrentText());
        }
    }

    // ChangeReport methods
    @Override
    public void init(ChangeStack changeStack) {
        this.changeStack = changeStack;
    }

    @Override
    public void updateOnClose() {
        ChangeSet current = changeStack.getClosedChangeSet();
        ReportSet currentSet = currentReportBuilder.build(current);
        pastReports.addLast(currentSet);
        futureReports.clear();

        // a new builder
        currentReportBuilder = ReportSet.builder();

        // update observer (ReportWindow)
        updateObserver();
    }

    @Override
    public void informOnUndo() {
        ReportSet undoSet = pastReports.pollLast();
        futureReports.addFirst(undoSet);
    }

    @Override
    public void informOnRedo() {
        ReportSet redoSet = futureReports.pollFirst();
        pastReports.addLast(redoSet);
    }

    @Override
    public void updateAfterUndoRedo() {
        updateObserver();
    }

    /**
     * Shortcut to add a message to DisplayBuffer
     */
    public static void add(RailsItem item, String message) {
        item.getRoot().getReportManager().getReportBuffer().addMessage(message);
    }

    // FIXME: Rails 2.0 Is it possible to remove the only usecase for 1856 escrow money?
    @Deprecated
    public static void addWaiting (RailsItem item, String message) {
        item.getRoot().getReportManager().getReportBuffer().waitQueue.add(message);
    }

    @Deprecated
    public static void getAllWaiting (RailsItem item) {
        ReportBuffer reportBuffer = item.getRoot().getReportManager().getReportBuffer();
        for (String message : reportBuffer.waitQueue) {
            reportBuffer.addMessage(message);
        }
        reportBuffer.waitQueue.clear();
    }

    public static interface Observer {

        void append(String text);

        void update(String newText);

    }

}
