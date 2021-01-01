package net.sf.rails.game;

import net.sf.rails.common.*;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Configure;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.PlayerManager.PlayerOrderModel;
import net.sf.rails.game.financial.*;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.special.LocatedBonus;
import net.sf.rails.game.special.SellBonusToken;
import net.sf.rails.game.special.SpecialBonusTokenLay;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.*;
import net.sf.rails.game.state.Currency;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.util.GameLoader;
import net.sf.rails.util.GameSaver;
import net.sf.rails.util.Util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;
import rails.game.correct.CorrectionAction;
import rails.game.correct.CorrectionManager;
import rails.game.correct.CorrectionType;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;

import com.google.common.collect.ComparisonChain;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded &agrave; la 1830.
 */
public class GameManager extends RailsManager implements Configurable, Owner {

    protected enum GameEnd {
        UNDEFINED,
        AFTER_THIS_OR,
        AFTER_SET_OF_ORS
    }

    private static final Logger log = LoggerFactory.getLogger(GameManager.class);

    public static final String ARCHIVE_ENABLED = "save.archive.enabled";
    public static final String ARCHIVE_DIRECTORY = "save.archive.dir";
    public static final String ARCHIVE_KEEP_COUNT = "save.archive.keep_count";


    protected Class<? extends StockRound> stockRoundClass = StockRound.class;
    protected Class<? extends OperatingRound> operatingRoundClass = OperatingRound.class;
    protected Class<? extends ShareSellingRound> shareSellingRoundClass = ShareSellingRound.class;

    // Variable UI Class names
    protected String gameUIManagerClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.GAME_UI_MANAGER);
    protected String orUIManagerClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.OR_UI_MANAGER);
    protected String gameStatusClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.GAME_STATUS);
    protected String statusWindowClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.STATUS_WINDOW);
    protected String orWindowClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.OR_WINDOW);
    protected String startRoundWindowClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.START_ROUND_WINDOW);

    // map of correctionManagers
    protected final Map<CorrectionType, CorrectionManager> correctionManagers = new HashMap<>();

    protected int currentNumberOfOperatingRounds = 1;
    protected boolean skipFirstStockRound = false;
    protected boolean showCompositeORNumber = true;

    protected boolean forcedSellingCompanyDump = true;
    protected boolean gameEndsWithBankruptcy = false;
    protected int gameEndsWhenBankHasLessOrEqual = 0;
    protected GameEnd gameEndWhen = GameEnd.UNDEFINED;

    protected boolean dynamicOperatingOrder = true;

    /**
     * Will only be set during game reload
     */
    protected boolean reloading = false;

    protected final EnumMap<GameDef.Parm, Object> gameParameters = new EnumMap<>(GameDef.Parm.class);

    /**
     * Current round should not be set here but from within the Round classes.
     * This is because in some cases the round has already changed to another
     * one when the constructor terminates. Example: if the privates have not
     * been sold, it finishes by starting an Operating Round, which handles the
     * privates payout and then immediately starts a new Start Round.
     */
    protected final GenericState<RoundFacade> currentRound = new GenericState<>(this, "currentRound");
    protected RoundFacade interruptedRound = null;

    protected final IntegerState startRoundNumber = IntegerState.create(this, "startRoundNumber");
    protected final IntegerState stockRoundNumber = IntegerState.create(this, "srNumber");

    protected final IntegerState absoluteORNumber = IntegerState.create(this, "absoluteORNUmber");
    protected final IntegerState relativeORNumber = IntegerState.create(this, "relativeORNumber");
    protected final IntegerState numOfORs = IntegerState.create(this, "numOfORs");

    protected int maxRounds; // For SOH

    protected final BooleanState firstAllPlayersPassed = new BooleanState(this, "firstAllPlayersPassed");


    /**
     * GameOver pending, a last OR or set of ORs must still be completed
     */
    protected final BooleanState gameOverPending = new BooleanState(this, "gameOverPending");
    /**
     * GameOver is executed, no more moves
     */
    protected final BooleanState gameOver = new BooleanState(this, "gameOver");
    protected Boolean gameOverReportedUI = false;
    protected final BooleanState endedByBankruptcy = new BooleanState(this, "endedByBankruptcy");

    /**
     * UI display hints
     */
    protected GuiHints guiHints;

    /** Flags to be passed to the UI, aiding the layout definition */
    protected final EnumMap<GuiDef.Parm, Boolean> guiParameters = new EnumMap<>(GuiDef.Parm.class);

    protected GenericState<StartPacket> startPacket = new GenericState<>(this, "startPacket");

    protected PossibleActions possibleActions = PossibleActions.create();

    protected final ArrayListState<PossibleAction> executedActions = new ArrayListState<>(this, "executedActions");

    /**
     * Special properties that can be used by other players or companies
     * than just the owner (such as buyable bonus tokens as in 1856).
     */
    protected Portfolio<SpecialProperty> commonSpecialProperties = null;

    /**
     * indicates that the recoverySave already issued a warning, avoids displaying several warnings
     */
    protected boolean recoverySaveWarning = true;

    /**
     * Flag to skip a subsequent Done action (if present) during reloading.
     * <br>This is a fix to maintain backwards compatibility when redundant
     * actions are skipped in new code versions (such as the bypassing of
     * a treasury trading step if it cannot be executed).
     * <br>This flag must be reset after processing <i>any</i> action (not just Done).
     */
    protected boolean skipNextDone = false;
    /**
     * Step that must be in effect to do an actual Done skip during reloading.
     * <br> This is to ensure that Done actions in different OR steps are
     * considered separately.
     */
    protected GameDef.OrStep skippedStep = null;

    // storage to replace static class variables
    // TODO: Move that to a better place
    protected Map<String, Object> objectStorage = new HashMap<>();
    protected Map<String, Integer> storageIds = new HashMap<>();

    private int revenueSpinnerIncrement = 10;
    //Used for Storing the PublicCompany to be Founded by a formationround
    private PublicCompany nationalToFound;

    private final Map<PublicCompany, Player> NationalFormStartingPlayer = new HashMap<>();

    protected PlayerOrderModel playerNamesModel;

    /**
     * @return the revenueSpinnerIncrement
     */
    public int getRevenueSpinnerIncrement() {
        return revenueSpinnerIncrement;
    }

    private int actionCount = 0; // To log during reloading

    public GameManager(RailsRoot parent, String id) {
        super(parent, id);

        this.guiHints = new GuiHints(this, "guiHints");
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {
        /* Get the rails.game name as configured */
        Tag gameTag = tag.getChild("Game");
        if (gameTag == null)
            throw new ConfigurationException(
                    "No Game tag specified in GameManager tag");

        String gameName = gameTag.getAttributeAsString("name");
        // TODO (Rails 2.0): Check if this still works and is still needed
        if (gameName == null) {
            throw new ConfigurationException("No gameName specified in Game tag");
        }
        if (!gameName.equals(getRoot().getGameName())) {
            throw new ConfigurationException("Deviating gameName specified in Game tag");
        }

        initGameParameters();

        Tag gameParmTag = tag.getChild("GameParameters");
        if (gameParmTag != null) {


            // StockRound class and other properties
            Tag srTag = gameParmTag.getChild("StockRound");
            if (srTag != null) {
                // FIXME: Rails 2.0, move this to some default .xml!
                String srClassName =
                        srTag.getAttributeAsString("class", "net.sf.rails.game.financial.StockRound");
                try {
                    stockRoundClass =
                            Class.forName(srClassName).asSubclass(StockRound.class);
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("Cannot find class "
                            + srClassName, e);
                }
                String stockRoundSequenceRuleString =
                        srTag.getAttributeAsString("sequence");
                if (Util.hasValue(stockRoundSequenceRuleString)) {
                    if ( "SellBuySell".equalsIgnoreCase(stockRoundSequenceRuleString)) {
                        setGameParameter(GameDef.Parm.STOCK_ROUND_SEQUENCE,
                                StockRound.SELL_BUY_SELL);
                    } else if ( "SellBuy".equalsIgnoreCase(stockRoundSequenceRuleString)) {
                        setGameParameter(GameDef.Parm.STOCK_ROUND_SEQUENCE,
                                StockRound.SELL_BUY);
                    } else if ( "SellBuyOrBuySell".equalsIgnoreCase(stockRoundSequenceRuleString)) {
                        setGameParameter(GameDef.Parm.STOCK_ROUND_SEQUENCE,
                                StockRound.SELL_BUY_OR_BUY_SELL);
                    }
                }

                skipFirstStockRound =
                        srTag.getAttributeAsBoolean("skipFirst",
                                skipFirstStockRound);

                for (String ruleTagName : srTag.getChildren().keySet()) {
                    switch (ruleTagName) {
                        case "NoSaleInFirstSR":
                            setGameParameter(GameDef.Parm.NO_SALE_IN_FIRST_SR, true);
                            break;
                        case "NoSaleIfNotOperated":
                            setGameParameter(GameDef.Parm.NO_SALE_IF_NOT_OPERATED, true);
                            break;
                        case "NoSaleOfJustBoughtShare":
                            setGameParameter(GameDef.Parm.NO_SALE_OF_JUST_BOUGHT_CERT, true);
                            break;
                        case "NoSaleOfJustStartedCompany":
                            setGameParameter(GameDef.Parm.NO_SALE_OF_JUST_STARTED_COMPANY, true);
                            break;
                    }

                }
            }

            // OperatingRound class
            Tag orTag = gameParmTag.getChild("OperatingRound");
            if (orTag != null) {
                // FIXME: Rails 2.0, move this to some default .xml!
                String orClassName =
                        orTag.getAttributeAsString("class", "net.sf.rails.game.OperatingRound");
                try {
                    operatingRoundClass =
                            Class.forName(orClassName).asSubclass(
                                    OperatingRound.class);
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("Cannot find class "
                            + orClassName, e);
                }

                Tag orderTag = orTag.getChild("OperatingOrder");
                if (orderTag != null) {
                    dynamicOperatingOrder = orderTag.getAttributeAsBoolean("dynamic",
                            dynamicOperatingOrder);
                }

                Tag emergencyTag = orTag.getChild("EmergencyTrainBuying");
                if (emergencyTag != null) {
                    setGameParameter(GameDef.Parm.EMERGENCY_MUST_BUY_CHEAPEST_TRAIN,
                            emergencyTag.getAttributeAsBoolean("mustBuyCheapestTrain",
                                    GameDef.Parm.EMERGENCY_MUST_BUY_CHEAPEST_TRAIN.defaultValueAsBoolean()));
                    setGameParameter(GameDef.Parm.EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN,
                            emergencyTag.getAttributeAsBoolean("mayAlwaysBuyNewTrain",
                                    GameDef.Parm.EMERGENCY_MAY_ALWAYS_BUY_NEW_TRAIN.defaultValueAsBoolean()));
                    setGameParameter(GameDef.Parm.EMERGENCY_MAY_BUY_FROM_COMPANY,
                            emergencyTag.getAttributeAsBoolean("mayBuyFromCompany",
                                    GameDef.Parm.EMERGENCY_MAY_BUY_FROM_COMPANY.defaultValueAsBoolean()));
                    setGameParameter(GameDef.Parm.EMERGENCY_COMPANY_BANKRUPTCY,
                            emergencyTag.getAttributeAsBoolean("companyBankruptcy",
                                    GameDef.Parm.EMERGENCY_COMPANY_BANKRUPTCY.defaultValueAsBoolean()));
                    setGameParameter(GameDef.Parm.EMERGENCY_MUST_SELL_TREASURY_SHARES,
                            emergencyTag.getAttributeAsBoolean("mustSellTreasuryShares",
                                    GameDef.Parm.EMERGENCY_MUST_SELL_TREASURY_SHARES.defaultValueAsBoolean()));
                    setGameParameter(GameDef.Parm.MUST_BUY_TRAIN_EVEN_IF_NO_ROUTE,
                            emergencyTag.getAttributeAsBoolean("mustBuyTrainEvenIfNoRoute",
                                    GameDef.Parm.MUST_BUY_TRAIN_EVEN_IF_NO_ROUTE.defaultValueAsBoolean()));
                }
                Tag revenueIncrementTag = orTag.getChild("RevenueIncrement");
                if (revenueIncrementTag != null) {
                    revenueSpinnerIncrement = revenueIncrementTag.getAttributeAsInteger("amount", 10);
                }
            }

            // ShareSellingRound class
            Tag ssrTag = gameParmTag.getChild("ShareSellingRound");
            if (ssrTag != null) {
                // FIXME: Rails 2.0, move this to some default .xml!
                String ssrClassName =
                        ssrTag.getAttributeAsString("class", "net.sf.rails.game.financial.ShareSellingRound");
                try {
                    shareSellingRoundClass =
                            Class.forName(ssrClassName).asSubclass(ShareSellingRound.class);
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("Cannot find class "
                            + ssrClassName, e);
                }
            }

            /* Max. % of shares of one company that a player may hold */
            Tag shareLimitTag = gameParmTag.getChild("PlayerShareLimit");
            if (shareLimitTag != null) {
                setGameParameter(GameDef.Parm.PLAYER_SHARE_LIMIT,
                        shareLimitTag.getAttributeAsInteger("percentage",
                                GameDef.Parm.PLAYER_SHARE_LIMIT.defaultValueAsInt()));
            }

            /* Max. % of shares of one company that the bank pool may hold */
            Tag poolLimitTag = gameParmTag.getChild("BankPoolShareLimit");
            if (poolLimitTag != null) {
                setGameParameter(GameDef.Parm.POOL_SHARE_LIMIT,
                        shareLimitTag.getAttributeAsInteger("percentage",
                                GameDef.Parm.POOL_SHARE_LIMIT.defaultValueAsInt()));
            }

            /* Max. % of own shares that a company treasury may hold */
            Tag treasuryLimitTag = gameParmTag.getChild("TreasuryShareLimit");
            if (treasuryLimitTag != null) {
                setGameParameter(GameDef.Parm.TREASURY_SHARE_LIMIT,
                        shareLimitTag.getAttributeAsInteger("percentage",
                                GameDef.Parm.TREASURY_SHARE_LIMIT.defaultValueAsInt()));
            }
        }


        /* End of rails.game criteria */
        Tag endOfGameTag = tag.getChild("EndOfGame");
        if (endOfGameTag != null) {
            Tag forcedSellingTag = endOfGameTag.getChild("ForcedSelling");
            if (forcedSellingTag != null) {
                forcedSellingCompanyDump =
                        forcedSellingTag.getAttributeAsBoolean("CompanyDump", true);
            }
            if (endOfGameTag.getChild("Bankruptcy") != null) {
                gameEndsWithBankruptcy = true;
            }
            Tag bankBreaksTag = endOfGameTag.getChild("BankBreaks");
            if (bankBreaksTag != null) {
                gameEndsWhenBankHasLessOrEqual =
                        bankBreaksTag.getAttributeAsInteger("limit",
                                gameEndsWhenBankHasLessOrEqual);
                String attr = bankBreaksTag.getAttributeAsString("finish");
                if ( "setOfORs".equalsIgnoreCase(attr)) {
                    gameEndWhen = GameEnd.AFTER_SET_OF_ORS;
                } else if ( "currentOR".equalsIgnoreCase(attr)) {
                    gameEndWhen = GameEnd.AFTER_THIS_OR;
                }
            }
            if (endOfGameTag.getChild("Bankruptcy") != null) {
                gameEndsWithBankruptcy = true;
            }
            Tag maxPriceTag = endOfGameTag.getChild("MaxPriceReached");
            if (maxPriceTag != null) {
                String attr = maxPriceTag.getAttributeAsString("finish");
                if ( "setOfORs".equalsIgnoreCase(attr)) {
                    gameEndWhen = GameEnd.AFTER_SET_OF_ORS;
                } else if ( "currentOR".equalsIgnoreCase(attr)) {
                    gameEndWhen = GameEnd.AFTER_THIS_OR;
                }
            }
            Tag maxRoundsTag = endOfGameTag.getChild("MaxRoundsReached");
            if (maxRoundsTag != null) {
                maxRounds = maxRoundsTag.getAttributeAsInteger("number", 0);
            }
        }

        Tag guiClassesTag = tag.getChild("GuiClasses");
        if (guiClassesTag != null) {

            // GameUIManager class
            Tag gameUIMgrTag = guiClassesTag.getChild("GameUIManager");
            if (gameUIMgrTag != null) {
                gameUIManagerClassName = gameUIMgrTag.getAttributeAsString("class", gameUIManagerClassName);
                // Check instantiatability (not sure if this belongs here)
                Configure.canClassBeInstantiated(gameUIManagerClassName);
            }

            // ORUIManager class
            Tag orMgrTag = guiClassesTag.getChild("ORUIManager");
            if (orMgrTag != null) {
                orUIManagerClassName = orMgrTag.getAttributeAsString("class", orUIManagerClassName);
                // Check instantiatability (not sure if this belongs here)
                Configure.canClassBeInstantiated(orUIManagerClassName);
            }

            // GameStatus class
            Tag gameStatusTag = guiClassesTag.getChild("GameStatus");
            if (gameStatusTag != null) {
                gameStatusClassName = gameStatusTag.getAttributeAsString("class", gameStatusClassName);
                // Check instantiatability (not sure if this belongs here)
                Configure.canClassBeInstantiated(gameStatusClassName);
            }

            // StatusWindow class
            Tag statusWindowTag = guiClassesTag.getChild("StatusWindow");
            if (statusWindowTag != null) {
                statusWindowClassName = statusWindowTag.getAttributeAsString("class",
                        statusWindowClassName);
                // Check instantiatability (not sure if this belongs here)
                Configure.canClassBeInstantiated(statusWindowClassName);
            }

            // ORWindow class
            Tag orWindowTag = guiClassesTag.getChild("ORWindow");
            if (orWindowTag != null) {
                orWindowClassName = orWindowTag.getAttributeAsString("class", orWindowClassName);
                // Check instantiatability (not sure if this belongs here)
                Configure.canClassBeInstantiated(orWindowClassName);
            }

            // StartRoundWindow class
            Tag startRoundWindowTag = guiClassesTag.getChild("StartRoundWindow");
            if (startRoundWindowTag != null) {
                startRoundWindowClassName = startRoundWindowTag.getAttributeAsString("class", startRoundWindowClassName);
                // Check instantiatability (not sure if this belongs here)
                Configure.canClassBeInstantiated(startRoundWindowClassName);
            }
        }
    }

    public void finishConfiguration(RailsRoot root) {
    }

    public void init() {
        showCompositeORNumber = !"simple".equalsIgnoreCase(Config.get("or.number_format"));
    }

    public void startGame() {
        setGuiParameters();
        getRoot().getCompanyManager().initStartPackets(this);
        beginStartRound();
    }

    public boolean isDynamicOperatingOrder() {
        return dynamicOperatingOrder;
    }

    public PossibleActions getPossibleActions() {
        return possibleActions;
    }

    protected void setGuiParameters() {
        CompanyManager cm = getRoot().getCompanyManager();

        for (PublicCompany company : cm.getAllPublicCompanies()) {
            if (company.hasParPrice()) guiParameters.put(GuiDef.Parm.HAS_ANY_PAR_PRICE, true);
            if (company.canBuyPrivates()) guiParameters.put(GuiDef.Parm.CAN_ANY_COMPANY_BUY_PRIVATES, true);
            if (company.canHoldOwnShares()) guiParameters.put(GuiDef.Parm.CAN_ANY_COMPANY_HOLD_OWN_SHARES, true);
            if (company.getMaxNumberOfLoans() != 0) guiParameters.put(GuiDef.Parm.HAS_ANY_COMPANY_LOANS, true);
        }

        loop:
        for (PrivateCompany company : cm.getAllPrivateCompanies()) {
            for (SpecialProperty sp : company.getSpecialProperties()) {
                if (sp instanceof SpecialBonusTokenLay || sp instanceof SellBonusToken) {
                    guiParameters.put(GuiDef.Parm.DO_BONUS_TOKENS_EXIST, true);
                    break loop;
                }
            }

        }

        // define guiParameters from gameOptions
        if (GameOption.getAsBoolean(this, "NoMapMode")) {
            guiParameters.put(GuiDef.Parm.NO_MAP_MODE, true);
            guiParameters.put(GuiDef.Parm.ROUTE_HIGHLIGHT, false);
            guiParameters.put(GuiDef.Parm.REVENUE_SUGGEST, false);
        } else {
            if ("Highlight".equalsIgnoreCase(GameOption.getValue(this, "RouteAwareness"))) {
                guiParameters.put(GuiDef.Parm.ROUTE_HIGHLIGHT, true);
            }
            if ("Suggest".equalsIgnoreCase(GameOption.getValue(this, "RevenueCalculation"))) {
                guiParameters.put(GuiDef.Parm.REVENUE_SUGGEST, true);
            }
        }

    }

    private void initGameParameters() {
        for (GameDef.Parm parm : GameDef.Parm.values()) {
            gameParameters.put(parm, parm.defaultValue());
        }
    }

    protected void setRound(RoundFacade round) {
        currentRound.set(round);
    }

    public void nextRound(Round round) {
        if (round instanceof StartRound) {
            if (((StartRound) round).getStartPacket().areAllSold()) { //This start Round was completed
                StartPacket nextStartPacket = getRoot().getCompanyManager().getNextUnfinishedStartPacket();
                if (nextStartPacket == null) {
                    if (skipFirstStockRound) {
                        Phase currentPhase =
                                getRoot().getPhaseManager().getCurrentPhase();
                        if (currentPhase.getNumberOfOperatingRounds() != numOfORs.value()) {
                            numOfORs.set(currentPhase.getNumberOfOperatingRounds());
                        }
                        log.info("Phase={} ORs={}", currentPhase.toText(), numOfORs);

                        // Create a new OperatingRound (never more than one Stock Round)
                        // OperatingRound.resetRelativeORNumber();

                        relativeORNumber.set(1);
                        startOperatingRound(true);
                    } else {
                        startStockRound();
                    }
                } else {
                    beginStartRound();
                }
            } else {
                startOperatingRound(runIfStartPacketIsNotCompletelySold());
            }
        } else if (round instanceof StockRound) {
            Phase currentPhase = getRoot().getPhaseManager().getCurrentPhase();
            if (currentPhase == null) log.error("Current Phase is null??", new Exception(""));
            numOfORs.set(currentPhase.getNumberOfOperatingRounds());
            log.debug("Phase={} ORs={}", currentPhase.toText(), numOfORs);

            // Create a new OperatingRound (never more than one Stock Round)
            // OperatingRound.resetRelativeORNumber();
            relativeORNumber.set(1);
            startOperatingRound(true);

        } else if (round instanceof OperatingRound) {
            if (gameOverPending.value() && gameEndWhen == GameEnd.AFTER_THIS_OR) {

                finishGame();

            } else if (relativeORNumber.add(1) <= numOfORs.value()) {
                // There will be another OR
                startOperatingRound(true);
            } else if (getRoot().getCompanyManager().getNextUnfinishedStartPacket() != null) {
                beginStartRound();
            } else {
                if (gameOverPending.value() && gameEndWhen == GameEnd.AFTER_SET_OF_ORS) {
                    finishGame();
                } else {
                    ((OperatingRound) round).checkForeignSales();
                    startStockRound();
                }
            }
        }
    }

    protected void beginStartRound() {
        StartPacket startPacket = getRoot().getCompanyManager().getNextUnfinishedStartPacket();

        // check if there are still unfinished startPackets
        if (startPacket != null) {
            // set this to the current startPacket
            this.startPacket.set(startPacket);
            // start a new StartRound
            createStartRound(startPacket);
        } else {
            // otherwise
            startStockRound();
        }
    }

    protected void createStartRound(StartPacket startPacket) {
        String startRoundClassName = startPacket.getRoundClassName();
        StartRound startRound = createRound(startRoundClassName,
                "startRound_" + startRoundNumber.value());
        startRoundNumber.add(1);
        startRound.start();
    }

    /**
     * Stub, to be overridden if companies can run before the Start Packet has been completely sold
     * (as in 1835). Also see further comments at the overriding method in GameManager_18Scan.
     *
     * @return true if companies can run regardless. Default false.
     */
    protected boolean runIfStartPacketIsNotCompletelySold() {
        return false;
    }

    protected void startStockRound() {
        StockRound sr = createRound(stockRoundClass, "SR_" + stockRoundNumber.value());
        stockRoundNumber.add(1);
        sr.start();
    }

    protected void startOperatingRound(boolean operate) {
        log.debug("Operating round started with operate-flag={}", operate);
        String orId;
        if (operate) {
            orId = "OR_" + absoluteORNumber.value();
        } else {
            orId = "OR_Start_" + startRoundNumber.value();
        }
        OperatingRound or = createRound(operatingRoundClass, orId);
        if (operate) absoluteORNumber.add(1);
        or.start();
    }

    // FIXME: We need an ID!
    protected <T extends RoundFacade> T createRound(String roundClassName, String id) {
        T round = null;
        try {
            round = Configure.create((Class<T>) StartRound.class, roundClassName, GameManager.class, this, id);
        } catch (Exception e) {
            log.error("Cannot instantiate class {}", StartRound.class.getName(), e);
            System.exit(1);
        }
        setRound(round);
        return round;
    }

    // FIXME: We need an ID!
    protected <T extends RoundFacade> T createRound(Class<T> roundClass, String id) {
        T round = null;
        try {
            round = Configure.create(roundClass, GameManager.class, this, id);
        } catch (Exception e) {
            log.error("Cannot instantiate class {}", roundClass.getName(), e);
            System.exit(1);
        }
        setRound(round);
        return round;
    }

    public void newPhaseChecks(RoundFacade round) {

    }

    public void reportAllPlayersPassed() {
        ReportBuffer.add(this, LocalText.getText("ALL_PASSED"));
        firstAllPlayersPassed.set(true);
    }

    public boolean getFirstAllPlayersPassed() {
        return firstAllPlayersPassed.value();
    }

    public String getORId() {
        if (showCompositeORNumber) {
            return getCompositeORNumber();
        } else {
            return String.valueOf(absoluteORNumber.value());
        }
    }

    public int getAbsoluteORNumber() {
        return absoluteORNumber.value();
    }

    public String getCompositeORNumber() {
        return stockRoundNumber.value() + "." + relativeORNumber.value();
    }

    public int getRelativeORNumber() {
        return relativeORNumber.value();
    }

    public String getNumOfORs() {
        return numOfORs.toText();
    }

    public int getStartRoundNumber() {
        return startRoundNumber.value();
    }

    public int getSRNumber() {
        return stockRoundNumber.value();
    }

    public void startShareSellingRound(Player player, int cashToRaise,
                                       PublicCompany cashNeedingCompany, boolean problemDumpOtherCompanies) {

        interruptedRound = getCurrentRound();

        // An id based on interruptedRound and company id
        String id = "SSR_" + interruptedRound.getId() + "_" + cashNeedingCompany.getId()+"_"+cashToRaise;

        // check if other companies can be dumped
        createRound(shareSellingRoundClass, id).start(
                interruptedRound, player, cashToRaise, cashNeedingCompany,
                !problemDumpOtherCompanies || forcedSellingCompanyDump);
        // the last parameter indicates if the dump of other companies is allowed, either this is explicit or
        // the action does not require that check
    }

    public void startTreasuryShareTradingRound(PublicCompany company) {
        interruptedRound = getCurrentRound();
        String id = "TreasuryShareRound_" + interruptedRound.getId() + "_" + company.getId();
        createRound(TreasuryShareRound.class, id).start(interruptedRound);
    }

    public boolean process(PossibleAction action) {
        boolean result = true;

        getRoot().getReportManager().getDisplayBuffer().clear();
        guiHints.clearVisibilityHints();
        ChangeStack changeStack = getRoot().getStateManager().getChangeStack();
        boolean startGameAction = false;

        logActionTaken (action);

        if (action instanceof NullAction && ((NullAction) action).getMode() == NullAction.Mode.START_GAME) {
            // Skip processing at game start after Load.
            // We're only here to create PossibleActions.
            startGameAction = true;
        } else if (action != null) {
            // Should never be null.
            // EV: actually, it is null if the StartRound needs no user interaction.
            // Example: Steam Over Holland, when privates are just dealt out randomly.

            action.setActed();

            // Check player
            String actionPlayerName = action.getPlayerName();
            String currentPlayerName = getCurrentPlayer().getId();
            if (!actionPlayerName.equals(currentPlayerName)) {
                DisplayBuffer.add(this, LocalText.getText("WrongPlayer", actionPlayerName, currentPlayerName));
                return false;
            }

            // Check if the action is allowed
            if (!possibleActions.validate(action)) {
                DisplayBuffer.add(this, LocalText.getText("ActionNotAllowed", action.toString()));
                return false;
            }

            if (action instanceof GameAction) {
                // Process undo/redo centrally
                GameAction gameAction = (GameAction) action;
                result = processGameActions(gameAction);
            } else {
                // All other actions: process per round
                result = processCorrectionActions(action)
                        || getCurrentRound().process(action);
                if (result && action.hasActed()) {
                    executedActions.add(action);
                }
            }

        }

        possibleActions.clear();

        // Note: round may have changed!
        getCurrentRound().setPossibleActions();

        // TODO: SetPossibleAction can contain state changes (like initTurn)
        // Remove that and move closing the ChangeStack after the processing of the action
        if (result && !(action instanceof GameAction) && !(startGameAction)) {
            changeStack.close(action);
        }

        // only pass available => execute automatically
        if (!isGameOver() && possibleActions.containsOnlyPass()) {
            log.debug("{} may only pass", getCurrentPlayer().getId());
            result = process(possibleActions.getList().get(0));
        }

        // TODO: Check if this still works as it moved above the close of the ChangeStack
        // to have a ChangeSet open to initialize the CorrectionManagers
        if (!isGameOver()) setCorrectionActions();

        // Add the Undo/Redo possibleActions here.
        if (changeStack.isUndoPossible(getCurrentPlayer())) {
            possibleActions.add(new GameAction(getRoot(), GameAction.Mode.UNDO));
        }
        if (changeStack.isUndoPossible()) {
            possibleActions.add(new GameAction(getRoot(), GameAction.Mode.FORCED_UNDO));
        }
        if (changeStack.isRedoPossible()) {
            possibleActions.add(new GameAction(getRoot(), GameAction.Mode.REDO));
        }

        // logging of game actions activated
        log.debug ("Action result: {}", result);
        for (PossibleAction pa : possibleActions.getList()) {
            log.info("{}", pa);
        }

        return result;
    }

    protected void logActionTaken (PossibleAction action) {
        if (action instanceof NullAction
                && ((NullAction)action).getMode() == NullAction.Mode.START_GAME)    {
            log.info("*** Action 0: {}", action);
        } else if (getCurrentRound() instanceof OperatingRound) {
            OperatingRound thisOR = (OperatingRound) getCurrentRound();
            log.info("*** Action {} by {}: {}", actionCount++,
                    thisOR.getCompAndPresName(thisOR.operatingCompany.value()),
                    action);
        } else {
            log.info("*** Action {} by {}: {}", actionCount++, action.getPlayerName(), action);
        }
    }

    /**
     * Adds all Game actions
     * Examples are: undo/redo/corrections
     */
    private void setCorrectionActions() {

        // If any Correction is active
        for (CorrectionType ct : EnumSet.allOf(CorrectionType.class)) {
            CorrectionManager cm = getCorrectionManager(ct);
            if (cm.isActive()) {
                possibleActions.clear();
            }

        }

        // Correction Actions
        for (CorrectionType ct : EnumSet.allOf(CorrectionType.class)) {
            CorrectionManager cm = getCorrectionManager(ct);
            possibleActions.addAll(cm.createCorrections());
        }
    }

    private boolean processCorrectionActions(PossibleAction a) {

        boolean result = false;

        if (a instanceof CorrectionAction) {
            CorrectionAction ca = (CorrectionAction) a;
            CorrectionType ct = ca.getCorrectionType();
            CorrectionManager cm = getCorrectionManager(ct);
            result = cm.executeCorrection(ca);
        }

        return result;
    }

    private boolean processGameActions(GameAction gameAction) {
        // Process undo/redo centrally
        boolean result = false;

        ChangeStack changeStack = getRoot().getStateManager().getChangeStack();
        int index = gameAction.getmoveStackIndex();
        switch (gameAction.getMode()) {
            case SAVE:
                result = save(gameAction);
                break;
            case RELOAD:
                result = reload(gameAction);
                break;
            case UNDO:
                changeStack.undo();
                result = true;
                break;
            case FORCED_UNDO:
                if (index == -1) {
                    changeStack.undo();
                } else {
                    changeStack.undo(index);
                }
                result = true;
                break;
            case REDO:
                if (index == -1) {
                    changeStack.redo();
                } else {
                    changeStack.redo(index);
                }
                result = true;
                break;
            case EXPORT:
                result = export(gameAction);
                break;
        }

        return result;
    }

    public boolean processOnReload(PossibleAction action) {
        getRoot().getReportManager().getDisplayBuffer().clear();
        RoundFacade thisRound = getCurrentRound();

        // TEMPORARY FIX TO ALLOW OLD 1856 SAVED FILES TO BE PROCESSED
        if ( "1856".equals(getRoot().getGameName())
                //&& currentRound.get().getClass() != CGRFormationRound.class
                && possibleActions.contains(RepayLoans.class)
                && (!possibleActions.contains(action.getClass())
                || (action.getClass() == NullAction.class
                && ((NullAction) action).getMode() != NullAction.Mode.DONE))) {
            // Insert "Done"
            log.debug("Action DONE inserted");
            thisRound.process(new NullAction(getRoot(), NullAction.Mode.DONE));
            possibleActions.clear();
            thisRound.setPossibleActions();
            if (!isGameOver()) setCorrectionActions();
        }

       // Log possible actions (normally this is outcommented)
        for (PossibleAction a : possibleActions.getList()) {
            log.info("{}", a);
        }

        logActionTaken (action);

        // New in Rails2.0: Check if the action is allowed
        if (!possibleActions.validate(action)) {
            DisplayBuffer.add(this, LocalText.getText("ActionNotAllowed",
                    action.toString()));
            return false;
        }

        // FOR BACKWARDS COMPATIBILITY
        boolean doProcess = true;
        if (skipNextDone) {
            if (action instanceof NullAction
                    && ((NullAction) action).getMode() == NullAction.Mode.DONE) {
                if (currentRound.value() instanceof OperatingRound
                        && ((OperatingRound) currentRound.value()).getStep() == skippedStep) {
                    doProcess = false;
                }
            }
        }
        skipNextDone = false;
        skippedStep = null;

        ChangeStack changeStack = getRoot().getStateManager().getChangeStack();

        if (doProcess && !processCorrectionActions(action) && !getCurrentRound().process(action)) {
            String msg = "Player " + action.getPlayerName() + "'s action \""
                    + action.toString() + "\"\n  in " + getCurrentRound().getRoundName()
                    + " is considered invalid by the game engine";
            log.error(msg);
            DisplayBuffer.add(this, msg);
            return false;
        }
        executedActions.add(action);

        possibleActions.clear();
        getCurrentRound().setPossibleActions();
        changeStack.close(action);

        if (!isGameOver()) setCorrectionActions();

        log.debug("Turn: {}", getCurrentPlayer().getId());
        return true;
    }

    public void finishLoading() {
        guiHints.clearVisibilityHints();
    }

    /**
     * recoverySave method
     * Uses filePath defined in save.recovery.filepath
     */
    protected void recoverySave() {
        if (Config.get("save.recovery.active", "yes").equalsIgnoreCase("no")) return;

        GameSaver gameSaver = new GameSaver(getRoot().getGameData(), executedActions.view());
        try {
            gameSaver.autoSave();
            recoverySaveWarning = false;
        } catch (IOException e) {
            // suppress warning after first occurrence
            if (!recoverySaveWarning) {
                DisplayBuffer.add(this, LocalText.getText("RecoverySaveFailed", e.getMessage()));
                recoverySaveWarning = true;
            }
            log.error("autosave failed", e);
        }
    }

    protected boolean save(GameAction saveAction) {
        GameSaver gameSaver = new GameSaver(getRoot().getGameData(), executedActions.view());
        File file = new File(saveAction.getFilepath());
        try {
            gameSaver.saveGame(file);
        } catch (IOException e) {
            DisplayBuffer.add(this, LocalText.getText("SaveFailed", e.getMessage()));
            log.error("save failed", e);
            return false;
        }

        boolean archive = Config.getBoolean(ARCHIVE_ENABLED, false);
        if ( archive ) {
            int count = Config.getInt(ARCHIVE_KEEP_COUNT, 5);
            if ( count < 1 ) {
                count = 1;
            }

            String archiveDir = Config.get(ARCHIVE_DIRECTORY);
            if ( StringUtils.isBlank(archiveDir) ) {
                // default to "archive"
                archiveDir = "archive";
            }
            if ( ! archiveDir.startsWith(File.separator) ) {
                // it should be relative to the current saved files
                archiveDir = file.getParent() + File.separator + archiveDir;
            }
            log.debug("archiving old saved game files to {}", archiveDir);

            File archiveDirFile = new File(archiveDir);
            if ( ! archiveDirFile.exists() ) {
                // create it
                try {
                    Files.createDirectories(Path.of(archiveDir,File.separator,"dummy"));
                }
                catch (IOException e) {
                    log.warn("Unable to create archive directory {}", archiveDir, e);
                    archive = false;
                }
            } else if ( archiveDirFile.exists() && ! archiveDirFile.isDirectory() ) {
                log.warn("Archive directory doesn't seem to be a directory?");
                archive = false;
            }

            if ( archive ) {
                // iterate through files in current directory
                SortedSet<File> files = new TreeSet<>((a, b) -> ComparisonChain.start()
                        .compare(b.lastModified(), a.lastModified())
                        .result());

                for (File entry : file.getParentFile().listFiles()) {
                    if (entry.isFile() ) {
                        String ext = StringUtils.substringAfterLast(entry.getName(), ".");
                        boolean doInclude = GameUIManager.DEFAULT_SAVE_EXTENSION.equals(ext);
                        // TODO: verify it matches out expected file name format
                        if ( doInclude ) {
                            files.add(entry);
                        }
                    }
                }
                if ( files.size() > count ) {
                    File[] fileList = files.toArray(new File[]{});
                    for ( int i = count; i < fileList.length; i++ ) {
                        File toMove = fileList[i];
                        File destFile = new File(archiveDir + File.separator + toMove.getName());
                        if ( ! toMove.renameTo(destFile) ) {
                            log.warn("Unable to archive {} to {}", toMove.getName(), destFile.getAbsolutePath());
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * tries to reload the current game
     * executes the additional action(s)
     */
    protected boolean reload(GameAction reloadAction) {
        log.debug("Reloading started");

        /* Use gameLoader to load the game data */
        GameLoader gameLoader = new GameLoader();
        String filepath = reloadAction.getFilepath();

        if (!gameLoader.reloadGameFromFile(getRoot(), new File(filepath))) {
            return false;
        }

        log.debug("Starting to compare loaded actions");

        /* gameLoader actions get compared to the executed actions of the current game */
        List<PossibleAction> savedActions = gameLoader.getActions();

        setReloading(true);

        // Check size
        if (savedActions.size() < executedActions.size()) {
            log.warn("found {} actions in new file but have executed {}", savedActions.size(), executedActions.size());
            log.debug("last executed action: {}", executedActions.get(executedActions.size() - 1));
            for ( int i = executedActions.size() - 1, j = 5; i >= 0 && j >= 0; i--, j-- ) {
                log.debug("executed {}: {}", i, executedActions.get(i));
            }
            log.debug("last loaded action: {}", savedActions.get(savedActions.size() - 1));
            for ( int i = savedActions.size() -  1, j = 5; i >= 0 && j >= 0; i--, j-- ) {
                log.debug("loaded {}: {}", i, savedActions.get(i));
            }

            DisplayBuffer.add(this, LocalText.getText("LOAD_FAILED_MESSAGE",
                    "loaded file has less actions than current game"));
            return false;
        }

        // Check action identity
        int index = 0;
        // save off the current # of executed actions as it will grow as we execute newly loaded
        int executedActionsCount = executedActions.size();
        PossibleAction executedAction;
        try {
            for (PossibleAction savedAction : savedActions) {
                if (index < executedActionsCount) {
                    executedAction = executedActions.get(index);
                    if (!savedAction.equalsAsAction(executedAction)) {
                        log.warn("loaded action {} is not the same as expected game action {}", savedAction, executedAction);
                        DisplayBuffer.add(this, LocalText.getText("LoadFailed",
                                "loaded action \"" + savedAction.toString()
                                        + "\"<br>   is not same as game action \"" + executedAction.toString()
                                        + "\""));
                        return false;
                    }
                } else {
                    if (index == executedActionsCount) {
                        log.info("Finished comparing old actions, starting to process new actions");
                    }
                    // Found a new action: execute it
                    if (!processOnReload(savedAction)) {
                        log.error("Reload interrupted");
                        DisplayBuffer.add(this, LocalText.getText("LoadFailed",
                                " loaded action \"" + savedAction.toString() + "\" is invalid"));
                        break;
                    }
                }
                index++;
            }
        } catch (Exception e) {
            log.error("Reload failed", e);
            DisplayBuffer.add(this, LocalText.getText("LoadFailed", e.getMessage()));
            return false;
        }

        setReloading(false);
        finishLoading();

        // use new comments (without checks)
        // FIXME (Rails2.0): CommentItems have to be replaced
        // ReportBuffer.setCommentItems(gameLoader.getComments());

        log.info("Reloading finished");
        return true;
    }

    protected boolean export(GameAction exportAction) {
        String filename = exportAction.getFilepath();
        boolean result = false;

        try {
            PrintWriter pw = new PrintWriter(filename);

            // write map information
            for (MapHex hex : getRoot().getMapManager().getHexes()) {
                pw.println(hex.getId() + "," + hex.getCurrentTile().toText() + ","
                        + hex.getCurrentTileRotation() + ","
                        + hex.getOrientationName(hex.getCurrentTileRotation())
                );
            }
            pw.close();
            result = true;

        } catch (IOException e) {
            log.error("Save failed", e);
            DisplayBuffer.add(this, LocalText.getText("SaveFailed", e.getMessage()));
        }

        return result;
    }

    public void finishShareSellingRound() {
        finishShareSellingRound(true);
    }

    public void finishShareSellingRound(boolean resume) {
        setRound(interruptedRound);
        guiHints.setCurrentRoundType(interruptedRound.getClass());
        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setActivePanel(GuiDef.Panel.MAP);
        if (resume) getCurrentRound().resume();
    }

    public void finishTreasuryShareRound() {
        setRound(interruptedRound);
        guiHints.setCurrentRoundType(interruptedRound.getClass());
        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setActivePanel(GuiDef.Panel.MAP);
        ((OperatingRound) getCurrentRound()).nextStep();
    }

    /**
     * NOTE: this is provisional, incomplete and not used yet.
     * Calculate the total value of sellable shares in an emergency case,
     * taking the pool limit into account.<br>
     * Note: the complex 1841 rules cannot easily be included here,
     * these probably will need a subclass.
     * @param company The company possibly holding shares.
     * @param cashToRaise A required cash value (an emergency train price).
     *                    If zero, just the total share value is returned.
     * @return The potential yield of selling treasury shares.
     */
    protected int getSellableSharesValue (PublicCompany company, int cashToRaise,
                                          boolean dumpAllowed) {

        if (company == null || !company.hasStarted() || company.isClosed()) return 0;

        int value = 0;
        PortfolioModel pool = getRoot().getBank().getPool().getPortfolioModel();

        // Note every company's pool space in number of shares.
        Map <PublicCompany, Integer> poolSpace = new HashMap<>();
        for (PublicCompany comp : getAllPublicCompanies()) {
            poolSpace.put(comp,
                    getParmAsInt(GameDef.Parm.POOL_SHARE_LIMIT) / comp.getShareUnit()
                    - pool.getShares(comp));
        }

        // Treasury shares
        if (company.canHoldOwnShares()) {
            // Other companies (as in 1841) not yet considered.
            PortfolioModel portfolio = company.getPortfolioModel();
            int numberAvailable = portfolio.getShares(company);
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
                    // Count all shares
                    numberToSell = numberAvailable;
                }
                // Don't exceed the 50% pool limit
                int space = poolSpace.get(company);
                numberToSell = Math.min(numberToSell, space);
                value += numberToSell * sharePrice;
                // Subtract from pool space
                poolSpace.put (company, space - numberToSell);
            }
        }

        Player player = company.getPresident();

        value += player.getCash();
        /*
        if (player != null) {
            Map<PublicCompany, PublicCertificate> certsMap =
            for (PublicCompany comp : certsMap.keySet()) {
                for (PublicCertificate cert : certsMap.get (comp))
            }
        }*/

        return value;
    }


    public void registerPlayerBankruptcy(Player player) {
        endedByBankruptcy.set(true);
        String message =
                LocalText.getText("PlayerIsBankrupt",
                        player.getId());
        ReportBuffer.add(this, message);
        DisplayBuffer.add(this, message);
        if (gameEndsWithBankruptcy) {
            finishGame();
        } else {
            processPlayerBankruptcy();
        }
    }

    public void registerCompanyBankruptcy (PublicCompany company) {
        OperatingRound or = (OperatingRound) interruptedRound;
        String message = LocalText.getText("CompanyIsBankrupt",
                company.getId());
        ReportBuffer.add(this, message);
        DisplayBuffer.add(this, message);
        if (currentRound.value() instanceof ShareSellingRound) {
            finishShareSellingRound(false);
        }
        or.finishTurn();
    }

    /**
     * Process the effects of a player going bankrupt, without ending the game.
     * This code applies to (most of) the David Hecht games.
     * So far 1826, 18EU, 18VA, 18Scan have been identified to use this code.
     */
    protected void processPlayerBankruptcy() {

        // Assume default case as in 18EU: all assets to Bank/Pool
        Player bankrupter = getCurrentPlayer();
        Currency.toBankAll(bankrupter); // All money has already gone to company
        PortfolioModel bpf = bankrupter.getPortfolioModel();
        List<PublicCompany> presidencies = new ArrayList<>();
        for (PublicCertificate cert : bpf.getCertificates()) {
            if (cert.isPresidentShare()) presidencies.add(cert.getCompany());
        }
        for (PublicCompany company : presidencies) {
            // Check if the presidency is dumped onto someone
            Player newPresident = null;
            int maxShare = 0;
            PlayerManager pm = getRoot().getPlayerManager();
            for (Player player : pm.getNextPlayers(false)) {
                int share = player.getPortfolioModel().getShare(company);
                if (share >= company.getPresidentsShare().getShare()
                        && (share > maxShare)) {
                    maxShare = share;
                    newPresident = player;
                }
            }
            if (newPresident != null) {
                bankrupter.getPortfolioModel().swapPresidentCertificate(company,
                        newPresident.getPortfolioModel());
                ReportBuffer.add(this, LocalText.getText("IS_NOW_PRES_OF",
                        newPresident.getId(),
                        company.getId()));
            } else {
                // This process is game-dependent.
                processCompanyAfterPlayerBankruptcy(bankrupter, company);
            }
        }

        // Dump all shares to pool
        Portfolio.moveAll(PublicCertificate.class, bankrupter,
                getRoot().getBank().getPool());
        bankrupter.setBankrupt();

        // Finish the share selling round
        if (getCurrentRound() instanceof ShareSellingRound) {
            finishShareSellingRound();
        }
    }

    /** Stub, to be implemented by per-game subclasses.
     *  Only to be called by processPlayerBankruptcy().
     * (Perhaps a default version can be put here if sensible)
     */
    protected void processCompanyAfterPlayerBankruptcy(Player player, PublicCompany company) {
        return;
    }

    public void registerBrokenBank() {
        gameOverPending.set(true);
        ReportBuffer.add(this, LocalText.getText("BankIsBrokenReportText"));
        String msgContinue = "";
        if (gameEndWhen == GameEnd.AFTER_SET_OF_ORS)
            msgContinue = LocalText.getText("gameOverPlaySetOfORs");
        else if (gameEndWhen == GameEnd.AFTER_THIS_OR)
            msgContinue = LocalText.getText("gameOverPlayOnlyOR");
        String msg = LocalText.getText("BankIsBrokenDisplayText", msgContinue);
        DisplayBuffer.add(this, msg);
        addToNextPlayerMessages(msg, true);
    }

    public void registerMaxedSharePrice(PublicCompany company, StockSpace space) {
        gameOverPending.set(true);
        ReportBuffer.add(this, LocalText.getText("MaxedSharePriceReportText",
                company.getId(),
                Bank.format(this, space.getPrice())));
        String msgContinue = "";
        if (gameEndWhen == GameEnd.AFTER_SET_OF_ORS)
            msgContinue = LocalText.getText("gameOverPlaySetOfORs");
        else if (gameEndWhen == GameEnd.AFTER_THIS_OR)
            msgContinue = LocalText.getText("gameOverPlayOnlyOR");
        String msg = LocalText.getText("MaxedSharePriceDisplayText",
                company.getId(),
                Bank.format(this, space.getPrice()),
                msgContinue);
        DisplayBuffer.add(this, msg);
        addToNextPlayerMessages(msg, true);
    }

    protected void finishGame() {
        gameOver.set(true);

        String message = LocalText.getText("GameOver");
        ReportBuffer.add(this, message);

        // FIXME: Rails 2.0 this is not allowed as this causes troubles with Undo
        // DisplayBuffer.add(this, message);

        ReportBuffer.add(this, "");

        List<String> gameReport = getGameReport();
        for (String s : gameReport)
            ReportBuffer.add(this, s);

        // activate gameReport for UI
        setGameOverReportedUI(false);

        // FIXME: This will not work, as it will create duplicated IDs
        createRound(EndOfGameRound.class, "EndOfGameRound");
    }

    public boolean isGameOver() {
        return gameOver.value();
    }

    public BooleanState getGameOverPendingModel() {
        return gameOverPending;
    }

    public void setGameOverReportedUI(boolean b) {
        gameOverReportedUI = b;
    }

    public boolean getGameOverReportedUI() {
        return (gameOverReportedUI);
    }

    public List<String> getGameReport() {

        List<String> b = new ArrayList<>();

        /* Sort players by total worth */
        List<Player> rankedPlayers = new ArrayList<>(getRoot().getPlayerManager().getPlayers());
        Collections.sort(rankedPlayers);

        /* Report winner */
        Player winner = rankedPlayers.get(0);
        b.add(LocalText.getText("EoGWinner") + winner.getId() + "!");
        b.add(LocalText.getText("EoGFinalRanking") + " :");

        /* Report final ranking */
        int i = 0;
        for (Player p : rankedPlayers) {
            b.add((++i) + ". " + Bank.format(this, p.getWorth()) + " "
                    + p.getId());
        }

        return b;
    }

    public RoundFacade getCurrentRound() {
        return currentRound.value();
    }

    public GenericState<RoundFacade> getCurrentRoundModel() {
        return currentRound;
    }

    public List<PublicCompany> getAllPublicCompanies() {
        return getRoot().getCompanyManager().getAllPublicCompanies();
    }

    public List<PrivateCompany> getAllPrivateCompanies() {
        return getRoot().getCompanyManager().getAllPrivateCompanies();
    }

    public StartPacket getStartPacket() {
        return startPacket.value();
    }

    public Phase getCurrentPhase() {
        return getRoot().getPhaseManager().getCurrentPhase();
    }

    public boolean canAnyCompanyHoldShares() {
        return (Boolean) getGuiParameter(GuiDef.Parm.CAN_ANY_COMPANY_HOLD_OWN_SHARES);
    }

    public String getClassName(GuiDef.ClassName key) {
        switch (key) {

            case GAME_UI_MANAGER:
                return gameUIManagerClassName;

            case OR_UI_MANAGER:
                return orUIManagerClassName;

            case STATUS_WINDOW:
                return statusWindowClassName;

            case START_ROUND_WINDOW:
                return startRoundWindowClassName;

            case GAME_STATUS:
                return gameStatusClassName;

            default:
                return "";
        }
    }

    public Object getGuiParameter(GuiDef.Parm key) {
        return guiParameters.getOrDefault(key, false);
    }

    public void setGuiParameter(GuiDef.Parm key, boolean value) {
        guiParameters.put(key, value);
    }

    public void setGameParameter(GameDef.Parm key, Object value) {
        gameParameters.put(key, value);
    }

    public Object getGameParameter(GameDef.Parm key) {
        return gameParameters.getOrDefault(key, false);
    }

    public int getParmAsInt (GameDef.Parm key) {
        Object parm = getGameParameter (key);
        if (parm instanceof Integer) {
            return (Integer) parm;
        } else {
            return -1;
        }
    }

    public boolean getParmAsBoolean (GameDef.Parm key) {
        Object parm = getGameParameter (key);
        if (parm instanceof Boolean) {
            return (Boolean) parm;
        } else {
            return false;
        }
    }

    public RoundFacade getInterruptedRound() {
        return interruptedRound;
    }

    /**
     * Move the special property to the party that will later benefit from it:
     * the company or the player (in the latter case it is stored in the GameManager
     * as a "common" property: buyable by all companies.
     * @param owner The current buyer (and future seller) of the property.
     * @param sps The (set of) property(ies) to be allocated.
     */
    public void allocateSpecialProperties(MoneyOwner owner, Set<SpecialProperty> sps) {
        // Move any special abilities to the portfolio, if configured so.

        if (sps != null) {
            // Need intermediate List to avoid ConcurrentModificationException
            List<SpecialProperty> spsToMoveToPC = new ArrayList<>(2);
            List<SpecialProperty> spsToMoveToGM = new ArrayList<>(2);
            List<SpecialProperty> spsMoveToPlayer = new ArrayList<>(2);
            for (SpecialProperty sp : sps) {
                if ((owner instanceof PublicCompany && sp.isUsableIfOwnedByCompany())
                        || (owner instanceof Player && sp.isUsableIfOwnedByPlayer())
                        && "toGameManager".equalsIgnoreCase(sp.getTransferText())) {
                    // This must be SellBonusToken - remember the owner!
                    if (sp instanceof SellBonusToken) {
                        // TODO: Check if this works correctly
                        ((SellBonusToken) sp).setSeller(owner);
                        // Also note 1 has been used
                        // EV: ???? Why this? Breaks 18Scan
                        //((SellBonusToken) sp).setExercised();
                        spsToMoveToGM.add(sp);
                    }
                    // Used in 1856 (reporting only) and SOH (no reporting)
                    // -- Strange, but true
                    if (sp instanceof LocatedBonus) {
                        PublicCompany company = (PublicCompany) owner;
                        LocatedBonus b = (LocatedBonus) sp;
                        if (!"SOH".equals(getRoot().getGameName())) { //Prevent reporting twice
                            ReportBuffer.add(this, LocalText.getText("AcquiresBonus",
                                    company.getId(),
                                    b.getName(),
                                    Bank.format(company, b.getValue()),
                                    b.getLocationNameString()));
                        }
                        if (!"1856".equals(getRoot().getGameName())) { // Necessary, but why not here?
                            spsToMoveToPC.add(sp); // Required for SOH
                        }
                    }

                } else if (owner instanceof PublicCompany && sp.isUsableIfOwnedByCompany()
                        && "toCompany".equalsIgnoreCase(sp.getTransferText())) {
                    spsToMoveToPC.add(sp);

                } else if (owner instanceof Player && sp.isUsableIfOwnedByPlayer()
                        && "toPlayer".equalsIgnoreCase(sp.getTransferText())) {
                    spsMoveToPlayer.add (sp);
                }
            }
            for (SpecialProperty sp : spsToMoveToPC) {
                sp.moveTo(owner);
            }
            for (SpecialProperty sp : spsToMoveToGM) {
                this.addSpecialProperty(sp);
                log.debug("SP {} is now a common property", sp.getInfo());
            }
            for (SpecialProperty sp : spsMoveToPlayer) {
                sp.moveTo(owner);
                log.debug("SP {} is now a player property", sp.getInfo());
            }
        }
    }

    // TODO: Was the int position argument required?
    public boolean addSpecialProperty(SpecialProperty property) {
        if (commonSpecialProperties == null) {
            commonSpecialProperties = PortfolioSet.create(this,
                    "CommonSpecialProperties", SpecialProperty.class);
        }
        return commonSpecialProperties.add(property);
    }

    // TODO: Write new SpecialPropertiesModel

    /* (non-Javadoc)
     * @see rails.game.GameManager#getCommonSpecialProperties()
     */
/*    public boolean removeSpecialProperty(SpecialProperty property) {

        if (commonSpecialProperties != null) {
            return commonSpecialProperties.removeObject(property);
        }

        return false;
    } */

    public List<SpecialProperty> getCommonSpecialProperties() {
        return getSpecialProperties(null, false);
    }

    public Portfolio<SpecialProperty> getCommonSpecialPropertiesPortfolio() {
        return commonSpecialProperties;
    }

    @SuppressWarnings("unchecked")
    public <T extends SpecialProperty> List<T> getSpecialProperties(
            Class<T> clazz, boolean includeExercised) {

        List<T> result = new ArrayList<>();

        if (commonSpecialProperties != null) {
            for (SpecialProperty sp : commonSpecialProperties) {
                if ((clazz == null || clazz.isAssignableFrom(sp.getClass()))
                        && sp.isExecutionable()
                        && (!sp.isExercised() || includeExercised)) {
                    result.add((T) sp);
                }
            }
        }

        return result;
    }

    public GuiHints getUIHints() {
        return guiHints;
    }

    public CorrectionManager getCorrectionManager(CorrectionType ct) {
        CorrectionManager cm = correctionManagers.get(ct);
        if (cm == null) {
            cm = ct.newCorrectionManager(this);
            correctionManagers.put(ct, cm);
            log.debug("Added CorrectionManager for {}", ct);
        }
        return cm;
    }

    public List<PublicCompany> getCompaniesInRunningOrder() {

        Map<Integer, PublicCompany> operatingCompanies =
                new TreeMap<>();
        StockSpace space;
        int key;
        int minorNo = 0;
        for (PublicCompany company : getRoot().getCompanyManager().getAllPublicCompanies()) {

            // Key must put companies in reverse operating order, because sort
            // is ascending.
            if (company.hasStockPrice() && company.hasStarted()) {
                space = company.getCurrentSpace();
                key =
                        1000000 * (999 - space.getPrice()) + 10000
                                * (99 - space.getColumn()) + 100
                                * space.getRow()
                                + space.getStackPosition(company);
            } else {
                key = ++minorNo;
            }
            operatingCompanies.put(key, company);
        }

        return new ArrayList<>(operatingCompanies.values());
    }

    public boolean isReloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

    public void setSkipDone(GameDef.OrStep step) {
        skipNextDone = true;
        skippedStep = step;
    }

    public void resetStorage() {
        objectStorage = new HashMap<>();
        storageIds = new HashMap<>();
    }

    public int getStorageId(String typeName) {
        Integer id = storageIds.get(typeName);
        if (id == null) id = 0;
        return id;
    }

    public int storeObject(String typeName, Object object) {
        Integer id = storageIds.get(typeName);
        if (id == null) id = 0;
        objectStorage.put(typeName + id, object);
        storageIds.put(typeName, id + 1); // store next id
        return id;
    }

    public Object retrieveObject(String typeName, int id) {
        return objectStorage.get(typeName + id);
    }

    // TODO (Rails2.0): rewrite this, use PhaseAction interface stored at PhaseManager
    public void processPhaseAction(String name, String value) {
        getCurrentRound().processPhaseAction(name, value);
    }

    // FIXME (Rails2.0): does nothing now, replace this with a rewrite
    public void addToNextPlayerMessages(String s, boolean undoable) {

    }

    // shortcut to PlayerManager
    public int getPlayerCertificateLimit(Player player) {
        return getRoot().getPlayerManager().getPlayerCertificateLimit(player);
    }

    // shortcut to PlayerManager
    protected Player getCurrentPlayer() {
        return getRoot().getPlayerManager().getCurrentPlayer();
    }

    public void setNationalToFound(String national) {

        for (PublicCompany company : this.getAllPublicCompanies()) {
            if ( "national".equals(company.getId())) {
                this.nationalToFound = company;
            }
        }
    }

    public PublicCompany getNationalToFound() {
        // TODO Auto-generated method stub
        return nationalToFound;
    }

    public void setNationalFormationStartingPlayer(PublicCompany nationalToFound2, Player currentPlayer) {
        this.NationalFormStartingPlayer.put(nationalToFound2, currentPlayer);

    }

    public Player getNationalFormationStartingPlayer(PublicCompany comp) {
        return this.NationalFormStartingPlayer.get(comp);
    }

    private Random randomGenerator;
    public Random getRandomGenerator () {
        if (randomGenerator == null) {
            long seed = Long.parseLong(getRoot().getGameOptions().get(GameOption.RANDOM_SEED));
            log.info ("Random seed = {}", seed);
            randomGenerator = new Random (seed);
        }
        return randomGenerator;
    }
}


