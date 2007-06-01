package rails.game;

import rails.game.state.State;
import rails.util.*;

import java.util.*;

import org.apache.log4j.Logger;
import org.w3c.dom.*;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded à la 1830.
 */
public class GameManager implements ConfigurableComponentI
{

	protected static Player[] players;
	protected static int numberOfPlayers;
	//protected static int currentPlayerIndex = 0;
	//protected static Player currentPlayer = null;
	protected static State currentPlayer =
		new State ("CurrentPlayer", Player.class);
	//protected static int priorityPlayerIndex = 0;
	//protected static Player priorityPlayer = null;
	protected static State priorityPlayer = 
	    new State ("PriorityPlayer", Player.class);

	protected static int playerShareLimit = 60;
	protected static int currentNumberOfOperatingRounds = 1;

	protected static boolean companiesCanBuyPrivates = false;
	protected static boolean gameEndsWithBankruptcy = false;
	protected static int gameEndsWhenBankHasLessOrEqual = 0;
	protected static boolean gameEndsAfterSetOfORs = true;

	/**
	 * Current round should not be set here but from within the Round classes.
	 * This is because in some cases the round has already changed to another
	 * one when the constructor terminates. Example: if the privates have not
	 * been sold, it finishes by starting an Operating Round, which handles the
	 * privates payout and then immediately starts a new Start Round.
	 */
	protected static RoundI currentRound = null;
	protected static RoundI interruptedRound = null;

	// protected Round insertingRound = null;
	// protected Round insertedRound = null;
	protected int orNumber;
	protected int numOfORs;

	//protected static PhaseI currentPhase = null;
	protected static boolean gameOver = false;
	protected static boolean endedByBankruptcy = false;
	protected static boolean hasAnyParPrice = false;
	protected static boolean canAnyCompBuyPrivates = false;

	protected static GameManager instance;

	protected static String name;

	protected StartPacket startPacket;

	/*----- Default variant -----*/
	/* Others will always be configured per rails.game */
	public static final String STANDARD = "Standard";

	/** Start round variant, can be used where applicable */
	protected static String variant = STANDARD;

	/** A Map of variant names */
	protected static List<String> lVariants = new ArrayList<String>();
	//protected static Map mVariants = new HashMap();

	protected static Logger log = Logger.getLogger(GameManager.class.getPackage().getName());

	static
	{
		addVariant(STANDARD);
	}

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
	public void configureFromXML(Element el) throws ConfigurationException
	{
		/* Get the rails.game name as configured */
		Element element = (Element) el.getElementsByTagName("Game").item(0);
		NamedNodeMap nnp = element.getAttributes();
		name = XmlUtils.extractStringAttribute(nnp, "name");

		/* Get any variant names */
		NodeList nl = el.getElementsByTagName("Variant");
		String varName;
		for (int i = 0; i < nl.getLength(); i++)
		{
			element = (Element) nl.item(i);
			nnp = element.getAttributes();
			varName = XmlUtils.extractStringAttribute(nnp, "name");
			if (varName != null)
				addVariant(varName);
		}

		/* Max. % of shares of one company that a player may hold */
		element = (Element) el.getElementsByTagName("PlayerShareLimit").item(0);
		if (element != null)
		{
			nnp = element.getAttributes();
			Player.setShareLimit(XmlUtils.extractIntegerAttribute(nnp,
					"percentage"));
		}

		/* Max. % of shares of one company that the bank pool may hold */
		element = (Element) el.getElementsByTagName("BankPoolShareLimit")
				.item(0);
		if (element != null)
		{
			nnp = element.getAttributes();
			Bank.setPoolShareLimit(XmlUtils.extractIntegerAttribute(nnp,
					"percentage"));
		}

		/* End of rails.game criteria */
		element = (Element) el.getElementsByTagName("EndOfGame").item(0);
		if (element != null)
		{
			Element el2;
			nl = element.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++)
			{
				if (!(nl.item(i) instanceof Element))
					continue;
				el2 = (Element) nl.item(i);
				if (el2.getNodeName().equals("Bankruptcy"))
				{
					gameEndsWithBankruptcy = true;
				}
				else if (el2.getNodeName().equals("BankBreaks"))
				{
					nnp = el2.getAttributes();
					gameEndsWhenBankHasLessOrEqual = XmlUtils.extractIntegerAttribute(nnp,
							"limit",
							gameEndsWhenBankHasLessOrEqual);
					String attr = XmlUtils.extractStringAttribute(nnp, "finish");
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
		}
	}

	public void startGame()
	{
		players = Game.getPlayerManager().getPlayersArray();
		numberOfPlayers = players.length;
		//setPriorityPlayer (players[0]);
		priorityPlayer.setState(players[0]);

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

		currentRound = round;
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
		playHomeTokens(); // TODO Not always at this moment, and not at all is StartPacket has not yet been sold
		new OperatingRound(operate);
	}

	public void startShareSellingRound(OperatingRound or,
			PublicCompanyI companyNeedingTrain, int cashToRaise)
	{

		interruptedRound = currentRound;
		new ShareSellingRound(companyNeedingTrain, cashToRaise).start();
	}

	public void finishShareSellingRound()
	{
		currentRound = interruptedRound;
		((OperatingRound) currentRound).resumeTrainBuying();
	}

	public void registerBankruptcy()
	{
		endedByBankruptcy = true;
		ReportBuffer.add(LocalText.getText("PlayerIsBankrupt",
				currentPlayer.getName()));
		if (gameEndsWithBankruptcy)
		{
			finishGame();
		}
	}

	private void finishGame()
	{
		gameOver = true;
		ReportBuffer.add(LocalText.getText("GameOver"));
		currentRound = null;

		logGameReport();
	}

	/**
	 * To be called by the UI to check if the rails.game is over.
	 * 
	 * @return
	 */
	public static boolean isGameOver()
	{
		return gameOver;
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
		for (int ip = 0; ip < players.length; ip++)
		{
			rankedPlayers.add(players[ip]);
		}
		Collections.sort(rankedPlayers);

		/* Report winner */
		Player winner = (Player) rankedPlayers.get(0);
		b.append("The winner is " + winner.getName() + "!");

		/* Report final ranking */
		b.append("\n\nThe final ranking is:");
		//Player p;
		int i = 0;
		//for (Iterator it = rankedPlayers.iterator(); it.hasNext();)
		for (Player p : rankedPlayers)
		{
			//p = (Player) it.next();
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
		return currentRound;
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
		currentPlayerIndex = currentPlayerIndex % numberOfPlayers;
		currentPlayer.set (players[currentPlayerIndex]);
	}

	public static void setCurrentPlayer(Player player)
	{
		currentPlayer.set(player);
	}

	/**
	 * Set priority deal to the player after the current player.
	 * 
	 */
	public static void setPriorityPlayer()
	{
		int priorityPlayerIndex = (getCurrentPlayer().getIndex() + 1) % numberOfPlayers;
		setPriorityPlayer (players[priorityPlayerIndex]);

	}
	
	public static void setPriorityPlayer(Player player) {
	    priorityPlayer.set(player);
	    log.debug ("Priority player set to "
	    		+player.getIndex()+" "+player.getName());
	}

	/**
	 * @return Returns the priorityPlayer.
	 */
	public static Player getPriorityPlayer()
	{
		return (Player) priorityPlayer.getState();
	}

	/**
	 * @return Returns the currentPlayer.
	 */
	public static Player getCurrentPlayer()
	{
		return (Player)currentPlayer.getState();
	}

	/**
	 * @return Returns the players.
	 */
	public static Player[] getPlayers()
	{
		return players;
	}

	public static int getNumberOfPlayers()
	{
		return numberOfPlayers;
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
		return players[index % players.length];
	}

	public static void setNextPlayer()
	{
		int currentPlayerIndex = getCurrentPlayerIndex();
		currentPlayerIndex = ++currentPlayerIndex % numberOfPlayers;
		setCurrentPlayerIndex (currentPlayerIndex);
	}

	/**
	 * @return the StartPacket
	 */
	public StartPacket getStartPacket()
	{
		return startPacket;
	}

	/**
	 * @return List of variants
	 */
	public static List<String> getVariants()
	{
		return lVariants;
	}

	public static String getName()
	{
		return name;
	}

	/**
	 * @return Current phase
	 */
	public static PhaseI getCurrentPhase()
	{
		return PhaseManager.getInstance().getCurrentPhase();
	}

	public static void initialiseNewPhase(PhaseI phase)
	{
		ReportBuffer.add(LocalText.getText("StartOfPhase", phase.getName()));
		if (phase.doPrivatesClose())
		{
			Game.getCompanyManager().closeAllPrivates();
		}
	}

	protected static void addVariant(String name)
	{
		if (!lVariants.contains(name))
		{
			lVariants.add(name);
			//mVariants.put(name, null);
		}
	}

	public static String getVariant()
	{
		return variant;
	}

	public static void setVariant(String variant)
	{
		if (existVariant(variant))
		{
			GameManager.variant = variant;
			ReportBuffer.add(LocalText.getText("VariantIs",  variant));
			log.info ("Game variant is "+variant);
		} else {
			log.error ("Unknown variant selected: "+variant);
		}
	}

	public static boolean existVariant(String variant)
	{
		//return mVariants.containsKey(variant);
		return lVariants.contains(variant);
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
				 */
			    if (company.getNumberOfLaidBaseTokens() == 0) {
			        company.layHomeBaseTokens();
			    }
			    
			}
		}
	}

	public static void setCompaniesCanBuyPrivates()
	{
		companiesCanBuyPrivates = true;
	}

	public static boolean getCompaniesCanBuyPrivates()
	{
		return companiesCanBuyPrivates;
	}

	public String getHelp()
	{
		return currentRound.getHelp();
	}

	public static boolean hasAnyParPrice() {
		return hasAnyParPrice;
	}

	public static void setHasAnyParPrice(boolean hasAnyParPrice) {
		GameManager.hasAnyParPrice = hasAnyParPrice;
	}

	public static boolean canAnyCompBuyPrivates() {
		return canAnyCompBuyPrivates;
	}

	public static void setCanAnyCompBuyPrivates(boolean canAnyCompBuyPrivates) {
		GameManager.canAnyCompBuyPrivates = canAnyCompBuyPrivates;
	}
	
	
}
