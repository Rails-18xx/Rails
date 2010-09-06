/**
 * This class implements the 1835 rules for making new companies
 * being available in the IPO after buying shares of another company.
 */
package rails.game.specific._1835;

import java.util.ArrayList;
import java.util.List;

import rails.game.*;
import rails.game.action.BuyCertificate;
import rails.util.LocalText;

public class StockRound_1835 extends StockRound {

    /**
     * Constructor with the GameManager, will call super class (StockRound's) Constructor to initialize
     *
     * @param aGameManager The GameManager Object needed to initialize the Stock Round
     *
     */
    public StockRound_1835 (GameManagerI aGameManager) {
        super (aGameManager);
    }

    /** Add nationalisations */
    @Override
    protected void setGameSpecificActions() {
        if (!mayCurrentPlayerBuyAnything()) return;
        if (companyBoughtThisTurnWrapper.get() != null) return;

        List<Player> otherPlayers = new ArrayList<Player>();
        Portfolio holder;
        CashHolder owner;
        Player otherPlayer;
        int price;
        int cash = currentPlayer.getCash();

        // Nationalization
        for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {
            if (!company.getTypeName().equalsIgnoreCase("Major")) continue;
            if (!company.hasFloated()) continue;
            if (company.getPresident() != currentPlayer) continue;
            if (currentPlayer.getPortfolio().getShare(company) >= 55) {
                otherPlayers.clear();
                for (PublicCertificateI cert : company.getCertificates()) {
                    holder = (Portfolio)cert.getHolder();
                    owner = holder.getOwner(); 
                    /* Would the player exceed the total certificate limit? */
                    StockSpaceI stockSpace = company.getCurrentSpace();
                    if ((stockSpace == null || !stockSpace.isNoCertLimit()) && !mayPlayerBuyCertificate(
                            currentPlayer, company, cert.getCertificateCount())) continue;
                    // only nationalize other players
                    if (owner instanceof Player && owner != currentPlayer) {
                        otherPlayer = (Player) owner;
                        if (!otherPlayers.contains(otherPlayer)) {
                            price = (int)(1.5 * company.getCurrentPriceModel().getPrice().getPrice());
                            if (price <= cash) {
                                possibleActions.add(new BuyCertificate (company, cert.getShare(),
                                        holder,
                                    (int)(1.5 * company.getCurrentPriceModel().getPrice().getPrice()),
                                    1));
                            }
                            otherPlayers.add(otherPlayer);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean checkAgainstHoldLimit(Player player, PublicCompanyI company, int number) {
        return true;
    }

    @Override
    protected int getBuyPrice (BuyCertificate action, StockSpaceI currentSpace) {
        int price = currentSpace.getPrice();
        if (action.getFromPortfolio().getOwner() instanceof Player) {
            price *= 1.5;
        }
        return price;
    }

    @Override
    // The sell-in-same-turn-at-decreasing-price option does not apply here
    protected int getCurrentSellPrice (PublicCompanyI company) {

        String companyName = company.getName();
        int price;

        if (sellPrices.containsKey(companyName)) {
            price = (sellPrices.get(companyName)).getPrice();
        } else {
            price = company.getCurrentSpace().getPrice();
        }
        // stored price is the previous unadjusted price
        price = price / company.getShareUnitsForSharePrice();
    return price;
    }


    /** Share price goes down 1 space for any number of shares sold.
     */
    @Override
    protected void adjustSharePrice (PublicCompanyI company, int numberSold, boolean soldBefore) {
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
    @Override
    protected void gameSpecificChecks (Portfolio boughtFrom,
            PublicCompanyI company) {

        if (boughtFrom != ipo) return;

        String name = company.getName();
        int sharesInIPO = ipo.getShare(company);

        // Check for group releases
        if (sharesInIPO == 0) {
            if (name.equals(GameManager_1835.SX_ID) &&
                ipo.getShare(companyManager.getPublicCompany(GameManager_1835.BY_ID)) == 0
            || name.equals(GameManager_1835.BY_ID) &&
                ipo.getShare(companyManager.getPublicCompany(GameManager_1835.SX_ID)) == 0) {
                // Group 1 sold out: release Badische
                releaseCompanyShares (companyManager.getPublicCompany(GameManager_1835.BA_ID));
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        "All", GameManager_1835.BA_ID));
            } else if (name.equals(GameManager_1835.BA_ID) || name.equals(GameManager_1835.WT_ID) || name.equals(GameManager_1835.HE_ID)) {
                if (ipo.getShare(companyManager.getPublicCompany(GameManager_1835.BA_ID)) == 0
                        && ipo.getShare(companyManager.getPublicCompany(GameManager_1835.WT_ID)) == 0
                        && ipo.getShare(companyManager.getPublicCompany(GameManager_1835.HE_ID)) == 0) {
                    // Group 2 sold out: release MS
                    releaseCompanyShares (companyManager.getPublicCompany(GameManager_1835.MS_ID));
                    ReportBuffer.add (LocalText.getText("SharesReleased",
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
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        "All", GameManager_1835.WT_ID));
            } else if (sharesInIPO == 80) {
                // President sold: release four 10% Prussian shares
            	gameManager.getCompanyManager().getPublicCompany(GameManager_1835.PR_ID).setBuyable(true);
                for (int i=0; i<4; i++) {
                    unavailable.getCertOfType(GameManager_1835.PR_ID+"_10%").moveTo(ipo);
                }
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        "4 10%", GameManager_1835.PR_ID));
            }
        } else if (name.equals(GameManager_1835.WT_ID)) { //Wurttembergische
            if (sharesInIPO == 50) {  // 50% sold: release Hessische
                releaseCompanyShares (companyManager.getPublicCompany(GameManager_1835.HE_ID));
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        "All", GameManager_1835.HE_ID));
            }
        } else if (name.equals(GameManager_1835.MS_ID)) { // Mecklenburg/Schwerin
            if (sharesInIPO == 40) {  // 60% sold: release Oldenburg
                releaseCompanyShares (companyManager.getPublicCompany(GameManager_1835.OL_ID));
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        "All", GameManager_1835.OL_ID));
            }
        }
    }
}
