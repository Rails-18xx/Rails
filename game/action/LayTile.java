/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/LayTile.java,v 1.3 2006/11/26 19:30:00 evos Exp $
 * 
 * Created on 14-Sep-2006
 * Change Log:
 */
package game.action;

import java.util.*;

import game.MapHex;
import game.TileI;
import game.special.SpecialTileLay;

/**
 * @author Erik Vos
 */
public class LayTile extends PossibleAction {
    
    /* LayTile types */
    public final static int GENERIC = 0; // Stop-gap only
    public final static int LOCATION_SPECIFIC = 1; // Valis hex and allowed tiles 
    public final static int SPECIAL_PROPERTY = 2; // Directed by a special property
    
    protected int type = 0;
    
    /*--- Preconditions ---*/
    
    /** Where to lay a tile (null means anywhere) */
    private MapHex location = null; 
    
    /** Highest tile colour (empty means unspecified) */
    private Map tileColours = null;
    
    /** Allowed tiles on a specific location (empty means unspecified) */
    private List tiles = null;
    
    /** Special property that will be fulfilled by this tile lay.
     * If null, this is a normal tile lay. */
    private SpecialTileLay specialProperty = null;
    
    /*--- Postconditions ---*/
    
    /** The tile actually laid */
    private TileI laidTile = null;
    
    /** The map hex on which the tile is laid */
    private MapHex chosenHex = null;

    /**
     * Allow laying a tile on a given location.
     */
    public LayTile(MapHex location, List tiles) {
        type = LOCATION_SPECIFIC;
        this.location = location;
        this.tiles = tiles;
    }
    
    public LayTile(Map tileColours) {
        type = GENERIC;
        this.tileColours = tileColours;
    }
    
     public LayTile (SpecialTileLay specialProperty) {
        type = SPECIAL_PROPERTY;
        this.location = specialProperty.getLocation();
        this.specialProperty = specialProperty;
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
    }
    /**
     * @return Returns the tiles.
     */
    public List getTiles() {
        return tiles;
    }
    /**
     * @param tiles The tiles to set.
     */
    public void setTiles(List tiles) {
        this.tiles = tiles;
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
    public Map getTileColours() {
        return tileColours;
    }
    public boolean isTileColourAllowed (String tileColour) {
        return tileColours.containsKey(tileColour);
    }
    public void setTileColours (Map map) {
        tileColours = map;
    }
    
    public String toString () {
        return "LayTile type="+type+" location="+location+" spec.prop="+specialProperty;
    }
}
