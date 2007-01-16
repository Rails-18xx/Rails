package game;

import java.util.*;

import util.LocalText;

/**
 * Implements an 1835-style startpacket sale.
 */
public class StartRound_1835 extends StartRound
{

	/* To control the player sequence in the Clemens and Snake variants */
	private static int cycle = 0;
	private static int startRoundNumber = 0;
	private int turns = 0;
	private int numberOfPlayers = GameManager.getNumberOfPlayers();
	private String variant;

	/* Additional variants */
	public static final String CLEMENS_VARIANT = "Clemens";
	public static final String SNAKE_VARIANT = "Snake";

	/**
	 * Constructor, only to be used in dynamic instantiation.
	 */
	public StartRound_1835()
	{
		super();
		hasBidding = false;
	}

	/**
	 * Start the 1835-style start round.
	 * 
	 * @param startPacket
	 *            The startpacket to be sold in this start round.
	 */
	public void start(StartPacket startPacket)
	{
		super.start(startPacket);
		startRoundNumber++;
		variant = GameManager.getVariant();

		// Select first player
		if (variant.equalsIgnoreCase("Clemens"))
		{
			GameManager.setCurrentPlayerIndex(numberOfPlayers - 1);
		}
		else
		{
			GameManager.setCurrentPlayerIndex(0);
		}

		// Select initially buyable items
		defaultStep = nextStep = BUY_OR_PASS;
		//getBuyableItems(); // Needed for Start Window
	}

	/**
	 * Get a list of items that may be bought immediately.
	 * <p>
	 * In an 1835-style auction this method will usually return several items.
	 * 
	 * @return An array of start items that can be bought.
	 */
	public StartItem[] getBuyableItems() {return null;}
	
	public List getStartItems()
	{
		Player currentPlayer = GameManager.getCurrentPlayer();
		int cashToSpend = currentPlayer.getCash();
		List startItems = startPacket.getItems();
		StartItem item;
		int row;
		int minRow = 0;
		int items = 0;
		
		Iterator it = startItems.iterator();
		while (it.hasNext())
		{
			item = (StartItem) it.next();
			if (item.isSold()) {
				item.setStatus (StartItem.SOLD);
			} else if (item.getBasePrice() > cashToSpend) {
				item.setStatus (StartItem.UNAVAILABLE);
			} else if (variant.equalsIgnoreCase(CLEMENS_VARIANT)) {
				item.setStatus (StartItem.BUYABLE);
			} else {
				row = item.getRow();
				if (minRow == 0)
					minRow = row;
				if (row == minRow)
				{
					// Allow all items in the top row.
					item.setStatus (StartItem.BUYABLE);
					items++;
				}
				else if (row == minRow + 1 && items == 1)
				{
					// Allow the first item in the next row if the
					// top row has only one item.
					item.setStatus (StartItem.BUYABLE);
				} else {
					item.setStatus (StartItem.UNAVAILABLE);
				}
			}
		}
		
		return startItems;
	}

	/**
	 * Get a list of items that teh current player may bid upon.
	 * <p>
	 * In an 1835-style auction this method will always return an empty list.
	 * 
	 * @return An empty array of start items.
	 */
	public StartItem[] getBiddableItems()
	{
		return new StartItem[0];
	}

	/**
	 * Get the company for which a par price must be set in the SET_PRICE state.
	 * Not used in 1835.
	 * 
	 * @return Always null.
	 */
	public PublicCompanyI getCompanyNeedingPrice()
	{
		return null;
	}

	/*----- MoveSet methods -----*/

	/**
	 * The current player bids 5 more than the previous bid on a given start
	 * item.
	 * <p>
	 * A separate method is provided for this action because 5 is the usual
	 * amount with which bids are raised.
	 * 
	 * @param playerName
	 *            The name of the current player (for checking purposes).
	 * @param itemName
	 *            The name of the start item on which the bid is placed.
	 */
	public boolean bid5(String playerName, String itemName)
	{

		DisplayBuffer.add("Invalid action in this game");
		return false;
	}

	/**
	 * The current player bids on a given start item.
	 * 
	 * @param playerName
	 *            The name of the current player (for checking purposes).
	 * @param itemName
	 *            The name of the start item on which the bid is placed.
	 * @param amount
	 *            The bid amount.
	 */
	public boolean bid(String playerName, String itemName, int amount)
	{

		DisplayBuffer.add(LocalText.getText("InvalidAction"));
		return false;
	}

	/**
	 * Define the next action to take after a start item is bought.
	 * 
	 */
	protected void setNextAction()
	{

		if (startPacket.areAllSold())
		{
			// No more start items: start a stock round
			nextStep = CLOSED;
			GameManager.getInstance().nextRound(this);
		}
		else
		{

			// Select the player that has the turn
			int newIndex = 0;
			if (++turns == numberOfPlayers)
			{
				cycle++;
				turns = 0;
			}
			if (startRoundNumber > 1)
			{
				newIndex = GameManager.getPriorityPlayer().getIndex();
			}
			else if (variant.equalsIgnoreCase(CLEMENS_VARIANT))
			{
				newIndex = cycle == 0 ? numberOfPlayers - 1 - turns : turns;
			}
			else if (variant.equalsIgnoreCase(SNAKE_VARIANT))
			{
				newIndex = cycle == 1 ? numberOfPlayers - 1 - turns : turns;
			}
			else
			{
				newIndex = turns;
			}
			Player oldPlayer = GameManager.getCurrentPlayer();
			GameManager.setCurrentPlayerIndex(newIndex);
			Player newPlayer = GameManager.getCurrentPlayer();
			log.debug ("Game turn has moved from "
					+ oldPlayer.getName()+" to "+newPlayer.getName()
					+" [startRound="+startRoundNumber
					+" cycle="+cycle
					+" turn="+turns
					+" newIndex="+newIndex
					+"]");

			nextStep = BUY_OR_PASS;
		}
		return;
	}

	/**
	 * Set an initial price for a President's share acquired
	 * in a Start Round.
	 * This action does not apply to 1835, where start prices are fixed.
	 * 
	 * @param playerName
	 *            The name of the par price setting player.
	 * @param companyName
	 *            The name of teh company for which a par price is set.
	 * @param parPrice
	 *            The par price.
	 */
	public boolean setPrice(String playerName, String companyName, int parPrice)
	{

		DisplayBuffer.add(LocalText.getText("InvalidAction"));
		return false;
	}

	/**
	 * Process a player's pass.
	 * 
	 * @param playerName
	 *            The name of the current player (for checking purposes).
	 */
	public boolean pass(String playerName)
	{

		String errMsg = null;
		Player player = GameManager.getCurrentPlayer();

		while (true)
		{

			// Check player
			if (!playerName.equals(player.getName()))
			{
				errMsg = LocalText.getText("WrongPlayer");
				break;
			}
			break;
		}

		if (errMsg != null)
		{
			DisplayBuffer.add(LocalText.getText("InvalidPass", new String[] {
					playerName,
					errMsg
				}));
			return false;
		}

		ReportBuffer.add(LocalText.getText("PASSES", playerName));
		//GameManager.setNextPlayer();
		setNextAction();

		if (++numPasses >= numPlayers)
		{
			// All players have passed.
			ReportBuffer.add(LocalText.getText("ALL_PASSED"));
			GameManager.getInstance().nextRound(this);
		}

		return true;
	}

	public boolean isBuyable(StartItem item)
	{
		return item.getStatus() == StartItem.BUYABLE;
	}

	public boolean isBiddable(StartItem item)
	{
		return false;
	}

	public String getHelp() {
	    return "1835 Start Round help text";
	}


}
