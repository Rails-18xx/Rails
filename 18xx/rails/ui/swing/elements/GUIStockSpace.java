/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/elements/GUIStockSpace.java,v 1.2 2007/10/05 22:02:30 evos Exp $*/
package rails.ui.swing.elements;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.SwingConstants;

import org.apache.log4j.Logger;

import rails.game.PublicCompanyI;
import rails.game.StockSpace;
import rails.game.model.ModelObject;
import rails.ui.swing.GUIToken;
import rails.ui.swing.StatusWindow;


public class GUIStockSpace extends JLayeredPane implements ViewObject
{

	StockSpace model;
	JLabel priceLabel;

	int depth = 0;

	Dimension size = new Dimension(40, 40);

	List tokenList;

	private static final Color BROWN = new Color(144, 72, 0);
	private static final Color LIGHT_GRAY = new Color(200, 200, 200);
	private static final Color ORANGE = new Color(255, 180, 0);

	protected static Logger log = Logger.getLogger(GUIStockSpace.class.getPackage().getName());

	public GUIStockSpace(int x, int y, StockSpace model)
	{

		this.model = model;

		priceLabel = new JLabel();

		priceLabel.setBounds(1, 1, size.width, size.height);
		priceLabel.setOpaque(true);

		moveToBack(priceLabel);
		setPreferredSize(new Dimension(40, 40));

		if (model != null)
		{

			priceLabel.setText(Integer.toString(model.getPrice()));
			priceLabel.setBackground(stringToColor(model.getColour()));
			priceLabel.setVerticalTextPosition(SwingConstants.TOP);

			model.addObserver(this);
			if (model.isStart())
			{
				priceLabel.setBorder(BorderFactory.createLineBorder(Color.red,
						2));
			}
		}
		else
		{
			priceLabel.setText("");
			priceLabel.setBackground(LIGHT_GRAY);
		}

		recreate();

	}

	private void recreate()
	{

		removeAll();
		add(priceLabel, new Integer(0), 0);
		placeTokens();
		// repaint();
		revalidate();
	}

	private void placeTokens()
	{

		if (model == null)
			return;
		if (model.hasTokens())
		{
			tokenList = (ArrayList) model.getTokens();

			placeToken(tokenList);
		}
	}

	private void placeToken(List tokenList)
	{

		Point origin = new Point(16, 0);
		Dimension size = new Dimension(40, 40);
		Color bgColour;
		Color fgColour;
		PublicCompanyI co;
		GUIToken token;

		for (int k = tokenList.size()-1; k >=0 ; k--)
		{
			co = (PublicCompanyI) tokenList.get(k);
			bgColour = co.getBgColour();
			fgColour = co.getFgColour();

			token = new GUIToken(fgColour, bgColour, co.getName());
			token.setBounds(origin.x, origin.y, size.width, size.height);

			add(token, new Integer(0), 0);
			origin.y += 6;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see rails.ui.swing.elements.ViewObject#getModel()
	 */
	public ModelObject getModel()
	{
		return model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see rails.ui.swing.elements.ViewObject#deRegister()
	 */
	public void deRegister()
	{
		if (model != null && StatusWindow.useObserver)
			model.deleteObserver(this);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.rails.util.Observer#update(java.rails.util.Observable, java.lang.Object)
	 */
	public void update(Observable o1, Object o2)
	{

		if (StatusWindow.useObserver)
		{
			recreate();
		}

	}

	/**
	 * Quick n' dirty method of converting strings to color objects. This has
	 * been replaced by using hex colors in the XML definitions.
	 * 
	 * @deprecated
	 */
	private static Color stringToColor(String color)
	{
		if (color.equalsIgnoreCase("yellow"))
		{
			return Color.YELLOW;
		}
		else if (color.equalsIgnoreCase("orange"))
		{
			return ORANGE;
		}
		else if (color.equalsIgnoreCase("brown"))
		{
			return BROWN;
		}
		else if (color.equalsIgnoreCase("red"))
		{
			return Color.RED;
		}
		else if (color.equalsIgnoreCase("green"))
		{
			return Color.GREEN;
		}
		else if (color.equalsIgnoreCase("blue"))
		{
			return Color.BLUE;
		}
		else if (color.equalsIgnoreCase("black"))
		{
			return Color.BLACK;
		}
		else if (color.equalsIgnoreCase("white"))
		{
			return Color.WHITE;
		}
		else if (color.equals(""))
		{
			return Color.WHITE;
		}
		else
		{
			log.warn ("Unknown color: " + color + ".");
			return Color.MAGENTA;
		}
	}

}
