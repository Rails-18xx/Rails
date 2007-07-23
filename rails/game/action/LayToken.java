/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/LayToken.java,v 1.3 2007/07/23 19:59:16 evos Exp $
 * 
 * Created on 14-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.MapHex;
import rails.game.MapManager;
import rails.game.special.SpecialProperty;
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
    transient protected MapHex location = null;
    protected String locationName;
    
    /** Special property that will be fulfilled by this tile lay.
     * If null, this is a normal tile lay. */
    transient protected SpecialTokenLay specialProperty = null;
    protected int specialPropertyId; 
    
    /*--- Postconditions ---*/
    
    /** The map hex on which the tile is laid */
    transient protected MapHex chosenHex = null;
    protected String chosenHexName;
    
    /** The station (or city) on the hex where the token is laid */
    protected int chosenStation = 0; // Default 

    /**
     * Allow laying a base token on a given location.
     */
    public LayToken(MapHex location) {
        type = LOCATION_SPECIFIC;
        this.location = location;
        if (location != null) this.locationName = location.getName();
    }
    
     public LayToken (SpecialTokenLay specialProperty) {
        type = SPECIAL_PROPERTY;
        this.location = specialProperty.getLocation();
        if (location != null) this.locationName = location.getName();
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
        // TODO this.specialPropertyUniqueId = specialProperty.getUniqueId();
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
        StringBuffer b = new StringBuffer  ("LayToken ");
        if (chosenHex == null) {
        	b.append("type=").append(type)
        	 .append(" location=").append(location)
        	 .append(" spec.prop=").append(specialProperty);
        } else {
        	b.append("hex=").append(chosenHex.getName())
        	 .append(" station=").append(chosenStation);
        }
        return b.toString();
    }

    /** Deserialize */
	private void readObject (ObjectInputStream in) 
	throws IOException, ClassNotFoundException {

		in.defaultReadObject();
		
		location = MapManager.getInstance().getHex(locationName);
		if (specialPropertyId  > 0) {
			specialProperty = (SpecialTokenLay) SpecialProperty.getByUniqueId (specialPropertyId);
		}
		if (chosenHexName != null && chosenHexName.length() > 0) {
			chosenHex = MapManager.getInstance().getHex(chosenHexName);
		}
	}
}
