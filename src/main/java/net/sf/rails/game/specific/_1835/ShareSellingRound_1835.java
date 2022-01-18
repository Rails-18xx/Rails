package net.sf.rails.game.specific._1835;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.state.Owner;

public class ShareSellingRound_1835 extends ShareSellingRound {

    public ShareSellingRound_1835(GameManager parent, String id) {
        super(parent, id);
    }

    /** Share price goes down 1 space for any number of shares sold.
     *  Rules explicitly state that different sale actions do NOT sell at the same price.
     *  So the parameter values of both numberSold and soldBefore are ignored.
     */
    @Override
    protected void adjustSharePrice (PublicCompany company, Owner seller, int sharesSold, boolean soldBefore) {
        super.adjustSharePrice (company, seller,1, false);
    }

}
