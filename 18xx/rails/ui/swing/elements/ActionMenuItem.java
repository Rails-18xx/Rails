/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/ActionMenuItem.java,v 1.4 2008/06/04 19:00:39 evos Exp $*/
package rails.ui.swing.elements;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import rails.game.action.ActionTaker;
import rails.game.action.PossibleAction;

/**
 * A subclass of JButton that allows linking "PossibleAction" objects to it.
 * 
 * @see ClickField
 */
public class ActionMenuItem extends JMenuItem implements ActionTaker {

    private static final long serialVersionUID = 1L;

    private List<PossibleAction> actions = new ArrayList<PossibleAction>(1);

    public ActionMenuItem(String text) {
        super(text);
    }

    public void addPossibleAction(PossibleAction o) {
        actions.add(o);
    }

    public List<PossibleAction> getPossibleActions() {
        return actions;
    }

    public void clearPossibleActions() {
        actions.clear();
    }

    public void setPossibleAction(PossibleAction action) {
        clearPossibleActions();
        addPossibleAction(action);
    }

}
