package ui.hexmap;

import game.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

import ui.ImageLoader;

/**
 * Base abstract class that holds common components for GUIHexes of all orientations.  
 */

public abstract class GUIHex extends JComponent
{

	public static final double SQRT3 = Math.sqrt(3.0);
	public static final double NORMAL_SCALE = 0.33;
	public static final double SELECTED_SCALE = 0.27;
	//public static final double DEG60 = Math.PI / 3;
	protected MapHex model;
	protected GeneralPath innerHexagon;
	protected static final Color highlightColor = Color.red;

	// Added by Erik Vos
	protected String hexName;
	protected int currentTileId;
	protected int currentTileOrientation;
	protected String tileFilename;
	protected TileI currentTile;
	// Tile laid but not yet confirmed
	//protected int provisionalTileId;
	//protected TileI provisionalTile = null; 
	
	protected GUITile currentGUITile = null;
	protected GUITile provisionalGUITile = null;
	protected int provisionalTileOrientation;


	// These are only here for scope visibility
	protected double tileScale = NORMAL_SCALE;
	//protected int x_adjust;
	//protected int y_adjust;
	//protected double rotation;
	//protected int arr_index = 0;
	//protected double[] rotation_arr = new double[7];
	//protected int[] x_adjust_arr = new int[7];
	//protected int[] y_adjust_arr = new int[7];

	//protected BufferedImage tileImage;
	//protected AffineTransform af = new AffineTransform();
	protected JComponent map;
	//protected ImageLoader imageLoader = new ImageLoader();

	protected String toolTip = "";

	/**
	 * Stores the neighbouring views. This parallels the neighors field in
	 * MapHex, just on the view side.
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
	static boolean antialias = true;
	/** Globally turns overlay on or off for all hexes */
	static boolean useOverlay = true;
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
		currentTile = model.getCurrentTile();
		hexName = model.getName();
		currentTileId = model.getPreprintedTileId();
		currentTileOrientation = model.getPreprintedTileOrientation();
		//tileFilename = model.getTileFileName();
		//tileImage = imageLoader.getTile(currentTileId);
		currentGUITile = new GUITile (currentTileId, model);
		currentGUITile.setRotation(currentTileOrientation);
		
	}

	public Rectangle getBounds()
	{
		return rectBound;
	}
	
	public void setBounds(Rectangle rectBound)
	{
		this.rectBound = rectBound;
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

	public void setSelected(boolean selected)
	{
		this.selected = selected;
		if (selected) {
		    currentGUITile.setScale (SELECTED_SCALE);
		} else {
		    currentGUITile.setScale (NORMAL_SCALE);
		    provisionalGUITile = null;
		}
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
			getHexModel().setNeighbor(i, hex.getHexModel());
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
			g2.setColor(highlightColor);
			g2.fill(hexagon);

			g2.setColor(terrainColor);
			g2.fill(innerHexagon);

			g2.setColor(Color.black);
			g2.draw(innerHexagon);
		}

		//FIXME: Disabled until we can properly update the overlay drawing to work with the scrollpane
		paintOverlay(g2);

		/*
		FontMetrics fontMetrics = g2.getFontMetrics();

		// Added by Erik Vos: show hex name
		g2.drawString(hexName,
				rectBound.x + (rectBound.width - fontMetrics.stringWidth(getHexModel().getName())) * 2/5,
				rectBound.y	+ ((fontMetrics.getHeight() + rectBound.height) * 3/10));

		g2.drawString("("+model.getX()+","+model.getY()+")", 
				rectBound.x + (rectBound.width - fontMetrics.stringWidth("("+getHexModel().getX()+","+getHexModel().getY()+")")) * 1/3,
				rectBound.y	+ ((fontMetrics.getHeight() + rectBound.height) * 1/2));

		// Added by Erik Vos: show the preprinted tile id
		g2.drawString(currentTileId == -999 ? "?" : "#" + currentTileId,
				rectBound.x	+ (rectBound.width - fontMetrics.stringWidth("#"+getHexModel().getPreprintedTileId())) * 2/5,
				rectBound.y	+ ((fontMetrics.getHeight() + rectBound.height) * 7/10));
		*/
	}

	public void paintOverlay(Graphics2D g2)
	{
	    /*
		if (tileImage != null)
		{ // first, draw the Hex itself
			Point center = findCenter();
			af = AffineTransform.getRotateInstance(rotation);
			af.scale(tileScale, tileScale);

			// All adjustments to AffineTransform must be done before being
			// assigned to the ATOp here.
			AffineTransformOp aop = new AffineTransformOp(af,
					AffineTransformOp.TYPE_BILINEAR);

			g2.drawImage(tileImage,
					aop,
					center.x + x_adjust,
					center.y + y_adjust);
		}
		*/
		Point center = findCenter();
		if (provisionalGUITile != null) {
		    //provisionalGUITile.paintTile(g2, center.x + x_adjust, center.y + y_adjust, rotation);
		    provisionalGUITile.paintTile(g2, center.x, center.y);
		} else {
		    //currentGUITile.paintTile(g2, center.x + x_adjust, center.y + y_adjust, rotation);
		    currentGUITile.paintTile(g2, center.x, center.y);
		}
	    
	}
	
	public void rotateTile () {
	    
		if (provisionalGUITile != null) {
		    provisionalGUITile.rotate(1);
		}
	    
	    
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
     * @return Returns the currentTile.
     */
    public TileI getCurrentTile() {
        return currentTile;
    }

	/**
	 * @param currentTileOrientation
	 *            The currentTileOrientation to set.
	 */
	public void setTileOrientation(int tileOrientation)
	{
		this.currentTileOrientation = tileOrientation;
	}

	/**
	 * 
	 * @return Filename of the tile image
	 */
	/*
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
	*/

	protected String getToolTip()
	{
	    StringBuffer toolTip = new StringBuffer ("<html>");
	    toolTip.append ("<b>Hex</b>: ").append(hexName);
	    // The next line is a temporary development aid, that can be removed later.
	    toolTip.append ("  <small>(").append(model.getX()).append(",").append(model.getY()).append(")</small>");
	    toolTip.append ("<br><b>Tile</b>: ").append(currentTile.getId());
	    if (currentTile.hasStations()) {
	        Iterator it = currentTile.getStations().iterator();
	        Station st;
	        while (it.hasNext()) {
	            st = (Station)it.next();
	            toolTip.append("<br>  ").append(st.getType());
	            toolTip.append(": value ").append(st.getValue());
	            if (st.getValue() > 0 && st.getBaseSlots() > 0) {
	                toolTip.append(", ").append(st.getBaseSlots()).append(" slots");
	            }
	        }
	    }
	    String upgrades = currentTile.getUpgradesString(model);
		if (upgrades.equals("")) {
		    toolTip.append ("<br>No upgrades");
		} else {
		    toolTip.append("<br><b>Upgrades</b>: ").append(upgrades);
		}
		return toolTip.toString();
	}

	/*
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
	*/

	
	public JComponent getMap()
	{
		return map;
	}

	
	public void setMap(JComponent map)
	{
		this.map = map;
	}
	
	public void dropTile (int tileId) {
		provisionalGUITile = new GUITile (tileId, model);
		provisionalGUITile.setScale(SELECTED_SCALE);

	}
	
	public void removeTile () {
	    provisionalGUITile = null;
	    setSelected (false);
	}
	
	public void fixTile () {
	    currentGUITile = provisionalGUITile;
	    if (currentGUITile != null) {
	        currentTile = currentGUITile.getTile();
	        currentTileId = currentTile.getId();
	    }
	    
	    setSelected (false);
	    
	}

}
