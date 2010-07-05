/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/DisplayBuffer.java,v 1.9 2010/01/31 22:22:28 macfreek Exp $ */
package rails.game;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rails.util.Util;

/**
 * Class to write a log, and also to maintain a log message stack for writing to
 * the UI.
 */
public final class DisplayBuffer {

    /** List to catch messages before the buffer is instantiated,
     * based on the supposition that never 2 games will be initialised simultaneously...
     */
    protected static List<String> initialQueue = new ArrayList<String>();

    protected static Logger log =
            Logger.getLogger(DisplayBuffer.class.getPackage().getName());

    public DisplayBuffer() {
        if (!initialQueue.isEmpty()) {
            for (String s : initialQueue) {
                addMessage (s, true);
            }
            initialQueue.clear();
        }
    }

    /**
     * A buffer for displaying messages in a popup window after any user action.
     * These include error messages and other notifications of immediate
     * interest to players.
     */
    private List<String> displayBuffer = new ArrayList<String>();

    private boolean autoDisplay = true;

    /**
     * Add a message to the message (display) buffer (and display it on the
     * console)
     */
    public static void add(String message) {
        add (message, true);
    }

    public static void add(String message, boolean autoDisplay) {
        GameManagerI gm = GameManager.getInstance();
        DisplayBuffer instance = null;
        if (gm != null) instance = gm.getDisplayBuffer();
        if (gm == null || instance == null) {
            // Queue in a static buffer until the instance is created
            initialQueue.add(message);
        } else {
            instance.addMessage(message, autoDisplay);
        }
    }

    private void addMessage (String message, boolean autoDisplay) {
        DisplayBuffer instance = getInstance();
        instance.autoDisplay = autoDisplay;
        if (Util.hasValue(message)) {
            instance.displayBuffer.add(message);
            /* Also log the message (don't remove this,
             * otherwise the message will not be logged during a reload,
             * which may hinder troubleshooting) */
            log.debug("To display: " + message);
        }
    }

    private static DisplayBuffer getInstance() {
        GameManagerI gm = GameManager.getInstance();
        if (gm == null) {
            return null;
        } else {
            return gm.getDisplayBuffer();
        }
    }

    /** Get the current message buffer, and clear it */
    public static String[] get() {
        DisplayBuffer instance = getInstance();
        if (instance == null) {
            if (initialQueue.isEmpty()) {
                return null;
            } else {
                String[] message = initialQueue.toArray(new String[0]);
                initialQueue.clear();
                return message;
            }
        } else if (instance.displayBuffer.size() > 0) {
            String[] message = instance.displayBuffer.toArray(new String[0]);
            instance.displayBuffer.clear();
            return message;
        } else {
            return null;
        }
    }

    public static int getSize() {
        return getInstance().displayBuffer.size();
    }

    public static boolean getAutoDisplay () {
        return getInstance().autoDisplay;
    }

    public static void clear() {
        getInstance().displayBuffer.clear();
    }

}
