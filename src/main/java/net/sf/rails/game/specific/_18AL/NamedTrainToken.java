package net.sf.rails.game.specific._18AL;

import java.util.List;

import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NamedTrainToken extends Token<NamedTrainToken> implements Configurable {

    protected static Logger log =
        LoggerFactory.getLogger(NamedTrainToken.class);

    private String name;
    private String longName;
    private int value;
    private String hexesString;
    private List<MapHex> hexes;
    private String description;

    /**
     * Used via Configure (reflection)
     */
    public NamedTrainToken(RailsItem parent, String id) {
        super(parent, id, NamedTrainToken.class);
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
                longName + " [" + hexesString + "] +" + Bank.format(this, value);
    }

    public void finishConfiguration (RailsRoot root)
    throws ConfigurationException {

        if (hexesString != null) {
            hexes = root.getMapManager().parseLocations(hexesString);
        }
        
    }

    public String getId() {
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
