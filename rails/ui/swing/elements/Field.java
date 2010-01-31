/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/Field.java,v 1.11 2010/01/31 22:22:34 macfreek Exp $*/
package rails.ui.swing.elements;

import java.awt.Color;
import java.awt.Graphics;
import java.util.*;

import javax.swing.*;
import javax.swing.border.Border;

import rails.game.model.ModelObject;
import rails.game.model.ViewUpdate;
import rails.util.Util;

public class Field extends JLabel implements ViewObject {

    private static final long serialVersionUID = 1L;

    private Border labelBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);

    private static final Color NORMAL_BG_COLOUR = Color.WHITE;

    private static final Color HIGHLIGHT_BG_COLOUR = new Color(255, 255, 80);

    private ModelObject modelObject;
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

    public Field(ModelObject modelObject) {
        this("");
        //this(modelObject.getText());
        this.modelObject = modelObject;
        //Object mu = modelObject.getUpdate();
        //if (mu instanceof ViewUpdate) {
        //	updateDetails ((ViewUpdate) mu);
        //}
        modelObject.addObserver(this);
    }

    public Field(ModelObject modelObject, boolean pull) {
        this(modelObject);
        this.pull = pull;
    }

    public Field(ModelObject modelObject, ImageIcon icon, int position) {
        this(modelObject);
        setIcon(icon);
        setHorizontalAlignment(position);
    }

    public ModelObject getModel() {
        return modelObject;
    }

    public void setModel(ModelObject m) {
        modelObject.deleteObserver(this);
        modelObject = m;
        modelObject.addObserver(this);
        update(null, null);
    }

    public void setHighlight(boolean highlight) {
        setBackground(highlight ? HIGHLIGHT_BG_COLOUR : normalBgColour);
    }

    /** This method is mainly needed when NOT using the Observer pattern. */

    @Override
    public void paintComponent(Graphics g) {
        if (modelObject != null && pull) {
            setText(modelObject.getText());
        }
        super.paintComponent(g);
    }

    /** Needed to satisfy the Observer interface. */
    public void update(Observable o1, Object o2) {
        if (o2 instanceof String) {
            setText((String) o2);
        } else if (o2 instanceof ViewUpdate) {
            updateDetails ((ViewUpdate)o2);
        } else {
            setText(modelObject.toString());
        }
    }

    protected void updateDetails (ViewUpdate vu) {
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

    /** Needed to satisfy the ViewObject interface. Currently not used. */
    public void deRegister() {
        if (modelObject != null) modelObject.deleteObserver(this);
        dependents = null;
    }

    public void addDependent (JComponent dependent) {
        if (dependents == null) dependents = new ArrayList<JComponent>(2);
        dependents.add(dependent);
    }

    public List<JComponent> getDependents () {
        return dependents;
    }


}
