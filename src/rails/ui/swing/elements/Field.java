package rails.ui.swing.elements;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import com.google.common.base.Objects;

import rails.game.state.Observable;
import rails.game.state.Observer;

// TODO: Replace ViewObject with Observer mechanisms
// TODO: Make the color and font options work again
public class Field extends JLabel implements Observer {

    private static final long serialVersionUID = 1L;

    private Border labelBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);

    private static final Color NORMAL_BG_COLOUR = Color.WHITE;

    private static final Color HIGHLIGHT_BG_COLOUR = new Color(255, 255, 80);

    private Observable observable;
    private Color normalBgColour = NORMAL_BG_COLOUR;

    private List<JComponent> dependents = null;

    private boolean pull = false;

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

    public Field(Observable modelObject) {
        this("");
        this.observable = modelObject;
        modelObject.addObserver(this);
        setText(modelObject.toText());
    }

    public Field(Observable modelObject, boolean pull) {
        this(modelObject);
        this.pull = pull;
    }

    public Field(Observable modelObject, ImageIcon icon, int position) {
        this(modelObject);
        setIcon(icon);
        setHorizontalAlignment(position);
    }

    public Observable getModel() {
        return observable;
    }


    public void setHighlight(boolean highlight) {
        setBackground(highlight ? HIGHLIGHT_BG_COLOUR : normalBgColour);
    }

    /** This method is mainly needed when NOT using the Observer pattern. */

    @Override
    public void paintComponent(Graphics g) {
        if (observable != null && pull) {
            setText(observable.toText());
        }
        super.paintComponent(g);
    }

    // FIXME: ViewUpdate has to be rewritten in the new structure
/*    protected void updateDetails (ViewUpdate vu) {
        for (String key : vu.getKeys()) {
            if (ViewUpdate.TEXT.equalsIgnoreCase(key)) {
                setText (vu.getText());
            } else if (ViewUpdate.BGCOLOUR.equalsIgnoreCase(key)) {
                setBackground((Color)vu.getValue(key));
                   normalBgColour = getBackground();
                   setForeground (Util.isDark(normalBgColour) ? Color.WHITE : Color.BLACK);
            }
        }
    }
        */

    public void addDependent (JComponent dependent) {
        if (dependents == null) dependents = new ArrayList<JComponent>(2);
        dependents.add(dependent);
    }

    public List<JComponent> getDependents () {
        return dependents;
    }

    /** Needed to satisfy the Observer interface. */
    public void update(String text) {
        setText(text);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("observable", observable.getId()).toString();
    }

}
