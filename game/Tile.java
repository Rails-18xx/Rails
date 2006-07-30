package game;

import java.util.*;
import java.util.regex.*;

import org.w3c.dom.*;
import util.*;

public class Tile implements TileI
{

	private int id;
	private String name;
	private String colour; // May become a separate class TileType
	private boolean upgradeable;
	private List upgrades = new ArrayList(); // Contains Upgrade instances
	private String upgradesString = "";
	private List[] tracksPerSide = new ArrayList[6];
	private List tracks = new ArrayList();
	private List stations = new ArrayList();
	private static final Pattern sidePattern = Pattern.compile("side(\\d+)");
	private static final Pattern cityPattern = Pattern.compile("city(\\d+)");
	private int quantity;
	private boolean unlimited = false;
	public static final int UNLIMITED = -1;
	private ArrayList tilesLaid = new ArrayList();

	public Tile(Integer id)
	{
		this.id = id.intValue();
		name = "" + this.id;

		for (int i = 0; i < 6; i++)
			tracksPerSide[i] = new ArrayList();
	}

	/**
	 * @param se
	 *            &lt;Tile&gt; element from TileSet.xml
	 * @param te
	 *            &lt;Tile&gt; element from Tiles.xml
	 */
	public void configureFromXML(Element se, Element te)
			throws ConfigurationException
	{

	    if (te == null) {
	        throw new ConfigurationException ("Missing Tile in Tiles.xml");
	    }
		/*
		 * EV 23oct05: There is a lot to read and configure here, for now we
		 * only read the tracks to determine the impassable hexsides of offmap
		 * and fixed preprinted track.
		 */

		NamedNodeMap teAttr = te.getAttributes();

		name = XmlUtils.extractStringAttribute(teAttr, "name", name);

		colour = XmlUtils.extractStringAttribute(teAttr, "colour");
		if (colour == null)
			throw new ConfigurationException(LocalText.getText("TileColorMissing")
					+ " " + id);

		upgradeable = !colour.equals("red") && !colour.equals("fixed");

		/* Tracks (only number per side, no cities yet) */
		NodeList trackElements = te.getElementsByTagName("Track");
		Element trackElement;
		Track track;
		int from, to;
		String fromStr, toStr;
		NamedNodeMap nnp;
		for (int i = 0; i < trackElements.getLength(); i++)
		{
			trackElement = (Element) trackElements.item(i);
			nnp = trackElement.getAttributes();
			fromStr = XmlUtils.extractStringAttribute(nnp, "from");
			toStr = XmlUtils.extractStringAttribute(nnp, "to");
			if (fromStr == null || toStr == null)
			{
				throw new ConfigurationException(LocalText.getText("FromToMissing")
						+ " " + id);
			}

			from = getPointNumber(fromStr);
			to = getPointNumber(toStr);
			track = new Track(from, to);
			tracks.add(track);
			if (from >= 0)
				tracksPerSide[from].add(track);
			if (to >= 0)
				tracksPerSide[to].add(track);
		}

		/* Stations */
		NodeList stationElements = te.getElementsByTagName("Station");
		Element stationElement;
		String sid, type;
		int value, slots;
		Station station;
		for (int i = 0; i < stationElements.getLength(); i++)
		{
			stationElement = (Element) stationElements.item(i);
			nnp = stationElement.getAttributes();
			sid = XmlUtils.extractStringAttribute(nnp, "id");
			if (sid == null)
				throw new ConfigurationException(LocalText.getText("TILE")
						+ this.id + LocalText.getText("StationNoID"));
			type = XmlUtils.extractStringAttribute(nnp, "type");
			if (type == null)
				throw new ConfigurationException(LocalText.getText("TILE")
						+ this.id + LocalText.getText("StationNoType"));
			value = XmlUtils.extractIntegerAttribute(nnp, "value", 0);
			slots = XmlUtils.extractIntegerAttribute(nnp, "slots", 0);
			station = new Station(sid, type, value, slots);
			stations.add(station);
		}

		/* Amount */
		NamedNodeMap seAttr = se.getAttributes();
		quantity = XmlUtils.extractIntegerAttribute(seAttr, "quantity", 0);
		/* Value '99' and '-1' mean 'unlimited' */
		unlimited = (quantity == 99 || quantity == UNLIMITED);
		if (unlimited)
			quantity = UNLIMITED;

		/* Upgrades */
		NodeList upgnl = se.getElementsByTagName("Upgrade");
		String ids;
		int id;
		String[] idArray;
		TileI upgradeTile;
		Upgrade upgrade;
		String hexes;
		String[] hexArray;
		MapHex hex;
		
		for (int i = 0; i < upgnl.getLength(); i++)
		{
			trackElement = (Element) trackElements.item(i);
			nnp = ((Element) upgnl.item(i)).getAttributes();
			ids = XmlUtils.extractStringAttribute(nnp, "id");
			upgradesString = ids; // TEMPORARY
			List newUpgrades = new ArrayList();
			
			if (ids != null)
			{
				idArray = ids.split(",");
				for (int j = 0; j < idArray.length; j++)
				{
					try
					{
						id = Integer.parseInt(idArray[j]);
						upgradeTile = TileManager.get().getTile(id);
						if (upgradeTile != null)
						{
							upgrade = new Upgrade (upgradeTile);
							upgrades.add(upgrade);
							newUpgrades.add(upgrade);
						}
						else
						{
							throw new ConfigurationException(LocalText.getText("UpgradeNotFound",
							        new String[] {name, String.valueOf(id)}));
						}
					}
					catch (NumberFormatException e)
					{
						throw new ConfigurationException(LocalText.getText("NonNumericUpgrade",
						        new String[] {name, idArray[j]}),
								e);
					}

				}

			}

			// Process any included or excluded hexes for the current set of upgrades
			hexes = XmlUtils.extractStringAttribute(nnp, "hex");
			if (hexes != null) 
			{
	            for (Iterator it = newUpgrades.iterator(); it.hasNext(); ) {
	                ((Upgrade)it.next()).setHexes(hexes);
	            }
			    
			}
		}

	}

	/**
	 * @return Returns the colour.
	 */
	public String getColour()
	{
		return colour;
	}

	/**
	 * @return Returns the id.
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return name;
	}

	private int getPointNumber(String trackEnd) throws ConfigurationException
	{

		Matcher m;
		if ((m = sidePattern.matcher(trackEnd)).matches())
		{
			return (Integer.parseInt(m.group(1)) + 3) % 6;
		}
		else if ((m = cityPattern.matcher(trackEnd)).matches())
		{
			return -Integer.parseInt(m.group(1));
		}
		// Should add some validation!
		throw new ConfigurationException(LocalText.getText("InvalidTrackEnd") + ": " + trackEnd);
	}

	public boolean hasTracks(int sideNumber)
	{
		while (sideNumber < 0)
			sideNumber += 6;
		return (tracksPerSide[sideNumber % 6].size() > 0);
	}

	/**
	 * Is a tile upgradeable at any time (regardles the phase)?
	 */
	public boolean isUpgradeable()
	{
		return upgradeable;
	}

	/**
	 * Is the tile layable now (in the current phase)?
	 * 
	 * @return
	 */
	public boolean isLayableNow()
	{
		return GameManager.getCurrentPhase().isTileColourAllowed(colour);
	}

	/**
	 * Get the valid upgrades if this tile on a certain hex (restrictions per
	 * hex have not yet been implemented).
	 * 
	 * @param hex
	 *            The MapHex to be upgraded.
	 * @return A List of valid upgrade TileI objects.
	 */
	public List getUpgrades(MapHex hex)
	{
	    List upgr = new ArrayList();
	    Upgrade upgrade;
	    TileI tile;
	    for (Iterator it = upgrades.iterator(); it.hasNext(); ) {
	        upgrade = (Upgrade) it.next();
	        tile = upgrade.getTile();
	        if (hex == null || upgrade.isAllowedForHex(hex)) upgr.add (tile);
	    }
		return upgr;
	}

	public String getUpgradesString(MapHex hex)
	{
		/*
		 * Iterator it = getUpgrades (hex).iterator(); StringBuffer b = new
		 * StringBuffer(); while (it.hasNext()) { if (b.length() > 0)
		 * b.append(","); b.append (((TileI)it.next()).getName()); } return
		 * b.toString();
		 */
		return upgradesString;
	}

	public List getValidUpgrades(MapHex hex, PhaseI phase)
	{
		List valid = new ArrayList();
		Iterator it = upgrades.iterator();
		Upgrade upgrade;
		TileI tile;
		while (it.hasNext())
		{
			upgrade = (Upgrade) it.next();
			tile = (TileI) upgrade.getTile();
			if (phase.isTileColourAllowed(tile.getColour())
					&& tile.countFreeTiles() != 0 /* -1 means unlimited */
					&& upgrade.isAllowedForHex(hex))
			{
				valid.add(tile);
			}
		}
		return valid;
	}

	public boolean hasStations()
	{
		return stations.size() > 0;
	}

	public List getStations()
	{
		return stations;
	}

	public int getNumStations()
	{
		return stations.size();
	}

	public boolean lay(MapHex hex)
	{

		tilesLaid.add(hex);
		return true;
	}

	public boolean remove(MapHex hex)
	{

		tilesLaid.remove(hex);
		return true;
	}

	/**
	 * Find the index of the tile laid on a certain hex. If the argument is
	 * null, the first free tile index is returned.
	 * 
	 * @param hex
	 * @return
	 */
	/*
	 * private int findTile(MapHex hex) { if (tiles == null || tiles.length ==
	 * 0) return -1; for (int i=0; i<tiles.length; i++) { if (tiles[i] == hex)
	 * return i; } return -1; }
	 */

	/** Return the number of free tiles */
	public int countFreeTiles()
	{
		if (unlimited)
			return UNLIMITED;
		else
			return quantity - tilesLaid.size();
	}

	public int getQuantity()
	{
		return quantity;
	}
	
	protected class Upgrade {
	    
	    /** The upgrade tile */
	    TileI tile;
	    
	    /** Hexes where the upgrade can be executed */
	    List allowedHexes = null;
	    /** Hexes where the upgrade cannot be executed 
	     * Only one of allowedHexes and disallowedHexes should be used 
	     * */
	    List disallowedHexes = null;
	    
	    /** A temporary String holding the in/excluded hexes.
	     * This will be processed at the first usage, because Tiles
	     * are initialised before the Map.
	     * @author Erik Vos
	     */
	    String hexes = null;
	    
	    protected Upgrade (TileI tile) {
	        this.tile = tile;
	    }
	    
	    protected boolean isAllowedForHex (MapHex hex) {
	        
	        if (hexes != null) convertHexString();
	        
	        if (allowedHexes != null) {
	            return allowedHexes.contains(hex);
	        } else if (disallowedHexes != null) {
	            return !disallowedHexes.contains(hex);
	        } else {
	            return true;
	        }
	    }
	    
	    protected TileI getTile() {
	        return tile;
	    }
	    
	    protected void setHexes (String hexes) {
	        this.hexes = hexes;
	    }
	    
	    private void convertHexString () {
	        
	        boolean allowed = !hexes.startsWith("!");
	        if (!allowed) hexes = hexes.substring(1);
	        String[] hexArray = hexes.split(",");
	        MapHex hex;
	        for (int i=0; i<hexArray.length; i++) {
	            hex = MapManager.getInstance().getHex(hexArray[i]);
	            if (hex != null) {
	                if (allowed) {
	                    if (allowedHexes == null) allowedHexes = new ArrayList();
	                    allowedHexes.add (hex);
	                } else {
	                    if (disallowedHexes == null) disallowedHexes = new ArrayList();
	                    disallowedHexes.add(hex);
	                }
	            }
	        }
	        hexes = null; // Do this only once
	    }
	}
}
