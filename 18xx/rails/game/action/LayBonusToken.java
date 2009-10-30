/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/LayBonusToken.java,v 1.7 2009/10/30 21:53:04 evos Exp $
 *
 * Created on 14-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import rails.game.*;
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

    public static final long serialVersionUID = 1L;

    public LayBonusToken(SpecialTokenLay specialProperty, BonusToken token) {
        super(specialProperty);
        this.token = token;
        this.tokenId = token.getUniqueId();
    }

    public BonusToken getToken() {
        return token;
    }

    @Override
	public boolean equals(PossibleAction action) {
        if (!(action instanceof LayBonusToken)) return false;
        LayBonusToken a = (LayBonusToken) action;
        return (a.locationNames == null && locationNames == null || a.locationNames.equals(locationNames))
               && a.company == company && a.specialProperty == specialProperty;
    }

    @Override
	public String toString() {
        StringBuffer b = new StringBuffer("LayBonusToken ");
        if (chosenHex == null) {
            b.append(" location=").append(locationNames).append(" spec.prop=").append(
                    specialProperty);
        } else {
            b.append("hex=").append(chosenHex.getName());
        }
        return b.toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        MapManager mmgr = gameManager.getMapManager();
        locations = new ArrayList<MapHex>();
        if (Util.hasValue(locationNames)) {
            for (String hexName : locationNames.split(",")) {
                locations.add(mmgr.getHex(hexName));
            }
        }

        if (tokenId != null) {
            token = (BonusToken) Token.getByUniqueId(tokenId);
        }
        if (specialPropertyId > 0) {
            specialProperty =
                    (SpecialTokenLay) SpecialProperty.getByUniqueId(specialPropertyId);
        }
        if (chosenHexName != null && chosenHexName.length() > 0) {
            chosenHex = mmgr.getHex(chosenHexName);
        }
    }

}
