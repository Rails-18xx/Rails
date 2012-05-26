/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/ClickField.java,v 1.7 2008/06/04 19:00:39 evos Exp $*/
package rails.ui.swing.elements;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;

import rails.game.action.ActionTaker;
import rails.game.action.PossibleAction;

public class ClickField extends JToggleButton implements ActionTaker {
    private static final long serialVersionUID = 1L;

    private final Color buttonColour = new Color(255, 220, 150);

    private final Insets buttonInsets = new Insets(0, 1, 0, 1);

    /** PossibleAction object(s) linked to this field */
    private List<PossibleAction> actions;

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

    public void addPossibleAction(PossibleAction o) {
        if (actions == null) actions = new ArrayList<PossibleAction>(2);
        actions.add(o);
    }

    public List<PossibleAction> getPossibleActions() {
        return actions;
    }

    public void clearPossibleActions() {
        if (actions != null) actions.clear();
    }

    public void setPossibleAction(PossibleAction action) {
        clearPossibleActions();
        addPossibleAction(action);
    }
}
