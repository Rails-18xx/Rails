/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/LocatedBonus.java,v 1.1 2009/09/23 21:38:57 evos Exp $ */
package rails.game.special;

import java.util.ArrayList;
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
        parseLocations ();

        name = bonusTag.getAttributeAsString("name");

        value = bonusTag.getAttributeAsInteger("value");
        if (value <= 0)
            throw new ConfigurationException("Value invalid ["+value+"] or missing");
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

    private void parseLocations ()
    throws ConfigurationException {

        MapManager mmgr = MapManager.getInstance();
        MapHex hex;
        locations = new ArrayList<MapHex>();
        for (String hexName : locationCodes.split(",")) {
            hex = mmgr.getHex(hexName);
            if (hex == null)
                throw new ConfigurationException("Location " + hexName
                                                 + " does not exist");
            locations.add(hex);
        }
    }

	@Override
	public String toString() {
        return "LocatedBonus "+name+" comp=" + privateCompany.getName() + " hex="
               + locationCodes + " value=" + value;
    }

}
