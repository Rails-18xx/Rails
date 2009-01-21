package rails.game.specific._1856;

import java.util.ArrayList;
import java.util.List;

import rails.game.*;
import rails.game.action.*;
import rails.game.move.CashMove;
import rails.game.state.IntegerState;
import rails.util.LocalText;

public class OperatingRound_1856 extends OperatingRound {

    public static final int STEP_REPAY_LOANS = 6;

    static {
        STEP_FINAL = 7;

        steps =
                new int[] { STEP_LAY_TRACK, STEP_LAY_TOKEN, STEP_CALC_REVENUE,
                        STEP_PAYOUT, STEP_BUY_TRAIN, STEP_TRADE_SHARES,
                        STEP_REPAY_LOANS, STEP_FINAL };

        stepNames =
                new String[] { "LayTrack", "LayToken", "EnterRevenue", "Payout",
                        "BuyTrain", "TradeShares", "RepayLoans", "Final" };
    }

    public OperatingRound_1856 (GameManagerI gameManager) {
        super (gameManager);

    }

    /**
     * Implements special rules for first time operating in 1856
     */
    @Override
    protected boolean setNextOperatingCompany(boolean initial) {


        if (operatingCompanyIndexObject == null) {
            operatingCompanyIndexObject =
                    new IntegerState("OperatingCompanyIndex");
        }

        while (true) {
            if (initial) {
                operatingCompanyIndexObject.set(0);
                initial = false;
            } else {
                operatingCompanyIndexObject.add(1);
            }

            operatingCompanyIndex = operatingCompanyIndexObject.intValue();

            if (operatingCompanyIndex >= operatingCompanyArray.length) {
                return false;
            } else {
                operatingCompany = operatingCompanyArray[operatingCompanyIndex];

                // 1856 special: check if the company may operate
                if (!operatingCompany.hasOperated()) {
                    int soldPercentage
                        = 100 - operatingCompany.getUnsoldPercentage();

                    TrainI nextAvailableTrain = TrainManager.get().getAvailableNewTrains().get(0);
                    int trainNumber;
                    try {
                        trainNumber = Integer.parseInt(nextAvailableTrain.getName());
                    } catch (NumberFormatException e) {
                        trainNumber = 6; // Diesel!
                    }
                    int floatPercentage = 10 * trainNumber;

                    log.debug ("Float percentage is "+floatPercentage
                            +" sold percentage is "+soldPercentage);


                    if (soldPercentage < floatPercentage) {
                        DisplayBuffer.add(LocalText.getText("MayNotYetOperate",
                                operatingCompany.getName(),
                                String.valueOf(soldPercentage),
                                String.valueOf(floatPercentage)
                        ));
                        // Company may not yet operate
                        continue;
                    }

                }
                return true;
            }
        }
    }

    @Override
    protected void prepareRevenueAndDividendAction () {

        int requiredCash = 0;

        // Check if any loan interest can be paid
        int loanValue = operatingCompany.getLoanValueModel().intValue();
        if (loanValue > 0) {
            int interest = loanValue * operatingCompany.getLoanInterestPct() / 100;
            int compCash = (operatingCompany.getCash() / 10) * 10;
            requiredCash = Math.max(interest - compCash, 0);
       }

        // There is only revenue if there are any trains
        if (operatingCompany.getPortfolio().getNumberOfTrains() > 0) {
            int[] allowedRevenueActions =
                    operatingCompany.isSplitAlways()
                            ? new int[] { SetDividend.SPLIT }
                            : operatingCompany.isSplitAllowed()
                                    ? new int[] { SetDividend.PAYOUT,
                                            SetDividend.SPLIT,
                                            SetDividend.WITHHOLD }
                                    : new int[] { SetDividend.PAYOUT,
                                            SetDividend.WITHHOLD };

            possibleActions.add(new SetDividend(
                    operatingCompany.getLastRevenue(), true,
                    allowedRevenueActions,
                    requiredCash));
            // UI directions:
            // Any nonzero required cash should be reported to the user.
            // If the revenue is less than that, the allocation
            // question should be suppressed.
            // In that case, the follow-up is done from this class.
        }
    }

    @Override
    protected int checkForDeductions (SetDividend action) {

        int amount = action.getActualRevenue();
        int due = calculateLoanInterest(operatingCompany.getCurrentNumberOfLoans());
        if (due == 0) return amount;
        int remainder = due;

        ReportBuffer.add((LocalText.getText("CompanyMustPayLoanInterest",
                operatingCompany.getName(),
                Bank.format(due))));

        // Can it be paid from company treasury?
        int payment = Math.min(due, (operatingCompany.getCash() / 10) * 10);
        if (payment > 0) {
            remainder -= payment;
        }
        if (remainder == 0) return amount;

        // Can any remainder be paid from revenue?
        payment = Math.min (remainder, amount);
        if (payment > 0) {
            remainder -= payment;
            // This reduces train income
            amount -= payment;
        }
        if (remainder == 0) return amount;

        // Pay any remainder from president cash
        // First check if president has enough cash
        Player president = operatingCompany.getPresident();
        int presCash = president.getCash();
        if (remainder > presCash) {
            // Start a share selling round
            cashToBeRaisedByPresident = remainder - presCash;
            log.info("A share selling round must be started as the president cannot pay $"
                    + remainder + " loan interest");
            log.info("President has $"+presCash+", so $"+cashToBeRaisedByPresident+" must be added");
            savedAction = action;
            gameManager.startShareSellingRound(this, operatingCompany,
                    cashToBeRaisedByPresident);
            // Return arbitrary negative value to signal end of processing to caller.
            return -remainder;

        } else {
            // OK, nothing more to here
         }

        return amount;
    }

    @Override
    protected int executeDeductions (SetDividend action) {

        int amount = action.getActualRevenue();
        int due = calculateLoanInterest(operatingCompany.getCurrentNumberOfLoans());
        if (due == 0) return amount;
        int remainder = due;

        // Pay from company treasury
        int payment = Math.min(due, (operatingCompany.getCash() / 10) * 10);
        if (payment > 0) {
            new CashMove (operatingCompany, null, payment);
            if (payment == due) {
                ReportBuffer.add (LocalText.getText("InterestPaidFromTreasury",
                        operatingCompany.getName(),
                        Bank.format(payment)));
            } else {
                ReportBuffer.add (LocalText.getText("InterestPartlyPaidFromTreasury",
                        operatingCompany.getName(),
                        Bank.format(payment),
                        Bank.format(due)));
            }
            remainder -= payment;
        }
        if (remainder == 0) return amount;

        // Pay any remainder from revenue
        payment = Math.min (remainder, amount);
        if (payment > 0) {
            // Payment money remains in the bank
            remainder -= payment;
            ReportBuffer.add (LocalText.getText("InterestPaidFromRevenue",
                    operatingCompany.getName(),
                    Bank.format(payment),
                    Bank.format(due)));
            // This reduces train income
            amount -= payment;
        }
        if (remainder == 0) return amount;

        // Pay any remainder from president cash
        // First check if president has enough cash
        Player president = operatingCompany.getPresident();
        int presCash = president.getCash();
        if (remainder > presCash) {
            // This can't happen in this stage, log an error
            log.error("??? The president still cannot pay $"
                    + remainder + " loan interest???");
            return 0;

        } else {

            payment = remainder;
            new CashMove (president, null, payment);
            ReportBuffer.add (LocalText.getText("InterestPaidFromPresidentCash",
                    operatingCompany.getName(),
                    Bank.format(payment),
                    Bank.format(due),
                    president.getName()));
        }

        return amount;
    }

     @Override
    protected void setDestinationActions() {

        List<PublicCompanyI> possibleDestinations = new ArrayList<PublicCompanyI>();
        for (PublicCompanyI comp : operatingCompanyArray) {
            if (comp.hasDestination()
                    && ((PublicCompany_1856)comp).getTrainNumberAvailableAtStart() < 5
                    && !comp.hasReachedDestination()) {
                possibleDestinations.add (comp);
            }
        }
        if (possibleDestinations.size() > 0) {
            possibleActions.add(new ReachDestinations (possibleDestinations));
        }
    }

    @Override
    protected void reachDestination (PublicCompanyI company) {

        PublicCompany_1856 comp = (PublicCompany_1856) company;
        int cashInEscrow = comp.getMoneyInEscrow();
        if (cashInEscrow > 0) {
            new CashMove (null, company, cashInEscrow);
            ReportBuffer.add(LocalText.getText("ReleasedFromEscrow",
                    company.getName(),
                    Bank.format(cashInEscrow)
            ));
        }

    }

    @Override
    protected void setGameSpecificPossibleActions() {
        // Take a loan
        if ((loansThisRound == null
                || !loansThisRound.containsKey(operatingCompany)
                || loansThisRound.get(operatingCompany) == 0)
            && gameManager.getCurrentPhase().getIndex()
                <= gameManager.getPhaseManager().getPhaseByName("4").getIndex()
            && operatingCompany.getCurrentNumberOfLoans()
                < operatingCompany.sharesOwnedByPlayers()) {
            possibleActions.add(new TakeLoans(operatingCompany,
                    1, operatingCompany.getValuePerLoan()));
        }

        if (getStep() == STEP_REPAY_LOANS) {
            // Has company any outstanding loans to repay?
            if (operatingCompany.getMaxNumberOfLoans() != 0
                && operatingCompany.getCurrentNumberOfLoans() > 0) {

                // Minimum number to repay
                int minNumber = Math.max(0,
                        operatingCompany.getCurrentNumberOfLoans()
                            - operatingCompany.sharesOwnedByPlayers());
                // Maximum number to repay (dependent on cash)
                int maxNumber = Math.min(operatingCompany.getCurrentNumberOfLoans(),
                        operatingCompany.getCash() / operatingCompany.getValuePerLoan());

                if (maxNumber < minNumber) {
                    // Company doesn't have the cash, president must contribute.
                    maxNumber = minNumber;
                }

                if (minNumber > 0) {
                    // Mandatory repayment
                    DisplayBuffer.add(LocalText.getText("MustRepayLoans",
                            operatingCompany.getName(),
                            minNumber,
                            Bank.format(operatingCompany.getValuePerLoan()),
                            Bank.format(minNumber * operatingCompany.getValuePerLoan())));
                }
                possibleActions.add(new RepayLoans(operatingCompany,
                        minNumber, maxNumber, operatingCompany.getValuePerLoan()));

                // Step may only be skipped if repayment is optional
                if (minNumber == 0) doneAllowed = true;
            }
        }
    }

    @Override
    protected String validateTakeLoans (TakeLoans action) {

        String errMsg = super.validateTakeLoans(action);

        if (errMsg == null) {

            while (true) {
                // Still allowed in current phase?
                if (gameManager.getCurrentPhase().getIndex()
                        > gameManager.getPhaseManager().getPhaseByName("4").getIndex()) {
                    errMsg = LocalText.getText("WrongPhase",
                            gameManager.getCurrentPhase().getName());
                    break;
                }
                // Exceeds number of shares in player hands?
                int newLoans = operatingCompany.getCurrentNumberOfLoans()
                        + action.getNumberTaken();
                int maxLoans = operatingCompany.sharesOwnedByPlayers();
                if (newLoans > maxLoans) {
                    errMsg = LocalText.getText("WouldExceedSharesAtPlayers",
                            newLoans, maxLoans);
                    break;
                }
                break;
            }
        }
        return errMsg;
    }

    @Override
    protected int calculateLoanAmount (int numberOfLoans) {

        int amount = super.calculateLoanAmount(numberOfLoans);

        // Deduct interest immediately?
        if (stepObject.intValue() > STEP_PAYOUT) {
            amount -= calculateLoanInterest(numberOfLoans);
        }

        return amount;
    }

    protected int calculateLoanInterest (int numberOfLoans) {

        return numberOfLoans
            * operatingCompany.getValuePerLoan()
            * operatingCompany.getLoanInterestPct() / 100;
    }

    @Override
    protected boolean gameSpecificNextStep (int step) {

        if (step == STEP_REPAY_LOANS) {

            // Has company any outstanding loans to repay?
            if (operatingCompany.getMaxNumberOfLoans() == 0
                || operatingCompany.getCurrentNumberOfLoans() == 0) {
                return false;
            // Is company required to repay loans?
            } else if (operatingCompany.sharesOwnedByPlayers()
                    < operatingCompany.getCurrentNumberOfLoans()) {
                return true;
            // Has company enough money to repay at least one loan?
            } else if (operatingCompany.getCash()
                    < operatingCompany.getValuePerLoan()) {
                return false;
            } else {
                // Loan repayment is possible but optional
                return true;
            }
        } else {
            // We are not in this step
            return true;
        }

    }

}
