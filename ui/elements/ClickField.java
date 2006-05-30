package ui.elements;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;

public class ClickField extends JToggleButton
{
	private final Color buttonColour = new Color(255, 220, 150);
	private final Insets buttonInsets = new Insets(0, 1, 0, 1);
	
	/** options canbe used to hold choices to me made after clicking this button */
	private List options = new ArrayList();

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
	
	public void addOption (Object o) {
	    options.add(o);
	}
	
	public List getOptions () {
	    return options;
	}
	
	public void clearOptions () {
	    options.clear();
	}

}
