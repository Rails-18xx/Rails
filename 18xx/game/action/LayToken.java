/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/LayToken.java,v 1.1 2006/12/03 21:38:13 evos Exp $
 * 
 * Created on 14-Sep-2006
 * Change Log:
 */
package game.action;

import java.util.*;

import game.MapHex;
import game.PublicCompanyI;
import game.TileI;
import game.special.SpecialTileLay;
import game.special.SpecialTokenLay;

/**
 * @author Erik Vos
 */
public class LayToken extends PossibleAction {
    
    /* LayTile types */
    public final static int GENERIC = 0; // Stop-gap only
    public final static int LOCATION_SPECIFIC = 1; // Valid hex 
    public final static int SPECIAL_PROPERTY = 2; // Directed by a special property
    
    protected int type = 0;
    
    /*--- Preconditions ---*/
    
    /** Where to lay a tile (null means anywhere) */
    private MapHex location = null; 
    
     /** Which company's token (null = any) */
    private PublicCompanyI company = null;
    
    /** Special property that will be fulfilled by this tile lay.
     * If null, this is a normal tile lay. */
    private SpecialTokenLay specialProperty = null;
    
    /*--- Postconditions ---*/
    
    /** The map hex on which the tile is laid */
    private MapHex chosenHex = null;

    /**
     * Allow laying a tile on a given location.
     */
    public LayToken(MapHex location, PublicCompanyI company) {
        type = LOCATION_SPECIFIC;
        this.location = location;
        this.company = company;
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
     * @return Returns the tiles.
     */
    public PublicCompanyI getCompany() {
        return company;
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
    
    public String toString () {
        return "LayToken type="+type+" location="+location+" spec.prop="+specialProperty;
    }
}
