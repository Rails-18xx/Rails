/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/ActionButton.java,v 1.3 2007/10/05 22:02:30 evos Exp $*/
package rails.ui.swing.elements;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import rails.game.action.ActionTaker;
import rails.game.action.PossibleAction;

/** A subclass of JButton that allows linking "PossibleAction" 
 * objects to it.
 * @author VosE
 * @see ClickField
 */
public class ActionButton extends JButton implements ActionTaker {

	private List<PossibleAction> actions 
		= new ArrayList<PossibleAction>(1);
	
	public ActionButton (String text) {
		super (text);
	}

	public void addPossibleAction (PossibleAction o) {
	    actions.add(o);
	}
	
	public List<PossibleAction> getPossibleActions () {
	    return actions;
	}
	
	public void clearPossibleActions () {
	    actions.clear();
	}

	public void setPossibleAction (PossibleAction action) {
		clearPossibleActions();
		addPossibleAction (action);
	}

}
