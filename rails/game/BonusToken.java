/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/BonusToken.java,v 1.17 2010/03/21 17:43:50 evos Exp $
 *
 * Created on Jan 1, 2007
 * Change Log:
 */
package rails.game;

import rails.game.move.ObjectMove;
import rails.util.Tag;
import rails.util.Util;

/**
 * A BonusToken object represents a token that a operating public company can
 * place on the map to gain extra revenue or other privileges.
 * <p>Such tokens are usually not placed in city slots, 
 * which are intended for base tokens, but on some unoccupied part of a tile.  
 *
 * @author Erik Vos
 */
public class BonusToken extends Token implements Closeable, ConfigurableComponentI  {

    int value;
    String name;
    String removingObjectDesc = null;
    Object removingObject = null;
    PublicCompanyI user = null;

    /**
     * Create a BonusToken.
     */
    public BonusToken() {
        super();
        setHolder(null);
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
        description = name + " +" + Bank.format(value) + " bonus token";

        removingObjectDesc = bonusTokenTag.getAttributeAsString("removed");
    }

    public void finishConfiguration(GameManagerI gameManager) {
        prepareForRemoval (gameManager.getPhaseManager());
    }

    /**
     * Remove the token.
     * This method can be called by a certain phase when it starts.
     * See prepareForRemovel().
     */
    public void close() {

        new ObjectMove(this, holder, GameManager.getInstance().getBank().getScrapHeap());
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

    public void setUser(PublicCompanyI user) {
        this.user = user;
    }

    public boolean isPlaced() {
        return (holder instanceof MapHex);
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return description;
    }

    public String getClosingInfo () {
        return description;
    }

}
