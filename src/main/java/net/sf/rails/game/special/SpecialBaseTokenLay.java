package net.sf.rails.game.special;

import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.util.Util;

public class SpecialBaseTokenLay extends SpecialProperty {
    private String locationCodes = null;
    private List<MapHex> locations = null;
    
    private boolean extra = false;
    private boolean free = false;
    private boolean connected = false;
    private boolean requiresTile = false;

    /**
     * Used by Configure (via reflection) only
     */
    public SpecialBaseTokenLay(RailsItem parent, String id) {
        super(parent, id);
    }

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

        Tag tokenLayTag = tag.getChild("SpecialBaseTokenLay");
        if (tokenLayTag == null) {
            throw new ConfigurationException("<SpecialBaseTokenLay> tag missing");
        }

        locationCodes = tokenLayTag.getAttributeAsString("location");
        if (!Util.hasValue(locationCodes))
            throw new ConfigurationException(
                    "SpecialBaseTokenLay: location missing");

        extra = tokenLayTag.getAttributeAsBoolean("extra", extra);
        free = tokenLayTag.getAttributeAsBoolean("free", free);
        connected = tokenLayTag.getAttributeAsBoolean("connected", connected);
        requiresTile = tokenLayTag.getAttributeAsBoolean("requiresTile", requiresTile);

        description = LocalText.getText("LayBaseTokenInfo",
                locationCodes,
                (extra ? LocalText.getText("extra"):LocalText.getText("notExtra")),
                (free ? LocalText.getText("noCost") : LocalText.getText("normalCost")));
    }

    @Override
    public void finishConfiguration (RailsRoot root) throws ConfigurationException {
        locations = root.getMapManager().parseLocations(locationCodes);
    }

    public boolean isExecutionable() {
        return true;
    }

    public boolean isExtra() {
        return extra;
    }

    public boolean isFree() {
        return free;
    }
    
    public boolean requiresTile() {
        return requiresTile;
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public String getLocationCodeString() {
        return locationCodes;
    }

    @Override
    public String toText() {
        return description;
    }

    @Override
    public String toMenu() {
        return description;
    }

    @Override
    public String getInfo() {
        return description;
    }
}
