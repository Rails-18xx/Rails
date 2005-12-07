/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/GameManager.java,v 1.13 2005/12/07 22:34:24 evos Exp $
 * 
 * Created on 04-May-2005
 * Change Log:
 */
package game;

import util.*;

import java.util.*;

import org.w3c.dom.*;

/**
 * This class manages the playing rounds by supervising all implementations of
 * Round. Currently everything is hardcoded à la 1830.
 * 
 * @author Erik Vos
 */
public class GameManager implements ConfigurableComponentI
{

	protected static Player[] players;
	protected static int numberOfPlayers;
	protected static int currentPlayerIndex = 0;
	protected static Player currentPlayer = null;
	protected static int priorityPlayerIndex = 0;
	protected static Player priorityPlayer = null;

	protected static int playerShareLimit = 60;

	/**
	 * Current round should not be set here but from within the Round classes.
	 * This is because in some cases the round has already changed to another
	 * one when the constructor terminates. Example: if the privates have not
	 * been sold, it finishes by starting an Operating Round, which handles the
	 * privates payout and then immediately starts a new Start Round.
	 */
	protected static Round currentRound = null;

	protected Round insertingRound = null;
	protected Round insertedRound = null;
	protected int orNumber;
	protected int numOfORs;
	protected int[] orsPerPhase = new int[] { 1, 2, 2, 3, 3 };
	protected int phase = 0;

	protected static PhaseI currentPhase = null;

	protected static GameManager instance;

	protected static String name;

	protected StartPacket startPacket;

	/*----- Default variant -----*/
	/* Others will always be configured per game */
	public static final String STANDARD = "Standard";

	/** Start round variant, can be used where applicable */
	protected static String variant = STANDARD;

	/** A Map of variant names */
	protected static List lVariants = new ArrayList();
	protected static Map mVariants = new HashMap();

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
	 * @see game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
	 */
	public void configureFromXML(Element el) throws ConfigurationException
	{
		/* Get the game name as configured */
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
			Bank.setShareLimit(XmlUtils.extractIntegerAttribute(nnp,
					"percentage"));
		}
	}

	public void startGame()
	{
		players = Game.getPlayerManager().getPlayersArray();
		numberOfPlayers = players.length;

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

	public void setRound(Round round)
	{

		currentRound = round;
	}

	/**
	 * Should be called by each Round when it finishes.
	 * 
	 * @param round
	 *            The object that represents the finishing round.
	 */
	public void nextRound(Round round)
	{

		if (round instanceof StartRound)
		{

			if (startPacket != null && !startPacket.areAllSold())
			{

				startOperatingRound();

			}
			else
			{

				startStockRound();

			}

		}
		else if (round instanceof StockRound)
		{

			// Create a new OperatingRound (never more than one Stock Round)
			OperatingRound.resetRelativeORNumber();
			startOperatingRound();

			numOfORs = orsPerPhase[phase];
			orNumber = 1;

		}
		else if (round instanceof OperatingRound)
		{

			if (++orNumber < numOfORs)
			{

				// There will be another OR
				startOperatingRound();

			}
			else if (startPacket != null && !startPacket.areAllSold())
			{

				startStartRound();

			}
			else
			{

				startStockRound();
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

	private void startOperatingRound()
	{
		playHomeTokens();
		new OperatingRound();
	}

	/**
	 * Should be called whenever a Phase changes. The effect on the number of
	 * ORs is delayed until a StockRound finishes.
	 * 
	 */
	public void nextPhase()
	{
		if (phase < orsPerPhase.length - 1)
			phase++;
	}

	public int getPhase()
	{
		return phase;
	}

	public Round getCurrentRound()
	{
		return currentRound;
	}

	/**
	 * @return Returns the currentPlayerIndex.
	 */
	public static int getCurrentPlayerIndex()
	{
		return currentPlayerIndex;
	}

	/**
	 * @param currentPlayerIndex
	 *            The currentPlayerIndex to set.
	 */
	public static void setCurrentPlayerIndex(int currentPlayerIndex)
	{
		GameManager.currentPlayerIndex = currentPlayerIndex % numberOfPlayers;
		GameManager.currentPlayer = players[GameManager.currentPlayerIndex];
	}

	public static void setCurrentPlayer(Player player)
	{
		currentPlayer = player;
		for (int i = 0; i < numberOfPlayers; i++)
		{
			if (player == players[i])
			{
				currentPlayerIndex = i;
				break;
			}
		}
	}

	/**
	 * @return Returns the priorityPlayerIndex.
	 */
	public static int getPriorityPlayerIndex()
	{
		return priorityPlayerIndex;
	}

	/**
	 * @param priorityPlayerIndex
	 *            The priorityPlayerIndex to set. The value may exceed the
	 *            number of players; if so, the modulus is taken. This allows
	 *            giving the next player the priority bu adding +1.
	 */
	public static void setPriorityPlayerIndex(int priorityPlayerIndex)
	{
		GameManager.priorityPlayerIndex = priorityPlayerIndex % numberOfPlayers;
		GameManager.priorityPlayer = players[GameManager.priorityPlayerIndex];
	}

	/**
	 * Set priority deal to the player after the current player.
	 * 
	 */
	public static void setPriorityPlayer()
	{
		priorityPlayerIndex = (currentPlayerIndex + 1) % numberOfPlayers;
		priorityPlayer = players[priorityPlayerIndex];

	}

	/**
	 * @return Returns the currentPlayer.
	 */
	public static Player getCurrentPlayer()
	{
		return currentPlayer;
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

	/**
	 * @return Returns the priorityPlayer.
	 */
	public static Player getPriorityPlayer()
	{
		return priorityPlayer;
	}

	public static void setNextPlayer()
	{
		currentPlayerIndex = ++currentPlayerIndex % numberOfPlayers;
		currentPlayer = players[currentPlayerIndex];
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
	public static List getVariants()
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
		return currentPhase;
	}

	protected static void addVariant(String name)
	{
		if (!mVariants.containsKey(name))
		{
			lVariants.add(name);
			mVariants.put(name, null);
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
			Log.write("Variant is " + variant);
		}
	}

	public static boolean existVariant(String variant)
	{
		return mVariants.containsKey(variant);
	}

	private Object instantiate(String className)
	{
		try
		{
			return Class.forName(className).newInstance();
		}
		catch (Exception e)
		{
			Log.error("Cannot instantiate class " + className);
			System.out.println(e.getStackTrace());
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
			System.out.println(companies[compIndex]);
			if (companies[compIndex].hasFloated()
					&& companies[compIndex].hasStarted())
			{
				MapHex[][] map = MapManager.getInstance().getHexes();

				for (int i = 0; i < map.length; i++)
				{
					for (int j = 0; j < map[i].length; j++)
					{
						// if these are the same number, we haven't yet played
						// the city token.
						if (((PublicCompany) companies[compIndex]).getMaxCityTokens() == 
							((PublicCompany) companies[compIndex]).getNumCityTokens())
						{
							try
							{
								if (map[i][j].getCompanyHome().equals(companies[compIndex]))
								{
									if(map[i][j].getPreferredHomeCity() > 0)
										map[i][j].addToken(companies[compIndex], map[i][j].getPreferredHomeCity()-1);
									else
										map[i][j].addToken(companies[compIndex]);
									
								}
							}
							catch (NullPointerException e)
							{
								// Not our home or no home here. So sad.
							}
						}
					}
				}
			}
		}
	}

	public String getHelp()
	{
		return currentRound.getHelp();
	}

}
