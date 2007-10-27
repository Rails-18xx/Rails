/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/LayBonusToken.java,v 1.1 2007/10/27 15:26:35 evos Exp $
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
import rails.util.Util;

/**
 * @author Erik Vos
 */
public class LayBonusToken extends PossibleORAction {
    
    /*--- Preconditions ---*/
    
    /** Where to lay a token (null means anywhere) */
    transient protected List<MapHex> locations = null;
    protected String locationNames;
    
    /** Special property that will be fulfilled by this token lay.*/
    transient protected SpecialTokenLay specialProperty = null;
    protected int specialPropertyId; 
    
    /*--- Postconditions ---*/
    
    /** The map hex on which the token is laid */
    transient protected MapHex chosenHex = null;
    protected String chosenHexName;
    
     public LayBonusToken (SpecialTokenLay specialProperty) {
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

    public List<MapHex> getLocations() {
        return locations;
    }
    
    public boolean equals (PossibleAction action) {
        if (!(action instanceof LayBonusToken)) return false;
        LayBonusToken a = (LayBonusToken) action;
        return (a.locationNames == null && locationNames == null
                || a.locationNames.equals(locationNames))
            && a.company == company
            && a.specialProperty == specialProperty;
    }

    public String toString () {
        StringBuffer b = new StringBuffer  ("LayBonusToken ");
        if (chosenHex == null) {
        	b.append(" location=").append(locationNames)
        	 .append(" spec.prop=").append(specialProperty);
        } else {
        	b.append("hex=").append(chosenHex.getName());
        }
        return b.toString();
    }

    /** Deserialize */
	private void readObject (ObjectInputStream in) 
	throws IOException, ClassNotFoundException {

		in.defaultReadObject();
		
        MapManager mmgr = MapManager.getInstance();
        locations = new ArrayList<MapHex>();
        if (Util.hasValue(locationNames)) {
            for (String hexName : locationNames.split(",")) {
                locations.add(mmgr.getHex(hexName));
            }
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
