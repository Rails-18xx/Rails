/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/MapHex.java,v 1.13 2007/12/23 16:30:37 evos Exp $ */
package rails.game;


import java.util.*;
import java.util.regex.*;

import org.apache.log4j.Logger;

import rails.game.model.ModelObject;
import rails.game.move.Moveable;
import rails.game.move.TileMove;
import rails.util.Tag;


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
public class MapHex extends ModelObject 
	implements ConfigurableComponentI, StationHolderI, TokenHolderI
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

	// Coordinates as used in the rails.ui.swing.hexmap package
	protected int x;
	protected int y;

	// Map coordinates as printed on the rails.game board
	protected String name;
	protected int row;
	protected int column;
	protected int letter;
	protected int number;
	protected String tileFileName;
	protected int preprintedTileId;
	protected TileI currentTile;
	protected int currentTileRotation;
	protected int tileCost;

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

	protected List<Station> stations;

	protected boolean isBlocked = false;
	
	protected Map<PublicCompanyI, Station> homes;
	protected List<PublicCompanyI> destinations;
	
	/** Tokens that are not bound to a Station (City), such as Bonus tokens */
	protected List<TokenI> offStationTokens;

	protected static Logger log = Logger.getLogger(MapHex.class.getPackage().getName());

	public MapHex()
	{
	}

	/**
	 * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
	 */
	public void configureFromXML(Tag tag) throws ConfigurationException
	{
		Pattern namePattern = Pattern.compile("(\\D)(\\d+)");

		name = tag.getAttributeAsString("name");
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

		preprintedTileId = tag.getAttributeAsInteger("tile", -999);

		currentTileRotation = tag.getAttributeAsInteger(
				"orientation",
				0);

		currentTile = TileManager.get().getTile(preprintedTileId);
		impassable = tag.getAttributeAsString("impassable");
		tileCost = tag.getAttributeAsInteger("cost", 0);

		// We need completely new objects, not just references to the Tile's
		// stations.
		stations = new ArrayList<Station>();
		for (int i = 0; i < currentTile.getStations().size(); i++)
		{
			// sid, type, value, slots
			Station s = (Station) currentTile.getStations().get(i);
			stations.add(new Station(this, s));  // Clone it
		}

		// Off-board revenue values
		offBoardValues = tag.getAttributeAsIntegerArray("value", null);

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

	/** Prepare a tile upgrade.
	 * The actual tile replacement is done in replaceTile(), via a TileMove object.
	 */
	public void upgrade(TileI newTile, int newOrientation)
	{
	    /* Create a new set of stations, each with the tokens that will end up there,
	     * and save both the old and the new set.
	     */
		List<Station> newTileStations = (ArrayList<Station>) newTile.getStations();
		List<Station> newHexStations = new ArrayList<Station>();
	    
		Station oldStation, newStation;
	    TokenI token;
	    
	    /* Clone the stations of the new tile. */
	    for (Station station : newTileStations) {
	    	newStation = new Station (this, station);
		    newHexStations.add (newStation);
	    }
	    
		/* TODO: If the number of stations is > 1, then we need code to map
		 * the old to the new stations. Let's for now take the shortcut that
		 * station 0 on the old tile maps to station 0 on the new one.
		 * This is definitely insufficient to handle the 1830 OO cities correctly! 
		 */
	    for (int i=0, j=0; i<stations.size(); i++, j++) {
		    
		    oldStation = (Station) stations.get(i);

		    /* TODO: The below statement handles simple 2-to-1 or 3-to-1 station 
		     * mergers, as in 1856 Toronto and 18EU Vienna & Berlin.
			 * This is not sufficient to handle complex cases like Vienna in 1837,
			 * where 3 out of 6 green stations merge to 1 out of 4 brown stations.
			 */
	        j = Math.min(j, newTileStations.size()-1);

	        newStation = (Station) newHexStations.get(j);

		    for (Iterator it = oldStation.getTokens().iterator(); it.hasNext(); ) {
		        token = (TokenI) it.next();
		        newStation.addToken(token);
		    }
		    
		}
	    new TileMove (this, currentTile, currentTileRotation, stations,
	            newTile, newOrientation, newHexStations);

		/* TODO Further consequences to be processed here, e.g. new routes etc.*/
	}

	/** Execute a tile replacement.
	 * This method should only be called from TileMove objects.
	 * It is also used to undo tile lays.
	 * @param oldTile The tile to be replaced (only used for validation).
	 * @param newTile The new tile to be laid on this hex.
	 * @param newTileOrientation The orientation of the new tile (0-5).*/
	public void replaceTile (TileI oldTile, TileI newTile, int newTileOrientation,
	        List<Station> newStations) {
	    
	    if (oldTile != currentTile) {
	        new Exception ("ERROR! Hex "+name+" wants to replace tile #"+oldTile.getId()
	                +" but has tile #"+currentTile.getId()+"!")
	            .printStackTrace();
	    }
		if (currentTile != null) {
			currentTile.remove(this);
		}
		
		log.debug ("On hex "+name+" replacing tile "
		        +currentTile.getId()+"/"+currentTileRotation
		        +" by "+newTile.getId()+"/"+newTileOrientation);
		
		newTile.lay(this);

		currentTile = newTile;
		currentTileRotation = newTileOrientation;
		
		stations = newStations;
		/* TODO: Further consequences to be processed here, e.g. new routes etc. */
		
		update(); // To notify ViewObject (Observer)

	}

	public boolean layBaseToken(PublicCompanyI company)
	{
		return layBaseToken(company, null);
	}

	public boolean layBaseToken(PublicCompanyI company, int station)
	{
		return layBaseToken(company, (Station)stations.get(station));
	}

	public boolean layBaseToken(PublicCompanyI company, Station station)
	{
	    if (stations == null || stations.isEmpty()) {
	        log.error ("Tile "+getName()
	        		+" has no station for home token of company "+company.getName());
	        return false;
	    }
	    if (station == null) station = (Station)stations.get(0);

	    BaseToken token = company.getFreeToken();
	    if (token == null) {
	        log.error ("Company "+company.getName()+" has no free token");
	        return false;
	    } else {
	        //new TokenMove (token, company, station);
	        token.moveTo(station);
	        return true;
	    }
	}
	
	public boolean layBonusToken(BonusToken token)
	{
	    if (token == null) {
	        log.error ("No token specified");
	        return false;
	    } else {
	        //new TokenMove (token, token.getHolder(), this);
	        token.moveTo(this);
	        return true;
	    }
	}
	
	public boolean addToken (TokenI token) {
	    
		if (offStationTokens == null) offStationTokens = new ArrayList<TokenI>();
	    if (offStationTokens.contains(token)) {
	        return false;
	    } else {
		    token.setHolder(this);
	        return offStationTokens.add(token);
	    }
	}

	public List<TokenI> getTokens()
	{
		return offStationTokens;
	}

	public boolean hasTokens()
	{
		return offStationTokens.size() > 0;
	}
	
	public boolean removeToken (TokenI token) {
	    
	    return offStationTokens.remove(token);
	}
	
    public boolean addObject (Moveable object) {
        if (object instanceof TokenI) {
            return addToken ((TokenI)object);
        } else {
            return false;
        }
    }

    public boolean removeObject (Moveable object) {
        if (object instanceof TokenI) {
            return removeToken ((TokenI)object);
        } else {
            return false;
        }
    }

	
	public boolean hasTokenSlotsLeft (int station) {
	    if (station < stations.size()) {
		    return hasTokenSlotsLeft ((Station)stations.get(station));
	    } else {
	        log.error ("Invalid station "+station+", max is "+(stations.size()-1));
	        return false;
	    }
	}
	
	public boolean hasTokenSlotsLeft (Station station) {
	    return station != null && station.hasTokenSlotsLeft();
	}
    
    public boolean hasTokenSlotsLeft () {
        for (Station station : stations) {
            if (hasTokenSlotsLeft (station)) return true;
        }
        return false;
    }
	
	/** Check if the tile already has a token of a company in any station */
	public boolean hasTokenOfCompany (PublicCompanyI company) {
	    
	    for (Iterator it = stations.iterator(); it.hasNext(); ) {
	        if (hasTokenOfCompany (company, (Station)it.next())) return true;
	    }
	    return false;
	}
	
	/** Check if the tile already has a token of a company in one station */
	public boolean hasTokenOfCompany (PublicCompanyI company, Station station) {
	    
	    for (Iterator it = station.getTokens().iterator(); it.hasNext(); ) {
	        if (((BaseToken)it.next()).getCompany() == company) return true;
	    }
	    return false;
	}

	/** Check if the tile already has a token of a company in one station */
	public boolean hasTokenOfCompany (PublicCompanyI company, int station) {
	    
	    return hasTokenOfCompany (company, (Station)stations.get(station));
	}

	/**
	 * @return ArrayList of all tokens in all stations merged into a single
	 *         list.
	 * 
	 * To get ArrayList of tokens from stations, use explicit station number
	 */
	/*
	public List getTokens()
	{
		if (stations.size() > 1)
		{
			ArrayList<TokenI> tokens = new ArrayList<TokenI>();
			for (int i = 0; i < stations.size(); i++)
			{
				tokens.addAll(getTokens(i));
			}
			return tokens;
		}
		else
			return getTokens(0);
	}
	*/

	public List<TokenI> getTokens(int stationNumber)
	{
		if (stations.size() > 0)
		{
			return (ArrayList<TokenI>) ((Station) stations.get(stationNumber)).getTokens();
		}
		else
		{
			return new ArrayList<TokenI>();
		}
	}

	public List<Station> getStations()
	{
		return stations;
	}
	
	public void addHome (PublicCompanyI company, Station station) {
	    if (homes == null) homes = new HashMap<PublicCompanyI, Station>();
	    homes.put (company, station);
	}
	
	public Map<PublicCompanyI, Station> getHomes () {
	    return homes;
	}
	
	public boolean isHome (PublicCompanyI company) {
		boolean result = homes != null && homes.get(company) != null;
		return result;
	}
	
	public void addDestination (PublicCompanyI company) {
	    if (destinations == null) destinations = new ArrayList<PublicCompanyI>();
	    destinations.add (company);
	}
	
	public List getDestinations () {
	    return destinations;
	}
	
	public boolean isDestination (PublicCompanyI company) {
		return destinations != null && destinations.contains(company);
	}

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
		    log.debug ("Hex "+name+" is blocked");
			return false;
		}
		if (currentTile != null) {
			if (currentTile.isUpgradeable()) {
			    return true;
			} else {
			    log.debug ("Hex "+name+" tile #"+currentTile.getId()
			            +" is not upgradable now");
			    return false;
			}
		}
		log.debug ("No tile on hex "+name);
		return false;
	}
	
	public boolean hasOffBoardValues () {
	    return offBoardValues != null && offBoardValues.length > 0;
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
	 * Format is tileId/orientation.
	 * @TODO include tokens??
	 */
	public String getText() {
	    return currentTile.getId() + "/" + currentTileRotation; 
	}
}
