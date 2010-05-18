/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/GameManager.java,v 1.102 2010/05/18 04:12:23 stefanfrey Exp $ */
package rails.game;

import java.io.*;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.*;


import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import rails.algorithms.RevenueManager;
import rails.common.GuiDef;
import rails.common.GuiHints;
import rails.game.action.*;
import rails.game.correct.*;
import rails.game.move.*;
import rails.game.special.SpecialPropertyI;
import rails.game.special.SpecialTokenLay;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;
import rails.game.state.State;
import rails.util.*;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded &agrave; la 1830.
 */
public class GameManager implements ConfigurableComponentI, GameManagerI {
    /** Version ID of the Save file header, as written in save() */
    private static final long saveFileHeaderVersionID = 3L;
    /**
     * Overall save file version ID, taking into account the version ID of the
     * action package.
     */
    public static final long saveFileVersionID =
            saveFileHeaderVersionID * PossibleAction.serialVersionUID;

    protected Class<? extends StockRound> stockRoundClass = StockRound.class;
    protected Class<? extends OperatingRound> operatingRoundClass =
            OperatingRound.class;

    // Variable UI Class names
    protected String gameUIManagerClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.GAME_UI_MANAGER);
    protected String orUIManagerClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.OR_UI_MANAGER);
    protected String gameStatusClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.GAME_STATUS);
    protected String statusWindowClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.STATUS_WINDOW);
    protected String orWindowClassName = GuiDef.getDefaultClassName(GuiDef.ClassName.OR_WINDOW);

    protected PlayerManager playerManager;
    protected CompanyManagerI companyManager;
    protected PhaseManager phaseManager;
    protected TrainManager trainManager;
    protected StockMarketI stockMarket;
    protected MapManager mapManager;
    protected TileManager tileManager;
    protected RevenueManager revenueManager;
    protected Bank bank;
    
    // map of correctionManagers
    protected Map<CorrectionType, CorrectionManagerI> correctionManagers =
        new HashMap<CorrectionType, CorrectionManagerI>();

    protected String gameName;
    protected Map<String, String> gameOptions;

    protected List<Player> players;
    protected List<String> playerNames;
    protected int numberOfPlayers;
    protected State currentPlayer = new State("CurrentPlayer", Player.class);
    protected State priorityPlayer = new State("PriorityPlayer", Player.class);

    /** Map relating portfolio names and objects, to enable deserialization */
    protected Map<String, Portfolio> portfolioMap =
        new HashMap<String, Portfolio> ();

    protected IntegerState playerCertificateLimit
            = new IntegerState ("PlayerCertificateLimit", 0);
    protected int currentNumberOfOperatingRounds = 1;
    protected boolean skipFirstStockRound = false;
    protected boolean showCompositeORNumber = true;

    protected boolean gameEndsWithBankruptcy = false;
    protected int gameEndsWhenBankHasLessOrEqual = 0;
    protected boolean gameEndsAfterSetOfORs = true;

    protected EnumMap<GameDef.Parm, Object> gameParameters
            = new EnumMap<GameDef.Parm, Object>(GameDef.Parm.class);

//    protected EnumSet<CorrectionType> activeCorrections
//        = EnumSet.noneOf(CorrectionType.class);

    /**
     * Current round should not be set here but from within the Round classes.
     * This is because in some cases the round has already changed to another
     * one when the constructor terminates. Example: if the privates have not
     * been sold, it finishes by starting an Operating Round, which handles the
     * privates payout and then immediately starts a new Start Round.
     */
    protected State currentRound = new State("CurrentRound", Round.class);
    protected RoundI interruptedRound = null;

    protected IntegerState srNumber = new IntegerState ("SRNumber");

    protected IntegerState absoluteORNumber =
            new IntegerState("AbsoluteORNUmber");
    protected IntegerState relativeORNumber =
            new IntegerState("RelativeORNumber");
    protected int numOfORs;

    protected BooleanState gameOver = new BooleanState("GameOver" ,false);
    protected Boolean gameOverReportedUI = false;
    protected BooleanState endedByBankruptcy = new BooleanState("EndedByBankruptcy", false);

    /** UI display hints */
    protected GuiHints guiHints;

    /** Flags to be passed to the UI, aiding the layout definition */
    protected EnumMap<GuiDef.Parm, Boolean> guiParameters =
        new EnumMap<GuiDef.Parm, Boolean>(GuiDef.Parm.class);

    /**
     * Map of GameManager instances.
     * Currently there can be only one instance, but in a possible
     * future multi-game server there may be several instances
     * running in parallel.
     *
     * <p>The reason for creating this map is the need to access
     * GameManager instances (or other common instances via the GM)
     * from many different classes, some of which
     * (like those in the move package) are many method calls away from
     * the actual GM.
     * <p>To prevent the need to pass GameManager instances or the keys to
     * this map around throughout the code, NDC is (mis-)used as the
     * mechanism to pass around a string key to each GM instance.
     * This is possible,because the server processes all player actions
     * in one thread. The key will be set in process(), which is where server
     * processing currently starts (in the future it will probably be moved
     * to the communication interface that will be added by then).
     * The key can be retrieved (via NDC.peek()) anywhere.
     * <p>
     * For now, the key is a fixed string, but that may change in the future.
     */
    protected static Map<String, GameManagerI> gameManagerMap
        = new HashMap<String, GameManagerI>();

    /**
     * The temporary fixed key to the currently single GameManager instance
     * in the GameManager map.
     * It will only be used inside the GM objects.
     * All other objects will access it via NDC.
     */
    protected static final String GM_KEY = "01";
    public static final String GM_NAME = "GameManager";

    /**
     * The MoveSet stack is maintained to enable Undo and Redo throughout the game.
     */
    protected MoveStack moveStack = new MoveStack();

    /**
     * The DisplayBuffer instance collects messages to be displayed in the UI.
     */
    protected DisplayBuffer displayBuffer;
    /**
     * nextPlayerMessages collects all messages to be displayed to the next player
     */
    protected List<String> nextPlayerMessages = new ArrayList<String>();

    /**
     * The ReportBuffer collectes messages to be shown in the Game Report.
     */
    protected ReportBuffer reportBuffer;

    protected String gmName;
    protected String gmKey;

    protected StartPacket startPacket;

    protected PossibleActions possibleActions = PossibleActions.getInstance();

    protected List<PossibleAction> executedActions = new ArrayList<PossibleAction>();

    /** Special properties that can be used by other players or companies
     * than just the owner (such as buyable bonus tokens as in 1856).
     */
    protected List<SpecialPropertyI> commonSpecialProperties = null;

    /** A List of available game options */
    protected List<GameOption> availableGameOptions =
            new ArrayList<GameOption>();

    protected static Logger log =
            Logger.getLogger(GameManager.class.getPackage().getName());

    public GameManager() {
        gmName = GM_NAME;
        gmKey = GM_KEY;
        NDC.clear();
        NDC.push (GM_KEY);
        gameManagerMap.put(GM_KEY, this);
        displayBuffer = new DisplayBuffer();
        reportBuffer = new ReportBuffer();
        guiHints = new GuiHints();
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {
        /* Get the rails.game name as configured */
        Tag gameTag = tag.getChild("Game");
        if (gameTag == null)
            throw new ConfigurationException(
                    "No Game tag specified in GameManager tag");
        gameName = gameTag.getAttributeAsString("name");
        if (gameName == null)
            throw new ConfigurationException("No name specified in Game tag");

        gameOptions = tag.getGameOptions();

        initGameParameters();

        /* Check the game options provided with the game.
         * These duplicate the game options in Game.xml, but are still
         * required here to set default values when loading a game from
         * a save file, as in that case the options in GamesList.xml are
         * not included.
         * */
        GameOption option;
        String optionName, optionType, optionValues, optionDefault;
        String optionNameParameters;
        List<Tag> optionTags = tag.getChildren("GameOption");
        if (optionTags != null) {
            for (Tag optionTag : optionTags) {
                optionName = optionTag.getAttributeAsString("name");
                if (optionName == null)
                    throw new ConfigurationException("GameOption without name");
                if (gameOptions.containsKey(optionName)) continue;

                // Include missing option
                option = new GameOption(optionName);
                availableGameOptions.add(option);
                optionNameParameters = optionTag.getAttributeAsString("parm");
                if (optionNameParameters != null) {
                    option.setParameters(optionNameParameters.split(","));
                }
                optionType = optionTag.getAttributeAsString("type");
                if (optionType != null) option.setType(optionType);
                optionValues = optionTag.getAttributeAsString("values");
                if (optionValues != null)
                    option.setAllowedValues(optionValues.split(","));
                optionDefault = optionTag.getAttributeAsString("default", "");
                if (optionDefault != null)
                    option.setDefaultValue(optionDefault);

                gameOptions.put(optionName, optionDefault);
            }
        }


        Tag gameParmTag = tag.getChild("GameParameters");
        if (gameParmTag != null) {


            // StockRound class and other properties
               Tag srTag = gameParmTag.getChild("StockRound");
            if (srTag != null) {
                String srClassName =
                        srTag.getAttributeAsString("class", "rails.game.StockRound");
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
                    if (stockRoundSequenceRuleString.equalsIgnoreCase("SellBuySell")) {
                        setGameParameter(GameDef.Parm.STOCK_ROUND_SEQUENCE,
                                StockRound.SELL_BUY_SELL);
                    } else if (stockRoundSequenceRuleString.equalsIgnoreCase("SellBuy")) {
                        setGameParameter(GameDef.Parm.STOCK_ROUND_SEQUENCE,
                                StockRound.SELL_BUY);
                    } else if (stockRoundSequenceRuleString.equalsIgnoreCase("SellBuyOrBuySell")) {
                        setGameParameter(GameDef.Parm.STOCK_ROUND_SEQUENCE,
                                StockRound.SELL_BUY_OR_BUY_SELL);
                    }
                }

                skipFirstStockRound =
                        srTag.getAttributeAsBoolean("skipFirst",
                                skipFirstStockRound);

                for (String ruleTagName : srTag.getChildren().keySet()) {
                    if (ruleTagName.equals("NoSaleInFirstSR")) {
                        setGameParameter(GameDef.Parm.NO_SALE_IN_FIRST_SR, true);
                    } else if (ruleTagName.equals("NoSaleIfNotOperated")) {
                        setGameParameter(GameDef.Parm.NO_SALE_IF_NOT_OPERATED, true);
                    } else if (ruleTagName.equals("NoSaleOfJustBoughtShare")) {
                        setGameParameter(GameDef.Parm.NO_SALE_OF_JUST_BOUGHT_CERT, true);
                    }

                }
        }

            // OperatingRound class
            Tag orTag = gameParmTag.getChild("OperatingRound");
            if (orTag != null) {
                String orClassName =
                        orTag.getAttributeAsString("class",
                                "rails.game.OperatingRound");
                try {
                    operatingRoundClass =
                            Class.forName(orClassName).asSubclass(
                                    OperatingRound.class);
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("Cannot find class "
                                                     + orClassName, e);
                }
            }

            /* Max. % of shares of one company that a player may hold */
            Tag shareLimitTag = gameParmTag.getChild("PlayerShareLimit");
            if (shareLimitTag != null) {
                setGameParameter (GameDef.Parm.PLAYER_SHARE_LIMIT,
                        shareLimitTag.getAttributeAsInteger("percentage",
                                GameDef.Parm.PLAYER_SHARE_LIMIT.defaultValueAsInt()));
            }

            /* Max. % of shares of one company that the bank pool may hold */
            Tag poolLimitTag = gameParmTag.getChild("BankPoolShareLimit");
            if (poolLimitTag != null) {
                setGameParameter (GameDef.Parm.POOL_SHARE_LIMIT,
                        shareLimitTag.getAttributeAsInteger("percentage",
                                GameDef.Parm.POOL_SHARE_LIMIT.defaultValueAsInt()));
            }

            /* Max. % of own shares that a company treasury may hold */
            Tag treasuryLimitTag = gameParmTag.getChild("TreasuryShareLimit");
            if (treasuryLimitTag != null) {
                setGameParameter (GameDef.Parm.TREASURY_SHARE_LIMIT,
                        shareLimitTag.getAttributeAsInteger("percentage",
                                GameDef.Parm.TREASURY_SHARE_LIMIT.defaultValueAsInt()));
            }
        }


        /* End of rails.game criteria */
        Tag endOfGameTag = tag.getChild("EndOfGame");
        if (endOfGameTag != null) {
            if (endOfGameTag.getChild("Bankruptcy") != null) {
                gameEndsWithBankruptcy = true;
            }
            Tag bankBreaksTag = endOfGameTag.getChild("BankBreaks");
            if (bankBreaksTag != null) {
                gameEndsWhenBankHasLessOrEqual =
                        bankBreaksTag.getAttributeAsInteger("limit",
                                gameEndsWhenBankHasLessOrEqual);
                String attr = bankBreaksTag.getAttributeAsString("finish");
                if (attr.equalsIgnoreCase("SetOfORs")) {
                    gameEndsAfterSetOfORs = true;
                } else if (attr.equalsIgnoreCase("CurrentOR")) {
                    gameEndsAfterSetOfORs = false;
                }
            }
        }

        Tag guiClassesTag = tag.getChild("GuiClasses");
        if (guiClassesTag != null) {

            // GameUIManager class
            Tag gameUIMgrTag = guiClassesTag.getChild("GameUIManager");
            if (gameUIMgrTag != null) {
                gameUIManagerClassName =
                        gameUIMgrTag.getAttributeAsString("class", gameUIManagerClassName);
                // Check instantiatability (not sure if this belongs here)
                canClassBeInstantiated (gameUIManagerClassName);
            }

            // ORUIManager class
            Tag orMgrTag = guiClassesTag.getChild("ORUIManager");
            if (orMgrTag != null) {
                orUIManagerClassName =
                        orMgrTag.getAttributeAsString("class", orUIManagerClassName);
                // Check instantiatability (not sure if this belongs here)
                canClassBeInstantiated (orUIManagerClassName);
            }

            // GameStatus class
            Tag gameStatusTag = guiClassesTag.getChild("GameStatus");
            if (gameStatusTag != null) {
                gameStatusClassName =
                        gameStatusTag.getAttributeAsString("class", gameStatusClassName);
                // Check instantiatability (not sure if this belongs here)
                canClassBeInstantiated (gameStatusClassName);
            }

            // StatusWindow class
            Tag statusWindowTag = guiClassesTag.getChild("StatusWindow");
            if (statusWindowTag != null) {
                statusWindowClassName =
                        statusWindowTag.getAttributeAsString("class",
                                statusWindowClassName);
                // Check instantiatability (not sure if this belongs here)
                canClassBeInstantiated (statusWindowClassName);
            }

            // ORWindow class
            Tag orWindowTag = guiClassesTag.getChild("ORWindow");
            if (orWindowTag != null) {
                orWindowClassName =
                    orWindowTag.getAttributeAsString("class",
                            orWindowClassName);
                // Check instantiatability (not sure if this belongs here)
                canClassBeInstantiated (orWindowClassName);
            }
        }
    }

    public void finishConfiguration (GameManagerI gameManager) {}

    /** Check if a classname can be instantiated.
     * Throws a ConfiguratioNException if not.
     * @param className
     * @throws ConfigurationException
     */
    protected void canClassBeInstantiated (String className)
    throws ConfigurationException {

        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Cannot find class "
                                             + className, e);
        }
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#startGame(rails.game.PlayerManager, rails.game.CompanyManagerI, rails.game.PhaseManager)
     */
    public void init(String gameName,
            PlayerManager playerManager,
            CompanyManagerI companyManager,
            PhaseManager phaseManager,
            TrainManager trainManager,
            StockMarketI stockMarket,
            MapManager mapManager,
            TileManager tileManager,
            RevenueManager revenueManager,
            Bank bank) {
        this.gameName = gameName;
        this.playerManager = playerManager;
        this.companyManager = companyManager;
        this.phaseManager = phaseManager;
        this.trainManager = trainManager;
        this.stockMarket = stockMarket;
        this.mapManager = mapManager;
        this.tileManager = tileManager;
        this.revenueManager = revenueManager;
        this.bank = bank;

        players = playerManager.getPlayers();
        playerNames = playerManager.getPlayerNames();
        numberOfPlayers = players.size();
        priorityPlayer.setState(players.get(0));
        setPlayerCertificateLimit (playerManager.getInitialPlayerCertificateLimit());

        showCompositeORNumber =  !"simple".equalsIgnoreCase(Config.get("or.number_format"));
    }

    public void startGame(Map<String,String> gameOptions) {

        this.gameOptions = gameOptions;
        setGuiParameters();

        if (startPacket == null)
            startPacket = companyManager.getStartPacket(StartPacket.DEFAULT_NAME);
        if (startPacket != null && !startPacket.areAllSold()) {
            startPacket.init(this);

            // If we have a non-exhausted start packet
            startStartRound();
        } else {
            startStockRound();
        }

        // Initialisation is complete. Undoability starts here.
        moveStack.enable();
    }

    private void setGuiParameters () {

        for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {
            if (company.hasParPrice()) guiParameters.put(GuiDef.Parm.HAS_ANY_PAR_PRICE, true);
            if (company.canBuyPrivates()) guiParameters.put(GuiDef.Parm.CAN_ANY_COMPANY_BUY_PRIVATES, true);
            if (company.canHoldOwnShares()) guiParameters.put(GuiDef.Parm.CAN_ANY_COMPANY_HOLD_OWN_SHARES, true);
            if (company.getMaxNumberOfLoans() != 0) guiParameters.put(GuiDef.Parm.HAS_ANY_COMPANY_LOANS, true);
        }

loop:   for (PrivateCompanyI company : companyManager.getAllPrivateCompanies()) {
            for (SpecialPropertyI sp : company.getSpecialProperties()) {
                if (sp instanceof SpecialTokenLay
                        && ((SpecialTokenLay)sp).getToken() instanceof BonusToken) {
                    guiParameters.put(GuiDef.Parm.DO_BONUS_TOKENS_EXIST, true);
                    break loop;
                }
            }

        }

        if (GameOption.convertValueToBoolean(getGameOption("NoMapMode")))
            guiParameters.put(GuiDef.Parm.NO_MAP_MODE, true);

    }

    private void initGameParameters() {

        for (GameDef.Parm parm : GameDef.Parm.values()) {
            gameParameters.put(parm, parm.defaultValue());
        }
    }

    /**
     * @return instance of GameManager
     */
    public static GameManagerI getInstance () {
        return gameManagerMap.get(NDC.peek());
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getCompanyManager()
     */
    public CompanyManagerI getCompanyManager() {
        return companyManager;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#setRound(rails.game.RoundI)
     */
    protected void setRound(RoundI round) {
        currentRound.set(round);
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#nextRound(rails.game.RoundI)
     */
    public void nextRound(RoundI round) {
        if (round instanceof StartRound) {
            if (startPacket != null && !startPacket.areAllSold()) {
                startOperatingRound(false);
            } else if (skipFirstStockRound) {
                PhaseI currentPhase =
                        phaseManager.getCurrentPhase();
                numOfORs = currentPhase.getNumberOfOperatingRounds();
                log.info("Phase=" + currentPhase.getName() + " ORs=" + numOfORs);

                // Create a new OperatingRound (never more than one Stock Round)
                // OperatingRound.resetRelativeORNumber();

                relativeORNumber.set(1);
                startOperatingRound(true);
            } else {
                startStockRound();
            }
        } else if (round instanceof StockRound) {
            PhaseI currentPhase = getCurrentPhase();
            if (currentPhase == null) log.error ("Current Phase is null??", new Exception (""));
            numOfORs = currentPhase.getNumberOfOperatingRounds();
            log.info("Phase=" + currentPhase.getName() + " ORs=" + numOfORs);

            // Create a new OperatingRound (never more than one Stock Round)
            // OperatingRound.resetRelativeORNumber();
            relativeORNumber.set(1);
            startOperatingRound(true);

        } else if (round instanceof OperatingRound) {
            if (bank.isBroken() && !gameEndsAfterSetOfORs) {

                finishGame();

            } else if (relativeORNumber.add(1) <= numOfORs) {
                // There will be another OR
                startOperatingRound(true);
            } else if (startPacket != null && !startPacket.areAllSold()) {
                startStartRound();
            } else {
                if (bank.isBroken() && gameEndsAfterSetOfORs) {
                    finishGame();
                } else {
                    startStockRound();
                }
            }
        }
    }

    protected void startStartRound() {
        String startRoundClassName = startPacket.getRoundClassName();
        Class<? extends StartRound> startRoundClass = null;
        try {
            startRoundClass = Class.forName (startRoundClassName).asSubclass(StartRound.class);
        } catch (Exception e) {
            log.fatal("Cannot find class "
                    + startRoundClassName, e);
          System.exit(1);
        }
        StartRound startRound = createRound (startRoundClass);
        startRound.start ();
    }

    protected void startStockRound() {
        StockRound sr = createRound (stockRoundClass);
        srNumber.add(1);
        sr.start();
    }

    protected void startOperatingRound(boolean operate) {
        log.debug("Operating round started with operate-flag=" + operate);

        OperatingRound or = createRound(operatingRoundClass);
        if (operate) absoluteORNumber.add(1);
        or.start();
    }

    protected <T extends RoundI> T createRound (Class<T> roundClass) {

        T round = null;
        try {
            Constructor<T> cons = roundClass.getConstructor(GameManagerI.class);
            round = cons.newInstance(this);
        } catch (Exception e) {
            log.fatal("Cannot instantiate class "
                      + roundClass.getName(), e);
            System.exit(1);
        }
        setRound (round);
        return round;
    }

    protected <T extends RoundI, U extends RoundI>
            T createRound (Class<T> roundClass, U parentRound) {

        if (parentRound == null) {
            return createRound (roundClass);
        }

        T round = null;
        try {
            Constructor<T> cons = roundClass.getConstructor(GameManagerI.class, RoundI.class);
            round = cons.newInstance(this, parentRound);
        } catch (Exception e) {
            log.fatal("Cannot instantiate class "
                      + roundClass.getName(), e);
            System.exit(1);
        }
        setRound (round);
        return round;
    }

    /** Stub, can be overridden in subclasses with actual actions */
    public void newPhaseChecks (RoundI round) {
        
    }
    
    public String getORId () {
        if (showCompositeORNumber) {
            return getCompositeORNumber();
        } else {
            return String.valueOf(absoluteORNumber.intValue());
        }
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getCompositeORNumber()
     */
    public String getCompositeORNumber() {
        return srNumber.intValue() + "." + relativeORNumber.intValue();
    }

    public int getRelativeORNumber() {
        return relativeORNumber.intValue();
    }

    public String getNumOfORs () {
        return new Integer(numOfORs).toString();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getSRNumber()
     */
    public int getSRNumber () {
        return srNumber.intValue();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#startShareSellingRound(rails.game.OperatingRound, rails.game.PublicCompanyI, int)
     */
    public void startShareSellingRound(Player player, int cashToRaise,
            PublicCompanyI unsellableCompany) {

        interruptedRound = getCurrentRound();
        createRound (ShareSellingRound.class, interruptedRound)
            .start(player, cashToRaise, unsellableCompany);
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#startTreasuryShareTradingRound(rails.game.OperatingRound, rails.game.PublicCompanyI)
     */
    public void startTreasuryShareTradingRound() {

        interruptedRound = getCurrentRound();
        createRound (TreasuryShareRound.class, interruptedRound).start();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#process(rails.game.action.PossibleAction)
     */
    public boolean process(PossibleAction action) {

        NDC.clear();
        NDC.push (GM_KEY);

        boolean result = true;

        DisplayBuffer.clear();
        guiHints.clearVisibilityHints();

        if (action instanceof NullAction && ((NullAction)action).getMode() == NullAction.START_GAME) {
            // Skip processing at game start after Load.
            // We're only here to create PossibleActions.
            result = true;

        } else if (action != null) {
            // Should never be null.

            action.setActed();
            result = false;

            // Check player
            String actionPlayerName = action.getPlayerName();
            String currentPlayerName = getCurrentPlayer().getName();
            if (!actionPlayerName.equals(currentPlayerName)) {
                DisplayBuffer.add(LocalText.getText("WrongPlayer",
                        actionPlayerName, currentPlayerName ));
                return false;
            }

            // Check if the action is allowed
            if (!possibleActions.validate(action)) {
                DisplayBuffer.add(LocalText.getText("ActionNotAllowed",
                        action.toString()));
                return false;
            }

            for (;;) {

                // Process undo/redo centrally
                if (action instanceof GameAction) {

                    GameAction gameAction = (GameAction) action;
                    switch (gameAction.getMode()) {
                    case GameAction.SAVE:
                        result = save(gameAction);
                        break;
                    case GameAction.UNDO:
                        moveStack.undoMoveSet(false);
                        result = true;
                        break;
                    case GameAction.FORCED_UNDO:
                        moveStack.undoMoveSet(true);
                        result = true;
                        break;
                    case GameAction.REDO:
                        moveStack.redoMoveSet();
                        result = true;
                        break;
                    case GameAction.EXPORT:
                        result = export(gameAction);
                        break;
                    }
                    if (result) break;

                }

                // All other actions: process per round
                result = processCorrectionActions(action) ||  getCurrentRound().process(action);
                break;
            }

            if (result && !(action instanceof GameAction) && action.hasActed()) {
                new AddToList<PossibleAction>(executedActions, action,
                        "ExecutedActions");
            }
        }

        // Note: round may have changed!
        possibleActions.clear();
        getCurrentRound().setPossibleActions();

        // moveStack closing is done here to allow state changes to occur
        // when setting possible actions
        if (action != null) {
            if (result && !(action instanceof GameAction) && action.hasActed()) {
                if (moveStack.isOpen()) moveStack.finish();
            } else {
                if (moveStack.isOpen()) moveStack.cancel();
            }
        }

        if (!isGameOver()) setCorrectionActions();

        // Add the Undo/Redo possibleActions here.
        if (moveStack.isUndoableByPlayer()) {
            possibleActions.add(new GameAction(GameAction.UNDO));
        }
        if (moveStack.isUndoableByManager()) {
            possibleActions.add(new GameAction(GameAction.FORCED_UNDO));
        }
        if (moveStack.isRedoable()) {
            possibleActions.add(new GameAction(GameAction.REDO));
        }


        // logging of game actions activated
        for (PossibleAction pa : possibleActions.getList()) {
            log.debug(((Player) currentPlayer.getObject()).getName() + " may: "
                      + pa.toString());
        }

        return result;

    }

    /**
     * Adds all Game actions
     * Examples are: undo/redo/corrections
     */
    private void setCorrectionActions(){

        // If any Correction is active
        for (CorrectionType ct:EnumSet.allOf(CorrectionType.class)) {
            CorrectionManagerI cm = getCorrectionManager(ct);
            if (cm.isActive()) {
                possibleActions.clear();
            }

        }

        // Correction Actions
        for (CorrectionType ct:EnumSet.allOf(CorrectionType.class)) {
            CorrectionManagerI cm = getCorrectionManager(ct);
            possibleActions.addAll(cm.createCorrections());
        }
    }

    private boolean processCorrectionActions(PossibleAction a){

       boolean result = false;

       if (a instanceof CorrectionAction) {
            CorrectionAction ca= (CorrectionAction)a;
            CorrectionType ct = ca.getCorrectionType();
            CorrectionManagerI cm = getCorrectionManager(ct);
            result = cm.executeCorrection(ca);
        }

        return result;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#processOnReload(java.util.List)
     */
    public boolean processOnReload(List<PossibleAction> actions) throws Exception {

        for (PossibleAction action : actions) {

            DisplayBuffer.clear();
            // TEMPORARY FIX TO ALLOW OLD 1856 SAVED FILES TO BE PROCESSED
            if (!possibleActions.contains(action.getClass())
                    && possibleActions.contains(RepayLoans.class)) {
                // Insert "Done"
                log.debug("Action DONE inserted");
                getCurrentRound().process(new NullAction (NullAction.DONE));
                getCurrentRound().setPossibleActions();
                if (!isGameOver()) setCorrectionActions();
            }

            try {
                log.debug("Action: " + action);
                if (!processCorrectionActions(action) && !getCurrentRound().process(action)) {
                    String msg = "Player "+action.getPlayerName()+"\'s action \""
                        +action.toString()+"\"\n  in "+getCurrentRound().getRoundName()
                        +" is considered invalid by the game engine";
                    log.error(msg);
                    DisplayBuffer.add(msg);
                    if (moveStack.isOpen()) moveStack.finish();
                    return false;
                }
                getCurrentRound().setPossibleActions();
                if (!isGameOver()) setCorrectionActions();

            } catch (Exception e) {
                log.debug("Error while reprocessing " + action.toString(), e);
                throw new Exception("Reload failure", e);
            }
            new AddToList<PossibleAction>(executedActions, action,
                    "ExecutedActions");
            if (moveStack.isOpen()) moveStack.finish();
        }

//        DisplayBuffer.clear();
//        previous line removed to allow display of nextPlayerMessages
        guiHints.clearVisibilityHints();

        return true;
    }

    protected boolean save(GameAction saveAction) {

        String filepath = saveAction.getFilepath();
        boolean result = false;

        try {
            ObjectOutputStream oos =
                    new ObjectOutputStream(new FileOutputStream(new File(
                            filepath)));
            oos.writeObject(Game.version+" "+BuildInfo.buildDate);
            oos.writeObject(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            oos.writeObject(saveFileVersionID);
            oos.writeObject(gameName);
            oos.writeObject(gameOptions);
            oos.writeObject(playerNames);
            oos.writeObject(executedActions);
            oos.close();

            result = true;
        } catch (IOException e) {
            log.error("Save failed", e);
            DisplayBuffer.add(LocalText.getText("SaveFailed", e.getMessage()));
        }

        return result;
    }

    protected boolean export(GameAction exportAction) {
        
        String filename = exportAction.getFilepath();
        boolean result = false;

        try {
            PrintWriter pw = new PrintWriter(filename);
            
            // write map information
            MapHex[][] allHexes =mapManager.getHexes();
            
            for (MapHex[] hexRow:allHexes)
                for (MapHex hex:hexRow)
                    if (hex != null) {
                        pw.println(hex.getName() + "," + hex.getCurrentTile().getExternalId() + "," 
                               + hex.getCurrentTileRotation() + "," 
                               + hex.getOrientationName(hex.getCurrentTileRotation())
                        ) ;
                    }
            
            pw.close();
            result = true;
            
            
        } catch (IOException e) {
            log.error("Save failed", e);
            DisplayBuffer.add(LocalText.getText("SaveFailed", e.getMessage()));
        }
        
        return result;
    }
    
    
    /* (non-Javadoc)
     * @see rails.game.GameManagerI#finishShareSellingRound()
     */
    public void finishShareSellingRound() {
        setRound(interruptedRound);
        guiHints.setCurrentRoundType(interruptedRound.getClass());
        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setActivePanel(GuiDef.Panel.MAP);
        getCurrentRound().resume();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#finishTreasuryShareRound()
     */
    public void finishTreasuryShareRound() {
        setRound(interruptedRound);
        guiHints.setCurrentRoundType(interruptedRound.getClass());
        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setActivePanel(GuiDef.Panel.MAP);
        ((OperatingRound) getCurrentRound()).nextStep();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#registerBankruptcy()
     */
    public void registerBankruptcy() {
        endedByBankruptcy.set(true);
        String message =
                LocalText.getText("PlayerIsBankrupt",
                        getCurrentPlayer().getName());
        ReportBuffer.add(message);
        DisplayBuffer.add(message);
        if (gameEndsWithBankruptcy) {
            finishGame();
        } else {
            Player player, newPresident;
            int numberOfPlayers = getNumberOfPlayers();
            int maxShare;
            int share;
            
            // Assume default case as in 18EU: all assets to Bank/Pool
            Player bankrupter = getCurrentPlayer();
            new CashMove (bankrupter, bank, bankrupter.getCash());
            Portfolio bpf = bankrupter.getPortfolio();
            List<PublicCompanyI> presidencies = new ArrayList<PublicCompanyI>();
            for (PublicCertificateI cert : bpf.getCertificates()) {
                if (cert.isPresidentShare()) presidencies.add(cert.getCompany());
            }
            for (PublicCompanyI company : presidencies) {
                // Check if the presidency is dumped on someone
                newPresident = null;
                maxShare = 0;
                for (int index=getCurrentPlayerIndex()+1; 
                        index<getCurrentPlayerIndex()+numberOfPlayers; index++) {
                    player = getPlayerByIndex(index%numberOfPlayers);
                    share = player.getPortfolio().getShare(company);
                    if (share >= company.getPresidentsShare().getShare()
                            && (share > maxShare)) {
                        maxShare = share;
                        newPresident = player;
                    }
                }
                if (newPresident != null) {
                    bankrupter.getPortfolio().swapPresidentCertificate(company, 
                            newPresident.getPortfolio());
                } else {
                    company.setClosed();
                    // TODO: can be restarted (in 18EU)
                }
            }
            // Dump all shares
            Util.moveObjects(bankrupter.getPortfolio().getCertificates(), bank.getPool());
        }
    }
    
    public void registerBrokenBank(){
        ReportBuffer.add(LocalText.getText("BankIsBrokenReportText"));
        String msgContinue;
        if (gameEndsAfterSetOfORs)
            msgContinue = LocalText.getText("bankIsBrokenPlaySetOfORs");
        else
            msgContinue = LocalText.getText("bankIsBrokenPlayOnlyOR");
        String msg = LocalText.getText("BankIsBrokenDisplayText", msgContinue);
        DisplayBuffer.add(msg);
        addToNextPlayerMessages(msg, true);
    }

    private void finishGame() {
        gameOver.set(true);

        String message = LocalText.getText("GameOver");
        ReportBuffer.add(message);
        DisplayBuffer.add(message);

        ReportBuffer.add("");
        
        List<String> gameReport = getGameReport();
        for (String s:gameReport)
            ReportBuffer.add(s);
        
        // activate gameReport for UI
        setGameOverReportedUI(false);

        createRound(EndOfGameRound.class);
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#isGameOver()
     */
    public boolean isGameOver() {
        return gameOver.booleanValue();
    }
    
    public void setGameOverReportedUI(boolean b){
        gameOverReportedUI = b;
    }

    public boolean getGameOverReportedUI(){
        return(gameOverReportedUI);
    }
    
    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getGameReport()
     */
    public List<String> getGameReport() {

        List<String> b = new ArrayList<String>();

        /* Sort players by total worth */
        List<Player> rankedPlayers = new ArrayList<Player>();
        for (Player player : players) {
            rankedPlayers.add(player);
        }
        Collections.sort(rankedPlayers);

        /* Report winner */
        Player winner = rankedPlayers.get(0);
        b.add(LocalText.getText("EoGWinner") + winner.getName()+ "!");
        b.add(LocalText.getText("EoGFinalRanking") + " :");

        /* Report final ranking */
        int i = 0;
        for (Player p : rankedPlayers) {
            b.add((++i) + ". " + Bank.format(p.getWorth()) + " "
                     + p.getName());
        }

        return b;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getCurrentRound()
     */
    public RoundI getCurrentRound() {
        return (RoundI) currentRound.getObject();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getCurrentPlayerIndex()
     */
    public int getCurrentPlayerIndex() {
        return getCurrentPlayer().getIndex();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#setCurrentPlayerIndex(int)
     */
    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        currentPlayerIndex = currentPlayerIndex % numberOfPlayers;
//        currentPlayer.set(players.get(currentPlayerIndex));
//      changed to activate nextPlayerMessages
        setCurrentPlayer(players.get(currentPlayerIndex));
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#setCurrentPlayer(rails.game.Player)
     */
    public void setCurrentPlayer(Player player) {
        // transfer messages for the next player to the display buffer
        if ((Player)currentPlayer.getObject() != player && !nextPlayerMessages.isEmpty()) {
            DisplayBuffer.add(
                    LocalText.getText("NextPlayerMessage", getCurrentPlayer().getName()));
            for (String s:nextPlayerMessages)
                DisplayBuffer.add(s);
            nextPlayerMessages.clear();
        }
        currentPlayer.set(player);
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#setPriorityPlayer()
     */
    public void setPriorityPlayer() {
        int priorityPlayerIndex =
                (getCurrentPlayer().getIndex() + 1) % numberOfPlayers;
        setPriorityPlayer(players.get(priorityPlayerIndex));

    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#setPriorityPlayer(rails.game.Player)
     */
    public void setPriorityPlayer(Player player) {
        priorityPlayer.set(player);
        log.debug("Priority player set to " + player.getIndex() + " "
                  + player.getName());
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getPriorityPlayer()
     */
    public Player getPriorityPlayer() {
        return (Player) priorityPlayer.getObject();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getCurrentPlayer()
     */
    public Player getCurrentPlayer() {
        return (Player) currentPlayer.getObject();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getPlayers()
     */
    public List<Player> getPlayers() {
        return players;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getNumberOfPlayers()
     */
    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getPlayerNames()
     */
    public List<String> getPlayerNames() {
        return playerNames;
    }

    public int getPlayerCertificateLimit(Player player) {
        return playerCertificateLimit.intValue();
    }

    public void setPlayerCertificateLimit(int newLimit) {
        playerCertificateLimit.set (newLimit);
    }

    public IntegerState getPlayerCertificateLimitModel () {
        return playerCertificateLimit;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getAllPublicCompanies()
     */
    public List<PublicCompanyI> getAllPublicCompanies() {
        return companyManager.getAllPublicCompanies();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getAllPrivateCompanies()
     */
    public List<PrivateCompanyI> getAllPrivateCompanies() {
        return companyManager.getAllPrivateCompanies();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getPlayerByIndex(int)
     */
    public Player getPlayerByIndex(int index) {
        return players.get(index % numberOfPlayers);
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#setNextPlayer()
     */
    public void setNextPlayer() {
        int currentPlayerIndex = getCurrentPlayerIndex();
        currentPlayerIndex = ++currentPlayerIndex % numberOfPlayers;
        setCurrentPlayerIndex(currentPlayerIndex);
    }

    public void addPortfolio (Portfolio portfolio) {
        portfolioMap.put(portfolio.getName(), portfolio);
    }

    public Portfolio getPortfolioByName (String name) {
        return portfolioMap.get(name);
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getStartPacket()
     */
    public StartPacket getStartPacket() {
        return startPacket;
    }

    public PhaseI getCurrentPhase() {
        return phaseManager.getCurrentPhase();
    }

    public PhaseManager getPhaseManager() {
        return phaseManager;
    }

    public String getGameName () {
        return gameName;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public TrainManager getTrainManager () {
        return trainManager;
    }

    public StockMarketI getStockMarket() {
        return stockMarket;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public TileManager getTileManager() {
        return tileManager;
    }

    /**
     * The RevenueManager is optional, thus a null reference might be returned
     */
    public RevenueManager getRevenueManager() {
        return revenueManager;
    }

    public Bank getBank () {
        return bank;
    }

    public String getGameOption (String key) {
        return gameOptions.get(key);
    }

    // TODO Should be removed
    public void initialiseNewPhase(PhaseI phase) {
        ReportBuffer.add(LocalText.getText("StartOfPhase", phase.getName()));

        phase.activate();

        // TODO The below should be merged into activate()
        if (phase.doPrivatesClose()) {
            companyManager.closeAllPrivates();
        }
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getHelp()
     */
    public String getHelp() {
        return getCurrentRound().getHelp();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#canAnyCompanyHoldShares()
     */
    public boolean canAnyCompanyHoldShares() {
        return (Boolean) getGuiParameter(GuiDef.Parm.CAN_ANY_COMPANY_HOLD_OWN_SHARES);
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getClassName(rails.common.Defs.ClassName)
     */
    public String getClassName (GuiDef.ClassName key) {

        switch (key) {

        case GAME_UI_MANAGER:
            return gameUIManagerClassName;

        case OR_UI_MANAGER:
            return orUIManagerClassName;

        case STATUS_WINDOW:
            return statusWindowClassName;

        case GAME_STATUS:
            return gameStatusClassName;

        default:
            return "";
        }
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getCommonParameter(rails.common.Defs.Parm)
     */
    public Object getGuiParameter (GuiDef.Parm key) {
        if (guiParameters.containsKey(key)) {
            return guiParameters.get(key);
        } else {
            return false;
        }
    }

    public void setGameParameter (GameDef.Parm key, Object value) {
        gameParameters.put(key, value);
    }

    public Object getGameParameter (GameDef.Parm key) {
        if (gameParameters.containsKey(key)) {
            return gameParameters.get(key);
        } else {
            return false;
        }
    }

    public RoundI getInterruptedRound() {
        return interruptedRound;
    }

    /**
     * Add an object.
     *
     * @param object The object to add.
     * @return True if successful.
     */
    public boolean addObject(Moveable object) {
        if (object instanceof SpecialPropertyI) {
            SpecialPropertyI sp = (SpecialPropertyI) object;
            sp.setHolder(null);
            return addSpecialProperty(sp);
        } else {
            return false;
        }
    }

    /**
     * Remove an object.
     *
     * @param object The object to remove.
     * @return True if successful.
     */
    public boolean removeObject(Moveable object) {
        if (object instanceof SpecialPropertyI) {
            return removeSpecialProperty((SpecialPropertyI) object);
        } else {
            return false;
        }
    }

    public boolean addSpecialProperty(SpecialPropertyI property) {

        if (commonSpecialProperties == null) {
            commonSpecialProperties = new ArrayList<SpecialPropertyI>(2);
        }
        return commonSpecialProperties.add(property);
    }

    /**
     * Remove a special property.
     *
     * @param property The special property object to remove.
     * @return True if successful.
     */
    public boolean removeSpecialProperty(SpecialPropertyI property) {

        if (commonSpecialProperties != null) {
            return commonSpecialProperties.remove(property);
        }

        return false;
    }

    public List<SpecialPropertyI> getCommonSpecialProperties () {
        return getSpecialProperties (null, false);
    }

    @SuppressWarnings("unchecked")
    public <T extends SpecialPropertyI> List<T> getSpecialProperties(
            Class<T> clazz, boolean includeExercised) {

        List<T> result = new ArrayList<T>();

        if (commonSpecialProperties != null) {
            for (SpecialPropertyI sp : commonSpecialProperties) {
                if ((clazz == null || clazz.isAssignableFrom(sp.getClass()))
                        && sp.isExecutionable()
                        && (!sp.isExercised() || includeExercised)) {
                    result.add((T) sp);
                }
            }
        }

        return result;
    }

    /**
     * Get name of the GM instance. Currently, the name is fixed,
     * but that will change whenever a multi-game server will be implemented.
     */
    public String getName () {
        return gmName;
    }

    public String getGMKey () {
        return gmKey;
    }

    public MoveStack getMoveStack () {
        return moveStack;
    }

    public DisplayBuffer getDisplayBuffer() {
        return displayBuffer;
    }

    public void addToNextPlayerMessages(String s, boolean undoable) {
        if (undoable)
            new AddToList<String>(nextPlayerMessages, s, "nextPlayerMessages");
        else
            nextPlayerMessages.add(s);
    }

    public ReportBuffer getReportBuffer() {
        return reportBuffer;
    }

    public GuiHints getUIHints() {
        return guiHints;
    }
    
    public CorrectionManagerI getCorrectionManager(CorrectionType ct) {
        CorrectionManagerI cm = correctionManagers.get(ct);
        if (cm == null) {
            cm=ct.newCorrectionManager(this);
            correctionManagers.put(ct, cm);
            log.debug("Added CorrectionManager for " + ct);
}
        return cm;
    }
   
}

