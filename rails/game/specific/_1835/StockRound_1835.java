/**
 * This class implements the 1835 rules for making new companies
 * being available in the IPO after buying shares of another company.
 */
package rails.game.specific._1835;

import rails.game.*;
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
	
    /**
     * The company release rules for 1835.
     * 
     * For now these rules are hardcoded (which makes this code vulnerablt
     * to company name changes!). It did not seem worthwhile to 
     * invent come complex XML for the unique 1835 rules on this matter.
     * 
     * @param boughtfrom The portfolio from which a certificate has been bought.
     * @param company The company of which a share has been traded.
     */
    protected void gameSpecificChecks (Portfolio boughtFrom,
            PublicCompanyI company) {
        
        if (boughtFrom != ipo) return;
        
        String name = company.getName();
        int sharesInIPO = ipo.getShare(company);
        
        // Check for group releases
        if (sharesInIPO == 0) {
            if (name.equals("Sax") && 
                ipo.getShare(companyManager.getCompanyByName("Bay")) == 0
            || name.equals("Bay") && 
                ipo.getShare(companyManager.getCompanyByName("Sax")) == 0) {
                // Group 1 sold out: release Badische
                releaseCompanyShares (companyManager.getCompanyByName("Bad"));
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        new String[] {"All", "Bad"}));
            } else if (name.equals("Bad") || name.equals("Wrt") || name.equals("Hes")) {
                if (ipo.getShare(companyManager.getCompanyByName("Bad")) == 0
                        && ipo.getShare(companyManager.getCompanyByName("Wrt")) == 0
                        && ipo.getShare(companyManager.getCompanyByName("Hes")) == 0) {
                    // Group 2 sold out: release MS
                    releaseCompanyShares (companyManager.getCompanyByName("MS"));
                    ReportBuffer.add (LocalText.getText("SharesReleased",
                            new String[] {"All", "MS"}));
                }
            }
        }
        
        // Check for releases within group
        /* We leave out the Bayern/Sachsen connection, as the latter
         * will always be available at the start of SR1.
         */
        if (name.equals("Bad")) {
            if (sharesInIPO == 50) {  // 50% sold: release Wurttemberg
                releaseCompanyShares (companyManager.getCompanyByName("Wrt"));
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        new String[] {"All", "Wrt"}));
            } else if (sharesInIPO == 80) { 
                // President sold: release four 10% Prussian shares
                for (int i=0; i<4; i++) {
                    unavailable.getCertOfType("Pr_10%").moveTo(ipo);
                }
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        new String[] {"4 10%", "Pr"}));
            }
        } else if (name.equals("Wrt")) { //Wurttembergische
            if (sharesInIPO == 50) {  // 50% sold: release Hessische
                releaseCompanyShares (companyManager.getCompanyByName("Hes"));
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        new String[] {"All", "Hes"}));
            }
        } else if (name.equals("MS")) { // Mecklenburg/Schwerin
            if (sharesInIPO == 40) {  // 60% sold: release Oldenburg
                releaseCompanyShares (companyManager.getCompanyByName("Old"));
                ReportBuffer.add (LocalText.getText("SharesReleased",
                        new String[] {"All", "Old"}));
            }
        }
    }
}
