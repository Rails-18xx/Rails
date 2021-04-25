package net.sf.rails.game.specific._1837;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.Train;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Owner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicCompany_1837 extends PublicCompany {

    private static final Logger log = LoggerFactory.getLogger(PublicCompany_1837.class);

    public PublicCompany_1837(RailsItem parent, String id) {
        super(parent, id);
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.PublicCompany#mayBuyTrainType(net.sf.rails.game.Train)
     */
    @Override
    public boolean mayBuyTrainType(Train train) {
        // Coal trains in 1837 are only allowed to buy/operate G-Trains
        if (this.getType().getId().equals("Coal")){
            return train.getType().getCategory().equalsIgnoreCase("goods");
        }
        return super.mayBuyTrainType(train);
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.PublicCompany#payout(int)
     * TODO: rewrite using the new price rise configuration framework (EV)
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

    /**
     * Get the company price to be used in calculating a player's worth,
     * not only at game end but also during the game, for UI display.
     * Implements a proposal by John David Galt
     * @return Company share price to be used in calculating player worth.
     */
    @Override
    public int getGameEndPrice() {
        String type = getType().getId();
        int price = 0;
        switch (type) {
            case "Coal":
            case "Minor1":
            case "Minor2":
                price = getRelatedPublicCompany().getMarketPrice();
                break;
            case "Major":
            case "National":
                price = getMarketPrice();
        }
        log.debug("$$$ {} price is {}", getId(), price);
        return price;
    }
}
