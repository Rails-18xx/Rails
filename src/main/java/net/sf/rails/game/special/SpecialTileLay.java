package net.sf.rails.game.special;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.*;
import net.sf.rails.util.Util;


public abstract class SpecialTileLay extends SpecialProperty {

    protected String locationCodes = null;
    protected List<MapHex> locations = null;
    protected String tileId = null;
    protected Tile tile = null;
    protected String name = null;
    protected boolean extra = false;
    protected boolean free = false;
    protected int discount = 0;
    protected boolean connected = false;
    
    /** Tile colours that can be laid with this special property.
     * Default is same colours as is allowed in a a normal tile lay.
     * Don't use if specific tiles are specified! */
    protected String[] tileColours = null;

    /**
     * Used by Configure (via reflection) only
     */
    public SpecialTileLay(RailsItem parent, String id) {
        super(parent, id);
    }

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {
        super.configureFromXML(tag);
    }

    @Override
	public abstract void finishConfiguration(RailsRoot root)
    throws ConfigurationException;

    @Override
    public boolean isExecutionable() {
        return true;
    }

    public boolean isExtra() {
        return extra;
    }

    public boolean isFree() {
        return free;
    }

    public int getDiscount() {
        return discount;
    }

    public boolean requiresConnection() {
        return connected;
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public String getLocationNameString() {
        return locationCodes;
    }

    public String getTileId() {
        return tileId;
    }

    public Tile getTile() {
        return tile;
    }

    public String[] getTileColours() {
        return tileColours;
    }

    @Override
	public abstract String toText();

    @Override
    public abstract String toMenu();

    @Override
    public abstract String getInfo();
}
