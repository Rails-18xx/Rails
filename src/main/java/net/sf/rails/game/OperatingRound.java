package net.sf.rails.game;

import com.google.common.collect.Iterables;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.common.*;
import net.sf.rails.game.financial.*;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.special.*;
import net.sf.rails.game.specific._1835.GameManager_1835;
import net.sf.rails.game.specific._1835.OperatingRound_1835;
// import net.sf.rails.game.specific._1835.OperatingRound_1835;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Observer;
import net.sf.rails.game.state.*;
import net.sf.rails.util.SequenceUtil;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;
import rails.game.correct.ClosePrivate;
import rails.game.correct.OperatingCost;

import java.util.*;

/**
 * Implements a basic Operating Round.
 * <p>
 * A new instance must be created for
 * each new Operating Round. At the end of a round, the current instance should
 * be discarded.
 */
/*
 * Because if its sheer size, this class has been divided into a number of
 * "chapters".
 * TABLE OF CONTENTS:
 * 1. OR START AND END
 * 2. CENTRAL PROCESSING
 * 2.1. Process user action
 * 2.2. Prepare next action
 * 2.3. Turn control
 * 2.4. Operating companies
 * 2.5. Step control
 * 3. COMMON ACTIONS
 * 3.1. Noops
 * 3.2. Discarding trains
 * 3.3. Privates
 * 3.4. Destinations
 * 3.5. Loans
 * 3.6. Rights
 * 3.7. Share actions
 * 4. LAYING TILES
 * 5. TOKEN LAYING
 * 5.1. Base tokens
 * 5.2. Bonus tokens
 * 5.3. All layable tokens
 * 6. REVENUE AND DIVIDEND
 * 6.1. Validate
 * 6.2. Execute revenue and dividend
 * 6.3. Earnings distribution
 * 6.4. Rounding
 * 6.5. Deductions
 * 6.6. Prepare action
 * 7. TRAIN PURCHASING
 * 7.1. Buy train execution
 * 7.2. Buy train effects
 * 7.3. Buy train preparation
 * 8. VARIOUS UTILITIES
 */
public class OperatingRound extends Round implements Observer {

    private static final Logger log = LoggerFactory.getLogger(OperatingRound.class);

    /* Transient memory (per round only) */
    protected final GenericState<GameDef.OrStep> stepObject = new GenericState<>(this, "ORStep",
            GameDef.OrStep.INITIAL);

    /* flag for using Rails without map support */
    protected final boolean noMapMode;

    // TODO: Check if this should not be turned into a State?
    protected final List<PublicCompany> companiesOperatedThisRound = new ArrayList<>();

    protected ArrayListState<PublicCompany> operatingCompanies;

    protected final GenericState<PublicCompany> operatingCompany = new GenericState<>(this, "operatingCompany");

    // Non-persistent lists (are recreated after each user action)

    protected final HashMapState<String, Integer> tileLaysPerColour = HashMapState.create(this, "tileLaysPerColour");

    protected transient final List<LayBaseToken> currentNormalTokenLays = new ArrayList<>();
    protected transient final List<LayBaseToken> currentSpecialTokenLays = new ArrayList<>();
    /**
     * A List per player with owned companies that have excess trains
     */
    protected transient Map<Player, List<PublicCompany>> excessTrainCompanies;

    protected final ArrayListState<TrainCardType> trainsBoughtThisTurn = new ArrayListState<>(this,
            "trainsBoughtThisTurn");

    protected HashMapState<PublicCompany, Integer> loansThisRound;
    protected String thisOrNumber;
    /** Tracks if a normal (non-extra) token has been laid this company's turn. */
    protected final BooleanState normalTokenLaidThisTurn = new BooleanState(this, "normalTokenLaidThisTurn", false);

    protected transient PossibleAction selectedAction;

    protected final GenericState<PossibleAction> savedAction = new GenericState<>(this, "savedAction");
    protected final GenericState<String> pendingTrainName = new GenericState<>(this, "pendingTrainName");

    protected GameDef.OrStep[] steps = new GameDef.OrStep[] {
            GameDef.OrStep.INITIAL, GameDef.OrStep.LAY_TRACK,
            GameDef.OrStep.LAY_TOKEN, GameDef.OrStep.CALC_REVENUE,
            GameDef.OrStep.PAYOUT, GameDef.OrStep.BUY_TRAIN,
            GameDef.OrStep.TRADE_SHARES, GameDef.OrStep.FINAL
    };

    // protected boolean doneAllowed = false;
    protected final BooleanState emergency = new BooleanState(this, "emergency", false);
    protected final BooleanState doneAllowed = new BooleanState(this, "doneAllowed", false);

    protected TrainManager trainManager = getRoot().getTrainManager();
    public transient GenericState<LayTile> pendingRelayAction = new GenericState<>(this, "pendingRelayAction");

    protected final BooleanState normalTileLaidThisTurn = new BooleanState(this, "normalTileLaidThisTurn", false);

    /*
     * =======================================
     * 1. OR START and END
     * =======================================
     */

    /**
     * Constructed via Configure
     */
    public OperatingRound(GameManager parent, String id) {
        super(parent, id);

        this.operatingCompanies = new ArrayListState<>(this, "operatingCompanies", setOperatingCompanies());
        this.stepObject.addObserver(this);

        this.noMapMode = GameOption.getAsBoolean(this, "NoMapMode");

        this.guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        this.guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
        this.guiHints.setActivePanel(GuiDef.Panel.MAP);

        // --- Initialize the emergency train maps here ---
        this.newEmergencyTrains = new TreeMap<>(); // *** ADD THIS LINE ***
        this.usedEmergencyTrains = new TreeMap<>(); // *** ADD THIS LINE ***

        // Clear registry of items that could otherwise generate income twice per OR
        // Used by 1837
        gameManager.clearBlockedCertificates();
        gameManager.clearBlockedTrains();
    }

    public void start() {
        thisOrNumber = gameManager.getORId();

        ReportBuffer.add(this, LocalText.getText("START_OR", thisOrNumber));

        for (Player player : getRoot().getPlayerManager().getPlayers()) {
            // Clear the "sold shares" memory so the UI stops showing red warnings (Stock
            // Market/Status Window).
            // NOTE: Verify that 'clearSoldCompanies()' is the exact method name in your
            // Player.java.
            // It might also be 'resetSoldCompanies()' or 'getSoldCompanies().clear()'.
            player.resetSoldThisRound();
            player.setWorthAtORStart();
        }

        privatesPayOut();

        if (operatingCompanies.size() > 0) {
            StringBuilder msg = new StringBuilder();
            for (PublicCompany company : operatingCompanies.view()) {
                msg.append(",").append(company.getId());
            }
            if (msg.length() > 0)
                msg.deleteCharAt(0);
            //

            if (setNextOperatingCompany(true)) {
                setStep(GameDef.OrStep.INITIAL);
            }
            return;
        }

        // No operating companies yet: close the round.
        String text = LocalText.getText("ShortORExecuted");
        ReportBuffer.add(this, text);
        DisplayBuffer.add(this, text);
        finishRound();
    }

    protected void privatesPayOut() {
        int count = 0;
        for (PrivateCompany priv : companyManager.getAllPrivateCompanies()) {
            if (!priv.isClosed()) {
                // Bank Portfolios are not MoneyOwners!
                if (priv.getOwner() instanceof MoneyOwner) {
                    MoneyOwner recipient = (MoneyOwner) priv.getOwner();
                    int revenue = priv.getRevenueByPhase(Phase.getCurrent(this));

                    if (revenue != 0) {
                        if (count++ == 0)
                            ReportBuffer.add(this, "");
                        String revText = Currency.fromBank(revenue, recipient);
                        ReportBuffer.add(this, LocalText.getText("ReceivesFor",
                                recipient.getId(), revText, priv.getId()));
                    }
                }
            }
        }
    }

    // File: OperatingRound.java (~ Line 172)
    // [REPLACE the entire 'resume' method with this]

    @Override
    public void resume() {

        // CRITICAL: Ensure all transient/runtime lists are clean BEFORE processing any
        // saved actions.
        if (this.currentNormalTokenLays != null)
            this.currentNormalTokenLays.clear();
        if (this.currentSpecialTokenLays != null)
            this.currentSpecialTokenLays.clear();

        // Fix for Double-Execution: Retrieve the saved action
        PossibleAction actionToProcess = savedAction.value();
        // DO NOT clear savedAction yet; validation inside buyTrain relies on it.

        // 1. Check for a pending train by ID string
        String targetName = pendingTrainName.value();
        log.info("LIFECYCLE: resume() invoked. pendingTrainName retrieved as: [{}]",
                targetName != null ? targetName : "NULL");

        if (targetName != null) {

            // Re-generate current valid actions
            possibleActions.clear();
            setBuyableTrains();

            boolean found = false;
            for (PossibleAction pa : possibleActions.getList()) {
                if (pa instanceof BuyTrain) {
                    BuyTrain bt = (BuyTrain) pa;
                    if (targetName.equals(bt.getTrain().getName())) {
                        buyTrain(bt); // Validation now passes since cash is present
                        found = true;
                        break;
                    }
                }
            }
            if (!found)
                log.error("Auto-purchase failed: Train {} not found in pool.", targetName);

        } else if (savedAction.value() instanceof RepayLoans) {
            executeRepayLoans((RepayLoans) savedAction.value());
        } else {
            setPossibleActions();
        }

        // 2. Clear states and signal UI refresh
        savedAction.set(null);
        pendingTrainName.set(null);
        wasInterrupted.set(true); // <--- UNCOMMENTED: Trigger UI sync

        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
        guiHints.setActivePanel(GuiDef.Panel.MAP);
    }

    protected void finishOR() {

        // Check if any privates must be closed
        // (now only applies to 1856 W&SR) - no, that is at end of TURN
        // for (PrivateCompany priv : gameManager.getAllPrivateCompanies()) {
        // priv.checkClosingIfExercised(true);
        // }

        ReportBuffer.add(this, " ");
        ReportBuffer.add(this,
                LocalText.getText("EndOfOperatingRound", thisOrNumber));

        // Update the worth increase per player
        int orWorthIncrease;
        for (Player player : getRoot().getPlayerManager().getPlayers()) {
            player.setLastORWorthIncrease();
            orWorthIncrease = player.getLastORWorthIncrease().value();
            ReportBuffer.add(this, LocalText.getText("ORWorthIncrease",
                    player.getId(), thisOrNumber,
                    Bank.format(this, orWorthIncrease)));
        }

        // OR done. Inform GameManager.
        finishRound();
    }

    @Override
    public boolean process(PossibleAction action) {

        // JIT Correction: Ensure the Operating Company matches the Saved Action.
        if (gameManager.isReloading()) {

            PublicCompany actionCompany = null;
            if (action instanceof PossibleORAction) {
                actionCompany = ((PossibleORAction) action).getCompany();
            } else if (action instanceof RelayTokenAction) {
                actionCompany = ((RelayTokenAction) action).getCompany();
            }

            // The subclass (OperatingRound_1835) must handle the swap for discards
            // to preserve the return-to-origin logic.
            if (actionCompany != null
                    && actionCompany != operatingCompany.value()
                    && !(action instanceof DiscardTrain)) {

                this.operatingCompany.set(actionCompany);
                setStep(GameDef.OrStep.INITIAL);
                try {
                    initTurn();
                } catch (Exception e) {
                }
                nextStep();
            }
        }

        // SAFETY NET: Zombie Company Detection
        if (operatingCompany.value() != null && operatingCompany.value().isClosed()) {
            if (!(action instanceof DiscardTrain)) {
                log.warn("OperatingRound: Detected closed company {} attempting to act. Forcing finishTurn().",
                        operatingCompany.value().getId());
                finishTurn();
                return true;
            }
        }

        boolean result = false;
        doneAllowed.set(false);

        /*--- Common OR checks ---*/
        /* Check operating company */
        if (!(action instanceof DiscardTrain) && !action.isCorrection()) {
            PublicCompany company = null;

            if (action instanceof PossibleORAction) {
                company = ((PossibleORAction) action).getCompany();
            } else if (action instanceof RelayTokenAction) {
                company = ((RelayTokenAction) action).getCompany();
            }

            if (company != null && company != operatingCompany.value()) {
                String currentId = (operatingCompany.value() != null) ? operatingCompany.value().getId() : "NULL";

                DisplayBuffer.add(this, LocalText.getText("WrongCompany",
                        company.getId(), currentId));
                return false;
            }
        }

        selectedAction = action;

        if (selectedAction instanceof LayTile) {
            if (selectedAction instanceof LayTileAndHomeTokenAction) {
                result = processLayTileAndHomeToken((LayTileAndHomeTokenAction) selectedAction);
            } else {
                LayTile layTileAction = (LayTile) selectedAction;
                if (layTileAction.getType() == LayTile.CORRECTION) {
                    result = layTileCorrection(layTileAction);
                } else {
                    result = layTile(layTileAction);
                }
            }

        } else if (selectedAction instanceof LayBaseToken) {
            result = layBaseToken((LayBaseToken) selectedAction);
        } else if (selectedAction instanceof LayBonusToken) {
            result = layBonusToken((LayBonusToken) selectedAction);
        } else if (selectedAction instanceof BuyBonusToken) {
            result = buyBonusToken((BuyBonusToken) selectedAction);
        } else if (selectedAction instanceof OperatingCost) {
            result = executeOperatingCost((OperatingCost) selectedAction);
        } else if (selectedAction instanceof SetDividend) {
            result = setRevenueAndDividend((SetDividend) selectedAction);
        } else if (selectedAction instanceof BuyTrain) {
            result = buyTrain((BuyTrain) selectedAction);
        } else if (selectedAction instanceof DiscardTrain) {

            executeDiscardTrain((DiscardTrain) action);

            // Return true to signal the action is complete.
            // The engine will refresh and re-check the train limit automatically.
            return true;

        } else if (selectedAction instanceof BuyPrivate) {
            result = buyPrivate((BuyPrivate) selectedAction);
        } else if (selectedAction instanceof ReachDestinations) {
            result = reachDestinations((ReachDestinations) selectedAction);
        } else if (selectedAction instanceof GrowCompany) {
            result = growCompany((GrowCompany) action);
        } else if (selectedAction instanceof TakeLoans) {
            result = takeLoans((TakeLoans) selectedAction);
        } else if (selectedAction instanceof RepayLoans) {
            result = repayLoans((RepayLoans) selectedAction);
        } else if (selectedAction instanceof ClosePrivate) {
            result = executeClosePrivate((ClosePrivate) selectedAction);
        } else if (selectedAction instanceof UseSpecialProperty
                && ((UseSpecialProperty) selectedAction).getSpecialProperty() instanceof SpecialRight) {
            result = buyRight((UseSpecialProperty) selectedAction);
        } else if (selectedAction instanceof UseSpecialProperty
                && ((UseSpecialProperty) selectedAction).getSpecialProperty() instanceof ExchangeForShare) {
            useSpecialProperty((UseSpecialProperty) selectedAction);
        } else if (selectedAction instanceof NullAction) {
            NullAction nullAction = (NullAction) action;
            switch (nullAction.getMode()) {
                case DONE:
                case PASS:
                    result = done(nullAction);
                    break;
                case SKIP:
                    skip(nullAction);
                    result = true;
                    break;
                default:
                    break;
            }
        } else if (processGameSpecificAction(action)) {
            result = true;
        } else {
            DisplayBuffer.add(
                    this,
                    LocalText.getText("UnexpectedAction",
                            selectedAction.toString()));
            return false;
        }

        return result;
    }

    public boolean useSpecialProperty(UseSpecialProperty action) {

        SpecialProperty sp = action.getSpecialProperty();

        if (sp instanceof ExchangeForShare) {

            return executeExchangeForShare(action, (ExchangeForShare) sp);

        } else {
            return false;
        }
    }

    public boolean executeExchangeForShare(UseSpecialProperty action, ExchangeForShare sp) {

        PublicCompany publicCompany = companyManager.getPublicCompany(sp.getPublicCompanyName());
        PrivateCompany privateCompany = (PrivateCompany) sp.getOriginalCompany();
        Owner owner = privateCompany.getOwner();
        Player player = null;
        String errMsg = null;
        boolean ipoHasShare = ipo.getShare(publicCompany) >= sp.getShare();
        boolean poolHasShare = pool.getShare(publicCompany) >= sp.getShare();

        while (true) {

            /* Check if the private is owned by a player */
            if (!(owner instanceof Player)) {
                errMsg = LocalText.getText("PrivateIsNotOwnedByAPlayer",
                        privateCompany.getId());
                break;
            }

            player = (Player) owner;

            /* Check if a share is available */
            if (!ipoHasShare && !poolHasShare) {
                errMsg = LocalText.getText("NoSharesAvailable",
                        publicCompany.getId());
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText(
                    "CannotSwapPrivateForCertificate",
                    player.getId(),
                    privateCompany.getId(),
                    sp.getShare(),
                    publicCompany.getId(),
                    errMsg));
            return false;
        }

        Certificate cert = ipoHasShare
                ? ipo.findCertificate(publicCompany, false)
                : pool.findCertificate(publicCompany, false);
        cert.moveTo(player);
        ReportBuffer.add(this, LocalText.getText("SwapsPrivateForCertificate",
                player.getId(),
                privateCompany.getId(),
                sp.getShare(),
                publicCompany.getId()));
        sp.setExercised();
        privateCompany.setClosed();

        return true;
    }

    /**
     * Stub, to be overridden in game-specific subclasses.
     */
    public boolean processGameSpecificAction(PossibleAction action) {
        return false;
    }

    /*
     * =======================================
     * 2.2. PREPARE NEXT ACTION
     * =======================================
     */

    /**
     * Stub, can be overridden by subclasses
     */
    protected void setGameSpecificPossibleActions() {

    }

    /*
     * =======================================
     * 2.3. TURN CONTROL
     * =======================================
     */

    protected void initTurn() {

        PublicCompany company = operatingCompany.value();

        GameManager gm = gameManager; // protected field from Round.java

        if (gm.isTimeManagementEnabled()) {
            Player playerToIncrement = company.getPresident(); // Returns Owner for Minors, Director for Share Co.
            int increment = 0;

            // 1. Unified Increment: Use the Major/Round increment from settings as the base
            increment = gm.getTimeMgmtMajorCoIncrement();

            // 2. Dynamic Operator Bonus
            // We check the specific Operator Name and Multiplier stored in GameManager
            if (playerToIncrement != null) {
                String operatorName = gm.getTimeMgmtOperatorName();

                // Compare ignoring case to be safe
                if (operatorName != null && operatorName.equalsIgnoreCase(playerToIncrement.getId())) {
                    double multiplier = gm.getTimeMgmtOperatorMultiplier();
                    increment = (int) (increment * multiplier);

                    ReportBuffer.add(this, LocalText.getText("OperatorBonusApplied",
                            playerToIncrement.getId(),
                            String.valueOf(increment)));
                }
            }

            // We pass the current Round ID (e.g. "OR_1.1") to ensure uniqueness
            if (increment > 0) {
                gm.grantTimeBonus(playerToIncrement, gm.getORId(), increment);
            }

        }

        ReportBuffer.add(this, " ");
        Player president = operatingCompany.value().getPresident();
        String presId = (president != null) ? president.getId() : "None";
        ReportBuffer.add(this, LocalText.getText("CompanyOperates",
                operatingCompany.value().getId(),
                presId));
        if (president != null) {
            playerManager.setCurrentPlayer(president);
        }

        if (noMapMode && !operatingCompany.value().hasLaidHomeBaseTokens()) {
            // Lay base token in noMapMode
            BaseToken token = operatingCompany.value().getNextBaseToken();
            if (token == null) {
                // (bank.getUnavailable().addBonusToken(token));
            } else {
                // FIXME: Check where to lay the base tokens in NoMapMode
                // (bank.getUnavailable().addBonusToken(token));
            }
        }
        operatingCompany.value().initTurn();
        normalTokenLaidThisTurn.set(false); // Reset for the new company's turn
        trainsBoughtThisTurn.clear();
        normalTileLaidThisTurn.set(false);
    }

    protected void finishTurn() {

        if (!operatingCompany.value().isClosed()) {
            operatingCompany.value().setOperated();
            companiesOperatedThisRound.add(operatingCompany.value());

            for (PrivateCompany priv : operatingCompany.value().getPortfolioModel().getPrivateCompanies()) {
                priv.checkClosingIfExercised(true);
            }
        }

        if (!finishTurnSpecials())
            return;

        if (setNextOperatingCompany(false)) {
            setStep(GameDef.OrStep.INITIAL);
        } else {
            finishOR();
        }
    }

    /**
     * Stub, may be overridden in subclasses
     * Return value: TRUE = normal turn end;
     * FALSE = return immediately from finishTurn().
     */
    protected boolean finishTurnSpecials() {
        return true;
    }

    /*
     * =======================================
     * 2.4. OPERATING COMPANIES
     * =======================================
     */

    protected boolean setNextOperatingCompany(boolean initial) {

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

            if (operatingCompany.value().isClosed()
                    || operatingCompany.value().isHibernating()
                    || operatingCompany.value().getPresident() == null)
                continue;

            return true;
        }
    }

    /**
     * Insert a newly formed company that is allowed to operate
     * into the current list of operating companies at the proper spot:
     * just before the first company with a lower current price
     * that has not yet operated and isn't currently operating.
     * 
     * @param newCompany New company to insert in operating order
     */
    /* Currently used by 1826 and 1835 */
    public void insertNewOperatingCompany(PublicCompany newCompany) {
        int index = 0;
        int operatingCompanyIndex = getOperatingCompanyIndex();
        List<PublicCompany> companies = setOperatingCompanies();
        for (PublicCompany company : companies) {
            if (index > operatingCompanyIndex
                    && company.hasStockPrice()
                    && company.hasFloated()
                    && !company.isClosed()
                    && company != operatingCompany.value()
                    && company.getCurrentSpace().getPrice() < newCompany.getCurrentSpace().getPrice()) {
                break;
            }
            if (index < companies.size() - 1)
                index++;
        }
        // Insert PR at the found index (possibly at the end)
        // operatingCompanies.view(), index);
        operatingCompanies.add(index, newCompany);
    }

    protected void setOperatingCompany(PublicCompany company) {
        operatingCompany.set(company);
    }

    /**
     * Close an operating company.
     *
     * Currently only called by OperatingEound_1826
     *
     * @param company The company to close
     */
    protected void closeCompany(PublicCompany company) {
        company.setClosed();
        operatingCompanies.remove(company);
    }

    /**
     * Get the public company that has the turn to operate.
     *
     * @return The currently operating company object.
     */
    public PublicCompany getOperatingCompany() {
        return operatingCompany.value();
    }

    public List<PublicCompany> getOperatingCompanies() {
        return operatingCompanies.view();
    }

    public int getOperatingCompanyIndex() {
        return operatingCompanies.indexOf(getOperatingCompany());
    }

    /*
     * =======================================
     * 2.5. STEP CONTROL
     * =======================================
     */

    /**
     * Get the current operating round step (i.e. the next action).
     *
     * @return The number that defines the next action.
     */
    public GameDef.OrStep getStep() {
        return stepObject.value();
    }

    /**
     * Bypass normal order of operations and explicitly set round step. This
     * should only be done for specific rails.game exceptions, such as forced
     * train purchases.
     *
     * @param step The next OR step to be executed
     */
    protected void setStep(GameDef.OrStep step) {
        stepObject.set(step);
    }

    /**
     * Internal method: change the OR state to the next step. If the currently
     * Operating Company is done, notify this.
     */
    protected void nextStep() {
        nextStep(getStep());
    }

    /**
     * Take the next step after a given one (see nextStep())
     */
    protected void nextStep(GameDef.OrStep step) {

        PublicCompany company = operatingCompany.value();

        // Cycle through the steps until we reach one where a user action is
        // expected.
        int stepIndex;
        GameDef.OrStep newStep = step;
        for (stepIndex = 0; stepIndex < steps.length; stepIndex++) {
            if (steps[stepIndex] == newStep)
                break;
        }
        while (++stepIndex < steps.length) {
            newStep = steps[stepIndex];
            if (newStep == GameDef.OrStep.LAY_TRACK) {
                initNormalTileLays();
            }

            if (newStep == GameDef.OrStep.LAY_TOKEN) {
                /*
                 * List<SpecialProperty> bonuses = gameManager.getCommonSpecialProperties();
                 * boolean bonusTokensForSale =
                 * //bonuses.size() > 0
                 * // FIXME The above condition is probably wrong, it likely should be:
                 * !getSpecialProperties(SpecialBonusTokenLay.class).isEmpty()
                 * // but that needs to be sorted out precisely.
                 * // The intended effect is that the TOKEN_LAY step is skipped
                 * // if there are no base or bonus tokens to be laid.
                 * //
                 * // Note: removed temporary exception for 1856 14apr2021
                 * ;
                 * if (company.getNumberOfFreeBaseTokens() == 0
                 * && !bonusTokensForSale) {
                 */

                company.clearTokenableStops();
                if (!canLayAnyTokens(true)) {
                    continue;
                }
            }

            if (newStep == GameDef.OrStep.CALC_REVENUE) {

                // if (company.hasTrains()) {
                if (companyHasRunningTrains(true)) {
                    // All OK, we can't check here if it has a route

                } else if (company.canGenerateOtherRevenue()) {
                    // In 18Scan a trainless minor company still pays out.
                    executeTrainlessRevenue(newStep);
                    continue;
                } else {
                    executeSetRevenueAndDividend(new SetDividend(getRoot(), 0,
                            false, new int[] { SetDividend.NO_TRAIN }));
                    // TODO: This probably does not handle share selling correctly
                    continue;
                }
            }

            if (newStep == GameDef.OrStep.PAYOUT) {
                // This step is now obsolete
                continue;
            }

            if (newStep == GameDef.OrStep.REPAY_LOANS) {
                if (!company.canLoan() || company.getCurrentNumberOfLoans() == 0) {
                    continue;
                }
            }

            if (newStep == GameDef.OrStep.TRADE_SHARES) {
                // Is company allowed to trade treasury shares?
                if ((!company.mayTradeShares() ||
                        (company.mustHaveOperatedToTradeShares() && !company.hasOperated()))
                        && !company.mayTradeBonds()) {
                    continue;
                }

                /*
                 * Check if any trading is possible. If not, skip this step.
                 * (but register a Done action for BACKWARDS COMPATIBILITY only)
                 */
                // Preload some expensive results
                int ownShare = company.getPortfolioModel().getShare(company);
                int poolShare = pool.getShare(company); // Expensive, do it once
                int bondsCount = company.getPortfolioModel().getBondsCount(company);
                // Can it buy?
                boolean canBuy = (company.hasOperated.value() || !company.mustHaveOperatedToBuyShares())
                        && ownShare < GameDef.getParmAsInt(this, GameDef.Parm.TREASURY_SHARE_LIMIT)
                        && company.getCash() >= company.getCurrentSpace().getPrice()
                        && poolShare > 0;
                // Can it sell?
                boolean canSell = (company.hasOperated.value() || !company.mustHaveOperatedToSellShares())
                        && company.getPortfolioModel().getShare(company) > 0
                        && poolShare < GameDef.getParmAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT);
                // Above we ignore the possible existence of double shares (as
                // in 1835).
                // Can we buy Bonds (only in 1826)?
                boolean canBuyBonds = company.hasBonds()
                        && bondsCount < company.getNumberOfBonds()
                        && company.getCash() >= company.getPriceOfBonds();

                if (!canBuy && !canSell && !canBuyBonds) {
                    // XXX For BACKWARDS COMPATIBILITY only,
                    // register a Done skip action during reloading.
                    if (gameManager.isReloading()) {
                        gameManager.setSkipDone(GameDef.OrStep.TRADE_SHARES);
                    }
                    continue;
                }

                gameManager.startTreasuryShareTradingRound(operatingCompany.value());

            }

            if (!gameSpecificNextStep(newStep)) {
                // Skipping newStep
                continue;
            }

            // No reason found to skip this step
            break;
        }

        if (newStep == GameDef.OrStep.FINAL) {
            finishTurn();
        } else {
            setStep(newStep);
        }

    }

    /**
     * Stub, may be overridden if there are non-running trains.
     * Used in 1837
     * 
     * @param display Not used here; see 1837 version
     * @return True if the company has trains that are allowed to run
     */
    protected boolean companyHasRunningTrains(boolean display) {
        return operatingCompany.value().hasTrains();
    }

    /**
     * Stub, can be overridden in subclasses to check for extra steps
     * 
     * @param step An OR step to be checked for producing possible actions
     * @return True if this step has any possible actions
     */
    protected boolean gameSpecificNextStep(GameDef.OrStep step) {
        return true;
    }

    /**
     * Stub, to be used in case a trainless company still
     * generates some income (e.g. 18Scan minors)
     * 
     * @param step An OR step that is considered for execution
     */
    protected void executeTrainlessRevenue(GameDef.OrStep step) {
    }

    /**
     * Stub, to be used in case a zero earnings run still
     * generates some income (e.g. 18Scan minors)
     * 
     * @param action SetDividend action with zero revenue
     */
    protected SetDividend checkZeroRevenue(SetDividend action) {
        return action;
    }

    /**
     * This method is only called at the start of each step (unlike
     * updateStatus(), which is called after each user action)
     */
    protected void prepareStep() {
        GameDef.OrStep step = stepObject.value();

        if (step == GameDef.OrStep.LAY_TRACK) {
            // getNormalTileLays();
        } else if (step == GameDef.OrStep.LAY_TOKEN) {

        }

    }

    /*
     * =======================================
     * 3. COMMON ACTIONS (not bound to steps)
     * 3.1. NOOPS
     * =======================================
     */

    public void skip(NullAction action) {

        nextStep();
    }

    public boolean done(NullAction action) {

        // BRANCH 1: The "Discard Trap" (Modified)
        if (checkForExcessTrains()) {
            setStep(GameDef.OrStep.DISCARD_TRAINS);
            return true; // <--- We only touched this line inside the IF block
        }

        // BRANCH 2: The Normal "Done" (Unchanged)
        // If checkForExcessTrains() returns false, the code above is skipped entirely.

        nextStep(); // Advance to Share Trading or Finish

        if (getStep() == GameDef.OrStep.FINAL) {
            finishTurn();
        }

        return true; // <--- The normal "Done" ALREADY returns true!
    }

    // In OperatingRound.java

    public boolean discardTrain(DiscardTrain action) {

        // 1. Setup Context
        PublicCompany currentOp = operatingCompany.value();
        Player currentPlayer = playerManager.getCurrentPlayer();

        PublicCompany actionComp = action.getCompany();
        Player actionPlayer = (actionComp != null) ? actionComp.getPresident() : null;

        if (actionComp != null) {
            log.info("Portfolio Inspection [{}]: {}", actionComp.getId(),
                    actionComp.getPortfolioModel().getTrainList());
        }

        // 2. The "Dirty Fix" (Make available to all games)
        // Ensures the engine validates the action even if the map wasn't updated yet.
        if (excessTrainCompanies == null) {
            excessTrainCompanies = new HashMap<>();
        }
        if (actionPlayer != null) {
            List<PublicCompany> comps = excessTrainCompanies.get(actionPlayer);
            if (comps == null) {
                comps = new ArrayList<>();
                excessTrainCompanies.put(actionPlayer, comps);
            }
            if (!comps.contains(actionComp)) {
                comps.add(actionComp);
            }
        }

        // 3. Context Swap (Handle Interjections)
        boolean isInterjection = (actionComp != null && currentOp != null && actionComp != currentOp);

        if (isInterjection) {
            operatingCompany.set(actionComp);
            if (actionPlayer != null) {
                playerManager.setCurrentPlayer(actionPlayer);
            }
        }

        // 4. Execute Action (With Safety Wrapper)
        boolean processed = false;
        try {
            processed = action.process(this);
        } catch (Exception e) {
            log.error(">>> FORENSIC ERROR: Exception during action.process()", e);
        }

        // 5. Restore Context
        if (isInterjection) {
            operatingCompany.set(currentOp);
            if (currentPlayer != null) {
                playerManager.setCurrentPlayer(currentPlayer);
            }
        }

        if (!processed) {
            return false;
        }

        // 6. Check for remaining discards
        boolean moreDiscards = checkForExcessTrains();

        // --- HOOK: Allow Subclasses to intervene (e.g., 1835 Prussian Formation) ---
        // If the hook returns TRUE, it means the subclass handled the flow, so we
        // return.
        if (processGameSpecificDiscard(action, moreDiscards)) {
            return true;
        }

        // 7. Standard Post-Discard Logic (Now improved for all games)
        if (!moreDiscards) {
            newPhaseChecks();

            // If phase change caused an interrupt (e.g. limit drop), pause here.
            if (gameManager.getInterruptedRound() != null) {
                return true;
            }

            boolean companySwitched = handleClosedOperatingCompany();
            if (!companySwitched) {
                playerManager.setCurrentPlayer(operatingCompany.value().getPresident());

                // Previously, this logic reset to INITIAL if no trains were bought.
                // This broke Voluntary Discards (1837) where a player discards TO buy a train.
                // We must ensure we stay in the BUY_TRAIN step.
                stepObject.set(GameDef.OrStep.BUY_TRAIN);

                // Original problematic code for reference:
                // if (trainsBoughtThisTurn.isEmpty()) {
                // setStep(GameDef.OrStep.INITIAL);
                // } else {
                // stepObject.set(GameDef.OrStep.BUY_TRAIN);
                // }
            }
        } else {
            // If more discards are needed, ensure we stay/enter the correct step
            setStep(GameDef.OrStep.DISCARD_TRAINS);
        }

        return true;
    }

    /**
     * Hook for subclasses (like 1835) to handle cases where the operating company
     * closes immediately after an action (e.g., Discard triggers Formation).
     * * @return true if the company was switched and the step was reset.
     */
    protected boolean handleClosedOperatingCompany() {
        return false; // Standard games do not switch companies inside discardTrain
    }

    /**
     * Hook for subclasses to add specific logic after a discard.
     * 
     * @return true if the subclass handled the flow and no further processing is
     *         needed.
     */
    protected boolean processGameSpecificDiscard(DiscardTrain action, boolean moreDiscards) {
        return false; // Default: do nothing, proceed to standard logic
    }

    // ... (lines of unchanged context code) ...

    public boolean checkForExcessTrains() {
        excessTrainCompanies = new HashMap<>();
        Player player;

        for (PublicCompany comp : operatingCompanies.view()) {
            int numTrains = comp.getPortfolioModel().getNumberOfTrains();
            int trainLimit = comp.getCurrentTrainLimit();

            if (numTrains > trainLimit) {
                player = comp.getPresident();
                if (player == null) {
                    continue;
                }

                if (!excessTrainCompanies.containsKey(player)) {
                    excessTrainCompanies.put(player, new ArrayList<>(2));
                }
                excessTrainCompanies.get(player).add(comp);
            }
        }

        boolean result = !excessTrainCompanies.isEmpty();
        return result;
    }

    protected void setTrainsToDiscard() {

        // 1. CRITICAL: Refresh the excess map immediately.
        // The logs showed this method running with stale data (SB=4) after a discard.
        // By calling this first, we ensure 'excessTrainCompanies' reflects the current
        // state (SB=3).
        if (!checkForExcessTrains()) {
            possibleActions.clear();
            // If the map is empty, we stop generating discard buttons entirely.
            return;
        }

        // 2. Clear previous buttons (standard cleanup)
        possibleActions.clear();

        // 1. Lock the turn
        doneAllowed.set(false);

        // 2. Clear previous buttons to prevent UI ghosting
        possibleActions.clear();

        List<Player> nextPlayers = getRoot().getPlayerManager().getNextPlayers(true);

        for (Player player : nextPlayers) {
            if (excessTrainCompanies.containsKey(player)) {

                getRoot().getPlayerManager().setCurrentPlayer(player);
                List<PublicCompany> comps = excessTrainCompanies.get(player);

                for (PublicCompany comp : comps) {
                    if (comp.getPortfolioModel().getNumberOfTrains() == 0)
                        continue;

                    generateGroupedDiscardActions(comp);

                    return;
                }
            }
        }
    }

    /*
     * =======================================
     * 3.3. PRIVATES (BUYING, SELLING, CLOSING)
     * =======================================
     */

// ... (lines of unchanged context code) ...
    public boolean buyPrivate(BuyPrivate action) {

        String errMsg = null;
        PublicCompany publicCompany = action.getCompany();
        String publicCompanyName = publicCompany.getId();
        PrivateCompany privateCompany = action.getPrivateCompany();
        String privateCompanyName = privateCompany.getId();
        int price = action.getPrice();
        Owner owner = null;
        Player player = null;
        int upperPrice;
        int lowerPrice;



        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            // Does private exist?
            if ((privateCompany = companyManager.getPrivateCompany(privateCompanyName)) == null) {
                errMsg = LocalText.getText("PrivateDoesNotExist",
                        privateCompanyName);

                break;
            }
            // Is private still open?
            if (privateCompany.isClosed()) {
                errMsg = LocalText.getText("PrivateIsAlreadyClosed",
                        privateCompanyName);

                break;
            }
            // Is private owned by a player?
            owner = privateCompany.getOwner();

            if (!(owner instanceof Player)) {
                errMsg = LocalText.getText("PrivateIsNotOwnedByAPlayer",
                        privateCompanyName);

                break;
            }
            player = (Player) owner;
            
boolean restrictPrivateTrade = GameOption.getAsBoolean(this, "RestrictPrivateTradingToSameOwner");
            
            if (restrictPrivateTrade && player != operatingCompany.value().getPresident()) {
                errMsg = "Private Trading restricted to same-owner only.";
                break;
            }

            upperPrice = privateCompany.getUpperPrice();
            lowerPrice = privateCompany.getLowerPrice();

            // Is private buying allowed?
            if (!isPrivateSellingAllowed()) {
                errMsg = LocalText.getText("PrivateBuyingIsNotAllowed");

                break;
            }

            // Price must be in the allowed range
            if (lowerPrice != PrivateCompany.NO_PRICE_LIMIT
                    && price < lowerPrice) {
                errMsg = LocalText.getText("PriceBelowLowerLimit",
                        Bank.format(this, price),
                        Bank.format(this, lowerPrice),
                        privateCompanyName);
                // --- START FIX ---
                log.warn("BUY_PRIVATE_TRACE: Failed - Price [{}] is below lower limit [{}].", price, lowerPrice);
                // --- END FIX ---
                break;
            }
            if (upperPrice != PrivateCompany.NO_PRICE_LIMIT
                    && price > upperPrice) {
                errMsg = LocalText.getText("PriceAboveUpperLimit",
                        Bank.format(this, price),
                        Bank.format(this, lowerPrice),
                        privateCompanyName);
                // --- START FIX ---
                log.warn("BUY_PRIVATE_TRACE: Failed - Price [{}] is above upper limit [{}].", price, upperPrice);
                // --- END FIX ---
                break;
            }
            // Does the company have the money?
            if (price > operatingCompany.value().getCash()) {
                errMsg = LocalText.getText("NotEnoughMoney", publicCompanyName,
                        Bank.format(this,
                                operatingCompany.value().getCash()),
                        Bank.format(this, price));
                // --- START FIX ---
                log.warn("BUY_PRIVATE_TRACE: Failed - Company has insufficient cash [{}].", operatingCompany.value().getCash());
                // --- END FIX ---
                break;
            }
            break;
        }
        


        if (errMsg != null) {
            if (owner != null) {
                DisplayBuffer.add(this, LocalText.getText(
                        "CannotBuyPrivateFromFor", publicCompanyName,
                        privateCompanyName, owner.getId(),
                        Bank.format(this, price), errMsg));
            } else {
                DisplayBuffer.add(this, LocalText.getText(
                        "CannotBuyPrivateFor", publicCompanyName,
                        privateCompanyName, Bank.format(this, price), errMsg));
            }
            return false;
        }

        // --- START FIX ---
        log.info("BUY_PRIVATE_TRACE: Validation PASSED. Executing state transfer via operatingCompany.buyPrivate().");
        // --- END FIX ---
        operatingCompany.value().buyPrivate(privateCompany, player, price);

        return true;

    }
// ... (rest of the method) ...


    protected boolean isPrivateSellingAllowed() {
        return Phase.getCurrent(this).isPrivateSellingAllowed();
    }

    protected int getPrivateMinimumPrice(PrivateCompany privComp) {
        int minPrice = privComp.getLowerPrice();
        if (minPrice == PrivateCompany.NO_PRICE_LIMIT) {
            minPrice = 0;
        }
        return minPrice;
    }

    protected int getPrivateMaximumPrice(PrivateCompany privComp) {
        int maxPrice = privComp.getUpperPrice();
        if (maxPrice == PrivateCompany.NO_PRICE_LIMIT) {
            maxPrice = operatingCompany.value().getCash();
        }
        return maxPrice;
    }

    protected boolean maySellPrivate(Player player) {
        return true;
    }

    protected boolean executeClosePrivate(ClosePrivate action) {

        PrivateCompany priv = action.getPrivateCompany();

        String errMsg = null;

        if (priv.isClosed())
            errMsg = LocalText.getText("PrivateAlreadyClosed", priv.getId());

        if (errMsg != null) {
            DisplayBuffer.add(this, errMsg);
            return false;
        }

        priv.setClosed();

        return true;
    }

    /*
     * =======================================
     * 3.4. DESTINATIONS
     * =======================================
     */

    /**
     * Stub for applying any follow-up actions when a company reaches it
     * destinations. Default version: no actions.
     *
     * @param companies Companies that have just been reported as having reached
     *                  their destinations.
     */
    protected void executeDestinationActions(List<PublicCompany> companies) {
    }

    public boolean reachDestinations(ReachDestinations action) {

        List<PublicCompany> destinedCompanies = action.getReachedCompanies();
        if (destinedCompanies != null) {
            for (PublicCompany company : destinedCompanies) {
                if (company.hasDestination()
                        && !company.hasReachedDestination()) {
                    company.setReachedDestination(true);
                    ReportBuffer.add(this, LocalText.getText(
                            "DestinationReached", company.getId(),
                            company.getDestinationHex().getId()));

                }
            }
            executeDestinationActions(destinedCompanies);
        }
        return true;
    }

    /**
     * This is currently a stub, as it is unclear if there is a common rule for
     * setting destination reaching options. See OperatingRound_1856 for a first
     * implementation of such rules.
     */
    protected void setDestinationActions() {

    }

    /*
     * =======================================
     * 3.5. LOANS
     * =======================================
     */

    /** Stub, to be overridden for games that have automatic loan taking (1826) */
    protected int canTakeLoans(PublicCompany company, int cashToRaise) {
        return 0;
    }

    protected boolean takeLoans(TakeLoans action) {

        String errMsg = validateTakeLoans(action);

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotTakeLoans",
                    action.getCompanyName(), action.getNumberTaken(),
                    Bank.format(this, action.getPrice()), errMsg));

            return false;
        }

        executeTakeLoans(action.getNumberTaken());

        return true;

    }

    protected String validateTakeLoans(TakeLoans action) {

        String errMsg = null;
        PublicCompany company = action.getCompany();
        String companyName = company.getId();
        int number = action.getNumberTaken();

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            // Is company operating?
            if (company != operatingCompany.value()) {
                errMsg = LocalText.getText("WrongCompany", companyName,
                        action.getCompanyName());
                break;
            }
            // Does company allow any loans?
            if (company.getMaxNumberOfLoans() == 0) {
                errMsg = LocalText.getText("LoansNotAllowed", companyName);
                break;
            }
            // Does the company exceed the maximum number of loans?
            if (company.getMaxNumberOfLoans() > 0
                    && company.getCurrentNumberOfLoans() + number > company.getMaxNumberOfLoans()) {
                errMsg = LocalText.getText("MoreLoansNotAllowed", companyName,
                        company.getMaxNumberOfLoans());
                break;
            }
            break;
        }

        return errMsg;
    }

    protected void executeTakeLoans(int number) {

        PublicCompany company = operatingCompany.value();
        int amount = calculateLoanAmount(number);
        company.addLoans(number);
        Currency.fromBank(amount, company);
        if (number == 1) {
            ReportBuffer.add(this, LocalText.getText("CompanyTakesLoan",
                    company.getId(),
                    bank.format(company.getValuePerLoan()),
                    bank.format(amount)));
        } else {
            ReportBuffer.add(this, LocalText.getText("CompanyTakesLoans",
                    company.getId(), number,
                    bank.format(company.getValuePerLoan()),
                    bank.format(amount)));
        }

        if (company.getMaxLoansPerRound() > 0) {
            int oldLoansThisRound = 0;
            if (loansThisRound == null) {
                loansThisRound = HashMapState.create(this, "loansThisRound");
            } else if (loansThisRound.containsKey(company)) {
                oldLoansThisRound = loansThisRound.get(company);
            }
            loansThisRound.put(company, oldLoansThisRound + number);
        }
    }

    protected boolean repayLoans(RepayLoans action) {

        String errMsg = validateRepayLoans(action);

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotRepayLoans",
                    action.getCompanyName(), action.getNumberRepaid(),
                    Bank.format(this, action.getPrice()), errMsg));

            return false;
        }

        int repayment = action.getNumberRepaid()
                * operatingCompany.value().getValuePerLoan();
        if (repayment > 0 && repayment > operatingCompany.value().getCash()) {
            // President must contribute
            int remainder = repayment - operatingCompany.value().getCash();
            Player president = operatingCompany.value().getPresident();
            int presCash = president.getCashValue();
            if (remainder > presCash) {
                // Start a share selling round
                int cashToBeRaisedByPresident = remainder - presCash;

                savedAction.set(action);
                gameManager.startShareSellingRound(
                        operatingCompany.value().getPresident(),
                        cashToBeRaisedByPresident, operatingCompany.value(),
                        false);
                return true;
            }
        }

        if (repayment > 0)
            executeRepayLoans(action);

        return true;
    }

    protected String validateRepayLoans(RepayLoans action) {

        String errMsg = null;
        // TODO add validation

        return errMsg;
    }

    protected void executeRepayLoans(RepayLoans action) {

        int number = action.getNumberRepaid();
        int payment;
        int remainder;

        operatingCompany.value().addLoans(-number);
        int amount = number * operatingCompany.value().getValuePerLoan();
        payment = Math.min(amount, operatingCompany.value().getCash());
        remainder = amount - payment;
        if (payment > 0) {
            String paymentText = Currency.toBank(operatingCompany.value(), payment);
            ReportBuffer.add(this, LocalText.getText(
                    "CompanyRepaysLoans",
                    operatingCompany.value().getId(),
                    paymentText,
                    bank.getCurrency().format(amount), // TODO: Do this nicer
                    number,
                    bank.getCurrency().format(
                            operatingCompany.value().getValuePerLoan()))); // TODO:
            // Do
            // this
            // nicer
        }
        if (remainder > 0) {
            Player president = operatingCompany.value().getPresident();
            if (president.getCashValue() >= remainder) {
                payment = remainder;
                String paymentText = Currency.toBank(president, payment);
                ReportBuffer.add(this, LocalText.getText(
                        "CompanyRepaysLoansWithPresCash",
                        operatingCompany.value().getId(),
                        paymentText,
                        bank.getCurrency().format(amount), // TODO: Do this
                        // nicer
                        number,
                        bank.getCurrency().format(
                                operatingCompany.value().getValuePerLoan()), // TODO:
                        // Do
                        // this
                        // nicer
                        president.getId()));
            }
        }
    }

    protected int calculateLoanAmount(int numberOfLoans) {
        return numberOfLoans * operatingCompany.value().getValuePerLoan();
    }

    // TODO UNUSED??
    // public void payLoanInterest () {
    // int amount = operatingCompany.value().getCurrentLoanValue()
    // * operatingCompany.value().getLoanInterestPct() / 100;
    //
    // MoneyModel.cashMove (operatingCompany.value(), bank, amount);
    // DisplayBuffer.add(this, LocalText.getText("CompanyPaysLoanInterest",
    // operatingCompany.value().getId(),
    // Currency.format(this, amount),
    // operatingCompany.value().getLoanInterestPct(),
    // operatingCompany.value().getCurrentNumberOfLoans(),
    // Currency.format(this, operatingCompany.value().getValuePerLoan())));
    // }

    /*
     * =======================================
     * 3.6. RIGHTS
     * =======================================
     */

    protected boolean buyRight(UseSpecialProperty action) {

        String errMsg = null;
        String rightName = "";
        String rightValue = "";
        SpecialRight right = null;
        int cost = 0;

        SpecialProperty sp = action.getSpecialProperty();

        while (true) {
            if (!(sp instanceof SpecialRight)) {
                errMsg = "Wrong right property class: " + sp.toString();
                break;
            }
            right = (SpecialRight) sp;
            rightName = right.getName();
            cost = right.getCost();

            if (cost > 0 && cost > operatingCompany.value().getCash()) {
                errMsg = LocalText.getText("NoMoney");
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotBuyRight",
                    action.getCompanyName(), rightName,
                    bank.getCurrency().format(cost), // TODO: Do this nicer
                    errMsg));

            return false;
        }

        operatingCompany.value().setRight(right);
        // TODO: Creates a zero cost transfer if cost == 0
        String costText;
        if (cost > 0) {
            costText = Currency.toBank(operatingCompany.value(), cost);
        } else {
            costText = "free";
        }

        ReportBuffer.add(this, LocalText.getText("BuysRight",
                operatingCompany.value().getId(), rightName, costText));

        sp.setExercised();

        return true;
    }

    /*
     * =======================================
     * 3.7. SHARE ACTIONS
     * =======================================
     */

    public boolean growCompany(GrowCompany action) {

        // TODO Validation to be added

        action.getCompany().grow();

        return true;
    }

    /*
     * =======================================
     * 4. LAYING TILES
     * =======================================
     */

    /**
     * Processes the LayTile action, updating the game state.
     *
     * @param action The LayTile action containing details of the tile, hex,
     *               orientation, and associated costs/properties.
     * @return true if the tile lay was successful, false otherwise.
     */
    // File: OperatingRound.java
    // Replace this entire method

    // File: OperatingRound.java
    // Replace this ENTIRE method

    // --- Hypothetical revenue calculation method (needs actual engine
    // implementation) ---
    /**
     * Calculates the current maximum potential revenue for a company synchronously.
     * NOTE: This is a placeholder. The actual implementation depends on how the
     * engine's RevenueAdapter or equivalent is structured and whether it
     * can be called efficiently and synchronously here.
     * 
     * @param company The company to calculate revenue for.
     * @return The calculated maximum potential revenue.
     */
    private int calculateCurrentPotentialRevenue(PublicCompany company) {
        // Check if the company can even run trains
        if (!companyHasRunningTrains(false)) { // Use false to avoid log spam if this method exists
            return 0;
        }

        // Use RevenueAdapter (assuming it can calculate potential without full run
        // state)
        RevenueAdapter ra = RevenueAdapter.createRevenueAdapter(getRoot(), company, Phase.getCurrent(this));
        if (ra == null) {
            return company.getLastRevenue(); // Fallback to last known revenue? Risky.
        }

        ra.initRevenueCalculator(true); // Assuming true for multigraph - check game config

        // Ideally, RevenueAdapter would have a method like:
        // return ra.getPotentialMaximumRevenue();
        // If not, calculateRevenue() might be the only option, but be wary of
        // performance impact.
        int potentialRevenue = ra.calculateRevenue();

        // 1. Get the special revenue (calculated by RunToCoalMineModifier)
        int specialRevenue = ra.getSpecialRevenue();

        // log.info("OR_TRACE: Revenue for " + company.getId()
        // + " | Base=" + potentialRevenue
        // + " | Special=" + specialRevenue);

        // 2. Add it to the total (This was commented out in your file!)
        potentialRevenue += specialRevenue;

        return potentialRevenue;
    }

    /**
     * Returns a formatted string for the current potential revenue.
     * Default behavior: Returns the formatted currency (e.g. "£ 50").
     * Subclasses can override this to show breakdowns (e.g. "20 + 30").
     */
    public String getRevenueDisplayString(PublicCompany company) {
        // Default: Just calculate the total and format it
        int total = calculateCurrentPotentialRevenue(company);
        return Bank.format(this, total);
    }

    /*
     * Extracted method, to be overridden for any extra cost.
     * Examples: SOH (river bridge), 1846 (generic lay cost), 1861 (second tile)
     */
    protected int tileLayCost(LayTile action) {

        SpecialTileLay stl = action.getSpecialProperty();
        int cost = 0;

        // 1. Calculate Standard Cost (Base Rails Logic)
        if (stl == null || !stl.isFree()) {
            cost = action.getChosenHex().getTileCost();
            if (stl != null) {
                cost = Math.max(0, cost - stl.getDiscount());
            }
        }

        // 2. Apply Game-Specific Overrides (e.g. 1837 Mountain Railway Waiver)
        // We pass the calculated standard cost to the hook. If the hook returns 0, it
        // overrides the cost.
        cost = getTileLayCost(action.getCompany(), action.getChosenHex(), cost);

        return cost;
    }

    public boolean layTileCorrection(LayTile action) {
        return layTileCorrection(action, false);
    }

    /**
     * Lay a tile, but suppress reporting.
     * Used to hide a behind-the-screen action to lay an invisible tile
     * that adds functionality to the visible tile.
     * 
     * @param action         A LayTileCorrection action.
     * @param suppressReport True if the action should not show up in the game
     *                       report.
     * @return True if successful.
     */
    public boolean layTileCorrection(LayTile action, boolean suppressReport) {

        Tile tile = action.getLaidTile();
        MapHex hex = action.getChosenHex();
        int orientation = action.getOrientation();

        String errMsg = null;
        // tiles have external id defined
        if (tile != null
                && tile != hex.getCurrentTile()
                && tile.getFreeCount() == 0) {
            errMsg = LocalText.getText("TileNotAvailable",
                    tile.toText());
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CorrectMapCannotLayTile",
                    tile.toText(),
                    hex.getId(),
                    errMsg));
            return false;
        }

        // lays tile
        hex.upgrade(action);

        if (!suppressReport) {
            String msg = LocalText.getText("CorrectMapLaysTileAt",
                    tile.toText(), hex.getId(), hex.getOrientationName(orientation));
            ReportBuffer.add(this, msg);
        }

        return true;
    }

    protected boolean validateNormalTileLay(Tile tile) {
        return checkNormalTileLay(tile, false);
    }

    protected void registerNormalTileLay(Tile tile) {
        checkNormalTileLay(tile, true);
    }

    protected boolean checkNormalTileLay(Tile tile, boolean update) {

        // Unspecified tile (e.g. 1889 D private, which is free on mountains)
        if (tile == null) {
            return !tileLaysPerColour.isEmpty();
        }

        String colour = tile.getColourText();
        Integer oldAllowedNumberObject = tileLaysPerColour.get(colour);

        if (oldAllowedNumberObject == null)
            return false;

        int oldAllowedNumber = oldAllowedNumberObject;
        if (oldAllowedNumber <= 0)
            return false;

        if (update)
            updateAllowedTileColours(colour, oldAllowedNumber);
        return true;
    }

    /*
     * We will assume that in all cases the following assertions hold: 1. If the
     * allowed number for the colour of the just laid tile reaches zero, all
     * normal tile lays have been consumed. 2. If any colour is laid, no
     * different colours may be laid. THIS MAY NOT BE TRUE FOR ALL GAMES!
     * EV sep 2020: Indeed it isn't true for SOH, 1846 and other games.
     * These games must override this method.
     */

    protected void updateAllowedTileColours(String colour, int oldAllowedNumber) {

        if (oldAllowedNumber <= 1) {
            tileLaysPerColour.clear();
        } else {
            List<String> coloursToRemove = new ArrayList<>();
            for (String key : tileLaysPerColour.viewKeySet()) {
                if (colour.equals(key)) {
                    tileLaysPerColour.put(key, oldAllowedNumber - 1);
                } else {
                    coloursToRemove.add(key);
                }
            }
            // Two-step removal to prevent ConcurrentModificationException.
            for (String key : coloursToRemove) {
                tileLaysPerColour.remove(key);
            }

        }
    }

    /**
     * Create a List of allowed normal tile lays (see LayTile class). This
     * method should be called only once per company turn in an OR: at the start
     * of the tile laying step.
     */
    protected void initNormalTileLays() {

        // duplicate the phase colours
        Map<String, Integer> newTileColours = new HashMap<>();
        for (String colour : Phase.getCurrent(this).getTileColours()) {
            int allowedNumber = operatingCompany.value().getNumberOfTileLays(colour);
            // Replace the null map value with the allowed number of lays
            newTileColours.put(colour, allowedNumber);
        }
        // store to state
        tileLaysPerColour.initFromMap(newTileColours);
    }

    protected List<LayTile> getNormalTileLays(boolean display) {

        /* Normal tile lays */
        List<LayTile> currentNormalTileLays = new ArrayList<>();

        // Check which colours can still be laid
        Map<String, Integer> remainingTileLaysPerColour = new HashMap<>();

        int lays;
        for (String colourName : tileLaysPerColour.viewKeySet()) {
            lays = tileLaysPerColour.get(colourName);
            if (lays != 0) {
                remainingTileLaysPerColour.put(colourName, lays);
            }
        }
        if (!remainingTileLaysPerColour.isEmpty()) {
            currentNormalTileLays.add(new LayTile(getRoot(), remainingTileLaysPerColour));
        }

        // NOTE: in a later stage tile lays will be specified per hex or set of
        // hexes.

        if (display) {
            int size = currentNormalTileLays.size();
            if (size == 0) {
            } else {
                for (LayTile tileLay : currentNormalTileLays) {
                }
            }
        }
        return currentNormalTileLays;
    }

    /**
     * Create a List of allowed special tile lays (see LayTile class). This
     * method should be called before each user action in the tile laying step.
     */
    protected List<LayTile> getSpecialTileLays(boolean forReal) {

        /* Special-property tile lays */
        List<LayTile> currentSpecialTileLays = new ArrayList<>();

        if (operatingCompany.value().canUseSpecialProperties()) {

            for (SpecialTileLay stl : getSpecialProperties(SpecialTileLay.class)) {

                LayTile layTile = new LayTile(stl);
                if (validateSpecialTileLay(layTile))
                    currentSpecialTileLays.add(layTile);
            }
            /*
             * for (SpecialMultiTileLay smtl :
             * getSpecialProperties(SpecialMultiTileLay.class)) {
             * 
             * LayTile layTile = new LayTile(smtl);
             * if (validateSpecialTileLay(layTile))
             * currentSpecialTileLays.add(layTile);
             * }
             */
        }

        if (forReal) {
            int size = currentSpecialTileLays.size();
            if (size == 0) {
            } else {
                for (LayTile tileLay : currentSpecialTileLays) {
                }
            }
        }

        return currentSpecialTileLays;
    }

    /**
     * Prevalidate a special tile lay.
     * <p>
     * During prevalidation, the action may
     * be updated (i.e. restricted). TODO
     * <p>
     * Note: The name of this method may
     * suggest that it can also be used for postvalidation (i.e. to validate the
     * action after the player has selected it). This is not yet the case, but
     * it is conceivable that this method can be extended to cover
     * postvalidation as well. Postvalidation is really a different process,
     * which in this context has not yet been considered in detail.
     *
     * @param layTile A LayTile object embedding a SpecialTileLay property. Any
     *                other LayTile objects are rejected. The object may be changed
     *                by this
     *                method.
     * @return TRUE if allowed.
     */
    protected boolean validateSpecialTileLay(LayTile layTile) {

        if (layTile == null)
            return false;

        SpecialTileLay stl = layTile.getSpecialProperty();

        if (!stl.isExtra()
                // If the special tile lay is not extra, it is only allowed if
                // normal tile lays are also (still) allowed
                && !checkNormalTileLay(stl.getTile(), false))
            return false;

        Tile tile = stl.getTile();

        // What colours can be laid in the current phase?
        List<String> phaseColours = Phase.getCurrent(this).getTileColours();

        // Which tile colour(s) are specified explicitly...
        String[] stlc = stl.getTileColours();
        if ((stlc == null || stlc.length == 0) && tile != null) {
            // ... or implicitly
            stlc = new String[] { tile.getColourText() };
        }

        // Which of the specified tile colours can really be laid now?
        List<String> layableColours;
        if (stlc == null) {
            layableColours = phaseColours;
        } else {
            layableColours = new ArrayList<>();
            for (String colour : stlc) {
                if (phaseColours.contains(colour))
                    layableColours.add(colour);
            }
            if (layableColours.isEmpty())
                return false;
        }

        // If any locations are specified, check if tile or colour(s) can be
        // laid there.
        Map<String, Integer> tc = new HashMap<>();
        List<MapHex> hexes = stl.getLocations();
        List<MapHex> remainingHexes = null;
        List<String> remainingColours = null;
        int cash = operatingCompany.value().getCash();

        if (hexes != null) {
            remainingHexes = new ArrayList<>();
            remainingColours = new ArrayList<>();
        }
        for (String colour : layableColours) {
            if (hexes != null) {
                for (MapHex hex : hexes) {
                    int cost = Math.max(0, hex.getTileCost() - stl.getDiscount());
                    // Check if the company can pay any costs (if not free)
                    if (!stl.isFree() && cash < cost)
                        continue;

                    // At least one hex does not have that colour yet
                    // TODO: Check if this can be rewritten in a simpler fashion
                    // using TileColours directly
                    if (hex.getCurrentTile().getColourNumber() + 1 == TileColour.valueOfIgnoreCase(
                            colour).getNumber()) {
                        tc.put(colour, 1);
                        remainingColours.add(colour);
                        remainingHexes.add(hex);
                    }
                }
            } else {
                tc.put(colour, 1);
            }
        }
        if (!tc.isEmpty())
            layTile.setTileColours(tc);

        if (hexes != null) {
            if (remainingHexes.isEmpty())
                return false;
            layTile.setLocations(remainingHexes);
        }

        return true;
    }

    protected boolean areTileLaysPossible() {
        return !tileLaysPerColour.isEmpty()
                || !getSpecialTileLays(false).isEmpty();
    }

    /**
     * Reports if a tile lay is allowed by a certain company on a certain hex
     * <p>
     * This method can be used both in restricting possible actions and in
     * validating submitted actions.
     * <p>
     * Currently, only a few standard checks
     * are included. This method can be extended to perform other generic
     * checks, such as if a route exists, and possibly in subclasses for
     * game-specific checks.
     *
     * @param company     The company laying a tile.
     * @param hex         The hex on which a tile is laid.
     * @param orientation The orientation in which the tile is laid (-1 is any).
     */
    public boolean isTileLayAllowed(PublicCompany company, MapHex hex,
            int orientation) {

        return gameSpecificTileLayAllowed(company, hex, orientation);
    }

    /**
     * Calculates the cost for a specific company to lay a tile on a hex.
     * 
     * @param company      The acting company.
     * @param hex          The target hex.
     * @param standardCost The standard terrain/action cost calculated by the Rails
     *                     engine.
     * @return The final cost (possibly adjusted by private company ownership, etc).
     */
    public int getTileLayCost(PublicCompany company, MapHex hex, int standardCost) {
        // Default behavior: The cost is exactly what the map says (mountains, rivers,
        // etc.)
        return standardCost;
    }

    protected boolean gameSpecificTileLayAllowed(PublicCompany company,
            MapHex hex, int orientation) {
        return !hex.isBlockedByPrivateCompany();
    }

    /*
     * =======================================
     * 5. TOKEN LAYING
     * 5.1. BASE TOKENS
     * =======================================
     */

    /**
     * Reports if a token lay is allowed by a certain company on a certain hex
     * and city
     * <p>
     * This method can be used both in restricting possible actions
     * and in validating submitted actions.
     * <p>
     * Currently, only a few standard
     * checks are included. This method can be extended to perform other generic
     * checks, such as if a route exists, and possibly in subclasses for
     * game-specific checks.
     *
     * @param company The company laying a tile.
     * @param hex     The hex on which a tile is laid.
     * @param stop    The number of the station/city on which the token is to be
     *                laid (0 if any or immaterial).
     */
    protected boolean isTokenLayAllowed(PublicCompany company, MapHex hex, Stop stop) {
        return !hex.isBlockedForTokenLays(company, stop);
    }

    protected void setNormalTokenLays() {
        PublicCompany company = operatingCompany.value();
        /* Normal token lays */
        currentNormalTokenLays.clear();

        /* For now, we allow one token of the currently operating company */
        // Old line: if (company.getNumberOfFreeBaseTokens() > 0
        if (company.getNumberOfFreeBaseTokens() > 0 && !normalTokenLaidThisTurn.value())
        // BASE_COSTrgba(6, 7, 6, 1)UENCE
        // && company.getBaseTokenLayCost(null) <= company.getCash()
        {
            PublicCompany.BaseCostMethod baseCostMethod = company.getBaseTokenLayCostMethod();
            switch (baseCostMethod) {
                case SEQUENCE:
                case HEX_DISTANCE:
                    // Check if the company can afford *at least one* token
                    // We check the cheapest possible token cost (which may be 0 for a home lay)
                    int minCost = 0;
                    Set<Integer> costs = company.getBaseTokenLayCosts(); // This method gets all possible costs
                    if (costs != null && !costs.isEmpty()) {
                        minCost = Collections.min(costs);
                    }

                    // If the company has no money, and the cheapest token still costs money, don't
                    // add the
                    // action.
                    if (company.getCash() >= minCost) {
                        currentNormalTokenLays.add(new LayBaseToken(getRoot(), (List<MapHex>) null));
                    }
                    break;
                case ROUTE_DISTANCE:
                    company.setTokenableStops();
                    // This logic is from the original file
                    // It creates a specific action for each reachable stop.
                    for (Stop stop : company.tokenableStops.keySet()) {
                        int cost = company.getBaseTokenLayCostOnStop(stop);
                        if (cost <= company.getCash()) { // Check affordability
                            currentNormalTokenLays.add(
                                    new LayBaseToken(getRoot(), List.of(stop.getHex()), cost));
                        }
                    }
                    break;
            } //
        }
    }

    /**
     * Create a List of allowed special token lays (see LayToken class). This
     * method should be called before each user action in the base token laying
     * step. TODO: Token preparation is practically identical to Tile
     * preparation, perhaps the two can be merged to one generic procedure.
     */
    protected void setSpecialTokenLays() {

        /* Special-property base token lays */
        currentSpecialTokenLays.clear();

        PublicCompany company = operatingCompany.value();
        if (!company.canUseSpecialProperties())
            return;
        // Check if the company still has tokens
        if (company.getNumberOfFreeBaseTokens() == 0)
            return;

        for (SpecialBaseTokenLay stl : getSpecialProperties(SpecialBaseTokenLay.class)) {
            // If the special tile lay is not extra, it is only allowed if
            // normal tile lays are also (still) allowed
            if (stl.isExtra() || !currentNormalTokenLays.isEmpty()) {

                // If this STL is location specific, check if there
                // isn't already a token of this company or if it is blocked
                List<MapHex> locations = stl.getLocations();
                if (locations != null && !locations.isEmpty()) {
                    boolean canLay = false;
                    for (MapHex location : locations) {
                        if (location.hasTokenOfCompany(company)) {
                            continue;
                        }
                        for (Stop stop : location.getStops()) {
                            canLay = !location.isBlockedForTokenLays(company, stop);
                        }
                    }
                    if (!canLay)
                        continue;
                }
                currentSpecialTokenLays.add(new LayBaseToken(getRoot(), stl));
            }
        }
    }

    /*
     * =======================================
     * 5.2. BONUS TOKENS
     * =======================================
     */

    public boolean layBonusToken(LayBonusToken action) {

        String errMsg = null;
        int cost = 0; // currently costs are always zero
        SpecialBonusTokenLay stl = null;

        MapHex hex = action.getChosenHex();
        BonusToken token = action.getToken();

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            MapHex location = action.getChosenHex();
            if (location != hex) {
                errMsg = LocalText.getText("TokenLayingHexMismatch",
                        hex.getId(), location.getId());
                break;
            }
            stl = action.getSpecialProperty();
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this,
                    LocalText.getText("CannotLayBonusTokenOn", token.getId(),
                            hex.getId(), Bank.format(this, cost), errMsg));
            return false;
        }

        /* End of validation, start of execution */

        if (hex.layBonusToken(token, getRoot().getPhaseManager())) {
            /* TODO: the false return value must be impossible. */

            operatingCompany.value().addBonus(
                    new Bonus(operatingCompany.value(), token.getId(),
                            token.getValue(), Collections.singletonList(hex)));
            token.setUser(operatingCompany.value());

            ReportBuffer.add(this, LocalText.getText("LaysBonusTokenOn",
                    operatingCompany.value().getId(), token.getId(),
                    Bank.format(this, token.getValue()), hex.getId()));

            // Was a special property used?
            if (stl != null) {
                stl.setExercised();
                // FIXME: currentSpecialTokenLays can't actually contain a LayBonusToken
                // currentSpecialTokenLays.remove(action);
            }
            // Copied from layBaseToken. Does this help??
            if (!canLayAnyTokens(false)) {
                nextStep();
            }

        }

        return true;
    }

    public boolean buyBonusToken(BuyBonusToken action) {

        String errMsg = null;
        int cost;
        SellBonusToken sbt;
        MoneyOwner seller;

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            sbt = action.getSpecialProperty();
            cost = sbt.getPrice();
            Owner from = sbt.getSeller();
            // TODO: Remove redundancy use a generalized check
            if (from instanceof BankPortfolio) {
                seller = bank;
            } else {
                seller = (MoneyOwner) from;
            }

            // Does the company have the money?
            if (cost > operatingCompany.value().getCash()) {
                errMsg = LocalText.getText("NotEnoughMoney",
                        operatingCompany.value().getId(), Bank.format(
                                this,
                                operatingCompany.value().getCash()),
                        Bank.format(this, cost));
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotBuyBonusToken",
                    operatingCompany.value().getId(), sbt.getId(),
                    seller.getId(), bank.getCurrency().format(cost), // TODO: Do
                    // this
                    // nicer
                    errMsg));
            return false;
        }

        /* End of validation, start of execution */

        // TODO: Is text of cost used below?
        Currency.wire(operatingCompany.value(), cost, seller);

        Bonus bonus = new Bonus(operatingCompany.value(), sbt.getId(),
                sbt.getValue(), sbt.getLocations(), sbt.allowOneTrainOnly());
        operatingCompany.value().addBonus(bonus);

        ReportBuffer.add(this, LocalText.getText("BuysBonusTokenFrom",
                operatingCompany.value().getId(), sbt.getName(),
                bank.getCurrency().format(sbt.getValue()), // TODO: Do this
                // nicer
                seller.getId(), bank.getCurrency().format(sbt.getPrice()))); // TODO:
        // Do
        // this
        // nicer

        sbt.setExercised();

        if (getStep() == GameDef.OrStep.LAY_TOKEN && !canLayAnyTokens(false)) {
            nextStep();
        }

        return true;
    }

    /**
     * TODO Should be merged with setSpecialTokenLays() in the future.
     * EV: No, that one is for base tokens only. For refactoring,
     * it may be more useful to look at the "can use special properties" code from
     * line 560.
     *
     * Assumptions: 1. Bonus tokens can be laid anytime during the OR. 2. Bonus
     * token laying is always extra.
     * TODO These assumptions should be made configurable conditions.
     */
    protected void setBonusTokenLays() {

        for (SpecialBonusTokenLay stl : getSpecialProperties(SpecialBonusTokenLay.class)) {
            if (stl.isUsableDuringOR(getStep())) {
                possibleActions.add(new LayBonusToken(getRoot(), stl, stl.getToken()));
            }
        }
    }

    /*
     * =======================================
     * 5.3. ALL LAYABLE TOKENS
     * =======================================
     */

    protected boolean canLayAnyTokens(boolean resetTokenLays) {
        if (resetTokenLays)
            setNormalTokenLays();
        if (!currentNormalTokenLays.isEmpty())
            return true;
        if (resetTokenLays)
            setSpecialTokenLays();
        if (!currentSpecialTokenLays.isEmpty())
            return true;
        if (!getSpecialProperties(SpecialBonusTokenLay.class).isEmpty())
            return true;
        return false;
    }

    /*
     * =======================================
     * 6. REVENUE AND DIVIDEND
     * 6.1. VALIDATE
     * =======================================
     */

    public boolean setRevenueAndDividend(SetDividend action) {

        String errMsg = validateSetRevenueAndDividend(action);

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotProcessRevenue",
                    Bank.format(this, action.getActualRevenue()),
                    action.getCompanyName(), errMsg));
            return false;
        }

        ReportBuffer.add(this, LocalText.getText("CompanyRevenue",
                action.getCompanyName(),
                Bank.format(this, action.getActualRevenue())));

        int remainingAmount = checkForDeductions(action);
        if (remainingAmount < 0) {
            // A share selling round will be run to raise cash to pay debts
            return true;
        }

        executeSetRevenueAndDividend(action);

        return true;

    }

    /*
     * =======================================
     * 6.2. EXECUTE REVENUE AND DIVIDEND
     * =======================================
     */

    /**
     * Validate the SetRevenue action, default version
     * 
     * @param action The completed SetRevenue action
     * @return True if valid
     */
    protected String validateSetRevenueAndDividend(SetDividend action) {
        return validateSetRevenueAndDividend(action,
                action.getRevenueAllocation() != SetDividend.NO_ROUTE);
    }

    /**
     * Validate the SetRevenue action, with option to bypass the allocation check.
     * This is needed in reloading a saved file, to accept the allocation change
     * that is needed in 18Scan to process the Minor company default revenue of K10.
     * 
     * @param action          The completed SetRevenue action
     * @param checkAllocation False if the allocation check must be bypassed (18Scan
     *                        only).
     * @return True if valid
     */
    protected String validateSetRevenueAndDividend(SetDividend action, boolean checkAllocation) {

        String errMsg = null;
        PublicCompany company;
        String companyName;
        int amount = 0;
        int revenueAllocation = -1;

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            // Must be correct company.
            company = action.getCompany();
            companyName = company.getId();
            if (company != operatingCompany.value()) {
                errMsg = LocalText.getText("WrongCompany", companyName,
                        operatingCompany.value().getId());
                break;
            }
            // Must be correct step
            if (getStep() != GameDef.OrStep.CALC_REVENUE) {
                errMsg = LocalText.getText("WrongActionNoRevenue");
                break;
            }

            // Amount must be non-negative multiple of 10
            amount = action.getActualRevenue();
            if (amount < 0) {
                errMsg = LocalText.getText("NegativeAmountNotAllowed",
                        String.valueOf(amount));
                break;
            }
            if (amount % 10 != 0) {
                errMsg = LocalText.getText("AmountMustBeMultipleOf10",
                        String.valueOf(amount));
                break;
            }

            // Check chosen revenue distribution
            revenueAllocation = action.getRevenueAllocation();
            if (amount > 0) {
                // Check the allocation type index (see SetDividend for values)
                if (revenueAllocation < 0
                        || revenueAllocation >= SetDividend.NUM_OPTIONS) {
                    errMsg = LocalText.getText("InvalidAllocationTypeIndex",
                            String.valueOf(revenueAllocation));
                    break;
                }

                // Validate the chosen allocation type
                if (checkAllocation) {
                    int[] allowedAllocations = ((SetDividend) selectedAction).getAllowedAllocations();
                    boolean valid = false;
                    for (int aa : allowedAllocations) {
                        if (revenueAllocation == aa) {
                            valid = true;
                            break;
                        }
                    }
                    if (!valid) {
                        errMsg = LocalText.getText(SetDividend.getAllocationNameKey(revenueAllocation));
                        break;
                    }
                }
            } else if (revenueAllocation != SetDividend.NO_ROUTE) {
                // If there is no revenue, use withhold.
                action.setRevenueAllocation(SetDividend.WITHHOLD);
            }

            if (amount == 0
                    && operatingCompany.value().getNumberOfTrains() == 0) {
                DisplayBuffer.add(this, LocalText.getText(
                        "RevenueWithNoTrains",
                        operatingCompany.value().getId(), Bank.format(this, 0)));
            }

            break;
        }

        return errMsg;
    }

    protected void executeSetRevenueAndDividend(SetDividend action) {
        executeSetRevenueAndDividend(action, null);
    }

    protected void executeSetRevenueAndDividend(SetDividend action, String report) {

        int earnings = action.getActualRevenue();
        int specialRevenue = action.getActualCompanyTreasuryRevenue();

        PublicCompany company = operatingCompany.value();

        // Sometimes there still is a payout; if so, the action will be updated
        // TODO: no need to comment this out? See 18Scan.
        // if (earnings == 0) action = checkZeroRevenue(action);

        int revenueAllocation = action.getRevenueAllocation();

        company.setLastRevenue(earnings);
        company.setLastRevenueAllocation(revenueAllocation);

        // Pay any debts from treasury, revenue and/or president's cash
        // The remaining dividend may be less that the original income
        earnings = executeDeductions(action);

        // Assign any income that goes to the company.
        // The dividend may be changed!
        int dividend = processSpecialRevenue(earnings, specialRevenue);
        company.setLastRevenue(earnings);

        if (dividend == 0) {

            if (report == null)
                report = LocalText.getText(
                        "CompanyDoesNotPayDividend",
                        company.getId());
            ReportBuffer.add(this, report);
            withhold(dividend);

        } else if (revenueAllocation == SetDividend.PAYOUT) {

            if (report == null)
                report = LocalText.getText(
                        "CompanyPaysOutFull",
                        company.getId(),
                        Bank.format(this, dividend));
            ReportBuffer.add(this, report);
            payout(dividend);

        } else if (revenueAllocation == SetDividend.SPLIT) {

            if (report == null)
                report = LocalText.getText(
                        "CompanySplits",
                        company.getId(),
                        Bank.format(this, dividend));
            ReportBuffer.add(this, report);
            splitRevenue(dividend);

        } else if (revenueAllocation == SetDividend.WITHHOLD) {
            if (report == null)
                report = LocalText.getText(
                        "CompanyWithholds",
                        company.getId(),
                        Bank.format(this, dividend));
            ReportBuffer.add(this, report);
            withhold(dividend);
        }

        if (company.hasBankLoan()) {
            int repayment = Math.min(company.getBankLoan(), action.getActualRevenue());
            Currency.toBank(company, repayment);
            company.repayBankLoan(repayment);
            ReportBuffer.add(this, LocalText.getText(
                    "CompanyRepaysBankLoan", company,
                    Bank.format(this, repayment),
                    Bank.format(this, company.getBankLoan())));
        }

        // Rust any obsolete trains
        company.getPortfolioModel().rustObsoleteTrains();

        // We have done the payout step, so continue from there
        nextStep(GameDef.OrStep.PAYOUT);
    }

    /**
     * Process any special revenue, adapting the dividend as required.
     * Default version: dividend = earnings.
     * To be overridden if any special revenue must be processed.
     * 
     * @param earnings       The total income from train runs.
     * @param specialRevenue Any income that needs special processing.
     * @return The resulting dividend (default: equal to the earnings).
     */
    protected int processSpecialRevenue(int earnings, int specialRevenue) {
        return earnings;
    }

    /*
     * =======================================
     * 6.3. EARNINGS DISTRIBUTION
     * =======================================
     */

    /**
     * Distribute the dividend amongst the shareholders.
     *
     * @param amount The dividend to be payed out
     */
    public void payout(int amount) {

        if (amount == 0)
            return;

        int part;
        int shares;

        Map<MoneyOwner, Integer> sharesPerRecipient = countSharesPerRecipient();

        // Calculate, round up, report and add the cash

        // Define a reproducible sequence for reporting (otherwise tests will fail)
        Set<MoneyOwner> recipientSet = sharesPerRecipient.keySet();
        for (MoneyOwner recipient : SequenceUtil.sortCashHolders(recipientSet)) {
            if (recipient instanceof Bank)
                continue;
            shares = (sharesPerRecipient.get(recipient));
            if (shares == 0)
                continue;

            double pricePerShare = amount * operatingCompany.value().getShareUnit() / 100.0;
            part = roundShareholderPayout(pricePerShare, shares, Rounding.UP, Multiplication.BEFORE_ROUNDING);

            String partText = Currency.fromBank(part, recipient);
            ReportBuffer.add(this, LocalText.getText("Payout",
                    recipient.getId(), partText, shares,
                    operatingCompany.value().getShareUnit()));
        }

        // Move the token
        operatingCompany.value().payout(amount);
    }

    protected Map<MoneyOwner, Integer> countSharesPerRecipient() {

        Map<MoneyOwner, Integer> sharesPerRecipient = new HashMap<>();

        // First count the shares per recipient
        for (PublicCertificate cert : operatingCompany.value().getCertificates()) {
            MoneyOwner recipient = getBeneficiary(cert);
            if (recipient instanceof Player && gameManager.isCertificateBlocked(cert)) { // 1837
                ReportBuffer.add(this, LocalText.getText("PayoutBlocked",
                        recipient,
                        cert.getShare(),
                        operatingCompany.value()));
            } else if (!sharesPerRecipient.containsKey(recipient)) {
                sharesPerRecipient.put(recipient, cert.getShares());
            } else {
                sharesPerRecipient.put(recipient,
                        sharesPerRecipient.get(recipient) + cert.getShares());
            }
        }
        return sharesPerRecipient;
    }

    /**
     * Who gets the per-share revenue?
     */
    protected MoneyOwner getBeneficiary(PublicCertificate cert) {
        MoneyOwner beneficiary;

        // Special cases apply if the holder is the IPO or the Pool
        if (operatingCompany.value().paysOutToTreasury(cert)) {
            beneficiary = operatingCompany.value();
        } else if (cert.getOwner() instanceof MoneyOwner) {
            beneficiary = (MoneyOwner) cert.getOwner();
        } else { // TODO: check if this is a correct assumption that otherwise
            // the money goes to the bank
            beneficiary = bank;
        }
        return beneficiary;
    }

    /**
     * Withhold a given amount of revenue (and store it).
     * Note: the amount can be zero if the company had no route.
     *
     * @param amount The revenue amount.
     */
    public void withhold(int amount) {

        PublicCompany company = operatingCompany.value();

        // Payout revenue to company
        if (amount > 0)
            Currency.fromBank(amount, company);

        // Move the token
        company.withhold(amount);

        if (!company.hasStockPrice())
            return;

        // Check if company has entered a closing area
        StockSpace newSpace = company.getCurrentSpace();
        if (newSpace.closesCompany() && company.canClose()) {
            company.setClosed();
            ReportBuffer.add(this, LocalText.getText("CompanyClosesAt",
                    company.getId(), newSpace.getId()));
            finishTurn();
        }
    }

    /**
     * Split a dividend, default version.
     *
     * NOTE: to set different payout or rounding rules, don't override
     * this method but the method calculateCompanyIncomeFromSplit(),
     * which is called in the body of this method.
     *
     * @param amount The revenue to be split
     */
    public void splitRevenue(int amount) {

        if (amount > 0) {
            // Withhold half of it
            int numberOfShares = operatingCompany.value().getActiveShareCount();

            int withheld = calculateCompanyIncomeFromSplit(amount);

            String withheldText = Currency.fromBank(withheld, operatingCompany.value());

            ReportBuffer.add(this, LocalText.getText("RECEIVES",
                    operatingCompany.value().getId(), withheldText));

            // Payout the remainder
            int payed = amount - withheld;
            payout(payed);
        }

    }

    /*
     * =======================================
     * 6.4. ROUNDING
     * =======================================
     */

    /* Rounding enums */
    protected enum Rounding {
        UP, DOWN
    }

    protected enum ToMultipleOf {
        ONE(1),
        TEN(10);

        private int value;

        ToMultipleOf(int value) {
            this.value = value;
        }
    }

    protected enum Multiplication {
        BEFORE_ROUNDING, AFTER_ROUNDING
    }

    /**
     * Rounds a split revenue or payout up or down, depending on
     * - the value of the rounding parameter (up or down),
     * - the nearest multiple to which the split revenue must be rounded.
     * Normally one, but 18EU and 1870 have 10.
     *
     * This method is not meant to be overridden, hence the 'final' modifier.
     * To properly round a split revenue or a shareholder payout,
     * use the below overridable methods calculateCompanyIncomeFromSplit()
     * or calculateCompanyIncomeFromSplit(), which can be overridden to
     * apply different rules than de default ones used here.
     *
     * @param amount     A double-precision value to be rounded as required
     * @param rounding   Up or down
     * @param multipleOf The multiple to which the amount must be rounded to (e.g.
     *                   10 in 18EU majors)
     * @return The integer rounding result
     */
    public final int roundIncome(double amount, Rounding rounding, ToMultipleOf multipleOf) {
        // The bitwise xor works as a logical xor on booleans.
        int result;
        int multiple = multipleOf.value;
        if (rounding == Rounding.DOWN) {
            // Round down
            result = multiple * ((int) (amount / multiple + 0.01));
        } else {
            // Round up
            result = multiple * ((int) (amount / multiple + 0.51));
        }

        return result;
    }

    /** To set the shareholder payout before rounding */
    protected final int roundShareholderPayout(double payoutPerShare, int shares,
            Rounding rounding, Multiplication beforeOrAfter) {
        int result;
        if (beforeOrAfter == Multiplication.BEFORE_ROUNDING) {
            // First multiply, then round
            result = roundIncome(shares * payoutPerShare, rounding, ToMultipleOf.ONE);
        } else {
            // First round, then multiply
            result = shares * roundIncome(payoutPerShare, rounding, ToMultipleOf.ONE);
        }

        return result;
    }

    /**
     * Default version for calculating the company part of
     * a revenue amount being split.
     *
     * This method should be overrirden in games where a different rule applies.
     * NOTE: these parameters should become configurable
     *
     * @param revenue The revenue amount to be split.
     * @return The part that goes directly to the company treasury.
     *         (the difference is to be payed out to the shareholders).
     *
     */
    protected int calculateCompanyIncomeFromSplit(int revenue) {
        return roundIncome(0.5 * revenue, Rounding.DOWN, ToMultipleOf.ONE);
    }

    /**
     * Default version of calculating a shareholder's part
     * of a dividend to be payed out, rounded up.
     *
     * This method should be overridden in games where a different rule applies.
     *
     * @param payoutPerShare The unrounded revenue amount to be paid per share.
     * @param numberOfShares the number of shares held buy the shareholder
     * @return The rounded revenue amount to be paid to the shareholder
     *
     */
    protected int calculateShareholderPayout(double payoutPerShare, int numberOfShares) {
        return roundShareholderPayout(payoutPerShare, numberOfShares,
                Rounding.UP, Multiplication.BEFORE_ROUNDING);
    }

    /*
     * =======================================
     * 6.5. DEDUCTIONS
     * =======================================
     */

    /**
     * Default version, to be overridden if need be
     * NOT USED - so far. See executeDeductions()
     */
    protected int checkForDeductions(SetDividend action) {
        return action.getActualRevenue();
    }

    protected int calculateLoanInterest(PublicCompany company) {

        if (company.canLoan()) {
            return company.getCurrentNumberOfLoans()
                    * company.getValuePerLoan()
                    * company.getLoanInterestPct() / 100;
        } else {
            return 0;
        }
    }

    /**
     * Default version, to be overridden if need be
     */
    protected int executeDeductions(SetDividend action) {
        return action.getActualRevenue();
    }

    protected boolean executeOperatingCost(OperatingCost action) {

        String companyName = action.getCompanyName();
        OperatingCost.OCType typeOC = action.getOCType();

        int amount = action.getAmount();

        String errMsg = null;

        while (true) {
            // Must be correct company.
            if (!companyName.equals(operatingCompany.value().getId())) {
                errMsg = LocalText.getText("WrongCompany", companyName,
                        operatingCompany.value().getId());
                break;
            }
            // amount is available
            if ((amount + operatingCompany.value().getCash()) < 0) {
                errMsg = LocalText.getText("NotEnoughMoney", companyName,
                        Bank.format(this,
                                operatingCompany.value().getCash()),
                        Bank.format(this, amount));
                break;
            }
            if (typeOC == OperatingCost.OCType.LAY_BASE_TOKEN
                    && operatingCompany.value().getNumberOfFreeBaseTokens() == 0) {
                errMsg = LocalText.getText("HasNoTokensLeft", companyName);
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this,
                    LocalText.getText("OCExecutionError", companyName, errMsg));
            return false;
        }

        String cashText = null;
        if (amount > 0) {
            // positive amounts: remove cash from cashholder
            cashText = Currency.toBank(operatingCompany.value(), amount);
        } else if (amount < 0) {
            // negative amounts: add cash to cashholder
            cashText = Currency.fromBank(-amount, operatingCompany.value());
        }

        if (typeOC == OperatingCost.OCType.LAY_TILE) {
            operatingCompany.value().layTilenNoMapMode(amount);
            ReportBuffer.add(this, LocalText.getText("OCLayTileExecuted",
                    operatingCompany.value().getId(), cashText));
        }
        if (typeOC == OperatingCost.OCType.LAY_BASE_TOKEN) {
            // move token to Bank
            BaseToken token = operatingCompany.value().getNextBaseToken();
            if (token == null) {
                return false;
            } else {
                // FIXME: Check where to lay the base tokens in NoMapMode
                // (bank.getUnavailable().addBonusToken(token));
            }
            operatingCompany.value().layBaseTokenInNoMapMode(amount);
            ReportBuffer.add(this, LocalText.getText("OCLayBaseTokenExecuted",
                    operatingCompany.value().getId(), cashText));
        }

        return true;
    }

    /*
     * =======================================
     * 6.6. PREPARE ACTION
     * =======================================
     */

    // We are modifying OperatingRound.java to enforce automatic revenue
    // calculation.

    // ... (lines of unchanged context code) ...
    /**
     * Prepares the SetDividend action for the current company.
     * This now includes a forced revenue recalculation to ensure accuracy,
     * especially after rapid state changes like tile lays followed by skips.
     */
    protected void prepareRevenueAndDividendAction() {
        // Check if the company can potentially earn revenue (e.g., has running trains)
        if (companyHasRunningTrains(false)) { // Using false to minimize log spam from the check
            int[] allowedRevenueActions;
            // Determine allowed allocations (PAYOUT, SPLIT, WITHHOLD) based on company
            // type/rules
            if (operatingCompany.value().isSplitAlways()) {
                allowedRevenueActions = new int[] { SetDividend.SPLIT };
            } else if (operatingCompany.value().isSplitAllowed()) {
                allowedRevenueActions = new int[] { SetDividend.PAYOUT, SetDividend.SPLIT, SetDividend.WITHHOLD };
            } else {
                allowedRevenueActions = new int[] { SetDividend.PAYOUT, SetDividend.WITHHOLD };
            }

            int currentPresetRevenue = 0;
            try {
                // Call the same synchronous calculation method used in layTile
                currentPresetRevenue = calculateCurrentPotentialRevenue(operatingCompany.value());

            } catch (Exception e) {
                currentPresetRevenue = operatingCompany.value().getLastRevenue(); // Existing fallback
            }

            // Enforce Calculated Revenue ---
            // Create the SetDividend action using the freshly calculated revenue
            // mayUserSetRevenue = false (User cannot edit)
            SetDividend action = new SetDividend(getRoot(),
                    currentPresetRevenue,
                    false, // Locked
                    allowedRevenueActions);

            // Pre-fill the actual revenue so the action is ready to fire immediately
            action.setActualRevenue(currentPresetRevenue);

            possibleActions.add(action);
        }
        // else: No running trains, so no SetDividend action is added.
    }

    protected void prepareNoMapActions() {

        // LayTile Actions
        for (Integer tc : mapManager.getPossibleTileCosts()) {
            if (tc <= operatingCompany.value().getCash())
                possibleActions.add(new OperatingCost(getRoot(),
                        OperatingCost.OCType.LAY_TILE, tc, false));
        }

        // LayBaseToken Actions
        if (operatingCompany.value().getNumberOfFreeBaseTokens() != 0) {
            Set<Integer> baseCosts = operatingCompany.value().getBaseTokenLayCosts();

            // change to set to allow for identity and ordering
            Set<Integer> costsSet = new TreeSet<>();
            for (int cost : baseCosts)
                if (!(cost == 0 && baseCosts.size() != 1)) // fix for sequence
                    // based home token
                    costsSet.add(cost);

            // SpecialBaseTokenLay Actions - workaround for a better handling of
            // those later
            for (SpecialBaseTokenLay stl : getSpecialProperties(SpecialBaseTokenLay.class)) {
                if (stl.isFree()) {
                    costsSet.add(0);
                }
            }

            for (int cost : costsSet) {
                if (cost <= operatingCompany.value().getCash()) // distance
                    // method
                    // returns home
                    // base, but in
                    // sequence
                    // costsSet can
                    // be zero
                    possibleActions.add(new OperatingCost(getRoot(),
                            OperatingCost.OCType.LAY_BASE_TOKEN, cost, false));
            }
        }

    }

    /*
     * =======================================
     * 7. TRAIN PURCHASING STEP
     * 7.1 BUY TRAIN EXECUTION
     * =======================================
     */

    // In OperatingRound.java

    /**
     * Executes the BuyTrain action, transferring the train and cash,
     * and handling phase changes or emergency conditions.
     *
     * @param action The BuyTrain action containing details about the train, seller,
     *               price, etc.
     * @return True if the action was successfully processed (or if a share selling
     *         round was initiated), false if validation failed.
     */
    // In OperatingRound.java

    /*
     * =======================================
     * 7. TRAIN PURCHASING STEP
     * 7.1 BUY TRAIN EXECUTION
     * =======================================
     */

    /**
     * Executes the BuyTrain action, transferring the train and cash,
     * and handling phase changes or emergency conditions.
     *
     * @param action The BuyTrain action containing details about the train, seller,
     *               price, etc.
     * @return True if the action was successfully processed (or if a share selling
     *         round was initiated), false if validation failed.
     */
    public boolean buyTrain(BuyTrain action) {

        Train train = action.getTrain();
        PublicCompany company = action.getCompany();
        String companyName = company.getId();
        Train exchangedTrain = action.getExchangedTrain();
        SpecialTrainBuy stb = null;

        String errMsg = null;
        boolean presidentMustSellShares = false;
        // boolean companyMustSellShares = false;
        int pricePaid = action.getPricePaid();
        int companyCash = company.getCash();
        int loansTaken = 0;

        int presidentCash;
        int actualPresidentCash = 0;
        int cashToBeRaisedByPresident = 0;
        Player currentPlayer = operatingCompany.value().getPresident();
        int playerCash = currentPlayer != null ? currentPlayer.getCash() : 0;

        // --- Validation Phase ---
        while (true) {

            // Relax validation: If we are resuming a saved action (e.g. returning from
            // emergency share selling), we allow the purchase even if the step logic
            // (like PFR triggers) has shifted the state temporarily.
            if (getStep() != GameDef.OrStep.BUY_TRAIN && pendingTrainName.value() == null
                    && savedAction.value() == null) {

                errMsg = LocalText.getText("WrongActionNoTrainBuyingCost");
                break;
            }

            if (train == null) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            if (!company.mayBuyTrainType(train)) {
                errMsg = LocalText.getText("MayNotBuyTrain",
                        company.getId(), train.getType()); // Corrected to use company ID
                break; // ak
            }

            // The relationship between fixedCost and mode is explained
            // in the Javadoc of the Mode enum in the BuyTrain class.
            int fixedPrice = action.getFixedCost();
            BuyTrain.Mode mode = action.getFixedCostMode();

            // *** CRITICAL MODIFICATION: Tolerating pricePaid=0 for FIXED mode (Engine
            boolean validPrice = (mode == BuyTrain.Mode.FREE && pricePaid >= 0) // <--- THIS IS THE FIX
                    || (mode == BuyTrain.Mode.FIXED && (pricePaid == fixedPrice || (pricePaid == 0 && fixedPrice > 0))) // Allow
                                                                                                                        // pricePaid=0
                                                                                                                        // if
                                                                                                                        // fixed>0
                    || (mode == BuyTrain.Mode.MIN && pricePaid >= fixedPrice && pricePaid > 0) // Ensure price > 0 for
                                                                                               // MIN
                    || (mode == BuyTrain.Mode.MAX && pricePaid > 0 && pricePaid <= fixedPrice); // Ensure price > 0 for
                                                                                                // MAX

            if (!validPrice) {
                errMsg = LocalText.getText("InvalidPricePaid",
                        Bank.format(this, pricePaid)); // Use Bank formatting
                break;
            }

            // This must happen after validation but before affordability checks.
            if (pricePaid == 0 && mode == BuyTrain.Mode.FIXED && fixedPrice > 0) {
                action.setPricePaid(fixedPrice);
                pricePaid = fixedPrice; // Update local variable

            }

            // Does the company have room for another train?
            int trainLimit = company.getCurrentTrainLimit();
            if (!canBuyTrainNow() && !action.isForExchange()) {
                errMsg = LocalText.getText("WouldExceedTrainLimit",
                        String.valueOf(trainLimit));
                break;
            }

            /* Check if this is an emergency buy */
            int cashToRaise = Math.max(0, pricePaid - companyCash);
            if (action.isForcedBuyIfHasRoute() || action.isForcedBuyIfNoRoute()) {

                // REVERT: Removing "Bank Bailout" House Rule.
                // We restore the standard logic which allows the president to sell shares
                // via startShareSellingRound if they have enough assets.

                if (willBankruptcyOccur(company, cashToRaise)) {
                    // DisplayBuffer.add(this, LocalText.getText("YouMustRaiseCashButCannot",
                    // Bank.format(this, cashToRaise)));
                    if (GameDef.getParmAsBoolean(this, GameDef.Parm.EMERGENCY_COMPANY_BANKRUPTCY)) {
                        company.setBankrupt();
                        gameManager.registerCompanyBankruptcy(company);
                        // If company goes bankrupt, the buy action itself doesn't complete,
                        // but we need to signal success to stop further processing in this turn.
                        return true; // Or handle bankruptcy state transition appropriately
                    }
                    // If bankruptcy doesn't end the game/turn, but prevents the buy:
                    errMsg = LocalText.getText("BankruptcyInevitable"); // Example error message
                    break; // Stop validation
                }

                // In some games (SOH) company must sell treasury shares first
                if (GameDef.getParmAsBoolean(this, GameDef.Parm.EMERGENCY_MUST_SELL_TREASURY_SHARES)) {
                    PortfolioModel portfolio = company.getPortfolioModel();
                    Set<PublicCertificate> ownedCerts = portfolio.getCertificates(company);
                    if (!ownedCerts.isEmpty()) {
                        int sharePrice = company.getCurrentSpace().getPrice();

                        // For now, assume that all treasury certs are single shares.
                        // Check how many certs we can and must sell.
                        List<PublicCertificate> soldCerts = new ArrayList<>();
                        int sharesSold = 0;
                        for (PublicCertificate cert : ownedCerts) {
                            soldCerts.add(cert);
                            if (++sharesSold * sharePrice >= cashToRaise)
                                break;
                        }
                        // Don't exceed the 50% pool limit
                        int poolLimit = GameDef.getParmAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT)
                                / company.getShareUnit();
                        int poolShares = pool.getShares(company); // Get current pool shares

                        sharesSold = Math.min(sharesSold, poolLimit - poolShares); // Max sellable = limit - current

                        if (sharesSold > 0) { // Only proceed if shares can actually be sold
                            int raisedCash = sharesSold * sharePrice;

                            // Get the money
                            String cashText = Currency.fromBank(raisedCash, company);
                            String message;
                            if (sharesSold == 1) {
                                message = LocalText.getText("SELL_SHARE_LOG",
                                        companyName + " (" + currentPlayer.getId() + ")",
                                        company.getShareUnit(),
                                        companyName,
                                        cashText);
                            } else {
                                message = LocalText.getText("SELL_SHARES_LOG",
                                        companyName + " (" + currentPlayer.getId() + ")",
                                        sharesSold,
                                        company.getShareUnit(),
                                        sharesSold * company.getShareUnit(),
                                        companyName,
                                        cashText);
                            }
                            ReportBuffer.add(this, message);

                            // Transfer the sold certificates (select specific certs to move)
                            List<PublicCertificate> certsToMove = new ArrayList<>();
                            int movedCount = 0;
                            for (PublicCertificate cert : ownedCerts) {
                                if (movedCount < sharesSold) {
                                    certsToMove.add(cert);
                                    movedCount++;
                                } else {
                                    break;
                                }
                            }
                            Portfolio.moveAll(certsToMove, pool.getParent()); // Move selected certs
                            stockMarket.sell(company, company, sharesSold); // Update market state

                            cashToRaise -= raisedCash;
                            if (cashToRaise < 0)
                                cashToRaise = 0; // Ensure not negative
                            companyCash += raisedCash; // Update company cash *after* selling
                        }
                    }
                } // End EMERGENCY_MUST_SELL_TREASURY_SHARES check

                // Check loan requirements (e.g., 1826)
                if (GameDef.getParmAsBoolean(this, GameDef.Parm.EMERGENCY_MUST_TAKE_LOANS)
                        && company.canLoan() && cashToRaise > 0) { // Check if cash still needed
                    int loansRequired = 0;
                    int currentLoans = company.getCurrentNumberOfLoans();
                    int maxLoans = company.getMaxNumberOfLoans();
                    int loanValue = company.getValuePerLoan();

                    while (currentLoans + loansRequired < maxLoans && cashToRaise > 0) {
                        loansRequired++;
                        cashToRaise -= loanValue;
                        // Don't update companyCash here, just track loans needed
                    }
                    if (cashToRaise < 0)
                        cashToRaise = 0; // Ensure not negative

                    loansTaken = action.getLoansToTake(); // Loans specified by the UI/action
                    if (loansRequired != loansTaken) {
                        errMsg = LocalText.getText("WrongNumberOfLoansTaken",
                                loansTaken, loansRequired);
                        break;
                    }
                    // If loansTaken matches loansRequired, loans will be executed later.
                } // End EMERGENCY_MUST_TAKE_LOANS check

                // Check what the president must add *after* potential share sales/loans
                presidentCash = action.getPresidentCashToAdd(); // Amount specified by UI/action

                if (cashToRaise > 0) { // President contribution needed
                    // Validate if the specified presidentCash matches the required cashToRaise
                    if (presidentCash != cashToRaise) {
                        presidentCash = cashToRaise; // Use the calculated required amount
                    }

                    if (playerCash >= presidentCash) {
                        actualPresidentCash = presidentCash; // President has enough
                    } else {
                        presidentMustSellShares = true; // President needs to sell personal shares
                        cashToBeRaisedByPresident = presidentCash - playerCash;
                    }
                } else {
                    // No president contribution needed (or specified)
                    if (presidentCash > 0) {
                    }
                    actualPresidentCash = 0; // Ensure it's zero
                }

            } else if (action.mayPresidentAddCash()) {
                // Optional president contribution (e.g., buying from another company)
                // presidentCash = action.getPresidentCashToAdd(); // <-- OLD, WRONG (gets
                // MAX_VALUE limit)
                cashToRaise = Math.max(0, pricePaid - companyCash); // How much is *actually* needed
                presidentCash = cashToRaise; // The president *only* needs to add the shortfall.
                if (presidentCash > 0) { // Only proceed if contribution is offered
                    // This checrgba(4, 4, 4, 0.73)now redundant because presidentCash IS
                    // cashToRaise
                    // if (presidentCash < cashToRaise) {
                    // errMsg = LocalText.getText("OptionalContributionInsufficient",
                    // Bank.format(this, presidentCash), Bank.format(this, cashToRaise));
                    // break;
                    // }

                    // Check if offered amount exceeds max allowed
                    int maxAllowedContribution = action.getPresidentCashToAdd(); // This is the limit (MAX_VALUE)
                    if (presidentCash > maxAllowedContribution) {
                        errMsg = LocalText.getText("OptionalContributionInsufficient",
                                Bank.format(this, presidentCash), Bank.format(this, maxAllowedContribution));
                        break;
                    }
                    if (playerCash >= presidentCash) {
                        actualPresidentCash = presidentCash; // President has enough for the optional amount
                    } else {
                        presidentMustSellShares = true; // President needs to sell personal shares for the optional
                                                        // amount
                        cashToBeRaisedByPresident = presidentCash - playerCash;
                    }
                } else {
                    actualPresidentCash = 0; // No optional contribution offered/needed
                }

            } else {
                // No emergency, no optional contribution - standard affordability check
                if (pricePaid > companyCash) {
                    errMsg = LocalText.getText(
                            "NotEnoughMoney",
                            companyName,
                            Bank.format(this, companyCash), // Use current company cash
                            Bank.format(this, pricePaid));
                    break;
                }
                actualPresidentCash = 0; // Ensure it's zero
            }

            // Validate exchanged train (if applicable)
            if (action.isForExchange()) {
                if (exchangedTrain == null) {
                    errMsg = LocalText.getText("NoExchangedTrainSpecified");
                    break;
                } else if (operatingCompany.value().getPortfolioModel().getTrainOfType(
                        exchangedTrain.getType()) == null) { // Check if company owns the train to exchange
                    errMsg = LocalText.getText("CompanyDoesNotOwnTrain",
                            operatingCompany.value().getId(),
                            exchangedTrain.toText());
                    break;
                }
            }

            // Validate special property (if applicable)
            stb = action.getSpecialProperty();
            if (stb != null) {
                if (stb.isExercised()) {
                    errMsg = LocalText.getText("SpecialPropertyAlreadyUsed", stb.getId());
                    break;
                }
                // Add other validation specific to the SpecialTrainBuy property if needed
            }

            break; // Exit validation loop if all checks passed
        } // End of validation loop (while (true))

        if (errMsg != null) {
            DisplayBuffer.add(
                    this,
                    LocalText.getText("CannotBuyTrainFor", companyName, // Correct variable name
                            (train != null ? train.toText() : "N/A"), // Handle null train safely
                            Bank.format(this, pricePaid), errMsg));
            return false; // Indicate validation failed
        }

        // --- Handle Emergency Share Selling (if required for president) ---
        if (presidentMustSellShares) {

            if (train != null) {
                pendingTrainName.set(train.getName()); // Save ID as persistent string
                log.info("LIFECYCLE: pendingTrainName SET to [{}] for company [{}]", pendingTrainName.value(),
                        company.getId());
            } else {
                log.warn("LIFECYCLE: WARNING - train was null during emergency trigger!");
            }

            if (train != null) {
                pendingTrainName.set(train.getName()); // Save ID as persistent string
            }

            ReportBuffer.add(this, LocalText.getText("PlayerMustRaiseCash",
                    currentPlayer.getId(), // Use correct variable
                    Bank.format(this, cashToBeRaisedByPresident),
                    (train != null ? train.getType().getName() : "train"))); // Use type name, handle null

            gameManager.startShareSellingRound(
                    currentPlayer, // Start for the president
                    cashToBeRaisedByPresident,
                    company, // Company benefiting (context for selling round)
                    true); // Indicate it's an emergency buy context

            return true; // Signal that a share round was initiated, stop normal processing here
        }

        // --- Execute Pre-Purchase Actions ---
        if (loansTaken > 0) {
            executeTakeLoans(loansTaken);
            // Recalculate company cash after taking loans if subsequent steps depend on it
            companyCash = company.getCash();
        }

        if (actualPresidentCash > 0) {
            // MoneyModel.cashMove(currentPlayer, operatingCompany.value(),
            // actualPresidentCash); // Old direct move
            String cashText = Currency.wire(currentPlayer, actualPresidentCash, // Use actualPresidentCash
                    operatingCompany.value());
            ReportBuffer.add(this, LocalText.getText("PresidentAddsCash",
                    operatingCompany.value().getId(), currentPlayer.getId(),
                    cashText));
            // Recalculate company cash after president contribution
            companyCash = company.getCash();
        }

        // --- Execute Train Transfer/Purchase ---
        Owner oldOwner = train.getCard().getOwner(); // Get owner *before* moving the train

        // Report based on exchange, special property, or normal buy
        if (exchangedTrain != null) {
            Train oldTrain = operatingCompany.value().getPortfolioModel().getTrainOfType(exchangedTrain.getType());
            if (oldTrain != null) { // Ensure the train to discard exists
                PortfolioModel destination = (oldTrain.isObsolete() ? scrapHeap : pool);
                destination.addTrainCard(oldTrain.getCard()); // Move the *card* of the old train
                ReportBuffer.add(this, LocalText.getText("ExchangesTrain",
                        companyName, exchangedTrain.toText(), train.toText(),
                        oldOwner.getId(), Bank.format(this, pricePaid)));
            } else {

            }
        } else if (stb == null) {
            ReportBuffer.add(this, LocalText.getText("BuysTrainFrom", companyName, // Use correct variable
                    train.toText(), oldOwner.getId(), Bank.format(this, pricePaid)));
        } else {
            ReportBuffer.add(this, LocalText.getText("BuysTrainUsingSP",
                    companyName, train.toText(), oldOwner.getId(),
                    Bank.format(this, pricePaid), stb.getOriginalCompany().getId()));
        }

        // --- Update Game State ---
        train.getCard().setActualTrain(train); // Needed for dual trains bought from Bank

        operatingCompany.value().buyTrain(train, pricePaid); // This method should handle portfolio changes and cash
                                                             // deduction

        // Handle effects of buying from IPO/Bank
        if (oldOwner == ipo.getParent()) { // Check if bought from IPO/Bank
            train.getCardType().addToBoughtFromIPO(); // Track purchase count for this type
            trainManager.setAnyTrainBought(true); // Flag that *a* train was bought this round

            // Clone the train if infinitely available (e.g., 18GA)
            if (train.getCardType().hasInfiniteQuantity()) {
                TrainCard clonedCard = trainManager.cloneTrain(train.getCardType());
                if (clonedCard != null) { // Ensure cloning was successful
                    ipo.addTrainCard(clonedCard);
                } else {

                }
            }

        }

        // Track trains bought this turn if bought from Bank/IPO
        if (oldOwner instanceof BankPortfolio) { // More robust check for Bank/IPO origin
            trainsBoughtThisTurn.add(train.getCardType());
        }

        // Mark special property as used if applicable
        if (stb != null) {
            stb.setExercised();
        }

        // In OperatingRound.java, inside buyTrain()

        // Check for phase changes, rusting, new train availability *immediately* after
        // the buy
        trainManager.checkTrainAvailability(train, oldOwner); //

        newPhaseChecks();
        // The newPhaseChecks() method will now handle both phase-change-detected and
        // non-detected scenarios.

        // Check if the TrainManager flagged that a phase change occurred
        if (trainManager.hasPhaseChanged()) {
            // This will call the 1835-specific override
            newPhaseChecks();

        }

        // Check if newPhaseChecks() (above) set the step to DISCARD_TRAINS
        if (getStep() == GameDef.OrStep.DISCARD_TRAINS) {
            // The action was a SUCCESS. It correctly set the next step.

            return true;
        }

        // Immediately trigger discard step if the purchase put us over the limit.
        // This removes the need to click "Done" to see the discard window.
        if (checkForExcessTrains()) {
            setStep(GameDef.OrStep.DISCARD_TRAINS);
        }

        return true; // Signal successful processing
    } // end buy train

    protected void newPhaseChecks() {
    }

    /**
     * Public accessor for the UI to read the list of companies that must discard.
     * This map is populated by checkForExcessTrains().
     */
    public Map<Player, List<PublicCompany>> getExcessTrainCompanies() {
        return excessTrainCompanies;
    }

    /*
     * =======================================
     * 7.3 BUY TRAIN PREPARATION
     * =======================================
     */

    /**
     * Can the operating company buy a train now? Normally only calls
     * isBelowTrainLimit() to get the result. May be overridden if other
     * considerations apply (such as having a Pullmann in 18EU).
     *
     * @return True if the company has room tobuy a train
     */
    protected boolean canBuyTrainNow() {
        return isBelowTrainLimit();
    }

    /**
     * Predict if bankruptcy will occur in emergency train buying.
     * Must be called <i>after</i> the president has selected a train to buy,
     * and only if treasury cash is insufficient to buy a selected train.
     *
     * Currently a stub, only called in SOH (see OperatingRound_SOH).
     * May be useful in other cases, in which case the code will likely move to
     * here.
     *
     * @param owner       Either a player or a PublicCompany.
     *                    Only tested for the latter (SOH).
     * @param cashToRaise The extra cash amount needed to buy a selected train.
     * @return True if bankruptcy is inevitable.
     */
    public boolean willBankruptcyOccur(Owner owner,
            int cashToRaise) {
        return false;
    }

    protected SortedMap<Integer, Train> newEmergencyTrains;
    protected SortedMap<Integer, Train> usedEmergencyTrains;

    /**
     * Helper to add a BuyTrain action if affordable and within limits.
     * Handles adding to emergency lists if not affordable but might be needed.
     */
    // In OperatingRound.java

    private void addBuyTrainOptionIfPossible(Train train, PortfolioModel from,
            int companyCash, SpecialTrainBuy reducePrice,
            boolean useReducedPrice, // Make sure this boolean is here
            boolean canBuyTrainNow, boolean mustBuyTrain) {
        int cost = train.getCost();
        // Use the passed reducePrice if applicable
        if (useReducedPrice && reducePrice != null) { // *** Check useReducedPrice flag ***
            cost = reducePrice.getPrice(cost);
        }

        if (cost <= companyCash) { // Can the company afford it?
            if (canBuyTrainNow) { // Is there room for it (non-exchange)?
                BuyTrain action = new BuyTrain(train, train.getType(), from.getParent(), cost);
                action.setForcedBuyIfHasRoute(mustBuyTrain); // Mark if mandatory
                if (useReducedPrice && reducePrice != null) { // *** Check useReducedPrice flag ***
                    action.setSpecialProperty(reducePrice);
                }

                // [REFACTOR] Set button label
                String label = createTrainButtonLabel(train, from.getParent(), cost,
                        (useReducedPrice ? reducePrice : null), false);
                action.setButtonLabel(label);

                possibleActions.add(action);
            }
            // else: Affordable, but at train limit. Ignore unless it's an exchange.
        } else if (mustBuyTrain) {
            // Cannot afford, but MUST buy. Add to potential emergency lists.
            // Store the *potentially reduced* cost in the emergency list key
            if (from == ipo) {
                newEmergencyTrains.put(cost, train); // Store with potentially reduced cost
            } else {
                usedEmergencyTrains.put(cost, train); // Store with potentially reduced cost
            }
        }
        // else: Cannot afford and not mandatory. Ignore.
    }

    /**
     * Helper method to process the lists of potentially unaffordable trains
     * (newEmergencyTrains, usedEmergencyTrains) when a train purchase is mandatory.
     * It determines which emergency options (requiring president cash) should be
     * added to the possibleActions list.
     *
     * @param companyCash     Current cash of the operating company.
     * @param reducePrice     The SpecialTrainBuy property, if applicable (can be
     *                        null).
     * @param useReducedPrice True if the reduced price should be considered.
     * @param useNormalPrice  True if the normal price should be considered.
     * @param mustBuyTrain    True if buying a train is mandatory (should always be
     *                        true when this method is called).
     */
    @Deprecated
    private void addEmergencyOptionsToPossibleActions(int companyCash, SpecialTrainBuy reducePrice,
            boolean useReducedPrice, boolean useNormalPrice,
            boolean mustBuyTrain) {

        // Combine trains from both emergency lists (new and used)
        List<Train> emergencyCandidates = new ArrayList<>();
        if (newEmergencyTrains != null && !newEmergencyTrains.isEmpty()) {
            emergencyCandidates.addAll(newEmergencyTrains.values());
        }
        if (usedEmergencyTrains != null && !usedEmergencyTrains.isEmpty()) {
            emergencyCandidates.addAll(usedEmergencyTrains.values());
        }

        if (emergencyCandidates.isEmpty()) {
            // candidates found for company {}.",
            // operatingCompany.value().getId());
            return; // No options to add
        }

        // Check if the rules require buying only the cheapest option
        if (GameDef.getParmAsBoolean(this, GameDef.Parm.EMERGENCY_MUST_BUY_CHEAPEST_TRAIN)) {
            // Sort candidates by cost. Note: This simple sort doesn't account for
            // reducedPrice yet.
            // A more complex sort might be needed if reducedPrice applies differently to
            // different trains.
            emergencyCandidates.sort(Comparator.comparingInt(Train::getCost));

            // Get the cheapest train overall
            Train cheapestTrain = emergencyCandidates.get(0);

            // Add the emergency action(s) for the cheapest train
            if (useReducedPrice) {
                // Add option using reduced price, passing 'true' for useReducedPrice flag
                addEmergencyAction(cheapestTrain, companyCash, reducePrice, true, mustBuyTrain);
            }
            if (useNormalPrice) {
                // Add option using normal price, passing 'false' for useReducedPrice flag
                addEmergencyAction(cheapestTrain, companyCash, null, false, mustBuyTrain);
            }
        } else {
            // Rules allow buying any available train, even in emergency
            for (Train train : emergencyCandidates) {
                // Add the emergency action(s) for each potential train
                if (useReducedPrice) {
                    // Add option using reduced price, passing 'true' for useReducedPrice flag
                    addEmergencyAction(train, companyCash, reducePrice, true, mustBuyTrain);
                }
                if (useNormalPrice) {
                    // Add option using normal price, passing 'false' for useReducedPrice flag
                    addEmergencyAction(train, companyCash, null, false, mustBuyTrain);
                }
            }
        }
    } // End of addEmergencyOptionsToPossibleActions

    /**
     * Helper to create and add a single emergency BuyTrain action to
     * possibleActions.
     * This action indicates that the president must contribute cash.
     *
     * @param train           The train being considered for purchase.
     * @param companyCash     Current cash of the operating company.
     * @param reducePrice     The SpecialTrainBuy property, if applicable (can be
     *                        null).
     * @param useReducedPrice True if the reduced price should be applied for this
     *                        specific call.
     * @param mustBuyTrain    True if buying a train is mandatory for the company
     *                        this turn.
     */

    // [REPLACE]
    /**
     * Helper to create and add emergency BuyTrain actions to possibleActions.
     * This now creates up to three discrete options per train:
     * 1. Buy for 1
     * 2. Buy for Company Cash
     * 3. Buy for Company Cash + President Cash
     */
    private void addEmergencyAction(Train train, int companyCash, SpecialTrainBuy reducePrice,
            boolean useReducedPrice, boolean mustBuyTrain) {

        int cost = train.getCost();
        if (useReducedPrice && reducePrice != null) {
            cost = reducePrice.getPrice(cost);
        }

        Player president = operatingCompany.value().getPresident();
        int presidentCash = (president != null) ? president.getCashValue() : 0;
        int totalCash = companyCash + presidentCash;

        // Determine the train's current owner (IPO or Pool)
        Owner owner = (train.getOwner() instanceof BankPortfolio) ? ipo.getParent() : train.getOwner();

        // Check if bankruptcy is inevitable for the *cheapest* possible option (cost=1)
        if (willBankruptcyOccur(president, Math.max(0, 1 - companyCash))) {
            return; // Don't offer any actions if even 1 is impossible
        }

        // --- Action 1: Buy for 1 (Min Price) ---
        if (totalCash >= 1) {
            BuyTrain btMin = new BuyTrain(train, owner, 1);
            btMin.setFixedCostMode(BuyTrain.Mode.FIXED); // It's a fixed price click
            int cashToRaiseMin = Math.max(0, 1 - companyCash);
            btMin.setPresidentMustAddCash(cashToRaiseMin);
            btMin.setForcedBuyIfHasRoute(mustBuyTrain);
            if (useReducedPrice && reducePrice != null)
                btMin.setSpecialProperty(reducePrice);
            btMin.setButtonLabel(createTrainButtonLabel(train, owner, 1, (useReducedPrice ? reducePrice : null), true));
            possibleActions.add(btMin);
        }

        // --- Action 2: Buy for Max Company Cash ---
        // Only add if coCash > 1 (to avoid duplicating '1')
        if (companyCash > 1) {
            // Check for bankruptcy *at this specific price*
            if (!willBankruptcyOccur(president, Math.max(0, companyCash - companyCash))) {
                BuyTrain btCo = new BuyTrain(train, owner, companyCash);
                btCo.setFixedCostMode(BuyTrain.Mode.FIXED);
                btCo.setPresidentMustAddCash(0); // Company pays all
                btCo.setForcedBuyIfHasRoute(mustBuyTrain);
                if (useReducedPrice && reducePrice != null)
                    btCo.setSpecialProperty(reducePrice);
                btCo.setButtonLabel(createTrainButtonLabel(train, owner, companyCash,
                        (useReducedPrice ? reducePrice : null), true));
                possibleActions.add(btCo);
            }
        }

        // --- Action 3: Buy for Max Total Cash (Company + President) ---
        // Only add if totalCash > companyCash (to avoid duplicating) and totalCash > 1
        if (totalCash > companyCash && totalCash > 1) {
            // Check for bankruptcy *at this specific price*
            if (!willBankruptcyOccur(president, Math.max(0, totalCash - companyCash))) {
                BuyTrain btMax = new BuyTrain(train, owner, totalCash);
                btMax.setFixedCostMode(BuyTrain.Mode.FIXED);
                btMax.setPresidentMustAddCash(presidentCash); // President adds all their cash
                btMax.setForcedBuyIfHasRoute(mustBuyTrain);
                if (useReducedPrice && reducePrice != null)
                    btMax.setSpecialProperty(reducePrice);
                btMax.setButtonLabel(
                        createTrainButtonLabel(train, owner, totalCash, (useReducedPrice ? reducePrice : null), true));
                possibleActions.add(btMax);
            }
        }
    }

    /**
     * Helper to create and add a single emergency BuyTrain action to
     * possibleActions.
     */
    private void addEmergencyAction(Train train, int companyCash, SpecialTrainBuy reducePrice, boolean mustBuyTrain) {
        int cost = train.getCost();
        if (reducePrice != null)
            cost = reducePrice.getPrice(cost);
        int cashToRaise = cost - companyCash; // Amount president needs to cover

        // TODO: Add checks for loan requirements (1826) or treasury share sales (SOH)
        // here
        // If those can cover part of cashToRaise, reduce it before setting president
        // contribution

        // Check if bankruptcy is inevitable before adding the option
        if (willBankruptcyOccur(operatingCompany.value().getPresident(), cashToRaise)) {

            return; // Don't offer an impossible action
        }

        BuyTrain bt = new BuyTrain(train, train.getOwner(), cost); // Owner is IPO or Pool here
        bt.setPresidentMustAddCash(cashToRaise); // Indicate amount needed from president
        bt.setForcedBuyIfHasRoute(mustBuyTrain); // Mark as mandatory
        // bt.setForcedBuyIfNoRoute(mustBuyTrainWithoutRoute); // If needed
        if (reducePrice != null)
            bt.setSpecialProperty(reducePrice);

        possibleActions.add(bt);

    }

    protected void addOtherExchangesAtTrainLimit(PublicCompany company, Train train) {
    }

    /**
     * Calculate the potential contribution of selling any treasury
     * shares of a given company to pay for an "emergency" train.
     * It is assumed that no more shares may be sold than is necessary
     * to pay that bill.
     * 
     * @param company     The company that must buy a train.
     * @param cashToRaise The missing cash value.
     *                    If zero, just the total share value is returned.
     * @return The potential yield of selling treasury shares.
     */
    protected int treasurySharesValue(PublicCompany company, int cashToRaise) {

        int value = 0;

        if (GameDef.getParmAsBoolean(this,
                GameDef.Parm.EMERGENCY_MUST_SELL_TREASURY_SHARES)) {
            PortfolioModel portfolio = company.getPortfolioModel();
            int numberAvailable = portfolio.getCertificates(company).size();
            int numberToSell = 0;
            if (numberAvailable > 0) {
                int sharePrice = company.getCurrentSpace().getPrice();

                // If a required cash value is specified, restrict the
                // number to sell to just the required minimum.
                if (cashToRaise > 0) {
                    // For now, assume that all treasury certs are single shares.
                    // Calculate how many certs we should sell.
                    while (++numberToSell * sharePrice < cashToRaise
                            && numberToSell < numberAvailable) {
                    }
                } else {
                    numberToSell = numberAvailable;
                }
                // Don't exceed the 50% pool limit
                numberToSell = Math.min(numberToSell,
                        GameDef.getParmAsInt(this,
                                GameDef.Parm.POOL_SHARE_LIMIT) / company.getShareUnit()
                                - pool.getShares(company));
                value += numberToSell * sharePrice;

            }
        }
        return value;
    }

    protected void addBuyTrainOption(Train train, PortfolioModel from,
            int companyCash, SpecialTrainBuy reducePrice,
            boolean canBuyTrainNow, boolean mustBuyTrain,
            boolean mustBuyTrainWithoutRoute) {

        int cost = train.getCost();
        if (reducePrice != null)
            cost = reducePrice.getPrice(cost);
        if (cost <= companyCash) {
            if (canBuyTrainNow) {
                BuyTrain action = new BuyTrain(train, train.getType(), from.getParent(), cost);
                action.setForcedBuyIfNoRoute(mustBuyTrainWithoutRoute);
                action.setForcedBuyIfHasRoute(mustBuyTrain);
                if (reducePrice != null)
                    action.setSpecialProperty(reducePrice);
                possibleActions.add(action);
            }
        } else if (mustBuyTrain) {

            emergency.set(true);
            if (from == ipo) {
                newEmergencyTrains.put(cost, train);
            } else {
                usedEmergencyTrains.put(cost, train);
            }
        }
    }

    protected void addEmergencyBuyTrainOption(Train train, Owner from,
            int companyCash, SpecialTrainBuy reducePrice,
            boolean mustBuyTrain,
            boolean mustBuyTrainWithoutRoute) {
        int cost = train.getCost();
        if (reducePrice != null)
            cost = reducePrice.getPrice(cost);
        BuyTrain bt = new BuyTrain(train, from, cost);
        PublicCompany company = operatingCompany.value();

        int cashToRaise = cost - companyCash;

        // In 1826, companies must first take loans before the president can help
        if (GameDef.getParmAsBoolean(this, GameDef.Parm.EMERGENCY_MUST_TAKE_LOANS)) {
            int loansToTake = canTakeLoans(company, cashToRaise);
            if (loansToTake > 0) {
                bt.setLoansToTake(loansToTake);
                cashToRaise = Math.max(0, cashToRaise - loansToTake * company.getValuePerLoan());
                bt.setExtraMessage(LocalText.getText("MustTakeLoan",
                        company.getId(),
                        loansToTake));
            }
        }

        // President contribution
        int presidentCash = cashToRaise;
        if (cashToRaise > 0) {
            // Check if company has shares to sell (as in SOH).
            // If so, reduce or nullify the president contribution
            int sharesValue = treasurySharesValue(company, cashToRaise);
            if (sharesValue > 0) {
                presidentCash -= sharesValue;
                if (presidentCash < 0)
                    presidentCash = 0;
                bt.setExtraMessage(LocalText.getText("MustSellShares",
                        company.getId(),
                        sharesValue / company.getMarketPrice(),
                        company.getShareUnit(),
                        Bank.format(this, sharesValue)));
            }

        }
        // In the BuyTrain action object, presidentCash being > 0
        // has the side effect of being the emergency indicator.
        bt.setPresidentMustAddCash(presidentCash);
        bt.setForcedBuyIfHasRoute(mustBuyTrain);
        bt.setForcedBuyIfNoRoute(mustBuyTrainWithoutRoute);
        // TEMPORARY
        possibleActions.add(bt);
    }

    /**
     * Returns whether or not the company is allowed to buy a train, considering
     * its train limit.
     *
     * @return True if the company is below its train limit
     */
    protected boolean isBelowTrainLimit() {
        return operatingCompany.value().getNumberOfTrains() < operatingCompany.value().getCurrentTrainLimit();
    }

    public void checkForeignSales() {
        // Get the set of available trains *once* at the beginning.
        Set<Train> availableNewTrains = trainManager.getAvailableNewTrains();

        // If the list is empty, there is nothing to remove, so we can exit immediately.
        if (availableNewTrains.isEmpty()) {
            return;
        }

        if (GameDef.getParmAsBoolean(this, GameDef.Parm.REMOVE_TRAIN_BEFORE_SR)
                && trainManager.isAnyTrainBought()) {

            // We already know the list is not empty, so this call is now safe.
            Train train = Iterables.get(availableNewTrains, 0);
            if (train.getCardType().hasInfiniteQuantity())
                return;
            // Dont export Permanent Trains; MBR: 030102021
            if (train.getCardType().isPermanent())
                return;
            scrapHeap.addTrainCard(train.getCard());
            ReportBuffer.add(this,
                    LocalText.getText("RemoveTrain", train.toText()));
            // MBr: 03012021 Trains were not made available after export prior
            trainManager.checkTrainAvailability(train, bank.getIpo());
            // MBr: 02012021 - 18Chesapeake Remove a non permanent train before every
            // Stockround
        } else {
            if (GameDef.getParmAsBoolean(this, GameDef.Parm.REMOVE_TRAIN_BEFORE_SR)
                    && (!GameDef.getParmAsBoolean(this, GameDef.Parm.REMOVE_PERMANENT))) {

                // We already know the list is not empty, so this call is now safe.
                Train train = Iterables.get(availableNewTrains, 0);
                if (train.getCardType().isPermanent())
                    return;
                if (train.getCardType().hasInfiniteQuantity())
                    return;
                scrapHeap.addTrainCard(train.getCard());
                trainManager.checkTrainAvailability(train, bank.getIpo());
                ReportBuffer.add(this,
                        LocalText.getText("RemoveTrain", train.toText()));
            } else { // MBr: 03012021 export a train if one has been sold....
                if (GameDef.getParmAsBoolean(this, GameDef.Parm.REMOVE_TRAIN_BEFORE_SR)
                        && trainManager.isAnyTrainBought()) {

                    // We already know the list is not empty, so this call is now safe.
                    Train train = Iterables.get(availableNewTrains, 0);
                    if (train.getCardType().hasInfiniteQuantity())
                        return;
                    scrapHeap.addTrainCard(train.getCard());
                    ReportBuffer.add(this,
                            LocalText.getText("RemoveTrain", train.toText()));
                    // MBr: 03012021 Trains were not made available after export prior
                    trainManager.checkTrainAvailability(train, bank.getIpo());
                }
            }
        }
    }
    /*
     * =======================================
     * 8. VARIOUS UTILITIES
     * =======================================
     */

    protected <T extends SpecialProperty> List<T> getSpecialProperties(
            Class<T> clazz) {
        List<T> specialProperties = new ArrayList<>();
        if (!operatingCompany.value().isClosed()) {
            // OC may have closed itself (e.g. in 1835 when M2 buys 1st 4T and
            // starts PR)
            specialProperties.addAll(operatingCompany.value().getPortfolioModel().getSpecialProperties(
                    clazz, false));
            specialProperties.addAll(operatingCompany.value().getPresident().getPortfolioModel().getSpecialProperties(
                    clazz, false));
        }
        return specialProperties;
    }

    /**
     * Update the status if the step has changed by an Undo or Redo
     */
    public void update(String text) {
        prepareStep();
    }

    @Override
    public String toString() {
        return "OperatingRound " + thisOrNumber;
    }

    public boolean equals(RoundFacade round) {
        return round instanceof OperatingRound
                && thisOrNumber.equals(((OperatingRound) round).thisOrNumber);
    }

    @Override
    public String getRoundName() {
        return toString();
    }

    // For logging
    public String getCompAndPresName(PublicCompany company) {
        // Null-Check for President (Closed Company Safety) ---
        String presName = (company.getPresident() != null) ? company.getPresident().getId() : "N/A";
        return company.getId() + "(" + presName + ")";
    }

    // Observer methods
    public Observable getObservable() {
        return stepObject;
    }

    // ++ START AI STATE RESTORATION SETTERS ++
    /**
     * Bypasses ALL logic and notifications (e.g., prepareStep()).
     * Directly sets the current step for state re-hydration.
     */
    public void setStep_AI(String stepName) {
        try {
            GameDef.OrStep newStep = GameDef.OrStep.valueOf(stepName);
            this.stepObject.set(newStep); // Directly set the GenericState
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Creates a human-readable button label for a specific train purchase.
     * 
     * @param train     The train to buy.
     * @param from      The owner (seller) of the train.
     * @param cost      The price of the train.
     * @param stb       A special property, if used (can be null).
     * @param emergency True if this is an emergency purchase.
     * @return A formatted String for the button label.
     */
    private String createTrainButtonLabel(Train train, Owner from, int cost, SpecialTrainBuy stb, boolean emergency) {
        StringBuilder label = new StringBuilder();

        // 1. Action
        label.append("Buy '").append(train.getType()).append("'");

        // 2. Source
        label.append(" from ").append(from.getId());

        // 3. Price
        label.append(" (");
        if (from instanceof PublicCompany && cost == 0) {
            // Variable price from another company
            int cash = operatingCompany.value().getCash();
            label.append("1-").append(cash > 0 ? cash : 1); // Show "1-X" or "1-1" if no cash
        } else {
            label.append(cost);
        }
        label.append(")");

        return label.toString();
    }

    /**
     * Public method to allow the GameManager to force the OR
     * back to a specific step, resetting any flags
     * that would prevent it from running.
     */
    public void forceStep(GameDef.OrStep newStep) {

        // 1. Set the step
        setStep(newStep);

        // 2. Reset the flags that would block this step from re-running

        // Found in file: this is an ArrayListState (line 116)
        // This is the critical reset to allow buying another train.
        this.trainsBoughtThisTurn.clear();

        // Found in file: this is a boolean (line 121)
        normalTokenLaidThisTurn.set(false);

        // The other flags I guessed (revenueSet, tileLaid) were incorrect
        // and are not needed to re-run the BUY_TRAIN step.
    }
    // File: OperatingRound.java
    // Add these two new methods anywhere inside the class

    // ... (lines of unchanged context code) ...
    // [REPLACE the entire 'layTile' method in OperatingRound.java with this]

    public boolean layTile(LayTile action) {

        String errMsg = null;
        int cost = 0;
        SpecialTileLay stl = null;
        boolean extra = false;

        PublicCompany company = action.getCompany();
        String companyName = company.getId();
        Tile tile = action.getLaidTile();
        MapHex hex = action.getChosenHex();
        int orientation = action.getOrientation();

        while (true) {
            if (!companyName.equals(operatingCompany.value().getId())) {
                errMsg = LocalText.getText("WrongCompany", companyName, operatingCompany.value().getId());
                break;
            }
            if (getStep() != GameDef.OrStep.LAY_TRACK) {
                errMsg = LocalText.getText("WrongActionNoTileLay");
                break;
            }
            if (tile == null) {
                break;
            }
            if (!Phase.getCurrent(this).isTileColourAllowed(tile.getColourText())) {
                errMsg = LocalText.getText("TileNotYetAvailable", tile.toText());
                break;
            }
            if (tile.getFreeCount() == 0) {
                errMsg = LocalText.getText("TileNotAvailable", tile.toText());
                break;
            }

            // Add Logging to debug why tiles are blocked
            boolean allowed = isTileLayAllowed(operatingCompany.value(), hex, orientation);

            if (!allowed) {
                // Use a generic existing error message since "Blocked" might not exist
                errMsg = LocalText.getText("TileMayNotBeLaidInHex", tile.toText(), hex.getId());
                break;
            }

            List<Tile> allowedTiles = action.getTiles();
            if (allowedTiles != null && !allowedTiles.isEmpty() && !allowedTiles.contains(tile)) {
                errMsg = LocalText.getText("TileMayNotBeLaidInHex", tile.toText(), hex.getId());
                break;
            }
            stl = action.getSpecialProperty();
            if (stl != null) {
                extra = stl.isExtra();
            }
            if (!extra && !validateNormalTileLay(tile)) {
                errMsg = LocalText.getText("NumberOfNormalTileLaysExceeded", tile.getColourText());
                break;
            }
            cost = tileLayCost(action);
            if (cost < 0) {
                errMsg = LocalText.getText("NegativeAmountNotAllowed", Bank.format(this, cost));
                break;
            }
            if (cost > operatingCompany.value().getCash()) {
                errMsg = LocalText.getText("NotEnoughMoney", companyName,
                        Bank.format(this, operatingCompany.value().getCash()),
                        Bank.format(this, cost));
                break;
            }
            break;
        } // End of validation loop

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotLayTileOn",
                    companyName, (tile != null ? tile.toText() : "null tile"), hex.getId(),
                    Bank.format(this, cost), errMsg));
            return false; // Indicate action failed
        }

        if (tile != null) {
            String costText = null;
            if (cost > 0) {
                costText = Currency.toBank(operatingCompany.value(), cost);
            }
            // Capture the old tile before the upgrade overwrites it
            Tile oldTile = hex.getCurrentTile();

            operatingCompany.value().layTile(hex, tile, orientation, cost);
            hex.upgrade(action);

            // Deregister the old tile to return it to the pool
            if (oldTile != null) {
                oldTile.remove(hex);
            }
            // Register the new tile to decrement its count
            tile.add(hex);

            try {
                int newPresetRevenue = calculateCurrentPotentialRevenue(operatingCompany.value());

            } catch (Exception e) {
            }

            if (costText == null) {
                ReportBuffer.add(this, LocalText.getText("LaysTileAt",
                        companyName, tile.toText(), hex.getId(),
                        hex.getOrientationName(HexSide.get(orientation))));
            } else {
                ReportBuffer.add(this, LocalText.getText("LaysTileAtFor",
                        companyName, tile.toText(), hex.getId(),
                        hex.getOrientationName(HexSide.get(orientation)),
                        costText));
            }

            if (stl != null) {
                stl.setExercised();
                if (!extra) {

                    registerNormalTileLay(tile);
                    normalTileLaidThisTurn.set(true);
                }
            } else {
                registerNormalTileLay(tile);
                normalTileLaidThisTurn.set(true);
            }
        } else {
        }

        // - for PfB / Extra Lays / Second Tile ---
        // Check if the lay was an "extra" lay (like PfB)
        if (stl != null && stl.isExtra()) {
            // Remaining in LAY_TRACK step for normal lay.");
            // Do NOT advance the step, just return. This allows the 2nd (normal) lay.
            return true;
        }

        // Check if the company is still allowed to lay another normal tile
        if (!tileLaysPerColour.isEmpty()) {
            // lay(s) remaining. Remaining in LAY_TRACK step.", tileLaysPerColour.size());
            // Do NOT advance the step, just return.
            return true;
        }

        // This was the last normal/extra lay. Advance the step.

        nextStep();
        return true;
    }

    /**
     * Processes the atomic LayTileAndHomeTokenAction.
     * This method executes the tile lay, and then immediately
     * executes the home token lay.
     */
    private boolean processLayTileAndHomeToken(LayTileAndHomeTokenAction action) {
        // --- Step 1: Lay the Tile ---
        // We call layTile() with the action.
        // This will lay the tile, charge costs, and (critically)
        // advance the game step from LAY_TRACK to LAY_TOKEN.
        boolean tileLaid = layTile(action);

        if (!tileLaid) {
            // layTile failed (e.g., validation), so we abort.
            return false;
        }

        // --- Step 2: Lay the Home Token ---
        // The OperatingRound is now in the LAY_TOKEN step. We immediately
        // create and process the corresponding LayBaseToken action.

        // Create the new action
        LayBaseToken tokenAction = new LayBaseToken(this.getRoot(), action.getChosenHex());
        tokenAction.setCompany(action.getCompany());
        tokenAction.setChosenStation(action.getChosenStation());
        tokenAction.setType(LayBaseToken.HOME_CITY); // Mark this as a home token lay

        // Call OperatingRound.layBaseToken()
        // This method will lay the token and, because it's a HOME_CITY type,
        // it will reset the turn by calling setStep(INITIAL),
        // allowing the company to take its full, normal turn.
        return layBaseToken(tokenAction);
    }

    // File: OperatingRound.java
    // Method: resetTransientStateOnLoad

    @Override
    public boolean setPossibleActions() {

        possibleActions.clear();
        selectedAction = null;
        boolean forced = false;
        doneAllowed.set(false);

        // Safety Guard: If the operating company was reset (e.g. during reload).
        if (operatingCompany.value() == null) {
            if (operatingCompanies != null && !operatingCompanies.isEmpty()) {
                // 1. PRIME THE STATE: Explicitly set to the first company.
                // This ensures operatingCompany.value() is NOT null, protecting against
                // subclass logic or initTurn() crashes if setNextOperatingCompany fails.
                setOperatingCompany(operatingCompanies.get(0));

                // 2. FIND ACTUAL NEXT: Call the logic to find the correct starting company
                // (e.g. skipping closed/hibernated companies).
                setNextOperatingCompany(true);

                // 3. RESET STEP: Force step to INITIAL to ensure initTurn() is called.
                setStep(GameDef.OrStep.INITIAL);
            } else {
                // Should not happen in a valid OR, but prevents a crash if empty
                return false;
            }
        }

        // Get the current step *once*
        GameDef.OrStep step = getStep();
        PublicCompany company = operatingCompany.value();

        if (step == GameDef.OrStep.INITIAL) {
            initTurn(); // This logs the turn start

            nextStep(); // Advance from INITIAL to LAY_TRACK (or whatever comes next)
            step = getStep(); // Get the new step

        }

        // --- Normal Step Processing ---
        if (step == GameDef.OrStep.LAY_TRACK) {

            possibleActions.addAll(getNormalTileLays(true));
            possibleActions.addAll(getSpecialTileLays(true));

            doneAllowed.set(true); // Enable "Skip All" (DONE)

            possibleActions.add(new NullAction(getRoot(), NullAction.Mode.SKIP));

        } else if (step == GameDef.OrStep.LAY_TOKEN) {

            setNormalTokenLays();
            setSpecialTokenLays();

            boolean bonusLaysAvailable = !getSpecialProperties(SpecialBonusTokenLay.class).isEmpty();

            if (!currentNormalTokenLays.isEmpty() || !currentSpecialTokenLays.isEmpty() || bonusLaysAvailable) {
                possibleActions.addAll(currentNormalTokenLays);
                possibleActions.addAll(currentSpecialTokenLays);
                if (!forced) {
                    doneAllowed.set(true); // Enable "Skip All" (DONE)
                    possibleActions.add(new NullAction(getRoot(), NullAction.Mode.SKIP));
                }
            } else {

                nextStep();
                return setPossibleActions(); // Re-run for the next step
            }

        } else if (step == GameDef.OrStep.CALC_REVENUE) {

            // Disable "Done" (Skip) for Revenue.
            // Players must explicitly choose Payout, Withhold, or Split (generated by
            // prepareRevenue...).
            // Pressing "Done" (or 'D') previously skipped revenue entirely, resulting in 0
            // dividend.
            doneAllowed.set(false);
            prepareRevenueAndDividendAction();
            if (noMapMode)
                prepareNoMapActions();

        } else if (step == GameDef.OrStep.BUY_TRAIN) {
            setBuyableTrains(); // This populates possibleActions

            boolean hasBuyActions = false;
            for (PossibleAction pa : possibleActions.getList()) {
                if (pa instanceof BuyTrain) {
                    hasBuyActions = true;
                    break;
                }
            }
            // This is the DONE/SKIP logic block for BUY_TRAIN
            if (hasBuyActions) {
                // 'SKIP' (Done Buying) button.
                // JSON State Recovery / Logic Fix: Ensure SKIP is not offered if purchase is
                // mandatory.
                boolean mustBuy = !operatingCompany.value().hasTrains() &&
                        operatingCompany.value().mustOwnATrain();
                if (!mustBuy) {
                    possibleActions.add(new NullAction(getRoot(), NullAction.Mode.SKIP));
                }
            } else {
                // 'doneAllowed = true'.
                doneAllowed.set(true);
            }

            if (noMapMode && (operatingCompany.value().getLastRevenue() == 0))
                prepareNoMapActions();

            // This step already correctly sets doneAllowed inside setBuyableTrains()

        } else if (step == GameDef.OrStep.DISCARD_TRAINS) {
            forced = true;
            // On Undo, the transient 'excessTrainCompanies' map is cleared.
            // We must regenerate it to verify if discards are actually needed.
            if (excessTrainCompanies == null || excessTrainCompanies.isEmpty()) {
                checkForExcessTrains();
            }

            setTrainsToDiscard(); // This populates possibleActions

            // This detects the race condition/data desync where setTrainsToDiscard
            // fails to generate actions but the step is still DISCARD_TRAINS.
            if (possibleActions.getType(DiscardTrain.class).isEmpty()) {
                // This is a bug state, but we must not hang.
                playerManager.setCurrentPlayer(operatingCompany.value().getPresident());
                stepObject.set(GameDef.OrStep.BUY_TRAIN);
                return setPossibleActions(); // Re-run for the new step
            }

        } else if (step == GameDef.OrStep.TRADE_SHARES) {
            gameManager.getCurrentRound().setPossibleActions();
        }

        // --- Common Actions ---
        if (!forced) {

            setBonusTokenLays();
            setDestinationActions();
            setGameSpecificPossibleActions();

            // Can private companies be bought?
            if (isPrivateSellingAllowed()) {

                // Create a list of players with the current one in front
                int currentPlayerIndex = operatingCompany.value().getPresident().getIndex();
                Player player;
                int minPrice, maxPrice;
                List<Player> players = playerManager.getPlayers();
                int numberOfPlayers = playerManager.getNumberOfPlayers();

                for (int i = currentPlayerIndex; i < currentPlayerIndex + numberOfPlayers; i++) {
                    player = players.get(i % numberOfPlayers);

                    // Allow game-specific logic to restrict selling (e.g. must be President)
                    if (!maySellPrivate(player))
                        continue;

boolean restrictPrivateTrade = GameOption.getAsBoolean(this, "RestrictPrivateTradingToSameOwner");

                    if (restrictPrivateTrade && player != operatingCompany.value().getPresident()) {
                        continue;
                    }

                    for (PrivateCompany privComp : player.getPortfolioModel().getPrivateCompanies()) {

                        // Check to see if the private can be sold to a company
                        if (!privComp.tradeableToCompany()) {
                            continue;
                        }

                        // Determine price limits
                        minPrice = getPrivateMinimumPrice(privComp);
                        maxPrice = getPrivateMaximumPrice(privComp);

                        // Generate the action (The UI will attach this to the Private Card)
                        BuyPrivate buyPrivate = new BuyPrivate(privComp, minPrice, maxPrice);
                        possibleActions.add(buyPrivate);
                    }
                }
            }

            // // Can private companies be bought?
            // if (isPrivateSellingAllowed()) {

            // // Create a list of players with the current one in front
            // int currentPlayerIndex = operatingCompany.value().getPresident().getIndex();
            // Player player;
            // int minPrice, maxPrice;
            // List<Player> players = playerManager.getPlayers();
            // int numberOfPlayers = playerManager.getNumberOfPlayers();
            // for (int i = currentPlayerIndex; i < currentPlayerIndex + numberOfPlayers;
            // i++) {
            // player = players.get(i % numberOfPlayers);
            // if (!maySellPrivate(player))
            // continue;
            // for (PrivateCompany privComp :
            // player.getPortfolioModel().getPrivateCompanies()) {

            // // check to see if the private can be sold to a company
            // if (!privComp.tradeableToCompany()) {
            // continue;
            // }

            // minPrice = getPrivateMinimumPrice(privComp);

            // maxPrice = getPrivateMaximumPrice(privComp);

            // BuyPrivate buyPrivate = new BuyPrivate(privComp, minPrice, maxPrice);
            // possibleActions.add(buyPrivate);
            // }
            // }
            // }

            if (operatingCompany.value().canUseSpecialProperties()) {

                // Are there any "common" special properties,
                // i.e. properties that are available to everyone?
                List<SpecialProperty> commonSP = gameManager.getCommonSpecialProperties();
                if (commonSP != null) {
                    SellBonusToken sbt;
                    loop: for (SpecialProperty sp : commonSP) {
                        if (sp instanceof SellBonusToken && sp.isUsableDuringOR(getStep())) {
                            sbt = (SellBonusToken) sp;
                            // Can't buy if already owned
                            if (operatingCompany.value().getBonuses() != null) {
                                for (Bonus bonus : operatingCompany.value().getBonuses()) {
                                    if (bonus.getName().equals(sp.getId()))
                                        continue loop;
                                }
                            }
                            possibleActions.add(new BuyBonusToken(sbt));
                        }
                    }
                }

                // Are there other step-independent special properties owned by
                // the company?
                List<SpecialProperty> orsps = operatingCompany.value().getPortfolioModel().getAllSpecialProperties();

                // TODO: Do we still need this directly from the operating
                // company?
                // List<SpecialProperty> compsps =
                // operatingCompany.get().getSpecialProperties();
                // if (compsps != null) orsps.addAll(compsps);

                if (orsps != null) {
                    for (SpecialProperty sp : orsps) {
                        if (!sp.isExercised() && sp.isUsableIfOwnedByCompany()
                                && sp.isUsableDuringOR(step)) {
                            if (sp instanceof SpecialBaseTokenLay) {
                                if (getStep() != GameDef.OrStep.LAY_TOKEN) {
                                    possibleActions.add(new LayBaseToken(getRoot(),
                                            (SpecialBaseTokenLay) sp));

                                }
                            } else if (!(sp instanceof SpecialTileLay)
                                    && !(sp instanceof SpecialBonusTokenLay)) {
                                possibleActions.add(new UseSpecialProperty(sp));
                            }
                        }
                    }
                }
                // Are there other step-independent special properties owned by
                // the president?
                orsps = playerManager.getCurrentPlayer().getPortfolioModel().getAllSpecialProperties();
                if (orsps != null) {
                    for (SpecialProperty sp : orsps) {
                        if (!sp.isExercised() && sp.isUsableIfOwnedByPlayer()
                                && sp.isUsableDuringOR(step)) {
                            if (sp instanceof SpecialBaseTokenLay) {
                                if (getStep() != GameDef.OrStep.LAY_TOKEN) {
                                    possibleActions.add(new LayBaseToken(getRoot(),
                                            (SpecialBaseTokenLay) sp));
                                }
                            } else if (sp instanceof SpecialBonusTokenLay) {
                                if (getStep() != GameDef.OrStep.LAY_TOKEN) {
                                    possibleActions.add(new LayBonusToken(getRoot(),
                                            (SpecialBonusTokenLay) sp,
                                            ((SpecialBonusTokenLay) sp).getToken()));

                                }
                            } else if (!(sp instanceof SpecialTileLay)) {
                                possibleActions.add(new UseSpecialProperty(sp));
                            }
                        }
                    }
                }
            }
        } else {

        }

        if (doneAllowed.value()) {

            possibleActions.add(new NullAction(getRoot(), NullAction.Mode.DONE));
        }

        for (PossibleAction pa : possibleActions.getList()) {
            if (pa instanceof PossibleORAction) {
                PossibleORAction pORA = (PossibleORAction) pa;
                if (pORA.getCompany() == null) {
                    pORA.setCompany(operatingCompany.value());
                }
            }
        }

        // possibleActions.getList().size());
        return true;
    }

    public boolean layBaseToken(LayBaseToken action) {

        PublicCompany company = action.getCompany();

        String errMsg = null;
        int cost = 0;
        SpecialBaseTokenLay stl = null;
        boolean extra = false;

        MapHex hex = action.getChosenHex();
        Stop stop = action.getChosenStop();

        String companyName = company.getId();

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            // Must be correct step (exception: home base lay & some special
            // token lay)
            if (getStep() != GameDef.OrStep.LAY_TOKEN
                    && action.getType() != LayBaseToken.HOME_CITY
                    && action.getType() != LayBaseToken.SPECIAL_PROPERTY
                    && action.getType() != LayBaseToken.CORRECTION
                    && action.getType() != LayBaseToken.FORCED_LAY) {
                errMsg = LocalText.getText("WrongActionNoTokenLay");
                break;
            }

            if (company.getNumberOfFreeBaseTokens() == 0) {
                errMsg = LocalText.getText("HasNoTokensLeft", companyName);
                break;
            }

            if (!isTokenLayAllowed(company, hex, stop)) {
                errMsg = LocalText.getText("BaseTokenSlotIsReserved");
                break;
            }

            if (!stop.hasTokenSlotsLeft()) {
                errMsg = LocalText.getText("CityHasNoEmptySlots");
                break;
            }

            /*
             * TODO: the below condition holds for 1830. in some games, separate
             * cities on one tile may hold tokens of the same company; this case
             * is not yet covered.
             */
            if (stop.hasTokenOf(company)) {
                errMsg = LocalText.getText("TileAlreadyHasToken", hex.getId(),
                        companyName);
                break;
            }

            if (action != null) {
                List<MapHex> locations = action.getLocations();
                if (locations != null && locations.size() > 0
                        && !locations.contains(hex) && !locations.contains(null)) {
                    errMsg = LocalText.getText("TokenLayingHexMismatch",
                            hex.getId(), action.getLocationNameString());
                    break;
                }
                stl = action.getSpecialProperty();
                if (stl != null)
                    extra = stl.isExtra();
            }
            if (company.getBaseTokenLayCostMethod() == PublicCompany.BaseCostMethod.ROUTE_DISTANCE) {
                cost = action.getCost();
                if (cost > 0 && cost != company.getBaseTokenLayCostOnStop(stop)) {
                    errMsg = LocalText.getText("WrongCost", cost, company.getBaseTokenLayCostOnStop(stop));
                    break;
                }
            } else {
                cost = company.getBaseTokenLayCostOnHex(hex);
            }
            if (stl != null && stl.isFree())
                cost = 0;

            // Does the company have the money?
            if (cost > company.getCash()) {
                errMsg = LocalText.getText("NotEnoughMoney", companyName,
                        Bank.format(this,
                                company.getCash()),
                        Bank.format(this, cost));
                break;
            }
            break;
        }
        if (errMsg != null) {
            String msg = LocalText.getText("CannotLayBaseTokenOn", companyName,
                    hex.getId(), Bank.format(this, cost), errMsg);
            DisplayBuffer.add(this, msg);
            return false;
        }

        /* End of validation, start of execution */

        if (hex.layBaseToken(company, stop)) {
            /* TODO: the false return value must be impossible. */

            company.layBaseToken(hex, cost);

            if (action.getType() == LayBaseToken.HOME_CITY) {

                extra = true; // This is an extra action

                boolean normalLayPending = !tileLaysPerColour.isEmpty();
                boolean specialLayPending = !getSpecialTileLays(false).isEmpty();

                if (normalLayPending || specialLayPending) {

                    setStep(GameDef.OrStep.LAY_TRACK);
                    return true; // End action processing
                } else {

                }
            }

            StringBuilder text = new StringBuilder();
            if (action.isCorrection()) {
                text.append(LocalText.getText("CorrectionPrefix"));
            }
            if (cost > 0) {
                String costText = Currency.toBank(company, cost);
                text.append(LocalText.getText("LAYS_TOKEN_ON", companyName,
                        hex.getId(), Bank.format(this, cost)));
                // text.append(" ").append(stop.toText());
            } else {
                text.append(LocalText.getText("LAYS_FREE_TOKEN_ON",
                        companyName, hex.getId()));
            }
            ReportBuffer.add(this, text.toString());

            // Was a special property used?
            if (stl != null) {
                stl.setExercised();
                currentSpecialTokenLays.remove(action);
            }
            // Update the state to enforce the "one token per turn" limit for all games.
            // 'extra' is true for Special Properties or Home City lays.
            if (!extra) {
                normalTokenLaidThisTurn.set(true);
            }

            // Jump out if we aren't in the token laying step or it is a correction lay
            if (getStep() != GameDef.OrStep.LAY_TOKEN || action.isCorrection()) {
                return true;
            }

            if (currentNormalTokenLays.isEmpty()) {

            } else if (operatingCompany.value().getNumberOfFreeBaseTokens() == 0) {

                currentNormalTokenLays.clear();
            } else {
            }
            setSpecialTokenLays();

            // Can more tokens be laid? Otherwise, next step
            if (!canLayAnyTokens(false)) {

                nextStep();
            }

        }

        return true;
    }

    /**
     * Generates the list of trains the company can currently buy.
     * Populates the 'possibleActions' list.
     * * FIXED LOGIC:
     * 1. Strict Affordability: If not an emergency, hide trains > Company Cash.
     * 2. Minimum Price: Enforce minPrice >= 1 for all inter-company trades.
     * 3. Strict Phase Limits: Do not show options if "One Train Per Turn" limit is
     * reached.
     */
    public void setBuyableTrains() {
        PublicCompany company = operatingCompany.value();
        if (company == null)
            return;

        // --- 1. Setup Current State ---
        int companyCash = company.getCash();
        boolean hasTrains = company.getPortfolioModel().getNumberOfTrains() > 0;
        boolean canBuyTrainNow = canBuyTrainNow();

        // Forced Buy Condition: No trains AND company rules require ownership.
        boolean mustBuyTrain = !hasTrains && company.mustOwnATrain();

        // Check Phase Restrictions (e.g. "One train per turn")
        boolean canBuyMoreTrains = Phase.getCurrent(this).canBuyMoreTrainsPerTurn();
        boolean alreadyBought = !trainsBoughtThisTurn.isEmpty();

        // If we already bought a train, and the phase says "Only 1", and we aren't
        // forced to buy another...
        // STOP HERE. Do not generate any buy actions.
        if (alreadyBought && !canBuyMoreTrains && !mustBuyTrain) {
            doneAllowed.set(true);
            return;
        }

        // --- 2. Check New Trains (IPO/Bank) ---
        if (canBuyTrainNow || mustBuyTrain) {
            boolean mayBuyMoreOfEachType = Phase.getCurrent(this).canBuyMoreTrainsPerTypePerTurn();
            Set<Train> availableNewTrains = trainManager.getAvailableNewTrains();

            for (Train train : availableNewTrains) {
                if (!company.mayBuyTrainType(train))
                    continue;

                // Rule: Some games restrict buying multiple of same type in one turn
                if (!mayBuyMoreOfEachType && trainsBoughtThisTurn.contains(train.getCardType()))
                    continue;

                int price = train.getCost();

                // --- FILTER: STRICT AFFORDABILITY (IPO) ---
                // If this is NOT a forced emergency buy, the company MUST be able to pay.
                // We filter this out HERE so the AI never sees an unaffordable option.
                if (!mustBuyTrain && price > companyCash) {
                    continue;
                }

                // Create action
                BuyTrain action = new BuyTrain(train, train.getType(), ipo.getParent(), price, price,
                        PriceMode.VARIABLE);

                action.setForcedBuyIfHasRoute(mustBuyTrain);
                action.setButtonLabel(createTrainButtonLabel(train, ipo.getParent(), price, null, mustBuyTrain));
                possibleActions.add(action);
            }
        }

        // --- 3. Check Used Trains (Pool) ---
        if (canBuyTrainNow) {
            Set<Train> availableUsedTrains = pool.getUniqueTrains();
            for (Train train : availableUsedTrains) {
                if (!company.mayBuyTrainType(train))
                    continue;

                int price = train.getCost();

                // --- FILTER: STRICT AFFORDABILITY (POOL) ---
                if (!mustBuyTrain && price > companyCash) {
                    continue;
                }

                BuyTrain action = new BuyTrain(train, train.getType(), pool.getParent(), price, price,
                        PriceMode.VARIABLE);

                action.setForcedBuyIfHasRoute(mustBuyTrain);
                action.setButtonLabel(createTrainButtonLabel(train, pool.getParent(), price, null, false));
                possibleActions.add(action);
            }
        }

        // --- 4. Check Trains from Other Companies (Trading) ---
        if (Phase.getCurrent(this).isTrainTradingAllowed() && canBuyTrainNow) {
            Player currentPresident = company.getPresident();
            boolean restrictTradeToPresident = GameOption.getAsBoolean(this, "RestrictTrainTradingToPresident");

            for (PublicCompany otherCompany : companyManager.getAllPublicCompanies()) {
                // Skip invalid targets
                if (!otherCompany.hasFloated() || otherCompany.isClosed() || otherCompany == company)
                    continue;

                if (restrictTradeToPresident && otherCompany.getPresident() != currentPresident)
                    continue;

                // Enforce global parameter: Only allow trading if Presidents match
                if (gameManager.isRestrictTrainTradingToSameOwner()
                        && otherCompany.getPresident() != currentPresident) {
                    continue;
                }

                Set<Train> otherCompanyTrains = otherCompany.getPortfolioModel().getUniqueTrains();
                for (Train train : otherCompanyTrains) {
                    if (!company.mayBuyTrainType(train) || train.isObsolete() || !train.isTradeable())
                        continue;

                    boolean fixedPriceTrade = (otherCompany.getPresident() != currentPresident
                            && GameDef.getParmAsBoolean(this, GameDef.Parm.FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS))
                            || company.mustTradeTrainsAtFixedPrice()
                            || otherCompany.mustTradeTrainsAtFixedPrice();

                    BuyTrain action;
                    if (fixedPriceTrade) {
                        int price = train.getCost();

                        // --- FILTER: STRICT AFFORDABILITY (FIXED TRADE) ---
                        if (!mustBuyTrain && price > companyCash)
                            continue;

                        action = new BuyTrain(train, train.getType(), otherCompany, price, price, PriceMode.VARIABLE);
                        action.setButtonLabel(createTrainButtonLabel(train, otherCompany, price, null, false));

                    } else {
                        // Variable Price (Negotiation)
                        // We set a HARD FLOOR of 1. Buying for 0 is illegal.
                        int minPrice = 1;
                        int maxPrice = companyCash; // Cap at available cash

                        // --- FILTER: STRICT AFFORDABILITY (VARIABLE TRADE) ---
                        // If company has 0 cash, maxPrice is 0.
                        // Since minPrice is 1, the range is invalid [1..0].
                        // We MUST filter this out unless it's a forced buy.
                        if (!mustBuyTrain && maxPrice < minPrice) {
                            continue;
                        }

                        if (mustBuyTrain) {
                            maxPrice = Integer.MAX_VALUE; // President can add cash
                        }

                        // Sanity Check for forced buy if cash is 0
                        if (minPrice > maxPrice)
                            minPrice = maxPrice;

                        action = new BuyTrain(train, train.getType(), otherCompany, minPrice, maxPrice,
                                PriceMode.VARIABLE);
                        action.setForcedBuyIfHasRoute(mustBuyTrain);
                        action.setButtonLabel(createTrainButtonLabel(train, otherCompany, 0, null, mustBuyTrain));
                    }
                    possibleActions.add(action);
                }
            }
        }

        // --- 5. Set Done/Skip Status ---
        boolean buyOptionAvailable = false;
        for (PossibleAction pa : possibleActions.getList()) {
            if (pa instanceof BuyTrain) {
                buyOptionAvailable = true;
                break;
            }
        }

        if (!mustBuyTrain) {
            // Voluntary turn: Always allow Done.
            doneAllowed.set(true);
        } else {
            // Forced turn: Only allow Done if there are absolutely NO trains to buy
            doneAllowed.set(!buyOptionAvailable);
        }
    }

    public void cleanup() {
    }

    /**
     * Resets transient flags and safety sets the operating company to the first
     * available one.
     * This is primarily used during load to ensure a valid state before processing
     * actions.
     */
    public void resetTransientStateOnLoad() { // CHANGED from protected to public
        // 1. Reset Flags
        if (this.doneAllowed != null) {
            this.doneAllowed.set(false);
        }

        // 2. SAFE RESET: Set to the first available company.
        if (operatingCompanies != null && !operatingCompanies.isEmpty()) {
            boolean found = false;
            for (PublicCompany comp : operatingCompanies.view()) {
                if (!comp.isClosed() && comp.getPresident() != null) {
                    this.operatingCompany.set(comp);
                    found = true;
                    break;
                }
            }
            if (!found)
                this.operatingCompany.set(operatingCompanies.get(0));
        }

        // 3. Reset Step to ensure initTurn() runs when the real move starts
        setStep(GameDef.OrStep.INITIAL);
    }

    /**
     * AI Accessor: Re-hydrates the exact list of companies remaining to operate.
     */
    public void setOperatingCompanies_AI(List<PublicCompany> companies) {
        if (companies != null) {
            this.operatingCompanies.setTo(companies);
        }
    }

    /**
     * AI Accessor: Sets the active company pointer.
     */
    public void setOperatingCompany_AI(PublicCompany comp) {
        if (comp != null) {
            this.operatingCompany.set(comp);
        }
    }

    public int getBaseRevenueOnly(PublicCompany company) {
        return company.getLastRevenue(); // Fallback to model value
    }

    public int getSpecialRevenueOnly(PublicCompany company) {
        return 0;
    }

    // (Reminder of the correct helper in OperatingRound.java)
    public boolean checkAndGenerateDiscardActions(PublicCompany company) {
        // use the master function in Round.java
        return enforceTrainLimit(company);
    }

}
