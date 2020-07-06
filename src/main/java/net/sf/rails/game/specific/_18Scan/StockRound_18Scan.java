package net.sf.rails.game.specific._18Scan;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.StockRound;

public class StockRound_18Scan extends StockRound {

    public StockRound_18Scan (GameManager parent, String id) {
        super (parent, id);
    }

    protected void checkFlotation(PublicCompany company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        // Company floats if number of shares sold is equal to the current Phase (max 5).
        if (company.getSoldPercentage() >= 10 * Math.min (5,
                (2 + gameManager.getCurrentPhase().getIndex()))) {
            // Company floats
            floatCompany(company);
        }
    }

}
