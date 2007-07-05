/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/LayToken.java,v 1.2 2007/07/05 17:57:54 evos Exp $
 * 
 * Created on 14-Sep-2006
 * Change Log:
 */
package rails.game.action;

import rails.game.MapHex;
import rails.game.special.SpecialTokenLay;

/**
 * @author Erik Vos
 */
public class LayToken extends PossibleORAction {
    
    /* LayTile types */
    public final static int GENERIC = 0; // Stop-gap only
    public final static int LOCATION_SPECIFIC = 1; // Valid hex 
    public final static int SPECIAL_PROPERTY = 2; // Directed by a special property
    
    protected int type = 0;
    
    /*--- Preconditions ---*/
    
    /** Where to lay a tile (null means anywhere) */
    MapHex location = null; 
    
    /** Special property that will be fulfilled by this tile lay.
     * If null, this is a normal tile lay. */
    SpecialTokenLay specialProperty = null;
    
    /*--- Postconditions ---*/
    
    /** The map hex on which the tile is laid */
    MapHex chosenHex = null;
    
    /** The station (or city) on the hex where the token is laid */
    int chosenStation = 0; // Default 

    /**
     * Allow laying a base token on a given location.
     */
    public LayToken(MapHex location) {
        type = LOCATION_SPECIFIC;
        this.location = location;
    }
    
     public LayToken (SpecialTokenLay specialProperty) {
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
    
    public int getChosenStation() {
        return chosenStation;
    }

    public void setChosenStation(int chosenStation) {
        this.chosenStation = chosenStation;
    }

    /**
     * @return Returns the specialProperty.
     */
    public SpecialTokenLay getSpecialProperty() {
        return specialProperty;
    }
    /**
     * @param specialProperty The specialProperty to set.
     */
    public void setSpecialProperty(SpecialTokenLay specialProperty) {
        this.specialProperty = specialProperty;
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
    
    public boolean equals (PossibleAction action) {
        if (!(action instanceof LayToken)) return false;
        LayToken a = (LayToken) action;
        return a.location == location
            && a.type == type
            && a.company == company
            && a.specialProperty == specialProperty;
    }

    public String toString () {
        return "LayToken type="+type+" location="+location+" spec.prop="+specialProperty;
    }
}
