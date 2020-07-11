package net.sf.rails.game.specific._18Scan;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.specific._1856.PublicCompany_1856;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.MoneyOwner;
import net.sf.rails.game.state.Owner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.LayTile;
import rails.game.action.PossibleAction;
import rails.game.action.ReachDestinations;
import rails.game.action.SetDividend;

import java.util.ArrayList;
import java.util.List;

public class OperatingRound_18Scan extends OperatingRound {

    protected GameDef.OrStep[] steps = new GameDef.OrStep[]{
            GameDef.OrStep.INITIAL,
            GameDef.OrStep.LAY_TRACK,
            GameDef.OrStep.LAY_TOKEN,
            GameDef.OrStep.CALC_REVENUE,
            GameDef.OrStep.PAYOUT,
            GameDef.OrStep.BUY_TRAIN,
            GameDef.OrStep.FINAL
    };

    private List<PublicCompany> unreachedDestinations = new ArrayList<>();
    private List<PublicCompany> reachedDestinations = new ArrayList<>();

    private final String SJ = "SJ";

    private static final Logger log = LoggerFactory.getLogger(OperatingRound_18Scan.class);

    public OperatingRound_18Scan(GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    protected void executeTrainlessRevenue (GameDef.OrStep step) {
        // Minors always pay out something.
        PublicCompany company = operatingCompany.value();
        if (step == GameDef.OrStep.CALC_REVENUE && company.isOfType("Minor")
                && !company.canRunTrains()) {
            int amount = 10;
            String report = LocalText.getText("NoTrainsButBankPaysAnyway",
                    company.getId(),
                    Bank.format(this, amount),
                    company.getPresident().getId());
            log.debug("OR skips {}: Cannot run trains but still pays {}", step, amount);
            SetDividend action = new SetDividend(getRoot(), 0, false,
                    new int[]{SetDividend.PAYOUT});
            action.setRevenueAllocation(SetDividend.PAYOUT);
            action.setActualRevenue(amount);
            executeSetRevenueAndDividend(action, report);
        }
    }

    /**
     * This method checks if an upcoming OR step must be skipped for any reason not covered
     * in the superclass.
     * In 18Scan, CHECK_DESTINATIONS is an extra step between LAY_TILE and LAY_TOKEN,
     * because this is the only moment that a company can claim having reached a destination.
     * @param step The next OR Step to be considered for eligibility.
     * @return False if a reason is found to skip this step, else True.
     */
    @Override
    protected boolean gameSpecificNextStep(GameDef.OrStep step) {

        /* Disabled for now. Do this in the TOKEN_LAY step.
         if (step == GameDef.OrStep.CHECK_DESTINATIONS) {
            unreachedDestinations.clear();
            for (PublicCompany comp : operatingCompanies.view()) {
                if (!comp.isClosed() && comp.hasDestination() && !comp.hasReachedDestination()) {
                    unreachedDestinations.add(comp);
                }
            }
            return false;
            //return unreachedDestinations.size() > 0;
        } */
        return true;
    }

    // Almost identical to the same method in 1856,
    // except that a restriction to the LAY_TOKEN step has been added.
    @Override
    protected void setDestinationActions() {

        if (getStep() == GameDef.OrStep.LAY_TOKEN
                && !getRoot().getPhaseManager().hasReachedPhase("5")) {
            List<PublicCompany> possibleDestinations = new ArrayList<>();
            for (PublicCompany comp : operatingCompanies.view()) {
                if (!comp.isClosed() && comp.hasDestination() && !comp.hasReachedDestination()) {
                    possibleDestinations.add(comp);
                }
            }
            if (possibleDestinations.size() > 0) {
                possibleActions.add(new ReachDestinations(getRoot(), possibleDestinations));
            }
        }
    }

    @Override
    protected void executeDestinationActions(List<PublicCompany> companies) {

        ((GameManager_18Scan)gameManager).StartDestinationRuns (this, companies);

    }

    public void resume() {
        setStep (GameDef.OrStep.LAY_TOKEN);
        setPossibleActions();
    }

    @Override
    protected void newPhaseChecks() {
        Phase phase = Phase.getCurrent(this);
        if ( "5".equals(phase.getId())) {
            formSJ();
            checkForExcessTrains(); // Again, for new SJ
        }
    }

    private void formSJ () {
        PublicCompany sj = companyManager.getPublicCompany("SJ");
        List<Company> minors = companyManager.getCompaniesByType("Minor");
        String message;
        for (Company comp : minors) {
            PublicCompany minor = (PublicCompany) comp;
            MoneyOwner owner = minor.getPresident();
            PublicCertificate minorCert = minor.getPresidentsShare();
            minorCert.moveTo(getRoot().getBank().getScrapHeap());
            PublicCertificate sjCert = getRoot().getBank().getUnavailable()
                    .getPortfolioModel().findCertificate(sj, false);

            int cash = minor.getCash();
            int trains = minor.getPortfolioModel().getNumberOfTrains();
            int bonusTokens = minor.getBonuses().size();
            sjCert.moveTo(owner);
            sj.transferAssetsFrom(minor);
            ReportBuffer.add(this,
                    LocalText.getText("MinorTransfers",
                    minor.getId(),
                    Bank.format(this, cash),
                    trains,
                    bonusTokens,
                    SJ));

            for (BaseToken token : minor.getBaseTokensModel().getLaidTokens()) {
                // Procedure copied from 1835
                Stop city = (Stop) token.getOwner();
                MapHex hex = city.getParent();
                token.moveTo(minor);
                if (!hex.hasTokenOfCompany(sj) && hex.layBaseToken(sj, city)) {
                    message = LocalText.getText("ExchangesBaseToken",
                            SJ, minor.getId(),
                            city.getSpecificId());
                    ReportBuffer.add(this, message);

                    sj.layBaseToken(hex, 0); // Already laid, but this handles some consequences
                }
            }
            minor.setClosed();
            ReportBuffer.add (this,
                    LocalText.getText("MinorClosesAndShareExchanged",
                            minor.getId(), owner.getId(), sjCert.getShare(), SJ));
        }
        checkFlotation(sj);
        if (sj.hasFloated()) sj.checkPresidency();
    }
 }
