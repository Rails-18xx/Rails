package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import com.google.common.base.Objects;

import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.game.BonusToken;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Token;
import net.sf.rails.game.special.SpecialBonusTokenLay;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.util.Util;

/**
 * Rails 2.0: updated equals and toString methods
 */
public class LayBonusToken extends LayToken {

    transient BonusToken token = null;
    String tokenId = null;

    /*--- Preconditions ---*/

    /*--- Postconditions ---*/

    public static final long serialVersionUID = 1L;

    public LayBonusToken(SpecialBonusTokenLay specialProperty, BonusToken token) {
        super(specialProperty);
        this.token = token;
        this.tokenId = token.getUniqueId();
    }

    public void finishConfiguration (RailsRoot root)
    throws ConfigurationException {
        token.prepareForRemoval(root.getPhaseManager());
    }

    public BonusToken getToken() {
        return token;
    }
    
    @Override
    public SpecialBonusTokenLay getSpecialProperty() {
        return (SpecialBonusTokenLay)specialProperty;
    }
    
    @Override
    public int getPotentialCost(MapHex hex) {
        return 0;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        LayBonusToken action = (LayBonusToken)pa; 
        return Objects.equal(this.token, action.token);
        // no asAction attributes to be checked
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("LayBonusToken ");
        if (chosenHex == null) {
            b.append(" location=").append(locationNames).append(" spec.prop=").append(
                    specialProperty);
        } else {
            b.append("hex=").append(chosenHex.getId());
        }
        return b.toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        MapManager mmgr = getRoot().getMapManager();
        locations = new ArrayList<MapHex>();
        if (Util.hasValue(locationNames)) {
            for (String hexName : locationNames.split(",")) {
                locations.add(mmgr.getHex(hexName));
            }
        }

        if (tokenId != null) {
            token = Token.getByUniqueId(getRoot(), BonusToken.class, tokenId);
        }
        if (specialPropertyId > 0) {
            specialProperty =
                    (SpecialBonusTokenLay) SpecialProperty.getByUniqueId(getRoot(), specialPropertyId);
        }
        if (chosenHexName != null && chosenHexName.length() > 0) {
            chosenHex = mmgr.getHex(chosenHexName);
        }
    }

}
