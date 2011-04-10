/**
 * This class implements the 1835 rules for making new companies
 * being available in the IPO after buying shares of another company.
 */
package rails.game.specific._1825;

import java.util.ArrayList;
import java.util.List;

import rails.game.*;
import rails.game.action.SellShares;

public class StockRound_1825 extends StockRound {
    
    protected int[] priceBands = {100,90,82,76,71,67};
    private List<PublicCompanyI> lPublicCompanies = companyManager.getAllPublicCompanies();
    /**
     * Constructor with the GameManager, will call super class (StockRound's) Constructor to initialize
     *
     * @param aGameManager The GameManager Object needed to initialize the Stock Round
     * 
     */
    public StockRound_1825 (GameManagerI aGameManager) {
        super (aGameManager);
    }
    
    protected void adjustSharePrice (PublicCompanyI company, int numberSold, boolean soldBefore) {
        // Sales do not affect share price, do nothing
    }
    
    /**
     * In 1825, whenever a company is sold out, the block of companies with the next lowest price become available.
     *
     * Hopefully this logic is generic enough to withstand any combination of units for future implementation
     *
     * @param boughtfrom The portfolio from which a certificate has been bought.
     * @param company The company of which a share has been traded.
     */
    @Override
    protected void gameSpecificChecks (Portfolio boughtFrom, PublicCompanyI company) {
        if (boughtFrom != ipo) return;

        int sharesInIPO = ipo.getShare(company);

        // Check for group releases
        if (sharesInIPO == 0) {
            //Need to release the next block of companies
            
            //First find out what price band we were just in
            //then find out what the next price band is
            //then cycle through companies to find ones at that price
            //then set them buyable
            for (int i = 0; i < priceBands.length; i++) {
                if (priceBands[i] == company.getParPriceModel().getPrice().getPrice()) {
                    //Found the price band we were in
                    //We had better break out now if it was the last price band or the next loop
                    //will run infinitely
                    if (priceBands[i] == priceBands[priceBands.length - 1]) return;
                    List<PublicCompanyI> lCompaniesToRelease = new ArrayList<PublicCompanyI>();
                    while (lCompaniesToRelease.isEmpty()) {
                        //while loop needed in case we have no corps at the next valid price band
                        for (int k = 0; k < companyManager.getAllPublicCompanies().size(); k++){
                            //for each public company
                            if (lPublicCompanies.get(k).getIPOPrice() == priceBands[i+1]){
                                //this companies IPO matches the next price band value, add it to the list
                                lCompaniesToRelease.add(lPublicCompanies.get(k));
                            }
                        }
                        //If we found corps the loop won't repeat, if we didn't we need the next price band
                        i++;
                    }
                    //We should have found some companies to release now, so do that
                    for (int j = 0; j < lCompaniesToRelease.size(); j++) {
                        releaseCompanyShares(lCompaniesToRelease.get(j));
                        lCompaniesToRelease.get(j).setBuyable(true);
                    }
                }
            }
        }

    }

    @Override
    public void setSellableShares() {
        if (!mayCurrentPlayerSellAnything()) return;

        String compName;
        int price;
        int number;
        int maxShareToSell;
        Portfolio playerPortfolio = currentPlayer.getPortfolio();

        /*
         * First check of which companies the player owns stock, and what
         * maximum percentage he is allowed to sell.
         */
        for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {

            // Check if shares of this company can be sold at all
            if (!mayPlayerSellShareOfCompany(company)) continue;

            maxShareToSell = playerPortfolio.getShare(company);
            if (maxShareToSell == 0) continue;

            /* May not sell more than the Pool can accept */
            maxShareToSell =
                Math.min(maxShareToSell,
                        getGameParameterAsInt(GameDef.Parm.POOL_SHARE_LIMIT)
                        - pool.getShare(company));
            if (maxShareToSell == 0) continue;

            /*
             * Check what share units the player actually owns. In some games
             * (e.g. 1835) companies may have different ordinary shares: 5% and
             * 10%, or 10% and 20%. The president's share counts as a multiple
             * of the smallest ordinary share unit type.
             */
            // Take care for max. 4 share units per share
            int[] shareCountPerUnit = new int[5];
            compName = company.getName();
            for (PublicCertificateI c : playerPortfolio.getCertificatesPerCompany(compName)) {
                if (c.isPresidentShare()) {
                    shareCountPerUnit[1] += c.getShares();
                } else {
                    ++shareCountPerUnit[c.getShares()];
                }
            }
            // TODO The above ignores that a dumped player must be
            // able to exchange the president's share.

            /*
             * Check the price. If a cert was sold before this turn, the
             * original price is still valid
             */
            price = getCurrentSellPrice(company);

            // removed as this is done in getCurrentSellPrice
            // price /= company.getShareUnitsForSharePrice();

            /* Allow for different share units (as in 1835) */
            for (int i = 1; i <= 4; i++) {
                number = shareCountPerUnit[i];
                if (number == 0) continue;
                number =
                    Math.min(number, maxShareToSell
                            / (i * company.getShareUnit()));

                /* In some games (1856), a just bought share may not be sold */
                // This code ignores the possibility of different share units
                if ((Boolean)gameManager.getGameParameter(GameDef.Parm.NO_SALE_OF_JUST_BOUGHT_CERT)
                        && company.equals(companyBoughtThisTurnWrapper.get())) {
                    number--;
                }
                if (number <= 0) continue;

                possibleActions.add(new SellShares(compName, i, number, price));

            }
        }
        
        
    }
    

}
