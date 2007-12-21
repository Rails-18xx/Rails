/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/LayToken.java,v 1.7 2007/12/21 21:18:13 evos Exp $
 * 
 * Created on 14-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.util.List;

import rails.game.MapHex;
import rails.game.special.SpecialTokenLay;

/**
 * @author Erik Vos
 */
public abstract class LayToken extends PossibleORAction {
    
    /*--- Preconditions ---*/
    
    /** Where to lay a token (null means anywhere) */
    transient protected List<MapHex> locations = null;
    protected String locationNames;
    
    /** Special property that will be fulfilled by this token lay.
     * If null, this is a normal token lay. */
    transient protected SpecialTokenLay specialProperty = null;
    protected int specialPropertyId; 
    
    /*--- Postconditions ---*/
    
    /** The map hex on which the token is laid */
    transient protected MapHex chosenHex = null;
    protected String chosenHexName;
    
    /**
     * Allow laying a base token on a given location.
     */
    public LayToken(List<MapHex> locations) {
        this.locations = locations;
        if (locations != null) {
            this.locations = locations;
            buildLocationNameString();
        }
        
    }
    
     public LayToken (SpecialTokenLay specialProperty) {
        this.locations = specialProperty.getLocations();
        if (locations != null) buildLocationNameString();
        this.specialProperty = specialProperty;
        this.specialPropertyId = specialProperty.getUniqueId();
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
        // TODO this.specialPropertyUniqueId = specialProperty.getUniqueId();
    }

   /**
    * @deprecated
     * @return Returns the location.
     */
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
        return locationNames;
    }
    private void buildLocationNameString () {
        StringBuffer b = new StringBuffer();
        for (MapHex hex : locations) {
            if (b.length() > 0) b.append(",");
            b.append(hex.getName());
        }
        locationNames = b.toString();
    }
    
}
