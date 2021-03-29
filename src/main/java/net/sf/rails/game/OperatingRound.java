package net.sf.rails.game;

import com.google.common.collect.Iterables;
import net.sf.rails.common.*;
import net.sf.rails.game.financial.*;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.special.*;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Observer;
import net.sf.rails.game.state.*;
import net.sf.rails.util.SequenceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;
import rails.game.correct.ClosePrivate;
import rails.game.correct.OperatingCost;

import java.util.*;

/**
 * Implements a basic Operating Round. <p> A new instance must be created for
 * each new Operating Round. At the end of a round, the current instance should
 * be discarded.
 */
public class OperatingRound extends Round implements Observer {

    private static final Logger log = LoggerFactory.getLogger(OperatingRound.class);

    /* Transient memory (per round only) */
    protected final GenericState<GameDef.OrStep> stepObject
            = new GenericState<>(this, "ORStep", GameDef.OrStep.INITIAL);

    protected boolean actionPossible = true;

    /* flag for using rails without map support */
    protected final boolean noMapMode;

    // TODO: Check if this should not be turned into a State?
    protected final List<PublicCompany> companiesOperatedThisRound = new ArrayList<>();

    protected ArrayListState<PublicCompany> operatingCompanies;

    protected final GenericState<PublicCompany> operatingCompany = new GenericState<>(this, "operatingCompany");
    // do not use a operatingCompany.getObject() as reference
    // TODO: Question is this remark above still relevant?

    // Non-persistent lists (are recreated after each user action)

    protected final HashMapState<String, Integer> tileLaysPerColour = HashMapState.create(this, "tileLaysPerColour");

    protected final List<LayBaseToken> currentNormalTokenLays = new ArrayList<>();

    protected final List<LayBaseToken> currentSpecialTokenLays = new ArrayList<>();

    /**
     * A List per player with owned companies that have excess trains
     */
    protected Map<Player, List<PublicCompany>> excessTrainCompanies;

    protected final ArrayListState<TrainCardType> trainsBoughtThisTurn = new ArrayListState<>(this, "trainsBoughtThisTurn");

    protected HashMapState<PublicCompany, Integer> loansThisRound;

    protected String thisOrNumber;

    protected PossibleAction selectedAction;

    protected PossibleAction savedAction;

    protected GameDef.OrStep[] steps = new GameDef.OrStep[]{
            GameDef.OrStep.INITIAL, GameDef.OrStep.LAY_TRACK,
            GameDef.OrStep.LAY_TOKEN, GameDef.OrStep.CALC_REVENUE,
            GameDef.OrStep.PAYOUT, GameDef.OrStep.BUY_TRAIN,
            GameDef.OrStep.TRADE_SHARES, GameDef.OrStep.FINAL
    };

    protected boolean doneAllowed = false;
    protected boolean emergency = false;

    protected TrainManager trainManager = getRoot().getTrainManager();

    /*
     * =======================================
     *  1. OR START and END
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
        log.info ("--- Starting OR type round: {} ---", getId());
    }

    public void start() {
        thisOrNumber = gameManager.getORId();

        ReportBuffer.add(this, LocalText.getText("START_OR", thisOrNumber));

        for (Player player : getRoot().getPlayerManager().getPlayers()) {
            player.setWorthAtORStart();
        }

        privatesPayOut();

        if (operatingCompanies.size() > 0) {
            StringBuilder msg = new StringBuilder();
            for (PublicCompany company : operatingCompanies.view()) {
                msg.append(",").append(company.getId());
            }
            if (msg.length() > 0) msg.deleteCharAt(0);
            log.debug("Initial operating sequence is {}", msg);

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
                        if (count++ == 0) ReportBuffer.add(this, "");
                        String revText = Currency.fromBank(revenue, recipient);
                        ReportBuffer.add(this, LocalText.getText("ReceivesFor",
                                recipient.getId(), revText, priv.getId()));
                    }
                }
            }
        }
    }

    @Override
    public void resume() {

        if (savedAction instanceof BuyTrain) {
            buyTrain((BuyTrain) savedAction);
        } else if (savedAction instanceof SetDividend) {
            executeSetRevenueAndDividend((SetDividend) savedAction);
        } else if (savedAction instanceof RepayLoans) {
            executeRepayLoans((RepayLoans) savedAction);
        } else if (savedAction == null) {
            // nextStep();
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

    /*
     * =======================================
     *  2. CENTRAL PROCESSING FUNCTIONS
     * 2.1. PROCESS USER ACTION
     * =======================================
     */

    @Override
    public boolean process(PossibleAction action) {

        boolean result = false;
        doneAllowed = false;

        /*--- Common OR checks ---*/
        /* Check operating company */
        if (action instanceof PossibleORAction
                && !(action instanceof DiscardTrain) && !action.isCorrection()) {
            PublicCompany company = ((PossibleORAction) action).getCompany();
            if (company != operatingCompany.value()) {
                DisplayBuffer.add(this, LocalText.getText("WrongCompany",
                        company.getId(), operatingCompany.value().getId()));
                return false;
            }
        }

        selectedAction = action;

        if (selectedAction instanceof LayTile) {

            LayTile layTileAction = (LayTile) selectedAction;

            switch (layTileAction.getType()) {
                case (LayTile.CORRECTION):
                    result = layTileCorrection(layTileAction);
                    break;
                default:
                    result = layTile(layTileAction);
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

            result = discardTrain((DiscardTrain) selectedAction);

        } else if (selectedAction instanceof BuyPrivate) {

            result = buyPrivate((BuyPrivate) selectedAction);

        } else if (selectedAction instanceof ReachDestinations) {

            result = reachDestinations((ReachDestinations) selectedAction);

        } else if (selectedAction instanceof TakeLoans) {

            result = takeLoans((TakeLoans) selectedAction);

        } else if (selectedAction instanceof RepayLoans) {

            result = repayLoans((RepayLoans) selectedAction);

            /*
             * } else if (selectedAction instanceof ExchangeTokens) {
             *
             * result = exchangeTokens((ExchangeTokens) selectedAction, false); // 2nd //
             * parameter: // unlinked // moveset
             */
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

        PublicCompany publicCompany =
                companyManager.getPublicCompany(sp.getPublicCompanyName());
        PrivateCompany privateCompany = (PrivateCompany) sp.getOriginalCompany();
        Owner owner = privateCompany.getOwner();
        Player player = null;
        String errMsg = null;
        boolean ipoHasShare = ipo.getShare(publicCompany) >= sp.getShare();
        boolean poolHasShare = pool.getShare(publicCompany) >= sp.getShare();

        while (true) {

            /* Check if the private is owned by a player */
            if (!(owner instanceof Player)) {
                errMsg =
                        LocalText.getText("PrivateIsNotOwnedByAPlayer",
                                privateCompany.getId());
                break;
            }

            player = (Player) owner;

            /* Check if a share is available */
            if (!ipoHasShare && !poolHasShare) {
                errMsg =
                        LocalText.getText("NoSharesAvailable",
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


        Certificate cert =
                ipoHasShare
                        ? ipo.findCertificate(publicCompany,false)
                        : pool.findCertificate(publicCompany,false);
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
     *  2.2. PREPARE NEXT ACTION
     * =======================================
     */

    /**
     * To be called after each change, to re-establish the currently allowed
     * actions. (new method, intended to absorb code from several other
     * methods).
     */
    @Override
    public boolean setPossibleActions() {

        /* Create a new list of possible actions for the UI */
        possibleActions.clear();
        selectedAction = null;

        boolean forced = false;
        doneAllowed = false; // set default (fix of bug 2954654)

        if (getStep() == GameDef.OrStep.INITIAL) {
            initTurn();
            nextStep();
       }

        GameDef.OrStep step = getStep();
        if (step == GameDef.OrStep.LAY_TRACK) {

            if (!operatingCompany.value().hasLaidHomeBaseTokens()) {
                // This can occur if the home hex has two cities and track,
                // such as the green OO tile #59

                // BR: as this is a home token, need to call LayBaseToken with a
                // MapHex, not a list
                // to avoid the LayBaseToken action from being a regular token
                // lay
                // I am not sure that this will work with multiple home hexes.
                for (MapHex home : operatingCompany.value().getHomeHexes()) {
                    possibleActions.add(new LayBaseToken(getRoot(), home));
                }
                forced = true;
            } else {
                possibleActions.addAll(getNormalTileLays(true));
                possibleActions.addAll(getSpecialTileLays(true));
                possibleActions.add(new NullAction(getRoot(), NullAction.Mode.SKIP));
            }

        } else if (step == GameDef.OrStep.LAY_TOKEN) {
            setNormalTokenLays();
            setSpecialTokenLays();
            log.debug("Normal token lays: {}", currentNormalTokenLays.size());
            log.debug("Special token lays: {}", currentSpecialTokenLays.size());

            possibleActions.addAll(currentNormalTokenLays);
            possibleActions.addAll(currentSpecialTokenLays);
            possibleActions.add(new NullAction(getRoot(), NullAction.Mode.SKIP));

        } else if (step == GameDef.OrStep.CALC_REVENUE) {
            prepareRevenueAndDividendAction();
            if (noMapMode) prepareNoMapActions();

        } else if (step == GameDef.OrStep.BUY_TRAIN) {
            setBuyableTrains();
            // TODO Need route checking here.
            // TEMPORARILY allow not buying a train if none owned
            // if (!operatingCompany.getObject().mustOwnATrain()
            // ||
            // operatingCompany.getObject().getPortfolio().getNumberOfTrains() >
            // 0) {
            doneAllowed = true;
            // }
            if (noMapMode && (operatingCompany.value().getLastRevenue() == 0))
                prepareNoMapActions();

        } else if (step == GameDef.OrStep.DISCARD_TRAINS) {

            forced = true;
            setTrainsToDiscard();

        } else if (step == GameDef.OrStep.TRADE_SHARES) {
            gameManager.getCurrentRound().setPossibleActions();
        }

        // The following additional "common" actions are only available if the
        // primary action is not forced.
        if (!forced) {

            setBonusTokenLays();  // This does not cover "common" bonuses, see below. Confusing.

            setDestinationActions();

            setGameSpecificPossibleActions();

            // Private Company manually closure
            for (PrivateCompany priv : companyManager.getAllPrivateCompanies()) {
                if (!priv.isClosed() && priv.closesManually())
                    possibleActions.add(new ClosePrivate(priv));
            }

            // Can private companies be bought?
            if (isPrivateSellingAllowed()) {

                // Create a list of players with the current one in front
                int currentPlayerIndex =
                        operatingCompany.value().getPresident().getIndex();
                Player player;
                int minPrice, maxPrice;
                List<Player> players = playerManager.getPlayers();
                int numberOfPlayers = playerManager.getNumberOfPlayers();
                for (int i = currentPlayerIndex; i < currentPlayerIndex + numberOfPlayers; i++) {
                    player = players.get(i % numberOfPlayers);
                    if (!maySellPrivate(player)) continue;
                    for (PrivateCompany privComp : player.getPortfolioModel().getPrivateCompanies()) {

                        // check to see if the private can be sold to a company
                        if (!privComp.tradeableToCompany()) {
                            continue;
                        }

                        minPrice = getPrivateMinimumPrice(privComp);

                        maxPrice = getPrivateMaximumPrice(privComp);

                        BuyPrivate buyPrivate = new BuyPrivate(privComp, minPrice, maxPrice);
                        possibleActions.add(buyPrivate);
                    }
                }
            }

            if (operatingCompany.value().canUseSpecialProperties()) {

                // Are there any "common" special properties,
                // i.e. properties that are available to everyone?
                List<SpecialProperty> commonSP = gameManager.getCommonSpecialProperties();
                if (commonSP != null) {
                    SellBonusToken sbt;
                    loop:
                    for (SpecialProperty sp : commonSP) {
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
                List<SpecialProperty> orsps =
                        operatingCompany.value().getPortfolioModel().getAllSpecialProperties();

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
                            } else if (!(sp instanceof SpecialTileLay)) {
                                possibleActions.add(new UseSpecialProperty(sp));
                            }
                        }
                    }
                }
                // Are there other step-independent special properties owned by
                // the president?
                orsps =
                        playerManager.getCurrentPlayer().getPortfolioModel().getAllSpecialProperties();
                if (orsps != null) {
                    for (SpecialProperty sp : orsps) {
                        if (!sp.isExercised() && sp.isUsableIfOwnedByPlayer()
                                && sp.isUsableDuringOR(step)) {
                            if (sp instanceof SpecialBaseTokenLay) {
                                if (getStep() != GameDef.OrStep.LAY_TOKEN) {
                                    possibleActions.add(new LayBaseToken(getRoot(),
                                            (SpecialBaseTokenLay) sp));
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
            possibleActions.add(new NullAction(getRoot(), NullAction.Mode.DONE));
        }

        // Fix missing company info in some OR actions, for logging purposes
        for (PossibleAction pa : possibleActions.getList()) {
            if (pa instanceof PossibleORAction) {
                PossibleORAction pORA = (PossibleORAction) pa;
                if (pORA.getCompany() == null) {
                    pORA.setCompany(operatingCompany.value());
                }
            }
        }

        return true;
    }

    /**
     * Stub, can be overridden by subclasses
     */
    protected void setGameSpecificPossibleActions() {

    }

    /*
     * =======================================
     *  2.3. TURN CONTROL
     * =======================================
     */

    protected void initTurn() {
        log.debug("Starting turn of {}", operatingCompany.value().getId());
        ReportBuffer.add(this, " ");
        ReportBuffer.add(this, LocalText.getText("CompanyOperates",
                operatingCompany.value().getId(),
                operatingCompany.value().getPresident().getId()));
        playerManager.setCurrentPlayer(operatingCompany.value().getPresident());

        if (noMapMode && !operatingCompany.value().hasLaidHomeBaseTokens()) {
            // Lay base token in noMapMode
            BaseToken token = operatingCompany.value().getNextBaseToken();
            if (token == null) {
                log.error("Company {} has no free token to lay base token", operatingCompany.value().getId());
            } else {
                log.debug("Company {} lays base token in nomap mode", operatingCompany.value().getId());
                // FIXME: This has to be rewritten
                // Where are the nomap base tokens to be stored?
                // bank.getUnavailable().addBonusToken(token);
            }
        }
        operatingCompany.value().initTurn();
        trainsBoughtThisTurn.clear();
    }

    protected void finishTurn() {

        if (!operatingCompany.value().isClosed()) {
            operatingCompany.value().setOperated();
            companiesOperatedThisRound.add(operatingCompany.value());

            for (PrivateCompany priv : operatingCompany.value().getPortfolioModel().getPrivateCompanies()) {
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

    /**
     * Stub, may be overridden in subclasses
     * Return value: TRUE = normal turn end;
     * FALSE = return immediately from finishTurn().
     */
    protected boolean finishTurnSpecials() {
        return true;
    }

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

                // Check if the operating order has changed
                List<PublicCompany> newOperatingCompanies = setOperatingCompanies(operatingCompanies.view(), operatingCompany.value());
                PublicCompany company;
                for (int i = 0; i < newOperatingCompanies.size(); i++) {
                    company = newOperatingCompanies.get(i);
                    if (company != operatingCompanies.get(i)) {
                        log.debug("Company {} replaces {} in operating sequence", company.getId(), operatingCompanies.get(i).getId());
                        operatingCompanies.move(company, i);
                    }
                }

                setOperatingCompany(operatingCompanies.get(index));
            }

            if (operatingCompany.value().isClosed()
                    || operatingCompany.value().isHibernating()) continue;

            return true;
        }
    }

    protected void setOperatingCompany(PublicCompany company) {
        operatingCompany.set(company);
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
     *  2.4. STEP CONTROL
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
     * @param step
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
            if (steps[stepIndex] == newStep) break;
        }
        while (++stepIndex < steps.length) {
            newStep = steps[stepIndex];
            log.debug("OR considers newStep {}", newStep);

            if (newStep == GameDef.OrStep.LAY_TRACK) {
                initNormalTileLays();
            }

            if (newStep == GameDef.OrStep.LAY_TOKEN) {
                List<SpecialProperty> bonuses = gameManager.getCommonSpecialProperties();
                boolean bonusTokensForSale = bonuses.size() > 0
                        // We must (perhaps temporarily) make an exception for 1856,
                        // because otherwise its test files no longer get loaded.
                        // BTW the 1856 rules don't state when bonus tokens can be bought.
                        // TODO: Need to check how Rails handles that game.
                        // TODO: Also need to consider the "when" value.
                        && !"1856".equals(getRoot().getGameName());
                if (company.getNumberOfFreeBaseTokens() == 0
                    && !bonusTokensForSale) {
                    log.debug("OR skips {}: No tokens available", newStep);
                    continue;
                }
            }

            if (newStep == GameDef.OrStep.CALC_REVENUE) {

                if (company.hasTrains()) {
                    // All OK, we can't check here if it has a route

                } else if (company.canGenerateRevenue()) {
                    // In 18Scan a trainless minor company still pays out.
                    executeTrainlessRevenue(newStep);
                    continue;
                } else {
                    log.debug("OR skips {}: Cannot run trains", newStep);
                    executeSetRevenueAndDividend(new SetDividend(getRoot(), 0,
                            false, new int[] { SetDividend.NO_TRAIN }));
                    // TODO: This probably does not handle share selling correctly
                    continue;
                }
            }

            if (newStep == GameDef.OrStep.PAYOUT) {
                // This step is now obsolete
                log.debug("OR skips {}: Always skipped", newStep);
                continue;
            }

            if (newStep == GameDef.OrStep.TRADE_SHARES) {
                // Is company allowed to trade treasury shares?
                if (!company.mayTradeShares() ||
                        (company.mustHaveOperatedToTradeShares() && !company.hasOperated())) {
                    continue;
                }

                /*
                 * Check if any trading is possible. If not, skip this step.
                 * (but register a Done action for BACKWARDS COMPATIBILITY only)
                 */
                // Preload some expensive results
                int ownShare = company.getPortfolioModel().getShare(company);
                int poolShare = pool.getShare(company); // Expensive, do it once
                // Can it buy?
                boolean canBuy =
                        ownShare < GameDef.getParmAsInt(this, GameDef.Parm.TREASURY_SHARE_LIMIT)
                                && company.getCash() >= company.getCurrentSpace().getPrice()
                                && poolShare > 0;
                // Can it sell?
                boolean canSell =
                        company.getPortfolioModel().getShare(company) > 0
                                && poolShare < GameDef.getParmAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT);
                // Above we ignore the possible existence of double shares (as
                // in 1835).

                if (!canBuy && !canSell) {
                    // XXX For BACKWARDS COMPATIBILITY only,
                    // register a Done skip action during reloading.
                    if (gameManager.isReloading()) {
                        gameManager.setSkipDone(GameDef.OrStep.TRADE_SHARES);
                        log.debug("If the next saved action is 'Done', skip it");
                    }
                    log.debug("Skipping Treasury share trading newStep");
                    continue;
                }

                gameManager.startTreasuryShareTradingRound(operatingCompany.value());

            }

            if (!gameSpecificNextStep(newStep)) {
                log.debug("OR skips {}: Not game specific", newStep);
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
     * Stub, can be overridden in subclasses to check for extra steps
     */
    protected boolean gameSpecificNextStep(GameDef.OrStep step) {
        return true;
    }

    /**
     * Stub, to be used in case a trainless company still
     * generates some income (e.g. 18Scan minors)
     * @param step An OR step that is considered for execution
     */
    protected void executeTrainlessRevenue (GameDef.OrStep step) {}

    /** Stub, to be used in case a zero earnings run still
     * generates some income (e.g. 18Scan minors)
     * @param action SetDividend action with zero revenue
     */
    protected SetDividend  checkZeroRevenue (SetDividend action) {
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
     *  3. COMMON ACTIONS (not bound to steps)
     *  3.1. NOOPS
     * =======================================
     */

    public void skip(NullAction action) {
        log.debug("Skip step {}", stepObject.value());

        nextStep();
    }

    /**
     * The current Company is done operating.
     *
     * @param action TODO
     * @return false if an error is found.
     */
    public boolean done(NullAction action) {

        if (operatingCompany.value().getPortfolioModel().getNumberOfTrains() == 0
                && operatingCompany.value().mustOwnATrain()) {
            // FIXME: Need to check for valid route before throwing an
            // error.
            /*
             * Check TEMPORARILY disabled errMsg =
             * LocalText.getText("CompanyMustOwnATrain",
             * operatingCompany.getObject().getName()); setStep(STEP_BUY_TRAIN);
             * DisplayBuffer.add(this, errMsg); return false;
             */
        }

        nextStep();

        if (getStep() == GameDef.OrStep.FINAL) {
            finishTurn();
        }

        return true;
    }

    /*
     * =======================================
     *  3.2. DISCARDING TRAINS
     * =======================================
     */

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
                                company.getId(), train.toText());
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this,
                    LocalText.getText("CannotDiscardTrain", companyName,
                            (train != null ? train.toText() : "?"), errMsg));
            return false;
        }

        /* End of validation, start of execution */

        // FIXME: if (action.isForced()) changeStack.linkToPreviousMoveSet();

       train.getCard().discard();

        // Check if any more companies must discard trains,
        // otherwise continue train buying
        if (!checkForExcessTrains()) {
            // Trains may have been discarded by other players
            playerManager.setCurrentPlayer(operatingCompany.value().getPresident());
            stepObject.set(GameDef.OrStep.BUY_TRAIN);
        }

        // setPossibleActions();

        return true;
    }

    protected void setTrainsToDiscard() {

        // Scan the players in SR sequence, starting with the current player
        for (Player player : getRoot().getPlayerManager().getNextPlayers(true)) {
            if (excessTrainCompanies.containsKey(player)) {
                playerManager.setCurrentPlayer(player);
                List<PublicCompany> list = excessTrainCompanies.get(player);
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

    /*
     * =======================================
     *  3.3. PRIVATES (BUYING, SELLING, CLOSING)
     * =======================================
     */

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
                    companyManager.getPrivateCompany(privateCompanyName)) == null) {
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
            if (lowerPrice != PrivateCompany.NO_PRICE_LIMIT
                    && price < lowerPrice) {
                errMsg =
                        LocalText.getText("PriceBelowLowerLimit",
                                Bank.format(this, price),
                                Bank.format(this, lowerPrice),
                                privateCompanyName);
                break;
            }
            if (upperPrice != PrivateCompany.NO_PRICE_LIMIT
                    && price > upperPrice) {
                errMsg =
                        LocalText.getText("PriceAboveUpperLimit",
                                Bank.format(this, price),
                                Bank.format(this, lowerPrice),
                                privateCompanyName);
                break;
            }
            // Does the company have the money?
            if (price > operatingCompany.value().getCash()) {
                errMsg =
                        LocalText.getText("NotEnoughMoney", publicCompanyName,
                                Bank.format(this,
                                        operatingCompany.value().getCash()),
                                Bank.format(this, price));
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

        operatingCompany.value().buyPrivate(privateCompany, player, price);

        return true;

    }

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

        log.debug("Executed close private action for private {}", priv.getId());

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
     *  3.4. DESTINATIONS
     * =======================================
     */

    /**
     * Stub for applying any follow-up actions when a company reaches it
     * destinations. Default version: no actions.
     *
     * @param companies Companies that have just been reported as having reached their destinations.
     */
    protected void executeDestinationActions(List<PublicCompany> companies) {}

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
                    // Process any consequences of reaching a destination
                    // (default none)
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
     *  3.5. LOANS
     * =======================================
     */

    protected boolean takeLoans(TakeLoans action) {

        String errMsg = validateTakeLoans(action);

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotTakeLoans",
                    action.getCompanyName(), action.getNumberTaken(),
                    Bank.format(this, action.getPrice()), errMsg));

            return false;
        }

        executeTakeLoans(action);

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
                errMsg =
                        LocalText.getText("WrongCompany", companyName,
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
                errMsg =
                        LocalText.getText("MoreLoansNotAllowed", companyName,
                                company.getMaxNumberOfLoans());
                break;
            }
            break;
        }

        return errMsg;
    }

    protected void executeTakeLoans(TakeLoans action) {

        int number = action.getNumberTaken();
        int amount = calculateLoanAmount(number);
        operatingCompany.value().addLoans(number);
        Currency.fromBank(amount, operatingCompany.value());
        if (number == 1) {
            ReportBuffer.add(this, LocalText.getText("CompanyTakesLoan",
                    operatingCompany.value().getId(), Bank.format(this,
                            operatingCompany.value().getValuePerLoan()),
                    Bank.format(this, amount)));
        } else {
            ReportBuffer.add(this, LocalText.getText("CompanyTakesLoans",
                    operatingCompany.value().getId(), number, Bank.format(this,
                            operatingCompany.value().getValuePerLoan()),
                    Bank.format(this, amount)));
        }

        if (operatingCompany.value().getMaxLoansPerRound() > 0) {
            int oldLoansThisRound = 0;
            if (loansThisRound == null) {
                loansThisRound = HashMapState.create(this, "loansThisRound");
            } else if (loansThisRound.containsKey(operatingCompany.value())) {
                oldLoansThisRound =
                        loansThisRound.get(operatingCompany.value());
            }
            loansThisRound.put(operatingCompany.value(), oldLoansThisRound + number);
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

        int repayment =
                action.getNumberRepaid()
                        * operatingCompany.value().getValuePerLoan();
        if (repayment > 0 && repayment > operatingCompany.value().getCash()) {
            // President must contribute
            int remainder = repayment - operatingCompany.value().getCash();
            Player president = operatingCompany.value().getPresident();
            int presCash = president.getCashValue();
            if (remainder > presCash) {
                // Start a share selling round
                int cashToBeRaisedByPresident = remainder - presCash;
                log.info("A share selling round must be started as the president cannot pay ${} loan repayment", remainder);
                log.info("President has ${}, so ${} must be added", presCash, cashToBeRaisedByPresident);
                savedAction = action;

                gameManager.startShareSellingRound(
                        operatingCompany.value().getPresident(),
                        cashToBeRaisedByPresident, operatingCompany.value(),
                        false);
                return true;
            }
        }

        if (repayment > 0) executeRepayLoans(action);

        return true;
    }

    protected String validateRepayLoans(RepayLoans action) {

        String errMsg = null;

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
            String paymentText =
                    Currency.toBank(operatingCompany.value(), payment);
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
     *  3.6. RIGHTS
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
            rightValue = right.getValue();
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
        String costText = Currency.toBank(operatingCompany.value(), cost);

        ReportBuffer.add(this, LocalText.getText("BuysRight",
                operatingCompany.value().getId(), rightName, costText));

        sp.setExercised();

        return true;
    }

    /*
     * =======================================
     *  4. LAYING TILES
     * =======================================
     */

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
            if (!companyName.equals(operatingCompany.value().getId())) {
                errMsg =
                        LocalText.getText("WrongCompany", companyName,
                                operatingCompany.value().getId());
                break;
            }
            // Must be correct step
            if (getStep() != GameDef.OrStep.LAY_TRACK) {
                errMsg = LocalText.getText("WrongActionNoTileLay");
                break;
            }

            if (tile == null) break;

            if (!Phase.getCurrent(this).isTileColourAllowed(tile.getColourText())) {
                errMsg =
                        LocalText.getText("TileNotYetAvailable", tile.toText());
                break;
            }
            if (tile.getFreeCount() == 0) {
                errMsg = LocalText.getText("TileNotAvailable", tile.toText());
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
                            LocalText.getText("TileMayNotBeLaidInHex",
                                    tile.toText(), hex.getId());
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
                                tile.getColourText());
                break;
            }

            // Sort out cost
            /*
            if (stl != null && stl.isFree()) {
                cost = 0;
            } else {
                cost = hex.getTileCost() + extraLayTileCost(action);
                if (stl != null) {
                    cost = Math.max(0, cost - stl.getDiscount());
                }
            }
            */
            cost = tileLayCost(action);

            // Amount must be non-negative multiple of 10
            if (cost < 0) {
                errMsg =
                        LocalText.getText("NegativeAmountNotAllowed",
                                Bank.format(this, cost));
                break;
            }
            if (cost % 10 != 0) {
                errMsg =
                        LocalText.getText("AmountMustBeMultipleOf10",
                                Bank.format(this, cost));
                break;
            }
            // Does the company have the money?
            if (cost > operatingCompany.value().getCash()) {
                errMsg =
                        LocalText.getText("NotEnoughMoney", companyName,
                                Bank.format(this,
                                        operatingCompany.value().getCash()),
                                Bank.format(this, cost));
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotLayTileOn",
                    companyName, tile.toText(), hex.getId(),
                    Bank.format(this, cost), errMsg));
            return false;
        }

        /* End of validation, start of execution */

        if (tile != null) {
            String costText = null;
            if (cost > 0) {
                costText = Currency.toBank(operatingCompany.value(), cost);
            }
            operatingCompany.value().layTile(hex, tile, orientation, cost);

            if (costText == null) {
                ReportBuffer.add(
                        this,
                        LocalText.getText(
                                "LaysTileAt",
                                companyName,
                                tile.toText(),
                                hex.getId(),
                                hex.getOrientationName(HexSide.get(orientation))));
            } else {
                ReportBuffer.add(this, LocalText.getText("LaysTileAtFor",
                        companyName, tile.toText(), hex.getId(),
                        hex.getOrientationName(HexSide.get(orientation)),
                        costText));
            }
            hex.upgrade(action);

            // Was a special property used?
            if (stl != null) {
                stl.setExercised();
                // currentSpecialTileLays.remove(action);
                log.debug("This was a special tile lay, {} extra", extra ? "" : " not");
                if (!extra) {
                    // Still reduces normal tile lay allowance
                    registerNormalTileLay(tile);
                }
            } else {
                log.debug("This was a normal tile lay");
                registerNormalTileLay(tile);
            }
        }

        if (tile == null || !areTileLaysPossible()) {
            nextStep();
        }

        return true;
    }

    /*
     * Extracted method, to be overridden for any extra cost.
     * Examples: SOH (river bridge), 1846 (generic lay cost), 1861 (second tile)
     */
    protected int tileLayCost(LayTile action) {

        SpecialTileLay stl = action.getSpecialProperty();
        int cost = 0;
        if (stl == null || !stl.isFree()) {
            cost = action.getChosenHex().getTileCost();
            if (stl != null) {
                cost = Math.max(0, cost - stl.getDiscount());
            }
        }

        return cost;
    }

    public boolean layTileCorrection(LayTile action) {

        Tile tile = action.getLaidTile();
        MapHex hex = action.getChosenHex();
        int orientation = action.getOrientation();

        String errMsg = null;
        // tiles have external id defined
        if (tile != null
                && tile != hex.getCurrentTile()
                && tile.getFreeCount() == 0) {
            errMsg =
                    LocalText.getText("TileNotAvailable",
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

        String msg = LocalText.getText("CorrectMapLaysTileAt",
                tile.toText(), hex.getId(), hex.getOrientationName(orientation));
        ReportBuffer.add(this, msg);
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

        if (oldAllowedNumberObject == null) return false;

        int oldAllowedNumber = oldAllowedNumberObject;
        if (oldAllowedNumber <= 0) return false;

        if (update) updateAllowedTileColours(colour, oldAllowedNumber);
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
            log.debug("No more normal tile lays allowed");
            // currentNormalTileLays.clear();// Shouldn't be needed anymore ??
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
            log.debug("{} additional {} tile lays allowed; no other colours", oldAllowedNumber - 1, colour);
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
        Map<String, Integer> remainingTileLaysPerColour =
                new HashMap<>();

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
                log.debug("No normal tile lays");
            } else {
                for (LayTile tileLay : currentNormalTileLays) {
                    log.debug("Normal tile lay: {}", tileLay);
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
/*              for (SpecialMultiTileLay smtl : getSpecialProperties(SpecialMultiTileLay.class)) {

                LayTile layTile = new LayTile(smtl);
                if (validateSpecialTileLay(layTile))
                    currentSpecialTileLays.add(layTile);
            }*/
        }

        if (forReal) {
            int size = currentSpecialTileLays.size();
            if (size == 0) {
                log.debug("No special tile lays");
            } else {
                for (LayTile tileLay : currentSpecialTileLays) {
                    log.debug("Special tile lay: {}", tileLay);
                }
            }
        }

        return currentSpecialTileLays;
    }

    /**
     * Prevalidate a special tile lay. <p>During prevalidation, the action may
     * be updated (i.e. restricted). TODO <p>Note: The name of this method may
     * suggest that it can also be used for postvalidation (i.e. to validate the
     * action after the player has selected it). This is not yet the case, but
     * it is conceivable that this method can be extended to cover
     * postvalidation as well. Postvalidation is really a different process,
     * which in this context has not yet been considered in detail.
     *
     * @param layTile A LayTile object embedding a SpecialTileLay property. Any
     *                other LayTile objects are rejected. The object may be changed by this
     *                method.
     * @return TRUE if allowed.
     */
    protected boolean validateSpecialTileLay(LayTile layTile) {

        if (layTile == null) return false;

        SpecialTileLay stl = layTile.getSpecialProperty();

        if (!stl.isExtra()
                // If the special tile lay is not extra, it is only allowed if
                // normal tile lays are also (still) allowed
                && !checkNormalTileLay(stl.getTile(), false)) return false;

        Tile tile = stl.getTile();

        // What colours can be laid in the current phase?
        List<String> phaseColours = Phase.getCurrent(this).getTileColours();

        // Which tile colour(s) are specified explicitly...
        String[] stlc = stl.getTileColours();
        if ((stlc == null || stlc.length == 0) && tile != null) {
            // ... or implicitly
            stlc = new String[]{tile.getColourText()};
        }

        // Which of the specified tile colours can really be laid now?
        List<String> layableColours;
        if (stlc == null) {
            layableColours = phaseColours;
        } else {
            layableColours = new ArrayList<>();
            for (String colour : stlc) {
                if (phaseColours.contains(colour)) layableColours.add(colour);
            }
            if (layableColours.isEmpty()) return false;
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
                    if (!stl.isFree() && cash < cost) continue;

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
        if (!tc.isEmpty()) layTile.setTileColours(tc);

        if (hexes != null) {
            if (remainingHexes.isEmpty()) return false;
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
     * <p> This method can be used both in restricting possible actions and in
     * validating submitted actions. <p> Currently, only a few standard checks
     * are included. This method can be extended to perform other generic
     * checks, such as if a route exists, and possibly in subclasses for
     * game-specific checks.
     *
     * @param company     The company laying a tile.
     * @param hex         The hex on which a tile is laid.
     * @param orientation The orientation in which the tile is laid (-1 is any).
     */
    protected boolean isTileLayAllowed(PublicCompany company, MapHex hex,
                                       int orientation) {

        return gameSpecificTileLayAllowed(company, hex, orientation);
    }

    protected boolean gameSpecificTileLayAllowed(PublicCompany company,
                                                 MapHex hex, int orientation) {
        return hex.isBlockedByPrivateCompany();
    }

    /*
     * =======================================
     *  5. TOKEN LAYING
     *  5.1. BASE TOKENS
     * =======================================
     */

    public boolean layBaseToken(LayBaseToken action) {

        String errMsg = null;
        int cost = 0;
        SpecialBaseTokenLay stl = null;
        boolean extra = false;

        MapHex hex = action.getChosenHex();
        Stop stop = action.getChosenStop();

        PublicCompany company = action.getCompany();
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
            if (hex.hasTokenOfCompany(company)) {
                errMsg =
                        LocalText.getText("TileAlreadyHasToken", hex.getId(),
                                companyName);
                break;
            }

            if (action != null) {
                List<MapHex> locations = action.getLocations();
                if (locations != null && locations.size() > 0
                        && !locations.contains(hex) && !locations.contains(null)) {
                    errMsg =
                            LocalText.getText("TokenLayingHexMismatch",
                                    hex.getId(), action.getLocationNameString());
                    break;
                }
                stl = action.getSpecialProperty();
                if (stl != null) extra = stl.isExtra();
            }

            cost = company.getBaseTokenLayCost(hex);
            if (stl != null && stl.isFree()) cost = 0;

            // Does the company have the money?
            if (cost > company.getCash()) {
                errMsg =
                        LocalText.getText("NotEnoughMoney", companyName,
                                Bank.format(this,
                                        company.getCash()),
                                Bank.format(this, cost));
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(
                    this,
                    LocalText.getText("CannotLayBaseTokenOn", companyName,
                            hex.getId(), Bank.format(this, cost), errMsg));
            return false;
        }

        /* End of validation, start of execution */

        if (hex.layBaseToken(company, stop)) {
            /* TODO: the false return value must be impossible. */

            company.layBaseToken(hex, cost);

            // If this is a home base token lay, stop here
            if (action.getType() == LayBaseToken.HOME_CITY) {
                return true;
            }

            StringBuilder text = new StringBuilder();
            if (action.isCorrection()) {
                text.append(LocalText.getText("CorrectionPrefix"));
            }
            if (cost > 0) {
                String costText =
                        Currency.toBank(company, cost);
                text.append(LocalText.getText("LAYS_TOKEN_ON", companyName,
                        hex.getId(), costText));
                text.append(" ").append(stop.toText());
            } else {
                text.append(LocalText.getText("LAYS_FREE_TOKEN_ON",
                        companyName, hex.getId()));
            }
            ReportBuffer.add(this, text.toString());

            // Was a special property used?
            if (stl != null) {
                stl.setExercised();
                currentSpecialTokenLays.remove(action);
                log.debug("This was a special token lay, {} extra", extra ? "" : " not");

            }

            // Jump out if we aren't in the token laying step or it is a correction lay
            if (getStep() != GameDef.OrStep.LAY_TOKEN || action.isCorrection()) {
                return true;
            }

            if (!extra) {
                currentNormalTokenLays.clear();
                log.debug("This was a normal token lay");
            }

            if (currentNormalTokenLays.isEmpty()) {
                log.debug("No more normal token lays are allowed");
            } else if (operatingCompany.value().getNumberOfFreeBaseTokens() == 0) {
                log.debug("Normal token lay allowed by no more tokens");
                currentNormalTokenLays.clear();
            } else {
                log.debug("A normal token lay is still allowed");
            }
            setSpecialTokenLays();
            log.debug("There are now {} special token lay objects", currentSpecialTokenLays.size());
            if (currentNormalTokenLays.isEmpty()
                    && currentSpecialTokenLays.isEmpty()) {
                nextStep();
            }

        }

        return true;
    }

    /**
     * Reports if a token lay is allowed by a certain company on a certain hex
     * and city <p> This method can be used both in restricting possible actions
     * and in validating submitted actions. <p> Currently, only a few standard
     * checks are included. This method can be extended to perform other generic
     * checks, such as if a route exists, and possibly in subclasses for
     * game-specific checks.
     *
     * @param company The company laying a tile.
     * @param hex     The hex on which a tile is laid.
     * @param stop    The number of the station/city on which the token is to be laid (0 if any or immaterial).
     */
    protected boolean isTokenLayAllowed(PublicCompany company, MapHex hex, Stop stop) {
        return !hex.isBlockedForTokenLays(company, stop);
    }

    protected void setNormalTokenLays() {
        /* Normal token lays */
        currentNormalTokenLays.clear();

        /* For now, we allow one token of the currently operating company */
        if (operatingCompany.value().getNumberOfFreeBaseTokens() > 0) {
            currentNormalTokenLays.add(new LayBaseToken(getRoot(), (List<MapHex>) null));
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

        PublicCompany company = operatingCompany.value();
        if (!company.canUseSpecialProperties()) return;
        // Check if the company still has tokens
        if (company.getNumberOfFreeBaseTokens() == 0) return;

        /*
         * In 1835, this only applies to major companies. TODO: For now,
         * hardcode this, but it must become configurable later.
         */
        // Removed EV 24-11-2011 - entirely redundant; why did I ever do this??
        // if (operatingCompany.get().getType().getName().equals("Minor"))
        // return;

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
                    if (!canLay) continue;
                }
                currentSpecialTokenLays.add(new LayBaseToken(getRoot(), stl));
            }
        }
    }

    /*
     * =======================================
     *  5.2. BONUS TOKENS
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
                errMsg =
                        LocalText.getText("TokenLayingHexMismatch",
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
                //currentSpecialTokenLays.remove(action);
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
                errMsg =
                        LocalText.getText("NotEnoughMoney",
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

        if (currentNormalTokenLays.size() == 0
                && currentSpecialTokenLays.size() == 0
                && gameManager.getCommonSpecialProperties().size() == 0)
            nextStep();

        return true;
    }

    /**
     * TODO Should be merged with setSpecialTokenLays() in the future.
     * Assumptions: 1. Bonus tokens can be laid anytime during the OR. 2. Bonus
     * token laying is always extra. TODO This assumptions will be made
     * configurable conditions.
     */
    protected void setBonusTokenLays() {

        for (SpecialBonusTokenLay stl : getSpecialProperties(SpecialBonusTokenLay.class)) {
            possibleActions.add(new LayBonusToken(getRoot(), stl, stl.getToken()));
        }
    }

    /*
     * =======================================
     *  6. REVENUE AND DIVIDEND
     *  6.1. VALIDATE
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
     *  6.2. EXECUTE REVENUE AND DIVIDEND
     * =======================================
     */

    /**
     * Validate the SetRevenue action, default version
     * @param action The completed SetRevenue action
     * @return True if valid
     */
    protected String validateSetRevenueAndDividend (SetDividend action) {
        return validateSetRevenueAndDividend (action, true);
    }

    /**
     * Validate the SetRevenue action, with option to bypass the allocation check.
     * This is needed in reloading a saved file, to accept the allocation change
     * that is needed in 18Scan to process the Minor company default revenue of K10.
     * @param action The completed SetRevenue action
     * @param checkAllocation False if the allocation check must be bypassed (18Scan only).
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
                errMsg =
                        LocalText.getText("WrongCompany", companyName,
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
                if (checkAllocation) {
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
                }
            } else {
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
        //if (earnings == 0) action = checkZeroRevenue(action);

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

            if (report == null) report = LocalText.getText (
                    "CompanyDoesNotPayDividend",
                     company.getId());
            ReportBuffer.add(this, report);
            withhold(dividend);

        } else if (revenueAllocation == SetDividend.PAYOUT) {

            if (report == null) report = LocalText.getText (
                    "CompanyPaysOutFull",
                    company.getId(),
                    Bank.format(this, dividend));
            ReportBuffer.add(this, report);
            payout(dividend);

        } else if (revenueAllocation == SetDividend.SPLIT) {

            if (report == null) report = LocalText.getText (
                    "CompanySplits",
                    company.getId(),
                    Bank.format(this, dividend));
            ReportBuffer.add(this, report);
            splitRevenue(dividend);

        } else if (revenueAllocation == SetDividend.WITHHOLD) {
            if (report == null) report = LocalText.getText (
                    "CompanyWithholds",
                    company.getId(),
                    Bank.format(this, dividend));
            ReportBuffer.add(this, report);
            withhold(dividend);

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
     * @param earnings The total income from train runs.
     * @param specialRevenue Any income that needs special processing.
     * @return The resulting dividend (default: equal to the earnings).
     */
    protected int processSpecialRevenue(int earnings, int specialRevenue) {
        return earnings;
    }

    /*
     * =======================================
     *  6.3. EARNINGS DISTRIBUTION
     * =======================================
     */

    /**
     * Distribute the dividend amongst the shareholders.
     *
     * @param amount The dividend to be payed out
     */
    public void payout(int amount) {

        if (amount == 0) return;

        int part;
        int shares;

        Map<MoneyOwner, Integer> sharesPerRecipient = countSharesPerRecipient();

        // Calculate, round up, report and add the cash

        // Define a precise sequence for the reporting
        Set<MoneyOwner> recipientSet = sharesPerRecipient.keySet();
        for (MoneyOwner recipient : SequenceUtil.sortCashHolders(recipientSet)) {
            if (recipient instanceof Bank) continue;
            shares = (sharesPerRecipient.get(recipient));
            if (shares == 0) continue;

            double pricePerShare = amount * operatingCompany.value().getShareUnit() / 100.0;
            part = roundShareholderPayout (pricePerShare, shares, Rounding.UP, Multiplication.BEFORE_ROUNDING);

            String partText = Currency.fromBank(part, recipient);
            ReportBuffer.add(this, LocalText.getText("Payout",
                    recipient.getId(), partText, shares,
                    operatingCompany.value().getShareUnit()));
        }

        // Move the token
        operatingCompany.value().payout(amount);

    }

    protected Map<MoneyOwner, Integer> countSharesPerRecipient() {

        Map<MoneyOwner, Integer> sharesPerRecipient =
                new HashMap<>();

        // First count the shares per recipient
        for (PublicCertificate cert : operatingCompany.value().getCertificates()) {
            MoneyOwner recipient = getBeneficiary(cert);
            if (!sharesPerRecipient.containsKey(recipient)) {
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
     *
     * @param amount The revenue amount.
     */
    public void withhold(int amount) {

        PublicCompany company = operatingCompany.value();

        // Payout revenue to company
        Currency.fromBank(amount, company);

        // Move the token
        company.withhold(amount);

        if (!company.hasStockPrice()) return;

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
            int numberOfShares = operatingCompany.value().getNumberOfShares();

            int withheld = calculateCompanyIncomeFromSplit (amount);

            String withheldText =
                    Currency.fromBank(withheld, operatingCompany.value());

            ReportBuffer.add(this, LocalText.getText("RECEIVES",
                    operatingCompany.value().getId(), withheldText));

            // Payout the remainder
            int payed = amount - withheld;
            payout(payed);
        }

    }

    /*
     * =======================================
     *  6.4. ROUNDING
     * =======================================
     */

    /* Rounding enums */
    protected enum Rounding { UP, DOWN; }
    protected enum ToMultipleOf {
        ONE (1),
        TEN (10);
        private int value;
        ToMultipleOf (int value) { this.value = value; }
    }
    protected enum Multiplication { BEFORE_ROUNDING, AFTER_ROUNDING };

    /**
     * Rounds a split revenue or payout up or down, depending on
     * - the value of the rounding parameter (up or down),
     * - the nearest multiple to which the split revenue must be rounded.
     *   Normally one, but 18EU and 1870 have 10.
     *
     * This method is not meant to be overridden, hence the 'final' modifier.
     * To properly round a split revenue or a shareholder payout,
     * use the below overridable methods calculateCompanyIncomeFromSplit()
     * or calculateCompanyIncomeFromSplit(), which can be overridden to
     * apply different rules than de default ones used here.
     *
     * @param amount A double-precision value to be rounded as required
     * @param rounding Up or down
     * @param multipleOf The multiple to which the amount must be rounded to (e.g. 10 in 18EU majors)
     * @return The integer rounding result
     */
    public final int roundIncome(double amount, Rounding rounding, ToMultipleOf multipleOf) {
        // The bitwise xor works as a logical xor on booleans.
        int result;
        int multiple = multipleOf.value;
        if (rounding == Rounding.DOWN) {
            // Round down
            result = multiple * ((int) (amount/multiple + 0.01));
        } else {
            // Round up
            result = multiple * ((int) (amount/multiple + 0.51));
        }
        log.info("$$$ [{},{}] {} rounded to {}", rounding, multipleOf, amount, result);
        return result;
    }

   /** To set the shareholder payout before rounding */
    protected final int roundShareholderPayout (double payoutPerShare, int shares,
                                             Rounding rounding, Multiplication beforeOrAfter) {
        int result;
        if (beforeOrAfter == Multiplication.BEFORE_ROUNDING) {
            // First multiply, then round
            result = roundIncome(shares * payoutPerShare, rounding, ToMultipleOf.ONE);
        } else {
            // First round, then multiply
            result = shares * roundIncome(payoutPerShare, rounding, ToMultipleOf.ONE);
        }
        log.info("$$$ [{},{}] {}*{} rounded to {}", rounding, beforeOrAfter, shares, payoutPerShare, result);
        return result;
    }

    /**
     * Default version for calculating the company part of
     * a revenue amount being split.
     *
     * This method should be overriden in games where a different rule applies.
     *
     * @param revenue The revenue amount to be split.
     * @return The part that goes directly to the company treasury.
     * (the difference is to be payed out to the shareholders).
     *
     */
    protected int calculateCompanyIncomeFromSplit (int revenue) {
        return roundIncome(0.5 * revenue, Rounding.DOWN, ToMultipleOf.ONE);
    }

    /**
     * Default version of calculating a shareholder's part
     * of a dividend to be payed out.
     *
     * This method should be overridden in games where a different rule applies.
     *
     * @param revenue The revenue amount to be split.
     * @return The part that goes directly to the company treasury.
     * (the difference is to be payed out to the shareholders).
     *
     */
    protected int calculateShareholderPayout (double payoutPerShare, int numberOfShares) {
        return roundShareholderPayout(payoutPerShare, numberOfShares,
                Rounding.UP, Multiplication.BEFORE_ROUNDING);
    }

    /*
     * =======================================
     *  6.5. DEDUCTIONS
     * =======================================
     */

    /**
     * Default version, to be overridden if need be
     */
    protected int checkForDeductions(SetDividend action) {
        return action.getActualRevenue();
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
                errMsg =
                        LocalText.getText("WrongCompany", companyName,
                                operatingCompany.value().getId());
                break;
            }
            // amount is available
            if ((amount + operatingCompany.value().getCash()) < 0) {
                errMsg =
                        LocalText.getText("NotEnoughMoney", companyName,
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
                log.error("Company {} has no free token", operatingCompany.value().getId());
                return false;
            } else {
                // FIXME: Check where to lay the base tokens in NoMapMode
                // (bank.getUnavailable().addBonusToken(token));
            }
            operatingCompany.value().layBaseTokennNoMapMode(amount);
            ReportBuffer.add(this, LocalText.getText("OCLayBaseTokenExecuted",
                    operatingCompany.value().getId(), cashText));
        }

        return true;
    }

    /*
     * =======================================
     *  6.6. PREPARE ACTION
     * =======================================
     */

    protected void prepareRevenueAndDividendAction() {

        // There is only revenue if there are any trains
        if (operatingCompany.value().hasTrains()) {
            int[] allowedRevenueActions;
            if (operatingCompany.value().isSplitAlways()) {
                allowedRevenueActions = new int[]{
                        SetDividend.SPLIT};
            } else if (operatingCompany.value().isSplitAllowed()) {
                allowedRevenueActions = new int[]{
                        SetDividend.PAYOUT,
                        SetDividend.SPLIT,
                        SetDividend.WITHHOLD};
            } else {
                allowedRevenueActions = new int[]{
                        SetDividend.PAYOUT,
                        SetDividend.WITHHOLD};
            }

            possibleActions.add(new SetDividend(getRoot(),
                    operatingCompany.value().getLastRevenue(), true,
                    allowedRevenueActions));
        }
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
            Set<Integer> baseCosts =
                    operatingCompany.value().getBaseTokenLayCosts();

            // change to set to allow for identity and ordering
            Set<Integer> costsSet = new TreeSet<>();
            for (int cost : baseCosts)
                if (!(cost == 0 && baseCosts.size() != 1)) // fix for sequence
                    // based home token
                    costsSet.add(cost);

            // SpecialBaseTokenLay Actions - workaround for a better handling of
            // those later
            for (SpecialBaseTokenLay stl : getSpecialProperties(SpecialBaseTokenLay.class)) {
                log.debug("Special tokenlay property: {}", stl);
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
     *  7. TRAIN PURCHASING
     * =======================================
     */

    public boolean buyTrain(BuyTrain action) {

        Train train = action.getTrain();
        PublicCompany company = action.getCompany();
        String companyName = company.getId();
        Train exchangedTrain = action.getExchangedTrain();
        SpecialTrainBuy stb = null;

        String errMsg = null;
        boolean presidentMustSellShares = false;
        boolean companyMustSellShares = false;
        int trainPrice = action.getPricePaid();
        int companyCash = company.getCash();

        int presidentCash;
        int actualPresidentCash = 0;
        int cashToBeRaisedByPresident = 0;
        Player currentPlayer = operatingCompany.value().getPresident();
        int playerCash = currentPlayer != null ? currentPlayer.getCash() : 0;

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

            if (!company.mayBuyTrainType(train)) {
                errMsg = LocalText.getText("MayNotBuyTrain",
                        company, train.getType());
            }


            // Amount must be non-negative
            if (trainPrice < 0) {
                errMsg =
                        LocalText.getText("NegativeAmountNotAllowed",
                                Bank.format(this, trainPrice));
                break;
            }

            // Fixed price must be honoured
            int fixedPrice = action.getFixedCost();
            if (fixedPrice != 0 && fixedPrice != trainPrice) {
                errMsg =
                        LocalText.getText("FixedPriceNotPaid",
                                Bank.format(this, trainPrice),
                                Bank.format(this, fixedPrice));
            }

            // Does the company have room for another train?
            int trainLimit = company.getCurrentTrainLimit();
            if (!canBuyTrainNow() && !action.isForExchange()) {
                errMsg =
                        LocalText.getText("WouldExceedTrainLimit",
                                String.valueOf(trainLimit));
                break;
            }

            /* Check if this is an emergency buy */
            int cashToRaise = Math.max(0, trainPrice - companyCash);
            if (emergency || cashToRaise > 0) { // Not all games set emergency yet
                if (willBankruptcyOccur(company, cashToRaise)) {
                    DisplayBuffer.add(this, LocalText.getText("YouMustRaiseCashButCannot",
                            Bank.format(this, cashToRaise)));
                    if (GameDef.getParmAsBoolean(this, GameDef.Parm.EMERGENCY_COMPANY_BANKRUPTCY)) {
                        company.setBankrupt();
                        gameManager.registerCompanyBankruptcy(company);

                        return true;
                    }
                }

                // In some games (SOH) company must sell treasury shares first
                if (GameDef.getParmAsBoolean(this, GameDef.Parm.EMERGENCY_MUST_SELL_TREASURY_SHARES)) {
                    PortfolioModel portfolio = company.getPortfolioModel();
                    Set<PublicCertificate> ownedCerts
                            = portfolio.getCertificates(company);
                    if (!ownedCerts.isEmpty()) {
                        int sharePrice = company.getCurrentSpace().getPrice();

                        // For now, assume that all treasury certs are single shares.
                        // Check how many certs we can and must sell.
                        List<PublicCertificate> soldCerts = new ArrayList<>();
                        int numberSold = 0;
                        for (PublicCertificate cert : ownedCerts) {
                            soldCerts.add (cert);
                            if (++numberSold * sharePrice >= cashToRaise) break;
                        }
                        // Don't exceed the 50% pool limit
                        numberSold = Math.min (numberSold,
                                GameDef.getParmAsInt(this,
                                        GameDef.Parm.POOL_SHARE_LIMIT )/ company.getShareUnit()
                                        - pool.getShares(company));
                        int raisedCash = numberSold * sharePrice;

                        // Get the money
                        String cashText = Currency.fromBank(raisedCash, company);
                        String message;
                        if (numberSold == 1) {
                            message = LocalText.getText("SELL_SHARE_LOG",
                                    companyName + " ("+currentPlayer.getId()+")",
                                    company.getShareUnit(),
                                    companyName,
                                    cashText);
                        } else {
                            message = LocalText.getText("SELL_SHARES_LOG",
                                    companyName + " ("+currentPlayer.getId()+")",
                                    numberSold,
                                    company.getShareUnit(),
                                    numberSold * company.getShareUnit(),
                                    companyName,
                                    cashText);
                        }
                        ReportBuffer.add (this,  message);
                        DisplayBuffer.add (this, message);

                        // Transfer the sold certificates
                        Portfolio.moveAll(soldCerts, pool.getParent());
                        stockMarket.sell(company, company, numberSold);
                        cashToRaise -= raisedCash;
                        if (cashToRaise <= 0) break;
                        companyCash += raisedCash;
                   }
                }

                // Check what the president can add
                //presidentCash = action.getPresidentCashToAdd();
                if (playerCash >= cashToRaise) {
                    actualPresidentCash = cashToRaise;
                } else {
                    presidentMustSellShares = true;
                    cashToBeRaisedByPresident = cashToRaise - playerCash;
                }
            } else if (action.mayPresidentAddCash()) {
                // From another company
                presidentCash = trainPrice - operatingCompany.value().getCash();
                if (presidentCash > action.getPresidentCashToAdd()) {
                    errMsg =
                            LocalText.getText(
                                    "PresidentMayNotAddMoreThan",
                                    Bank.format(this,
                                            action.getPresidentCashToAdd()));
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
                if (trainPrice > companyCash) {
                    errMsg =
                            LocalText.getText(
                                    "NotEnoughMoney",
                                    companyName,
                                    Bank.format(this,
                                            operatingCompany.value().getCash()),
                                    Bank.format(this, trainPrice));
                    break;
                }
            }

            if (action.isForExchange()) {
                if (exchangedTrain == null) {
                    errMsg = LocalText.getText("NoExchangedTrainSpecified");
                    // TEMPORARY FIX to clean up invalidated saved files - DOES
                    // NOT WORK!!??
                    // exchangedTrain =
                    // operatingCompany.getObject().getPortfolio().getTrainList().get(0);
                    // action.setExchangedTrain(exchangedTrain);
                    break;
                } else if (operatingCompany.value().getPortfolioModel().getTrainOfType(
                        exchangedTrain.getType()) == null) {
                    errMsg =
                            LocalText.getText("CompanyDoesNotOwnTrain",
                                    operatingCompany.value().getId(),
                                    exchangedTrain.toText());
                    break;
                }
            }

            stb = action.getSpecialProperty();
            // TODO Note: this is not yet validated

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(
                    this,
                    LocalText.getText("CannotBuyTrainFor", companyName,
                            train.toText(), Bank.format(this, trainPrice), errMsg));
            return false;
        }

        /* End of validation, start of execution */

        Phase previousPhase = Phase.getCurrent(this);

        if (presidentMustSellShares) {
            savedAction = action;

            gameManager.startShareSellingRound(
                    operatingCompany.value().getPresident(),
                    cashToBeRaisedByPresident, operatingCompany.value(), true);

            return true;
        }

        if (actualPresidentCash > 0) {
            // MoneyModel.cashMove(currentPlayer, operatingCompany.value(),
            // presidentCash);
            String cashText =
                    Currency.wire(currentPlayer, actualPresidentCash,
                            operatingCompany.value());
            ReportBuffer.add(this, LocalText.getText("PresidentAddsCash",
                    operatingCompany.value().getId(), currentPlayer.getId(),
                    cashText));
        }

        Owner oldOwner = train.getCard().getOwner();

        if (exchangedTrain != null) {
            Train oldTrain =
                    operatingCompany.value().getPortfolioModel().getTrainOfType(
                            exchangedTrain.getType());
            (train.isObsolete() ? scrapHeap : pool).addTrainCard(oldTrain.getCard());
            ReportBuffer.add(this, LocalText.getText("ExchangesTrain",
                    companyName, exchangedTrain.toText(), train.toText(),
                    oldOwner.getId(), Bank.format(this, trainPrice)));
        } else if (stb == null) {
            ReportBuffer.add(this, LocalText.getText("BuysTrain", companyName,
                    train.toText(), oldOwner.getId(), Bank.format(this, trainPrice)));
        } else {
            ReportBuffer.add(this, LocalText.getText("BuysTrainUsingSP",
                    companyName, train.toText(), oldOwner.getId(),
                    Bank.format(this, trainPrice), stb.getOriginalCompany().getId()));
        }

        train.getCard().setActualTrain(train); // Needed for dual trains bought from
        // the Bank

        operatingCompany.value().buyTrain(train, trainPrice);

        if (oldOwner == ipo.getParent()) {
            train.getCardType().addToBoughtFromIPO();
            trainManager.setAnyTrainBought(true);
            // Clone the train if infinitely available
            if (train.getCardType().hasInfiniteQuantity()) {
                ipo.addTrainCard(trainManager.cloneTrain(train.getCardType()));
            }

        }
        if (oldOwner instanceof BankPortfolio) {
            trainsBoughtThisTurn.add(train.getCardType());
        }

        if (stb != null) {
            stb.setExercised();
            log.debug("This was a special train buy");
        }

        // Check if the phase has changed.
        trainManager.checkTrainAvailability(train, oldOwner);

        // Check if any companies must discard trains
        if (Phase.getCurrent(this) != previousPhase && checkForExcessTrains()) {
            stepObject.set(GameDef.OrStep.DISCARD_TRAINS);
        }

        if (trainManager.hasPhaseChanged()) newPhaseChecks();

        return true;
    }

    /**
     * Can the operating company buy a train now? Normally only calls
     * isBelowTrainLimit() to get the result. May be overridden if other
     * considerations apply (such as having a Pullmann in 18EU).
     *
     * @return True if the company has room buy a train
     */
    protected boolean canBuyTrainNow() {
        return isBelowTrainLimit();
    }

    public boolean checkForExcessTrains() {

        excessTrainCompanies = new HashMap<>();
        Player player;
        for (PublicCompany comp : operatingCompanies.view()) {
            if (comp.getPortfolioModel().getNumberOfTrains() > comp.getCurrentTrainLimit()) {
                player = comp.getPresident();
                if (!excessTrainCompanies.containsKey(player)) {
                    excessTrainCompanies.put(player,
                            new ArrayList<>(2));
                }
                excessTrainCompanies.get(player).add(comp);
            }

        }
        return !excessTrainCompanies.isEmpty();
    }

    /**
     * Predict if bankruptcy will occur in emergency train buying.
     * Must be called <i>after</i> the president has selected a train to buy,
     * and only if treasury cash is insufficient to buy a selected train.
     *
     * Currently a stub, only called in SOH (see OperatingRound_SOH).
     * May be useful in other cases, in which case the code will likely move to here.
     *
     * @param owner Either a player or a PublicCompany.
     *              Only tested for the latter (SOH).
     * @param cashToRaise The extra cash amount needed to buy a selected train.
     * @return True if bankruptcy is inevitable.
     */
    public boolean willBankruptcyOccur(Owner owner,
                                       int cashToRaise) {
        return false;
    }

    /**
     * Stub
     */
    protected void newPhaseChecks() {
    }

    protected SortedMap<Integer, Train> newEmergencyTrains;
    protected SortedMap<Integer, Train> usedEmergencyTrains;

    /**
     * Get a list of buyable trains for the currently operating company. Omit
     * trains that the company has no money for. If there is no cash to buy any
     * train from the Bank, prepare for emergency train buying.
     */
    public void setBuyableTrains() {

        PublicCompany company = operatingCompany.value();
        if (company == null) return;

        int companyCash = company.getCash();

        int cost;
        boolean useReducedPrice = false;
        boolean useNormalPrice = true;
        SpecialTrainBuy reducePrice = null;

        /*
         * Only relevant if a special property offers a reduced price,
         * and usage would imply closing a private company.
         * In such a case, both usage and non-usage are made possible.
         */
        boolean withAndWithoutDeduction;

        Set<Train> trains;
        boolean hasTrains =
                company.getPortfolioModel().getNumberOfTrains() > 0;

        // Cannot buy a train without any cash, unless you have to
        if (companyCash == 0 && hasTrains) return;

        boolean canBuyTrainNow = canBuyTrainNow();
        boolean mustBuyTrain =
                !hasTrains && company.mustOwnATrain();
        boolean mustBuyTrainWithoutRoute =
                mustBuyTrain && GameDef.getParmAsBoolean(this,
                        GameDef.Parm.MUST_BUY_TRAIN_EVEN_IF_NO_ROUTE);

        emergency = false;
        newEmergencyTrains = new TreeMap<>();
        usedEmergencyTrains = new TreeMap<>();

        // First check if any more trains may be bought from the Bank
        // Postpone train limit checking, because an exchange might be possible
        if (Phase.getCurrent(this).canBuyMoreTrainsPerTurn()
                || trainsBoughtThisTurn.isEmpty()) {
            boolean mayBuyMoreOfEachType =
                    Phase.getCurrent(this).canBuyMoreTrainsPerTypePerTurn();

            // Can a special property be used?
            // N.B. Assume that this never occurs in combination with
            // dual trains or train exchanges,
            // otherwise the below code must be duplicated above.
            // Also assume that there is max. one property with price reduction.
            PrivateCompany pc;
            for (SpecialTrainBuy stb : getSpecialProperties(SpecialTrainBuy.class)) {
                if (!stb.hasDeduction()) continue;
                reducePrice = stb;
                useReducedPrice = true;
                pc = (PrivateCompany) stb.getParent();
                // If the private closes after use, the player may choose not to use it now
                withAndWithoutDeduction = pc.closeIfAllExercised;
                // If the private does not close after use (SOH),
                // then offering the train at normal price makes no sense.
                useNormalPrice = !useReducedPrice || withAndWithoutDeduction;
            }

            /* New trains */
            trains = trainManager.getAvailableNewTrains();
            for (Train train : trains) {
                if (!company.mayBuyTrainType(train)) continue;
                if (!mayBuyMoreOfEachType
                        && trainsBoughtThisTurn.contains(train.getCardType())) {
                    continue;
                }

                // First with the reduced price, if applicable
                if (useReducedPrice) {
                    addBuyTrainOption (train, ipo, companyCash, reducePrice,
                            canBuyTrainNow, mustBuyTrain, mustBuyTrainWithoutRoute);
                }

                // With the normal price, if sensible
                if (useNormalPrice) {
                    addBuyTrainOption (train, ipo, companyCash, null,
                            canBuyTrainNow, mustBuyTrain,mustBuyTrainWithoutRoute);
                }

                // Even at train limit, exchange is allowed (per 1856)
                // Assumed: no game has both exchange and price reduction
                if (train.canBeExchanged() && hasTrains) {
                    cost = train.getType().getExchangeCost();
                    if (cost <= companyCash) {
                        Set<Train> exchangeableTrains =
                                company.getPortfolioModel().getUniqueTrains();
                        BuyTrain action =
                                new BuyTrain(train, ipo.getParent(), cost);
                        action.setTrainsForExchange(exchangeableTrains);
                        // if (atTrainLimit) action.setForcedExchange(true);
                        possibleActions.add(action);
                    }
                }

                // In some games (e.g. 1837) trains can be voluntary discarded
                // at train limit by a different mechanism.
                // Assumption: restricted to when buying new trains.
                if (hasTrains && !train.canBeExchanged() && !isBelowTrainLimit()) {
                    addOtherExchangesAtTrainLimit(company, train);
                }

            }
            if (!canBuyTrainNow) return;

            /* Used trains */
            trains = pool.getUniqueTrains();
            for (Train train : trains) {
                if (!company.mayBuyTrainType(train)) continue;
                if (!mayBuyMoreOfEachType
                        && trainsBoughtThisTurn.contains(train.getCardType())) {
                    continue;
                }
                if (useReducedPrice) {
                    addBuyTrainOption(train, pool, companyCash, reducePrice,
                            canBuyTrainNow, mustBuyTrain, mustBuyTrainWithoutRoute);
                }
                if (useNormalPrice) {
                    addBuyTrainOption(train, pool, companyCash, null,
                            canBuyTrainNow, mustBuyTrain, mustBuyTrainWithoutRoute);
                }
            }

            // If we must buy a train and the company does no have
            // enough cash, the president must supply the difference.
            if (emergency
                    // Some people think it's allowed in 1835 to buy a new train
                    // with president cash
                    // even if the company has enough cash to buy a used train.
                    // Players who think differently can ignore that extra option.
                    || GameDef.getParmAsBoolean(this, GameDef.Parm.EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN)
                    && !newEmergencyTrains.isEmpty()) {
                if (GameDef.getParmAsBoolean(this, GameDef.Parm.EMERGENCY_MUST_BUY_CHEAPEST_TRAIN)) {
                    // Find the cheapest one
                    // Assume there is always one available from IPO
                    int cheapestTrainCost = newEmergencyTrains.firstKey();
                    Train cheapestTrain =
                            newEmergencyTrains.get(cheapestTrainCost);
                    if (!usedEmergencyTrains.isEmpty()
                            && usedEmergencyTrains.firstKey() < cheapestTrainCost) {
                        cheapestTrainCost = usedEmergencyTrains.firstKey();
                        cheapestTrain =
                                usedEmergencyTrains.get(cheapestTrainCost);
                    }
                    if (useReducedPrice) {
                        addEmergencyBuyTrainOption(cheapestTrain, cheapestTrain.getOwner(),
                                companyCash, reducePrice, mustBuyTrain, mustBuyTrainWithoutRoute);
                    }
                    if (useNormalPrice) {
                        addEmergencyBuyTrainOption(cheapestTrain, cheapestTrain.getOwner(),
                                companyCash, null, mustBuyTrain, mustBuyTrainWithoutRoute);
                    }
                } else {
                    // All possible bank trains are buyable
                    for (Train train : newEmergencyTrains.values()) {
                        if (useReducedPrice) {
                            addEmergencyBuyTrainOption(train, train.getOwner(),
                                    companyCash, reducePrice, mustBuyTrain, mustBuyTrainWithoutRoute);
                        }
                        if (useNormalPrice) {
                            addEmergencyBuyTrainOption(train, train.getOwner(),
                                    companyCash, null, mustBuyTrain, mustBuyTrainWithoutRoute);
                        }

                    }
                    for (Train train : usedEmergencyTrains.values()) {
                        if (useReducedPrice) {
                            addEmergencyBuyTrainOption(train, train.getOwner(),
                                    companyCash, reducePrice, mustBuyTrain, mustBuyTrainWithoutRoute);
                        }
                        if (useNormalPrice) {
                            addEmergencyBuyTrainOption(train, train.getOwner(),
                                    companyCash, null, mustBuyTrain, mustBuyTrainWithoutRoute);
                        }
                    }
                }
            }
        }

        if (!canBuyTrainNow) return;

        /* Other company trains, sorted by president (current player first) */
        if (Phase.getCurrent(this).isTrainTradingAllowed()) {
            BuyTrain bt;
            Player p;
            int index;
            int numberOfPlayers = playerManager.getNumberOfPlayers();
            int presidentCash =
                    company.getPresident().getCashValue();
            int potentialCompanyCash = companyCash + treasurySharesValue(company, 0);

            // Set up a list per player of presided companies
            List<List<PublicCompany>> companiesPerPlayer =
                    new ArrayList<>(numberOfPlayers);
            for (int i = 0; i < numberOfPlayers; i++)
                companiesPerPlayer.add(new ArrayList<>(4));
            List<PublicCompany> companies;
            // Sort out which players preside over which companies.
            for (PublicCompany c : companyManager.getAllPublicCompanies()) {
                if (!c.hasFloated()) continue;
                if (c.isClosed() || c == company) continue;
                p = c.getPresident();
                if (p == null) continue; // Can occur in 18Scan
                index = p.getIndex();
                companiesPerPlayer.get(index).add(c);
            }
            // Scan trains per company per player, operating company president
            // first
            int currentPlayerIndex =
                    playerManager.getCurrentPlayer().getIndex();
            for (int i = currentPlayerIndex; i < currentPlayerIndex
                    + numberOfPlayers; i++) {
                companies = companiesPerPlayer.get(i % numberOfPlayers);
                for (PublicCompany c : companies) {
                    trains = c.getPortfolioModel().getUniqueTrains();
                    for (Train train : trains) {
                        if (!company.mayBuyTrainType(train)) continue;
                        if (train.isObsolete() || !train.isTradeable()) continue;
                        bt = null;
                        if (i != currentPlayerIndex
                                && GameDef.getParmAsBoolean(this,
                                    GameDef.Parm.FIXED_PRICE_TRAINS_BETWEEN_PRESIDENTS)
                                || operatingCompany.value().mustTradeTrainsAtFixedPrice()
                                || c.mustTradeTrainsAtFixedPrice()) {
                            // Fixed price
                            if ((companyCash >= train.getCost())
                                    && (operatingCompany.value().mayBuyTrainType(train))) {
                                bt = new BuyTrain(train, c, train.getCost());
                            } else {
                                continue;
                            }
                        } else if (companyCash > 0
                                || emergency
                                && GameDef.getParmAsBoolean(this,
                                    GameDef.Parm.EMERGENCY_MAY_BUY_FROM_COMPANY)) {
                            bt = new BuyTrain(train, c, 0);

                            // In some games the president may add extra cash up
                            // to the list price
                            if (emergency && potentialCompanyCash < train.getCost()) {
                                bt.setPresidentMayAddCash(Math.min(
                                        train.getCost() - potentialCompanyCash, presidentCash));
                            }
                        }
                        if (bt != null) possibleActions.add(bt);
                    }
                }
            }
        }

        if (!operatingCompany.value().mustOwnATrain()
                || operatingCompany.value().getPortfolioModel().getNumberOfTrains() > 0) {
            doneAllowed = true;
        }
    }

    protected void addOtherExchangesAtTrainLimit(PublicCompany company, Train train) {
    }

    /**
     * Calculate the potential contribution of selling any treasury
     * shares of a given company to pay for an "emergency" train.
     * It is assumed that no more shares may be sold than is necessary
     * to pay that bill.
     * @param company The company that must buy a train.
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
                            && numberToSell < numberAvailable) {}
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
        if (reducePrice != null) cost = reducePrice.getPrice(cost);
        if (cost <= companyCash) {
            if (canBuyTrainNow) {
                BuyTrain action =
                        new BuyTrain(train, train.getType(), from.getParent(), cost);
                action.setForcedBuyIfNoRoute(mustBuyTrainWithoutRoute);
                action.setForcedBuyIfHasRoute(mustBuyTrain);
                if (reducePrice != null) action.setSpecialProperty(reducePrice);
                possibleActions.add(action);
            }
        } else if (mustBuyTrain) {
            emergency = true;
            if (from == ipo) {
                newEmergencyTrains.put(cost, train);
            } else {
                usedEmergencyTrains.put(cost, train);
            }
        }
    }

    protected void addEmergencyBuyTrainOption (Train train, Owner from,
                                               int companyCash, SpecialTrainBuy reducePrice,
                                               boolean mustBuyTrain,
                                               boolean mustBuyTrainWithoutRoute) {
        int cost = train.getCost();
        if (reducePrice != null) cost = reducePrice.getPrice(cost);
        BuyTrain bt = new BuyTrain(train, from, cost);
        PublicCompany company = operatingCompany.value();

        int cashToRaise = cost-companyCash;
        int presidentCash = cashToRaise;
        if (cashToRaise > 0) {
            // Check if company has shares to sell (as in SOH).
            // If so, reduce or nullify the president cash contribution
            int sharesValue = treasurySharesValue(company, cashToRaise);
            if (sharesValue > 0) {
                presidentCash -= sharesValue;
                if (presidentCash < 0) presidentCash = 0;
                bt.setExtraMessage(LocalText.getText ("MustSellShares",
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
        if (GameDef.getParmAsBoolean(this, GameDef.Parm.REMOVE_TRAIN_BEFORE_SR)
                && trainManager.isAnyTrainBought()) {
            Train train =
                    Iterables.get(trainManager.getAvailableNewTrains(), 0);
            if (train.getCardType().hasInfiniteQuantity()) return;
            //Dont export Permanent Trains; MBR: 030102021
            if (train.getCardType().isPermanent()) return;
                   scrapHeap.addTrainCard(train.getCard());
            ReportBuffer.add(this,
                    LocalText.getText("RemoveTrain", train.toText()));
            //MBr: 03012021 Trains were not made available after export prior
            trainManager.checkTrainAvailability(train,bank.getIpo());
            //MBr: 02012021 - 18Chesapeake Remove a non permanent train before every Stockround
        } else {
            if (GameDef.getParmAsBoolean(this, GameDef.Parm.REMOVE_TRAIN_BEFORE_SR)
                    && (!GameDef.getParmAsBoolean(this, GameDef.Parm.REMOVE_PERMANENT))) {
                Train train =
                        Iterables.get(trainManager.getAvailableNewTrains(), 0);
                if (train.getCardType().isPermanent()) return;
                if (train.getCardType().hasInfiniteQuantity()) return;
                scrapHeap.addTrainCard(train.getCard());
                trainManager.checkTrainAvailability(train, bank.getIpo());
                ReportBuffer.add(this,
                        LocalText.getText("RemoveTrain", train.toText()));
            }
        else { //MBr: 03012021 export a train if one has been sold....
                if (GameDef.getParmAsBoolean(this, GameDef.Parm.REMOVE_TRAIN_BEFORE_SR)
                        && trainManager.isAnyTrainBought()) {
                    Train train =
                            Iterables.get(trainManager.getAvailableNewTrains(), 0);
                    if (train.getCardType().hasInfiniteQuantity()) return;
                    scrapHeap.addTrainCard(train.getCard());
                    ReportBuffer.add(this,
                            LocalText.getText("RemoveTrain", train.toText()));
                    //MBr: 03012021 Trains were not made available after export prior
                    trainManager.checkTrainAvailability(train, bank.getIpo());
                }
            }
        }
    }

    /*
     * =======================================
     *  8. VARIOUS UTILITIES
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
    public String getCompAndPresName (PublicCompany company) {
        return company.getId() + "(" + company.getPresident().getId() + ")";
    }

    // Observer methods
    public Observable getObservable() {
        return stepObject;
    }

}
