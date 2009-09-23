/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/BonusToken.java,v 1.10 2009/09/23 21:38:57 evos Exp $
 *
 * Created on Jan 1, 2007
 * Change Log:
 */
package rails.game;

import rails.game.move.ObjectMove;
import rails.util.Tag;
import rails.util.Util;

/**
 * A BaseToken object represents a token that a operating public company can
 * place on the map to act as a rail building and train running starting point.
 * <p> The "Base" qualifier is used (more or less) consistently in this
 * rails.game program as it most closely the function of such a token: to act as
 * a base from which a company can operate. Other names used in various games
 * and discussions are "railhead", "station", "garrison", or just "token".
 *
 * @author Erik Vos
 */
public class BonusToken extends Token implements Closeable {

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

    /**
     * Remove the token.
     * This method can be called by a certain phase when it starts.
     * See prepareForRemovel().
     */
    public void close() {

        new ObjectMove(this, holder, Bank.getScrapHeap());
        user.removeBonus(name);
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

}
