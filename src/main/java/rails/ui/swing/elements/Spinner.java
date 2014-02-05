/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/Spinner.java,v 1.4 2008/06/04 19:00:39 evos Exp $*/
package rails.ui.swing.elements;

import java.awt.Color;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class Spinner extends JSpinner {
    private static final long serialVersionUID = 1L;
    private Color buttonColour = new Color(255, 220, 150);

    public Spinner(int value, int from, int to, int step) {
        super(new SpinnerNumberModel(new Integer(value), new Integer(from),
                to > 0 ? new Integer(to) : null, new Integer(step)));
        this.setBackground(buttonColour);
        this.setOpaque(true);
        this.setVisible(false);
    }

}
