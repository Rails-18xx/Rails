/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/Spinner.java,v 1.4 2008/06/04 19:00:39 evos Exp $*/
package net.sf.rails.ui.swing.elements;

import java.awt.Color;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class Spinner extends JSpinner {
    private static final long serialVersionUID = 1L;
    private Color buttonColour = new Color(255, 220, 150);
    private SpinnerNumberModel model;

    public Spinner(int value, int from, int to, int step) {
        super(new SpinnerNumberModel(Integer.valueOf(value), Integer.valueOf(from), to > 0 ? to : null, Integer.valueOf(step)));
        this.setBackground(buttonColour);
        this.setOpaque(true);
        this.setVisible(false);
        model = (SpinnerNumberModel)getModel();
    }

    public void setMinimum (int value) {
        model.setMinimum(value);
    }

    public void setMaximum (int value) {
        model.setMaximum(value);
    }

}
