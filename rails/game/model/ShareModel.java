/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/ShareModel.java,v 1.8 2009/10/06 18:34:04 evos Exp $*/
package rails.game.model;

import rails.game.*;

public class ShareModel extends ModelObject {

    private int share;
    private Portfolio portfolio;
    private PublicCompanyI company;

    public ShareModel(Portfolio portfolio, PublicCompanyI company) {
        this.portfolio = portfolio;
        this.company = company;
        this.share = 0;
        // add companies president model as observer
        this.addObserver(company.getPresidentModel());
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
            b.append(company.getExtraShareMarks());
        }
        return b.toString();
    }

}
