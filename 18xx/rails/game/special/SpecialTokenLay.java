/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialTokenLay.java,v 1.3 2007/10/05 22:02:25 evos Exp $ */
package rails.game.special;


import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.*;

import rails.game.*;
import rails.util.Util;
import rails.util.XmlUtils;


public class SpecialTokenLay extends SpecialProperty
{
	String locationCodes = null;
    List<MapHex> locations = null;
	boolean extra = false;
	boolean free = false;
	boolean connected = false;
	Class tokenClass;
	TokenI token = null;
    int numberAvailable = 1;
    int numberUsed = 0;

	public void configureFromXML(Element element) throws ConfigurationException
	{
		
		super.configureFromXML (element);

		NodeList nl = element.getElementsByTagName("SpecialTokenLay");
		if (nl == null || nl.getLength() == 0)
		{
			throw new ConfigurationException("<SpecialTokenLay> tag missing");
		}
		Element stlEl = (Element) nl.item(0);

		NamedNodeMap nnp = stlEl.getAttributes();
		locationCodes = XmlUtils.extractStringAttribute(nnp, "location");
		if (!Util.hasValue(locationCodes))
			throw new ConfigurationException("SpecialTokenLay: location missing");
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
		connected = XmlUtils.extractBooleanAttribute(nnp, "connected", connected);
		closingValue = XmlUtils.extractIntegerAttribute(nnp,
				"closingValue",
				closingValue);
		
		String tokenClassName = XmlUtils.extractStringAttribute(
				nnp, "class", "rails.game.BaseToken");
		try {
			tokenClass = Class.forName(tokenClassName);
			if (tokenClass == BonusToken.class) {
                BonusToken bToken = (BonusToken) tokenClass.newInstance();
                token = bToken;
				int value = XmlUtils.extractIntegerAttribute(
						nnp, "value");
				if (value <= 0) {
					throw new ConfigurationException ("Missing or invalid value "+value);
				}
                bToken.setValue(value);
                
                numberAvailable = XmlUtils.extractIntegerAttribute(
                        nnp, "number", numberAvailable);
			}
		} catch (ClassNotFoundException e) {
			throw new ConfigurationException ("Unknown class "+tokenClassName, e);
		} catch (Exception e) {
			throw new ConfigurationException ("Cannot instantiate class "+tokenClassName, e);
		}
	}
	
	public boolean isExecutionable() {
		return true;
	}
    
    public int getNumberLeft () {
        return numberAvailable - numberUsed;
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
	    return "SpecialTokenLay comp="+privateCompany.getName()+" hex="+locationCodes+" extra="+extra+" cost="+free;
	}
}
