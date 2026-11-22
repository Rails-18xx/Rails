package net.sf.rails.ui.swing.elements;

// Keep Observer import
import net.sf.rails.game.state.Observer;
// Keep Model import if Field constructor uses it, but Observable is safer
import net.sf.rails.game.state.Model;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Keep Observable import
import net.sf.rails.game.state.Observable;

/**
 * A specialized Field that displays an Integer value (representing seconds)
 * formatted as MM:SS.
 */
// Keep 'implements Observer' if Field doesn't already, otherwise remove
public class TimeField extends Field implements Observer {

    private static final Logger log = LoggerFactory.getLogger(TimeField.class);
    private final Observable observedModel; // Store the model reference

    /**
     * Constructor for TimeField. Accepts any Observable.
     * @param model The Observable model representing time in seconds (e.g., an IntegerState).
     */
    // *** CHANGE constructor parameter type to Observable ***
    public TimeField(Observable model) {
        super(model); // Call the superclass Field constructor (assuming it accepts Observable)
        this.observedModel = model; // Store the model

        // Immediately format the initial value
        SwingUtilities.invokeLater(() -> {
            if (model != null && model.toText() != null) {
                setText(model.toText());
            } else {
                setText("0"); // Default to 0 seconds
            }
        });
    }

    // ... formatTime method remains the same ...
    private String formatTime(String secondsStr) {
        try {
            if (secondsStr != null && secondsStr.contains(":")) {
                return secondsStr;
            }
            int totalSeconds = Integer.parseInt(secondsStr);
            boolean isNegative = totalSeconds < 0;
            totalSeconds = Math.abs(totalSeconds);
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format("%s%d:%02d", (isNegative ? "-" : ""), minutes, seconds);
        } catch (NumberFormatException | NullPointerException e) {
           // log.warn("Could not parse '{}' as seconds for formatting.", secondsStr, e);
            return (secondsStr != null) ? secondsStr : "0:00";
        }
    }


    // ... overridden setText method remains the same ...
    @Override
    public void setText(String text) {
        super.setText(formatTime(text));
    }


    // --- Observer Interface Methods ---
    // Re-add update, getObservable, deRegister if Field doesn't implement Observer

    /**
     * Called by the observed Observable when the value changes.
     * Ensures the update happens on the Swing Event Dispatch Thread.
     * @param text The new time value in seconds, as a String.
     */
    @Override
    public void update(String text) {
        SwingUtilities.invokeLater(() -> {
            log.trace("TimeField.update received raw text: {}", text);
            // Calling setText triggers our overridden method with formatting
            setText(text);
        });
    }

    @Override
    public Observable getObservable() {
        return this.observedModel;
    }


}