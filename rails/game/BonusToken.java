/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/BonusToken.java,v 1.7 2008/06/04 19:00:31 evos Exp $
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

    public void close() {
        // new TokenMove (this, holder, Bank.getScrapHeap());
        new ObjectMove(this, holder, Bank.getScrapHeap());
        user.removeBonusToken(this);
    }

    @Override
    public void setHolder(TokenHolderI newHolder) {
        super.setHolder(newHolder);

        // Prepare for removal, is requested
        if (removingObjectDesc != null && removingObject == null) {
            String[] spec = removingObjectDesc.split(":");
            if (spec[0].equalsIgnoreCase("Phase")) {
                removingObject =
                        PhaseManager.getInstance().getPhaseNyName(spec[1]);
            }
        }

        // If the token is placed, prepare its removal when required
        if (newHolder instanceof MapHex && removingObject != null) {
            if (removingObject instanceof Phase) {
                ((Phase) removingObject).addObjectToClose(this);
            }
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
