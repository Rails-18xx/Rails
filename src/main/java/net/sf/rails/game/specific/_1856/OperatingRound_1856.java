package net.sf.rails.game.specific._1856;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.*;
import net.sf.rails.algorithms.RevenueAdapter;
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
import net.sf.rails.game.state.GenericState;

public class OperatingRound_1856 extends OperatingRound {

    private static final Logger log = LoggerFactory.getLogger(OperatingRound_1856.class);

    protected final BooleanState loanTakenThisTurn = new BooleanState(this, "loanTakenThisTurn", false);

    /**
     * Set after the first 6-train is bought, irrespective whether any loans are
     * outstanding or not.
     */
    private final BooleanState finalLoanRepaymentPending = new BooleanState(this, "LoanRepaymentPending");

    private Player playerToStartLoanRepayment = null;

    private final BooleanState isLayingPort = new BooleanState(this, "isLayingPort", false);

    public static class LayPortToken_1856 extends rails.game.action.PossibleAction {
        private static final long serialVersionUID = 1L;
        private final String companyId;
        private String hexId;

        public LayPortToken_1856(net.sf.rails.game.RailsRoot root, String companyId, String hexId) {
            super(root);
            this.companyId = companyId;
            this.hexId = hexId;
        }

        public String getCompanyId() {
            return companyId;
        }

        public String getHexId() {
            return hexId;
        }

        public void setHexId(String hexId) {
            this.hexId = hexId;
        }

        @Override
        public String toString() {
            return "Lay Port token" + (hexId != null ? " on " + hexId : "");
        }

        @Override
        public String getButtonLabel() {
            return toString();
        }
    }

    @Override
protected void initTurn() {
    super.initTurn();
    loanTakenThisTurn.set(false); // Reset via state
}

@Override
protected void executeTakeLoans(int number) {
    super.executeTakeLoans(number);
    loanTakenThisTurn.set(true); // Persist via state
}


    /**
     * Constructed via Configure
     */
    public OperatingRound_1856(GameManager parent, String id) {
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

        // log.debug("+++ old OC is
        // "+(operatingCompany.getObject()!=null?operatingCompany.getObject().getName():"null"));
        while (true) {
            if (initial || operatingCompany == null || operatingCompany.value() == null) {
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
            if (operatingCompany.value() instanceof PublicCompany_CGR)
                return true;

            if (operatingCompany.value().isClosed())
                continue;

            if (!operatingCompany.value().hasOperated()) {
                int soldPercentage = operatingCompany.value().getSoldPercentage();
                int trainNumber = ((GameManager_1856) gameManager).getNextTrainNumberFromIpo();
                int floatPercentage = 10 * trainNumber;

                log.debug("Float percentage is {} sold percentage is {}", floatPercentage, soldPercentage);

                if (soldPercentage < floatPercentage) {
                    DisplayBuffer.add(this, LocalText.getText("MayNotYetOperate",
                            operatingCompany.value().getId(),
                            String.valueOf(soldPercentage),
                            String.valueOf(floatPercentage)));
                    // Company may not yet operate
                    continue;
                }
            }
            // log.debug("+++ new OC is
            // "+(operatingCompany.getObject()!=null?operatingCompany.getObject().getName():"null"));
            return true;
        }
    }

    @Override
    protected void prepareRevenueAndDividendAction() {

        int requiredCash = 0;

        // There is only revenue if there are any trains
        if (operatingCompany.value().hasTrains()) {

            if (operatingCompany.value() instanceof PublicCompany_CGR
                    && !((PublicCompany_CGR) operatingCompany.value()).hadPermanentTrain()) {
                DisplayBuffer.add(this, LocalText.getText("MustWithholdUntilPermanent",
                        PublicCompany_CGR.NAME));
                possibleActions.add(new SetDividend(getRoot(),
                        operatingCompany.value().getLastRevenue(), true,
                        new int[] { SetDividend.WITHHOLD }));
            } else {

                int[] allowedRevenueActions = operatingCompany.value().isSplitAlways()
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

                possibleActions.add(new SetDividend(getRoot(),
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

    // NOT USED so far, see executeDeductions()
    @Override
    protected int checkForDeductions(SetDividend action) {

        int amount = action.getActualRevenue();
        if (!operatingCompany.value().canLoan())
            return amount;
        int due = calculateLoanInterest(operatingCompany.value().getCurrentNumberOfLoans());
        if (due == 0)
            return amount;
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
        if (remainder == 0)
            return amount;

        // Can any remainder be paid from revenue?
        payment = Math.min(remainder, amount);
        if (payment > 0) {
            remainder -= payment;
            // This reduces train income
            amount -= payment;
        }
        if (remainder == 0)
            return amount;

        // Pay any remainder from president cash
        // First check if president has enough cash
        Player president = operatingCompany.value().getPresident();
        int presCash = president.getCash();
        if (remainder > presCash) {
            // Start a share selling round
            int cashToBeRaisedByPresident = remainder - presCash;
            log.info("A share selling round must be started as the president cannot pay ${} loan interest", remainder);
            log.info("President has ${}, so ${} must be added", presCash, cashToBeRaisedByPresident);
            savedAction.set(action);
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
    protected int executeDeductions(SetDividend action) {

        int amount = action.getActualRevenue();
        if (!operatingCompany.value().canLoan())
            return amount;
        int due = calculateLoanInterest(operatingCompany.value().getCurrentNumberOfLoans());
        if (due == 0)
            return amount;
        int remainder = due;

        // Pay from company treasury
        // TODO: Hard-coded 10% payment
        int payment = Math.min(due, (operatingCompany.value().getCash() / 10) * 10);
        if (payment > 0) {
            String paymentText = Currency.toBank(operatingCompany.value(), payment);
            if (payment == due) {
                ReportBuffer.add(this, LocalText.getText("InterestPaidFromTreasury",
                        operatingCompany.value().getId(),
                        paymentText,
                        LocalText.getText("loan")));
            } else {
                ReportBuffer.add(this, LocalText.getText("InterestPartlyPaidFromTreasury",
                        operatingCompany.value().getId(),
                        paymentText,
                        bank.getCurrency().format(due),
                        LocalText.getText("loan")));
            }
            remainder -= payment;
        }
        if (remainder == 0)
            return amount;

        // Pay any remainder from revenue
        payment = Math.min(remainder, amount);
        if (payment > 0) {
            // Payment money remains in the bank
            remainder -= payment;
            ReportBuffer.add(this, LocalText.getText("InterestPaidFromRevenue",
                    operatingCompany.value().getId(),
                    Bank.format(this, payment),
                    Bank.format(this, due),
                    LocalText.getText("loan")));
            // This reduces train income
            amount -= payment;
        }
        if (remainder == 0)
            return amount;

        // Pay any remainder from president cash
        // First check if president has enough cash
        Player president = operatingCompany.value().getPresident();
        int presCash = president.getCash();
        if (remainder > presCash) {
            // This can't happen in this stage, log an error
            log.error("??? The president still cannot pay ${} loan interest???", remainder);
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
        // Intentionally left blank. Destination checks are now automated after track
        // lays.

    }

    // --- START FIX ---
    @Override
    public boolean layTile(LayTile action) {
        boolean success = super.layTile(action);

        if (success) {
            checkAutomatedDestinations();
        }

        return success;
    }

    private void checkAutomatedDestinations() {
        for (PublicCompany company : operatingCompanies.view()) {
            if (company.hasDestination() && !company.hasReachedDestination()) {
                PublicCompany_1856 comp1856 = (PublicCompany_1856) company;

                // Only evaluate companies started before Phase 5
                if (comp1856.getTrainNumberAvailableAtStart() < 5) {
                    if (hasReachedDestinationVirtual(comp1856)) {

                        // --- START FIX ---
                        // 1. Mark destination as reached in the engine state
                        company.setReachedDestination(true);

                        // 2. Fetch escrow cash
                        int cashInEscrow = comp1856.getMoneyInEscrow();
                        String cashText = net.sf.rails.game.state.Currency.fromBank(cashInEscrow, company);

                        // 3. Construct a prominent notification message box for all players
                        String msg = "=================================================\n"
                                + " CONGRATULATIONS! " + company.getId() + " HAS REACHED ITS DESTINATION!\n"
                                + "=================================================\n\n"
                                + "The route to " + company.getDestinationHex().getId() + " is fully connected.\n"
                                + "Released Escrow Capital: " + cashText + " has been added to the treasury.\n\n"
                                + "From now on, initial offering share purchases will fund the treasury directly.";

                        // Pop up the congratulatory alert panel
                        if (!getRoot().getGameManager().isReloading()) {
                            javax.swing.JOptionPane.showMessageDialog(null, msg,
                                    "1856 Destination Achieved", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        }

                        // 4. Log the transaction details to the game report buffer
                        ReportBuffer.add(this, " ");
                        ReportBuffer.add(this, ">>> " + company.getId() + " has reached its destination city ("
                                + company.getDestinationHex().getId() + ")!");

                        if (cashInEscrow > 0) {
                            ReportBuffer.add(this, LocalText.getText("ReleasedFromEscrow", company.getId(), cashText));

                            // 5. Transfer the escrow cash value directly to the corporate treasury box
                            // Note: Depending on your exact PublicCompany_1856 field mutators,
                            // we must ensure 'setMoneyInEscrow(0)' or clearEscrow() is explicitly invoked.
                            // If your class has a clear or setter, use it here:
                            comp1856.setMoneyInEscrow(0);
                        }
                        // --- END FIX ---
                    }
                }
            }
        }
    }

    private boolean hasReachedDestinationVirtual(PublicCompany_1856 company) {
        MapHex destHex = company.getDestinationHex();
        if (destHex == null)
            return false;

        // 1. Get the pristine map graph (pure physical track network, no token blocking
        // applied)
        net.sf.rails.algorithms.NetworkAdapter na = net.sf.rails.algorithms.NetworkAdapter.create(getRoot());
        net.sf.rails.algorithms.NetworkGraph mapGraph = na.getMapGraph();
        org.jgrapht.Graph<net.sf.rails.algorithms.NetworkVertex, net.sf.rails.algorithms.NetworkEdge> jgraph = mapGraph
                .getGraph();

        // 2. Locate the starting points (the company's base tokens on the map graph)
        java.util.List<net.sf.rails.algorithms.NetworkVertex> startVertices = mapGraph
                .getCompanyBaseTokenVertexes(company);
        if (startVertices.isEmpty())
            return false;

        // 3. Standard BFS queue and visited set to find purely physical track
        // connectivity
        java.util.Queue<net.sf.rails.algorithms.NetworkVertex> queue = new java.util.LinkedList<>(startVertices);
        java.util.Set<net.sf.rails.algorithms.NetworkVertex> visited = new java.util.HashSet<>(startVertices);

        while (!queue.isEmpty()) {
            net.sf.rails.algorithms.NetworkVertex current = queue.poll();

// We must verify that 'current' represents the destination station, 
            // not merely a hex side/boundary entering the destination hex.
            if (current.getHex() != null 
                    && destHex.getId().equals(current.getHex().getId()) 
                    && current.isStation()) {
                return true;
            }

            // Traverse all physically connected neighboring tracks
            for (net.sf.rails.algorithms.NetworkEdge edge : jgraph.edgesOf(current)) {
                net.sf.rails.algorithms.NetworkVertex neighbor = org.jgrapht.Graphs.getOppositeVertex(jgraph, edge,
                        current);
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return false;
    }

    @Override
    protected void executeDestinationActions(List<PublicCompany> companies) {

        for (PublicCompany company : companies) {
            PublicCompany_1856 comp = (PublicCompany_1856) company;
            int cashInEscrow = comp.getMoneyInEscrow();
            if (cashInEscrow > 0) {
                String cashText = Currency.fromBank(cashInEscrow, company);
                ReportBuffer.add(this, LocalText.getText("ReleasedFromEscrow",
                        company.getId(),
                        cashText));
            }
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
                && operatingCompany.value().getCurrentNumberOfLoans() < operatingCompany.value()
                        .sharesOwnedByPlayers()) {

TakeLoans takeAction = new TakeLoans(operatingCompany.value(),
                    1, operatingCompany.value().getValuePerLoan());
            possibleActions.add(takeAction);
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
                    DisplayBuffer.add(this, LocalText.getText("MustRepayLoansBecause",
                            operatingCompany.value().getId(),
                            String.valueOf(operatingCompany.value().sharesOwnedByPlayers())));
                }
                RepayLoans repayAction = new RepayLoans(operatingCompany.value(),
                        minNumber, maxNumber, operatingCompany.value().getValuePerLoan());
                possibleActions.add(repayAction);

                // Step may only be skipped if repayment is optional
                if (minNumber == 0)
                    doneAllowed.set(true);


            } else {
                // No (more) loans
                doneAllowed.set(true);
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
                playerToStartLoanRepayment = playerManager.getPlayerByName(action.getPlayerName());

                // Find and remove the Port token when the 6-train triggers Phase 6
                try {
                    log.info("PORT_LAY_TRACE: Phase 6 triggered via 6-train. Initiating Port token removal.");
                    for (net.sf.rails.game.MapHex hex : getRoot().getMapManager().getHexes()) {
                        if (hex.getBonusTokens() != null) {
                            java.util.Iterator<net.sf.rails.game.BonusToken> iter = hex.getBonusTokens().iterator();
                            boolean found = false;
                            while (iter.hasNext()) {
                                net.sf.rails.game.BonusToken t = iter.next();
                                if (t.getName() != null && t.getName().toLowerCase().contains("port")) {
                                    iter.remove();
                                    found = true;
                                    break;
                                }
                            }
                            if (found) {
                                ReportBuffer.add(this, ">>> The Port token has been removed from hex " + hex.getId()
                                        + " due to Phase 6 (6-train purchase).");
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("PORT_LAY_TRACE: Failed during port token removal routine", e);
                }

            } else if (postPhase.getId().equals("5")) {
                // Make Bridge and Tunnel tokens buyable from the Bank.
                for (SpecialProperty sp : gameManager.getCommonSpecialProperties()) {
                    if (sp instanceof SellBonusToken) {
                        SellBonusToken sbt = (SellBonusToken) sp;
                        sbt.setSeller(bank);
                        log.debug("SP {} is now buyable from the Bank", sp.getId());
                    }
                }

                // Close all private companies when the 5-train purchase triggers Phase 5
                log.info("PHASE_5_TRACE: Phase 5 triggered via 5-train purchase. Closing all private companies.");
                ReportBuffer.add(this, ">>> Phase 5 has begun. All private companies are now closed.");
                
                List<PrivateCompany> privates = getRoot().getCompanyManager().getAllPrivateCompanies();
                if (privates != null) {
                    for (PrivateCompany privateComp : privates) {
                        if (privateComp != null && !privateComp.isClosed()) {
                            privateComp.close();
                            ReportBuffer.add(this, privateComp.getName() + " has closed.");
                            log.debug("PHASE_5_TRACE: Closed private company {}", privateComp.getId());
                        }
                    }
                }
            }
            
        }

        return result;
    }

    @Override
    protected String validateTakeLoans(TakeLoans action) {

        String errMsg = super.validateTakeLoans(action);

        if (errMsg == null) {

            while (true) {
                // Still allowed in current phase?
                if (gameManager.getCurrentPhase().getIndex() > getRoot().getPhaseManager().getPhaseByName("5")
                        .getIndex()) {
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
    protected int calculateLoanAmount(int numberOfLoans) {

        int amount = super.calculateLoanAmount(numberOfLoans);

        // Deduct interest immediately?
        if ((stepObject.value()).compareTo(GameDef.OrStep.PAYOUT) > 0) {
            amount -= calculateLoanInterest(numberOfLoans);
        }

        return amount;
    }

    protected int calculateLoanInterest(int numberOfLoans) {

        return numberOfLoans
                * operatingCompany.value().getValuePerLoan()
                * operatingCompany.value().getLoanInterestPct() / 100;
    }

    @Override
    protected boolean gameSpecificNextStep(GameDef.OrStep step) {

        if (step == GameDef.OrStep.REPAY_LOANS) {

            // Has company any outstanding loans to repay?
            if (operatingCompany.value().getMaxNumberOfLoans() == 0
                    || operatingCompany.value().getCurrentNumberOfLoans() == 0) {
                return false;
                // Is company required to repay loans?
            } else if (operatingCompany.value().sharesOwnedByPlayers() < operatingCompany.value()
                    .getCurrentNumberOfLoans()) {
                return true;
                // Has company enough money to repay at least one loan?
            } else if (operatingCompany.value().getCash() < operatingCompany.value().getValuePerLoan()) {
                return false;
            } else {
                // Loan repayment is possible but optional
                return true;
            }
        }

        return true;
    }

    public void resume(List<PublicCompany> mergingCompanies) {

        // End of CGRFormationRound
        finalLoanRepaymentPending.set(false);
        guiHints.setActivePanel(GuiDef.Panel.MAP);
        guiHints.setCurrentRoundType(getClass());

        if (!resetOperatingCompanies(mergingCompanies))
            return;
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
            if (companiesOperatedThisRound.contains(company))
                cgrCanOperate = false;
        }

        // Find the first company that has not yet operated
        // and is not closed.
        // while (setNextOperatingCompany(false)
        // && operatingCompany.getObject().isClosed());

        // Remove closed companies from the operating company list
        // (PLEASE leave this code in case we need it; it works)
        // for (Iterator<PublicCompany> it = companies.iterator();
        // it.hasNext(); ) {
        // if ((it.next()).isClosed()) {
        // it.remove();
        // }
        // }

        // if (operatingCompany.getObject() != null) {
        // operatingCompanyndex = companies.indexOf(operatingCompany.getObject());
        // }

        for (PublicCompany c : operatingCompanies.view()) {
            if (c.isClosed()) {
                log.info("{} is closed", c.getId());
            } else {
                log.debug("{} is operating", c.getId());
            }
        }

        String message;
        int operatingCompanyIndex = getOperatingCompanyIndex();
        if (cgr.hasStarted()) {
            if (cgrCanOperate) {
                operatingCompanyIndex = Math.max(0, operatingCompanyIndex);
                operatingCompanies.add(operatingCompanyIndex + 1, cgr);
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
            log.debug("Next operating company: {}", operatingCompany.value().getId());
        } else {
            finishOR();
            return false;
        }
        return true;
    }

    @Override
    protected boolean finishTurnSpecials() {
        if (finalLoanRepaymentPending.value()) {

            ((GameManager_1856) gameManager).startCGRFormationRound(this, playerToStartLoanRepayment);
            return false;
        }
        return true;

    }

    @Override
    public boolean processGameSpecificAction(rails.game.action.PossibleAction action) {
        if (action instanceof LayPortToken_1856) {
            LayPortToken_1856 portAction = (LayPortToken_1856) action;
            net.sf.rails.game.PublicCompany comp = getRoot().getCompanyManager()
                    .getPublicCompany(portAction.getCompanyId());

            if (comp != null) {
                String hexId = portAction.getHexId();

                if (hexId == null || hexId.trim().isEmpty()) {
                    if (getRoot().getGameManager().isReloading())
                        return false;

                    // Hardwire the valid anchor city locations directly from the game's XML
                    // specification
                    java.util.List<String> options = java.util.Arrays.asList(
                            "C14", "D19", "E18", "F9", "F17", "H5", "H7", "H17", "J5", "J17", "K2", "M18", "O18");

                    String chosen = (String) javax.swing.JOptionPane.showInputDialog(
                            null,
                            "Select the port city hex to lay the Great Lakes Shipping token:",
                            "Place Port Token",
                            javax.swing.JOptionPane.QUESTION_MESSAGE,
                            null,
                            options.toArray(),
                            options.get(0));

                    if (chosen == null || chosen.isEmpty())
                        return false;

                    portAction.setHexId(chosen);
                    hexId = chosen;
                }

                net.sf.rails.game.MapHex hex = getRoot().getMapManager().getHex(hexId);
                if (hex != null) {
                    net.sf.rails.game.PrivateCompany portPriv = null;
                    for (net.sf.rails.game.PrivateCompany priv : comp.getPrivates()) {
                        if (priv != null && (priv.getId().toLowerCase().contains("glsc")
                                || priv.getId().toLowerCase().contains("port")
                                || priv.getId().toLowerCase().contains("ship"))) {

                            portPriv = priv;
                            break;
                        }
                    }

                    if (portPriv != null) {
                        net.sf.rails.game.BonusToken portToken = net.sf.rails.game.BonusToken.create(portPriv);
                        if (portToken != null) {
                            portToken.setName(comp.getId() + "_Port");
                            portToken.setValue(20);
                            hex.layBonusToken(portToken, getRoot().getPhaseManager());
                            net.sf.rails.common.ReportBuffer.add(this,
                                    comp.getId() + " places the Great Lakes Shipping Port token on " + hex.getId()
                                            + ".");
                            // Rule: Placement closes the company
                            portPriv.close();
                            net.sf.rails.common.ReportBuffer.add(this, portPriv.getName() + " is closed.");
                        }
                    }
                    return true;
                }
            }
            return false;
        }
        return super.processGameSpecificAction(action);
    }

    @Override
    public boolean setPossibleActions() {
        boolean result = super.setPossibleActions();

        net.sf.rails.game.PublicCompany comp = getOperatingCompany();
        if (comp != null && !comp.isClosed()) {
            net.sf.rails.game.PrivateCompany portPriv = null;
            for (net.sf.rails.game.PrivateCompany priv : comp.getPrivates()) {
               
                if (priv != null && (priv.getId().toLowerCase().contains("glsc")
                        || priv.getId().toLowerCase().contains("port")
                        || priv.getId().toLowerCase().contains("ship"))) {

                    portPriv = priv;
                    break;
                }
            }
            if (portPriv != null && !portPriv.isClosed()) {
                // Check if port is already on the map
                boolean portOnMap = false;
                for (net.sf.rails.game.MapHex hex : getRoot().getMapManager().getHexes()) {
                    if (hex.getBonusTokens() != null) {
                        for (net.sf.rails.game.BonusToken t : hex.getBonusTokens()) {
                            if (t.getName() != null && t.getName().toLowerCase().contains("port")) {
                                portOnMap = true;
                                break;
                            }
                        }
                    }
                    if (portOnMap)
                        break;
                }

                if (!portOnMap) {
                    possibleActions.add(new LayPortToken_1856(getRoot(), comp.getId(), null));
                }
            }
        }

        // Filter out illegal self-purchase token buy choices using the structural wrapper API
        java.util.List<rails.game.action.PossibleAction> targetsToRemove = new java.util.ArrayList<>();
        for (rails.game.action.PossibleAction act : possibleActions.getList()) {
            if (act instanceof rails.game.action.BuyBonusToken) {
                rails.game.action.BuyBonusToken bbt = (rails.game.action.BuyBonusToken) act;
                if (bbt.getSeller() == comp) {
                    targetsToRemove.add(bbt);
                }
            }
            if (act instanceof rails.game.action.UseSpecialProperty) {
                rails.game.action.UseSpecialProperty usp = (rails.game.action.UseSpecialProperty) act;
                if (usp.getSpecialProperty() instanceof net.sf.rails.game.special.SellBonusToken) {
                    targetsToRemove.add(usp);
                }
            }
        }

        
        for (rails.game.action.PossibleAction act : targetsToRemove) {
            possibleActions.remove(act);
        }

        if (loanTakenThisTurn.value()) { // Use .value() to check state
        java.util.List<rails.game.action.PossibleAction> actionsToRemove = new java.util.ArrayList<>();
        for (rails.game.action.PossibleAction act : possibleActions.getList()) {
            if (act instanceof rails.game.action.RepayLoans) {
                actionsToRemove.add(act);
            }
        }
        possibleActions.removeAll(actionsToRemove);
    }

        return result;
    }


    @Override
    public boolean buyBonusToken(rails.game.action.BuyBonusToken action) {
        net.sf.rails.game.special.SellBonusToken sbt = action.getSpecialProperty();
        net.sf.rails.game.state.Owner seller = sbt.getSeller();
        net.sf.rails.game.PublicCompany company = operatingCompany.value();

        // Rulebook check: If this public company already owns the private company,
        // the token application is free and should not execute a self-wire transfer.
        if (seller == company) {
            
            // Replicate the token application process without calling Currency.wire()
            net.sf.rails.game.Bonus bonus = new net.sf.rails.game.Bonus(
                company, 
                sbt.getId(),
                sbt.getValue(), 
                sbt.getLocations(), 
                sbt.allowOneTrainOnly()
            );
            company.addBonus(bonus);

            net.sf.rails.common.ReportBuffer.add(this, 
                company.getId() + " activates its own " + sbt.getName() + " token bonus for $0.");

            sbt.setExercised();

            if (getStep() == net.sf.rails.game.GameDef.OrStep.LAY_TOKEN && !canLayAnyTokens(false)) {
                nextStep();
            }
            return true;
        }

        // Otherwise, proceed with the normal cross-company $50 purchase routine
        return super.buyBonusToken(action);
    }
}
