/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/LayBonusToken.java,v 1.3 2007/12/11 20:58:34 evos Exp $
 * 
 * Created on 14-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import rails.game.BonusToken;
import rails.game.MapHex;
import rails.game.MapManager;
import rails.game.Token;
import rails.game.TokenI;
import rails.game.special.SpecialProperty;
import rails.game.special.SpecialTokenLay;
import rails.util.Util;

/**
 * @author Erik Vos
 */
public class LayBonusToken extends LayToken {
	
	transient BonusToken token = null;
    String tokenId = null;
    
    /*--- Preconditions ---*/
    
    /*--- Postconditions ---*/
    
    public LayBonusToken (SpecialTokenLay specialProperty, BonusToken token) {
        super (specialProperty);
        this.token = token;
        this.tokenId = token.getUniqueId();
    }
    
    public BonusToken getToken() {
    	return token;
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

        if (tokenId != null) {
            token = (BonusToken) Token.getByUniqueId(tokenId);
        }
		if (specialPropertyId  > 0) {
			specialProperty = (SpecialTokenLay) SpecialProperty.getByUniqueId (specialPropertyId);
		}
		if (chosenHexName != null && chosenHexName.length() > 0) {
			chosenHex = MapManager.getInstance().getHex(chosenHexName);
		}
	}
    
    
}
