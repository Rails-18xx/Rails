package rails.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import rails.common.*;
import rails.common.parser.GameOption;
import rails.game.action.*;
import rails.game.correct.ClosePrivate;
import rails.game.correct.OperatingCost;
import rails.game.model.CashOwner;
import rails.game.model.MoneyModel;
import rails.game.special.*;
import rails.game.state.*;
import rails.util.SequenceUtil;

/**
 * Implements a basic Operating Round. <p> A new instance must be created for
 * each new Operating Round. At the end of a round, the current instance should
 * be discarded.
 */
public class OperatingRound extends Round implements Observer {

    /* Transient memory (per round only) */
    protected final GenericState<GameDef.OrStep> stepObject = GenericState.create(
            this, "ORStep", GameDef.OrStep.INITIAL);

    protected boolean actionPossible = true;

    /* flag for using rails without map support */
    protected final boolean noMapMode;

    // TODO: Check if this should not be turned into a State?
    protected final List<PublicCompany> companiesOperatedThisRound
    = new ArrayList<PublicCompany> ();

    protected ArrayListState<PublicCompany> operatingCompanies = null; // will be created below

    protected final GenericState<PublicCompany> operatingCompany = GenericState.create(this, "operatingCompany") ;
    // do not use a operatingCompany.getObject() as reference
    // TODO: Question is this remark above still relevant?

    // Non-persistent lists (are recreated after each user action)
    protected List<SpecialProperty> currentSpecialProperties = null;

    protected final HashMapState<String, Integer> tileLaysPerColour = HashMapState.create(this, "tileLaysPerColour");

    protected final List<LayBaseToken> currentNormalTokenLays =
        new ArrayList<LayBaseToken>();

    protected final List<LayBaseToken> currentSpecialTokenLays =
        new ArrayList<LayBaseToken>();

    /** A List per player with owned companies that have excess trains */
    protected Map<Player, List<PublicCompany>> excessTrainCompanies = null;

    protected final List<TrainCertificateType> trainsBoughtThisTurn =
        new ArrayList<TrainCertificateType>(4);

    protected HashMapState<PublicCompany, Integer> loansThisRound = null;

    protected String thisOrNumber;

    protected PossibleAction selectedAction = null;

    protected PossibleAction savedAction = null;

    public static final int SPLIT_ROUND_DOWN = 2; // More to the treasury

    //      protected static GameDef.OrStep[] steps =
    protected GameDef.OrStep[] steps =
        new GameDef.OrStep[] {
            GameDef.OrStep.INITIAL,
            GameDef.OrStep.LAY_TRACK,
            GameDef.OrStep.LAY_TOKEN,
            GameDef.OrStep.CALC_REVENUE,
            GameDef.OrStep.PAYOUT,
            GameDef.OrStep.BUY_TRAIN,
            GameDef.OrStep.TRADE_SHARES,
            GameDef.OrStep.FINAL };

    protected boolean doneAllowed = false;

    protected TrainManager trainManager = gameManager.getTrainManager();

    /*=======================================
     *  1.  OR START and END
     *=======================================*/

    /**
     * Constructor with no parameters, call the super Class (Round's) Constructor with no parameters
     */
    protected OperatingRound(GameManager parent, String id) {
        super (parent, id);

        operatingCompanies = ArrayListState.create(this, "operatingCompanies", setOperatingCompanies());
        stepObject.addObserver(this);

        noMapMode = GameOption.convertValueToBoolean(getGameOption("NoMapMode"));

        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
        guiHints.setActivePanel(GuiDef.Panel.MAP);
    }
    
    public static OperatingRound create(GameManager parent, String id) {
        return new OperatingRound(parent, id);
    }
    
    public void start() {

        thisOrNumber = gameManager.getORId();

        ReportBuffer.add(LocalText.getText("START_OR", thisOrNumber));

        for (Player player : gameManager.getPlayers()) {
            player.setWorthAtORStart();
        }

        privatesPayOut();

        if (operatingCompanies.size() > 0) {

            StringBuilder msg = new StringBuilder();
            for (PublicCompany company : operatingCompanies.view()) {
                msg.append(",").append(company.getId());
            }
            if (msg.length() > 0) msg.deleteCharAt(0);
            log.info("Initial operating sequence is "+msg.toString());

            if (setNextOperatingCompany(true)){
                setStep(GameDef.OrStep.INITIAL);
            }
            return;
        }

        // No operating companies yet: close the round.
        String text = LocalText.getText("ShortORExecuted");
        ReportBuffer.add(text);
        DisplayBuffer.add(text);
        finishRound();
    }

    protected void privatesPayOut() {
        int count = 0;
        for (PrivateCompany priv : companyManager.getAllPrivateCompanies()) {
            if (!priv.isClosed()) {
                // The bank portfolios are all not cashOwners themselves!
                // TODO: Check if this still works as intended
                if (priv.getOwner() instanceof CashOwner) {
                    Owner recipient = priv.getOwner();
                    int revenue = priv.getRevenueByPhase(getCurrentPhase()); // sfy 1889: revenue by phase
                    if (count++ == 0) ReportBuffer.add("");
                    ReportBuffer.add(LocalText.getText("ReceivesFor",
                            recipient.getId(),
                            Bank.format(revenue),
                            priv.getId()));
                    MoneyModel.cashMove(bank, (CashOwner)recipient, revenue);
                }

            }
        }

    }

    @Override
    public void resume() {

        if (savedAction instanceof BuyTrain) {
            buyTrain ((BuyTrain)savedAction);
        } else if (savedAction instanceof SetDividend) {
            executeSetRevenueAndDividend ((SetDividend) savedAction);
        } else if (savedAction instanceof RepayLoans) {
            executeRepayLoans ((RepayLoans) savedAction);
        } else if (savedAction == null) {
            //nextStep();
        }
        savedAction = null;
        wasInterrupted.set(true);

        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
        guiHints.setActivePanel(GuiDef.Panel.MAP);
    }

    protected void finishOR() {

        // Check if any privates must be closed
        // (now only applies to 1856 W&SR) - no, that is at end of TURN
        //for (PrivateCompany priv : gameManager.getAllPrivateCompanies()) {
        //    priv.checkClosingIfExercised(true);
        //}

        ReportBuffer.add(" ");
        ReportBuffer.add(LocalText.getText("EndOfOperatingRound", thisOrNumber));

        // Update the worth increase per player
        int orWorthIncrease;
        for (Player player : gameManager.getPlayers()) {
            player.setLastORWorthIncrease();
            orWorthIncrease = player.getLastORWorthIncrease().value();
            ReportBuffer.add(LocalText.getText("ORWorthIncrease",
                    player.getId(),
                    thisOrNumber,
                    Bank.format(orWorthIncrease)));
        }

        // OR done. Inform GameManager.
        finishRound();
    }

    /*=======================================
     *  2.  CENTRAL PROCESSING FUNCTIONS
     *  2.1. PROCESS USER ACTION
     *=======================================*/

    @Override
    public boolean process(PossibleAction action) {

        boolean result = false;
        doneAllowed = false;

        /*--- Common OR checks ---*/
        /* Check operating company */
        if (action instanceof PossibleORAction
                && !(action instanceof DiscardTrain)) {
            PublicCompany company = ((PossibleORAction) action).getCompany();
            if (company != operatingCompany.get()) {
                DisplayBuffer.add(LocalText.getText("WrongCompany",
                        company.getId(),
                        operatingCompany.get().getId() ));
                return false;
            }
        }

        selectedAction = action;

        if (selectedAction instanceof LayTile) {

            result = layTile((LayTile) selectedAction);

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

            result = discardTrain((DiscardTrain) selectedAction);

        } else if (selectedAction instanceof BuyPrivate) {

            result = buyPrivate((BuyPrivate) selectedAction);

        } else if (selectedAction instanceof ReachDestinations) {

            result = reachDestinations ((ReachDestinations) selectedAction);

        } else if (selectedAction instanceof TakeLoans) {

            result = takeLoans((TakeLoans) selectedAction);

        } else if (selectedAction instanceof RepayLoans) {

            result = repayLoans((RepayLoans) selectedAction);

        } else if (selectedAction instanceof ExchangeTokens) {

            result = exchangeTokens ((ExchangeTokens)selectedAction, false); // 2nd parameter: unlinked moveset

        } else if (selectedAction instanceof ClosePrivate) {

            result = executeClosePrivate((ClosePrivate)selectedAction);

        } else if (selectedAction instanceof UseSpecialProperty
                && ((UseSpecialProperty)selectedAction).getSpecialProperty() instanceof SpecialRight) {

            result = buyRight ((UseSpecialProperty)selectedAction);

        } else if (selectedAction instanceof NullAction) {

            NullAction nullAction = (NullAction) action;
            switch (nullAction.getMode()) {
            case NullAction.DONE:
            case NullAction.PASS:
                result = done();
                break;
            case NullAction.SKIP:
                skip();
                result = true;
                break;
            }

        } else if (processGameSpecificAction(action)) {

            result = true;

        } else {

            DisplayBuffer.add(LocalText.getText("UnexpectedAction",
                    selectedAction.toString()));
            return false;
        }

        return result;
    }

    /** Stub, to be overridden in game-specific subclasses. */
    public boolean processGameSpecificAction(PossibleAction action) {
        return false;
    }

    /*=======================================
     *  2.2. PREPARE NEXT ACTION
     *=======================================*/

    /**
     * To be called after each change, to re-establish the currently allowed
     * actions. (new method, intended to absorb code from several other
     * methods).
     *
     */
    @Override
    public boolean setPossibleActions() {

        /* Create a new list of possible actions for the UI */
        possibleActions.clear();
        selectedAction = null;

        boolean forced = false;
        doneAllowed = false; // set default (fix of bug  2954654)

        if (getStep() == GameDef.OrStep.INITIAL) {
            initTurn();
            if (noMapMode) {
                nextStep (GameDef.OrStep.LAY_TOKEN);
            } else {
                initNormalTileLays(); // new: only called once per turn ?
                setStep (GameDef.OrStep.LAY_TRACK);
            }
        }

        GameDef.OrStep step = getStep();
        if (step == GameDef.OrStep.LAY_TRACK) {

            if (!operatingCompany.get().hasLaidHomeBaseTokens()) {
                // This can occur if the home hex has two cities and track,
                // such as the green OO tile #59
                                
                // BR: as this is a home token, need to call LayBaseToken with a MapHex, not a list
                // to avoid the LayBaseToken action from being a regular token lay
                // I am not sure that this will work with multiple home hexes.
                for (MapHex home : operatingCompany.get().getHomeHexes()) {
                    possibleActions.add(new LayBaseToken (home) );
                }
                forced = true;
            } else {
                possibleActions.addAll(getNormalTileLays(true));
                possibleActions.addAll(getSpecialTileLays(true));
                possibleActions.add(new NullAction(NullAction.SKIP));
            }

        } else if (step == GameDef.OrStep.LAY_TOKEN) {
            setNormalTokenLays();
            setSpecialTokenLays();
            log.debug("Normal token lays: " + currentNormalTokenLays.size());
            log.debug("Special token lays: " + currentSpecialTokenLays.size());

            possibleActions.addAll(currentNormalTokenLays);
            possibleActions.addAll(currentSpecialTokenLays);
            possibleActions.add(new NullAction(NullAction.SKIP));
        } else if (step == GameDef.OrStep.CALC_REVENUE) {
            prepareRevenueAndDividendAction();
            if (noMapMode)
                prepareNoMapActions();
        } else if (step == GameDef.OrStep.BUY_TRAIN) {
            setBuyableTrains();
            // TODO Need route checking here.
            // TEMPORARILY allow not buying a train if none owned
            //if (!operatingCompany.getObject().mustOwnATrain()
            //        || operatingCompany.getObject().getPortfolio().getNumberOfTrains() > 0) {
            doneAllowed = true;
            //}
            if (noMapMode && (operatingCompany.get().getLastRevenue() == 0))
                prepareNoMapActions();

        } else if (step == GameDef.OrStep.DISCARD_TRAINS) {

            forced = true;
            setTrainsToDiscard();
        }

        // The following additional "common" actions are only available if the
        // primary action is not forced.
        if (!forced) {

            setBonusTokenLays();

            setDestinationActions();

            setGameSpecificPossibleActions();

            // Private Company manually closure
            for (PrivateCompany priv: companyManager.getAllPrivateCompanies()) {
                if (!priv.isClosed() && priv.closesManually())
                    possibleActions.add(new ClosePrivate(priv));
            }

            // Can private companies be bought?
            if (isPrivateSellingAllowed()) {

                // Create a list of players with the current one in front
                int currentPlayerIndex = operatingCompany.get().getPresident().getIndex();
                Player player;
                int minPrice, maxPrice;
                List<Player> players = getPlayers();
                int numberOfPlayers = getNumberOfPlayers();
                for (int i = currentPlayerIndex; i < currentPlayerIndex
                + numberOfPlayers; i++) {
                    player = players.get(i % numberOfPlayers);
                    if (!maySellPrivate(player)) continue;
                    for (PrivateCompany privComp : player.getPortfolioModel().getPrivateCompanies()) {

                        // check to see if the private can be sold to a company
                        if (!privComp.tradeableToCompany()) {
                            continue;
                        }

                        minPrice = getPrivateMinimumPrice (privComp);

                        maxPrice = getPrivateMaximumPrice (privComp);

                        possibleActions.add(new BuyPrivate(privComp, minPrice,
                                maxPrice));
                    }
                }
            }

            if (operatingCompany.get().canUseSpecialProperties()) {

                // Are there any "common" special properties,
                // i.e. properties that are available to everyone?
                List<SpecialProperty> commonSP = gameManager.getCommonSpecialProperties();
                if (commonSP != null) {
                    SellBonusToken sbt;
                    loop:   for (SpecialProperty sp : commonSP) {
                        if (sp instanceof SellBonusToken) {
                            sbt = (SellBonusToken) sp;
                            // Can't buy if already owned
                            if (operatingCompany.get().getBonuses() != null) {
                                for (Bonus bonus : operatingCompany.get().getBonuses()) {
                                    if (bonus.getName().equals(sp.getId())) continue loop;
                                }
                            }
                            possibleActions.add (new BuyBonusToken (sbt));
                        }
                    }
                }

                // Are there other step-independent special properties owned by the company?
                List<SpecialProperty> orsps = operatingCompany.get().getPortfolioModel().getAllSpecialProperties();
        
                // TODO: Do we still need this directly from the operating company?
//                List<SpecialProperty> compsps = operatingCompany.get().getSpecialProperties();
//                if (compsps != null) orsps.addAll(compsps);

                if (orsps != null) {
                    for (SpecialProperty sp : orsps) {
                        if (!sp.isExercised() && sp.isUsableIfOwnedByCompany()
                                && sp.isUsableDuringOR(step)) {
                            if (sp instanceof SpecialTokenLay) {
                                if (getStep() != GameDef.OrStep.LAY_TOKEN) {
                                    possibleActions.add(new LayBaseToken((SpecialTokenLay)sp));
                                }
                            } else {
                                possibleActions.add(new UseSpecialProperty(sp));
                            }
                        }
                    }
                }
                // Are there other step-independent special properties owned by the president?
                orsps = getCurrentPlayer().getPortfolioModel().getAllSpecialProperties();
                if (orsps != null) {
                    for (SpecialProperty sp : orsps) {
                        if (!sp.isExercised() && sp.isUsableIfOwnedByPlayer()
                                && sp.isUsableDuringOR(step)) {
                            if (sp instanceof SpecialTokenLay) {
                                if (getStep() != GameDef.OrStep.LAY_TOKEN) {
                                    possibleActions.add(new LayBaseToken((SpecialTokenLay)sp));
                                }
                            } else {
                                possibleActions.add(new UseSpecialProperty(sp));
                            }
                        }
                    }
                }
            }
        }

        if (doneAllowed) {
            possibleActions.add(new NullAction(NullAction.DONE));
        }

        for (PossibleAction pa : possibleActions.getList()) {
            try {
                log.debug(operatingCompany.get().getId() + " may: " + pa.toString());
            } catch (Exception e) {
                log.error("Error in toString() of " + pa.getClass(), e);
            }
        }

        return true;
    }

    /** Stub, can be overridden by subclasses */
    protected void setGameSpecificPossibleActions() {

    }

    /*=======================================
     *  2.3. TURN CONTROL
     *=======================================*/

    protected void initTurn() {
        log.debug("Starting turn of "+operatingCompany.get().getId());
        ReportBuffer.add(" ");
        ReportBuffer.add(LocalText.getText("CompanyOperates",
                operatingCompany.get().getId(),
                operatingCompany.get().getPresident().getId()));
        setCurrentPlayer(operatingCompany.get().getPresident());

        if (noMapMode && !operatingCompany.get().hasLaidHomeBaseTokens()){
            // Lay base token in noMapMode
            BaseToken token = operatingCompany.get().getFreeToken();
            if (token == null) {
                log.error("Company " + operatingCompany.get().getId() + " has no free token to lay base token");
            } else {
                log.debug("Company " + operatingCompany.get().getId() + " lays base token in nomap mode");
                // FIXME: This has to be rewritten
                // Where are the nomap base tokens to be stored?
                // bank.getUnavailable().addBonusToken(token);
            }
        }
        operatingCompany.get().initTurn();
        trainsBoughtThisTurn.clear();
    }

    protected void finishTurn() {

        if (!operatingCompany.get().isClosed()) {
            operatingCompany.get().setOperated();
            companiesOperatedThisRound.add(operatingCompany.get());

            for (PrivateCompany priv : operatingCompany.get().getPortfolioModel().getPrivateCompanies()) {
                priv.checkClosingIfExercised(true);
            }
        }

        if (!finishTurnSpecials()) return;

        if (setNextOperatingCompany(false)) {
            setStep(GameDef.OrStep.INITIAL);
        } else {
            finishOR();
        }
    }

    /** Stub, may be overridden in subclasses
     * Return value:
     * TRUE = normal turn end;
     * FALSE = return immediately from finishTurn().
     */
    protected boolean finishTurnSpecials () {
        return true;
    }

    protected boolean setNextOperatingCompany(boolean initial) {

        while (true) {
            if (initial || operatingCompany.get() == null || operatingCompany == null) {
                setOperatingCompany(operatingCompanies.get(0));
                initial = false;
            } else {
                int index = operatingCompanies.indexOf(operatingCompany.get());
                if (++index >= operatingCompanies.size()) {
                    return false;
                }

                // Check if the operating order has changed
                List<PublicCompany> newOperatingCompanies
                = setOperatingCompanies (operatingCompanies.view(), operatingCompany.get());
                PublicCompany company;
                for (int i=0; i<newOperatingCompanies.size(); i++) {
                    company = newOperatingCompanies.get(i);
                    if (company != operatingCompanies.get(i)) {
                        log.debug("Company "+company.getId()
                                +" replaces "+operatingCompanies.get(i).getId()
                                +" in operating sequence");
                        operatingCompanies.move(company, i);
                    }
                }

                setOperatingCompany(operatingCompanies.get(index));
            }

            if (operatingCompany.get().isClosed()) continue;

            return true;
        }
    }

    protected void setOperatingCompany (PublicCompany company) {
        operatingCompany.set(company);
    }

    /**
     * Get the public company that has the turn to operate.
     *
     * @return The currently operating company object.
     */
    public PublicCompany getOperatingCompany() {
        return operatingCompany.get();
    }

    public List<PublicCompany> getOperatingCompanies() {
        return operatingCompanies.view();
    }

    public int getOperatingCompanyndex() {
        int index = operatingCompanies.indexOf(getOperatingCompany());
        return index;
    }


    /*=======================================
     *  2.4. STEP CONTROL
     *=======================================*/

    /**
     * Get the current operating round step (i.e. the next action).
     *
     * @return The number that defines the next action.
     */
    public GameDef.OrStep getStep() {
        return (GameDef.OrStep) stepObject.get();
    }

    /**
     * Bypass normal order of operations and explicitly set round step. This
     * should only be done for specific rails.game exceptions, such as forced
     * train purchases.
     *
     * @param step
     */
    protected void setStep(GameDef.OrStep step) {

        stepObject.set(step);

    }

    /**
     * Internal method: change the OR state to the next step. If the currently
     * Operating Company is done, notify this.
     *
     * @param company The current company.
     */
    protected void nextStep() {
        nextStep(getStep());
    }

    /** Take the next step after a given one (see nextStep()) */
    protected void nextStep(GameDef.OrStep step) {

        PublicCompany company = operatingCompany.get();

        // Cycle through the steps until we reach one where a user action is
        // expected.
        int stepIndex;
        for (stepIndex = 0; stepIndex < steps.length; stepIndex++) {
            if (steps[stepIndex] == step) break;
        }
        while (++stepIndex < steps.length) {
            step = steps[stepIndex];
            log.debug("Step " + step);

            if (step == GameDef.OrStep.LAY_TOKEN
                    && company.getNumberOfFreeBaseTokens() == 0) {
                continue;
            }

            if (step == GameDef.OrStep.CALC_REVENUE) {

                if (!company.canRunTrains()) {
                    // No trains, then the revenue is zero.
                    executeSetRevenueAndDividend (
                            new SetDividend (0, false, new int[] {SetDividend.NO_TRAIN}));
                    // TODO: This probably does not handle share selling correctly
                    continue;
                }
            }

            if (step == GameDef.OrStep.PAYOUT) {
                // This step is now obsolete
                continue;
            }

            if (step == GameDef.OrStep.TRADE_SHARES) {

                // Is company allowed to trade trasury shares?
                if (!company.mayTradeShares()
                        || !company.hasOperated()) {
                    continue;
                }

                /* Check if any trading is possible.
                 * If not, skip this step.
                 * (but register a Done action for BACKWARDS COMPATIBILITY only)
                 */
                // Preload some expensive results
                int ownShare = company.getPortfolioModel().getShare(company);
                int poolShare = pool.getShare(company); // Expensive, do it once
                // Can it buy?
                boolean canBuy =
                    ownShare < getGameParameterAsInt (GameDef.Parm.TREASURY_SHARE_LIMIT)
                    && company.getCash() >= company.getCurrentSpace().getPrice()
                    && poolShare > 0;
                    // Can it sell?
                    boolean canSell =
                        company.getPortfolioModel().getShare(company) > 0
                        && poolShare < getGameParameterAsInt (GameDef.Parm.POOL_SHARE_LIMIT);
                        // Above we ignore the possible existence of double shares (as in 1835).

                        if (!canBuy && !canSell) {
                            // XXX For BACKWARDS COMPATIBILITY only,
                            // register a Done skip action during reloading.
                            if (gameManager.isReloading()) {
                                gameManager.setSkipDone(GameDef.OrStep.TRADE_SHARES);
                                log.debug("If the next saved action is 'Done', skip it");
                            }
                            log.info("Skipping Treasury share trading step");
                            continue;
                        }

                        gameManager.startTreasuryShareTradingRound();

            }

            if (!gameSpecificNextStep (step)) continue;

            // No reason found to skip this step
            break;
        }

        if (step == GameDef.OrStep.FINAL) {
            finishTurn();
        } else {
            setStep(step);
        }

    }

    /** Stub, can be overridden in subclasses to check for extra steps */
    protected boolean gameSpecificNextStep (GameDef.OrStep step) {
        return true;
    }

    /**
     * This method is only called at the start of each step (unlike
     * updateStatus(), which is called after each user action)
     */
    protected void prepareStep() {
        GameDef.OrStep step = stepObject.get();

        if (step == GameDef.OrStep.LAY_TRACK) {
            //            getNormalTileLays();
        } else if (step == GameDef.OrStep.LAY_TOKEN) {

        } else {
            currentSpecialProperties = null;
        }

    }

    /*=======================================
     *  3.  COMMON ACTIONS (not bound to steps)
     *  3.1.   NOOPS
     *=======================================*/

    public void skip() {
        log.debug("Skip step " + stepObject.get());
        // TODO: Check if this is ok
        // FIXME: changeStack.start(true);
        nextStep();
    }

    /**
     * The current Company is done operating.
     *
     * @param company Name of the company that finished operating.
     * @return False if an error is found.
     */
    public boolean done() {

        if (operatingCompany.get().getPortfolioModel().getNumberOfTrains() == 0
                && operatingCompany.get().mustOwnATrain()) {
            // FIXME: Need to check for valid route before throwing an
            // error.
            /* Check TEMPORARILY disabled
            errMsg =
                    LocalText.getText("CompanyMustOwnATrain",
                            operatingCompany.getObject().getName());
            setStep(STEP_BUY_TRAIN);
            DisplayBuffer.add(errMsg);
            return false;
             */
        }

        // TODO: changeStack.start(false);

        nextStep();

        if (getStep() == GameDef.OrStep.FINAL) {
            finishTurn();
        }

        return true;
    }

    /*=======================================
     *  3.2.   DISCARDING TRAINS
     *=======================================*/

    public boolean discardTrain(DiscardTrain action) {

        Train train = action.getDiscardedTrain();
        PublicCompany company = action.getCompany();
        String companyName = company.getId();

        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct step
            if (getStep() != GameDef.OrStep.BUY_TRAIN
                    && getStep() != GameDef.OrStep.DISCARD_TRAINS) {
                errMsg = LocalText.getText("WrongActionNoDiscardTrain");
                break;
            }

            if (train == null && action.isForced()) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Does the company own such a train?

            if (!company.getPortfolioModel().getTrainList().contains(train)) {
                errMsg =
                    LocalText.getText("CompanyDoesNotOwnTrain",
                            company.getId(),
                            train.getId() );
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotDiscardTrain",
                    companyName,
                    (train != null ?train.getId() : "?"),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        // TODO: changeStack.start(true);
        
        // FIXME: if (action.isForced()) changeStack.linkToPreviousMoveSet();

        // Reset type of dual trains
        if (train.getCertType().getPotentialTrainTypes().size() > 1) {
            train.setType(null);
        }

        (train.isObsolete() ? scrapHeap : pool).addTrain(train);
        ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                companyName,
                train.getId() ));

        // Check if any more companies must discard trains,
        // otherwise continue train buying
        if (!checkForExcessTrains()) {
            // Trains may have been discarded by other players
            setCurrentPlayer (operatingCompany.get().getPresident());
            stepObject.set(GameDef.OrStep.BUY_TRAIN);
        }

        //setPossibleActions();

        return true;
    }

    protected void setTrainsToDiscard() {

        // Scan the players in SR sequence, starting with the current player
        Player player;
        List<PublicCompany> list;
        int currentPlayerIndex = getCurrentPlayerIndex();
        for (int i = currentPlayerIndex; i < currentPlayerIndex
        + getNumberOfPlayers(); i++) {
            player = gameManager.getPlayerByIndex(i);
            if (excessTrainCompanies.containsKey(player)) {
                setCurrentPlayer(player);
                list = excessTrainCompanies.get(player);
                for (PublicCompany comp : list) {
                    possibleActions.add(new DiscardTrain(comp,
                            comp.getPortfolioModel().getUniqueTrains(), true));
                    // We handle one company at at time.
                    // We come back here until all excess trains have been
                    // discarded.
                    return;
                }
            }
        }
    }
    /*=======================================
     *  3.3.   PRIVATES (BUYING, SELLING, CLOSING)
     *=======================================*/

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
            if ((privateCompany =
                companyManager.getPrivateCompany(
                        privateCompanyName)) == null) {
                errMsg =
                    LocalText.getText("PrivateDoesNotExist",
                            privateCompanyName);
                break;
            }
            // Is private still open?
            if (privateCompany.isClosed()) {
                errMsg =
                    LocalText.getText("PrivateIsAlreadyClosed",
                            privateCompanyName);
                break;
            }
            // Is private owned by a player?
            owner = privateCompany.getOwner();
            if (!(owner instanceof Player)) {
                errMsg =
                    LocalText.getText("PrivateIsNotOwnedByAPlayer",
                            privateCompanyName);
                break;
            }
            player = (Player) owner;
            upperPrice = privateCompany.getUpperPrice();
            lowerPrice = privateCompany.getLowerPrice();

            // Is private buying allowed?
            if (!isPrivateSellingAllowed()) {
                errMsg = LocalText.getText("PrivateBuyingIsNotAllowed");
                break;
            }

            // Price must be in the allowed range
            if (lowerPrice != PrivateCompany.NO_PRICE_LIMIT && price < lowerPrice) {
                errMsg =
                    LocalText.getText("PriceBelowLowerLimit",
                            Bank.format(price),
                            Bank.format(lowerPrice),
                            privateCompanyName );
                break;
            }
            if (upperPrice != PrivateCompany.NO_PRICE_LIMIT && price > upperPrice) {
                errMsg =
                    LocalText.getText("PriceAboveUpperLimit",
                            Bank.format(price),
                            Bank.format(lowerPrice),
                            privateCompanyName );
                break;
            }
            // Does the company have the money?
            if (price > operatingCompany.get().getCash()) {
                errMsg =
                    LocalText.getText("NotEnoughMoney",
                            publicCompanyName,
                            Bank.format(operatingCompany.get().getCash()),
                            Bank.format(price) );
                break;
            }
            break;
        }
        if (errMsg != null) {
            if (owner != null) {
                DisplayBuffer.add(LocalText.getText("CannotBuyPrivateFromFor",
                        publicCompanyName,
                        privateCompanyName,
                        owner.getId(),
                        Bank.format(price),
                        errMsg ));
            } else {
                DisplayBuffer.add(LocalText.getText("CannotBuyPrivateFor",
                        publicCompanyName,
                        privateCompanyName,
                        Bank.format(price),
                        errMsg ));
            }
            return false;
        }

        // TODO: changeStack.start(true);

        operatingCompany.get().buyPrivate(privateCompany, player.getPortfolioModel(),
                price);

        return true;

    }

    protected boolean isPrivateSellingAllowed() {
        return getCurrentPhase().isPrivateSellingAllowed();
    }

    protected int getPrivateMinimumPrice (PrivateCompany privComp) {
        int minPrice = privComp.getLowerPrice();
        if (minPrice == PrivateCompany.NO_PRICE_LIMIT) {
            minPrice = 0;
        }
        return minPrice;
    }

    protected int getPrivateMaximumPrice (PrivateCompany privComp) {
        int maxPrice = privComp.getUpperPrice();
        if (maxPrice == PrivateCompany.NO_PRICE_LIMIT) {
            maxPrice = operatingCompany.get().getCash();
        }
        return maxPrice;
    }

    protected boolean maySellPrivate (Player player) {
        return true;
    }

    protected boolean executeClosePrivate(ClosePrivate action) {

        PrivateCompany priv = action.getPrivateCompany();

        log.debug("Executed close private action for private " + priv.getId());

        String errMsg = null;

        if (priv.isClosed())
            errMsg = LocalText.getText("PrivateAlreadyClosed", priv.getId());

        if (errMsg != null) {
            DisplayBuffer.add(errMsg);
            return false;
        }

        // TODO: changeStack.start(true);

        priv.setClosed();

        return true;
    }

    /*=======================================
     *  3.4.  DESTINATIONS
     *=======================================*/

    /** Stub for applying any follow-up actions when
     * a company reaches it destinations.
     * Default version: no actions.
     * @param company
     */
    protected void reachDestination (PublicCompany company) {

    }

    public boolean reachDestinations (ReachDestinations action) {

        List<PublicCompany> destinedCompanies
        = action.getReachedCompanies();
        if (destinedCompanies != null) {
            for (PublicCompany company : destinedCompanies) {
                if (company.hasDestination()
                        && !company.hasReachedDestination()) {
                    // TODO: if (!changeStack.isOpen()) changeStack.start(true);
                    company.setReachedDestination(true);
                    ReportBuffer.add(LocalText.getText("DestinationReached",
                            company.getId(),
                            company.getDestinationHex().getId()
                    ));
                    // Process any consequences of reaching a destination
                    // (default none)
                    reachDestination (company);
                }
            }
        }
        return true;
    }

    /**
     * This is currently a stub, as it is unclear if there is a common
     * rule for setting destination reaching options.
     * See OperatingRound_1856 for a first implementation
     * of such rules.
     */
    protected void setDestinationActions () {

    }

    /*=======================================
     *  3.5.  LOANS
     *=======================================*/

    protected boolean takeLoans (TakeLoans action) {

        String errMsg = validateTakeLoans (action);

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotTakeLoans",
                    action.getCompanyName(),
                    action.getNumberTaken(),
                    Bank.format(action.getPrice()),
                    errMsg));

            return false;
        }

        // TODO: changeStack.start(true);

        executeTakeLoans (action);

        return true;

    }

    protected String validateTakeLoans (TakeLoans action) {

        String errMsg = null;
        PublicCompany company = action.getCompany();
        String companyName = company.getId();
        int number = action.getNumberTaken();

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            // Is company operating?
            if (company != operatingCompany.get()) {
                errMsg =
                    LocalText.getText("WrongCompany",
                            companyName,
                            action.getCompanyName());
                break;
            }
            // Does company allow any loans?
            if (company.getMaxNumberOfLoans() == 0) {
                errMsg = LocalText.getText("LoansNotAllowed",
                        companyName);
                break;
            }
            // Does the company exceed the maximum number of loans?
            if (company.getMaxNumberOfLoans() > 0
                    && company.getCurrentNumberOfLoans() + number >
            company.getMaxNumberOfLoans()) {
                errMsg =
                    LocalText.getText("MoreLoansNotAllowed",
                            companyName,
                            company.getMaxNumberOfLoans());
                break;
            }
            break;
        }

        return errMsg;
    }

    protected void executeTakeLoans (TakeLoans action) {

        int number = action.getNumberTaken();
        int amount = calculateLoanAmount (number);
        operatingCompany.get().addLoans(number);
        MoneyModel.cashMove (bank, operatingCompany.get(), amount);
        if (number == 1) {
            ReportBuffer.add(LocalText.getText("CompanyTakesLoan",
                    operatingCompany.get().getId(),
                    Bank.format(operatingCompany.get().getValuePerLoan()),
                    Bank.format(amount)
            ));
        } else {
            ReportBuffer.add(LocalText.getText("CompanyTakesLoans",
                    operatingCompany.get().getId(),
                    number,
                    Bank.format(operatingCompany.get().getValuePerLoan()),
                    Bank.format(amount)
            ));
        }

        if (operatingCompany.get().getMaxLoansPerRound() > 0) {
            int oldLoansThisRound = 0;
            if (loansThisRound == null) {
                loansThisRound = HashMapState.create(this, "loansThisRound");
            } else if (loansThisRound.containsKey(operatingCompany.get())){
                oldLoansThisRound = loansThisRound.get(operatingCompany.get());
            }
            loansThisRound.put(operatingCompany.get(),
                    new Integer (oldLoansThisRound + number));
        }
    }

    protected boolean repayLoans (RepayLoans action) {

        String errMsg = validateRepayLoans (action);

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotRepayLoans",
                    action.getCompanyName(),
                    action.getNumberRepaid(),
                    Bank.format(action.getPrice()),
                    errMsg));

            return false;
        }

        int repayment = action.getNumberRepaid() * operatingCompany.get().getValuePerLoan();
        if (repayment > 0 && repayment > operatingCompany.get().getCash()) {
            // President must contribute
            int remainder = repayment - operatingCompany.get().getCash();
            Player president = operatingCompany.get().getPresident();
            int presCash = president.getCashValue();
            if (remainder > presCash) {
                // Start a share selling round
                int cashToBeRaisedByPresident = remainder - presCash;
                log.info("A share selling round must be started as the president cannot pay $"
                        + remainder + " loan repayment");
                log.info("President has $"+presCash+", so $"+cashToBeRaisedByPresident+" must be added");
                savedAction = action;
                // TODO: changeStack.start(true);
                gameManager.startShareSellingRound(operatingCompany.get().getPresident(),
                        cashToBeRaisedByPresident, operatingCompany.get(), false);
                return true;
            }
        }

        // TODO: changeStack.start(true);

        if (repayment > 0) executeRepayLoans (action);

        return true;
    }

    protected String validateRepayLoans (RepayLoans action) {

        String errMsg = null;

        return errMsg;
    }

    protected void executeRepayLoans (RepayLoans action) {

        int number = action.getNumberRepaid();
        int payment;
        int remainder = 0;

        operatingCompany.get().addLoans(-number);
        int amount = number * operatingCompany.get().getValuePerLoan();
        payment = Math.min(amount, operatingCompany.get().getCash());
        remainder = amount - payment;
        if (payment > 0) {
            MoneyModel.cashMove (operatingCompany.get(), bank, payment);
            ReportBuffer.add (LocalText.getText("CompanyRepaysLoans",
                    operatingCompany.get().getId(),
                    Bank.format(payment),
                    Bank.format(amount),
                    number,
                    Bank.format(operatingCompany.get().getValuePerLoan())));
        }
        if (remainder > 0) {
            Player president = operatingCompany.get().getPresident();
            if (president.getCashValue() >= remainder) {
                payment = remainder;
                MoneyModel.cashMove (president, bank, payment);
                ReportBuffer.add (LocalText.getText("CompanyRepaysLoansWithPresCash",
                        operatingCompany.get().getId(),
                        Bank.format(payment),
                        Bank.format(amount),
                        number,
                        Bank.format(operatingCompany.get().getValuePerLoan()),
                        president.getId()));
            }
        }
    }

    protected int calculateLoanAmount (int numberOfLoans) {
        return numberOfLoans * operatingCompany.get().getValuePerLoan();
    }

    // TODO UNUSED??
    public void payLoanInterest () {
        int amount = operatingCompany.get().getCurrentLoanValue()
        * operatingCompany.get().getLoanInterestPct() / 100;
        MoneyModel.cashMove (operatingCompany.get(), bank, amount);
        DisplayBuffer.add(LocalText.getText("CompanyPaysLoanInterest",
                operatingCompany.get().getId(),
                Bank.format(amount),
                operatingCompany.get().getLoanInterestPct(),
                operatingCompany.get().getCurrentNumberOfLoans(),
                Bank.format(operatingCompany.get().getValuePerLoan())));
    }

    /*=======================================
     *  3.6.  RIGHTS
     *=======================================*/

    protected boolean buyRight (UseSpecialProperty action) {

        String errMsg = null;

        SpecialProperty sp = action.getSpecialProperty();
        if (!(sp instanceof SpecialRight)) {
            errMsg = "Wrong right property class: "+sp.toString();
        }

        SpecialRight right = (SpecialRight) sp;
        String rightName = right.getId();
        String rightValue = right.getValue();

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotBuyRight",
                    action.getCompanyName(),
                    rightName,
                    Bank.format(right.getCost()),
                    errMsg));

            return false;
        }

        // TODO: changeStack.start(true);

        operatingCompany.get().setRight(rightName, rightValue);
        MoneyModel.cashMove (operatingCompany.get(), bank, right.getCost());

        ReportBuffer.add(LocalText.getText("BuysRight",
                operatingCompany.get().getId(),
                rightName,
                Bank.format(right.getCost())));

        sp.setExercised();

        return true;
    }

    /*=======================================
     *  4.   LAYING TILES
     *=======================================*/

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

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct company.
            if (!companyName.equals(operatingCompany.get().getId())) {
                errMsg =
                    LocalText.getText("WrongCompany",
                            companyName,
                            operatingCompany.get().getId() );
                break;
            }
            // Must be correct step
            if (getStep() != GameDef.OrStep.LAY_TRACK) {
                errMsg = LocalText.getText("WrongActionNoTileLay");
                break;
            }

            if (tile == null) break;

            if (!getCurrentPhase().isTileColourAllowed(tile.getColourName())) {
                errMsg =
                    LocalText.getText("TileNotYetAvailable",
                            tile.getExternalId());
                break;
            }
            if (tile.countFreeTiles() == 0) {
                errMsg =
                    LocalText.getText("TileNotAvailable",
                            tile.getExternalId());
                break;
            }

            /*
             * Check if the current tile is allowed via the LayTile allowance.
             * (currently the set if tiles is always null, which means that this
             * check is redundant. This may change in the future.
             */
            if (action != null) {
                List<Tile> tiles = action.getTiles();
                if (tiles != null && !tiles.isEmpty() && !tiles.contains(tile)) {
                    errMsg =
                        LocalText.getText(
                                "TileMayNotBeLaidInHex",
                                tile.getExternalId(),
                                hex.getId() );
                    break;
                }
                stl = action.getSpecialProperty();
                if (stl != null) extra = stl.isExtra();
            }

            /*
             * If this counts as a normal tile lay, check if the allowed number
             * of normal tile lays is not exceeded.
             */
            if (!extra && !validateNormalTileLay(tile)) {
                errMsg =
                    LocalText.getText("NumberOfNormalTileLaysExceeded",
                            tile.getColourName());
                break;
            }

            // Sort out cost
            if (stl != null && stl.isFree()) {
                cost = 0;
            } else {
                cost = hex.getTileCost();
            }

            // Amount must be non-negative multiple of 10
            if (cost < 0) {
                errMsg =
                    LocalText.getText("NegativeAmountNotAllowed",
                            Bank.format(cost));
                break;
            }
            if (cost % 10 != 0) {
                errMsg =
                    LocalText.getText("AmountMustBeMultipleOf10",
                            Bank.format(cost));
                break;
            }
            // Does the company have the money?
            if (cost > operatingCompany.get().getCash()) {
                errMsg =
                    LocalText.getText("NotEnoughMoney",
                            companyName,
                            Bank.format(operatingCompany.get().getCash()),
                            Bank.format(cost) );
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotLayTileOn",
                    companyName,
                    tile.getExternalId(),
                    hex.getId(),
                    Bank.format(cost),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        // TODO: changeStack.start(true);

        if (tile != null) {
            if (cost > 0)
                MoneyModel.cashMove(operatingCompany.get(), bank, cost);
            operatingCompany.get().layTile(hex, tile, orientation, cost);

            if (cost == 0) {
                ReportBuffer.add(LocalText.getText("LaysTileAt",
                        companyName,
                        tile.getExternalId(),
                        hex.getId(),
                        hex.getOrientationName(orientation)));
            } else {
                ReportBuffer.add(LocalText.getText("LaysTileAtFor",
                        companyName,
                        tile.getExternalId(),
                        hex.getId(),
                        hex.getOrientationName(orientation),
                        Bank.format(cost) ));
            }
            hex.upgrade(action);

            // Was a special property used?
            if (stl != null) {
                stl.setExercised();
                //currentSpecialTileLays.remove(action);
                log.debug("This was a special tile lay, "
                        + (extra ? "" : " not") + " extra");

            }
            if (!extra) {
                log.debug("This was a normal tile lay");
                registerNormalTileLay(tile);
            }
        }

        if (tile == null || !areTileLaysPossible()) {
            nextStep();
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

        String colour = tile.getColourName();
        Integer oldAllowedNumberObject = tileLaysPerColour.get(colour);

        if (oldAllowedNumberObject == null) return false;

        int oldAllowedNumber = oldAllowedNumberObject.intValue();
        if (oldAllowedNumber <= 0) return false;

        if (update) updateAllowedTileColours(colour, oldAllowedNumber);
        return true;
    }

    /*
     * We will assume that in all cases the following assertions hold: 1. If
     * the allowed number for the colour of the just laid tile reaches zero,
     * all normal tile lays have been consumed. 2. If any colour is laid, no
     * different colours may be laid. THIS MAY NOT BE TRUE FOR ALL GAMES!
     */

    protected void updateAllowedTileColours (String colour, int oldAllowedNumber) {

        if (oldAllowedNumber <= 1) {
            tileLaysPerColour.clear();
            log.debug("No more normal tile lays allowed");
            //currentNormalTileLays.clear();// Shouldn't be needed anymore ??
        } else {
            List<String> coloursToRemove = new ArrayList<String>();
            for (String key:tileLaysPerColour.viewKeySet()) {
                if (colour.equals(key)) {
                    tileLaysPerColour.put(key, oldAllowedNumber-1);
                } else  {
                    coloursToRemove.add(key);
                }
            }
            // Two-step removal to prevent ConcurrentModificatioonException.
            for (String key : coloursToRemove) {
                tileLaysPerColour.remove(key);
            }
            log.debug((oldAllowedNumber - 1) + " additional " + colour
                    + " tile lays allowed; no other colours");
        }
    }

    /**
     * Create a List of allowed normal tile lays (see LayTile class). This
     * method should be called only once per company turn in an OR: at the start
     * of the tile laying step.
     */
    protected void initNormalTileLays() {

        // duplicate the phase colours
        Map<String, Integer> newTileColours = new HashMap<String, Integer>(getCurrentPhase().getTileColours());
        for (String colour : newTileColours.keySet()) {
            int allowedNumber = operatingCompany.get().getNumberOfTileLays(colour);
            // Replace the null map value with the allowed number of lays
            newTileColours.put(colour, new Integer(allowedNumber));
        }
        // store to state
        tileLaysPerColour.initFromMap(newTileColours);
    }

    protected List<LayTile> getNormalTileLays(boolean display) {

        /* Normal tile lays */
        List<LayTile> currentNormalTileLays = new ArrayList<LayTile>();

        // Check which colours can still be laid
        Map <String, Integer> remainingTileLaysPerColour = new HashMap<String, Integer>();

        int lays = 0;
        for (String colourName: tileLaysPerColour.viewKeySet()) {
            lays = tileLaysPerColour.get(colourName);
            if (lays != 0) {
                remainingTileLaysPerColour.put(colourName, lays);
            }
        }
        if (!remainingTileLaysPerColour.isEmpty()) {
            currentNormalTileLays.add(new LayTile(remainingTileLaysPerColour));
        }

        // NOTE: in a later stage tile lays will be specified per hex or set of hexes.

        if (display) {
            int size = currentNormalTileLays.size();
            if (size == 0) {
                log.debug("No normal tile lays");
            } else {
                for (LayTile tileLay : currentNormalTileLays) {
                    log.debug("Normal tile lay: " + tileLay.toString());
                }
            }
        }
        return currentNormalTileLays;
    }

    /**
     * Create a List of allowed special tile lays (see LayTile class). This
     * method should be called before each user action in the tile laying step.
     */
    protected List<LayTile> getSpecialTileLays(boolean display) {

        /* Special-property tile lays */
        List<LayTile> currentSpecialTileLays = new ArrayList<LayTile>();

        if (operatingCompany.get().canUseSpecialProperties()) {

            for (SpecialTileLay stl : getSpecialProperties(SpecialTileLay.class)) {
                if (stl.isExtra()
                        // If the special tile lay is not extra, it is only allowed if
                        // normal tile lays are also (still) allowed
                        || checkNormalTileLay(stl.getTile(), false)) {
                    LayTile lt = new LayTile(stl);
                    String[] stlc = stl.getTileColours();
                    if (stlc != null) {
                        Map<String, Integer> tc = new HashMap<String, Integer>();
                        for (String c : stlc) {
                            tc.put(c, 1);
                        }
                        lt.setTileColours(tc);
                    }
                    currentSpecialTileLays.add(lt);
                }
            }
        }

        if (display) {
            int size = currentSpecialTileLays.size();
            if (size == 0) {
                log.debug("No special tile lays");
            } else {
                for (LayTile tileLay : currentSpecialTileLays) {
                    log.debug("Special tile lay: " + tileLay.toString());
                }
            }
        }

        return currentSpecialTileLays;
    }

    protected boolean areTileLaysPossible() {
        return !tileLaysPerColour.isEmpty()
        || !getSpecialTileLays(false).isEmpty();
    }

    /** Reports if a tile lay is allowed by a certain company on a certain hex
     * <p>
     * This method can be used both in restricting possible actions
     * and in validating submitted actions.
     * <p> Currently, only a few standard checks are included.
     * This method can be extended to perform other generic checks,
     * such as if a route exists,
     * and possibly in subclasses for game-specific checks.
     * @param company The company laying a tile.
     * @param hex The hex on which a tile is laid.
     * @param orientation The orientation in which the tile is laid (-1 is any).
     */
    protected boolean isTileLayAllowed (PublicCompany company,
            MapHex hex, int orientation) {
        return !hex.isBlockedForTileLays();
    }

    /*=======================================
     *  5.  TOKEN LAYING
     *  5.1.  BASE TOKENS
     *=======================================*/

    public boolean layBaseToken(LayBaseToken action) {

        String errMsg = null;
        int cost = 0;
        SpecialTokenLay stl = null;
        boolean extra = false;

        MapHex hex = action.getChosenHex();
        int station = action.getChosenStation();
        String companyName = operatingCompany.get().getId();

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            // Must be correct step (exception: home base lay & some special token lay)
            if (getStep() != GameDef.OrStep.LAY_TOKEN
                    && action.getType() != LayBaseToken.HOME_CITY
                    && action.getType() != LayBaseToken.SPECIAL_PROPERTY) {
                errMsg = LocalText.getText("WrongActionNoTokenLay");
                break;
            }

            if (operatingCompany.get().getNumberOfFreeBaseTokens() == 0) {
                errMsg = LocalText.getText("HasNoTokensLeft", companyName);
                break;
            }

            if (!isTokenLayAllowed (operatingCompany.get(), hex, station)) {
                errMsg = LocalText.getText("BaseTokenSlotIsReserved");
                break;
            }

            if (!hex.hasTokenSlotsLeft(station)) {
                errMsg = LocalText.getText("CityHasNoEmptySlots");
                break;
            }

            /*
             * TODO: the below condition holds for 1830. in some games, separate
             * cities on one tile may hold tokens of the same company; this case
             * is not yet covered.
             */
            if (hex.hasTokenOfCompany(operatingCompany.get())) {
                errMsg =
                    LocalText.getText("TileAlreadyHasToken",
                            hex.getId(),
                            companyName );
                break;
            }

            if (action != null) {
                List<MapHex> locations = action.getLocations();
                if (locations != null && locations.size() > 0
                        && !locations.contains(hex) && !locations.contains(null)) {
                    errMsg =
                        LocalText.getText("TokenLayingHexMismatch",
                                hex.getId(),
                                action.getLocationNameString() );
                    break;
                }
                stl = action.getSpecialProperty();
                if (stl != null) extra = stl.isExtra();
            }

            cost = operatingCompany.get().getBaseTokenLayCost(hex);
            if (stl != null && stl.isFree()) cost = 0;

            // Does the company have the money?
            if (cost > operatingCompany.get().getCash()) {
                errMsg = LocalText.getText("NotEnoughMoney",
                        companyName,
                        Bank.format(operatingCompany.get().getCash()),
                        Bank.format(cost));
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotLayBaseTokenOn",
                    companyName,
                    hex.getId(),
                    Bank.format(cost),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        // TODO: changeStack.start(true);

        if (hex.layBaseToken(operatingCompany.get(), station)) {
            /* TODO: the false return value must be impossible. */

            operatingCompany.get().layBaseToken(hex, cost);

            // If this is a home base token lay, stop here
            if (action.getType() == LayBaseToken.HOME_CITY) {
                return true;
            }

            if (cost > 0) {
                MoneyModel.cashMove(operatingCompany.get(), bank, cost);
                ReportBuffer.add(LocalText.getText("LAYS_TOKEN_ON",
                        companyName,
                        hex.getId(),
                        Bank.format(cost) ));
            } else {
                ReportBuffer.add(LocalText.getText("LAYS_FREE_TOKEN_ON",
                        companyName,
                        hex.getId() ));
            }

            // Was a special property used?
            if (stl != null) {
                stl.setExercised();
                currentSpecialTokenLays.remove(action);
                log.debug("This was a special token lay, "
                        + (extra ? "" : " not") + " extra");

            }

            // Jump out if we aren't in the token laying step
            if (getStep() != GameDef.OrStep.LAY_TOKEN) return true;

            if (!extra) {
                currentNormalTokenLays.clear();
                log.debug("This was a normal token lay");
            }

            if (currentNormalTokenLays.isEmpty()) {
                log.debug("No more normal token lays are allowed");
            } else if (operatingCompany.get().getNumberOfFreeBaseTokens() == 0) {
                log.debug("Normal token lay allowed by no more tokens");
                currentNormalTokenLays.clear();
            } else {
                log.debug("A normal token lay is still allowed");
            }
            setSpecialTokenLays();
            log.debug("There are now " + currentSpecialTokenLays.size()
                    + " special token lay objects");
            if (currentNormalTokenLays.isEmpty()
                    && currentSpecialTokenLays.isEmpty()) {
                nextStep();
            }

        }

        return true;
    }

    /** Reports if a token lay is allowed by a certain company on a certain hex and city
     * <p>
     * This method can be used both in restricting possible actions
     * and in validating submitted actions.
     * <p> Currently, only a few standard checks are included.
     * This method can be extended to perform other generic checks,
     * such as if a route exists,
     * and possibly in subclasses for game-specific checks.
     *
     * @param company The company laying a tile.
     * @param hex The hex on which a tile is laid.
     * @param station The number of the station/city on which the token
     * is to be laid (0 if any or immaterial).
     */
    protected boolean isTokenLayAllowed (PublicCompany company,
            MapHex hex, int station) {
        return !hex.isBlockedForTokenLays(company, station);
    }

    protected void setNormalTokenLays() {

        /* Normal token lays */
        currentNormalTokenLays.clear();

        /* For now, we allow one token of the currently operating company */
        if (operatingCompany.get().getNumberOfFreeBaseTokens() > 0) {
            currentNormalTokenLays.add(new LayBaseToken((List<MapHex>) null));
        }

    }

    /**
     * Create a List of allowed special token lays (see LayToken class). This
     * method should be called before each user action in the base token laying
     * step. TODO: Token preparation is practically identical to Tile
     * preparation, perhaps the two can be merged to one generic procedure.
     */
    protected void setSpecialTokenLays() {

        /* Special-property tile lays */
        currentSpecialTokenLays.clear();

        if (!operatingCompany.get().canUseSpecialProperties()) return;

        /*
         * In 1835, this only applies to major companies. TODO: For now,
         * hardcode this, but it must become configurable later.
         */
        if (operatingCompany.get().getType().getId().equals("Minor")) return;

        for (SpecialTokenLay stl : getSpecialProperties(SpecialTokenLay.class)) {
            log.debug("Spec.prop:" + stl);
            if (stl.getTokenClass().equals(BaseToken.class)
                    && (stl.isExtra() || !currentNormalTokenLays.isEmpty())) {
                /*
                 * If the special tile lay is not extra, it is only allowed if
                 * normal tile lays are also (still) allowed
                 */
                currentSpecialTokenLays.add(new LayBaseToken(stl));
            }
        }
    }

    /*=======================================
     *  5.2.  BONUS TOKENS
     *=======================================*/

    public boolean layBonusToken(LayBonusToken action) {

        String errMsg = null;
        int cost = 0;
        SpecialTokenLay stl = null;
        boolean extra = false;

        MapHex hex = action.getChosenHex();
        BonusToken token = action.getToken();

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            MapHex location = action.getChosenHex();
            if (location != hex) {
                errMsg =
                    LocalText.getText("TokenLayingHexMismatch",
                            hex.getId(),
                            location.getId() );
                break;
            }
            stl = action.getSpecialProperty();
            if (stl != null) extra = stl.isExtra();

            cost = 0; // Let's assume for now that bonus tokens are always
            // free
            if (stl != null && stl.isFree()) cost = 0;

            // Does the company have the money?
            if (cost > operatingCompany.get().getCash()) {
                errMsg =
                    LocalText.getText("NotEnoughMoney",
                            operatingCompany.get().getId());
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotLayBonusTokenOn",
                    token.getId(),
                    hex.getId(),
                    Bank.format(cost),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        // TODO: changeStack.start(true);

        if (hex.layBonusToken(token, gameManager.getPhaseManager())) {
            /* TODO: the false return value must be impossible. */

            operatingCompany.get().addBonus(new Bonus(operatingCompany.get(),
                    token.getId(),
                    token.getValue(), Collections.singletonList(hex)));
            token.setUser(operatingCompany.get());

            ReportBuffer.add(LocalText.getText("LaysBonusTokenOn",
                    operatingCompany.get().getId(),
                    token.getId(),
                    Bank.format(token.getValue()),
                    hex.getId() ));

            // Was a special property used?
            if (stl != null) {
                stl.setExercised();
                currentSpecialTokenLays.remove(action);
                log.debug("This was a special token lay, "
                        + (extra ? "" : " not") + " extra");

            }

        }

        return true;
    }

    public boolean buyBonusToken(BuyBonusToken action) {

        String errMsg = null;
        int cost;
        SellBonusToken sbt = null;
        CashOwner seller = null;

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            sbt = action.getSpecialProperty();
            cost = sbt.getPrice();
            seller = (CashOwner)sbt.getSeller(); // TODO: Remove the cast?

            // Does the company have the money?
            if (cost > operatingCompany.get().getCash()) {
                errMsg =
                    LocalText.getText("NotEnoughMoney",
                            operatingCompany.get().getId(),
                            Bank.format(operatingCompany.get().getCash()),
                            Bank.format(cost));
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotBuyBonusToken",
                    operatingCompany.get().getId(),
                    sbt.getId(),
                    seller.getId(),
                    Bank.format(cost),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        // TODO: changeStack.start(true);

        MoneyModel.cashMove(operatingCompany.get(), seller, cost);
        operatingCompany.get().addBonus(new Bonus(operatingCompany.get(),
                sbt.getId(),
                sbt.getValue(),
                sbt.getLocations()));

        ReportBuffer.add(LocalText.getText("BuysBonusTokenFrom",
                operatingCompany.get().getId(),
                sbt.getId(),
                Bank.format(sbt.getValue()),
                seller.getId(),
                Bank.format(sbt.getPrice())));

        sbt.setExercised();

        return true;
    }

    /**
     * TODO Should be merged with setSpecialTokenLays() in the future.
     * Assumptions: 1. Bonus tokens can be laid anytime during the OR. 2. Bonus
     * token laying is always extra. TODO This assumptions will be made
     * configurable conditions.
     */
    protected void setBonusTokenLays() {

        for (SpecialTokenLay stl : getSpecialProperties(SpecialTokenLay.class)) {
            if (stl.getTokenClass().equals(BonusToken.class)) {
                possibleActions.add(new LayBonusToken(stl,
                        (BonusToken) stl.getToken()));
            }
        }
    }

    /*=======================================
     *  6.  REVENUE AND DIVIDEND
     *=======================================*/

    public boolean setRevenueAndDividend(SetDividend action) {

        String errMsg = validateSetRevenueAndDividend (action);

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText(
                    "CannotProcessRevenue",
                    Bank.format (action.getActualRevenue()),
                    action.getCompanyName(),
                    errMsg
            ));
            return false;
        }

        // TODO: changeStack.start(true);

        ReportBuffer.add(LocalText.getText("CompanyRevenue",
                action.getCompanyName(),
                Bank.format(action.getActualRevenue())));

        int remainingAmount = checkForDeductions (action);
        if (remainingAmount < 0) {
            // A share selling round will be run to raise cash to pay debts
            return true;
        }

        executeSetRevenueAndDividend (action);

        return true;

    }

    protected String validateSetRevenueAndDividend (SetDividend action) {

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
            if (company != operatingCompany.get()) {
                errMsg =
                    LocalText.getText("WrongCompany",
                            companyName,
                            operatingCompany.get().getId() );
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
                errMsg =
                    LocalText.getText("NegativeAmountNotAllowed",
                            String.valueOf(amount));
                break;
            }
            if (amount % 10 != 0) {
                errMsg =
                    LocalText.getText("AmountMustBeMultipleOf10",
                            String.valueOf(amount));
                break;
            }

            // Check chosen revenue distribution
            if (amount > 0) {
                // Check the allocation type index (see SetDividend for values)
                revenueAllocation = action.getRevenueAllocation();
                if (revenueAllocation < 0
                        || revenueAllocation >= SetDividend.NUM_OPTIONS) {
                    errMsg =
                        LocalText.getText("InvalidAllocationTypeIndex",
                                String.valueOf(revenueAllocation));
                    break;
                }

                // Validate the chosen allocation type
                int[] allowedAllocations =
                    ((SetDividend) selectedAction).getAllowedAllocations();
                boolean valid = false;
                for (int aa : allowedAllocations) {
                    if (revenueAllocation == aa) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    errMsg =
                        LocalText.getText(SetDividend.getAllocationNameKey(revenueAllocation));
                    break;
                }
            } else {
                // If there is no revenue, use withhold.
                action.setRevenueAllocation(SetDividend.WITHHOLD);
            }

            if (amount == 0 && operatingCompany.get().getNumberOfTrains() == 0) {
                DisplayBuffer.add(LocalText.getText("RevenueWithNoTrains",
                        operatingCompany.get().getId(),
                        Bank.format(0) ));
            }

            break;
        }

        return errMsg;
    }

    protected void executeSetRevenueAndDividend (SetDividend action) {

        int amount = action.getActualRevenue();
        int revenueAllocation = action.getRevenueAllocation();

        operatingCompany.get().setLastRevenue(amount);
        operatingCompany.get().setLastRevenueAllocation(revenueAllocation);

        // Pay any debts from treasury, revenue and/or president's cash
        // The remaining dividend may be less that the original income
        amount = executeDeductions (action);

        if (amount == 0) {

            ReportBuffer.add(LocalText.getText("CompanyDoesNotPayDividend",
                    operatingCompany.get().getId()));
            withhold(amount);

        } else if (revenueAllocation == SetDividend.PAYOUT) {

            ReportBuffer.add(LocalText.getText("CompanyPaysOutFull",
                    operatingCompany.get().getId(), Bank.format(amount) ));

            payout(amount);

        } else if (revenueAllocation == SetDividend.SPLIT) {

            ReportBuffer.add(LocalText.getText("CompanySplits",
                    operatingCompany.get().getId(), Bank.format(amount) ));

            splitRevenue(amount);

        } else if (revenueAllocation == SetDividend.WITHHOLD) {

            ReportBuffer.add(LocalText.getText("CompanyWithholds",
                    operatingCompany.get().getId(),
                    Bank.format(amount) ));

            withhold(amount);

        }

        // Rust any obsolete trains
        operatingCompany.get().getPortfolioModel().rustObsoleteTrains();

        // We have done the payout step, so continue from there
        nextStep(GameDef.OrStep.PAYOUT);
    }

    /**
     * Distribute the dividend amongst the shareholders.
     *
     * @param amount
     */
    public void payout(int amount) {

        if (amount == 0) return;

        int part;
        int shares;

        Map<CashOwner, Integer> sharesPerRecipient = countSharesPerRecipient();

        // Calculate, round up, report and add the cash

        // Define a precise sequence for the reporting
        Set<CashOwner> recipientSet = sharesPerRecipient.keySet();
        for (CashOwner recipient : SequenceUtil.sortCashHolders(recipientSet)) {
            if (recipient instanceof Bank) continue;
            shares = (sharesPerRecipient.get(recipient));
            if (shares == 0) continue;
            part = (int) Math.ceil(amount * shares * operatingCompany.get().getShareUnit() / 100.0);
            ReportBuffer.add(LocalText.getText("Payout",
                    recipient.getId(),
                    Bank.format(part),
                    shares,
                    operatingCompany.get().getShareUnit()));
            pay(bank, recipient, part);
        }

        // Move the token
        operatingCompany.get().payout(amount);

    }

    protected  Map<CashOwner, Integer>  countSharesPerRecipient () {

        Map<CashOwner, Integer> sharesPerRecipient = new HashMap<CashOwner, Integer>();

        // Changed to accomodate the CGR 5% share roundup rule.
        // For now it is assumed, that actual payouts are always rounded up
        // (the withheld half of split revenues is not handled here, see splitRevenue()).

        // First count the shares per recipient
        for (PublicCertificate cert : operatingCompany.get().getCertificates()) {
            CashOwner recipient = getBeneficiary(cert);
            if (!sharesPerRecipient.containsKey(recipient)) {
                sharesPerRecipient.put(recipient, cert.getShares());
            } else {
                sharesPerRecipient.put(recipient,
                        sharesPerRecipient.get(recipient) + cert.getShares());
            }
        }
        return sharesPerRecipient;
    }

    /** Who gets the per-share revenue? */
    protected CashOwner getBeneficiary(PublicCertificate cert) {
        CashOwner beneficiary;
        
        // Special cases apply if the holder is the IPO or the Pool
        if (operatingCompany.get().paysOutToTreasury(cert)) {
            beneficiary = operatingCompany.get();
        } else if (cert.getOwner() instanceof CashOwner) {
            beneficiary = (CashOwner)cert.getOwner();
        } else { // TODO: check if this is a correct assumption that otherwise the money goes to the bank
            beneficiary = bank;
        }
        return beneficiary;
    }

    /**
     * Withhold a given amount of revenue (and store it).
     *
     * @param The revenue amount.
     */
    public void withhold(int amount) {

        PublicCompany company = operatingCompany.get();

        // Payout revenue to company
        pay (bank, company, amount);

        // Move the token
        company.withhold(amount);

        if (!company.hasStockPrice()) return;

        // Check if company has entered a closing area
        StockSpace newSpace = company.getCurrentSpace();
        if (newSpace.closesCompany() && company.canClose()) {
            company.setClosed();
            ReportBuffer.add(LocalText.getText("CompanyClosesAt",
                    company.getId(),
                    newSpace.getId()));
            finishTurn();
            return;
        }

    }

    /** Split a dividend. TODO Optional rounding down the payout
     *
     * @param amount
     */
    public void splitRevenue(int amount) {

        if (amount > 0) {
            // Withhold half of it
            // For now, hardcode the rule that payout is rounded up.
            int numberOfShares = operatingCompany.get().getNumberOfShares();
            int withheld =
                (amount / (2 * numberOfShares)) * numberOfShares;
            pay (bank, operatingCompany.get(), withheld);
            ReportBuffer.add(operatingCompany.get().getId() + " receives " + Bank.format(withheld));

            // Payout the remainder
            int payed = amount - withheld;
            payout(payed);
        }

    }

    /** Default version, to be overridden if need be */
    protected int checkForDeductions (SetDividend action) {
        return action.getActualRevenue();
    }

    /** Default version, to be overridden if need be */
    protected int executeDeductions (SetDividend action) {
        return action.getActualRevenue();
    }

    protected boolean executeOperatingCost(OperatingCost action){

        String companyName = action.getCompanyName();
        OperatingCost.OCType typeOC = action.getOCType();

        int amount = action.getAmount();

        String errMsg = null;

        while (true) {
            // Must be correct company.
            if (!companyName.equals(operatingCompany.get().getId())) {
                errMsg =
                    LocalText.getText("WrongCompany",
                            companyName,
                            operatingCompany.get().getId() );
                break;
            }
            // amount is available
            if ((amount + operatingCompany.get().getCash()) < 0) {
                errMsg =
                    LocalText.getText("NotEnoughMoney",
                            companyName,
                            Bank.format(operatingCompany.get().getCash()),
                            Bank.format(amount)
                    );
                break;
            }
            if (typeOC == OperatingCost.OCType.LAY_BASE_TOKEN && operatingCompany.get().getNumberOfFreeBaseTokens() == 0) {
                errMsg =
                    LocalText.getText("HasNoTokensLeft", companyName);
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("OCExecutionError",
                    companyName,
                    errMsg));
            return false;
        }

        // TODO: changeStack.start(true);

        if (amount > 0) {
            // positive amounts: remove cash from cashholder
            MoneyModel.cashMove(operatingCompany.get(), bank, amount);
        } else if (amount > 0) {
            // negative amounts: add cash to cashholder
            MoneyModel.cashMove(bank, operatingCompany.get(), -amount);
        }

        if (typeOC == OperatingCost.OCType.LAY_TILE) {
            operatingCompany.get().layTilenNoMapMode(amount);
            ReportBuffer.add(LocalText.getText("OCLayTileExecuted",
                    operatingCompany.get().getId(),
                    Bank.format(amount) ));
        }
        if (typeOC == OperatingCost.OCType.LAY_BASE_TOKEN) {
            // move token to Bank
            BaseToken token = operatingCompany.get().getFreeToken();
            if (token == null) {
                log.error("Company " + operatingCompany.get().getId() + " has no free token");
                return false;
            } else {
                // FIXME: Check where to lay the base tokens in NoMapMode
                // (bank.getUnavailable().addBonusToken(token));
            }
            operatingCompany.get().layBaseTokennNoMapMode(amount);
            ReportBuffer.add(LocalText.getText("OCLayBaseTokenExecuted",
                    operatingCompany.get().getId(),
                    Bank.format(amount) ));
        }

        return true;
    }

    protected void prepareRevenueAndDividendAction () {

        // There is only revenue if there are any trains
        if (operatingCompany.get().canRunTrains()) {
            int[] allowedRevenueActions =
                operatingCompany.get().isSplitAlways()
                ? new int[] { SetDividend.SPLIT }
            : operatingCompany.get().isSplitAllowed()
            ? new int[] { SetDividend.PAYOUT,
                    SetDividend.SPLIT,
                    SetDividend.WITHHOLD }
                : new int[] { SetDividend.PAYOUT,
                    SetDividend.WITHHOLD };

            possibleActions.add(new SetDividend(
                    operatingCompany.get().getLastRevenue(), true,
                    allowedRevenueActions));
        }
    }

    protected void prepareNoMapActions() {

        // LayTile Actions
        for (Integer tc: mapManager.getPossibleTileCosts()) {
            if (tc <= operatingCompany.get().getCash())
                possibleActions.add(new OperatingCost(OperatingCost.OCType.LAY_TILE, tc, false));
        }

        // LayBaseToken Actions
        if (operatingCompany.get().getNumberOfFreeBaseTokens() != 0) {
            int[] costsArray = operatingCompany.get().getBaseTokenLayCosts();

            // change to set to allow for identity and ordering
            Set<Integer> costsSet = new TreeSet<Integer>();
            for (int cost:costsArray)
                if (!(cost == 0 && costsArray.length != 1)) // fix for sequence based home token
                    costsSet.add(cost);

            // SpecialTokenLay Actions - workaround for a better handling of those later
            for (SpecialTokenLay stl : getSpecialProperties(SpecialTokenLay.class)) {
                log.debug("Special tokenlay property: " + stl);
                if (stl.getTokenClass().equals(BaseToken.class) && stl.isFree())
                    costsSet.add(0);
            }

            for (int cost : costsSet) {
                if (cost <= operatingCompany.get().getCash()) // distance method returns home base, but in sequence costsSet can be zero
                    possibleActions.add(new OperatingCost(OperatingCost.OCType.LAY_BASE_TOKEN, cost, false));
            }
        }

        // Default OperatingCost Actions
        //        possibleActions.add(new OperatingCost(
        //                OperatingCost.OCType.LAY_TILE, 0, true
        //            ));
        //        if (operatingCompany.getObject().getNumberOfFreeBaseTokens() != 0
        //                && operatingCompany.getObject().getBaseTokenLayCost(null) != 0) {
        //            possibleActions.add(new OperatingCost(OperatingCost.OCType.LAY_BASE_TOKEN, 0, true));
        //        }

    }

    /*=======================================
     *  7.  TRAIN PURCHASING
     *=======================================*/

    public boolean buyTrain(BuyTrain action) {

        Train train = action.getTrain();
        PublicCompany company = action.getCompany();
        String companyName = company.getId();
        Train exchangedTrain = action.getExchangedTrain();
        SpecialTrainBuy stb = null;

        String errMsg = null;
        int presidentCash = action.getPresidentCashToAdd();
        boolean presidentMustSellShares = false;
        int price = action.getPricePaid();
        int actualPresidentCash = 0;
        int cashToBeRaisedByPresident = 0;
        Player currentPlayer = operatingCompany.get().getPresident();

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct step
            if (getStep() != GameDef.OrStep.BUY_TRAIN) {
                errMsg = LocalText.getText("WrongActionNoTrainBuyingCost");
                break;
            }

            if (train == null) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Amount must be non-negative
            if (price < 0) {
                errMsg =
                    LocalText.getText("NegativeAmountNotAllowed",
                            Bank.format(price));
                break;
            }

            // Fixed price must be honoured
            int fixedPrice = action.getFixedCost();
            if (fixedPrice != 0 && fixedPrice != price) {
                errMsg = LocalText.getText("FixedPriceNotPaid",
                        Bank.format(price),
                        Bank.format(fixedPrice));
            }

            // Does the company have room for another train?
            int trainLimit = operatingCompany.get().getCurrentTrainLimit();
            if (!canBuyTrainNow() && !action.isForExchange()) {
                errMsg =
                    LocalText.getText("WouldExceedTrainLimit",
                            String.valueOf(trainLimit));
                break;
            }

            /* Check if this is an emergency buy */
            if (action.mustPresidentAddCash()) {
                // From the Bank
                presidentCash = action.getPresidentCashToAdd();
                if (currentPlayer.getCashValue() >= presidentCash) {
                    actualPresidentCash = presidentCash;
                } else {
                    presidentMustSellShares = true;
                    cashToBeRaisedByPresident =
                        presidentCash - currentPlayer.getCashValue();
                }
            } else if (action.mayPresidentAddCash()) {
                // From another company
                presidentCash = price - operatingCompany.get().getCash();
                if (presidentCash > action.getPresidentCashToAdd()) {
                    errMsg =
                        LocalText.getText("PresidentMayNotAddMoreThan",
                                Bank.format(action.getPresidentCashToAdd()));
                    break;
                } else if (currentPlayer.getCashValue() >= presidentCash) {
                    actualPresidentCash = presidentCash;
                } else {
                    presidentMustSellShares = true;
                    cashToBeRaisedByPresident =
                        presidentCash - currentPlayer.getCashValue();
                }

            } else {
                // No forced buy - does the company have the money?
                if (price > operatingCompany.get().getCash()) {
                    errMsg =
                        LocalText.getText("NotEnoughMoney",
                                companyName,
                                Bank.format(operatingCompany.get().getCash()),
                                Bank.format(price) );
                    break;
                }
            }

            if (action.isForExchange()) {
                if (exchangedTrain == null) {
                    errMsg = LocalText.getText("NoExchangedTrainSpecified");
                    // TEMPORARY FIX to clean up invalidated saved files - DOES NOT WORK!!??
                    //exchangedTrain = operatingCompany.getObject().getPortfolio().getTrainList().get(0);
                    //action.setExchangedTrain(exchangedTrain);
                    break;
                } else if (operatingCompany.get().getPortfolioModel().getTrainOfType(exchangedTrain.getCertType()) == null) {
                    errMsg = LocalText.getText("CompanyDoesNotOwnTrain",
                            operatingCompany.get().getId(),
                            exchangedTrain.getId());
                    break;
                }
            }

            stb = action.getSpecialProperty();
            // TODO Note: this is not yet validated

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotBuyTrainFor",
                    companyName,
                    train.getId(),
                    Bank.format(price),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        // TODO: changeStack.start(true);
        Phase previousPhase = getCurrentPhase();

        if (presidentMustSellShares) {
            savedAction = action;

            gameManager.startShareSellingRound(operatingCompany.get().getPresident(),
                    cashToBeRaisedByPresident, operatingCompany.get(), true);

            return true;
        }

        if (actualPresidentCash > 0) {
            MoneyModel.cashMove(currentPlayer, operatingCompany.get(), presidentCash);
            ReportBuffer.add(LocalText.getText("PresidentAddsCash",
                    operatingCompany.get().getId(),
                    currentPlayer.getId(),
                    Bank.format(actualPresidentCash)));
        }

        Owner oldOwner = train.getOwner();

        if (exchangedTrain != null) {
            Train oldTrain =
                operatingCompany.get().getPortfolioModel().getTrainOfType(
                        exchangedTrain.getCertType());
            (train.isObsolete() ? scrapHeap : pool).addTrain(oldTrain);
            ReportBuffer.add(LocalText.getText("ExchangesTrain",
                    companyName,
                    exchangedTrain.getId(),
                    train.getId(),
                    oldOwner.getId(),
                    Bank.format(price) ));
        } else if (stb == null) {
            ReportBuffer.add(LocalText.getText("BuysTrain",
                    companyName,
                    train.getId(),
                    oldOwner.getId(),
                    Bank.format(price) ));
        } else {
            ReportBuffer.add(LocalText.getText("BuysTrainUsingSP",
                    companyName,
                    train.getId(),
                    oldOwner.getId(),
                    Bank.format(price),
                    stb.getOriginalCompany().getId() ));
        }

        train.setType(action.getType()); // Needed for dual trains bought from the Bank

        operatingCompany.get().buyTrain(train, price);

        if (oldOwner == ipo.getTrainsModel()) {
            train.getCertType().addToBoughtFromIPO();
            trainManager.setAnyTrainBought(true);
            // Clone the train if infinitely available
            if (train.getCertType().hasInfiniteQuantity()) {
                ipo.addTrain(trainManager.cloneTrain(train.getCertType()));
            }

        }
        if (oldOwner instanceof Bank) {
            trainsBoughtThisTurn.add(train.getCertType());
        }

        if (stb != null) {
            stb.setExercised();
            log.debug("This was a special train buy");
        }

        // Check if the phase has changed.
        trainManager.checkTrainAvailability(train, oldOwner);

        // Check if any companies must discard trains
        if (getCurrentPhase() != previousPhase && checkForExcessTrains()) {
            stepObject.set(GameDef.OrStep.DISCARD_TRAINS);
        }

        if (trainManager.hasPhaseChanged()) newPhaseChecks();

        return true;
    }

    /**
     * Can the operating company buy a train now?
     * Normally only calls isBelowTrainLimit() to get the result.
     * May be overridden if other considerations apply (such as
     * having a Pullmann in 18EU).
     * @return
     */
    protected boolean canBuyTrainNow () {
        return isBelowTrainLimit();
    }

    public boolean checkForExcessTrains() {

        excessTrainCompanies = new HashMap<Player, List<PublicCompany>>();
        Player player;
        for (PublicCompany comp : operatingCompanies.view()) {
            if (comp.getPortfolioModel().getNumberOfTrains() > comp.getCurrentTrainLimit()) {
                player = comp.getPresident();
                if (!excessTrainCompanies.containsKey(player)) {
                    excessTrainCompanies.put(player,
                            new ArrayList<PublicCompany>(2));
                }
                excessTrainCompanies.get(player).add(comp);
            }

        }
        return !excessTrainCompanies.isEmpty();
    }

    /** Stub */
    protected void newPhaseChecks() {
    }

    /**
     * Get a list of buyable trains for the currently operating company. Omit
     * trains that the company has no money for. If there is no cash to buy any
     * train from the Bank, prepare for emergency train buying.
     */
    public void setBuyableTrains() {

        if (operatingCompany.get() == null) return;

        int cash = operatingCompany.get().getCash();

        int cost = 0;
        List<Train> trains;

        boolean hasTrains =
            operatingCompany.get().getPortfolioModel().getNumberOfTrains() > 0;

            // Cannot buy a train without any cash, unless you have to
            if (cash == 0 && hasTrains) return;

            boolean canBuyTrainNow = canBuyTrainNow();
            boolean mustBuyTrain = !hasTrains && operatingCompany.get().mustOwnATrain();
            boolean emergency = false;

            SortedMap<Integer, Train> newEmergencyTrains = new TreeMap<Integer, Train>();
            SortedMap<Integer, Train> usedEmergencyTrains = new TreeMap<Integer, Train>();
            TrainManager trainMgr = gameManager.getTrainManager();

            // First check if any more trains may be bought from the Bank
            // Postpone train limit checking, because an exchange might be possible
            if (getCurrentPhase().canBuyMoreTrainsPerTurn()
                    || trainsBoughtThisTurn.isEmpty()) {
                boolean mayBuyMoreOfEachType =
                    getCurrentPhase().canBuyMoreTrainsPerTypePerTurn();

                /* New trains */
                trains = trainMgr.getAvailableNewTrains();
                for (Train train : trains) {
                    if (!operatingCompany.get().mayBuyTrainType(train)) continue;
                    if (!mayBuyMoreOfEachType
                            && trainsBoughtThisTurn.contains(train.getCertType())) {
                        continue;
                    }

                    // Allow dual trains (since jun 2011)
                    List<TrainType> types = train.getCertType().getPotentialTrainTypes();
                    for (TrainType type : types) {
                        cost = type.getCost();
                        if (cost <= cash) {
                            if (canBuyTrainNow) {
                                BuyTrain action = new BuyTrain(train, type, ipo.getParent(), cost);
                                action.setForcedBuyIfNoRoute(mustBuyTrain); // TEMPORARY
                                possibleActions.add(action);
                            }
                        } else if (mustBuyTrain) {
                            newEmergencyTrains.put(cost, train);
                        }
                    }

                    // Even at train limit, exchange is allowed (per 1856)
                    if (train.canBeExchanged() && hasTrains) {
                        cost = train.getCertType().getExchangeCost();
                        if (cost <= cash) {
                            List<Train> exchangeableTrains =
                                operatingCompany.get().getPortfolioModel().getUniqueTrains();
                            BuyTrain action = new BuyTrain(train, ipo.getParent(), cost);
                            action.setTrainsForExchange(exchangeableTrains);
                            //if (atTrainLimit) action.setForcedExchange(true);
                            possibleActions.add(action);
                            canBuyTrainNow = true;
                        }
                    }

                    if (!canBuyTrainNow) continue;

                    // Can a special property be used?
                    // N.B. Assume that this never occurs in combination with
                    // dual trains or train exchanges,
                    // otherwise the below code must be duplicated above.
                    for (SpecialTrainBuy stb : getSpecialProperties(SpecialTrainBuy.class)) {
                        int reducedPrice = stb.getPrice(cost);
                        if (reducedPrice > cash) continue;
                        BuyTrain bt = new BuyTrain(train, ipo.getParent(), reducedPrice);
                        bt.setSpecialProperty(stb);
                        bt.setForcedBuyIfNoRoute(mustBuyTrain); // TEMPORARY
                        possibleActions.add(bt);
                    }

                }
                if (!canBuyTrainNow) return;

                /* Used trains */
                trains = pool.getUniqueTrains();
                for (Train train : trains) {
                    if (!mayBuyMoreOfEachType
                            && trainsBoughtThisTurn.contains(train.getCertType())) {
                        continue;
                    }
                    cost = train.getCost();
                    if (cost <= cash) {
                        BuyTrain bt = new BuyTrain(train, pool.getParent(), cost);
                        bt.setForcedBuyIfNoRoute(mustBuyTrain); // TEMPORARY
                        possibleActions.add(bt);
                    } else if (mustBuyTrain) {
                        usedEmergencyTrains.put(cost, train);
                    }
                }

                emergency = mustBuyTrain && possibleActions.getType(BuyTrain.class).isEmpty();

                // If we must buy a train and haven't found one yet, the president must add cash.
                if (emergency
                        // Some people think it's allowed in 1835 to buy a new train with president cash
                        // even if the company has enough cash to buy a used train.
                        // Players who think differently can ignore that extra option.
                        || getGameParameterAsBoolean (GameDef.Parm.EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN)
                        && !newEmergencyTrains.isEmpty()) {
                    if (getGameParameterAsBoolean (GameDef.Parm.EMERGENCY_MUST_BUY_CHEAPEST_TRAIN)) {
                        // Find the cheapest one
                        // Assume there is always one available from IPO
                        int cheapestTrainCost = newEmergencyTrains.firstKey();
                        Train cheapestTrain = newEmergencyTrains.get(cheapestTrainCost);
                        if (!usedEmergencyTrains.isEmpty()
                                && usedEmergencyTrains.firstKey() < cheapestTrainCost) {
                            cheapestTrainCost = usedEmergencyTrains.firstKey();
                            cheapestTrain = usedEmergencyTrains.get(cheapestTrainCost);
                        }
                        BuyTrain bt = new BuyTrain(cheapestTrain,
                                cheapestTrain.getOwner(), cheapestTrainCost);
                        bt.setPresidentMustAddCash(cheapestTrainCost - cash);
                        bt.setForcedBuyIfNoRoute(mustBuyTrain); // TODO TEMPORARY
                        possibleActions.add(bt);
                    } else {
                        // All possible bank trains are buyable
                        for (Train train : newEmergencyTrains.values()) {
                            BuyTrain bt = new BuyTrain(train, ipo.getParent(), train.getCost());
                            bt.setPresidentMustAddCash(train.getCost() - cash);
                            bt.setForcedBuyIfNoRoute(mustBuyTrain); // TODO TEMPORARY
                            possibleActions.add(bt);
                        }
                        for (Train train : usedEmergencyTrains.values()) {
                            BuyTrain bt = new BuyTrain(train, pool.getParent(), train.getCost());
                            bt.setPresidentMustAddCash(train.getCost() - cash);
                            bt.setForcedBuyIfNoRoute(mustBuyTrain); // TODO TEMPORARY
                            possibleActions.add(bt);
                        }
                    }
                }
            }

            if (!canBuyTrainNow) return;

            /* Other company trains, sorted by president (current player first) */
            if (getCurrentPhase().isTrainTradingAllowed()) {
                BuyTrain bt;
                Player p;
                int index;
                int numberOfPlayers = getNumberOfPlayers();
                int presidentCash = operatingCompany.get().getPresident().getCashValue();

                // Set up a list per player of presided companies
                List<List<PublicCompany>> companiesPerPlayer =
                    new ArrayList<List<PublicCompany>>(numberOfPlayers);
                for (int i = 0; i < numberOfPlayers; i++)
                    companiesPerPlayer.add(new ArrayList<PublicCompany>(4));
                List<PublicCompany> companies;
                // Sort out which players preside over which companies.
                for (PublicCompany c : getOperatingCompanies()) {
                    if (c.isClosed() || c == operatingCompany.get()) continue;
                    p = c.getPresident();
                    index = p.getIndex();
                    companiesPerPlayer.get(index).add(c);
                }
                // Scan trains per company per player, operating company president
                // first
                int currentPlayerIndex = getCurrentPlayer().getIndex();
                for (int i = currentPlayerIndex; i < currentPlayerIndex
                + numberOfPlayers; i++) {
                    companies = companiesPerPlayer.get(i % numberOfPlayers);
                    for (PublicCompany company : companies) {
                        trains = company.getPortfolioModel().getUniqueTrains();
                        for (Train train : trains) {
                            if (train.isObsolete() || !train.isTradeable()) continue;
                            bt = null;
                            if (i != currentPlayerIndex
                                    && getGameParameterAsBoolean(GameDef.Parm.FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS)
                                    || operatingCompany.get().mustTradeTrainsAtFixedPrice()
                                    || company.mustTradeTrainsAtFixedPrice()) {
                                // Fixed price
                                if ((cash >= train.getCost()) && (operatingCompany.get().mayBuyTrainType(train))) {
                                    // TODO: Check if this still works, as now the company is the from type
                                    bt = new BuyTrain(train, company, train.getCost());
                                } else {
                                    continue;
                                }
                            } else if (cash > 0
                                    || emergency
                                    && getGameParameterAsBoolean (GameDef.Parm.EMERGENCY_MAY_BUY_FROM_COMPANY)) {
                                // TODO: Check if this still works, as now the company is the from type
                                bt = new BuyTrain(train, company, 0);

                                // In some games the president may add extra cash up to the list price
                                if (emergency && cash < train.getCost()) {
                                    bt.setPresidentMayAddCash(Math.min(train.getCost() - cash,
                                            presidentCash));
                                }
                            }
                            if (bt != null) possibleActions.add(bt);
                        }
                    }
                }
            }

            if (!operatingCompany.get().mustOwnATrain()
                    || operatingCompany.get().getPortfolioModel().getNumberOfTrains() > 0) {
                doneAllowed = true;
            }
    }

    /**
     * Returns whether or not the company is allowed to buy a train, considering
     * its train limit.
     *
     * @return
     */
    protected boolean isBelowTrainLimit() {
        return operatingCompany.get().getNumberOfTrains() < operatingCompany.get().getCurrentTrainLimit();
    }

    public void checkForeignSales() {
        if (getGameParameterAsBoolean(GameDef.Parm.REMOVE_TRAIN_BEFORE_SR)
                && trainManager.isAnyTrainBought()) {
            Train train = trainManager.getAvailableNewTrains().get(0);
            if (train.getCertType().hasInfiniteQuantity()) return;
            scrapHeap.addTrain(train);
            ReportBuffer.add(LocalText.getText("RemoveTrain", train.getId()));
        }
    }

    /*=======================================
     *  8.   VARIOUS UTILITIES
     *=======================================*/

    protected <T extends SpecialProperty> List<T> getSpecialProperties(
            Class<T> clazz) {
        List<T> specialProperties = new ArrayList<T>();
        if (!operatingCompany.get().isClosed()) {
            // OC may have closed itself (e.g. in 1835 when M2 buys 1st 4T and starts PR)
            specialProperties.addAll(operatingCompany.get().getPortfolioModel().getSpecialProperties(
                    clazz, false));
            specialProperties.addAll(operatingCompany.get().getPresident().getPortfolioModel().getSpecialProperties(
                    clazz, false));
        }
        return specialProperties;
    }

    @Override
    public List<SpecialProperty> getSpecialProperties() {
        return currentSpecialProperties;
    }

    /* TODO This is just a start of a possible approach to a Help system */
    @Override
    public String getHelp() {
        GameDef.OrStep step = getStep();
        StringBuffer b = new StringBuffer();
        b.append("<big>Operating round: ").append(thisOrNumber).append(
        "</big><br>");
        b.append("<br><b>").append(operatingCompany.get().getId()).append(
        "</b> (president ").append(getCurrentPlayer().getId()).append(
        ") has the turn.");
        b.append("<br><br>Currently allowed actions:");
        switch (step) {
        case LAY_TRACK:
            b.append("<br> - Lay a tile");
            b.append("<br> - Press 'Done' if you do not want to lay a tile");
            break;
        case LAY_TOKEN:
            b.append("<br> - Lay a base token or press Done");
            b.append("<br> - Press 'Done' if you do not want to lay a base");
            break;
        case CALC_REVENUE:
            b.append("<br> - Enter new revenue amount");
            b.append("<br> - Press 'Done' if your revenue is zero");
            break;
        case PAYOUT:
            b.append("<br> - Choose how the revenue will be paid out");
            break;
        case BUY_TRAIN:
            b.append("<br> - Buy one or more trains");
            b.append("<br> - Press 'Done' to finish your turn");
            break;
        }

        /* TODO: The below if needs be refined. */
        if (getCurrentPhase().isPrivateSellingAllowed()
                && step != GameDef.OrStep.PAYOUT) {
            b.append("<br> - Buy one or more Privates");
        }

        if (step == GameDef.OrStep.LAY_TRACK) {
            b.append("<br><br><b>Tile laying</b> proceeds as follows:");
            b.append("<br><br> 1. On the map, select the hex that you want to lay a new tile upon.");
            b.append("<br>If tile laying is allowed on this hex, the current tile will shrink a bit <br>and a red background will show up around its edges;");
            b.append("<br>in addition, the tiles that can be laid on that hex will be displayed<br> in the 'upgrade panel' at the left hand side of the map.");
            b.append("<br>If tile laying is not allowed there, nothing will happen.");
            b.append("<br><br> 2. Select a tile in the upgrade panel.<br>This tile will be copied to the selected hex,<br>in some orientation");
            b.append("<br><br> 3. If you want to turn the tile just laid to a different orientation, click it.");
            b.append("<br>Repeatedly clicking the tile will rotate it through all allowed orientations.");
            b.append("<br><br> 4. Confirm tile laying by clicking 'Done'");
            b.append("<br><br>Before 'Done' has been pressed, you can change your mind<br>as often as you want");
            b.append(" (presuming that the other players don't get angry).");
            b.append("<br> - If you want to select another hex: repeat step 1");
            b.append("<br> - If you want to lay another tile on the currently selected hex: repeat step 2.");
            b.append("<br> - If you want to undo hex selection: click outside of the map hexes.");
            b.append("<br> - If you don't want to lay a tile after all: press 'Cancel'");
        }

        return b.toString();
    }

    /**
     * Update the status if the step has changed by an Undo or Redo
     */
    public void update() {
        prepareStep();
    }

    @Override
    public String toString() {
        return "OperatingRound " + thisOrNumber;
    }

    /** @Overrides */
    public boolean equals(Round round) {
        return round instanceof OperatingRound
        && thisOrNumber.equals(((OperatingRound) round).thisOrNumber);
    }

    @Override
    public String getRoundName() {
        return toString();
    }

    // Observer Interface
    public Observable getObservable() {
        return stepObject;
    }

    public boolean deRegister() {
        if (stepObject == null) return false;
        return stepObject.removeObserver(this);
    }

}