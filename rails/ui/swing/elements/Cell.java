/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/Cell.java,v 1.4 2008/06/04 19:00:38 evos Exp $*/
package rails.ui.swing.elements;

import java.awt.Color;

import javax.swing.*;
import javax.swing.border.Border;

public abstract class Cell extends JLabel {
    private static final long serialVersionUID = 1L;

    protected Border labelBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);

    public static final Color NORMAL_CAPTION_BG_COLOUR = new Color(240, 240, 240);
    public static final Color NORMAL_FIELD_BG_COLOUR = Color.WHITE;
    public static final Color HIGHLIGHT_BG_COLOUR = new Color(255, 255, 80);
    public static final Color NORMAL_FG_COLOUR = new Color (0, 0, 0);
    public static final Color LOCAL_PLAYER_COLOUR = new Color (255, 0, 0);

    protected Color normalBgColour = NORMAL_FIELD_BG_COLOUR;

    public Cell(String text, boolean asCaption) {
        super(text);
        setForeground(NORMAL_FG_COLOUR);
        setNormalBgColour (asCaption ? NORMAL_CAPTION_BG_COLOUR : NORMAL_FIELD_BG_COLOUR);
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(labelBorder);
        setOpaque(true);
    }

    public Cell (ImageIcon icon, boolean asCaption) {
        super (icon);
    }

    public void setHighlight(boolean highlight) {
        this.setBackground(highlight ? HIGHLIGHT_BG_COLOUR : normalBgColour);
    }

    public void setNormalBgColour (Color colour) {
        setBackground (normalBgColour = colour);
    }

    public void setLocalPlayer (boolean highlight) {
        this.setForeground(highlight ? LOCAL_PLAYER_COLOUR : NORMAL_FG_COLOUR);
    }
}
