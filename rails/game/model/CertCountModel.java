/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/CertCountModel.java,v 1.7 2009/08/30 20:50:29 evos Exp $*/
package rails.game.model;

import rails.game.Player;

public class CertCountModel extends ModelObject {

    private Player owner;

    public CertCountModel(Player owner) {
        this.owner = owner;
    }

    public String getText() {
        return ("" + owner.getPortfolio().getCertificateCount()).replaceFirst(".0", "").replaceFirst(".5", "\u00bd");
    }

}
