/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/ShareModel.java,v 1.7 2008/06/04 19:00:37 evos Exp $*/
package rails.game.model;

import rails.game.Player;
import rails.game.Portfolio;
import rails.game.PublicCompanyI;

public class ShareModel extends ModelObject {

    private int share;
    private Portfolio portfolio;
    private PublicCompanyI company;

    public ShareModel(Portfolio portfolio, PublicCompanyI company) {
        this.portfolio = portfolio;
        this.company = company;
        this.share = 0;
    }

    public void setShare() {
        this.share = portfolio.getShare(company);
        update();
    }

    public void addShare(int addedShare) {
        share += addedShare;
        update();
    }

    public int getShare() {
        return share;
    }

    @Override
    public String getText() {
        if (share == 0) return "";
        StringBuffer b = new StringBuffer();
        b.append(share).append("%");
        if (portfolio.getOwner() instanceof Player
            && company.getPresident() == portfolio.getOwner()) {
            b.append("P");
            if (!company.hasFloated()) b.append("U");
        }
        return b.toString();
    }

}
