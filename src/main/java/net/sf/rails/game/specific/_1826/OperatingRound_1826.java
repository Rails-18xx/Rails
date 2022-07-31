package net.sf.rails.game.specific._1826;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.StockMarket;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.special.SpecialRight;
import net.sf.rails.game.specific._1837.NationalFormationRound;
import net.sf.rails.game.specific._1837.PublicCompany_1837;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;

import java.util.*;

public class OperatingRound_1826 extends OperatingRound {

    private static final Logger log = LoggerFactory.getLogger(OperatingRound_1826.class);

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

        // Check if the current player can use the extra train right
        Player player = playerManager.getCurrentPlayer();
        if (player != null) {
            List<SpecialRight> srs = player.getPortfolioModel()
                    .getSpecialProperties(SpecialRight.class, false);
            if (srs != null && !srs.isEmpty()) {
                possibleActions.add(new UseSpecialProperty(srs.get(0)));
            }
        }

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
        }
    }

    protected void newPhaseChecks() {
        Phase phase = Phase.getCurrent(this);

        // How many companies are trainless?
        List<PublicCompany_1826> trainlessCompanies = new ArrayList<>();
        for (PublicCompany company : operatingCompanies) {
            if (!company.getId().equals(PublicCompany_1826.BELG)
                    && company.hasOperated() && company.getNumberOfTrains() == 0) {
                trainlessCompanies.add((PublicCompany_1826)company);
            }
        }
        if (trainlessCompanies.size() < 2) return;

        if (phase.getId().equals("6H")) {
            // Form Etat
            PublicCompany_1826 etat
                    = (PublicCompany_1826)companyManager.getPublicCompany(PublicCompany_1826.ETAT);
            if (!etat.hasStarted()) {
                formNational (etat, trainlessCompanies);
            }
        } else if (phase.getId().equals("10H") || phase.getId().equals("E")) {
            // Form SNCF (if not started before)
            PublicCompany_1826 sncf
                    = (PublicCompany_1826)companyManager.getPublicCompany(PublicCompany_1826.SNCF);
            if (!sncf.hasStarted()) {
                formNational (sncf, trainlessCompanies);
            }

        }
    }

    private void formNational (PublicCompany_1826 national,
                               List<PublicCompany_1826> trainlessCompanies) {

        NavigableSet<Integer> prices = new TreeSet<>();

        // Make all trainless companies 10-share ones
        for (PublicCompany_1826 company : trainlessCompanies) {
            if (company.getShareUnit() == 20) {  // 5-share
                company.grow(10);
            }
            prices.add (company.getCurrentSpace().getPrice());
            log.debug ("Price of {} is {}", company, company.getCurrentSpace().getPrice());
        }
        log.debug ("Sorted prices: {}", prices);

        // Determine the national's start price
        int nationalPrice = (prices.pollLast() + prices.pollLast()) / 2;
        log.debug ("National price is {}", nationalPrice);
        StockMarket stockMarket = getRoot().getStockMarket();
        int row = 0;
        int col = 0;
        while (stockMarket.getStockSpace(row, col).getPrice() < nationalPrice) { col++; };
        StockSpace nationalStockSpace = stockMarket.getStockSpace(row, --col);

        national.start();
        log.debug ("National price is {} at {}", nationalStockSpace.getPrice(),
                nationalStockSpace.getId());
        String msg = LocalText.getText("START_MERGED_COMPANY", national.getId(),
                nationalStockSpace.getPrice(),
                nationalStockSpace.getId());
        DisplayBuffer.add (this, msg);
        ReportBuffer.add (this, msg);


        // To exchange shares, start with player who started this phase,i.e. the current player
        for (Player player : playerManager.getNextPlayersAfter(
                playerManager.getCurrentPlayer(), true, false)) {
            for (PublicCompany_1826 company : trainlessCompanies) {
                int shares = player.getPortfolioModel().getShares(company);
                if (shares >= 4 && national.getPresident() == null) {

                }

            }
        }

    }


}
