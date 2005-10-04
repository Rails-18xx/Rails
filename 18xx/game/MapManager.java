/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/MapManager.java,v 1.5 2005/10/04 21:38:18 evos Exp $
 * 
 * Created on 08-Aug-2005
 * Change Log:
 */
package game;

import java.util.*;

import org.w3c.dom.*;

import util.XmlUtils;

/**
 * @author Erik Vos
 */
public class MapManager implements ConfigurableComponentI
{

	private String mapUIClassName = null;
	private static MapManager instance = null;

	// The next attributes are duplicates in MapHex. We'll see what we really
	// need.
	protected static int tileOrientation;
	protected static boolean lettersGoHorizontal;
	protected static boolean letterAHasEvenNumbers;

	protected MapHex[][] hexes;
	protected Map mHexes = new HashMap();
	
	protected static final int[] xDeltaNS = new int[] {0, -1, -1, 0, +1, +1};
	protected static final int[] yXEvenDeltaNS = new int[] {+1, 0, -1, -1, -1, 0};
	protected static final int[] yXOddDeltaNS = new int[] {+1, +1, 0, -1, 0, +1};
	protected static final int[] xYEvenDeltaEW = new int[] {-1, -1, -1, 0, +1, 0};
	protected static final int[] xYOddDeltaEW = new int[] {0, -1, 0, +1, +1, +1};
	protected static final int[] yDeltaEW = new int[] {+1, 0, -1, -1, 0, +1};

	public MapManager()
	{
		instance = this;
	}

	/**
	 * @see game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
	 */
	public void configureFromXML(Element el) throws ConfigurationException
	{
		NamedNodeMap nnp = el.getAttributes();
		mapUIClassName = XmlUtils.extractStringAttribute(nnp, "mapClass");
		if (mapUIClassName == null)
		{
			throw new ConfigurationException("Map class name missing");
		}

		String attr = XmlUtils.extractStringAttribute(nnp, "tileOrientation");
		if (attr == null)
			throw new ConfigurationException("Map orientation undefined");
		if (attr.equals("EW"))
		{
			tileOrientation = MapHex.EW;
			MapHex.setTileOrientation(MapHex.EW);
		}
		else if (attr.equals("NS"))
		{
			tileOrientation = MapHex.NS;
			MapHex.setTileOrientation(MapHex.NS);
		}
		else
		{
			throw new ConfigurationException("Invalid tile orientation: "
					+ attr);
		}

		attr = XmlUtils.extractStringAttribute(nnp, "letterOrientation");
		if (attr.equals("horizontal"))
		{
			lettersGoHorizontal = true;
		}
		else if (attr.equals("vertical"))
		{
			lettersGoHorizontal = false;
		}
		else
		{
			throw new ConfigurationException("Invalid letter orientation: "
					+ attr);
		}
		MapHex.setLettersGoHorizontal(lettersGoHorizontal);

		attr = XmlUtils.extractStringAttribute(nnp, "even");
		letterAHasEvenNumbers = ((int) (attr.toUpperCase().charAt(0) - 'A')) % 2 == 0;
		MapHex.setLetterAHasEvenNumbers(letterAHasEvenNumbers);

		NodeList children = el.getElementsByTagName("Hex");
		Element mapElement;
		MapHex hex;
		int maxX = 0;
		int maxY = 0;
		for (int i = 0; i < children.getLength(); i++)
		{
			mapElement = (Element) children.item(i);
			hex = new MapHex();
			hex.configureFromXML(mapElement);
			mHexes.put(hex.getName(), hex);
			maxX = Math.max(maxX, hex.getX());
			maxY = Math.max(maxY, hex.getY());
		}

		hexes = new MapHex[1 + maxX][1 + maxY];
		Iterator it = mHexes.keySet().iterator();
		while (it.hasNext())
		{
			hex = (MapHex) mHexes.get((String) it.next());
			hexes[hex.getX()][hex.getY()] = hex;
		}

		// Initialise the neighbours
		/** 
		 * TODO: impassable hexsides.
		 * TODO: blank sides of fixed and offboard preprinted tiles. 
		 */ 
		int i, j, k, dx, dy;
		MapHex nb;
		for (i=0; i<=maxX; i++) {
		    for (j=0; j<=maxY; j++) {
		        if ((hex = hexes[i][j]) == null) continue;
		        for (k=0; k<6; k++) {
		            if (tileOrientation == MapHex.EW) {
		                dx = (j%2 == 0 ? xYEvenDeltaEW[k] : xYOddDeltaEW[k]);
		                dy = yDeltaEW[k];
		            } else {
		                dx = xDeltaNS[k];
		                dy = (i%2 == 0 ? yXEvenDeltaNS[k] : yXOddDeltaNS[k]);
		            }
			        if (i+dx>=0 && i+dx<=maxX && j+dy>=0 && j+dy<=maxY
			                && (nb = hexes[i+dx][j+dy]) != null) {
			            hex.setNeighbour(k, nb);
			            nb.setNeighbour(k+3, hex);
			            /*
			            System.out.println("Setting neighbours["+k+"]: "+hex.getName()
			                    +"("+i+","+j+") and "+nb.getName()+"("+(i+dx)+","+(j+dy)+")");
			            */
			        }
		            
		        }
		    }
		}
	}

	public static MapManager getInstance()
	{
		return instance;
	}

	/**
	 * @return Returns the letterAHasEvenNumbers.
	 */
	public static boolean letterAHasEvenNumbers()
	{
		return letterAHasEvenNumbers;
	}

	/**
	 * @return Returns the lettersGoHorizontal.
	 */
	public static boolean lettersGoHorizontal()
	{
		return lettersGoHorizontal;
	}

	/**
	 * @return Returns the tileOrientation.
	 */
	public static int getTileOrientation()
	{
		return tileOrientation;
	}

	/**
	 * @return Returns the hexes.
	 */
	public MapHex[][] getHexes()
	{
		return hexes;
	}

	/**
	 * @return Returns the mapUIClassName.
	 */
	public String getMapUIClassName()
	{
		return mapUIClassName;
	}
}
