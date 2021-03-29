package net.sf.rails.game.specific._18Scan;

import com.google.common.collect.ImmutableSetMultimap;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.Portfolio;
import rails.game.action.BuyCertificate;

public class StockRound_18Scan extends StockRound {

    public StockRound_18Scan (GameManager parent, String id) {
        super (parent, id);
    }

    /**
     * Minors and privates can end up in the Pool after a bankruptcy.
     * The rules do not explicitly say so, but the above can only mean
     * that these certificates are buyable at list price.
     *
     * Only minors will be considered here.
     * Buying a private after a bankruptcy is too unlikely in my view (EV)
     */
    public void setBuyableCerts() {

        super.setBuyableCerts();

        ImmutableSetMultimap<PublicCompany, PublicCertificate> map =
                pool.getCertsPerCompanyMap();
        for (PublicCompany comp : map.keySet()) {
            if ("Minor".equalsIgnoreCase(comp.getType().getId())) {
                if (!mayPlayerBuyCertificate(currentPlayer, comp, 1)) continue;
                comp.setBuyable(true);
                BuyCertificate bc = new BuyCertificate(comp, 100, pool.getParent(),
                        comp.getFixedPrice(), 1);
                bc.setPresident(true);
                possibleActions.add(bc);
            }
        }
    }


    protected void checkFlotation(PublicCompany company) {

        if (!company.hasStarted() || company.hasFloated()) return;

        boolean phase5Reached = getRoot().getPhaseManager().hasReachedPhase("5");
        if (company.getId().equalsIgnoreCase(GameDef_18Scan.SJ)
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

    @Override
    protected void gameSpecificChecks(PortfolioModel boughtFrom,
                                      PublicCompany company, boolean arg) {

        if (company.isHibernating()
                && currentPlayer.getPortfolioModel().findCertificate(company, true) != null) {
            company.setHibernating(false);
        }
    }


}
