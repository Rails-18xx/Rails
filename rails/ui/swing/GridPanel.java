/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/GridPanel.java,v 1.8 2010/05/24 11:42:35 evos Exp $*/
package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import org.apache.log4j.Logger;

import rails.common.parser.Config;
import rails.game.*;
import rails.game.model.ModelObject;
import rails.game.state.BooleanState;
import rails.ui.swing.elements.*;

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
    protected List<Player> players;
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
                    new DynamicBorder(cellOutlineColor,NARROW_GAP),
                    new DynamicBorder(tableBorderColor,padTop,padLeft,padBottom,padRight)));

            gridPanel.add(comp, gbc);

            if (comp instanceof ViewObject
                    && ((ViewObject) comp).getModel() != null) {
                observers.add((ViewObject) comp);
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

        @Override
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

        @Override
        public Insets getBorderInsets (Component c) {
            return new Insets(padTop,padLeft,padBottom,padRight);
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }
    }
}
