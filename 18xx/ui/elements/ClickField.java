/*
 * $Header: /Users/blentz/rails_rcs/cvs/18xx/ui/elements/Attic/ClickField.java,v 1.1 2005/08/08 20:08:24 evos Exp $
 * 
 * Created on 06-Aug-2005 Change Log:
 */
package ui.elements;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;

public class ClickField extends JToggleButton {

    private final Color buttonColour = new Color(255, 220, 150);
    private final Insets buttonInsets = new Insets(0, 1, 0, 1);

    public ClickField(String text, String actionCommand, String toolTip,
            ActionListener caller, ButtonGroup group) {
        super(text);
        this.setBackground(buttonColour);
        this.setMargin(buttonInsets);
        this.setOpaque(true);
        this.setVisible(false);
        this.addActionListener(caller);
        this.setActionCommand(actionCommand);
        this.setToolTipText(toolTip);
        group.add(this);
    }

}