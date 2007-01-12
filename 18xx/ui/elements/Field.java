package ui.elements;

import game.model.ModelObject;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Observable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import ui.StatusWindow;

public class Field extends JLabel implements ViewObject
{

	private Border labelBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2);
	private static final Color NORMAL_BG_COLOUR = Color.WHITE;
	private static final Color HIGHLIGHT_BG_COLOUR = new Color(255, 255, 80);
	private ModelObject modelObject;
	private boolean pull = false;

	public Field(String text)
	{
		super(text.equals("0%") ? "" : text);
		this.setBackground(NORMAL_BG_COLOUR);
		this.setHorizontalAlignment(SwingConstants.CENTER);
		this.setBorder(labelBorder);
		this.setOpaque(true);
	}

	public Field(ModelObject modelObject)
	{
		this((String)modelObject.getNotificationObject());
		this.modelObject = modelObject;
		if (StatusWindow.useObserver)
			modelObject.addObserver(this);
	}

	public Field(ModelObject modelObject, boolean pull)
	{
		this(modelObject);
		this.pull = pull;
	}

	public ModelObject getModel()
	{
		return modelObject;
	}

	public void setModel(ModelObject m)
	{
		if (StatusWindow.useObserver)
			modelObject.deleteObserver(this);
		modelObject = m;
		if (StatusWindow.useObserver)
		{
			modelObject.addObserver(this);
			update(null, null);
		}
	}

	public void setHighlight(boolean highlight)
	{
		setBackground(highlight ? HIGHLIGHT_BG_COLOUR : NORMAL_BG_COLOUR);
	}

	/** This method is mainly needed when NOT using the Observer pattern. */

	public void paintComponent(Graphics g)
	{
		if (modelObject != null && (pull || !StatusWindow.useObserver))
		{
			setText((String)modelObject.getNotificationObject());
		}
		super.paintComponent(g);
	}

	/** Needed to satisfy the Observer interface. */
	public void update(Observable o1, Object o2)
	{
		if (StatusWindow.useObserver)
		{
			if (o2 instanceof String)
			{
				setText((String) o2);
			}
			else
			{
				setText(modelObject.toString());
			}
		}
	}

	/** Needed to satisfy the ViewObject interface. Currently not used. */
	public void deRegister()
	{
		if (modelObject != null && StatusWindow.useObserver)
			modelObject.deleteObserver(this);
	}

}
