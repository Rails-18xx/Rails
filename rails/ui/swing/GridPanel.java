/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/GridPanel.java,v 1.8 2010/05/24 11:42:35 evos Exp $*/
package rails.ui.swing;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;


import org.apache.log4j.Logger;

import rails.game.GameManager;
import rails.game.Player;
import rails.game.PublicCompany;
import rails.game.RoundI;
import rails.game.model.Model;
import rails.game.state.BooleanState;
import rails.game.state.Observable;
import rails.game.state.Observer;
import rails.ui.swing.elements.Field;

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
    protected PublicCompany[] companies;
    protected RoundI round;
    protected PublicCompany c;
    protected JComponent f;

    protected List<Observer> observers = new ArrayList<Observer>();

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
        for (Observer vo : observers) {
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

        if (comp instanceof Observer
            && ((Observer) comp).getObservable() != null) {
            observers.add((Observer) comp);
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

    public class RowVisibility implements Observer {

        private GridPanel parent;
        private Observable observable;
        private int rowIndex;
        private boolean lastValue;
        private boolean reverseValue;

        public RowVisibility (GridPanel parent, int rowIndex, Model model, boolean reverseValue) {
            this.parent = parent;
            this.observable = model;
            this.rowIndex = rowIndex;
            this.reverseValue = reverseValue;
            observable.addObserver(this);
            lastValue = ((BooleanState)observable).booleanValue() != reverseValue;
        }

        public boolean lastValue () {
            return lastValue;
        }

        /** Needed to satisfy the Observer interface.
         * The closedObject model will send true if the company is closed. */
        
/*  
       public void update(Observable o1, Object o2) {
            if (o2 instanceof Boolean) {
                lastValue = (Boolean)o2;
                parent.setRowVisibility(rowIndex, lastValue);
            }
        }
*/

        public void update() {
            // FIXME: There was a Boolean object submitted if the company is closed
            // TODO: Make this functionality available again
            // see above the old update method
        }

        public Observable getObservable() {
            return observable;
        }

        public boolean deRegister() {
            return observable.removeObserver(this);
        }


    }

}
