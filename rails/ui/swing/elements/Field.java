/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/Field.java,v 1.11 2010/01/31 22:22:34 macfreek Exp $*/
package rails.ui.swing.elements;

import java.awt.Color;
import java.awt.Graphics;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import rails.game.model.ModelObject;
import rails.game.model.ViewUpdate;
import rails.util.Util;

public class Field extends Cell implements ViewObject {

    private static final long serialVersionUID = 1L;

    private ModelObject modelObject;

    private List<JComponent> dependents = null;

    private boolean pull = false;

    private boolean html = false;

    /** Intended for (possibly varying) tooltip text that must be held across player actions */
    private String baseToolTipInfo = null;

    public Field(String text) {
        super(text.equals("0%") ? "" : text, false);
    }

    public Field(ImageIcon info) {
        super(info, false);
    }

    public Field(ModelObject modelObject) {
        this(modelObject, false, false);
        this.modelObject = modelObject;
        modelObject.addObserver(this);
    }

    public Field(ModelObject modelObject, boolean html, boolean pull) {
        this("");
        this.modelObject = modelObject;
        this.html = html;
        this.modelObject.addObserver(this);
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

    @Override
    public void setText (String text) {
        if (html) {
            super.setText("<html>" + text + "</html>");
        } else {
            super.setText(text);
        }
    }

    protected void updateDetails (ViewUpdate vu) {
        boolean fgSpecified = false;
        for (String key : vu.getKeys()) {
            if (ViewUpdate.TEXT.equalsIgnoreCase(key)) {
                setText (vu.getText());
            } else if (ViewUpdate.BGCOLOUR.equalsIgnoreCase(key)) {
                setBackground((Color)vu.getValue(key));
                if (fgSpecified == false) {// Handles where case where background was set second.  
                    normalBgColour = getBackground();
                    setForeground (Util.isDark(normalBgColour) ? Color.WHITE : Color.BLACK);
                }
            } else if (ViewUpdate.FGCOLOUR.equalsIgnoreCase(key)) {
                setForeground((Color)vu.getValue(key));
                fgSpecified = true;
            } else if (ViewUpdate.SHARES.equalsIgnoreCase(key)) {
            
                int count;
                String type;
                String[] items;
                StringBuilder b = new StringBuilder();
                for (String typeAndCount : ((String)vu.getValue(key)).split(",")) {
                    //Util.getLogger().debug(">>> "+typeAndCount+" <<<");
                    if (!Util.hasValue(typeAndCount)) continue;
                    items = typeAndCount.split(":");
                    count = Integer.parseInt(items[1]);
                    items = items[0].split("_");
                    type = items[1] + (items.length > 2 && items[2].contains("P") ? "P" : "");
                    if (b.length() > 0) b.append("<br>");
                    b.append(count).append(" x ").append(type);
                }
                baseToolTipInfo = b.toString();
                setToolTipText (b.length()>0 ? "<html>" + baseToolTipInfo : null);
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

    public void setHtml() {
        html = true;
    }

    public String getBaseToolTipInfo() {
        return baseToolTipInfo;
    }


}
