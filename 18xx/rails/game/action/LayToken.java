/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/LayToken.java,v 1.4 2007/09/20 19:49:27 evos Exp $
 * 
 * Created on 14-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

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
    
    /** The station (or city) on the hex where the token is laid */
    protected int chosenStation = 0; // Default 

    /**
     * Allow laying a base token on a given location.
     */
    public LayToken(List<MapHex> locations) {
        type = LOCATION_SPECIFIC;
        this.locations = locations;
        if (locations != null) {
            this.locations = locations;
            buildLocationNameString();
        }
        
    }
    
     public LayToken (SpecialTokenLay specialProperty) {
        type = SPECIAL_PROPERTY;
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
    
    public int getType () {
        return type;
    }
    
    public boolean equals (PossibleAction action) {
        if (!(action instanceof LayToken)) return false;
        LayToken a = (LayToken) action;
        return (a.locationNames == null && locationNames == null
                || a.locationNames.equals(locationNames))
            && a.type == type
            && a.company == company
            && a.specialProperty == specialProperty;
    }

    public String toString () {
        StringBuffer b = new StringBuffer  ("LayToken ");
        if (chosenHex == null) {
        	b.append("type=").append(type)
        	 .append(" location=").append(locationNames)
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
		
        MapManager mmgr = MapManager.getInstance();
        locations = new ArrayList<MapHex>();
        for (String hexName : locationNames.split(",")) {
            locations.add(mmgr.getHex(hexName));
        }

		if (specialPropertyId  > 0) {
			specialProperty = (SpecialTokenLay) SpecialProperty.getByUniqueId (specialPropertyId);
		}
		if (chosenHexName != null && chosenHexName.length() > 0) {
			chosenHex = MapManager.getInstance().getHex(chosenHexName);
		}
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
