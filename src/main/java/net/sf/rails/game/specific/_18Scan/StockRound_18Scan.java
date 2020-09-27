package net.sf.rails.game.specific._18Scan;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.Phase;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.state.Portfolio;

public class StockRound_18Scan extends StockRound {

    public StockRound_18Scan (GameManager parent, String id) {
        super (parent, id);
    }

    protected void checkFlotation(PublicCompany company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        Phase currentPhase = gameManager.getCurrentPhase();
        boolean phase5Reached = getRoot().getPhaseManager().hasReachedPhase("5");
        if ("SJ".equalsIgnoreCase(company.getId())
                && !phase5Reached) {
            // The SJ does not float in a stock round before phase 5
        } else
        // Company floats if number of shares sold is equal to the current Phase (max 5).
        if (company.getSoldPercentage() >= 10 * Math.min (5, getPhaseNumber())) {
            // Company floats
            floatCompany(company);
            if (phase5Reached) {
                Portfolio.moveAll(ipo.getCertificates(company), pool.getParent());
            }
        }
    }

}
