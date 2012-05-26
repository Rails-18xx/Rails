/**
 * This class implements the 1880 rules for making new companies
 * being available in the IPO after buying shares of another company.
 */
package rails.game.specific._1880;

import rails.game.GameManager;
import rails.game.PublicCompany;
import rails.game.StockRound;


public class StockRound_1880 extends StockRound {

    /**
     * Constructor with the GameManager, will call super class (StockRound's) Constructor to initialize
     *
     * @param aGameManager The GameManager Object needed to initialize the Stock Round
     *
     */
    public StockRound_1880 (GameManager aGameManager) {
        super (aGameManager);
    }

    
    @Override
    // The sell-in-same-turn-at-decreasing-price option does not apply here
    protected int getCurrentSellPrice (PublicCompany company) {

        String companyName = company.getId();
        int price;

        if (sellPrices.containsKey(companyName)) {
            price = (sellPrices.get(companyName)).getPrice();
        } else {
            price = company.getCurrentSpace().getPrice();
        }
        // stored price is the previous unadjusted price
        price = price / company.getShareUnitsForSharePrice();
        // Price adjusted by -5 per share for selling
        price = price -5;
    return price;
    }

    /** Share price goes down 1 space for any number of shares sold.
     */
    @Override
    protected void adjustSharePrice (PublicCompany company, int numberSold, boolean soldBefore) {
        // No more changes if it has already dropped
        if (!soldBefore) {
            super.adjustSharePrice (company, 1, soldBefore);
        }
    }
}

 