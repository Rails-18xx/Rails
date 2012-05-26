/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/ActionCheckBoxMenuItem.java,v 1.1 2010/03/03 00:45:39 stefanfrey Exp $*/
package rails.ui.swing.elements;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;

import rails.game.action.ActionTaker;
import rails.game.action.PossibleAction;

/**
 * A subclass of JButton that allows linking "PossibleAction" objects to it.
 * 
 * @see ClickField
 */
public class ActionCheckBoxMenuItem extends JCheckBoxMenuItem implements ActionTaker {

    private static final long serialVersionUID = 1L;

    private List<PossibleAction> actions = new ArrayList<PossibleAction>(1);

    public ActionCheckBoxMenuItem(String text) {
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
