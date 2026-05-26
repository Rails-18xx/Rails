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
    private String forcedText;

    private boolean extra = false;
    private boolean free = false;
    private boolean connected = false;
    private boolean requiresTile = false;
    private boolean requiresNoTile = false;
    private Forced forced = Forced.NO;
    // Two specials for 18VA, where both will be set  to true
    private boolean create = false;
    private boolean offCity = false;

    public enum Forced {
        NO,
        IF_YELLOW
    }

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
        // Locations can be null, in which case all reachable locations are valid.
        //if (!Util.hasValue(locationCodes))
        //    throw new ConfigurationException(
        //            "SpecialBaseTokenLay: location missing");

        extra = tokenLayTag.getAttributeAsBoolean("extra", extra);
        free = tokenLayTag.getAttributeAsBoolean("free", free);
        connected = tokenLayTag.getAttributeAsBoolean("connected", connected);
        requiresTile = tokenLayTag.getAttributeAsBoolean("requiresTile", requiresTile);
        requiresNoTile = tokenLayTag.getAttributeAsBoolean("requiresNoTile", requiresNoTile);
        forcedText = tokenLayTag.getAttributeAsString("forced", null);
        // For 18VA
        create = tokenLayTag.getAttributeAsBoolean("create", create);
        offCity = tokenLayTag.getAttributeAsBoolean("offCity", offCity);

        description = LocalText.getText("LayBaseTokenInfo",
                connected ? LocalText.getText("aconnected")
                          : LocalText.getText("anunconnected"),
                locationCodes,
                (extra ? LocalText.getText("extra"):LocalText.getText("notExtra")),
                (free ? LocalText.getText("noCost") : LocalText.getText("normalCost")));
    }

    @Override
    public void finishConfiguration (RailsRoot root) throws ConfigurationException {
        if (Util.hasValue(locationCodes)) {
            locations = root.getMapManager().parseLocations(locationCodes);
            if (Util.hasValue(forcedText)) {
                if ("ifYellow".equalsIgnoreCase(forcedText)) {
                    // 18Scan: destination token may be laid aside a city on a yellow tile
                    forced = Forced.IF_YELLOW;
                }
            }
        }
    }

    public Forced getForced() {
        return forced;
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
    
    public boolean requiresNoTile() {
        return requiresNoTile;
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public String getLocationCodeString() {
        return locationCodes;
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean isOffCity() {
        return offCity;
    }

    public void setOffCity(boolean offCity) {
        this.offCity = offCity;
    }

    // That's rather overdoing revealing descriptions, here below.
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
