package net.sf.rails.ui.swing.elements;

import com.google.common.base.MoreObjects;
import net.sf.rails.game.model.ColorModel;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Observer;

// [ADD 1] Import SLF4J Logger classes
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;


// TODO: Make the color and font options work again
public class Field extends JLabel implements Observer {

    private static final long serialVersionUID = 1L;

    // [ADD 2] Add a static logger instance
    private static final Logger log = LoggerFactory.getLogger(Field.class);


    private Border labelBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);

    private static final Color NORMAL_BG_COLOUR = Color.WHITE;
    private static final Color HIGHLIGHT_BG_COLOUR = new Color(255, 255, 80);

    private Observable observable;
    private Observer toolTipObserver;
    private Observer colorObserver;
    private Color normalBgColour = NORMAL_BG_COLOUR;

    private boolean pull = false;
    private boolean html = false;

    public Field(String text) {
        super(text.equals("0%") ? "" : text);
        this.setBackground(NORMAL_BG_COLOUR);
        this.setHorizontalAlignment(SwingConstants.CENTER);
        this.setBorder(labelBorder);
        this.setOpaque(true);
    }

    public Field(ImageIcon info) {
        super(info);
        this.setBackground(NORMAL_BG_COLOUR);
        this.setHorizontalAlignment(SwingConstants.CENTER);
        this.setBorder(labelBorder);
        this.setOpaque(true);
    }

    // TODO: Remove the pull option
    public Field(Observable observable, boolean html, boolean pull) {
        this(""); // create empty field first
        this.observable = observable;
        this.html = html;
        // Check if observable is not null before adding observer
        if (this.observable != null) {
            this.observable.addObserver(this);
            // initialize text
            this.setText(observable.toText());
        } else {
             log.warn("Field created with a null Observable!"); // Log warning if model is null
        }
        this.pull = pull;
    }

    public Field(Observable observable) {
        this(observable, false, false);
    }

    public Field(Observable observable, ImageIcon icon, int position) {
        this(observable);
        setIcon(icon);
        setHorizontalAlignment(position);
    }

    public void setToolTipModel(Observable toolTipModel) {
        final Observable storeModel = toolTipModel;
        toolTipObserver = new Observer() {
            @Override
            public void update(String text) {
                setToolTipText(text);
            }

            @Override
            public Observable getObservable() {
                return storeModel;
            }
        };
        // Check if toolTipModel is not null
        if (toolTipModel != null) {
            toolTipModel.addObserver(toolTipObserver);
            // initialize toolTip
            setToolTipText(toolTipModel.toText());
        } else {
             log.warn("setToolTipModel called with a null Observable!");
        }
    }

    public void setColorModel(ColorModel colorModel) {
        final ColorModel storeModel = colorModel;
        colorObserver = new Observer() {
            @Override
            public void update(String text) {
                if (storeModel.getBackground() != null) {
                    setBackground(storeModel.getBackground());
                } else {
                    setBackground(NORMAL_BG_COLOUR);
                }
                if (storeModel.getForeground() != null) {
                    setForeground(storeModel.getForeground());
                }
            }

            @Override
            public Observable getObservable() {
                return storeModel;
            }
        };
        // Check if colorModel is not null
        if (colorModel != null) {
            colorModel.addObserver(colorObserver);
            colorObserver.update(null); // Initialize color
        } else {
            log.warn("setColorModel called with a null ColorModel!");
        }
    }

    public void setHighlight(boolean highlight) {
        setBackground(highlight ? HIGHLIGHT_BG_COLOUR : normalBgColour);
    }

    /**
     * This method is mainly needed when NOT using the Observer pattern.
     */
    @Override
    public void paintComponent(Graphics g) {
        if (observable != null && pull) {
            setText(observable.toText());
        }
        super.paintComponent(g);
    }

    @Override
    public void setText(String text) {
        String effectiveText = (text == null) ? "" : text; // Handle null input
        if (html) {
            super.setText("<html>" + effectiveText + "</html>");
        } else {
            super.setText(effectiveText);
        }
    }

    public void setHtml() {
        html = true;
    }

    // FIXME: ViewUpdate has to be rewritten in the new structure
/* protected void updateDetails (ViewUpdate vu) {
    @Override
    public void setText (String text) {
        if (html) {
            super.setText("<html>" + text + "</html>");
        } else {
            super.setText(text);
        }
    }
        // ... (removed old commented code for brevity) ...
    }
        */

    // Observer methods
    // [ADD 3] Add logging inside this update method
// Observer methods
    @Override
    public void update(String text) {
        // --- START COMBINED DEBUG LOGGING ---
        String observableId = (observable != null) ? observable.getId() : "UnknownObservable";
        String receivedText = (text != null) ? "'" + text + "'" : "null"; // Show null clearly

        // Log the received text BEFORE calling setText
        log.debug("Field.update for '{}' RECEIVED text: {}", observableId, receivedText);
        // --- END COMBINED DEBUG LOGGING ---

        // Original logic: just set the text
        setText(text); // Pass the potentially null text to setText which handles it

        // --- Check AFTERWARDS ---
        String currentLabelText = getText();
        // Simplified check for blankness
        boolean isBlank = (currentLabelText == null || currentLabelText.isEmpty() || (html && currentLabelText.equals("<html></html>")));

        if (isBlank) {
       //     log.warn("  - Field '{}' appears BLANK after update with input: {}", observableId, receivedText);
        } else {
       //      log.debug("  - Field '{}' text set to: '{}'", observableId, currentLabelText);
        }
        // --- END Check ---
    }

    
    @Override
    public Observable getObservable() {
        return observable;
    }

    @Override
    public String toString() {
        String observableId = (observable != null) ? observable.getId() : "null";
        return MoreObjects.toStringHelper(this)
                .add("observableId", observableId) // Use getId for clarity
                .toString();
    }

}