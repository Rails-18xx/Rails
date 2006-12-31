package game;

import game.model.ModelObject;
import game.move.MoveSet;
import game.move.TileMove;

import java.util.*;
import java.util.regex.*;
import org.w3c.dom.*;

import util.Util;
import util.XmlUtils;

/**
 * Represents a Hex on the Map from the Model side.
 * 
 * <p>
 * <b>Tile orientations</b>. Tiles can be oriented NS or EW; the directions
 * refer to the "flat" hex sides.
 * <p>
 * The term "orientation" is also used to indicate the amount of rotation (in 60
 * degree units) from the standard orientation of the tile. The orientation
 * numbers are indicated in the below picture for an NS-oriented tile:
 * <p>
 * <code>
 * 
 *       ____3____            
 *      /         \
 *     2           4
 *    /     NS      \
 *    \             /
 *     1           5
 *      \____0____/
 * </code>
 * <p>
 * For EW-oriented tiles the above picture should be rotated 30 degrees
 * clockwise.
 */
public class MapHex extends ModelObject implements ConfigurableComponentI, TokenHolderI
{

	public static final int EW = 0;
	public static final int NS = 1;
	protected static int tileOrientation;
	protected static boolean lettersGoHorizontal;
	protected static boolean letterAHasEvenNumbers;

	private static final String[] ewOrNames = { "SW", "W", "NW", "NE", "E",
			"SE" };
	private static final String[] nsOrNames = { "S", "SW", "NW", "N", "NE",
			"SE" };

	// Coordinates as used in the ui.hexmap package
	protected int x;
	protected int y;

	// Map coordinates as printed on the game board
	protected String name;
	protected int row;
	protected int column;
	protected int letter;
	protected int number;
	protected String tileFileName;
	protected int preprintedTileId;
	protected int preprintedTileOrientation;
	protected TileI currentTile;
	protected int currentTileRotation;
	protected int tileCost;
	//protected int preferredCity;
	//protected PublicCompany companyHome = new PublicCompany();
	//protected PublicCompany companyDestination = new PublicCompany();
	//protected String companyHomeName;
	//protected String companyDestinationName;

	/** Neighbouring hexes <i>to which track may be laid</i>. */
	protected MapHex[] neighbours = new MapHex[6];
	
	/** Values if this is an off-board hex */
	protected int[] offBoardValues = null;

	/*
	 * Temporary storage for impassable hexsides. Once neighbours has been set
	 * up, this attribute is no longer used. Only the black or blue bars on the
	 * map need be specified, and each one only once. Impassable non-track sides
	 * of "offboard" (red) and "fixed" (grey or brown) preprinted tiles will be
	 * derived and need not be specified.
	 */
	protected String impassable = null;

	protected ArrayList stations;
	protected boolean hasTokens;

	protected boolean isBlocked = false;
	
	protected Map homes;
	protected List destinations;

	public MapHex()
	{
	}

	/**
	 * @see game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
	 */
	public void configureFromXML(Element el) throws ConfigurationException
	{
		NamedNodeMap nnp = el.getAttributes();
		Pattern namePattern = Pattern.compile("(\\D)(\\d+)");

		name = XmlUtils.extractStringAttribute(nnp, "name");
		Matcher m = namePattern.matcher(name);
		if (!m.matches())
		{
			throw new ConfigurationException("Invalid name format: " + name);
		}
		letter = m.group(1).charAt(0);
		try
		{
			number = Integer.parseInt(m.group(2));
		}
		catch (NumberFormatException e)
		{
			// Cannot occur!
		}

		/*
		 * Translate hex names (as on the board) to coordinates used for
		 * drawing.
		 */
		if (lettersGoHorizontal)
		{
			row = number;
			column = letter - '@';
			if (tileOrientation == MapHex.EW)
			{
				// Tiles with flat EW sides, letters go horizontally.
				// Example: 1841 (NOT TESTED, PROBABLY WRONG).
				x = column;
				y = row / 2;
			}
			else
			{
				// Tiles with flat NS sides, letters go horizontally.
				// Tested for 1856.
				x = column;
				y = (row + 1) / 2;
			}
		}
		else
		// letters go vertical (normal case)
		{
			row = letter - '@';
			column = number;
			if (tileOrientation == MapHex.EW)
			{
				// Tiles with flat EW sides, letters go vertically.
				// Most common case.
				// Tested for 1830 and 1870.
				x = (column + (letterAHasEvenNumbers ? 1 : 0)) / 2;
				y = row;
			}
			else
			{
				// Tiles with flat NS sides, letters go vertically.
				// Tested for 18AL.
				x = column;
				y = (row + 1) / 2;
			}
		}

		String sTileId = XmlUtils.extractStringAttribute(nnp, "tile");
		if (sTileId != null)
		{
			try
			{
				preprintedTileId = Integer.parseInt(sTileId);
			}
			catch (NumberFormatException e)
			{
				throw new ConfigurationException("Invalid tile ID: " + sTileId);
			}
		}
		else
		{
			preprintedTileId = -999;
		}

		preprintedTileOrientation = XmlUtils.extractIntegerAttribute(nnp,
				"orientation",
				0);

		currentTile = TileManager.get().getTile(preprintedTileId);
		currentTileRotation = preprintedTileOrientation;
		impassable = XmlUtils.extractStringAttribute(nnp, "impassable");
		tileCost = XmlUtils.extractIntegerAttribute(nnp, "cost", 0);
		//preferredCity = XmlUtils.extractIntegerAttribute(nnp,
		//		"preferredCity",
		//		0);
		//companyHomeName = XmlUtils.extractStringAttribute(nnp, "home");
		//companyDestinationName = XmlUtils.extractStringAttribute(nnp,
		//		"destination");

		// We need completely new objects, not just references to the Tile's
		// stations.
		stations = new ArrayList();
		for (int i = 0; i < currentTile.getStations().size(); i++)
		{
			// sid, type, value, slots
			Station s = (Station) currentTile.getStations().get(i);
			stations.add(new Station(s));  // Clone it
		}

		// Off-board renevue values
		String valueString = XmlUtils.extractStringAttribute(nnp, "value");
		if (Util.hasValue (valueString)) {
		    String[] values = valueString.split(",");
		    offBoardValues = new int[values.length];
		    for (int i=0; i<values.length; i++) {
		        try {
		            offBoardValues[i] = Integer.parseInt(values[i]);
		        } catch (NumberFormatException e) {
		            throw new ConfigurationException 
		            	("Invalid off-board value "+values[i]+" for hex "+name, e);
		        }
		    }
		}

	}

	public boolean isNeighbour(MapHex neighbour, int direction)
	{
		/*
		 * Various reasons why a bordering hex may not be a neighbour in the
		 * sense that track may be laid to that border:
		 */
		/* 1. The hex side is marked "impassable" */
		if (impassable != null && impassable.indexOf(neighbour.getName()) > -1)
			return false;
		/*
		 * 2. The preprinted tile on this hex is offmap or fixed and has no
		 * track to this side.
		 */
		TileI tile = neighbour.getCurrentTile();
		if (!tile.isUpgradeable()
				&& !tile.hasTracks(3 + direction
						- neighbour.getCurrentTileRotation()))
			return false;

		return true;
	}

	public static void setTileOrientation(int orientation)
	{
		tileOrientation = orientation;
	}

	public static int getTileOrientation()
	{
		return tileOrientation;
	}

	public static void setLettersGoHorizontal(boolean b)
	{
		lettersGoHorizontal = b;
	}

	/**
	 * @return Returns the letterAHasEvenNumbers.
	 */
	public static boolean hasLetterAEvenNumbers()
	{
		return letterAHasEvenNumbers;
	}

	/**
	 * @param letterAHasEvenNumbers
	 *            The letterAHasEvenNumbers to set.
	 */
	public static void setLetterAHasEvenNumbers(boolean letterAHasEvenNumbers)
	{
		MapHex.letterAHasEvenNumbers = letterAHasEvenNumbers;
	}

	/**
	 * @return Returns the lettersGoHorizontal.
	 */
	public static boolean isLettersGoHorizontal()
	{
		return lettersGoHorizontal;
	}

	public static String getOrientationName(int orientation)
	{

		if (tileOrientation == EW)
		{
			return ewOrNames[orientation % 6];
		}
		else
		{
			return nsOrNames[orientation % 6];
		}
	}

	/* ----- Instance methods ----- */

	/**
	 * @return Returns the column.
	 */
	public int getColumn()
	{
		return column;
	}

	/**
	 * @return Returns the row.
	 */
	public int getRow()
	{
		return row;
	}

	public String getName()
	{
		return name;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	/**
	 * @return Returns the preprintedTileId.
	 */
	public int getPreprintedTileId()
	{
		return preprintedTileId;
	}

	/**
	 * @return Returns the image file name for the tile.
	 */
	public String getTileFileName()
	{
		return tileFileName;
	}

	/**
	 * @return Returns the preprintedTileOrientation.
	 */
	public int getPreprintedTileOrientation()
	{
		return preprintedTileOrientation;
	}

	public void setNeighbor(int orientation, MapHex neighbour)
	{
		orientation %= 6;
		neighbours[orientation] = neighbour;
	}

	public MapHex getNeighbor(int orientation)
	{
		return neighbours[orientation % 6];
	}

	public MapHex[] getNeighbors()
	{
		return neighbours;
	}

	public boolean hasNeighbour(int orientation)
	{

		while (orientation < 0)
			orientation += 6;
		return neighbours[orientation % 6] != null;
	}

	public TileI getCurrentTile()
	{
		return currentTile;
	}

	public int getCurrentTileRotation()
	{
		return currentTileRotation;
	}

	/** Look for the Hex matching the Label in the terrain static map */
	/* EV: useful, but needs to be rewritten */
	public static MapHex getHexByLabel(String terrain, String label)
	{
		/*
		 * int x = 0; int y = Integer.parseInt(new String(label.substring(1)));
		 * switch (label.charAt(0)) { case 'A': case 'a': x = 0; break;
		 * 
		 * case 'B': case 'b': x = 1; break;
		 * 
		 * case 'C': case 'c': x = 2; break;
		 * 
		 * case 'D': case 'd': x = 3; break;
		 * 
		 * case 'E': case 'e': x = 4; break;
		 * 
		 * case 'F': case 'f': x = 5; break;
		 * 
		 * case 'X': case 'x': // entrances GUIHex[] gameEntrances = (GUIHex[])
		 * entranceHexes.get(terrain); return gameEntrances[y].getMapHexModel();
		 * 
		 * default: Log.error("Label " + label + " is invalid"); } y = 6 - y -
		 * (int) Math.abs(((x - 3) / 2)); GUIHex[][] correctHexes = (GUIHex[][])
		 * terrainH.get(terrain); return correctHexes[x][y].getMapHexModel();
		 */
		return null;
	}

	public int getTileCost()
	{
		return tileCost;
	}

	//public CompanyI getCompanyHome()
	//{
	//	return companyHome;
	//}

	//public CompanyI getCompanyDestination()
	//{
	//	return companyDestination;
	//}

	/** Prepare a tile upgrade.
	 * The actual tile replacement is done in replaceTile(), via a TileMove object.
	 */
	public void upgrade(TileI newTile, int newOrientation)
	{
	    MoveSet.add(new TileMove (this, currentTile, currentTileRotation,
	            newTile, newOrientation));
	    /*
		if (currentTile != null)
			currentTile.remove(this);
		
		newTile.lay(this);

		// Move tokens from old station list to new station list.
		// Merge lists if necessary.		
		moveTokens(newTile);
		
		currentTile = newTile;
		currentTileRotation = newOrientation;
		*/

		// Further consequences to be processed here, e.g. new routes etc.
	}
	
	/** Execute a tile replacement.
	 * This method should only be called from TileMove objects.
	 * It is also used to undo tile lays.
	 * @param oldTile The tile to be replaced (only used for validation).
	 * @param newTile The new tile to be laid on this hex.
	 * @param newTileOrientation The orientation of the new tile (0-5).*/
	public void replaceTile (TileI oldTile, TileI newTile, int newTileOrientation) {
	    
	    if (oldTile != currentTile) {
	        new Exception ("ERROR! Hex "+name+" wants to replace tile #"+oldTile.getName()
	                +" but has tile #"+currentTile.getName()+"!")
	            .printStackTrace();
	    }
		if (currentTile != null) {
			currentTile.remove(this);
		}
		
		System.out.println("On hex "+name+" replacing tile "
		        +currentTile.getName()+"/"+currentTileRotation
		        +" by "+newTile.getName()+"/"+newTileOrientation);
		
		newTile.lay(this);

		// Move tokens from old station list to new station list.
		// Merge lists if necessary.		
		moveTokens(newTile);
		
		currentTile = newTile;
		currentTileRotation = newTileOrientation;
		
		update(); // To notify ViewObject (Observer)

		/* TODO: Further consequences to be processed here, e.g. new routes etc. */
	}

	//public int getPreferredHomeCity()
	//{
	//	return preferredCity;
	//}

	public boolean addToken(TokenHolderI company)
	{
		return addToken(company, null);
	}

	public boolean addToken(TokenHolderI company, int station)
	{
		return addToken(company, (Station)stations.get(station));
	}

	public boolean addToken(TokenHolderI company, Station station)
	{
	    if (station == null) station = (Station)stations.get(0);
		if (station.addToken(company))
		{
			company.addToken(this);
			hasTokens = true;
			return true;
		}
		else
			return false;
	}
	
	public boolean hasTokenSlotsLeft (int station) {
	    if (station < stations.size()) {
		    return hasTokenSlotsLeft ((Station)stations.get(station));
	    } else {
	        System.out.println("ERROR: invalid station "+station+", max is "+(stations.size()-1));
	        return false;
	    }
	}
	
	public boolean hasTokenSlotsLeft (Station station) {
	    return station != null && station.hasTokenSlotsLeft();
	}

	/**
	 * @return ArrayList of all tokens in all stations merged into a single
	 *         list.
	 * 
	 * To get ArrayList of tokens from stations, use explicit station number
	 */
	public List getTokens()
	{
		if (stations.size() > 1)
		{
			ArrayList tokens = new ArrayList();
			for (int i = 0; i < stations.size(); i++)
			{
				tokens.addAll(getTokens(i));
			}
			return tokens;
		}
		else
			return getTokens(0);
	}

	public List getTokens(int stationNumber)
	{
		if (stations.size() > 0)
		{
			return (ArrayList) ((Station) stations.get(stationNumber)).getTokens();
		}
		else
		{
			return new ArrayList();
		}
	}

	public boolean hasTokens()
	{
		return hasTokens;
	}

	public boolean removeToken(TokenHolderI company)
	{
		for (int i = 0; i < stations.size(); i++)
		{
			if (((Station) stations.get(i)).removeToken(company))
			{
				// XXX: Could be buggy if loop exits before hitting all stations
				// in the tile.
				if (!((Station) stations.get(i)).hasTokens())
					hasTokens = false;
				return true;
			}
		}
		return false;
	}

	private void moveTokens(TileI newTile)
	{
		ArrayList movedTokens = new ArrayList();
		ArrayList newStations = (ArrayList) newTile.getStations();
		
		ArrayList co = (ArrayList) Game.getCompanyManager()
				.getAllPublicCompanies();
		Iterator coIT = co.iterator();

		// Flip through each company's list of tokens
		while (coIT.hasNext())
		{
			PublicCompanyI c = (PublicCompanyI) coIT.next();

			if (c.hasTokens())
			{
				ArrayList t = (ArrayList) c.getTokens();
				Iterator tokIT = t.iterator();

				while (tokIT.hasNext())
				{
					MapHex hex = (MapHex) tokIT.next();
					if (hex.equals(this))
					{
						// If a company has a token in this hex, make a note of it.
						movedTokens.add(c);
					}
				}
			}
		}
		
		if(newStations.size() == 1)
		{
			//This is here because Java sucks giant flaming donkey balls.
			//To avoid passing a reference of the Tile's station, we create
			//A new station with the same values as the newTile's station.
			Station s = new Station((Station)newTile.getStations().get(0));
			stations.clear();
			stations.add(s);
			((Station)stations.get(0)).setTokens(movedTokens);
		}
		else
		{
			for(int i=0; i < newTile.getStations().size(); i++)
			{
				Station newStation = (Station) newTile.getStations().get(i);
				Station oldStation = (Station) stations.get(i);
				Station s = new Station(newStation);
				
				for(int j=0; j < movedTokens.size(); j++)
				{
					if(oldStation.getTokens().contains(movedTokens.get(j)))
					{
						s.getTokens().add(movedTokens.get(j));
					}
				}
				
				stations.remove(i);
				stations.add(i, s); 
			}
		}

	}

	public List getStations()
	{
		return stations;
	}
	
	public void addHome (PublicCompanyI company, Station station) {
	    if (homes == null) homes = new HashMap();
	    homes.put (company, station);
	}
	
	public Map getHomes () {
	    return homes;
	}
	
	public void addDestination (PublicCompanyI company) {
	    if (destinations == null) destinations = new ArrayList();
	    destinations.add (company);
	}
	
	public List getDestinations () {
	    return destinations;
	}

	//public String getCompanyDestinationName()
	//{
	//	return companyDestinationName;
	//}

	//public String getCompanyHomeName()
	//{
	//	return companyHomeName;
	//}

	/**
	 * Necessary mechanism for delaying assignment of companyHome until after
	 * all of the proper elements of the XML have been loaded.
	 * 
	 * Called by MapManager.assignHomesAndDestinations()
	 */
	//protected void assignHome() throws NullPointerException
	//{
	//	try
	//	{
	//		companyHome = (PublicCompany) Game.getCompanyManager()
	//				.getPublicCompany(companyHomeName);
	//	}
	//	catch (NullPointerException e)
	//	{
	//		throw e;
	//	}
	//}

	/**
	 * Necessary mechanism for delaying assignment of companyDestination until
	 * after all of the proper elements of the XML have been loaded.
	 * 
	 * Called by MapManager.assignHomesAndDestinations()
	 */
	//protected void assignDestination() throws NullPointerException
	//{
	//	try
	//	{
	//		companyDestination = (PublicCompany) Game.getCompanyManager()
	//				.getPublicCompany(companyDestinationName);
	//	}
	//	catch (NullPointerException e)
	//	{
	//		throw e;
	//	}
	//}

	/**
	 * @return Returns the isBlocked.
	 */
	public boolean isBlocked()
	{
		return isBlocked;
	}

	/**
	 * @param isBlocked
	 *            The isBlocked to set.
	 */
	public void setBlocked(boolean isBlocked)
	{
		this.isBlocked = isBlocked;
	}

	public boolean isUpgradeableNow()
	{
		if (isBlocked) {
		    System.out.println("Hex "+name+" is blocked");
			return false;
		}
		if (currentTile != null) {
			if (currentTile.isUpgradeable()) {
			    return true;
			} else {
			    System.out.println("Hex "+name+" tile "+currentTile.getName()
			            +" is not upgradable now");
			    return false;
			}
		}
		System.out.println("No tile on hex "+name);
		return false;
	}
	
	public boolean hasOffBoardValues () {
	    return offBoardValues != null;
	}
	
	public int[] getOffBoardValues () {
	    return offBoardValues;
	}
	
	public int getCurrentOffBoardValue () {
	    if (hasOffBoardValues()) {
	        return offBoardValues[Math.min(offBoardValues.length, 
	                PhaseManager.getInstance().getCurrentPhase().getOffBoardRevenueStep()) - 1];
	    } else {
	        return 0;
	    }
	}

	public boolean equals(MapHex hex)
	{
		if (hex.getName().equals(getName()) && hex.row == row
				&& hex.column == column)
			return true;
		return false;
	}
	
	public String toString()
	{
		return name + " (" + row + "," + column + ")";
	}
	
	/**
	 * The string sent to the GUIHex as it is notified.
	 * Format is tile/orientation. 
	 * @TODO include tokens??
	 */
	public Object getNotificationObject() {
	    return currentTile.getName() + "/" + currentTileRotation; 
	}
}
