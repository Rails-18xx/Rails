/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/ui/swing/hexmap/GUIHex.java,v 1.7 2007/10/05 22:02:31 evos Exp $*/
package rails.ui.swing.hexmap;


import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import org.apache.log4j.Logger;

import rails.game.*;
import rails.game.model.ModelObject;
import rails.ui.swing.GUIToken;
import rails.ui.swing.GameUIManager;
import rails.ui.swing.StatusWindow;
import rails.ui.swing.elements.ViewObject;


/**
 * Base class that holds common components for GUIHexes of all orientations.
 */

public class GUIHex implements ViewObject
{

	public static final double SQRT3 = Math.sqrt(3.0);
	public static double NORMAL_SCALE = 1.0;
	public static double SELECTED_SCALE = 0.8;
	
	public static void setScale (double scale) {
		NORMAL_SCALE = scale;
		SELECTED_SCALE = 0.8 * scale;
	}

	protected MapHex model;
	protected GeneralPath innerHexagon;
	protected static final Color highlightColor = Color.red;
	protected Point center;

	protected String hexName;
	protected int currentTileId;
	protected int originalTileId;
	protected int currentTileOrientation;
	protected String tileFilename;
	protected TileI currentTile;

	protected GUITile currentGUITile = null;
	protected GUITile provisionalGUITile = null;

	protected GUIToken provisionalGUIToken = null;
	
	protected double tileScale = NORMAL_SCALE;

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
	// Selection is in-between GUI and rails.game state.
	private boolean selected;

	protected static Logger log = Logger.getLogger(GUIHex.class.getPackage().getName());

	public GUIHex(double cx, double cy, int scale, double xCoord, double yCoord)
	{
		if (MapManager.getTileOrientation() == MapHex.EW)
		{
			len = scale;
			xVertex[0] = cx + SQRT3 / 2 * scale;
			yVertex[0] = cy + 0.5 * scale;
			xVertex[1] = cx + SQRT3 * scale;
			yVertex[1] = cy;
			xVertex[2] = cx + SQRT3 * scale;
			yVertex[2] = cy - 1 * scale;
			xVertex[3] = cx + SQRT3 / 2 * scale;
			yVertex[3] = cy - 1.5 * scale;
			xVertex[4] = cx;
			yVertex[4] = cy - 1 * scale;
			xVertex[5] = cx;
			yVertex[5] = cy;
		}
		else
		{
			len = scale / 3.0;
			xVertex[0] = cx;
			yVertex[0] = cy;
			xVertex[1] = cx + 2 * scale;
			yVertex[1] = cy;
			xVertex[2] = cx + 3 * scale;
			yVertex[2] = cy + SQRT3 * scale;
			xVertex[3] = cx + 2 * scale;
			yVertex[3] = cy + 2 * SQRT3 * scale;
			xVertex[4] = cx;
			yVertex[4] = cy + 2 * SQRT3 * scale;
			xVertex[5] = cx - 1 * scale;
			yVertex[5] = cy + SQRT3 * scale;
		}

		hexagon = makePolygon(6, xVertex, yVertex, true);
		rectBound = hexagon.getBounds();

		center = new Point((int) ((xVertex[2] + xVertex[5]) / 2),
				(int) ((yVertex[0] + yVertex[3]) / 2));
		Point2D.Double center2D = new Point2D.Double((xVertex[2] + xVertex[5]) / 2.0,
				(yVertex[0] + yVertex[3]) / 2.0);

		final double innerScale = 0.8;
		AffineTransform at = AffineTransform.getScaleInstance(innerScale,
				innerScale);
		innerHexagon = (GeneralPath) hexagon.createTransformedShape(at);

		// Translate innerHexagon to make it concentric.
		Rectangle2D innerBounds = innerHexagon.getBounds2D();
		Point2D.Double innerCenter = new Point2D.Double(innerBounds.getX()
				+ innerBounds.getWidth() / 2.0, innerBounds.getY()
				+ innerBounds.getHeight() / 2.0);
		at = AffineTransform.getTranslateInstance(center2D.getX()
				- innerCenter.getX(), center2D.getY() - innerCenter.getY());
		innerHexagon.transform(at);

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
        currentTileId = model.getCurrentTile().getId();
        currentTileOrientation = model.getCurrentTileRotation();
    	currentGUITile = new GUITile(currentTileId, model);
		currentGUITile.setRotation(currentTileOrientation);
		setToolTip();
		
		if (StatusWindow.useObserver) {
			model.addObserver(this);
		}


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
		if (selected)
		{
			currentGUITile.setScale(SELECTED_SCALE);
		}
		else
		{
			currentGUITile.setScale(NORMAL_SCALE);
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

		Color terrainColor = Color.WHITE;
		if (isSelected())
		{
			g2.setColor(highlightColor);
			g2.fill(hexagon);

			g2.setColor(terrainColor);
			g2.fill(innerHexagon);

			g2.setColor(Color.black);
			g2.draw(innerHexagon);
		}

		paintOverlay(g2);
		paintToken(g2);

		FontMetrics fontMetrics = g2.getFontMetrics();
		if (getHexModel().getTileCost() > 0 && originalTileId == currentTileId)
		{
			g2.drawString("$" + getHexModel().getTileCost(),
					rectBound.x + (rectBound.width - fontMetrics.stringWidth(
								Integer.toString(getHexModel().getTileCost())))	* 3 / 5,
					rectBound.y + ((fontMetrics.getHeight() + rectBound.height) * 9 / 15));
		}

		Map homes;
		if ((homes = getHexModel().getHomes()) != null)
		{
		    StringBuffer b = new StringBuffer();
		    for (Iterator it = homes.keySet().iterator(); it.hasNext(); ) {
		        
		        PublicCompanyI co = (PublicCompanyI) it.next();

				if (!co.hasStarted() && !co.hasFloated())
				{
				    if (b.length() > 0) b.append(",");
				    b.append(co.getName());
				}
		    }
		    String label = b.toString();
			g2.drawString(label,
					rectBound.x + (rectBound.width - fontMetrics.stringWidth(label)) * 1 / 2,
					rectBound.y + ((fontMetrics.getHeight() + rectBound.height) * 3 / 10));
		}

		if (getHexModel().isBlocked())
		{
			ArrayList privates = (ArrayList) Game.getCompanyManager()
					.getAllPrivateCompanies();
			Iterator pIT = privates.iterator();

			while (pIT.hasNext())
			{
				PrivateCompany p = (PrivateCompany) pIT.next();
				ArrayList blocked = (ArrayList) p.getBlockedHexes();
				Iterator bIT = blocked.iterator();

				while (bIT.hasNext())
				{
					MapHex hex = (MapHex) bIT.next();

					if (getHexModel().equals(hex))
					{
						g2.drawString("(" + p.getName() + ")",
								rectBound.x + (rectBound.width - fontMetrics.stringWidth("("
												+ p.getName() + ")")) * 1 / 2,
								rectBound.y + ((fontMetrics.getHeight() + rectBound.height) * 7 / 15));
					}
				}
			}
		}

	}

	private void paintOverlay(Graphics2D g2)
	{
		if (provisionalGUITile != null)
		{
			provisionalGUITile.paintTile(g2, center.x, center.y);
		}
		else
		{
			currentGUITile.paintTile(g2, center.x, center.y);
		}
	}

	private void paintToken(Graphics2D g2)
	{
		if (getHexModel().getStations().size() > 1)
		{
			paintSplitStations(g2);
			return;
		}

		int numTokens = getHexModel().getTokens(0).size();
		ArrayList tokens = (ArrayList) getHexModel().getTokens(0);

		for (int i = 0; i < tokens.size(); i++)
		{
			PublicCompany co = (PublicCompany) ((BaseToken)tokens.get(i)).getCompany();
			Point origin = getTokenOrigin(numTokens, i, 1, 0);
			drawToken(g2, co, origin);
		}
	}

	private void paintSplitStations(Graphics2D g2)
	{
		int numStations = getHexModel().getStations().size();
		int numTokens;
		ArrayList tokens;
		Point origin;
		PublicCompany co;

		for (int i = 0; i < numStations; i++)
		{
			numTokens = getHexModel().getTokens(i).size();
			tokens = (ArrayList) getHexModel().getTokens(i);

			for (int j = 0; j < tokens.size(); j++)
			{
				origin = getTokenOrigin(numTokens, j, numStations, i);
				co = (PublicCompany) ((BaseToken)tokens.get(j)).getCompany();
				drawToken(g2, co, origin);
			}
		}
	}

	private void drawToken(Graphics2D g2, PublicCompany co, Point origin)
	{
		Dimension size = new Dimension(40, 40);

		GUIToken token = new GUIToken(co.getFgColour(),
				co.getBgColour(),
				co.getName(),
				origin.x,
				origin.y,
				15);
		token.setBounds(origin.x, origin.y, size.width, size.height);

		token.drawToken(g2);
	}

	/*
	 * Beware! Here be dragons! And nested switch/case statements! The horror!
	 * 
	 * NOTE: CurrentFoo starts at 0 TotalFoo starts at 1
	 */
	private Point getTokenOrigin(int numTokens, int currentToken,
			int numStations, int currentStation)
	{
		Point p = new Point();

		switch (numStations)
		{
			// Single city, variable number of token spots.
			// This is the most common scenario.
			case 1:
				switch (numTokens)
				{
					// Single dot, basic hex
					case 1:
						p.x = (center.x - 9);
						p.y = (center.y - 9);
						return p;
					// Two dots, common green hex upgrade
					case 2:
						// First token
						if (currentToken == 0)
						{
							p.x = (center.x - 5);
							p.y = (center.y - 9);
							return p;
						}
						// Second Token
						else
						{
							p.x = (center.x - 17);
							p.y = (center.y - 9);
							return p;
						}
					// Three dots, common brown hex upgrade
					case 3:
						// First token
						if (currentToken == 0)
						{
							p.x = (center.x - 14);
							p.y = (center.y - 3);
							return p;
						}
						// Second Token
						else if (currentToken == 1)
						{
							p.x = (center.x - 5);
							p.y = (center.y - 3);
							return p;
						}
						// Third Token
						else
						{
							p.x = (center.x - 9);
							p.y = (center.y - 14);
							return p;
						}
					// Four dots, slightly less common brown hex upgrade
					case 4:
					case 5:
					case 6:
					default:
						return center;
				}
			// Big Cities, two stations.
			// usually only one or two token spots per station
			case 2:
				// First Station... (left side)
				if (currentStation == 0)
				{
					switch (numTokens)
					{
						case 1:
							p.x = (center.x - 14);
							p.y = (center.y + 3);
							return p;
						case 2:
							// First token
							if (currentToken == 0)
							{
								p.x = (center.x - 20);
								p.y = (center.y - 3);
								return p;
							}
							// Second Token
							else
							{
								p.x = (center.x - 10);
								p.y = (center.y + 9);
								return p;
							}
						default:
							return center;
					}
				}
				// Second Station... (right side)
				else
				{
					switch (numTokens)
					{
						case 1:
							p.x = (center.x - 1);
							p.y = (center.y - 20);
							return p;
						case 2:
							// First token
							if (currentToken == 0)
							{
								p.x = (center.x - 6);
								p.y = (center.y - 23);
								return p;
							}
							// Second Token
							else
							{
								p.x = (center.x + 6);
								p.y = (center.y - 12);
								return p;
							}
						default:
							return center;
					}
				}
			case 3:
			// TODO: We'll deal with the 3 station scenario later... much later.

			// Known cases: 3 single token stations,
			// 2 double token station and a single token station
			default:
				return center;
		}
	}

	public void rotateTile()
	{
		if (provisionalGUITile != null)
		{
			provisionalGUITile.rotate(1, currentGUITile);
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
	 * @return Returns the currentTile.
	 */
	public TileI getCurrentTile()
	{
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

	public String getToolTip()
	{
		return toolTip;
	}

	protected void setToolTip()
	{
		StringBuffer tt = new StringBuffer("<html>");
		tt.append("<b>Hex</b>: ").append(hexName);
		// The next line is a temporary development aid, that can be removed
		// later.
		tt.append("  <small>(")
				.append(model.getX())
				.append(",")
				.append(model.getY())
				.append(")</small>");
		tt.append("<br><b>Tile</b>: ").append(currentTile.getId());
		if (currentTile.hasStations())
		{
			Iterator it = currentTile.getStations().iterator();
			Station st;
			while (it.hasNext())
			{
				st = (Station) it.next();
				tt.append("<br>  ").append(st.getType()).append(": value ");
				if (model.hasOffBoardValues()) {
				    tt.append(model.getCurrentOffBoardValue()).append(" [");
				    int[] values = model.getOffBoardValues();
				    for (int i=0; i<values.length; i++) {
				        if (i>0) tt.append(",");
				        tt.append(values[i]);
				    }
				    tt.append("]");
				} else {
				    tt.append(st.getValue());
				}
				if (st.getValue() > 0 && st.getBaseSlots() > 0)
				{
					tt.append(", ").append(st.getBaseSlots()).append(" slots");
				}
			}
		}
		String upgrades = currentTile.getUpgradesString(model);
		if (upgrades.equals(""))
		{
			tt.append("<br>No upgrades");
		}
		else
		{
			tt.append("<br><b>Upgrades</b>: ").append(upgrades);
			if (model.getTileCost() > 0)
				tt.append("<br>Upgrade cost: "
						+ Bank.format(model.getTileCost()));
		}

		if (getHexModel().getDestinations() != null) {
			tt.append("<br><b>Destination</b>:");
			for (Iterator it = getHexModel().getDestinations().iterator(); it.hasNext(); ) {
			    tt.append (" ");
				tt.append(((PublicCompanyI)it.next()).getName());
			}
		}
		toolTip = tt.toString();
	}

	public boolean dropTile(int tileId)
	{
		provisionalGUITile = new GUITile(tileId, model);
		/* Check if we can find a valid orientation of this tile */
		if (provisionalGUITile.rotate(0, currentGUITile))
		{
			/* If so, accept it */
			provisionalGUITile.setScale(SELECTED_SCALE);
			toolTip = "Click to rotate";
			return true;
		}
		else
		{
			/* If not, refuse it */
			provisionalGUITile = null;
			return false;
		}

	}

	public void removeTile()
	{
		provisionalGUITile = null;
		setSelected(false);
		setToolTip();
	}

	public boolean canFixTile () {
		return provisionalGUITile != null;
	}
	
	public TileI getProvisionalTile () {
		return provisionalGUITile.getTile();
	}
	
	public int getProvisionalTileRotation() {
		return provisionalGUITile.getRotation();
	}
	
	public void fixTile () {
		
		setSelected (false);
		setToolTip();
	}

	public void removeToken()
	{
		provisionalGUIToken = null;
		setSelected(false);
		setToolTip();
	}

    public void fixToken () {
        setSelected (false);
    }

	/** Needed to satisfy the ViewObject interface. Currently not used. */
	public void deRegister()
	{
		if (model != null && StatusWindow.useObserver)
			model.deleteObserver(this);
	}
	
	public ModelObject getModel() {
	    return model;
	}
	
    // Required to implement Observer pattern.
    // Used by Undo/Redo
	public void update (Observable observable, Object notificationObject) {
	    
	    if (notificationObject instanceof String) {
	        // The below code so far only deals with tile lay undo/redo.
	        // Tokens still to do  
	        String[] elements = ((String)notificationObject).split("/");
	        currentTileId = Integer.parseInt(elements[0]);
	        currentTileOrientation = Integer.parseInt(elements[1]);
	        currentGUITile = new GUITile(currentTileId, model);
	        currentGUITile.setRotation(currentTileOrientation);
	        currentTile = currentGUITile.getTile();
	        
			GameUIManager.getMapPanel().getMap().repaint(getBounds());
			
	        provisionalGUITile = null;

	        log.debug ("GUIHex "+model.getName()+" updated: new tile "+currentTileId+"/"+currentTileOrientation);
			GameUIManager.orWindow.updateStatus();
	    }
	}

}
