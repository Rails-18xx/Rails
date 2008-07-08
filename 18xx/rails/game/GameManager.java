/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/GameManager.java,v 1.34 2008/07/08 19:54:08 evos Exp $ */
package rails.game;

import java.io.*;
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
public class GameManager implements ConfigurableComponentI {
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
    protected String orUIManagerClassName = Defs.getDefaultClassName(Defs.ClassName.OR_UI_MANAGER);
    protected String gameStatusClassName = Defs.getDefaultClassName(Defs.ClassName.GAME_STATUS);
    protected String statusWindowClassName = Defs.getDefaultClassName(Defs.ClassName.STATUS_WINDOW);
    
    protected PlayerManager playerManager;
    protected CompanyManagerI companyManager;
    protected PhaseManager phaseManager;
    
    protected List<Player> players;
    protected List<String> playerNames;
    protected int numberOfPlayers;
    protected State currentPlayer = new State("CurrentPlayer", Player.class);
    protected State priorityPlayer = new State("PriorityPlayer", Player.class);

    protected int playerShareLimit = 60;
    protected int treasuryShareLimit = 50; // For some games
    protected int currentNumberOfOperatingRounds = 1;
    protected boolean skipFirstStockRound = false;

    //protected boolean companiesCanBuyPrivates = false;
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

    /**
     * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
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

        // ORUIManager class
        Tag orMgrTag = tag.getChild("ORUIManager");
        if (orMgrTag != null) {
            orUIManagerClassName =
                    orMgrTag.getAttributeAsString("class", "ORUIManager");
            // Check instantiatability (not sure if this belongs here)
            canClassBeInstantiated (orUIManagerClassName);
        }

        // GameStatus class
        Tag gameStatusTag = tag.getChild("GameStatus");
        if (gameStatusTag != null) {
            gameStatusClassName =
                    gameStatusTag.getAttributeAsString("class", "GameStatus");
            // Check instantiatability (not sure if this belongs here)
            canClassBeInstantiated (gameStatusClassName);
        }

        // StatusWindow class
        Tag statusWindowTag = tag.getChild("StatusWindow");
        if (statusWindowTag != null) {
            statusWindowClassName =
                    statusWindowTag.getAttributeAsString("class",
                            "StatusWindow");
            // Check instantiatability (not sure if this belongs here)
            canClassBeInstantiated (statusWindowClassName);
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

    public void startGame(PlayerManager playerManager,
            CompanyManagerI companyManager,
            PhaseManager phaseManager) {
        this.playerManager = playerManager;
        this.companyManager = companyManager;
        this.phaseManager = phaseManager;
        
        players = playerManager.getPlayers();
        playerNames = playerManager.getPlayerNames();
        numberOfPlayers = players.size();
        priorityPlayer.setState(players.get(0));
        
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
    public static GameManager getInstance() {
        return instance;
    }
    
    public CompanyManagerI getCompanyManager() {
        return companyManager;
    }

    public void setRound(RoundI round) {
        currentRound.set(round);
    }

    /**
     * Should be called by each Round when it finishes.
     * 
     * @param round The object that represents the finishing round.
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
        startRound.setGameManager(this);
        startRound.start (startPacket);
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
        or.start(operate, getCompositeORNumber());
    }
 
    protected <T extends Round> T createRound (Class<T> roundClass) {

        T round = null;
        try {
            round = roundClass.newInstance();
        } catch (Exception e) {
            log.fatal("Cannot instantiate class "
                      + roundClass.getName(), e);
            System.exit(1);
        }
        round.setGameManager (this);
        return round;
    }

    public String getCompositeORNumber() {
        return srNumber.intValue() + "."
               + relativeORNumber.intValue();
    }
    
    public int getSRNumber () {
        return srNumber.intValue();
    }

    public void startShareSellingRound(OperatingRound or,
            PublicCompanyI companyNeedingTrain, int cashToRaise) {

        interruptedRound = getCurrentRound();
        new ShareSellingRound(this, companyNeedingTrain, cashToRaise).start();
    }

    public void startTreasuryShareTradingRound(OperatingRound or,
            PublicCompanyI companyTradingShares) {

        interruptedRound = getCurrentRound();
        new TreasuryShareRound(this, companyTradingShares).start();
    }

    /**
     * The central server-side method that takes a client-side initiated action
     * and processes it.
     * 
     * @param action A PossibleAction subclass object sent by the client.
     * @return TRUE is the action was valid.
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
                        new String[] { actionPlayerName, currentPlayerName }));
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
        // log.debug("Calling setPossibleActions for round
        // "+getCurrentRound().toString());
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

    public void processOnReload(List<PossibleAction> actions) throws Exception {

        for (PossibleAction action : actions) {
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

    public void finishShareSellingRound() {
        // currentRound = interruptedRound;
        setRound(interruptedRound);
        ((OperatingRound) getCurrentRound()).resumeTrainBuying();
    }

    public void finishTreasuryShareRound() {
        setRound(interruptedRound);
        ((OperatingRound) getCurrentRound()).nextStep();
    }

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

    /**
     * To be called by the UI to check if the rails.game is over.
     * 
     * @return
     */
    public boolean isGameOver() {
        return gameOver;
    }

    public void logGameReport() {

        ReportBuffer.add(getGameReport());
    }

    /**
     * Create a HTML-formatted rails.game status report.
     * 
     * @return
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

    /**
     * Should be called whenever a Phase changes. The effect on the number of
     * ORs is delayed until a StockRound finishes.
     * 
     */
    public RoundI getCurrentRound() {
        return (RoundI) currentRound.getObject();
    }

    /**
     * @return Returns the currentPlayerIndex.
     */
    public int getCurrentPlayerIndex() {
        return getCurrentPlayer().getIndex();
    }

    /**
     * @param currentPlayerIndex The currentPlayerIndex to set.
     */
    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        currentPlayerIndex = currentPlayerIndex % numberOfPlayers;
        currentPlayer.set(players.get(currentPlayerIndex));
    }

    public void setCurrentPlayer(Player player) {
        currentPlayer.set(player);
    }

    /**
     * Set priority deal to the player after the current player.
     * 
     */
    public void setPriorityPlayer() {
        int priorityPlayerIndex =
                (getCurrentPlayer().getIndex() + 1) % numberOfPlayers;
        setPriorityPlayer(players.get(priorityPlayerIndex));

    }

    public void setPriorityPlayer(Player player) {
        priorityPlayer.set(player);
        log.debug("Priority player set to " + player.getIndex() + " "
                  + player.getName());
    }

    /**
     * @return Returns the priorityPlayer.
     */
    public Player getPriorityPlayer() {
        return (Player) priorityPlayer.getObject();
    }

    /**
     * @return Returns the currentPlayer.
     */
    public Player getCurrentPlayer() {
        return (Player) currentPlayer.getObject();
    }

    /**
     * @return Returns the players.
     */
    public List<Player> getPlayers() {
        return players;
    }

    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }
    
    public List<String> getPlayerNames() {
        return playerNames;
    }
    
    public List<PublicCompanyI> getAllPublicCompanies() {
        return companyManager.getAllPublicCompanies();
    }
    
    /**
     * Return a player by its index in the list, modulo the number of players.
     * 
     * @param index The player index.
     * @return A player object.
     */
    public Player getPlayerByIndex(int index) {
        return players.get(index % numberOfPlayers);
    }
    
    public void setNextPlayer() {
        int currentPlayerIndex = getCurrentPlayerIndex();
        currentPlayerIndex = ++currentPlayerIndex % numberOfPlayers;
        setCurrentPlayerIndex(currentPlayerIndex);
    }

    /**
     * @return the StartPacket
     */
    public StartPacket getStartPacket() {
        return startPacket;
    }

    /**
     * @return Current phase
     */
    public PhaseI getCurrentPhase() {
        return phaseManager.getCurrentPhase();
    }
    
    public PhaseManager getPhaseManager() {
        return phaseManager;
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

    /*
    public static void setCompaniesCanBuyPrivates() {
        instance.companiesCanBuyPrivates = true;
    }
    */

    public String getHelp() {
        return getCurrentRound().getHelp();
    }

    /*
    public static void setHasAnyParPrice(boolean hasAnyParPrice) {
        instance.hasAnyParPrice = hasAnyParPrice;
    }
    */

    public boolean canAnyCompanyHoldShares() {
        return canAnyCompanyHoldShares;
    }

    /*
    public static void setCanAnyCompanyHoldShares(
            boolean canAnyCompanyHoldShares) {
        instance.canAnyCompanyHoldShares = canAnyCompanyHoldShares;
    }
    */

    /*
    public static boolean canAnyCompBuyPrivates() {
        return instance.canAnyCompBuyPrivates;
    }
    */

    /*
    public static void setCanAnyCompBuyPrivates(boolean canAnyCompBuyPrivates) {
        instance.canAnyCompanyBuyPrivates = canAnyCompBuyPrivates;
    }
    */

    /*
    public static boolean doBonusTokensExist() {
        return instance.bonusTokensExist;
    }
    */

    /*
    public static void setBonusTokensExist(boolean bonusTokensExist) {
        instance.bonusTokensExist = bonusTokensExist;
    }
    */
    
    public String getClassName (Defs.ClassName key) {
        
        switch (key) {
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

    public int getStockRoundSequenceRule() {
        return stockRoundSequenceRule;
    }

    public int getTreasuryShareLimit() {
        return treasuryShareLimit;
    }
    
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
        default:
            return null;
        }

    }
    
}
