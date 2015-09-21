package net.sf.rails.game.specific._1837;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.Train;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.Owner;

public class PublicCompany_1837 extends PublicCompany {

    
    public PublicCompany_1837(RailsItem parent, String id) {
        super(parent, id);
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.PublicCompany#mayBuyTrainType(net.sf.rails.game.Train)
     */
    @Override
    public boolean mayBuyTrainType(Train train) { // Coal trains in 1837 are only allowed to buy/operate G-Trains
        if (this.getType().getId().equals("Coal")){
            if (train.getType().getInfo().contains("G")){
                return true;
            }
            else {
                return false;
            }
        }
        return super.mayBuyTrainType(train);
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.PublicCompany#payout(int)
     */
    public void payout(int amount, boolean b) {
        if (amount == 0) return;

        // Move the token
        if (hasStockPrice
                && (!payoutMustExceedPriceToMove
                        || amount >= currentPrice.getPrice().getPrice())) {
           ((StockMarket_1837) getRoot().getStockMarket()).payOut(this, b);
        }

    }
    /* (non-Javadoc)
     * @see rails.game.PublicCompany#isSoldOut()
     */
    @Override
    public boolean isSoldOut() {
        Owner owner;

        for (PublicCertificate cert : certificates.view()) {
                owner = cert.getOwner();
                if ((owner instanceof BankPortfolio || owner == cert.getCompany()) && (!owner.getId().equalsIgnoreCase("unavailable"))) {
                    return false;
                }
            }
            return true;
    }
}
