/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/Caption.java,v 1.4 2008/06/04 19:00:38 evos Exp $*/
package rails.ui.swing.elements;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

public class Caption extends JLabel {
    private static final long serialVersionUID = 1L;

    private Border labelBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);

    private static final Color NORMAL_BG_COLOUR = new Color(240, 240, 240);

    private static final Color HIGHLIGHT_BG_COLOUR = new Color(255, 255, 80);
    
    private static final Color NORMAL_FG_COLOUR = new Color (0, 0, 0);
    
    private static final Color LOCAL_PLAYER_COLOUR = new Color (255, 0, 0);

    public Caption(String text) {
        super(text);
        this.setForeground(NORMAL_FG_COLOUR);
        this.setBackground(NORMAL_BG_COLOUR);
        this.setHorizontalAlignment(SwingConstants.CENTER);
        this.setBorder(labelBorder);
        this.setOpaque(true);
    }

    public void setHighlight(boolean highlight) {
        this.setBackground(highlight ? HIGHLIGHT_BG_COLOUR : NORMAL_BG_COLOUR);
    }
    
    public void setLocalPlayer (boolean highlight) {
        this.setForeground(highlight ? LOCAL_PLAYER_COLOUR : NORMAL_FG_COLOUR);
    }
}
