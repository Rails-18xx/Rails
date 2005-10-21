package ui.hexmap;

import game.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

/**
 * Base abstract class that holds common components for GUIHexes of all orientations.  
 */

public abstract class GUIHex
{

	public static final double SQRT3 = Math.sqrt(3.0);
	public static final double DEG60 = Math.PI / 3;
	protected MapHex model;
	protected GeneralPath innerHexagon;
	protected static final Color highlightColor = Color.red;

	// Added by Erik Vos
	protected String hexName;
	protected int tileId;
	protected int tileOrientation;
	protected String tileFilename;

	// These are only here for scope visibility
	protected double tileScale = 0.33;
	protected int x_adjust;
	protected int y_adjust;
	protected double rotation;
	protected int arr_index = 0;
	protected double[] rotation_arr = new double[7];
	protected int[] x_adjust_arr = new int[7];
	protected int[] y_adjust_arr = new int[7];

	protected BufferedImage tileImage;
	protected AffineTransform af = new AffineTransform();

	protected String toolTip = "";

	/**
	 * Stores the neighbouring views. This parallels the neighors field in
	 * BattleHex, just on the view side.
	 * 
	 * @todo check if we can avoid this
	 */
	private GUIHex[] neighbors = new GUIHex[6];

	// GUI variables
	double[] xVertex = new double[6];
	double[] yVertex = new double[6];
	double len;
	GeneralPath hexagon;
	Rectangle rectBound;

	/** Globally turns antialiasing on or off for all hexes. */
	static boolean antialias;
	/** Globally turns overlay on or off for all hexes */
	static boolean useOverlay;
	// Selection is in-between GUI and game state.
	private boolean selected;

	public GUIHex()
	{
	}

	public MapHex getHexModel()
	{
		return this.model;
	}

	public void setHexModel(MapHex model)
	{
		this.model = model;
	}

	public Rectangle getBounds()
	{
		return rectBound;
	}

	public boolean contains(Point2D.Double point)
	{
		return (hexagon.contains(point));
	}

	public boolean contains(Point point)
	{
		return (hexagon.contains(point));
	}
	
	public boolean intersects(Rectangle2D r)
	{
		return (hexagon.intersects(r));
	}

	public void select()
	{
		selected = true;
	}

	public void unselect()
	{
		selected = false;
	}

	public void setSelected(boolean selected)
	{
		this.selected = selected;
	}

	public boolean isSelected()
	{
		return selected;
	}

	static boolean getAntialias()
	{
		return antialias;
	}

	static void setAntialias(boolean enabled)
	{
		antialias = enabled;
	}

	static boolean getOverlay()
	{
		return useOverlay;
	}

	public static void setOverlay(boolean enabled)
	{
		useOverlay = enabled;
	}

	/**
	 * Return a GeneralPath polygon, with the passed number of sides, and the
	 * passed x and y coordinates. Close the polygon if the argument closed is
	 * true.
	 */
	static GeneralPath makePolygon(int sides, double[] x, double[] y,
			boolean closed)
	{
		GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, sides);
		polygon.moveTo((float) x[0], (float) y[0]);
		for (int i = 1; i < sides; i++)
		{
			polygon.lineTo((float) x[i], (float) y[i]);
		}
		if (closed)
		{
			polygon.closePath();
		}

		return polygon;
	}

	/** Return the Point closest to the center of the polygon. */
	public Point findCenter()
	{
		return new Point((int) ((xVertex[2] + xVertex[5]) / 2),
				(int) ((yVertex[0] + yVertex[3]) / 2));
	}

	/** Return the Point2D.Double at the center of the polygon. */
	Point2D.Double findCenter2D()
	{
		return new Point2D.Double((xVertex[2] + xVertex[5]) / 2.0,
				(yVertex[0] + yVertex[3]) / 2.0);
	}

	public void setNeighbor(int i, GUIHex hex)
	{
		if (i >= 0 && i < 6)
		{
			neighbors[i] = hex;
			getMapHexModel().setNeighbor(i, hex.getMapHexModel());
		}
	}

	public GUIHex getNeighbor(int i)
	{
		if (i < 0 || i > 6)
		{
			return null;
		}
		else
		{
			return neighbors[i];
		}
	}

	public MapHex getMapHexModel()
	{
		return (MapHex) getHexModel();
	}

	public void paint(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;

		if (getAntialias())
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
		}
		else
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_OFF);
		}

		Color terrainColor = Color.WHITE; //getMapHexModel().getTerrainColor();
		if (isSelected())
		{
			// Slightly adjust the hex overlay size to allow the
			// highlighting to peek through.
			tileScale = 0.3;

			if (terrainColor.equals(highlightColor))
			{
				// g2.setColor(HTMLColor.invertRGBColor(highlightColor));
			}
			else
			{
				g2.setColor(highlightColor);
			}
			g2.fill(hexagon);

			g2.setColor(terrainColor);
			g2.fill(innerHexagon);

			g2.setColor(Color.black);
			g2.draw(innerHexagon);
		}
		else
		{
			// restore hex size to it's original scale.
			if (tileScale != 0.33)
				tileScale = 0.33;

			g2.setColor(terrainColor);
			g2.fill(hexagon);
		}

		g2.setColor(Color.black);
		g2.draw(hexagon);

		paintOverlay(g2);

		FontMetrics fontMetrics = g2.getFontMetrics();

		// Added by Erik Vos: show hex name
		g2.drawString(hexName,
				rectBound.x + (rectBound.width - fontMetrics.stringWidth(getMapHexModel().getName())) * 2/5,
				rectBound.y	+ ((fontMetrics.getHeight() + rectBound.height) * 3/10));

		g2.drawString("("+model.getX()+","+model.getY()+")", 
				rectBound.x + (rectBound.width - fontMetrics.stringWidth("("+getMapHexModel().getX()+","+getMapHexModel().getY()+")")) * 1/3,
				rectBound.y	+ ((fontMetrics.getHeight() + rectBound.height) * 1/2));

		// Added by Erik Vos: show the preprinted tile id
		g2.drawString(tileId == -999 ? "?" : "#" + tileId,
				rectBound.x	+ (rectBound.width - fontMetrics.stringWidth("#"+getMapHexModel().getPreprintedTileId())) * 2/5,
				rectBound.y	+ ((fontMetrics.getHeight() + rectBound.height) * 7/10));
	}

	public boolean paintOverlay(Graphics2D g)
	{
		BufferedImage overlay = tileImage;

		if (overlay != null)
		{ // first, draw the Hex itself

			Point center = findCenter();
			af = AffineTransform.getRotateInstance(rotation);
			af.scale(tileScale, tileScale);

			// All adjustments to AffineTransform must be done before being
			// assigned to the ATOp here.
			AffineTransformOp aop = new AffineTransformOp(af,
					AffineTransformOp.TYPE_BILINEAR);

			g.drawImage(tileImage,
					aop,
					(center.x + x_adjust),
					(center.y + y_adjust));
			g.setTransform(AffineTransform.getRotateInstance(0));

		}
		boolean didAllHexside = true;
		Shape oldClip = g.getClip();
		// make sure we draw only inside our hex
		g.setClip(null);
		g.clip(hexagon);
		g.setClip(oldClip);
		return didAllHexside;
	}

	// Added by Erik Vos
	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return hexName;
	}

	/**
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name)
	{
		this.hexName = name;
	}

	/**
	 * @return Returns the tileId.
	 */
	public int getTileId()
	{
		return tileId;
	}

	/**
	 * @param tileId
	 *            The tileId to set.
	 */
	public void setTileId(int tileId)
	{
		this.tileId = tileId;
	}

	/**
	 * @param tileOrientation
	 *            The tileOrientation to set.
	 */
	public void setTileOrientation(int tileOrientation)
	{
		this.tileOrientation = tileOrientation;
	}

	/**
	 * 
	 * @return Filename of the tile image
	 */
	public String getTileFilename()
	{
		return tileFilename;
	}

	public void setTileFilename(String tileFilename)
	{
		this.tileFilename = tileFilename;
	}

	public void setTileImage(BufferedImage tileImage)
	{
		this.tileImage = tileImage;
	}

	protected String getToolTip()
	{
		return "<html><b>Hex</b>: " + hexName + "<br><b>Tile</b>: " + tileId;
	}

	protected void rotateHexCW()
	{
		if (arr_index >= 6)
		{
			arr_index = 1;
		}
		else
			arr_index++;
	}

	protected void rotateHexCCW()
	{
		if (arr_index <= 1)
		{
			arr_index = 6;
		}
		else
			arr_index--;
	}

}
