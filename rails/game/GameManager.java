/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/GameManager.java,v 1.22 2008/01/18 19:58:15 evos Exp $ */
package rails.game;

import rails.game.action.GameAction;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleActions;
import rails.game.move.AddToList;
import rails.game.move.MoveSet;
import rails.game.state.State;
import rails.ui.swing.ORUIManager;
import rails.util.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

import org.apache.log4j.Logger;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded &agrave; la 1830.
 */
public class GameManager implements ConfigurableComponentI
{
    /** Version ID of the Save file header, as written in save() */
    private static final long saveFileHeaderVersionID = 3L;
    /** Overall save file version ID, taking into account the
     * version ID of the action package.
     */
    public static final long saveFileVersionID 
            = saveFileHeaderVersionID * PossibleAction.serialVersionUID;
    
    protected Class<? extends OperatingRound> operatingRoundClass = OperatingRound.class;
    protected Class<? extends ORUIManager> orUIManagerClass = ORUIManager.class;

	protected List<Player> players;
	protected List<String> playerNames;
	protected int numberOfPlayers;
	protected State currentPlayer =
		new State ("CurrentPlayer", Player.class);
	protected State priorityPlayer = 
	    new State ("PriorityPlayer", Player.class);

	protected int playerShareLimit = 60;
	protected int currentNumberOfOperatingRounds = 1;

	protected boolean companiesCanBuyPrivates = false;
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
    protected State currentRound 
        = new State ("CurrentRound", Round.class);
	protected RoundI interruptedRound = null;

	protected int orNumber;
	protected int numOfORs;

	protected boolean gameOver = false;
	protected boolean endedByBankruptcy = false;
	protected boolean hasAnyParPrice = false;
	protected boolean canAnyCompBuyPrivates = false;
	protected boolean canAnyCompanyHoldShares = false;
	protected boolean bonusTokensExist = false;

	protected static GameManager instance;

	protected String name;

	protected StartPacket startPacket;
    
    PossibleActions possibleActions = PossibleActions.getInstance();
    
    List<PossibleAction> executedActions = new ArrayList<PossibleAction>();

	/** A List of available game options */
	protected List<GameOption> availableGameOptions = new ArrayList<GameOption>();
    
    /* Some standard tags for conditional attributes */
    public static final String VARIANT_KEY = "Variant";
    public static final String OPTION_TAG = "GameOption";
    public static final String IF_OPTION_TAG = "IfOption";
    public static final String ATTRIBUTES_TAG = "Attributes";

	protected static Logger log = Logger.getLogger(GameManager.class.getPackage().getName());

	/**
	 * Private constructor.
	 * 
	 */
	public GameManager()
	{
		instance = this;
	}

	/**
	 * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
	 */
	public void configureFromXML(Tag tag) throws ConfigurationException
	{
		/* Get the rails.game name as configured */
		Tag gameTag = tag.getChild("Game");
		if (gameTag == null) throw new ConfigurationException ("No Game tag specified in GameManager tag");
		name = gameTag.getAttributeAsString("name");
		if (name == null) throw new ConfigurationException ("No name specified in Game tag");

		// Get any available game options
		GameOption option;
		String optionName, optionType, optionValues, optionDefault;
        String optionNameParameters;
		List<Tag> optionTags = tag.getChildren(OPTION_TAG);
		if (optionTags != null) {
			for (Tag optionTag : optionTags)
			{
				optionName = optionTag.getAttributeAsString("name");
				if (optionName == null) throw new ConfigurationException ("GameOption without name");
				option = new GameOption (optionName);
				availableGameOptions.add (option);
                optionNameParameters = optionTag.getAttributeAsString("parm");
                if (optionNameParameters != null ) {
                    option.setParameters(optionNameParameters.split(","));
                }
				optionType = optionTag.getAttributeAsString("type");
				if (optionType != null) option.setType (optionType);
				optionValues = optionTag.getAttributeAsString("values");
				if (optionValues != null) option.setAllowedValues(optionValues.split(","));
				optionDefault = optionTag.getAttributeAsString("default");
				if (optionDefault != null) option.setDefaultValue(optionDefault);
			}
		}
        
        // OperatingRound class
        Tag orTag = tag.getChild("OperatingRound");
        if (orTag != null) {
            String orClassName = orTag.getAttributeAsString("class", "OperatingRound");
            try {
                operatingRoundClass = Class.forName(orClassName).asSubclass(OperatingRound.class);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException ("Cannot find class "+orClassName, e);
            }
        }

		/* Max. % of shares of one company that a player may hold */
		Tag shareLimitTag = tag.getChild("PlayerShareLimit");
		if (shareLimitTag != null)
		{
			Player.setShareLimit(shareLimitTag.getAttributeAsInteger("percentage"));
		}

		/* Max. % of shares of one company that the bank pool may hold */
		Tag poolLimitTag = tag.getChild("BankPoolShareLimit");
		if (poolLimitTag != null)
		{
			Bank.setPoolShareLimit(poolLimitTag.getAttributeAsInteger("percentage"));
		}

		/* End of rails.game criteria */
		Tag endOfGameTag = tag.getChild("EndOfGame");
		if (endOfGameTag != null)
		{
			if (endOfGameTag.getChild("Bankruptcy") != null)
			{
				gameEndsWithBankruptcy = true;
			}
			Tag bankBreaksTag = endOfGameTag.getChild("BankBreaks");
			if (bankBreaksTag != null)
			{
				gameEndsWhenBankHasLessOrEqual = bankBreaksTag.getAttributeAsInteger("limit",
						gameEndsWhenBankHasLessOrEqual);
				String attr = bankBreaksTag.getAttributeAsString("finish");
				if (attr.equalsIgnoreCase("SetOfORs"))
				{
					gameEndsAfterSetOfORs = true;
				}
				else if (attr.equalsIgnoreCase("CurrentOR"))
				{
					gameEndsAfterSetOfORs = false;
				}
			}
		}

		
        // ORUIManager class
        Tag orMgrTag = tag.getChild("ORUIManager");
        if (orMgrTag != null) {
            String orMgrClassName = orMgrTag.getAttributeAsString("class", "OperatingRound");
            try {
                orUIManagerClass = Class.forName(orMgrClassName).asSubclass(ORUIManager.class);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException ("Cannot find class "+orMgrClassName, e);
            }
        }
	}

	public void startGame()
	{
		players = Game.getPlayerManager().getPlayers();
		playerNames = Game.getPlayerManager().getPlayerNames();
		numberOfPlayers = players.size();
		priorityPlayer.setState(players.get(0));

		if (startPacket == null)
			startPacket = StartPacket.getStartPacket("Initial");
		if (startPacket != null && !startPacket.areAllSold())
		{
			// If we have a non-exhausted start packet
			startStartRound();
		}
		else
		{
			startStockRound();
		}
		
		// Initialisation is complete. Undoability starts here.
		MoveSet.enable();
	}

	/**
	 * @return instance of GameManager
	 */
	public static GameManager getInstance()
	{
		return instance;
	}

	public void setRound(RoundI round)
	{
		currentRound.set (round);
	}

	/**
	 * Should be called by each Round when it finishes.
	 * 
	 * @param round
	 *            The object that represents the finishing round.
	 */
	public void nextRound(RoundI round)
	{
		if (round instanceof StartRound)
		{
			if (startPacket != null && !startPacket.areAllSold())
			{
				startOperatingRound(false);
			}
			else
			{
				startStockRound();
			}
		}
		else if (round instanceof StockRound)
		{
		    PhaseI currentPhase = PhaseManager.getInstance().getCurrentPhase();
			numOfORs = currentPhase.getNumberOfOperatingRounds();
			log.info ("Phase=" + currentPhase.getName() + " ORs="
					+ numOfORs);

			// Create a new OperatingRound (never more than one Stock Round)
			OperatingRound.resetRelativeORNumber();
			startOperatingRound(true);

			orNumber = 1;
		}
		else if (round instanceof OperatingRound)
		{
			if (Bank.isBroken() && !gameEndsAfterSetOfORs)
			{

				finishGame();

			}
			else if (++orNumber <= numOfORs)
			{
				// There will be another OR
				startOperatingRound(true);
			}
			else if (startPacket != null && !startPacket.areAllSold())
			{
				startStartRound();
			}
			else
			{
				if (Bank.isBroken() && gameEndsAfterSetOfORs)
				{
					finishGame();
				}
				else
				{
					startStockRound();
				}
			}
		}
	}

	private void startStartRound()
	{
		String startRoundClassName = startPacket.getRoundClassName();
		((StartRound) instantiate(startRoundClassName)).start(startPacket);
	}

	private void startStockRound()
	{
		new StockRound().start();
	}

	private void startOperatingRound(boolean operate)
	{
		log.debug("Operating round started with operate-flag="+operate);
		//playHomeTokens(); // TODO Not always at this moment, and not at all is StartPacket has not yet been sold
        
		//new OperatingRound().start(operate);
        try {
            OperatingRound or = (OperatingRound)operatingRoundClass.newInstance();
            or.start(operate);
        } catch (Exception e) {
            log.fatal ("Cannot instantiate class "+operatingRoundClass.getName(), e);
            System.exit(1);
        }
	}

	public void startShareSellingRound(OperatingRound or,
			PublicCompanyI companyNeedingTrain, int cashToRaise)
	{

		interruptedRound = getCurrentRound();
		new ShareSellingRound(companyNeedingTrain, cashToRaise).start();
	}
    
    /** The central server-side method that takes 
     * a client-side initiated action and processes it.
     * @param action A PossibleAction subclass object sent by the client.
     * @return TRUE is the action was valid.
     */
    public boolean process (PossibleAction action) {

		boolean result = true;
		
    	// The action is null only immediately after Load.
    	if (action != null) {
    		
            action.setActed();
    		result = false;
    		
			// Check player
			String actionPlayerName = action.getPlayerName();
			String currentPlayerName = getCurrentPlayer().getName();
			if (!actionPlayerName.equals(currentPlayerName))
			{
				DisplayBuffer.add (LocalText.getText("WrongPlayer", new String[] {
						actionPlayerName,
						currentPlayerName
				}));
				return false;
			}
			
			// Check if the action is allowed
			if (!possibleActions.validate(action)) {
				DisplayBuffer.add (LocalText.getText("ActionNotAllowed", action.toString()));
				return false;
			}
			
	    	
	        for (;;) {
				
				// Process undo/redo centrally
				if (action instanceof GameAction) {
					
					GameAction gameAction = (GameAction) action;
					switch (gameAction.getMode()) {
	                case GameAction.SAVE:
	                    result = save (gameAction);
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
	            new AddToList<PossibleAction> (executedActions, action, "ExecutedActions");
	            if (MoveSet.isOpen()) MoveSet.finish();
	         } else {
	            if (MoveSet.isOpen()) MoveSet.cancel();
	         }
    	}
        
        // Note: round may have changed!
        getCurrentRound().setPossibleActions();

        // Add the Undo/Redo possibleActions here.
        if (MoveSet.isUndoableByPlayer()) {
            possibleActions.add (new GameAction (GameAction.UNDO));
        }
        if (MoveSet.isUndoableByManager()) {
            possibleActions.add (new GameAction (GameAction.FORCED_UNDO));
        }
        if (MoveSet.isRedoable()) {
            possibleActions.add(new GameAction (GameAction.REDO));
        }
        possibleActions.add(new GameAction (GameAction.SAVE));
        
        return result;
        
    }
    
    public void processOnReload (List<PossibleAction> actions) 
    throws Exception {
        
        for (PossibleAction action : actions) {
            try {
                getCurrentRound().process(action);
            } catch (Exception e) {
                log.debug("Error while reprocessing "+action.toString(), e);
                throw new Exception ("Reload failure", e);
                
            }
            new AddToList<PossibleAction> (executedActions, action, "ExecutedActions");
            if (MoveSet.isOpen()) MoveSet.finish();
        }
    }
    
    protected boolean save (GameAction saveAction) {
        
        String filepath = saveAction.getFilepath();
        boolean result = false;
        
        try {
            ObjectOutputStream oos = new ObjectOutputStream (
                    new FileOutputStream (new File (filepath)));
            oos.writeObject(saveFileVersionID);
            oos.writeObject(name);
            oos.writeObject(Game.getGameOptions());
            oos.writeObject(playerNames);
            oos.writeObject(executedActions);
            oos.close();
            
            result = true;
        } catch (IOException e) {
            log.error ("Save failed", e);
            DisplayBuffer.add (LocalText.getText("SaveFailed", e.getMessage()));
        }

        return result;
    }

	public void finishShareSellingRound()
	{
		//currentRound = interruptedRound;
        setRound (interruptedRound);
		((OperatingRound) getCurrentRound()).resumeTrainBuying();
	}

	public void registerBankruptcy()
	{
		endedByBankruptcy = true;
		String message = LocalText.getText("PlayerIsBankrupt",
				getCurrentPlayer().getName());
		ReportBuffer.add(message);
		DisplayBuffer.add (message);
		if (gameEndsWithBankruptcy)
		{
			finishGame();
		}
	}

	private void finishGame()
	{
		gameOver = true;
		ReportBuffer.add(LocalText.getText("GameOver"));
		currentRound.set (null);

		logGameReport();
	}

	/**
	 * To be called by the UI to check if the rails.game is over.
	 * 
	 * @return
	 */
	public static boolean isGameOver()
	{
		return instance.gameOver;
	}

	public void logGameReport()
	{

		ReportBuffer.add(getGameReport());
	}

	/**
	 * Create a HTML-formatted rails.game status report.
	 * 
	 * @return
	 */
	public String getGameReport()
	{

		StringBuffer b = new StringBuffer();

		/* Sort players by total worth */
		List<Player> rankedPlayers = new ArrayList<Player>();
		for (Player player : players)
		{
			rankedPlayers.add(player);
		}
		Collections.sort(rankedPlayers);

		/* Report winner */
		Player winner = (Player) rankedPlayers.get(0);
		b.append("The winner is " + winner.getName() + "!");

		/* Report final ranking */
		b.append("\n\nThe final ranking is:");
		int i = 0;
		for (Player p : rankedPlayers)
		{
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
	public RoundI getCurrentRound()
	{
		return (RoundI) currentRound.getObject();
	}

	/**
	 * @return Returns the currentPlayerIndex.
	 */
	public static int getCurrentPlayerIndex()
	{
		return getCurrentPlayer().getIndex();
	}

	/**
	 * @param currentPlayerIndex
	 *            The currentPlayerIndex to set.
	 */
	public static void setCurrentPlayerIndex(int currentPlayerIndex)
	{
		currentPlayerIndex = currentPlayerIndex % instance.numberOfPlayers;
        instance.currentPlayer.set (instance.players.get(currentPlayerIndex));
	}

	public static void setCurrentPlayer(Player player)
	{
        instance.currentPlayer.set(player);
	}

	/**
	 * Set priority deal to the player after the current player.
	 * 
	 */
	public static void setPriorityPlayer()
	{
		int priorityPlayerIndex = (getCurrentPlayer().getIndex() + 1) % instance.numberOfPlayers;
		setPriorityPlayer (instance.players.get(priorityPlayerIndex));

	}
	
	public static void setPriorityPlayer(Player player) {
        instance.priorityPlayer.set(player);
	    log.debug ("Priority player set to "
	    		+player.getIndex()+" "+player.getName());
	}

	/**
	 * @return Returns the priorityPlayer.
	 */
	public static Player getPriorityPlayer()
	{
		return (Player) instance.priorityPlayer.getObject();
	}

	/**
	 * @return Returns the currentPlayer.
	 */
	public static Player getCurrentPlayer()
	{
		return (Player)instance.currentPlayer.getObject();
	}

	/**
	 * @return Returns the players.
	 */
	public static List<Player> getPlayers()
	{
		return instance.players;
	}

	public static int getNumberOfPlayers()
	{
		return instance.numberOfPlayers;
	}

	/**
	 * Return a player by its index in the list, modulo the number of players.
	 * 
	 * @param index
	 *            The player index.
	 * @return A player object.
	 */
	public static Player getPlayer(int index)
	{
		return instance.players.get(index % instance.numberOfPlayers);
	}

	public static void setNextPlayer()
	{
		int currentPlayerIndex = getCurrentPlayerIndex();
		currentPlayerIndex = ++currentPlayerIndex % instance.numberOfPlayers;
		setCurrentPlayerIndex (currentPlayerIndex);
	}

	/**
	 * @return the StartPacket
	 */
	public StartPacket getStartPacket()
	{
		return startPacket;
	}

	public static String getName()
	{
		return instance.name;
	}

	/**
	 * @return Current phase
	 */
	public static PhaseI getCurrentPhase()
	{
		return PhaseManager.getInstance().getCurrentPhase();
	}

    // TODO Should be removed 
	public static void initialiseNewPhase(PhaseI phase)
	{
		ReportBuffer.add(LocalText.getText("StartOfPhase", phase.getName()));

        phase.activate();

        // TODO The below should be merged into activate() 
		if (phase.doPrivatesClose())
		{
			Game.getCompanyManager().closeAllPrivates();
		}
	}

    public static List<GameOption> getAvailableOptions () {
    	return instance.availableGameOptions;
    }

	private Object instantiate(String className)
	{
		try
		{
			return Class.forName(className).newInstance();
		}
		catch (Exception e)
		{
			log.fatal ("Cannot instantiate class " + className, e);
			return null;
		}
	}

	/*
	private void playHomeTokens()
	{
		// TODO: Need to check whether player gets to choose placement of token
		// where OO tiles are concerned.

		PublicCompanyI[] companies = (PublicCompanyI[]) Game.getCompanyManager()
				.getAllPublicCompanies()
				.toArray(new PublicCompanyI[0]);

		for (int compIndex = 0; compIndex < companies.length; compIndex++)
		{
		    PublicCompanyI company = companies[compIndex];
			if (company.hasFloated() && company.hasStarted())
			{
			    // If the home token has not been placed yet, do it.
				/* TODO: in reality, the home token placement time
				 * is rails.game-dependent, so it should be configured. (EV)
				 *//*
			    if (company.getNumberOfLaidBaseTokens() == 0) {
			        company.layHomeBaseTokens();
			    }
			    
			}
		}
	}
	*/

	public static void setCompaniesCanBuyPrivates()
	{
        instance.companiesCanBuyPrivates = true;
	}

	public static boolean getCompaniesCanBuyPrivates()
	{
		return instance.companiesCanBuyPrivates;
	}

	public String getHelp()
	{
		return getCurrentRound().getHelp();
	}

	public static boolean hasAnyParPrice() {
		return instance.hasAnyParPrice;
	}

	public static void setHasAnyParPrice(boolean hasAnyParPrice) {
        instance.hasAnyParPrice = hasAnyParPrice;
	}
	
	public static boolean canAnyCompanyHoldShares() {
		return instance.canAnyCompanyHoldShares;
	}

	public static void setCanAnyCompanyHoldShares(boolean canAnyCompanyHoldShares) {
		instance.canAnyCompanyHoldShares = canAnyCompanyHoldShares;
	}

	public static boolean canAnyCompBuyPrivates() {
		return instance.canAnyCompBuyPrivates;
	}

	public static void setCanAnyCompBuyPrivates(boolean canAnyCompBuyPrivates) {
        instance.canAnyCompBuyPrivates = canAnyCompBuyPrivates;
	}

	public static boolean doBonusTokensExist() {
		return instance.bonusTokensExist;
	}

	public static void setBonusTokensExist(boolean bonusTokensExist) {
		instance.bonusTokensExist = bonusTokensExist;
	}
	
	public Class<? extends ORUIManager> getORUIManagerClass() {
		return orUIManagerClass;
	}
	
	
}
