/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/Select.java,v 1.4 2008/06/04 19:00:39 evos Exp $*/
package rails.ui.swing.elements;

import java.awt.Color;
import javax.swing.JComboBox;

public class Select extends JComboBox {

    private static final long serialVersionUID = 1L;
    private Color buttonColour = new Color(255, 220, 150);

    public Select(int[] values) {
        super();
        for (int i = 0; i < values.length; i++) {
            this.addItem("" + values[i]);
        }
        this.setBackground(buttonColour);
        this.setOpaque(true);
        this.setVisible(false);
    }

    public Select(Object[] values) {
        super(values);
        this.setBackground(buttonColour);
        this.setOpaque(true);
        this.setVisible(false);
    }
}
