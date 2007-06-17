package rails.ui.swing.elements;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import rails.game.action.PossibleAction;

/** A subclass of JButton that allows linking "PossibleAction" 
 * objects to it.
 * @author VosE
 * @see ClickField
 */
public class ActionButton extends JButton {

	private List<PossibleAction> actions 
		= new ArrayList<PossibleAction>(1);
	
	public ActionButton (String text) {
		super (text);
	}

	public void addAction (PossibleAction o) {
	    actions.add(o);
	}
	
	public List<PossibleAction> getActions () {
	    return actions;
	}
	
	public void clearActions () {
	    actions.clear();
	}

	public void setSelectedAction (PossibleAction action) {
		clearActions();
		addAction (action);
	}

}
