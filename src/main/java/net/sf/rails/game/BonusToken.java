package net.sf.rails.game;

import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.util.Util;

/**
 * A BonusToken object represents a token that a operating public company can
 * place on the map to gain extra revenue or other privileges.
 * <p>Such tokens are usually not placed in city slots, 
 * which are intended for base tokens, but on some unoccupied part of a tile.  
 */

public class BonusToken extends Token<BonusToken> implements Closeable, Configurable  {

    private int value;
    private String name;
    private String removingObjectDesc = null;
    private Object removingObject = null;
    private PublicCompany user = null;

    private BonusToken(RailsItem parent, String id) {
        super(parent, id, BonusToken.class);
    }
    
    public static BonusToken create(RailsItem parent) {
        String uniqueId = Token.createUniqueId(parent);
        BonusToken token = new BonusToken(parent, uniqueId);
        return token;
    }
    
    public void configureFromXML(Tag tag) throws ConfigurationException {
        Tag bonusTokenTag = tag.getChild("BonusToken");
        if (bonusTokenTag == null) {
            throw new ConfigurationException("<BonusToken> tag missing");
        }
        value = bonusTokenTag.getAttributeAsInteger("value");
        if (value <= 0) {
            throw new ConfigurationException("Missing or invalid value "
                                             + value);
        }

        name = bonusTokenTag.getAttributeAsString("name");
        if (!Util.hasValue(name)) {
            throw new ConfigurationException("Bonus token must have a name");
        }
        description = name + " +" + Bank.format(this, value) + " bonus token";

        removingObjectDesc = bonusTokenTag.getAttributeAsString("removed");
    }

    public void finishConfiguration(RailsRoot root) {
        prepareForRemoval (root.getPhaseManager());
    }

    /**
     * Remove the token.
     * This method can be called by a certain phase when it starts.
     * See prepareForRemovel().
     */
    public void close() {
        this.moveTo(getRoot().getBank().getScrapHeap());
        if (user != null) {
            user.removeBonus(name);
        }
    }

    /**
     * Prepare the bonus token for removal, if so configured.
     * The only case currently implemented to trigger removal
     * is the start of a given phase.
     */
    public void prepareForRemoval (PhaseManager phaseManager) {

        if (removingObjectDesc == null) return;

        if (removingObject == null) {
            String[] spec = removingObjectDesc.split(":");
            if (spec[0].equalsIgnoreCase("Phase")) {
                removingObject =
                        phaseManager.getPhaseByName(spec[1]);
            }
        }

        if (removingObject instanceof Phase) {
            ((Phase) removingObject).addObjectToClose(this);
        }
    }

    public void setUser(PublicCompany user) {
        this.user = user;
    }

    public boolean isPlaced() {
        return (getOwner() instanceof MapHex);
    }

    public String getId() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public String getClosingInfo () {
        return description;
    }

    
    @Override
    public String toString() {
        return description;
    }
}
