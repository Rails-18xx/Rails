/**
 * This class implements the 1835 rules for making new companies
 * being available in the IPO after buying shares of another company.
 */
package rails.game.specific._1835;

import rails.game.*;
import rails.util.LocalText;

public class StockRound_1835 extends StockRound {
    
    public static String BY_ID="Bay";
    public static String SX_ID="Sax";
    public static String BA_ID="Bad";
    public static String HE_ID="Hes";
    public static String WT_ID="Wrt";
    public static String MS_ID="MS";
    public static String OL_ID="Old";
    public static String PR_ID="Pr";

    /**
     * Constructor with the GameManager, will call super class (StockRound's) Constructor to initialize
     *
     * @param aGameManager The GameManager Object needed to initialize the Stock Round
     *
     */
    public StockRound_1835 (GameManagerI aGameManager) {
        super (aGameManager);
    }
    
    /** Share price goes down 1 space for any number of shares sold.
     */
    protected void adjustSharePrice (PublicCompanyI company, int numberSold, boolean soldBefore) {
        // No more changes if it has already dropped
        if (!soldBefore) {
            company.adjustSharePrice (SOLD, 1, gameManager.getStockMarket());
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
            if (name.equals(SX_ID) &&
                ipo.getShare(companyManager.getCompanyByName(BY_ID)) == 0
            || name.equals(BY_ID) &&
                ipo.getShare(companyManager.getCompanyByName(SX_ID)) == 0) {
                // Group 1 sold out: release Badische
                releaseCompanyShares (companyManager.getCompanyByName(BA_ID));
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        "All", BA_ID));
            } else if (name.equals(BA_ID) || name.equals(WT_ID) || name.equals(HE_ID)) {
                if (ipo.getShare(companyManager.getCompanyByName(BA_ID)) == 0
                        && ipo.getShare(companyManager.getCompanyByName(WT_ID)) == 0
                        && ipo.getShare(companyManager.getCompanyByName(HE_ID)) == 0) {
                    // Group 2 sold out: release MS
                    releaseCompanyShares (companyManager.getCompanyByName(MS_ID));
                    ReportBuffer.add (LocalText.getText("SharesReleased",
                            "All", MS_ID));
                }
            }
        }

        // Check for releases within group
        /* We leave out the Bayern/Sachsen connection, as the latter
         * will always be available at the start of SR1.
         */
        if (name.equals(BA_ID)) {
            if (sharesInIPO == 50) {  // 50% sold: release Wurttemberg
                releaseCompanyShares (companyManager.getCompanyByName(WT_ID));
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        "All", WT_ID));
            } else if (sharesInIPO == 80) {
                // President sold: release four 10% Prussian shares
            	gameManager.getCompanyManager().getPublicCompany(PR_ID).setBuyable(true);
                for (int i=0; i<4; i++) {
                    unavailable.getCertOfType(PR_ID+"_10%").moveTo(ipo);
                }
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        "4 10%", PR_ID));
            }
        } else if (name.equals(WT_ID)) { //Wurttembergische
            if (sharesInIPO == 50) {  // 50% sold: release Hessische
                releaseCompanyShares (companyManager.getCompanyByName(HE_ID));
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        "All", HE_ID));
            }
        } else if (name.equals(MS_ID)) { // Mecklenburg/Schwerin
            if (sharesInIPO == 40) {  // 60% sold: release Oldenburg
                releaseCompanyShares (companyManager.getCompanyByName(OL_ID));
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        "All", OL_ID));
            }
        }
    }
}
