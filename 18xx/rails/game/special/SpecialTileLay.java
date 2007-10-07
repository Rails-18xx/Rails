/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialTileLay.java,v 1.4 2007/10/07 20:14:53 evos Exp $ */
package rails.game.special;


import java.util.ArrayList;
import java.util.List;

import rails.game.*;
import rails.util.Tag;
import rails.util.Util;

public class SpecialTileLay extends SpecialProperty
{

	String locationCodes = null;
	List<MapHex> locations = null;
	boolean extra = false;
	boolean free = false;

	public void configureFromXML(Tag tag) throws ConfigurationException
	{
		super.configureFromXML (tag);

		Tag tileLayTag = tag.getChild("SpecialTileLay");
		if (tileLayTag == null)
		{
			throw new ConfigurationException("<SpecialTileLay> tag missing");
		}

		locationCodes = tileLayTag.getAttributeAsString("location");
		if (!Util.hasValue(locationCodes))
			throw new ConfigurationException("SpecialTileLay: location missing");
        MapManager mmgr = MapManager.getInstance();
        MapHex hex;
        locations = new ArrayList<MapHex>();
        for (String hexName : locationCodes.split(",")) {
            hex = mmgr.getHex(hexName);
            if (hex == null)
                throw new ConfigurationException("Location " + hexName
                        + " does not exist");
            locations.add (hex);
        }

		extra = tileLayTag.getAttributeAsBoolean("extra", extra);
		free = tileLayTag.getAttributeAsBoolean("free", free);
		closingValue = tileLayTag.getAttributeAsInteger("closingValue",	closingValue);
	}
	
	public boolean isExecutionable () {
		return true;
	}

	public boolean isExtra()
	{
		return extra;
	}

	public boolean isFree()
	{
		return free;
	}

    /** @deprecated */
    public MapHex getLocation()
    {
        if (locations != null) {
            return locations.get(0);
        } else {
            return null;
        }
    }
    
    public List<MapHex> getLocations () {
        return locations;
    }
    
	
	public String toString() {
	    return "SpecialTileLay comp="+privateCompany.getName()+" hex="+locationCodes+" extra="+extra+" cost="+free;
	}

}
