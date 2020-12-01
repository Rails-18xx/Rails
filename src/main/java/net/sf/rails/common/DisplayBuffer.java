package net.sf.rails.common;

import net.sf.rails.game.RailsItem;
import net.sf.rails.game.model.RailsModel;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;


/**
 * DisplayBuffer stores messages of the current action.
 */
public class DisplayBuffer extends RailsModel {

    private static final Logger log = LoggerFactory.getLogger(DisplayBuffer.class);

    private final ArrayList<String> buffer = new ArrayList<>();

    private final BooleanState autoDisplay = new BooleanState(this, "autoDisplay");

    public DisplayBuffer(ReportManager parent, String id) {
        super(parent, id);
    }

    @Override
    public ReportManager getParent() {
        return (ReportManager) super.getParent();
    }

    /**
     * Add a message to DisplayBuffer
     */
    public void add(String message) {
        add(message, true);
    }

    /**
     * Add a message to DisplayBuffer
     */
    // TODO (Rails2.0): What is the purpose of autoDisplay
    public void add(String message, boolean autoDisplay) {
        this.autoDisplay.set(autoDisplay);
        if (Util.hasValue(message)) {
            buffer.add(message);
            log.debug("To display: {}", message);
        }
    }

    /**
     * Get the current message buffer, and clear it
     */
    // TODO: (Rails2.0): Refactor this a little bit (use Model facilities)
    public String[] get() {
        if (buffer.size() > 0) {
            String[] message = buffer.toArray(new String[0]);
            buffer.clear();
            return message;
        } else {
            return null;
        }
    }

    public int getSize() {
        return buffer.size();
    }

    public boolean getAutoDisplay() {
        return autoDisplay.value();
    }

    public void clear() {
        buffer.clear();
    }

    /**
     * Shortcut to add a message to DisplayBuffer
     */
    public static void add(RailsItem item, String message) {
        item.getRoot().getReportManager().getDisplayBuffer().add(message, true);
    }

    /**
     * Shortcut to add a message to DisplayBuffer
     */
    public static void add(RailsItem item, String message, boolean autoDisplay) {
        item.getRoot().getReportManager().getDisplayBuffer().add(message, autoDisplay);
    }

    public static void addAll (RailsItem item, String[] messages) {
        DisplayBuffer instance = item.getRoot().getReportManager().getDisplayBuffer();
        for (String message : messages) {
            instance.add (item, message, true);
        }
    }

    public static String[] copyAll (RailsItem item) {
        DisplayBuffer instance = item.getRoot().getReportManager().getDisplayBuffer();
        return instance.get();
    }
}
