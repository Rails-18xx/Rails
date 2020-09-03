package net.sf.rails.game.specific._SOH;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.state.Owner;
import rails.game.action.PossibleAction;
import rails.game.action.StartCompany;

import java.util.List;

public class StockRound_SOH extends StockRound {

    public StockRound_SOH (GameManager parent, String id) {
        super (parent, id);
    }

    /**
     * In SOH a company is started by buying the number of *shares* required
     * to float the company in one single buy action.
     * That number of shares is equal to the phase name.<br><br>
     * Exception (necessary although the rules are silent about it):
     * in the very unlikely case that the NRS is started in phase 6,
     * by the player that owns the single NRS share that comes with the KO,
     * only 5 shares are bought, as the player may not own more than 6 shares.
     */
    @Override
    public void setBuyableCerts() {

        // An attempt to avoid rewriting this whole method (as in 18EU etc.),
        // by only replacing the possible actions that need special treatment.

        // First do a standard buyable shares setup
        super.setBuyableCerts();

        // Then replace all StartCompany actions as needed for SOH
        int playerCash = currentPlayer.getCash();
        int sharesToFloat = getPhaseNumber();
        PublicCompany company;
        StartCompany buyAction;
        int sharesToBuy;
        int numValidPrices;
        List<Integer> startPrices;

        for (PossibleAction action : possibleActions.getList()) {
            if (action instanceof StartCompany) {
                buyAction = (StartCompany) action;
                company = buyAction.getCompany();
                sharesToBuy = sharesToFloat;
                if (sharesToBuy == 6
                        && "NRS".equals(company.getId())
                        && currentPlayer.getPortfolioModel()
                            .findCertificate(company, false) != null) {
                    sharesToBuy = 5;
                }
                numValidPrices = 0;
                startPrices = stockMarket.getStartPrices();  // Should perhaps be sorted
                for (int price : startPrices) {
                    if (sharesToBuy * price <= playerCash) {
                        numValidPrices++;
                    } else {
                        break;
                    }
                }
                if (numValidPrices > 0) {
                    int[] newPrices = new int[numValidPrices];
                    for (int i=0; i<numValidPrices; i++) {
                        newPrices[i] = startPrices.get(i);
                    }
                    possibleActions.remove(action);
                    StartCompany startAction = new StartCompany (
                            company, newPrices, sharesToBuy);
                    // Fix the number to be bought
                    startAction.setNumberBought(sharesToBuy);
                    possibleActions.add (startAction);
                }
            }
        }
    }

    protected void checkFlotation(PublicCompany company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        // Company floats if number of shares sold is equal to the current Phase.
        // In fact, this is always the case, as the number of shares required
        // for floating is always bought in one action.
        if (company.getSoldPercentage() >= 10 * getPhaseNumber()) {
            floatCompany(company);
        }
    }

}
