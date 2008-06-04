/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/CertCountModel.java,v 1.4 2008/06/04 19:00:37 evos Exp $*/
package rails.game.model;

import rails.game.Player;

public class CertCountModel extends ModelObject {

    private Player owner;

    public CertCountModel(Player owner) {
        this.owner = owner;
    }

    public String getText() {
        return "" + owner.getPortfolio().getNumberOfCountedCertificates();
    }

}
