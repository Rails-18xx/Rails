/**
 * 
 */
package net.sf.rails.game.special;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapManager;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Tile;
import net.sf.rails.game.TileManager;
import net.sf.rails.util.Util;

/**
 * @author Administrator
 *
 */
public class SpecialMultiTileLay extends SpecialProperty {
	
	private String locationCodes = null;
	private List<MapHex> locations = null;
	private String name = null;
	private boolean extra = false;
	private boolean free = false;
	private int discount = 0;
	private String tileIdCodes = null;
    private String[] tileIds = null;
    private List<Tile> tiles = null;
    private boolean connected = false;
    private String constraints = null;
    
    /** Tile colours that can be laid with this special property.
     * Default is same colours as is allowed in a a normal tile lay.
     * Don't use if specific tiles are specified! */
    protected String[] tileColours = null;

    /**
     * Used by Configure (via reflection) only
     */
    public SpecialMultiTileLay(RailsItem parent, String id) {
        super(parent, id);
    }

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {
        super.configureFromXML(tag);

        Tag tileLayTag = tag.getChild("SpecialMultiTileLay");
        if (tileLayTag == null) {
            throw new ConfigurationException("<SpecialMultiTileLay> tag missing");
        }

        locationCodes = tileLayTag.getAttributeAsString("location");
        if (!Util.hasValue(locationCodes))
            throw new ConfigurationException("SpecialMultiTileLay: location missing");

        tileIdCodes = tileLayTag.getAttributeAsString("tiles", null);
        if (!Util.hasValue(tileIdCodes))
            throw new ConfigurationException("SpecialMultiTileLay: tileIds missing");
        tileIds = tileIdCodes.split(",");

        String coloursString = tileLayTag.getAttributeAsString("colour");
        if (Util.hasValue(coloursString)) {
            tileColours = coloursString.split(",");
        }

        name = tileLayTag.getAttributeAsString("name");

        extra = tileLayTag.getAttributeAsBoolean("extra", extra);
        free = tileLayTag.getAttributeAsBoolean("free", free);
        connected = tileLayTag.getAttributeAsBoolean("connected", connected);
        discount = tileLayTag.getAttributeAsInteger("discount", discount);
        constraints = tileLayTag.getAttributeAsString("constraints");
        maxOccurrance = tileLayTag.getAttributeAsInteger("occurance", maxOccurrance);

        if (!(tileIds.length==0) ) {
            description = LocalText.getText("LayNamedTileInfo",
                    tileIds,
                    name != null ? name : "",
                            locationCodes,
                            (extra ? LocalText.getText("extra"):LocalText.getText("notExtra")),
                            (free ? LocalText.getText("noCost") : discount != 0 ? LocalText.getText("discount", discount) : 
                                LocalText.getText("normalCost")),
                            (connected ? LocalText.getText("connected") : LocalText.getText("unconnected")),
                            		constraints
            );
        } else {
            description = LocalText.getText("LayTileInfo",
                    locationCodes,
                    (tileColours != null ? Arrays.toString(tileColours).replaceAll("[\\[\\]]", ""): ""),
                    (extra ? LocalText.getText("extra"):LocalText.getText("notExtra")),
                    (free ? LocalText.getText("noCost") : discount != 0 ? LocalText.getText("discount", discount) : 
                        LocalText.getText("normalCost")),
                    (connected ? LocalText.getText("connected") : LocalText.getText("unconnected")), 
                    constraints
            );
        }

    }

    @Override
	public void finishConfiguration (RailsRoot root)
    throws ConfigurationException {

        TileManager tmgr = root.getTileManager();
        MapManager mmgr = root.getMapManager();
        MapHex hex;
        Tile tile;

        tiles = new ArrayList<Tile>();
        if (tileIds != null) {
        	for (String tileId : tileIds) {
        		tile = tmgr.getTile(tileId);
        		if (tile == null)
        			throw new ConfigurationException("Tile " + tileId
                       + " does not exist");
        		tiles.add(tile);
        	}
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

    @Override
	public void setExercised(boolean value) {
		 occurred.add(1);
		 //After the first occurrance check the constraints...
		  if (occurred.value() == 1) { //FIXME: this assumes the only constraint that subsequent tile lays need to be connected to the first
			  if (constraints.equals("connected")) {
				  connected = true;
			  }
		  }
		 if (maxOccurrance == occurred.value()) {
			 super.setExercised(true);
		 }
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

    public String[] getTileIds() {
        return tileIds;
    }

     public String[] getTileColours() {
        return tileColours;
    }
    
    public List<Tile> getTiles() {
    	return tiles;
    }
    
    public String getConstraints() {
    	return constraints;
    }

    @Override
	public String toText() {
        return "SpecialMultiTileLay comp=" + originalCompany.getId()
        + " hex=" + locationCodes
        + " colour="+Util.joinWithDelimiter(tileColours, ",")
        + " extra=" + extra + " cost=" + free + " connected=" + connected + " constraints=" + constraints;
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
