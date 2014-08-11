package net.sf.rails.game.specific._1862;

import rails.game.action.PossibleAction;

public class BidParliamentAction extends PossibleAction {
    private ParliamentBiddableItem biddable;
    private int actualBid;

    private static final long serialVersionUID = 1L;

    public BidParliamentAction(ParliamentBiddableItem biddable) {
        this.biddable = biddable;
    }
    
    public ParliamentBiddableItem getBiddable() {
        return biddable;
    }

    public boolean equalsAsAction(PossibleAction action) {
        if (!(action instanceof BidParliamentAction)) return false;
        BidParliamentAction a = (BidParliamentAction) action;
        if ((a.getBiddable() == biddable) && (a.getActualBid() == actualBid)) {
            return true;
        }
        return false;
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("BidParliamentAction ").append(biddable.getCompany().getId()).append(
                " actualBid=").append(actualBid);
        return b.toString();
    }

    public void setActualBid(int bid) {
        actualBid = bid;
    }
    
    public int getActualBid() {
        return actualBid;
    }

}
