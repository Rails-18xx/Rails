package net.sf.rails.game.specific._1826;

import net.sf.rails.game.*;
import net.sf.rails.game.special.SpecialRight;
import rails.game.action.*;

import java.util.ArrayList;
import java.util.List;

public class OperatingRound_1826 extends OperatingRound {

    public OperatingRound_1826(GameManager parent, String id) {
        super(parent, id);

        steps = new GameDef.OrStep[]{
                GameDef.OrStep.INITIAL,
                GameDef.OrStep.LAY_TRACK,
                GameDef.OrStep.LAY_TOKEN,
                GameDef.OrStep.CALC_REVENUE,
                GameDef.OrStep.PAYOUT,
                GameDef.OrStep.BUY_TRAIN,
                GameDef.OrStep.TRADE_SHARES,
                //GameDef.OrStep.REPAY_LOANS,
                GameDef.OrStep.FINAL
        };
    }

    @Override
    protected void setDestinationActions() {

        List<PublicCompany> possibleDestinations = new ArrayList<>();
        for (PublicCompany comp : operatingCompanies.view()) {
            if (comp.hasDestination()
                    && comp.getNumberOfShares() == 5
                    && !comp.hasReachedDestination()) {
                possibleDestinations.add(comp);
            }
        }
        if (possibleDestinations.size() > 0) {
            possibleActions.add(new ReachDestinations(getRoot(), possibleDestinations));
        }
    }

    @Override
    protected void executeDestinationActions(List<PublicCompany> companies) {

        for (PublicCompany company : companies) {
            company.setReachedDestination(true);
        }
    }

    @Override
    protected void setGameSpecificPossibleActions() {

        PublicCompany_1826 company = (PublicCompany_1826) getOperatingCompany();

        // Once the destination is reached, a 5-share company
        // may convert to a 10-share company
        if (company.hasDestination() && company.hasReachedDestination()
                && company.getShareUnit() == 20) {
            possibleActions.add(new GrowCompany(getRoot(), 10));
        }

        Player player = getRoot().getPlayerManager().getCurrentPlayer();
        if (player != null) {
            List<SpecialRight> srs = player.getPortfolioModel()
                    .getSpecialProperties(SpecialRight.class, false);
            if (srs != null && !srs.isEmpty()) {
                possibleActions.add(new UseSpecialProperty(srs.get(0)));
            }
        }

        /* TODO all the below
        // Take a loan
        if (Phase.getCurrent(this).isLoanTakingAllowed()
                && operatingCompany.value().canLoan()
                && (loansThisRound == null
                || !loansThisRound.containsKey(operatingCompany.value())
                || loansThisRound.get(operatingCompany.value()) == 0)
                && operatingCompany.value().getCurrentNumberOfLoans()
                < operatingCompany.value().sharesOwnedByPlayers()) {
            possibleActions.add(new TakeLoans(operatingCompany.value(),
                    1, operatingCompany.value().getValuePerLoan()));
        }

        if (getStep() == GameDef.OrStep.REPAY_LOANS) {

            // Has company any outstanding loans to repay?
            if (operatingCompany.value().getMaxNumberOfLoans() != 0
                    && operatingCompany.value().getCurrentNumberOfLoans() > 0) {

                // Minimum number to repay
                int minNumber = Math.max(0,
                        operatingCompany.value().getCurrentNumberOfLoans()
                                - operatingCompany.value().sharesOwnedByPlayers());
                // Maximum number to repay (dependent on cash)
                int maxNumber = Math.min(operatingCompany.value().getCurrentNumberOfLoans(),
                        operatingCompany.value().getCash() / operatingCompany.value().getValuePerLoan());

                if (maxNumber < minNumber) {
                    // Company doesn't have the cash, president must contribute.
                    maxNumber = minNumber;
                }

                if (minNumber > 0) {
                    // Mandatory repayment
                    DisplayBuffer.add(this, LocalText.getText("MustRepayLoans",
                            operatingCompany.value().getId(),
                            minNumber,
                            Bank.format(this, operatingCompany.value().getValuePerLoan()),
                            Bank.format(this, minNumber * operatingCompany.value().getValuePerLoan())));
                }
                possibleActions.add(new RepayLoans(operatingCompany.value(),
                        minNumber, maxNumber, operatingCompany.value().getValuePerLoan()));

                // Step may only be skipped if repayment is optional
                if (minNumber == 0) doneAllowed = true;

            } else {
                // No (more) loans
                doneAllowed = true;
            }
        }*/
    }


}
