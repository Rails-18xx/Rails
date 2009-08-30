/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/CertCountModel.java,v 1.6 2009/08/30 19:28:34 evos Exp $*/
package rails.game.model;

import rails.game.Player;

public class CertCountModel extends ModelObject {

    private Player owner;

    public CertCountModel(Player owner) {
        this.owner = owner;
    }

    public String getText() {
        return ("" + owner.getPortfolio().getCertificateCount()).replaceFirst(".0", "").replaceFirst(".5", "½");
    }

}
