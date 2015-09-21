/**
 * This class implements the 1835 rules for making new companies
 * being available in the IPO after buying shares of another company.
 */
package net.sf.rails.game.specific._1835;

import java.util.Set;

import rails.game.action.BuyCertificate;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.model.PortfolioModel;


public class StockRound_1835 extends StockRound {

    /**
     * Constructed via Configure
     */
    public StockRound_1835 (GameManager parent, String id) {
        super(parent, id);
    }

    /** Add nationalisations */
    // change: nationalization is a specific BuyCertificate activity
    // requires: add a new activity
    @Override
    public void setBuyableCerts() {

        super.setBuyableCerts();
        if (companyBoughtThisTurnWrapper.value() != null) return;

        int price;
        int cash = currentPlayer.getCash();
        Set<PublicCertificate> certs;
        StockSpace stockSpace;
        PortfolioModel from;
        int unitsForPrice;

        // Nationalisation
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {
            if (!company.getType().getId().equalsIgnoreCase("Major")) continue;
            if (!company.hasFloated()) continue;
            if (company.getPresident() != currentPlayer) continue;
            if (currentPlayer.getPortfolioModel().getShare(company) < 55) continue;
            if (currentPlayer.hasSoldThisRound(company)) continue;
            
            for (Player otherPlayer : getRoot().getPlayerManager().getPlayers()) {
                if (otherPlayer == currentPlayer) continue;

                /* Get the unique player certificates and check which ones can be bought */
                from = otherPlayer.getPortfolioModel();
                certs = from.getCertificates(company);
                if (certs == null || certs.isEmpty()) continue;

                /* Allow for multiple share unit certificates (e.g. 1835) */
                PublicCertificate[] uniqueCerts;
                int shares;

                stockSpace = company.getCurrentSpace();
                unitsForPrice = company.getShareUnitsForSharePrice();
                price = (int)(1.5 * stockSpace.getPrice() / unitsForPrice);

                /* Check what share multiples are available
                 * Normally only 1, but 1 and 2 in 1835. Allow up to 4.
                 */
                uniqueCerts = new PublicCertificate[5];
                for (PublicCertificate cert2 : certs) {
                    shares = cert2.getShares();
                    if (uniqueCerts[shares] != null) continue;
                    uniqueCerts[shares] = cert2;
                }

                /* Create a BuyCertificate action per share size */
                for (shares = 1; shares < 5; shares++) {
                    if (uniqueCerts[shares] == null) continue;

                    /* Would the player exceed the total certificate limit? */
                    if (!stockSpace.isNoCertLimit()
                            && !mayPlayerBuyCertificate(currentPlayer, company,
                                    uniqueCerts[shares].getCertificateCount()))
                        continue;

                    // Does the player have enough cash?
                    if (cash < price * shares) continue;

                    possibleActions.add(new BuyCertificate(company,
                            uniqueCerts[shares].getShare(),
                            from.getParent(), price, 1));
                }
            }
        }
    }

    @Override
    // change: there is no holding limit in 1835
    // requires: should be parameterized?
    public boolean checkAgainstHoldLimit(Player player, PublicCompany company, int number) {
        return true;
    }

    @Override
    // change: price differs for nationalization action
    // requires: move into new activity
    protected int getBuyPrice (BuyCertificate action, StockSpace currentSpace) {
        int price = currentSpace.getPrice();
        if (action.getFromPortfolio().getParent() instanceof Player) {
            price *= 1.5;
        }
        return price;
    }

    // The sell-in-same-turn-at-decreasing-price option does not apply here
    
    // change: check exactly how it differs here
    // requires: do a parameterization
    @Override
    protected int getCurrentSellPrice (PublicCompany company) {

        int price;
        if (sellPrices.containsKey(company)) {
            price = (sellPrices.get(company)).getPrice();
        } else {
            price = company.getCurrentSpace().getPrice();
        }
        // stored price is the previous unadjusted price
        price = price / company.getShareUnitsForSharePrice();
        return price;
    }


    /** Share price goes down 1 space for any number of shares sold.
     */
    // change: specific share price adjustment
    // requires: do a parameterization
    @Override
    protected void adjustSharePrice (PublicCompany company, int numberSold, boolean soldBefore) {
        // No more changes if it has already dropped
        if (!soldBefore) {
            super.adjustSharePrice (company, 1, soldBefore);
        }
    }

    /**
     * The company release rules for 1835.
     *
     * For now these rules are hardcoded (which makes this code vulnerable
     * to company name changes!). It did not seem worthwhile to
     * invent come complex XML for the unique 1835 rules on this matter.
     *
     * @param boughtfrom The portfolio from which a certificate has been bought.
     * @param company The company of which a share has been traded.
     */
    // change: release of shares to IPO
    // requires: trigger on IPO portfolio
    @Override
    protected void gameSpecificChecks (PortfolioModel boughtFrom,
            PublicCompany company) {

        if (boughtFrom != ipo) return;

        String name = company.getId();
        int sharesInIPO = ipo.getShare(company);

        // Check for group releases
        if (sharesInIPO == 0) {
            if (name.equals(GameManager_1835.SX_ID) &&
                    ipo.getShare(companyManager.getPublicCompany(GameManager_1835.BY_ID)) == 0
                    || name.equals(GameManager_1835.BY_ID) &&
                    ipo.getShare(companyManager.getPublicCompany(GameManager_1835.SX_ID)) == 0) {
                // Group 1 sold out: release Badische
                releaseCompanyShares (companyManager.getPublicCompany(GameManager_1835.BA_ID));
                ReportBuffer.add(this, LocalText.getText("SharesReleased",
                        "All", GameManager_1835.BA_ID));
            } else if (name.equals(GameManager_1835.BA_ID) || name.equals(GameManager_1835.WT_ID) || name.equals(GameManager_1835.HE_ID)) {
                if (ipo.getShare(companyManager.getPublicCompany(GameManager_1835.BA_ID)) == 0
                        && ipo.getShare(companyManager.getPublicCompany(GameManager_1835.WT_ID)) == 0
                        && ipo.getShare(companyManager.getPublicCompany(GameManager_1835.HE_ID)) == 0) {
                    // Group 2 sold out: release MS
                    releaseCompanyShares (companyManager.getPublicCompany(GameManager_1835.MS_ID));
                    ReportBuffer.add(this, LocalText.getText("SharesReleased",
                            "All", GameManager_1835.MS_ID));
                }
            }
        }

        // Check for releases within group
        /* We leave out the Bayern/Sachsen connection, as the latter
         * will always be available at the start of SR1.
         */
        if (name.equals(GameManager_1835.BA_ID)) {
            if (sharesInIPO == 50) {  // 50% sold: release Wurttemberg
                releaseCompanyShares (companyManager.getPublicCompany(GameManager_1835.WT_ID));
                ReportBuffer.add(this, LocalText.getText("SharesReleased",
                        "All", GameManager_1835.WT_ID));
            } else if (sharesInIPO == 80) {
                // President sold: release four 10% Prussian shares
                companyManager.getPublicCompany(GameManager_1835.PR_ID).setBuyable(true);
                for (int i=0; i<4; i++) {
                    unavailable.getAnyCertOfType(GameManager_1835.PR_ID+"_10%").moveTo(ipo.getParent());
                }
                ReportBuffer.add(this, LocalText.getText("SharesReleased",
                        "4 10%", GameManager_1835.PR_ID));
            }
        } else if (name.equals(GameManager_1835.WT_ID)) { //Wurttembergische
            if (sharesInIPO == 50) {  // 50% sold: release Hessische
                releaseCompanyShares (companyManager.getPublicCompany(GameManager_1835.HE_ID));
                ReportBuffer.add(this, LocalText.getText("SharesReleased",
                        "All", GameManager_1835.HE_ID));
            }
        } else if (name.equals(GameManager_1835.MS_ID)) { // Mecklenburg/Schwerin
            if (sharesInIPO == 40) {  // 60% sold: release Oldenburg
                releaseCompanyShares (companyManager.getPublicCompany(GameManager_1835.OL_ID));
                ReportBuffer.add(this, LocalText.getText("SharesReleased",
                        "All", GameManager_1835.OL_ID));
            }
        }
    }
}
