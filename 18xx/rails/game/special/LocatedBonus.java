/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/LocatedBonus.java,v 1.5 2010/05/18 21:36:12 stefanfrey Exp $ */
package rails.game.special;

import java.util.List;

import rails.game.*;
import rails.util.Tag;
import rails.util.Util;

/**
 * An object of class LocatedBonus represent extra income for the owning company,
 * usually connected to certain map locations.
 * <p>LocatedBonus objects are configured as Special Properties in CompanyManager.xml.
 * @author VosE
 *
 */
public class LocatedBonus extends SpecialProperty {

    String locationCodes = null;
    List<MapHex> locations = null;
    String name;
    int value;

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

        Tag bonusTag = tag.getChild("LocatedBonus");
        if (bonusTag == null) {
            throw new ConfigurationException("<LocatedBonus> tag missing");
        }

        locationCodes = bonusTag.getAttributeAsString("location");
        if (!Util.hasValue(locationCodes))
            throw new ConfigurationException("LocatedBonus: location missing");

        name = bonusTag.getAttributeAsString("name");

        value = bonusTag.getAttributeAsInteger("value");
        if (value <= 0)
            throw new ConfigurationException("Value invalid ["+value+"] or missing");
    }

    @Override
    public void finishConfiguration (GameManagerI gameManager) 
    throws ConfigurationException {
        locations = gameManager.getMapManager().parseLocations(locationCodes);
    }

    public boolean isExecutionable() {
        return false;
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public String getLocationNameString() {
        return locationCodes;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }


    @Override
    public String toString() {
        return "LocatedBonus "+name+" comp=" + originalCompany.getName() + " hex="
               + locationCodes + " value=" + value;
    }
}
