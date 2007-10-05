/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialTileLay.java,v 1.3 2007/10/05 22:02:25 evos Exp $ */
package rails.game.special;


import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.*;

import rails.game.*;
import rails.util.Util;
import rails.util.XmlUtils;


public class SpecialTileLay extends SpecialProperty
{

	String locationCodes = null;
	List<MapHex> locations = null;
	boolean extra = false;
	boolean free = false;

	public void configureFromXML(Element element) throws ConfigurationException
	{
		super.configureFromXML (element);

		NodeList nl = element.getElementsByTagName("SpecialTileLay");
		if (nl == null || nl.getLength() == 0)
		{
			throw new ConfigurationException("<SpecialTileLay> tag missing");
		}
		Element stlEl = (Element) nl.item(0);

		NamedNodeMap nnp = stlEl.getAttributes();
		locationCodes = XmlUtils.extractStringAttribute(nnp, "location");
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

		extra = XmlUtils.extractBooleanAttribute(nnp, "extra", extra);
		free = XmlUtils.extractBooleanAttribute(nnp,
				"free",
				free);
		closingValue = XmlUtils.extractIntegerAttribute(nnp,
				"closingValue",
				closingValue);
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
