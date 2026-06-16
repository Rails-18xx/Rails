// File: rails.game.action.RelayTokenAction.java
package rails.game.action;

import net.sf.rails.game.BaseToken;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Station;
import net.sf.rails.game.RailsRoot; // --- FIX: Import RailsRoot ---
import java.io.Serializable;
/**
 * A PossibleAction representing a specific choice to move a "homeless" token
 * (a token on a tile that is being upgraded) to a specific station on the new
 * tile.
 */
public class RelayTokenAction extends PossibleAction implements Serializable {

    private final transient PublicCompany company;
    private final transient BaseToken token;
    private final transient MapHex hex;
    private final transient Station targetStation;

    public RelayTokenAction(BaseToken token, MapHex hex, Station targetStation) {
        // The super() constructor for PossibleAction requires a RailsRoot
        super(token.getRoot()); 
        this.company = token.getParent(); // Store the company locally
        this.token = token;
        this.hex = hex;
        this.targetStation = targetStation;
        
        // Example Label: "Move Baden (SX) to Station 1"
        String label = String.format("Move %s to Station %d",
                token.getParent().getId(),
                targetStation.getNumber());
        this.setButtonLabel(label);
    }

    /**
     * Override getCompany() to return the stored company, as this
     * no longer extends PossibleORAction.
     */
     public PublicCompany getCompany() {
        return this.company;
    }

    public BaseToken getToken() {
        return token;
    }

    public MapHex getHex() {
        return hex;
    }

    public Station getTargetStation() {
        return targetStation;
    }
}