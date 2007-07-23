/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/LayTile.java,v 1.5 2007/07/23 19:59:16 evos Exp $
 * 
 * Created on 14-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

import rails.game.MapHex;
import rails.game.MapManager;
import rails.game.TileI;
import rails.game.TileManager;
import rails.game.special.SpecialProperty;
import rails.game.special.SpecialTileLay;


/**
 * @author Erik Vos
 */
public class LayTile extends PossibleORAction {
    
    /* LayTile types */
    public final static int GENERIC = 0; // Stop-gap only
    public final static int LOCATION_SPECIFIC = 1; // Valis hex and allowed tiles 
    public final static int SPECIAL_PROPERTY = 2; // Directed by a special property
    
    protected int type = 0;
    
    /*--- Preconditions ---*/
    
    /** Where to lay a tile (null means anywhere) */
    transient private MapHex location = null;
    private String locationName;
    
    /** Highest tile colour (empty means unspecified) */
    private Map<String, Integer> tileColours = null;
    
    /** Allowed tiles on a specific location (empty means unspecified) */
    transient private List<TileI> tiles = null;
    private int[] tileIds;
    
    /** Special property that will be fulfilled by this tile lay.
     * If null, this is a normal tile lay. */
    transient private SpecialTileLay specialProperty = null;
    private int specialPropertyId;
    
    /*--- Postconditions ---*/
    
    /** The tile actually laid */
    transient private TileI laidTile = null;
    private int laidTileId;
    
    /** The map hex on which the tile is laid */
    transient private MapHex chosenHex = null;
    private String chosenHexName;
    
    /** The tile orientation */
    private int orientation;

    /**
     * Allow laying a tile on a given location.
     */
    public LayTile(MapHex location, List<TileI> tiles) {
        type = LOCATION_SPECIFIC;
        this.location = location;
        if (location != null) this.locationName = location.getName();
        setTiles (tiles);
    }
    
    public LayTile(Map<String, Integer> tileColours) {
        type = GENERIC;
        this.tileColours = tileColours;
    }
    
     public LayTile (SpecialTileLay specialProperty) {
        type = SPECIAL_PROPERTY;
        this.location = specialProperty.getLocation();
        if (location != null) this.locationName = this.location.getName();
        this.specialProperty = specialProperty;
        if (specialProperty != null) this.specialPropertyId = specialProperty.getUniqueId();
    }

    /**
     * @return Returns the chosenHex.
     */
    public MapHex getChosenHex() {
        return chosenHex;
    }
    /**
     * @param chosenHex The chosenHex to set.
     */
    public void setChosenHex(MapHex chosenHex) {
        this.chosenHex = chosenHex;
        this.chosenHexName = chosenHex.getName();
    }
    
    
    public int getOrientation() {
		return orientation;
	}

	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}

	/**
     * @return Returns the laidTile.
     */
    public TileI getLaidTile() {
        return laidTile;
    }
    /**
     * @param laidTile The laidTile to set.
     */
    public void setLaidTile(TileI laidTile) {
        this.laidTile = laidTile;
        this.laidTileId = laidTile.getId();
    }
    /**
     * @return Returns the specialProperty.
     */
    public SpecialTileLay getSpecialProperty() {
        return specialProperty;
    }
    /**
     * @param specialProperty The specialProperty to set.
     */
    public void setSpecialProperty(SpecialTileLay specialProperty) {
        this.specialProperty = specialProperty;
        // TODO this.specialPropertyName = specialProperty.getUniqueId();
    }
    /**
     * @return Returns the tiles.
     */
    public List<TileI> getTiles() {
        return tiles;
    }
    /**
     * @param tiles The tiles to set.
     */
    public void setTiles(List<TileI> tiles) {
        this.tiles = tiles;
        this.tileIds = new int[tiles.size()];
        for (int i=0; i<tiles.size(); i++) {
            tileIds[i] = tiles.get(i).getId();
        }
    }
    /**
     * @return Returns the location.
     */
    public MapHex getLocation() {
        return location;
    }
    
    public int getType () {
        return type;
    }
    
    /**
     * @return Returns the tileColours.
     */
    public Map<String, Integer> getTileColours() {
        return tileColours;
    }
    public boolean isTileColourAllowed (String tileColour) {
        return tileColours.containsKey(tileColour);
    }
    public void setTileColours (Map<String, Integer> map) {
        tileColours = map;
    }
    
    public boolean equals (PossibleAction action) {
        if (!(action instanceof LayTile)) return false;
        LayTile a = (LayTile) action;
        return a.location == location
            && a.type == type
            && a.tileColours == tileColours
            && a.tiles == tiles
            && a.specialProperty == specialProperty;
    }

    public String toString () {
        StringBuffer b = new StringBuffer("LayTile");
        if (laidTile == null) {
	        b.append(" type=").append(type);
	        if (location != null) b.append(" location=").append(location);
	        if (specialProperty != null) b.append(" spec.prop=").append(specialProperty);
	        if (tileColours != null && !tileColours.isEmpty()) {
	            String key;
	            int value;
	            for (Iterator it = tileColours.keySet().iterator(); it.hasNext(); ) {
	                key = (String) it.next();
	                value = ((Integer)tileColours.get(key)).intValue();
	                b.append(" ").append(key).append(":").append(value);
	            }
	        }
        } else {
        	b.append(" tile=").append(laidTile.getName())
        	 .append(" hex=").append(chosenHex.getName())
        	 .append(" orientation=").append(orientation);
        }
        return b.toString();
    }
    
    /** Deserialize */
	private void readObject (ObjectInputStream in) 
	throws IOException, ClassNotFoundException {

		in.defaultReadObject();
		
		location = MapManager.getInstance().getHex(locationName);
		if (tileIds != null
				&& tileIds.length > 0) {
			tiles = new ArrayList<TileI>();
			for (int i=0; i<tileIds.length; i++) {
				tiles.add (TileManager.get().getTile(tileIds[i]));
			}
		}
		if (specialPropertyId  > 0) {
			specialProperty = (SpecialTileLay) SpecialProperty.getByUniqueId (specialPropertyId);
		}
		if (laidTileId != 0) {
			laidTile = TileManager.get().getTile(laidTileId);
		}
		if (chosenHexName != null && chosenHexName.length() > 0) {
			chosenHex = MapManager.getInstance().getHex(chosenHexName);
		}
	}

}
