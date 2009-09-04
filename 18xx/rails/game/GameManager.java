/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/GameManager.java,v 1.47 2009/09/04 18:40:30 evos Exp $ */
package rails.game;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;

import org.apache.log4j.Logger;

import rails.common.Defs;
import rails.game.action.*;
import rails.game.move.AddToList;
import rails.game.move.MoveSet;
import rails.game.special.SpecialPropertyI;
import rails.game.special.SpecialTokenLay;
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
    protected String gameUIManagerClassName = Defs.getDefaultClassName(Defs.ClassName.GAME_UI_MANAGER);
    protected String orUIManagerClassName = Defs.getDefaultClassName(Defs.ClassName.OR_UI_MANAGER);
    protected String gameStatusClassName = Defs.getDefaultClassName(Defs.ClassName.GAME_STATUS);
    protected String statusWindowClassName = Defs.getDefaultClassName(Defs.ClassName.STATUS_WINDOW);
    protected String orWindowClassName = Defs.getDefaultClassName(Defs.ClassName.OR_WINDOW);

    protected PlayerManager playerManager;
    protected CompanyManagerI companyManager;
    protected PhaseManager phaseManager;
    protected TrainManagerI trainManager;

    protected List<Player> players;
    protected List<String> playerNames;
    protected int numberOfPlayers;
    protected State currentPlayer = new State("CurrentPlayer", Player.class);
    protected State priorityPlayer = new State("PriorityPlayer", Player.class);

    protected int playerShareLimit = 60;
    protected int treasuryShareLimit = 50; // For some games
    protected IntegerState playerCertificateLimit
    		= new IntegerState ("PlayerCertificateLimit", 0);
    protected int currentNumberOfOperatingRounds = 1;
    protected boolean skipFirstStockRound = false;

    protected boolean gameEndsWithBankruptcy = false;
    protected int gameEndsWhenBankHasLessOrEqual = 0;
    protected boolean gameEndsAfterSetOfORs = true;

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

    protected boolean gameOver = false;
    protected boolean endedByBankruptcy = false;
    protected boolean hasAnyParPrice = false;
    protected boolean canAnyCompanyBuyPrivates = false;
    protected boolean canAnyCompanyHoldShares = false;
    protected boolean bonusTokensExist = false;
    protected boolean hasAnyCompanyLoans = false;

    protected int stockRoundSequenceRule = StockRound.SELL_BUY_SELL;

    protected static GameManager instance;

    protected String name;

    protected StartPacket startPacket;

    PossibleActions possibleActions = PossibleActions.getInstance();

    List<PossibleAction> executedActions = new ArrayList<PossibleAction>();

    /** A List of available game options */
    protected List<GameOption> availableGameOptions =
            new ArrayList<GameOption>();

    /* Some standard tags for conditional attributes */
    public static final String VARIANT_KEY = "Variant";
    public static final String OPTION_TAG = "GameOption";
    public static final String IF_OPTION_TAG = "IfOption";
    public static final String ATTRIBUTES_TAG = "Attributes";

    protected static Logger log =
            Logger.getLogger(GameManager.class.getPackage().getName());

    /**
     * Private constructor.
     *
     */
    public GameManager() {
        instance = this;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#configureFromXML(rails.util.Tag)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        /* Get the rails.game name as configured */
        Tag gameTag = tag.getChild("Game");
        if (gameTag == null)
            throw new ConfigurationException(
                    "No Game tag specified in GameManager tag");
        name = gameTag.getAttributeAsString("name");
        if (name == null)
            throw new ConfigurationException("No name specified in Game tag");

        // Get any available game options
        GameOption option;
        String optionName, optionType, optionValues, optionDefault;
        String optionNameParameters;
        List<Tag> optionTags = tag.getChildren(OPTION_TAG);
        if (optionTags != null) {
            for (Tag optionTag : optionTags) {
                optionName = optionTag.getAttributeAsString("name");
                if (optionName == null)
                    throw new ConfigurationException("GameOption without name");
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
                optionDefault = optionTag.getAttributeAsString("default");
                if (optionDefault != null)
                    option.setDefaultValue(optionDefault);
            }
        }

        // StockRound class and other properties
        Tag srTag = tag.getChild("StockRound");
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
                    stockRoundSequenceRule = StockRound.SELL_BUY_SELL;
                } else if (stockRoundSequenceRuleString.equalsIgnoreCase("SellBuy")) {
                    stockRoundSequenceRule = StockRound.SELL_BUY;
                } else if (stockRoundSequenceRuleString.equalsIgnoreCase("SellBuyOrBuySell")) {
                    stockRoundSequenceRule = StockRound.SELL_BUY_OR_BUY_SELL;
                }
            }

            skipFirstStockRound =
                    srTag.getAttributeAsBoolean("skipFirst",
                            skipFirstStockRound);
        }

        // OperatingRound class
        Tag orTag = tag.getChild("OperatingRound");
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
        Tag shareLimitTag = tag.getChild("PlayerShareLimit");
        if (shareLimitTag != null) {
            Player.setShareLimit(shareLimitTag.getAttributeAsInteger("percentage"));
        }

        /* Max. % of shares of one company that the bank pool may hold */
        Tag poolLimitTag = tag.getChild("BankPoolShareLimit");
        if (poolLimitTag != null) {
            Bank.setPoolShareLimit(poolLimitTag.getAttributeAsInteger("percentage"));
        }

        /* Max. % of own shares that a company treasury may hold */
        Tag treasuryLimitTag = tag.getChild("TreasuryShareLimit");
        if (treasuryLimitTag != null) {
            treasuryShareLimit =
                    treasuryLimitTag.getAttributeAsInteger("percentage",
                            treasuryShareLimit);
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

        // GameUIManager class
        Tag gameUIMgrTag = tag.getChild("GameUIManager");
        if (gameUIMgrTag != null) {
            gameUIManagerClassName =
                    gameUIMgrTag.getAttributeAsString("class", gameUIManagerClassName);
            // Check instantiatability (not sure if this belongs here)
            canClassBeInstantiated (gameUIManagerClassName);
        }

        // ORUIManager class
        Tag orMgrTag = tag.getChild("ORUIManager");
        if (orMgrTag != null) {
            orUIManagerClassName =
                    orMgrTag.getAttributeAsString("class", orUIManagerClassName);
            // Check instantiatability (not sure if this belongs here)
            canClassBeInstantiated (orUIManagerClassName);
        }

        // GameStatus class
        Tag gameStatusTag = tag.getChild("GameStatus");
        if (gameStatusTag != null) {
            gameStatusClassName =
                    gameStatusTag.getAttributeAsString("class", gameStatusClassName);
            // Check instantiatability (not sure if this belongs here)
            canClassBeInstantiated (gameStatusClassName);
        }

        // StatusWindow class
        Tag statusWindowTag = tag.getChild("StatusWindow");
        if (statusWindowTag != null) {
            statusWindowClassName =
                    statusWindowTag.getAttributeAsString("class",
                            statusWindowClassName);
            // Check instantiatability (not sure if this belongs here)
            canClassBeInstantiated (statusWindowClassName);
        }

        // ORWindow class
        Tag orWindowTag = tag.getChild("ORWindow");
        if (orWindowTag != null) {
            orWindowClassName =
                orWindowTag.getAttributeAsString("class",
                        orWindowClassName);
            // Check instantiatability (not sure if this belongs here)
            canClassBeInstantiated (orWindowClassName);
        }
    }

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
    public void startGame(PlayerManager playerManager,
            CompanyManagerI companyManager,
            PhaseManager phaseManager,
            TrainManagerI trainManager) {
        this.playerManager = playerManager;
        this.companyManager = companyManager;
        this.phaseManager = phaseManager;
        this.trainManager = trainManager;

        players = playerManager.getPlayers();
        playerNames = playerManager.getPlayerNames();
        numberOfPlayers = players.size();
        priorityPlayer.setState(players.get(0));
        setPlayerCertificateLimit (playerManager.getInitialPlayerCertificateLimit());

        setGameParameters();

        if (startPacket == null)
            startPacket = StartPacket.getStartPacket("Initial");
        if (startPacket != null && !startPacket.areAllSold()) {
            // If we have a non-exhausted start packet
            startStartRound();
        } else {
            startStockRound();
        }

        // Initialisation is complete. Undoability starts here.
        MoveSet.enable();
    }

    private void setGameParameters () {

        for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {
            hasAnyParPrice = hasAnyParPrice || company.hasParPrice();
            canAnyCompanyBuyPrivates = canAnyCompanyBuyPrivates || company.canBuyPrivates();
            canAnyCompanyHoldShares = canAnyCompanyHoldShares || company.canHoldOwnShares();
            hasAnyCompanyLoans = hasAnyCompanyLoans || company.getMaxNumberOfLoans() != 0;
        }

loop:   for (PrivateCompanyI company : companyManager.getAllPrivateCompanies()) {
            for (SpecialPropertyI sp : company.getSpecialProperties()) {
                if (sp instanceof SpecialTokenLay
                        && ((SpecialTokenLay)sp).getToken() instanceof BonusToken) {
                    bonusTokensExist = true;
                    break loop;
                }
            }

        }
    }

    /**
     * @return instance of GameManager
     */
    public static GameManagerI getInstance() {
        return instance;
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
            PhaseI currentPhase = phaseManager.getCurrentPhase();
            numOfORs = currentPhase.getNumberOfOperatingRounds();
            log.info("Phase=" + currentPhase.getName() + " ORs=" + numOfORs);

            // Create a new OperatingRound (never more than one Stock Round)
            // OperatingRound.resetRelativeORNumber();
            relativeORNumber.set(1);
            startOperatingRound(true);

        } else if (round instanceof OperatingRound) {
            if (Bank.isBroken() && !gameEndsAfterSetOfORs) {

                finishGame();

            } else if (relativeORNumber.add(1) <= numOfORs) {
                // There will be another OR
                startOperatingRound(true);
            } else if (startPacket != null && !startPacket.areAllSold()) {
                startStartRound();
            } else {
                if (Bank.isBroken() && gameEndsAfterSetOfORs) {
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
        or.start(operate);
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

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getCompositeORNumber()
     */
    public String getCompositeORNumber() {
        return srNumber.intValue() + "." + relativeORNumber.intValue();
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
    public void startShareSellingRound(OperatingRound or,
            PublicCompanyI companyNeedingTrain, int cashToRaise) {

        interruptedRound = getCurrentRound();
        createRound (ShareSellingRound.class, interruptedRound).start();
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

        boolean result = true;

        // The action is null only immediately after Load.
        if (action != null) {

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
                        MoveSet.undo(false);
                        result = true;
                        break;
                    case GameAction.FORCED_UNDO:
                        MoveSet.undo(true);
                        result = true;
                        break;
                    case GameAction.REDO:
                        MoveSet.redo();
                        result = true;
                        break;
                    }
                    if (result) break;

                }

                // All other actions: process per round
                result = getCurrentRound().process(action);
                break;
            }

            if (result && !(action instanceof GameAction)) {
                new AddToList<PossibleAction>(executedActions, action,
                        "ExecutedActions");
                if (MoveSet.isOpen()) MoveSet.finish();
            } else {
                if (MoveSet.isOpen()) MoveSet.cancel();
            }
        }

        // Note: round may have changed!
        possibleActions.clear();
        getCurrentRound().setPossibleActions();

        for (PossibleAction pa : possibleActions.getList()) {
            log.debug(((Player) currentPlayer.getObject()).getName() + " may: "
                      + pa.toString());
        }

        // Add the Undo/Redo possibleActions here.
        if (MoveSet.isUndoableByPlayer()) {
            possibleActions.add(new GameAction(GameAction.UNDO));
        }
        if (MoveSet.isUndoableByManager()) {
            possibleActions.add(new GameAction(GameAction.FORCED_UNDO));
        }
        if (MoveSet.isRedoable()) {
            possibleActions.add(new GameAction(GameAction.REDO));
        }
        possibleActions.add(new GameAction(GameAction.SAVE));

        return result;

    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#processOnReload(java.util.List)
     */
    public void processOnReload(List<PossibleAction> actions) throws Exception {

        for (PossibleAction action : actions) {

            // TEMPORARY FIX TO ALLOW OLD 1856 SAVED FILES TO BE PROCESSED
            if (!possibleActions.contains(action.getClass())
                    && possibleActions.contains(RepayLoans.class)) {
                // Insert "Done"
                log.debug("Action DONE inserted");
                getCurrentRound().process(new NullAction (NullAction.DONE));
                getCurrentRound().setPossibleActions();
            }

            try {
                log.debug("Action: " + action);
                getCurrentRound().process(action);
                getCurrentRound().setPossibleActions();
            } catch (Exception e) {
                log.debug("Error while reprocessing " + action.toString(), e);
                throw new Exception("Reload failure", e);

            }
            new AddToList<PossibleAction>(executedActions, action,
                    "ExecutedActions");
            if (MoveSet.isOpen()) MoveSet.finish();
        }
    }

    protected boolean save(GameAction saveAction) {

        String filepath = saveAction.getFilepath();
        boolean result = false;

        try {
            ObjectOutputStream oos =
                    new ObjectOutputStream(new FileOutputStream(new File(
                            filepath)));
            oos.writeObject(saveFileVersionID);
            oos.writeObject(name);
            oos.writeObject(Game.getGameOptions());
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

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#finishShareSellingRound()
     */
    public void finishShareSellingRound() {
         setRound(interruptedRound);
        ((OperatingRound) getCurrentRound()).resume();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#finishTreasuryShareRound()
     */
    public void finishTreasuryShareRound() {
        setRound(interruptedRound);
        ((OperatingRound) getCurrentRound()).nextStep();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#registerBankruptcy()
     */
    public void registerBankruptcy() {
        endedByBankruptcy = true;
        String message =
                LocalText.getText("PlayerIsBankrupt",
                        getCurrentPlayer().getName());
        ReportBuffer.add(message);
        DisplayBuffer.add(message);
        if (gameEndsWithBankruptcy) {
            finishGame();
        }
    }

    private void finishGame() {
        gameOver = true;
        ReportBuffer.add(LocalText.getText("GameOver"));
        currentRound.set(null);

        logGameReport();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#isGameOver()
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#logGameReport()
     */
    public void logGameReport() {

        ReportBuffer.add(getGameReport());
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getGameReport()
     */
    public String getGameReport() {

        StringBuffer b = new StringBuffer();

        /* Sort players by total worth */
        List<Player> rankedPlayers = new ArrayList<Player>();
        for (Player player : players) {
            rankedPlayers.add(player);
        }
        Collections.sort(rankedPlayers);

        /* Report winner */
        Player winner = rankedPlayers.get(0);
        b.append("The winner is " + winner.getName() + "!");

        /* Report final ranking */
        b.append("\n\nThe final ranking is:");
        int i = 0;
        for (Player p : rankedPlayers) {
            b.append("\n" + (++i) + ". " + Bank.format(p.getWorth()) + " "
                     + p.getName());
        }

        return b.toString();
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
        currentPlayer.set(players.get(currentPlayerIndex));
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#setCurrentPlayer(rails.game.Player)
     */
    public void setCurrentPlayer(Player player) {
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

    public int getPlayerCertificateLimit() {
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

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getStartPacket()
     */
    public StartPacket getStartPacket() {
        return startPacket;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getCurrentPhase()
     */
    public PhaseI getCurrentPhase() {
        return phaseManager.getCurrentPhase();
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getPhaseManager()
     */
    public PhaseManager getPhaseManager() {
        return phaseManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public TrainManagerI getTrainManager () {
        return trainManager;
    }

    // TODO Should be removed
    public static void initialiseNewPhase(PhaseI phase) {
        ReportBuffer.add(LocalText.getText("StartOfPhase", phase.getName()));

        phase.activate();

        // TODO The below should be merged into activate()
        if (phase.doPrivatesClose()) {
            instance.companyManager.closeAllPrivates();
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
        return canAnyCompanyHoldShares;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getClassName(rails.common.Defs.ClassName)
     */
    public String getClassName (Defs.ClassName key) {

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
     * @see rails.game.GameManagerI#getStockRoundSequenceRule()
     */
    public int getStockRoundSequenceRule() {
        return stockRoundSequenceRule;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getTreasuryShareLimit()
     */
    public int getTreasuryShareLimit() {
        return treasuryShareLimit;
    }

    /* (non-Javadoc)
     * @see rails.game.GameManagerI#getCommonParameter(rails.common.Defs.Parm)
     */
    public Object getCommonParameter (Defs.Parm key) {
        switch (key) {
        case HAS_ANY_PAR_PRICE:
            return hasAnyParPrice;
        case CAN_ANY_COMPANY_BUY_PRIVATES:
            return canAnyCompanyBuyPrivates;
        case CAN_ANY_COMPANY_HOLD_OWN_SHARES:
            return canAnyCompanyHoldShares;
        case DO_BONUS_TOKENS_EXIST:
            return bonusTokensExist;
        case HAS_ANY_COMPANY_LOANS:
            return hasAnyCompanyLoans;
        default:
            return null;
        }

    }

    public RoundI getInterruptedRound() {
        return interruptedRound;
    }

}
