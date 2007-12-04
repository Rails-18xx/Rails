/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/ActionLabel.java,v 1.1 2007/12/04 20:25:20 evos Exp $*/
package rails.ui.swing.elements;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;

import rails.game.action.ActionTaker;
import rails.game.action.PossibleAction;

/** A subclass of JButton that allows linking "PossibleAction" 
 * objects to it.
 * @author VosE
 * @see ClickField
 */
public class ActionLabel extends JLabel implements ActionTaker {

	private List<PossibleAction> actions 
		= new ArrayList<PossibleAction>(1);
	
	public ActionLabel (String text) {
		super (text);
	}

	public ActionLabel (Icon icon) {
		super (icon);
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
