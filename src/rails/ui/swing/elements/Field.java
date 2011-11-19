package rails.ui.swing.elements;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import com.google.common.base.Objects;
import rails.game.model.ViewUpdate;

import rails.game.state.Observable;
import rails.game.state.Observer;

// TODO: Make the color and font options work again
public class Field extends JLabel implements Observer {

    private static final long serialVersionUID = 1L;

    private Border labelBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);

    private static final Color NORMAL_BG_COLOUR = Color.WHITE;
    private static final Color HIGHLIGHT_BG_COLOUR = new Color(255, 255, 80);

    private Observable observable;
    private Observer toolTipObserver;
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
        this.observable.addObserver(this);
        this.pull = pull;
        // initialize text
        this.setText(observable.toText());
    }

    public Field(Observable observable) {
        this(observable, false, false);
    }

    public Field(Observable observable, ImageIcon icon, int position) {
        this(observable);
        setIcon(icon);
        setHorizontalAlignment(position);
    }

    public void setToolTipModel(Observable toolTipModel){
        final Observable storeModel = toolTipModel; 
        toolTipObserver = new Observer() {
            public void update(String text) {
               setToolTipText(text);
            }
            public Observable getObservable() {
                return storeModel;
            }
        };
        toolTipModel.addObserver(toolTipObserver);
        // initialize toolTip
        setToolTipText(toolTipModel.toText());
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

    @Override
    public void setText(String text) {
        if (html) {
            super.setText("<html>" + text + "</html>");
        } else {
            super.setText(text);
        }
    }

    public void setHtml() {
        html = true;
    }
    
    // FIXME: ViewUpdate has to be rewritten in the new structure
/*    protected void updateDetails (ViewUpdate vu) {
    @Override
    public void setText (String text) {
        if (html) {
            super.setText("<html>" + text + "</html>");
        } else {
            super.setText(text);
        }
    }

        for (String key : vu.getKeys()) {
            if (ViewUpdate.TEXT.equalsIgnoreCase(key)) {
                setText (vu.getText());
            } else if (ViewUpdate.BGCOLOUR.equalsIgnoreCase(key)) {
                setBackground((Color)vu.getValue(key));
                normalBgColour = getBackground();
                setForeground (Util.isDark(normalBgColour) ? Color.WHITE : Color.BLACK);
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
        */

    // Observer methods
    public void update(String text) {
        setText(text);
    }

    public Observable getObservable() {
        return observable;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("observable", observable.getId()).toString();
    }

}
