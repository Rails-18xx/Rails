package net.sf.rails.game.specific._1856;

import java.util.ArrayList;
import java.util.List;

import rails.game.action.*;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.special.SellBonusToken;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;

import com.google.common.collect.Iterables;


public class OperatingRound_1856 extends OperatingRound {

    private final BooleanState finalLoanRepaymentPending = BooleanState.create(this, "LoanRepaymentPending");
    private Player playerToStartLoanRepayment = null;

    /**
     * Constructed via Configure
     */
    public OperatingRound_1856 (GameManager parent, String id) {
        super(parent, id);

        steps = new GameDef.OrStep[] {
                GameDef.OrStep.INITIAL,
                GameDef.OrStep.LAY_TRACK,
                GameDef.OrStep.LAY_TOKEN,
                GameDef.OrStep.CALC_REVENUE,
                GameDef.OrStep.PAYOUT,
                GameDef.OrStep.BUY_TRAIN,
                GameDef.OrStep.TRADE_SHARES,
                GameDef.OrStep.REPAY_LOANS,
                GameDef.OrStep.FINAL
        };
    }

    /**
     * Implements special rules for first time operating in 1856
     */
    @Override
    protected boolean setNextOperatingCompany(boolean initial) {

        //log.debug("+++ old OC is "+(operatingCompany.getObject()!=null?operatingCompany.getObject().getName():"null"));
        while (true) {
            if (initial || operatingCompany.value() == null || operatingCompany == null) {
                setOperatingCompany(operatingCompanies.get(0));
                initial = false;
            } else {
                int index = operatingCompanies.indexOf(operatingCompany.value());
                if (++index >= operatingCompanies.size()) {
                    return false;
                }
                setOperatingCompany(operatingCompanies.get(index));
            }

            // 1856 special: check if the company has sold enough shares to operate
            // This check does not apply to the CGR
            if (operatingCompany.value() instanceof PublicCompany_CGR) return true;

            if (operatingCompany.value().isClosed()) continue;

            if (!operatingCompany.value().hasOperated()) {
                int soldPercentage = operatingCompany.value().getSoldPercentage();
                // TODO: Refactor the code duplication
                Train nextAvailableTrain = Iterables.get(trainManager.getAvailableNewTrains(), 0);
                log.debug("Next Train type" + nextAvailableTrain.toText());
                int trainNumber;
                try {
                    trainNumber = Integer.parseInt(nextAvailableTrain.toText());
                } catch (NumberFormatException e) {
                    trainNumber = 6; // Diesel!
                }
                int floatPercentage = 10 * trainNumber;

                log.debug ("Float percentage is "+floatPercentage
                        +" sold percentage is "+soldPercentage);


                if (soldPercentage < floatPercentage) {
                    DisplayBuffer.add(this, LocalText.getText("MayNotYetOperate",
                            operatingCompany.value().getId(),
                            String.valueOf(soldPercentage),
                            String.valueOf(floatPercentage)
                    ));
                    // Company may not yet operate
                    continue;
                }
            }
            //log.debug("+++ new OC is "+(operatingCompany.getObject()!=null?operatingCompany.getObject().getName():"null"));
            return true;
        }
    }

    @Override
    protected void prepareRevenueAndDividendAction () {

        int requiredCash = 0;

        // There is only revenue if there are any trains
        if (operatingCompany.value().canRunTrains()) {

            if (operatingCompany.value() instanceof PublicCompany_CGR
                        && !((PublicCompany_CGR)operatingCompany.value()).hadPermanentTrain()) {
                    DisplayBuffer.add(this, LocalText.getText("MustWithholdUntilPermanent",
                            PublicCompany_CGR.NAME));
                    possibleActions.add(new SetDividend(
                            operatingCompany.value().getLastRevenue(), true,
                            new int[] {SetDividend.WITHHOLD }));
            } else {
                
                int[] allowedRevenueActions =
                        operatingCompany.value().isSplitAlways()
                                ? new int[] { SetDividend.SPLIT }
                                : operatingCompany.value().isSplitAllowed()
                                        ? new int[] { SetDividend.PAYOUT,
                                                SetDividend.SPLIT,
                                                SetDividend.WITHHOLD }
                                        : new int[] { SetDividend.PAYOUT,
                                                SetDividend.WITHHOLD };

                // Check if any loan interest can be paid
                if (operatingCompany.value().canLoan()) {
                    int loanValue = operatingCompany.value().getLoanValueModel().value();
                    if (loanValue > 0) {
                        int interest = loanValue * operatingCompany.value().getLoanInterestPct() / 100;
                        // TODO: Hard coded magic number
                        int compCash = (operatingCompany.value().getCash() / 10) * 10;
                        requiredCash = Math.max(interest - compCash, 0);
                    }
                }

                possibleActions.add(new SetDividend(
                        operatingCompany.value().getLastRevenue(), true,
                        allowedRevenueActions,
                        requiredCash));
            }

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
        if (!operatingCompany.value().canLoan()) return amount;
        int due = calculateLoanInterest(operatingCompany.value().getCurrentNumberOfLoans());
        if (due == 0) return amount;
        int remainder = due;

        ReportBuffer.add(this, (LocalText.getText("CompanyMustPayLoanInterest",
                operatingCompany.value().getId(),
                Bank.format(this, due))));

        // Can it be paid from company treasury?
        // TODO: Hard code 10% payment
        int payment = Math.min(due, (operatingCompany.value().getCash() / 10) * 10);
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
        Player president = operatingCompany.value().getPresident();
        int presCash = president.getCash();
        if (remainder > presCash) {
            // Start a share selling round
            int cashToBeRaisedByPresident = remainder - presCash;
            log.info("A share selling round must be started as the president cannot pay $"
                    + remainder + " loan interest");
            log.info("President has $"+presCash+", so $"+cashToBeRaisedByPresident+" must be added");
            savedAction = action;
            gameManager.startShareSellingRound(operatingCompany.value().getPresident(),
                    cashToBeRaisedByPresident, operatingCompany.value(), false);
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
        if (!operatingCompany.value().canLoan()) return amount;
        int due = calculateLoanInterest(operatingCompany.value().getCurrentNumberOfLoans());
        if (due == 0) return amount;
        int remainder = due;

        // Pay from company treasury
        // TODO: Hard-coded 10% payment
        int payment = Math.min(due, (operatingCompany.value().getCash() / 10) * 10);
        if (payment > 0) {
            String paymentText = Currency.toBank(operatingCompany.value(), payment);
            if (payment == due) {
                ReportBuffer.add(this, LocalText.getText("InterestPaidFromTreasury",
                        operatingCompany.value().getId(),
                        paymentText));
            } else {
                ReportBuffer.add(this, LocalText.getText("InterestPartlyPaidFromTreasury",
                        operatingCompany.value().getId(),
                        paymentText,
                        bank.getCurrency().format(due))); // TODO: Do this nicer
            }
            remainder -= payment;
        }
        if (remainder == 0) return amount;

        // Pay any remainder from revenue
        payment = Math.min (remainder, amount);
        if (payment > 0) {
            // Payment money remains in the bank
            remainder -= payment;
            ReportBuffer.add(this, LocalText.getText("InterestPaidFromRevenue",
                    operatingCompany.value().getId(),
                    Bank.format(this, payment),
                    Bank.format(this, due)));
            // This reduces train income
            amount -= payment;
        }
        if (remainder == 0) return amount;

        // Pay any remainder from president cash
        // First check if president has enough cash
        Player president = operatingCompany.value().getPresident();
        int presCash = president.getCash();
        if (remainder > presCash) {
            // This can't happen in this stage, log an error
            log.error("??? The president still cannot pay $"
                    + remainder + " loan interest???");
            return 0;

        } else {

            payment = remainder;
            String paymentText = Currency.toBank(president, payment);
            ReportBuffer.add(this, LocalText.getText("InterestPaidFromPresidentCash",
                    operatingCompany.value().getId(),
                    paymentText,
                    bank.getCurrency().format(due), // TODO: Do this nicer
                    president.getId()));
        }

        return amount;
    }

     @Override
    protected void setDestinationActions() {

        List<PublicCompany> possibleDestinations = new ArrayList<PublicCompany>();
        for (PublicCompany comp : operatingCompanies.view()) {
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
    protected void reachDestination (PublicCompany company) {

        PublicCompany_1856 comp = (PublicCompany_1856) company;
        int cashInEscrow = comp.getMoneyInEscrow();
        if (cashInEscrow > 0) {
            String cashText = Currency.fromBank(cashInEscrow, company);
            ReportBuffer.add(this, LocalText.getText("ReleasedFromEscrow",
                    company.getId(),
                    cashText));
        }

    }

    @Override
    protected void setGameSpecificPossibleActions() {
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

    @Override
    public boolean buyTrain(BuyTrain action) {

        Phase prePhase = Phase.getCurrent(this);

        boolean result = super.buyTrain(action);

        Phase postPhase = Phase.getCurrent(this);

        if (postPhase != prePhase) {
            if (postPhase.getId().equals("6")) {
                finalLoanRepaymentPending.set(true);
                playerToStartLoanRepayment
                    = playerManager.getPlayerByName(action.getPlayerName());
            } else if (postPhase.getId().equals("5")) {
                // Make Bridge and Tunnel tokens buyable from the Bank.
                for (SpecialProperty sp : gameManager.getCommonSpecialProperties()) {
                    if (sp instanceof SellBonusToken) {
                        SellBonusToken sbt = (SellBonusToken)sp;
                        // FIXME: Is it ipo or pool portfolio?
                        // Assume it is pool
                        sbt.setSeller(bank.getPool());
                        log.debug("SP "+sp.getId()+" is now buyable from the Bank");
                    }
                }
            }
        }

        return result;
    }


    @Override
    protected String validateTakeLoans (TakeLoans action) {

        String errMsg = super.validateTakeLoans(action);

        if (errMsg == null) {

            while (true) {
                // Still allowed in current phase?
                if (gameManager.getCurrentPhase().getIndex()
                        > getRoot().getPhaseManager().getPhaseByName("5").getIndex()) {
                    errMsg = LocalText.getText("WrongPhase",
                            gameManager.getCurrentPhase().toText());
                    break;
                }
                // Exceeds number of shares in player hands?
                int newLoans = operatingCompany.value().getCurrentNumberOfLoans()
                        + action.getNumberTaken();
                int maxLoans = operatingCompany.value().sharesOwnedByPlayers();
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
        if (((GameDef.OrStep) stepObject.value()).compareTo(GameDef.OrStep.PAYOUT) > 0) {
            amount -= calculateLoanInterest(numberOfLoans);
        }

        return amount;
    }

    protected int calculateLoanInterest (int numberOfLoans) {

        return numberOfLoans
            * operatingCompany.value().getValuePerLoan()
            * operatingCompany.value().getLoanInterestPct() / 100;
    }

    @Override
    protected boolean gameSpecificNextStep (GameDef.OrStep step) {

        if (step == GameDef.OrStep.REPAY_LOANS) {

            // Has company any outstanding loans to repay?
            if (operatingCompany.value().getMaxNumberOfLoans() == 0
                || operatingCompany.value().getCurrentNumberOfLoans() == 0) {
                return false;
            // Is company required to repay loans?
            } else if (operatingCompany.value().sharesOwnedByPlayers()
                    < operatingCompany.value().getCurrentNumberOfLoans()) {
                return true;
            // Has company enough money to repay at least one loan?
            } else if (operatingCompany.value().getCash()
                    < operatingCompany.value().getValuePerLoan()) {
                return false;
            } else {
                // Loan repayment is possible but optional
                return true;
            }
        }

        return true;
    }

    public void resume (List<PublicCompany> mergingCompanies) {

        // End of CGRFormationRound
        finalLoanRepaymentPending.set(false);
        guiHints.setActivePanel(GuiDef.Panel.MAP);
        guiHints.setCurrentRoundType(getClass());

        if (!resetOperatingCompanies(mergingCompanies)) return;
        if (getOperatingCompany() != null) {
            setStep(GameDef.OrStep.INITIAL);
        } else {
            finishOR();
        }
        wasInterrupted.set(true);
    }

    private boolean resetOperatingCompanies(List<PublicCompany> mergingCompanies) {

        PublicCompany cgr = companyManager.getPublicCompany(PublicCompany_CGR.NAME);
        boolean cgrCanOperate = cgr.hasStarted();
        boolean roundFinished = false;

        for (PublicCompany company : mergingCompanies) {
               if (companiesOperatedThisRound.contains(company)) cgrCanOperate = false;
        }

        // Find the first company that has not yet operated
        // and is not closed.
        //while (setNextOperatingCompany(false)
        //        && operatingCompany.getObject().isClosed());

        // Remove closed companies from the operating company list
        // (PLEASE leave this code in case we need it; it works)
        //for (Iterator<PublicCompany> it = companies.iterator();
        //        it.hasNext(); ) {
        //    if ((it.next()).isClosed()) {
        //        it.remove();
        //    }
        //}

        //if (operatingCompany.getObject() != null) {
        //    operatingCompanyndex = companies.indexOf(operatingCompany.getObject());
        //}

        for (PublicCompany c : operatingCompanies.view()) {
            if (c.isClosed()) {
                log.info(c.getId()+" is closed");
            } else {
                log.debug(c.getId()+" is operating");
            }
        }

        String message;
        int operatingCompanyndex = getOperatingCompanyndex();
        if (cgr.hasStarted()) {
            if (cgrCanOperate) {
                operatingCompanyndex = Math.max (0, operatingCompanyndex);
                operatingCompanies.add(operatingCompanyndex+1, cgr);
                setOperatingCompany(cgr);
                message = LocalText.getText("CanOperate", cgr.getId());
            } else {
                message = LocalText.getText("CannotOperate", cgr.getId());
                roundFinished = !setNextOperatingCompany(false);
           }
        } else {
            message = LocalText.getText("DoesNotForm", cgr.getId());
            roundFinished = !setNextOperatingCompany(false);
        }
        ReportBuffer.add(this, LocalText.getText("EndOfFormationRound",
                cgr.getId(),
                getRoundName()));
        ReportBuffer.add(this, message);
        DisplayBuffer.add(this, message);

        // Find the first company that has not yet operated
        // and is not closed.
        if (!roundFinished) {
            log.debug ("Next operating company: "+operatingCompany.value().getId());
        } else {
            finishOR();
            return false;
        }
        return true;
    }

    @Override
    protected boolean finishTurnSpecials() {

        if (finalLoanRepaymentPending.value()) {

            ((GameManager_1856)gameManager).startCGRFormationRound(this, playerToStartLoanRepayment);
            return false;
        }

        return true;
    }
}
