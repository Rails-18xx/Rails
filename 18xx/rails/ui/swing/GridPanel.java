/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/GridPanel.java,v 1.8 2010/05/24 11:42:35 evos Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.log4j.Logger;

import rails.game.*;
import rails.game.model.ModelObject;
import rails.game.state.BooleanState;
import rails.ui.swing.elements.Field;
import rails.ui.swing.elements.ViewObject;

public abstract class GridPanel extends JPanel
implements ActionListener, KeyListener {

    protected static final int NARROW_GAP = 1;
    protected static final int WIDE_GAP = 3;
    protected static final int WIDE_LEFT = 1;
    protected static final int WIDE_RIGHT = 2;
    protected static final int WIDE_TOP = 4;
    protected static final int WIDE_BOTTOM = 8;

    protected JPanel gridPanel;
    protected JFrame parentFrame;

    protected GridBagLayout gb;
    protected GridBagConstraints gbc;

    protected static Color buttonHighlight = new Color(255, 160, 80);

    protected int np;
    protected Player[] players;
    protected int nc;
    protected PublicCompanyI[] companies;
    protected RoundI round;
    protected PublicCompanyI c;
    protected JComponent f;

    protected List<ViewObject> observers = new ArrayList<ViewObject>();

    /** 2D-array of fields to enable show/hide per row or column */
    protected JComponent[][] fields;
    /** Array of Observer objects to set row visibility */
    protected RowVisibility[] rowVisibilityObservers;

    protected List<JMenuItem> menuItemsToReset = new ArrayList<JMenuItem>();

    protected static Logger log =
        Logger.getLogger(GridPanel.class.getPackage().getName());


    public void redisplay() {
        revalidate();
    }

    protected void deRegisterObservers() {
        log.debug("Deregistering observers");
        for (ViewObject vo : observers) {
            vo.deRegister();
        }
    }

    protected void addField(JComponent comp, int x, int y, int width, int height,
            int wideGapPositions) {
        addField (comp, x, y, width, height, wideGapPositions, true);
    }

    protected void addField(JComponent comp, int x, int y, int width, int height,
            int wideGapPositions, boolean visible) {

        int padTop, padLeft, padBottom, padRight;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        padTop = (wideGapPositions & WIDE_TOP) > 0 ? WIDE_GAP : NARROW_GAP;
        padLeft = (wideGapPositions & WIDE_LEFT) > 0 ? WIDE_GAP : NARROW_GAP;
        padBottom =
                (wideGapPositions & WIDE_BOTTOM) > 0 ? WIDE_GAP : NARROW_GAP;
        padRight = (wideGapPositions & WIDE_RIGHT) > 0 ? WIDE_GAP : NARROW_GAP;
        gbc.insets = new Insets(padTop, padLeft, padBottom, padRight);

        gridPanel.add(comp, gbc);

        if (comp instanceof ViewObject
            && ((ViewObject) comp).getModel() != null) {
            observers.add((ViewObject) comp);
        }

        if (fields != null && fields[x][y] == null) fields[x][y] = comp;
        comp.setVisible(visible);
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            HelpWindow.displayHelp(GameManager.getInstance().getHelp());
            e.consume();
        }
    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

    public void setRowVisibility (int rowIndex, boolean value) {

        List<JComponent> dependents;

        for (int j=0; j < fields.length; j++) {
            if (fields[j][rowIndex] != null) {
                fields[j][rowIndex].setVisible(value);
                if (fields[j][rowIndex] instanceof Field
                        && (dependents = ((Field)fields[j][rowIndex]).getDependents()) != null) {
                    for (JComponent dependent : dependents) {
                        dependent.setVisible(value);
                    }
                }
            }
        }
    }

    public class RowVisibility implements ViewObject {

        private GridPanel parent;
        private ModelObject modelObject;
        private int rowIndex;
        private boolean lastValue;
        private boolean reverseValue;

        public RowVisibility (GridPanel parent, int rowIndex, ModelObject model, boolean reverseValue) {
            this.parent = parent;
            this.modelObject = model;
            this.rowIndex = rowIndex;
            this.reverseValue = reverseValue;
            modelObject.addObserver(this);
            lastValue = ((BooleanState)modelObject).booleanValue() != reverseValue;
        }

        public boolean lastValue () {
            return lastValue;
        }

        /** Needed to satisfy the ViewObject interface. */
        public ModelObject getModel() {
            return modelObject;
        }

        /** Needed to satisfy the Observer interface.
         * The closedObject model will send true if the company is closed. */
        public void update(Observable o1, Object o2) {
            if (o2 instanceof Boolean) {
                lastValue = (Boolean)o2;
                parent.setRowVisibility(rowIndex, lastValue);
            }
        }

        /** Needed to satisfy the ViewObject interface. Currently not used. */
        public void deRegister() {
            if (modelObject != null)
                modelObject.deleteObserver(this);
        }
    }

}
