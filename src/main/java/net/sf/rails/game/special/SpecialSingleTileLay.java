package net.sf.rails.game.special;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.*;
import net.sf.rails.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpecialSingleTileLay extends SpecialTileLay {

    protected List<MapHex> neighbouringHexes = null;
    protected String neighbouringHexesCode =null;

    /**
     * Used by Configure (via reflection) only
     *
     * @param parent Railsitem this belongs
     * @param id this Item has
     */
    public SpecialSingleTileLay(RailsItem parent, String id) {
        super(parent, id);
    }

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {
        super.configureFromXML(tag);

        Tag tileLayTag = tag.getChild("SpecialSingleTileLay");
        if (tileLayTag == null) {
            throw new ConfigurationException("<SpecialSingleTileLay> tag missing");
        }

        locationCodes = tileLayTag.getAttributeAsString("location");
        // If all locations are allowed, as in SOH, "all" must be specified
        // ('connected' defines if the locations must be reachable or not).
        if (!Util.hasValue(locationCodes))
            throw new ConfigurationException("SpecialSingleTileLay: location missing");

        tileId = tileLayTag.getAttributeAsString("tile", null);

        String coloursString = tileLayTag.getAttributeAsString("colour");
        if (Util.hasValue(coloursString)) {
            tileColours = coloursString.split(",");
        }

        name = tileLayTag.getAttributeAsString("name");

        extra = tileLayTag.getAttributeAsBoolean("extra", extra);
        free = tileLayTag.getAttributeAsBoolean("free", free);
        connected = tileLayTag.getAttributeAsBoolean("connected", connected);
        discount = tileLayTag.getAttributeAsInteger("discount", discount);

        neighbouringHexesCode = tileLayTag.getAttributeAsString("neighbours", null);
        // If neighbours is defined the subsequent tile lays need to be connected to the first tile lay.

        if (tileId != null) {
            description = LocalText.getText("LayNamedTileInfo",
                    tileId,
                    name != null ? name : "",
                            locationCodes,
                            (extra ? LocalText.getText("extra"):LocalText.getText("notExtra")),
                            (free ? LocalText.getText("noCost") : discount != 0 ? LocalText.getText("discount", discount) :
                                LocalText.getText("normalCost")),
                            (connected ? LocalText.getText("connected") : LocalText.getText("unconnected")),
                            neighbouringHexesCode
            );
        } else {
            description = LocalText.getText("LayTileInfo",
                    locationCodes,
                    (tileColours != null ? Arrays.toString(tileColours).replaceAll("[\\[\\]]", ""): ""),
                    (extra ? LocalText.getText("extra"):LocalText.getText("notExtra")),
                    (free ? LocalText.getText("noCost") : discount != 0 ? LocalText.getText("discount", discount) :
                        LocalText.getText("normalCost")),
                    (connected ? LocalText.getText("connected") : LocalText.getText("unconnected")),
                    neighbouringHexesCode
            );
        }

    }

    @Override
	public void finishConfiguration (RailsRoot root)
    throws ConfigurationException {

        TileManager tmgr = root.getTileManager();
        MapManager mmgr = root.getMapManager();
        MapHex hex;

        if (tileId != null) {
            tile = tmgr.getTile(tileId);
        }

        if (locationCodes != null
                && !locationCodes.equalsIgnoreCase("all")) {
            locations = new ArrayList<>();
            for (String hexName : locationCodes.split(",")) {
                hex = mmgr.getHex(hexName);
                if (hex == null)
                    throw new ConfigurationException("Location " + hexName
                            + " does not exist");
                locations.add(hex);
            }
        }
        if (neighbouringHexesCode != null) {
            neighbouringHexes = new ArrayList<>();
            for (String hexName : neighbouringHexesCode.split(",")) {
                hex = mmgr.getHex(hexName);
                if (hex == null)
                    throw new ConfigurationException("Neighbour " + hexName
                            + " does not exist");
                neighbouringHexes.add(hex);
            }
        }

    }

    @Override
	public String toText() {
                if (neighbouringHexesCode != null) {
                    return "SpecialSingleTileLay comp="
                            + (originalCompany == null ? null : originalCompany.getId())
                            + " hex=" + locationCodes
                            + " colour=" + Util.join(tileColours, ",")
                            + " extra=" + extra + " cost=" + free + " connected=" + connected
                            + " neighours=" + neighbouringHexesCode;
                }
        return "SpecialSingleTileLay comp="
                + (originalCompany == null ? null : originalCompany.getId())
                + " hex=" + locationCodes
                + " colour=" + Util.join(tileColours, ",")
                + " extra=" + extra + " cost=" + free + " connected=" + connected;
    }

    @Override
    public String toMenu() {
        return description;
    }

    @Override
    public String getInfo() {
        return description;
    }

    @Override
    public String toString() { return toText(); }

    public List<MapHex> getNeighbouringHexes() {
        return neighbouringHexes;
    }

    public void setNeighbouringHexes(List<MapHex> neighbouringHexes) {
        this.neighbouringHexes = neighbouringHexes;
    }

    public boolean hasNeighbours() {
        return false;
    }
}
