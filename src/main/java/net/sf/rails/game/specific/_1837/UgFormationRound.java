package net.sf.rails.game.specific._1837;

import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UgFormationRound extends NationalFormationRound {
    private static final Logger log = LoggerFactory.getLogger(UgFormationRound.class);

    public UgFormationRound(GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    protected void processExchange(PublicCompany minor, PublicCompany major, ExchangeMinorAction action) {

        log.info("1837_UG_NFR: Processing specialized exchange for " + minor.getId());
        
        // 1. Mandatory Floating & Capitalization (875K)
        if (action.isFormation() && !major.hasFloated()) {
            net.sf.rails.game.financial.StockMarket market = getRoot().getStockMarket();
            net.sf.rails.game.financial.StockSpace parSpace = null;
            int targetPar = 175; // Rule: Ug starts at 175

            for (net.sf.rails.game.financial.StockSpace ss : market.getStartSpaces()) {
                if (ss.getPrice() == targetPar) {
                    parSpace = ss;
                    break;
                }
            }
            if (parSpace != null) major.setCurrentSpace(parSpace);
            
            // Inject starting capital!
            net.sf.rails.game.state.Currency.fromBank(875, major);
            major.setFloated();
            net.sf.rails.common.ReportBuffer.add(gameManager, major.getId() + " successfully floated and received 875K starting capital.");
net.sf.rails.common.ReportBuffer.add(gameManager, "Initial " + major.getId() + " State:\n" + Merger1837.build1837StateReport(gameManager, major));
            
            log.info("1837_UG_NFR: Ug successfully floated and received 875K.");
        }

        // Asset transfer natively executes the U1/U3 forced co-exchange by sweeping all minor certs
        Merger1837.mergeMinor(gameManager, minor, major);
    }



}