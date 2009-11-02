package rails.game.specific._18AL;

import java.util.List;

import rails.game.*;
import rails.util.Tag;
import rails.util.Util;

public class NamedTrainToken extends Token implements ConfigurableComponentI {

    private String name;
    private String longName;
    private int value;
    private String hexesString;
    private List<MapHex> hexes;
    private String description;

    public NamedTrainToken() {
        super();
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {
        value = tag.getAttributeAsInteger("value");
        if (value <= 0) {
            throw new ConfigurationException("Missing or invalid value "
                                             + value);
        }

        name = tag.getAttributeAsString("name");
        if (!Util.hasValue(name)) {
            throw new ConfigurationException(
                    "Named Train token must have a name");
        }

        longName = tag.getAttributeAsString("longName");
        if (!Util.hasValue(longName)) {
            throw new ConfigurationException(
                    "Named Train token must have a long name");
        }

        hexesString = tag.getAttributeAsString("ifRouteIncludes");

        description =
                longName + " [" + hexesString + "] +" + Bank.format(value);
    }

    public void finishConfiguration (GameManagerI gameManager)
    throws ConfigurationException {

        if (hexesString != null) {
            hexes = gameManager.getMapManager().parseLocations(hexesString);
        }
    }

    public String getName() {
        return name;
    }

    public String getLongName() {
        return longName;
    }

    public int getValue() {
        return value;
    }

    @Override
	public String toString() {
        return description;
    }

    public List<MapHex> getHexesToPass() {
        return hexes;
    }

}
