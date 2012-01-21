/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/GridPanel.java,v 1.8 2010/05/24 11:42:35 evos Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import org.apache.log4j.Logger;

import rails.game.*;
import rails.game.model.ModelObject;
import rails.game.state.BooleanState;
import rails.ui.swing.elements.Caption;
import rails.ui.swing.elements.ClickField;
import rails.ui.swing.elements.Field;
import rails.ui.swing.elements.ViewObject;

public abstract class GridPanel extends JPanel
implements ActionListener, KeyListener {

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

    private JComponent highlightedComp = null;
    protected Color tableBorderColor = Color.DARK_GRAY;
    protected Color cellOutlineColor = Color.GRAY;
    protected Color highlightedBorderColor = Color.RED;
    
    public GridPanel() {
        
    }

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
                new DynamicSymmetricBorder(cellOutlineColor,NARROW_GAP),
                new DynamicAsymmetricBorder(tableBorderColor,padTop,padLeft,padBottom,padRight)));

        gridPanel.add(comp, gbc);

        if (comp instanceof ViewObject
            && ((ViewObject) comp).getModel() != null) {
            observers.add((ViewObject) comp);
        }

        if (fields != null && fields[x][y] == null) fields[x][y] = comp;
        comp.setVisible(visible);
    }
    
    public void setHighlight(JComponent comp,boolean isToBeHighlighted) {
        //quit if nothing is to be done
        if (isToBeHighlighted && comp == highlightedComp) return;
        removeHighlight();
        if (isToBeHighlighted) {
            if (comp.getBorder() instanceof FieldBorder) {
                FieldBorder fb = (FieldBorder)comp.getBorder();
                fb.setHighlight(isToBeHighlighted);
                comp.repaint();
            }
            highlightedComp = comp;
        }
    }
    
    public void removeHighlight() {
        if (highlightedComp == null) return;
        if (highlightedComp.getBorder() instanceof FieldBorder) {
            FieldBorder fb = (FieldBorder)highlightedComp.getBorder();
            fb.setHighlight(false);
            highlightedComp.repaint();
        }
        highlightedComp = null;
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

    /**
     * Wrapper for three level compound borders and directly accessing border constituents 
     * @author Frederick Weld
     *
     */
    private class FieldBorder extends CompoundBorder {
        private static final long serialVersionUID = 1L;
        Border nativeInnerBorder;
        DynamicAsymmetricBorder highlightedInnerBorder;
        DynamicSymmetricBorder outlineBorder;
        DynamicAsymmetricBorder outerBorder;
        public FieldBorder(Border innerBorder,DynamicSymmetricBorder outlineBorder,DynamicAsymmetricBorder outerBorder) {
            super(new CompoundBorder(outerBorder,outlineBorder),innerBorder);
            this.nativeInnerBorder = innerBorder;
            this.outlineBorder = outlineBorder;
            this.outerBorder = outerBorder;
            this.highlightedInnerBorder = new DynamicAsymmetricBorder(
                    highlightedBorderColor,
                    nativeInnerBorder.getBorderInsets(null).top,
                    nativeInnerBorder.getBorderInsets(null).left,
                    nativeInnerBorder.getBorderInsets(null).bottom,
                    nativeInnerBorder.getBorderInsets(null).right);
        }
        public void setHighlight(boolean isToBeHighlighted) {
            outlineBorder.setHighlight(isToBeHighlighted);
            this.insideBorder = isToBeHighlighted ? 
                    highlightedInnerBorder : nativeInnerBorder;
        }
    }

    /**
     * A line border providing methods for changing the look 
     * @author Frederick Weld
     *
     */
    private class DynamicSymmetricBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private int thickness;
        private Color borderColor;
        private boolean isHighlighted = false;
        public DynamicSymmetricBorder (Color borderColor,int thickness) {
            this.thickness = thickness;
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
            g2d.setStroke(new BasicStroke(thickness));
            g2d.drawRect(x, y, width-1, height-1);
            g2d.setStroke(oldStroke);
        }

        public Insets getBorderInsets (Component c) {
            return new Insets(thickness,thickness,thickness,thickness);
        }

        public boolean isBorderOpaque() { 
            return true; 
        }
    }
    /**
     * An asymmetric line border providing methods for changing the look 
     * @author Frederick Weld
     *
     */
    private class DynamicAsymmetricBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private int padTop, padLeft, padBottom, padRight;
        private Color borderColor;
        public DynamicAsymmetricBorder (Color borderColor,int padTop, int padLeft, int padBottom, int padRight) {
            this.padTop = padTop;
            this.padLeft = padLeft;
            this.padBottom = padBottom;
            this.padRight = padRight;
            this.borderColor = borderColor;
        }
        public void paintBorder(Component c,Graphics g, int x, int y, int width,int height) {
            Graphics2D g2d = (Graphics2D)g;
            g2d.setColor(borderColor);
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
