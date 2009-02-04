/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/DisplayBuffer.java,v 1.6 2009/02/04 20:36:39 evos Exp $ */
package rails.game;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rails.util.*;

/**
 * Class to write a log, and also to maintain a log message stack for writing to
 * the UI.
 */
public final class DisplayBuffer {

    protected static Logger log =
            Logger.getLogger(DisplayBuffer.class.getPackage().getName());

    /** This class is not instantiated */
    private DisplayBuffer() {}

    /**
     * A buffer for displaying messages in a popup window after any user action.
     * These include error messages and other notifications of immediate
     * interest to players.
     */
    private static List<String> displayBuffer = new ArrayList<String>();
    
    private static boolean autoDisplay = true;

    /**
     * Add a message to the message (display) buffer (and display it on the
     * console)
     */
    public static void add(String message) {
        add (message, true);
    }
    
    public static void add (String message, boolean autoDisplay) {
        DisplayBuffer.autoDisplay = autoDisplay;
        if (Util.hasValue(message)) {
            displayBuffer.add(message);
            /* Also log the message */
            log.info("To display: " + message);
        }
    }

    /** Get the current message buffer, and clear it */
    public static String[] get() {
        if (displayBuffer.size() > 0) {
            String[] message = (String[]) displayBuffer.toArray(new String[0]);
            displayBuffer.clear();
            return message;
        } else {
            return null;
        }
    }
    
    public static int getSize() {
        return displayBuffer.size();
    }
    
    public static boolean getAutoDisplay () {
        return autoDisplay;
    }

    public static void clear() {
        displayBuffer.clear();
    }

}
