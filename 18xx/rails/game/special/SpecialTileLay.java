/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialTileLay.java,v 1.7 2009/10/30 21:53:04 evos Exp $ */
package rails.game.special;

import java.util.ArrayList;
import java.util.List;

import rails.game.*;
import rails.util.Tag;
import rails.util.Util;

public class SpecialTileLay extends SpecialProperty {

    String locationCodes = null;
    List<MapHex> locations = null;
    int tileNumber;
    TileI tile = null;
    String name;
    boolean extra = false;
    boolean free = false;

    @Override
	public void configureFromXML(Tag tag) throws ConfigurationException {
        super.configureFromXML(tag);

        Tag tileLayTag = tag.getChild("SpecialTileLay");
        if (tileLayTag == null) {
            throw new ConfigurationException("<SpecialTileLay> tag missing");
        }

        locationCodes = tileLayTag.getAttributeAsString("location");
        if (!Util.hasValue(locationCodes))
            throw new ConfigurationException("SpecialTileLay: location missing");

        tileNumber = tileLayTag.getAttributeAsInteger("tile", 0);

        name = tileLayTag.getAttributeAsString("name");

        extra = tileLayTag.getAttributeAsBoolean("extra", extra);
        free = tileLayTag.getAttributeAsBoolean("free", free);
        closingValue =
                tileLayTag.getAttributeAsInteger("closingValue", closingValue);
    }
    
    public void finishConfiguration (GameManagerI gameManager)
    throws ConfigurationException {
        
        TileManager tmgr = gameManager.getTileManager();
        MapManager mmgr = gameManager.getMapManager();
        MapHex hex;

        if (tileNumber > 0) {
            tile = tmgr.getTile(tileNumber);
        }

        locations = new ArrayList<MapHex>();
        for (String hexName : locationCodes.split(",")) {
            hex = mmgr.getHex(hexName);
            if (hex == null)
                throw new ConfigurationException("Location " + hexName
                                                 + " does not exist");
            locations.add(hex);
        }

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

    /** @deprecated */
    @Deprecated
	public MapHex getLocation() {
        if (locations != null) {
            return locations.get(0);
        } else {
            return null;
        }
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public String getLocationNameString() {
        return locationCodes;
    }

    public int getTileNumber() {
        return tileNumber;
    }

    public TileI getTile() {
        return tile;
    }

    public String getName() {
        return name;
    }

    @Override
	public String toString() {
        return "SpecialTileLay comp=" + privateCompany.getName() + " hex="
               + locationCodes + " extra=" + extra + " cost=" + free;
    }

}
