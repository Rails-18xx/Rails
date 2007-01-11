package game;

import java.util.*;

import util.Util;

/**
 * A Station object represents any node on the map that is relevant
 * for establishing train run length and revenue calculation.
 * The usual Station types are "City", "Town" and "OffMapCity".
 * Other types found in some games are "Pass" (1841), "Port" (1841, 18EU)
 * and "Halt" (1860).
 * <p>
 * The station types "City" and "OffMapCity" may have slots for placing tokens.
 * <p>
 * Station objects are used in two different contexts:
 * <p>
 * 1. In a Tile object, to represent the station(s) on a tile.
 * Each tile type is represented by just one Tile object (which is NOT
 * cloned or newly instantiated when a Tile is laid). Please note, that
 * all preprinted tiles on the map are also represented by Tile objects,
 * so laying the first tile on a hex is treated as a normal upgrade in this program. 
 * <p>
 * 2. In a MapHex object, which contains copies of the stations of the currantly
 * laid tile. These copies (clones) are created when the tile is laid, at which time 
 * also any tokens are moved from the old MapHex stations to the new ones. 
 * 
 * @author Erik Vos
 */
public class Station implements TokenHolderI, Cloneable
{

	private String id;
	private String type;
	private int value;
	private int baseSlots;
	private Track[] tracks;
	private ArrayList tokens;
	private StationHolderI holder; // Tile or MapHex
	
	public static final String CITY = "City";
	public static final String TOWN = "Town";
	public static final String HALT = "Halt";
	public static final String OFF_MAP_AREA = "OffMapCity";
	public static final String PORT = "Port";
	public static final String PASS = "Pass";
	public static final String JUNCTION = "Junction"; // No station, just a branching point.
	private static final String[] types = {
	        CITY, TOWN, HALT, OFF_MAP_AREA, PORT, PASS, JUNCTION
	};
	private static final List validTypes = Arrays.asList(types);
	
	/** Check validity of a Station type */
	public static boolean isTypeValid (String type) {
	    return validTypes.contains(type);
	}

	public Station(StationHolderI holder, String id, String type, int value)
	{
		this(holder, id, type, value, 0);
	}

	public Station(StationHolderI holder, String id, String type, int value, int slots)
	{
	    this.holder = holder;
		this.id = id;
		this.type = type;
		this.value = value;
		this.baseSlots = slots;

		tokens = new ArrayList();
	}

	/**
	 * Creates a clone of the station by calling Station's 4 argument
	 * constructor with specified station argument's values
	 * 
	 * @param s
	 */
	public Station(StationHolderI holder, Station s)
	{
		this(holder, s.id, s.type, s.value, s.baseSlots);
	}
	
	public String getName() {
	    return "Station "+id+" on "+Util.getClassShortName(holder)+" "+holder.getName();
	}
	

    /**
     * @return Returns the holder.
     */
    public Object getHolder() {
        return holder;
    }
    
	/**
	 * @return Returns the id.
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * @return Returns the type.
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * @return Returns the baseSlots.
	 */
	public int getBaseSlots()
	{
		return baseSlots;
	}

	/**
	 * @return Returns the tracks.
	 */
	public Track[] getTracks()
	{
		return tracks;
	}

	/**
	 * @return Returns the value.
	 */
	public int getValue()
	{
		return value;
	}

	/*
	public boolean addToken(TokenHolderI company)
	{
	    if (tokens.size() + 1 <= baseSlots)
		{
			if (!tokens.contains(company))
			{
				tokens.add(company);
				return true;
			}
			else
			{
				Log.error("Unable to add token to this station.\nThis company already has a token at this location.");
				return false;
			}
		}
		else
		{
			Log.error("Unable to add token to this station. No more open slots.");
			return false;
		}
	}
	*/
	
	/** Stub */
	public boolean addToken (TokenI token) {
	    
	    if (tokens.contains(token)) {
	        return false;
	    } else {
		    token.setHolder(this);
	        return tokens.add(token);
	    }
	}

	public List getTokens()
	{
		return tokens;
	}

	public boolean hasTokens()
	{
		return tokens.size() > 0;
	}
	
	public boolean hasTokenSlotsLeft() {
	    return tokens.size() < baseSlots;
	}

	/*
	public boolean removeToken(TokenHolderI company)
	{
		int index = tokens.indexOf(company);
		if (index >= 0)
		{
			tokens.remove(index);
			return true;
		}
		else
			return false;
	}
	*/
	
	/** Stub */
	public boolean removeToken (TokenI token) {
	    
	    return tokens.remove(token);
	}

	/**
	 * 
	 * @param company
	 * @return true if this Station already contains an instance of the
	 *         specified company's token.
	 */
	public boolean contains(PublicCompanyI company)
	{
		if (tokens.contains(company))
			return true;
		return false;
	}

	public void setTokens(ArrayList tokens)
	{
		this.tokens = tokens;
	}

	public String toString()
	{
		return "Station ID: " + id + ", Type: " + type + ", Slots: "
				+ baseSlots + ", Value: " + value;
	}
}
