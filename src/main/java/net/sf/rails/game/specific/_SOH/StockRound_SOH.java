package net.sf.rails.game.specific._SOH;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.Phase;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.StockRound;

public class StockRound_SOH extends StockRound {

    public StockRound_SOH (GameManager parent, String id) {
        super (parent, id);
    }

    protected void checkFlotation(PublicCompany company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        Phase currentPhase = gameManager.getCurrentPhase();
        // Company floats if number of shares sold is equal to the current Phase.
        // In fact, this is always the case, as the number of shares required
        // for floating is always bought in one action.
        if (company.getSoldPercentage() >= 10 * currentPhase.getIndex()) {
            floatCompany(company);
        }
    }

}
