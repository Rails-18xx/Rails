package rails.ui.swing.elements;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;

import rails.game.action.PossibleAction;

public class ClickField extends JToggleButton
{
	private final Color buttonColour = new Color(255, 220, 150);
	private final Insets buttonInsets = new Insets(0, 1, 0, 1);
	
	/** PossibleAction object(s) linked to this field */
	private List<PossibleAction> actions;
	/** @deprecated */
	private List<Object> options;

	public ClickField(String text, String actionCommand, String toolTip,
			ActionListener caller, ButtonGroup group)
	{
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
	
	/** @deprecated */
	public void addOption (Object o) {
		if (options == null) options = new ArrayList<Object>(2);
		options.add(o);
	}
	
	/** @deprecated */
	public List<Object> getOptions () {
	    return options;
	}
	
	/** @deprecated */
	public void clearOptions () {
	    if (options != null) options.clear();
	}

	public void addPossibleAction (PossibleAction o) {
		if (actions == null) actions = new ArrayList<PossibleAction>(2);
	    actions.add(o);
	}
	
	public List<PossibleAction> getPossibleActions () {
	    return actions;
	}
	
	public void clearPossibleActions () {
	    if (actions != null) actions.clear();
	}

}
