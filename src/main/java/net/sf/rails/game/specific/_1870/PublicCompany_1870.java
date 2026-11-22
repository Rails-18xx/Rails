package net.sf.rails.game.specific._1870;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;

public class PublicCompany_1870 extends PublicCompany {

    public PublicCompany_1870(RailsItem parent, String id) {
        super(parent, id);
    }

    public PublicCompany_1870(RailsItem parent, String id, boolean hasStockPrice) {
        super(parent, id, hasStockPrice);
    }

    public String getDestinationHexId() {
        return this.destinationHexName;
    }

    public boolean hasConnected() {
        return this.hasReachedDestination();
    }

    public void setConnected(boolean connected) {
        this.setReachedDestination(connected);
    }

@Override
    public void payout(int amount) {
        String allocationText = getLastRevenueAllocation();
        String splitText = net.sf.rails.common.LocalText.getText(rails.game.action.SetDividend.getAllocationNameKey(rails.game.action.SetDividend.SPLIT));
        
        if (allocationText != null && allocationText.equals(splitText)) {
            // 1870 Rules: If half dividends are paid, the share value token does not move.
            return;
        }
        super.payout(amount);
    }


}