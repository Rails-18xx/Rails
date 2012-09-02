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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.GameManager;
import rails.game.Player;
import rails.game.PublicCompany;
import rails.game.Round;
import rails.game.state.BooleanState;
import rails.game.state.Observer;
import rails.ui.swing.elements.Field;

public abstract class GridPanel extends JPanel
implements ActionListener, KeyListener {

    // TODO: Check if adding the field is compatible
    private static final long serialVersionUID = 1L;
    
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
    protected Round round;
    protected PublicCompany c;
    protected JComponent f;

    protected List<Observer> observers = new ArrayList<Observer>();

    /** 2D-array of fields to enable show/hide per row or column */
    protected JComponent[][] fields;
    /** Array of Observer objects to set row visibility */
    protected RowVisibility[] rowVisibilityObservers;

    protected List<JMenuItem> menuItemsToReset = new ArrayList<JMenuItem>();

    protected static Logger log =
        LoggerFactory.getLogger(GridPanel.class);


    public void redisplay() {
        revalidate();
    }

    // FIXME: This has to be replaced
    protected void deRegisterObservers() {
        log.debug("Deregistering observers");
        for (Observer vo : observers) {
//            vo.deRegister();
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

        // FIXME: This has to be replaced
//        if (comp instanceof Observer
//            && ((Observer) comp).getObservable() != null) {
//            observers.add((Observer) comp);
//        }

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

    
    /**
     * An observer object that receives the updates 
     * if the Company is closed
     * 
     * TODO: It is unclear to me what the reverseValue really does?
     */
    public class RowVisibility implements Observer {

        private final GridPanel parent;
        private final int rowIndex;
        private final BooleanState observable;

        public RowVisibility (GridPanel parent, int rowIndex, BooleanState observable, boolean reverseValue) {
            this.parent = parent;
            this.rowIndex = rowIndex;
            this.observable = observable;
            // TODO: This was the previous setup
//            lastValue = ((BooleanState)observable).value() != reverseValue;
            
        }

        public boolean lastValue () {
            return observable.value();
        }

        public void update(String text) {
            parent.setRowVisibility(rowIndex, lastValue());
        }

    }

}
