package net.sf.rails.ui.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import net.sf.rails.common.Config;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Observer;
import net.sf.rails.ui.swing.elements.ClickField;
import net.sf.rails.ui.swing.elements.Field;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    protected RoundFacade round;
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

    private List<JComponent> highlightedComps = new ArrayList<JComponent>();
    protected Color tableBorderColor;
    protected Color cellOutlineColor;
    protected Color highlightedBorderColor;
    
    public GridPanel() {
        //initialize border colors according to the configuration
        if ("enabled".equals(Config.get("gridPanel.tableBorders"))) {
            tableBorderColor = Color.DARK_GRAY;
            cellOutlineColor = Color.GRAY;
            highlightedBorderColor = Color.RED;
        } else {
            tableBorderColor = getBackground();
            cellOutlineColor = getBackground();
            highlightedBorderColor = Color.RED;
        }
    }

    public void redisplay() {
        revalidate();
    }

    protected void deRegisterObservers() {
        log.debug("Deregistering observers");
        for (Observer o : observers) {
            Observable observable = o.getObservable();
            if (observable != null) {
                observable.removeObserver(o);
            }
        }
    }

    protected void addField(JComponent comp, int x, int y, int width, int height,
            int wideGapPositions) {
        addField (comp, x, y, width, height, wideGapPositions, true);
    }

    protected void addField(JComponent comp, int x, int y, int width, int height,
            int wideGapPositions, boolean visible) {

        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0,0,0,0);

        //special handling of clickfields as their compound border does not fit
        //to this field border logic
        if ((comp instanceof ClickField)) {
            comp.setBorder(BorderFactory.createLineBorder(new Color(0,0,0,0),1));
        }

        int padTop, padLeft, padBottom, padRight;
        padTop = (wideGapPositions & WIDE_TOP) > 0 ? WIDE_GAP - NARROW_GAP : 0;
        padLeft = (wideGapPositions & WIDE_LEFT) > 0 ? WIDE_GAP - NARROW_GAP : 0;
        padBottom =
                (wideGapPositions & WIDE_BOTTOM) > 0 ? WIDE_GAP - NARROW_GAP : 0;
        padRight = (wideGapPositions & WIDE_RIGHT) > 0 ? WIDE_GAP - NARROW_GAP : 0;

        //set field borders
        //- inner border: the field's native border
        //- outline border: the field's outline (in narrow_gap thickness)
        //- outer border: grid table lines (in wide_gap - narrow_gap thickness)
        
        comp.setBorder(new FieldBorder(comp.getBorder(),
                new DynamicBorder(cellOutlineColor,NARROW_GAP),
                new DynamicBorder(tableBorderColor,padTop,padLeft,padBottom,padRight)));

        gridPanel.add(comp, gbc);

        if (comp instanceof Observer) {
            observers.add((Observer) comp);
        }

        if (fields != null && fields[x][y] == null) fields[x][y] = comp;
        comp.setVisible(visible);
    }
    
    /**
     * highlights given component by altering its border's attributes
     */
    protected void setHighlight(JComponent comp,boolean isToBeHighlighted) {
        //quit if nothing is to be done
        if (isToBeHighlighted && highlightedComps.contains(comp)) return;
        if (!isToBeHighlighted && !highlightedComps.contains(comp)) return;
        
        if (comp.getBorder() instanceof FieldBorder) {
            FieldBorder fb = (FieldBorder)comp.getBorder();
            fb.setHighlight(isToBeHighlighted);
            comp.repaint();
            if (isToBeHighlighted) {
                highlightedComps.add(comp);
            } else {
                highlightedComps.remove(comp);
            }
        }
    }
    
    protected void removeAllHighlights() {
        for (JComponent c : highlightedComps) {
            if (c.getBorder() instanceof FieldBorder) {
                FieldBorder fb = (FieldBorder)c.getBorder();
                fb.setHighlight(false);
                c.repaint();
            }
        }
        highlightedComps.clear();
    }

    /** Returns a string array, each element of which contains the text contents
     * of one row, separated by semicolons.
     */
    public List<String> getTextContents () {

        List<String> result = new ArrayList<String>(32);
        StringBuilder b;
        String text, tip;
        if (fields == null || fields.length == 0) return result;

        for (int i=0; i<fields.length; i++) {
            b = new StringBuilder();
            for (int j=0; j<fields[i].length; j++) {
                if (j > 0) b.append(";");
                if (fields[i][j] instanceof JLabel) {
                    text = ((JLabel)fields[i][j]).getText();
                    b.append (text == null ? "" : text);
                    if (fields[i][j] instanceof Field) {
                        tip = fields[i][j].getToolTipText();
                        if (Util.hasValue(tip)) {
                            b.append("{").append(tip).append("}");
                        }
                    }
                }
            }
            result.add(b.toString().replaceAll("</?html>", "").replaceAll("<br>",",").replaceAll(" x ", "x"));
        }

        return result;
    }
    
    public void keyPressed(KeyEvent e) {}

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

    public void setRowVisibility (int rowIndex, boolean value) {
        
        for (int j=0; j < fields.length; j++) {
            if (fields[j][rowIndex] != null) {
                fields[j][rowIndex].setVisible(value);
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
        
        public Observable getObservable() {
            return observable;
        }

    }

    /**
     * Wrapper for three level compound borders and directly accessing border constituents 
     * @author Frederick Weld
     *
     */
    private class FieldBorder extends CompoundBorder {
        private static final long serialVersionUID = 1L;
        Border nativeInnerBorder;
        /**
         * remains null if nativeInnerBorder is null or broken
         */
        DynamicBorder highlightedInnerBorder = null;
        DynamicBorder outlineBorder;
        DynamicBorder outerBorder;
        public FieldBorder(Border innerBorder,DynamicBorder outlineBorder,DynamicBorder outerBorder) {
            super(new CompoundBorder(outerBorder,outlineBorder),innerBorder);
            nativeInnerBorder = innerBorder;
            this.outlineBorder = outlineBorder;
            this.outerBorder = outerBorder;

            //prepare highlighted inner border
            if (nativeInnerBorder != null) {
                Insets nativeInnerBorderInsets = nativeInnerBorder.getBorderInsets(null);
                if (nativeInnerBorderInsets != null) {
                    highlightedInnerBorder = new DynamicBorder(
                            highlightedBorderColor,
                            nativeInnerBorderInsets.top,
                            nativeInnerBorderInsets.left,
                            nativeInnerBorderInsets.bottom,
                            nativeInnerBorderInsets.right);
                }
            }
        }
        public void setHighlight(boolean isToBeHighlighted) {
            outlineBorder.setHighlight(isToBeHighlighted);
            this.insideBorder = isToBeHighlighted ? 
                    highlightedInnerBorder : nativeInnerBorder;
        }
    }

    /**
     * A potentially asymmetric line border providing methods for changing the look 
     * @author Frederick Weld
     *
     */
    private class DynamicBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private int padTop, padLeft, padBottom, padRight;
        private Color borderColor;
        private boolean isHighlighted = false;

        public DynamicBorder (Color borderColor,int symmetricPad) {
            this.padTop = symmetricPad;
            this.padLeft = symmetricPad;
            this.padBottom = symmetricPad;
            this.padRight = symmetricPad;
            this.borderColor = borderColor;
        }
        
        public DynamicBorder (Color borderColor,int padTop, int padLeft, int padBottom, int padRight) {
            this.padTop = padTop;
            this.padLeft = padLeft;
            this.padBottom = padBottom;
            this.padRight = padRight;
            this.borderColor = borderColor;
        }
        
        public void setHighlight(boolean isToBeHighlighted) {
            if (isHighlighted != isToBeHighlighted) {
                isHighlighted = isToBeHighlighted;
            }
        }

        public void paintBorder(Component c,Graphics g, int x, int y, int width,int height) {
            Graphics2D g2d = (Graphics2D)g;
            if (isHighlighted) {
                g2d.setColor(highlightedBorderColor);
            } else {
                g2d.setColor(borderColor);
            }
            Stroke oldStroke = g2d.getStroke();
            if (padTop > 0) {
                g2d.setStroke(new BasicStroke(padTop));
                g2d.fillRect(x, y, width, padTop);
            }
            if (padLeft > 0) {
                g2d.setStroke(new BasicStroke(padLeft));
                g2d.fillRect(x, y, padLeft, height);
            }
            if (padBottom > 0) {
                g2d.setStroke(new BasicStroke(padBottom));
                g2d.fillRect(x, y+height-padBottom, width, padBottom);
            }
            if (padRight > 0) {
                g2d.setStroke(new BasicStroke(padRight));
                g2d.fillRect(x+width-padRight, y, padRight, height);
            }
            g2d.setStroke(oldStroke);
        }

        public Insets getBorderInsets (Component c) {
            return new Insets(padTop,padLeft,padBottom,padRight);
        }

        public boolean isBorderOpaque() { 
            return true; 
        }
    }
}
