package rails.ui.swing.elements;

import java.awt.Color;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class Spinner extends JSpinner
{
	private Color buttonColour = new Color(255, 220, 150);

	public Spinner(int value, int from, int to, int step)
	{
		super(new SpinnerNumberModel(new Integer(value),
				new Integer(from),
				to > 0 ? new Integer(to) : null,
				new Integer(step)));
		this.setBackground(buttonColour);
		this.setOpaque(true);
		this.setVisible(false);
	}

}
