package rails.game.specific._1856;

import java.util.ArrayList;
import java.util.List;

import rails.common.GuiDef;
import rails.game.*;
import rails.game.action.*;
import rails.game.move.CashMove;
import rails.game.special.SellBonusToken;
import rails.game.special.SpecialPropertyI;
import rails.game.state.BooleanState;
import rails.util.LocalText;

public class OperatingRound_1856 extends OperatingRound {

    private BooleanState finalLoanRepaymentPending
        = new BooleanState ("LoanRepaymentPending", false);

    private Player playerToStartLoanRepayment = null;

//    static
    {

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

    public OperatingRound_1856 (GameManagerI gameManager) {
        super (gameManager);

    }

    /**
     * Implements special rules for first time operating in 1856
     */
    @Override
    protected boolean setNextOperatingCompany(boolean initial) {

        //log.debug("+++ old OC is "+(operatingCompany.getObject()!=null?operatingCompany.getObject().getName():"null"));
        while (true) {
            if (initial || operatingCompany.get() == null || operatingCompany == null) {
                setOperatingCompany(operatingCompanies.get(0));
                initial = false;
            } else {
                int index = operatingCompanies.indexOf(operatingCompany.get());
                if (++index >= operatingCompanies.size()) {
                    return false;
                }
                setOperatingCompany(operatingCompanies.get(index));
            }

            // 1856 special: check if the company has sold enough shares to operate
            // This check does not apply to the CGR
            if (operatingCompany.get() instanceof PublicCompany_CGR) return true;

            if (operatingCompany.get().isClosed()) continue;

            if (!operatingCompany.get().hasOperated()) {
                int soldPercentage = getSoldPercentage (operatingCompany.get());

                TrainI nextAvailableTrain = gameManager.getTrainManager().getAvailableNewTrains().get(0);
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
                            operatingCompany.get().getName(),
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
        if (operatingCompany.get().canRunTrains()) {

            if (operatingCompany.get() instanceof PublicCompany_CGR
                        && !((PublicCompany_CGR)operatingCompany.get()).hadPermanentTrain()) {
                    DisplayBuffer.add(LocalText.getText("MustWithholdUntilPermanent",
                            PublicCompany_CGR.NAME));
                    possibleActions.add(new SetDividend(
                            operatingCompany.get().getLastRevenue(), true,
                            new int[] {SetDividend.WITHHOLD }));
            } else {
                
                int[] allowedRevenueActions =
                        operatingCompany.get().isSplitAlways()
                                ? new int[] { SetDividend.SPLIT }
                                : operatingCompany.get().isSplitAllowed()
                                        ? new int[] { SetDividend.PAYOUT,
                                                SetDividend.SPLIT,
                                                SetDividend.WITHHOLD }
                                        : new int[] { SetDividend.PAYOUT,
                                                SetDividend.WITHHOLD };

                // Check if any loan interest can be paid
                if (operatingCompany.get().canLoan()) {
                    int loanValue = operatingCompany.get().getLoanValueModel().intValue();
                    if (loanValue > 0) {
                        int interest = loanValue * operatingCompany.get().getLoanInterestPct() / 100;
                        int compCash = (operatingCompany.get().getCash() / 10) * 10;
                        requiredCash = Math.max(interest - compCash, 0);
                    }
                }

                possibleActions.add(new SetDividend(
                        operatingCompany.get().getLastRevenue(), true,
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
        if (!operatingCompany.get().canLoan()) return amount;
        int due = calculateLoanInterest(operatingCompany.get().getCurrentNumberOfLoans());
        if (due == 0) return amount;
        int remainder = due;

        ReportBuffer.add((LocalText.getText("CompanyMustPayLoanInterest",
                operatingCompany.get().getName(),
                Bank.format(due))));

        // Can it be paid from company treasury?
        int payment = Math.min(due, (operatingCompany.get().getCash() / 10) * 10);
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
        Player president = operatingCompany.get().getPresident();
        int presCash = president.getCash();
        if (remainder > presCash) {
            // Start a share selling round
            int cashToBeRaisedByPresident = remainder - presCash;
            log.info("A share selling round must be started as the president cannot pay $"
                    + remainder + " loan interest");
            log.info("President has $"+presCash+", so $"+cashToBeRaisedByPresident+" must be added");
            savedAction = action;
            gameManager.startShareSellingRound(operatingCompany.get().getPresident(),
                    cashToBeRaisedByPresident, operatingCompany.get(), false);
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
        if (!operatingCompany.get().canLoan()) return amount;
        int due = calculateLoanInterest(operatingCompany.get().getCurrentNumberOfLoans());
        if (due == 0) return amount;
        int remainder = due;

        // Pay from company treasury
        int payment = Math.min(due, (operatingCompany.get().getCash() / 10) * 10);
        if (payment > 0) {
            new CashMove (operatingCompany.get(), bank, payment);
            if (payment == due) {
                ReportBuffer.add (LocalText.getText("InterestPaidFromTreasury",
                        operatingCompany.get().getName(),
                        Bank.format(payment)));
            } else {
                ReportBuffer.add (LocalText.getText("InterestPartlyPaidFromTreasury",
                        operatingCompany.get().getName(),
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
                    operatingCompany.get().getName(),
                    Bank.format(payment),
                    Bank.format(due)));
            // This reduces train income
            amount -= payment;
        }
        if (remainder == 0) return amount;

        // Pay any remainder from president cash
        // First check if president has enough cash
        Player president = operatingCompany.get().getPresident();
        int presCash = president.getCash();
        if (remainder > presCash) {
            // This can't happen in this stage, log an error
            log.error("??? The president still cannot pay $"
                    + remainder + " loan interest???");
            return 0;

        } else {

            payment = remainder;
            new CashMove (president, bank, payment);
            ReportBuffer.add (LocalText.getText("InterestPaidFromPresidentCash",
                    operatingCompany.get().getName(),
                    Bank.format(payment),
                    Bank.format(due),
                    president.getName()));
        }

        return amount;
    }

     @Override
    protected void setDestinationActions() {

        List<PublicCompanyI> possibleDestinations = new ArrayList<PublicCompanyI>();
        for (PublicCompanyI comp : operatingCompanies.viewList()) {
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
            new CashMove (bank, company, cashInEscrow);
            ReportBuffer.add(LocalText.getText("ReleasedFromEscrow",
                    company.getName(),
                    Bank.format(cashInEscrow)
            ));
        }

    }

    @Override
    protected void setGameSpecificPossibleActions() {
        // Take a loan
        if (getCurrentPhase().isLoanTakingAllowed()
            && operatingCompany.get().canLoan()
            && (loansThisRound == null
                || !loansThisRound.containsKey(operatingCompany.get())
                || loansThisRound.get(operatingCompany.get()) == 0)
            && operatingCompany.get().getCurrentNumberOfLoans()
                < operatingCompany.get().sharesOwnedByPlayers()) {
            possibleActions.add(new TakeLoans(operatingCompany.get(),
                    1, operatingCompany.get().getValuePerLoan()));
        }

        if (getStep() == GameDef.OrStep.REPAY_LOANS) {

            // Has company any outstanding loans to repay?
            if (operatingCompany.get().getMaxNumberOfLoans() != 0
                && operatingCompany.get().getCurrentNumberOfLoans() > 0) {

                // Minimum number to repay
                int minNumber = Math.max(0,
                        operatingCompany.get().getCurrentNumberOfLoans()
                            - operatingCompany.get().sharesOwnedByPlayers());
                // Maximum number to repay (dependent on cash)
                int maxNumber = Math.min(operatingCompany.get().getCurrentNumberOfLoans(),
                        operatingCompany.get().getCash() / operatingCompany.get().getValuePerLoan());

                if (maxNumber < minNumber) {
                    // Company doesn't have the cash, president must contribute.
                    maxNumber = minNumber;
                }

                if (minNumber > 0) {
                    // Mandatory repayment
                    DisplayBuffer.add(LocalText.getText("MustRepayLoans",
                            operatingCompany.get().getName(),
                            minNumber,
                            Bank.format(operatingCompany.get().getValuePerLoan()),
                            Bank.format(minNumber * operatingCompany.get().getValuePerLoan())));
                }
                possibleActions.add(new RepayLoans(operatingCompany.get(),
                        minNumber, maxNumber, operatingCompany.get().getValuePerLoan()));

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

        PhaseI prePhase = getCurrentPhase();

        boolean result = super.buyTrain(action);

        PhaseI postPhase = getCurrentPhase();

        if (postPhase != prePhase) {
            if (postPhase.getName().equals("5")) {
                finalLoanRepaymentPending.set(true);
                playerToStartLoanRepayment
                    = gameManager.getPlayerByIndex(action.getPlayerIndex());
            } else if (postPhase.getName().equals("4")) {
                // Make Bridge and Tunnel tokens buyable from the Bank.
                for (SpecialPropertyI sp : gameManager.getCommonSpecialProperties()) {
                    if (sp instanceof SellBonusToken) {
                        SellBonusToken sbt = (SellBonusToken)sp;
                        sbt.setSeller(bank);
                        log.debug("SP "+sp.getName()+" is now buyable from the Bank");
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
                        > gameManager.getPhaseManager().getPhaseByName("4").getIndex()) {
                    errMsg = LocalText.getText("WrongPhase",
                            gameManager.getCurrentPhase().getName());
                    break;
                }
                // Exceeds number of shares in player hands?
                int newLoans = operatingCompany.get().getCurrentNumberOfLoans()
                        + action.getNumberTaken();
                int maxLoans = operatingCompany.get().sharesOwnedByPlayers();
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
        if (((GameDef.OrStep) stepObject.get()).compareTo(GameDef.OrStep.PAYOUT) > 0) {
            amount -= calculateLoanInterest(numberOfLoans);
        }

        return amount;
    }

    protected int calculateLoanInterest (int numberOfLoans) {

        return numberOfLoans
            * operatingCompany.get().getValuePerLoan()
            * operatingCompany.get().getLoanInterestPct() / 100;
    }

    @Override
    protected boolean gameSpecificNextStep (GameDef.OrStep step) {

        if (step == GameDef.OrStep.REPAY_LOANS) {

            // Has company any outstanding loans to repay?
            if (operatingCompany.get().getMaxNumberOfLoans() == 0
                || operatingCompany.get().getCurrentNumberOfLoans() == 0) {
                return false;
            // Is company required to repay loans?
            } else if (operatingCompany.get().sharesOwnedByPlayers()
                    < operatingCompany.get().getCurrentNumberOfLoans()) {
                return true;
            // Has company enough money to repay at least one loan?
            } else if (operatingCompany.get().getCash()
                    < operatingCompany.get().getValuePerLoan()) {
                return false;
            } else {
                // Loan repayment is possible but optional
                return true;
            }
        }

        return true;
    }

    public void resume (List<PublicCompanyI> mergingCompanies) {

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

    private boolean resetOperatingCompanies(List<PublicCompanyI> mergingCompanies) {

        PublicCompanyI cgr = companyManager.getPublicCompany(PublicCompany_CGR.NAME);
        boolean cgrCanOperate = cgr.hasStarted();
        boolean roundFinished = false;

        for (PublicCompanyI company : mergingCompanies) {
               if (companiesOperatedThisRound.contains(company)) cgrCanOperate = false;
        }

        // Find the first company that has not yet operated
        // and is not closed.
        //while (setNextOperatingCompany(false)
        //        && operatingCompany.getObject().isClosed());

        // Remove closed companies from the operating company list
        // (PLEASE leave this code in case we need it; it works)
        //for (Iterator<PublicCompanyI> it = companies.iterator();
        //        it.hasNext(); ) {
        //    if ((it.next()).isClosed()) {
        //        it.remove();
        //    }
        //}

        //if (operatingCompany.getObject() != null) {
        //    operatingCompanyIndex = companies.indexOf(operatingCompany.getObject());
        //}

        for (PublicCompanyI c : operatingCompanies.viewList()) {
            if (c.isClosed()) {
                log.info(c.getName()+" is closed");
            } else {
                log.debug(c.getName()+" is operating");
            }
        }

        String message;
        int operatingCompanyIndex = getOperatingCompanyIndex();
        if (cgr.hasStarted()) {
            if (cgrCanOperate) {
                operatingCompanyIndex = Math.max (0, operatingCompanyIndex);
                operatingCompanies.add(operatingCompanyIndex+1, cgr);
                setOperatingCompany(cgr);
                message = LocalText.getText("CanOperate", cgr.getName());
            } else {
                message = LocalText.getText("CannotOperate", cgr.getName());
                roundFinished = !setNextOperatingCompany(false);
           }
        } else {
            message = LocalText.getText("DoesNotForm", cgr.getName());
            roundFinished = !setNextOperatingCompany(false);
        }
        ReportBuffer.add(LocalText.getText("EndOfFormationRound",
                cgr.getName(),
                getRoundName()));
        ReportBuffer.add (message);
        DisplayBuffer.add(message);

        // Find the first company that has not yet operated
        // and is not closed.
        if (!roundFinished) {
            log.debug ("Next operating company: "+operatingCompany.get().getName());
        } else {
            finishOR();
            return false;
        }
        return true;
    }

    @Override
    protected boolean finishTurnSpecials() {

        if (finalLoanRepaymentPending.booleanValue()) {

            ((GameManager_1856)gameManager).startCGRFormationRound(this, playerToStartLoanRepayment);
            return false;
        }

        return true;
    }
}
