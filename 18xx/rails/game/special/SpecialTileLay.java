/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialTileLay.java,v 1.6 2008/06/04 19:00:38 evos Exp $ */
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

    public void configureFromXML(Tag tag) throws ConfigurationException {
        super.configureFromXML(tag);

        Tag tileLayTag = tag.getChild("SpecialTileLay");
        if (tileLayTag == null) {
            throw new ConfigurationException("<SpecialTileLay> tag missing");
        }

        locationCodes = tileLayTag.getAttributeAsString("location");
        if (!Util.hasValue(locationCodes))
            throw new ConfigurationException("SpecialTileLay: location missing");
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

        tileNumber = tileLayTag.getAttributeAsInteger("tile", 0);
        if (tileNumber > 0) {
            tile = TileManager.get().getTile(tileNumber);
        }

        name = tileLayTag.getAttributeAsString("name");

        extra = tileLayTag.getAttributeAsBoolean("extra", extra);
        free = tileLayTag.getAttributeAsBoolean("free", free);
        closingValue =
                tileLayTag.getAttributeAsInteger("closingValue", closingValue);
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

    public String toString() {
        return "SpecialTileLay comp=" + privateCompany.getName() + " hex="
               + locationCodes + " extra=" + extra + " cost=" + free;
    }

}
