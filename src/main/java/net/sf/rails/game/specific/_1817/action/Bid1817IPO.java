package net.sf.rails.game.specific._1817.action;

import rails.game.action.PossibleAction;
import net.sf.rails.game.RailsRoot;

public class Bid1817IPO extends PossibleAction {
    
    private static final long serialVersionUID = 2L;
    private int bidAmount;
    private int maxBidAmount;

    public Bid1817IPO(RailsRoot root, int bidAmount) {
        super(root);
        this.bidAmount = bidAmount;
        this.maxBidAmount = 9990; // Default no effective limit for IPOs
    }

    public Bid1817IPO(RailsRoot root, int bidAmount, int maxBidAmount) {
        super(root);
        this.bidAmount = bidAmount;
        this.maxBidAmount = maxBidAmount;
    }

    public int getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(int bidAmount) {
        this.bidAmount = bidAmount;
    }

    public int getMaxBidAmount() {
        return maxBidAmount;
    }

    @Override
    public String toString() {
        return "Bid $" + bidAmount;
    }
}