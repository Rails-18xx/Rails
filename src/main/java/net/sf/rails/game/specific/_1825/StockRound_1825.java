/**
 * This class implements the 1835 rules for making new companies
 * being available in the IPO after buying shares of another company.
 */
package net.sf.rails.game.specific._1825;

import java.util.ArrayList;
import java.util.List;

import rails.game.action.SellShares;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.model.PortfolioModel;


public class StockRound_1825 extends StockRound {
    
    protected int[] priceBands = {100,90,82,76,71,67};
    private List<PublicCompany> lPublicCompanies = companyManager.getAllPublicCompanies();

    /**
     * Constructed via Configure
     */
    public StockRound_1825 (GameManager parent, String id) {
        super(parent, id);
    }
    
    protected void adjustSharePrice (PublicCompany company, int numberSold, boolean soldBefore) {
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
    protected void gameSpecificChecks (PortfolioModel boughtFrom, PublicCompany company) {
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
                    List<PublicCompany> lCompaniesToRelease = new ArrayList<PublicCompany>();
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


}
