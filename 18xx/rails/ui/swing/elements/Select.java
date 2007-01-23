package rails.ui.swing.elements;

import java.awt.Color;
import javax.swing.JComboBox;

public class Select extends JComboBox
{

	private Color buttonColour = new Color(255, 220, 150);

	public Select(int[] values)
	{
		super();
		for (int i = 0; i < values.length; i++)
		{
			this.addItem("" + values[i]);
		}
		this.setBackground(buttonColour);
		this.setOpaque(true);
		this.setVisible(false);
	}

	public Select(Object[] values)
	{
		super(values);
		this.setBackground(buttonColour);
		this.setOpaque(true);
		this.setVisible(false);
	}
}
