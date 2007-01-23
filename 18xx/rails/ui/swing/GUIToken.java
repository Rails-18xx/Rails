package rails.ui.swing;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * This class draws a company's token.
 */

public class GUIToken extends JPanel
{

	private Color fgColor, bgColor;
	private Ellipse2D.Double circle;
	private String name;
	
	public static final int DEFAULT_DIAMETER = 21;
	public static final int DEFAULT_X_COORD = 1;
	public static final int DEFAULT_Y_COORD = 1;

	public void paintComponent(Graphics g)
	{
		clear(g);
		Graphics2D g2d = (Graphics2D) g;

		drawToken(g2d);
		
		Font f = new Font("Helvetica", Font.BOLD, 8);
		g2d.setFont(f);
		g2d.setColor(fgColor);
		g2d.drawString(name, 3, 14);
	}

	public void drawToken(Graphics2D g2d)
	{
		Color oldColor = g2d.getColor();
		g2d.setColor(bgColor);
		g2d.fill(circle);
		g2d.draw(circle);
		g2d.setColor(oldColor);
	}

	protected void clear(Graphics g)
	{
		super.paintComponent(g);
	}

	public GUIToken(String name)
	{
		this(Color.BLACK, Color.WHITE, name, DEFAULT_X_COORD, DEFAULT_Y_COORD, DEFAULT_DIAMETER);
	}

	public GUIToken(Color fc, Color bc, String name)
	{
		this(fc, bc, name, DEFAULT_X_COORD, DEFAULT_Y_COORD, DEFAULT_DIAMETER);
	}

	public GUIToken(double x, double y, String name)
	{
		this(Color.BLACK, Color.WHITE, name, x, y, DEFAULT_DIAMETER);
	}
	
	public GUIToken(Color fc, Color bc, String name, double x, double y)
	{
		this(fc, bc, name, x, y, DEFAULT_DIAMETER);
	}

	public GUIToken(Color fc, Color bc, String name, double x, double y,
			double diameter)
	{
		super();

		fgColor = fc;
		bgColor = bc;

		circle = new Ellipse2D.Double(x, y, diameter, diameter);

		this.setForeground(fgColor);
		this.setOpaque(false);
		this.setVisible(true);
		this.name = name;
	}

	public Color getBgColor()
	{
		return bgColor;
	}

	public Ellipse2D.Double getCircle()
	{
		return circle;
	}

	public Color getFgColor()
	{
		return fgColor;
	}

	public String getName()
	{
		return name;
	}

}
