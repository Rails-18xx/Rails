package net.sf.rails.game.specific._18Scan;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.specific._1856.PublicCompany_1856;
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
 }
