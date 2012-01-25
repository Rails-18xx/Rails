/**
 * This class implements the 1880 rules for making new companies
 * being available in the IPO after buying shares of another company.
 */
package rails.game.specific._1880;

import rails.common.LocalText;
import rails.game.*;
import rails.game.move.CashMove;
import rails.game.specific._1880.PublicCompany_1880;

public class StockRound_1880 extends StockRound {

    /**
     * Constructor with the GameManager, will call super class (StockRound's)
     * Constructor to initialize
     * 
     * @param aGameManager The GameManager Object needed to initialize the Stock
     * Round
     * 
     */
    public StockRound_1880(GameManagerI aGameManager) {
        super(aGameManager);
    }

    @Override
    // The sell-in-same-turn-at-decreasing-price option does not apply here
    protected int getCurrentSellPrice(PublicCompanyI company) {

        String companyName = company.getName();
        int price;

        if (sellPrices.containsKey(companyName)) {
            price = (sellPrices.get(companyName)).getPrice();
        } else {
            price = company.getCurrentSpace().getPrice();
        }
        if (!((PublicCompany_1880) company).isCommunistPhase()) {
            // stored price is the previous unadjusted price
            price = price / company.getShareUnitsForSharePrice();
            // Price adjusted by -5 per share for selling but only if we are not
            // in CommunistPhase...
            price = price - 5;
        }
        return price;
    }

    /**
     * Share price goes down 1 space for any number of shares sold.
     */
    @Override
    protected void adjustSharePrice(PublicCompanyI company, int numberSold,
            boolean soldBefore) {
        // No more changes if it has already dropped
        // Or we are in the CommunistTakeOverPhase after the 4T has been bought
        // and the 6T has not yet been bought
        if ((!soldBefore)
            || (!((PublicCompany_1880) company).isCommunistPhase())) {
            super.adjustSharePrice(company, 1, soldBefore);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * rails.game.StockRound#mayPlayerSellShareOfCompany(rails.game.PublicCompanyI
     * )
     */
    @Override
    public boolean mayPlayerSellShareOfCompany(PublicCompanyI company) {
        if ((company.getPresident() == gameManager.getCurrentPlayer())
            && (((PublicCompany_1880) company).isCommunistPhase())) {
            return false;
        }
        return super.mayPlayerSellShareOfCompany(company);
    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#gameSpecificChecks(rails.game.Portfolio, rails.game.PublicCompanyI)
     */
    @Override
    protected void gameSpecificChecks(Portfolio boughtFrom,
            PublicCompanyI company) {
        if (company.getCapitalisation() == 0) return; // If the company is already fully capitalized do nothing
        if (!company.hasFloated()) return; // If the company is not floated no money (Phase 6  problematic) 
        if ((getSoldPercentage(company)>= 50) && (((PublicCompany_1880) company).shouldBeCapitalisedFull())) {
                    ((PublicCompany_1880)company).setCapitalisation(0); //CAPITALISATION_FULL
                    int additionalOperatingCapital;
                    additionalOperatingCapital=company.getIPOPrice()*5;
                    company.addCash(additionalOperatingCapital);
                    // Can be used as 1880 has no game end on bank break or should CashMove() be used ?
        }// TODO: Do we need to add money to the companies wallet somewhere ?
        super.gameSpecificChecks(boughtFrom, company);
    }
    /**
     * Float a company, including a default implementation of moving cash and
     * shares as a result of flotation. <p>Fifty Percent capitalisation is implemented
     * as in 1880. 
     */
    @Override
    protected void floatCompany(PublicCompanyI company) {
        // Move cash and shares where required
        int cash = 0;
        int price = company.getIPOPrice();
        
        
        // For all Companies who float before the first 6T is purchased the Full Capitalisation will happen on purchase of 
        // 50 percent of the shares.
        // The exception of the rule of course are the late starting companies after the first 6 has been bought when the 
        // flotation will happen after 60 percent have been bought.
        if (((PublicCompany_1880) company).shanghaiExchangeIsOperational()) {
            cash = 10 * price;
        } else {
            cash = 5 * price;
        }
       
        company.setFloated(); 

        if (cash > 0) {
            new CashMove(bank, company, cash);
            ReportBuffer.add(LocalText.getText("FloatsWithCash",
                    company.getName(),
                    Bank.format(cash) ));
        } else {
            ReportBuffer.add(LocalText.getText("Floats",
                    company.getName()));
        }

    }
    
}
