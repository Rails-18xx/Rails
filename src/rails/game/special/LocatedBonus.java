package rails.game.special;

import java.util.List;

import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.*;
import rails.util.Util;

/**
 * An object of class LocatedBonus represent extra income for the owning company,
 * usually connected to certain map locations.
 * <p>LocatedBonus objects are configured as Special Properties in CompanyManager.xml.
 * @author VosE
 *
 */
public final class LocatedBonus extends SpecialProperty {

    private String locationCodes = null;
    private List<MapHex> locations = null;
    private String name;
    private int value;

    /**
     * Used by Configure (via reflection) only
     */
    public LocatedBonus(RailsItem parent, String id) {
        super(parent, id);
    }

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
    public void finishConfiguration (GameManager gameManager) 
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

    public int getValue() {
        return value;
    }

    @Override
    public String toText() {
        return "LocatedBonus " +name + " comp=" + originalCompany.getId() + " hex="
               + locationCodes + " value=" + value;
    }
}
