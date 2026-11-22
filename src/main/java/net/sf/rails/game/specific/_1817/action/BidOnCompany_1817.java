package net.sf.rails.game.specific._1817.action;

import rails.game.action.PossibleAction;

public class BidOnCompany_1817 extends PossibleAction {
    private static final long serialVersionUID = 1L;
    private final String companyId;
    private int bidAmount;
    private int maxBid;

    public BidOnCompany_1817(net.sf.rails.game.RailsRoot root, String companyId, int minBid, int maxBid) {
        super(root);
        this.companyId = companyId;
        this.bidAmount = minBid;
        this.maxBid = maxBid;
    }

    public String getCompanyId() { return companyId; }
    public int getBidAmount() { return bidAmount; }
    public void setBidAmount(int bidAmount) { this.bidAmount = bidAmount; }
    public int getMaxBid() { return maxBid; }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        if (pa == this) return true;
        if (!super.equalsAs(pa, asOption)) return false;
        BidOnCompany_1817 action = (BidOnCompany_1817) pa;
        return this.companyId.equals(action.companyId);
    }

    @Override
    public String toString() { 
        return "Bid $" + bidAmount + " on " + companyId; 
    }
}