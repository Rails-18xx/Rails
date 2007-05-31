package rails.game;

import java.util.*;

import rails.game.action.BuyOrBidStartItem;
import rails.game.action.NullAction;
import rails.game.move.MoveSet;
import rails.game.state.IntegerState;
import rails.util.LocalText;


/**
 * Implements an 1835-style startpacket sale.
 */
public class StartRound_1835 extends StartRound
{

	/* To control the player sequence in the Clemens and Snake variants */
	private static IntegerState turn
		= new IntegerState ("TurnNumber", 0);

	private static IntegerState startRoundNumber
		= new IntegerState ("StartRoundNumber" , 0);

	private int numberOfPlayers = GameManager.getNumberOfPlayers();
	//private String variant;

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
		startRoundNumber.add(1);
		//variant = GameManager.getVariant();

		// Select first player
		//if (variant.equalsIgnoreCase(CLEMENS_VARIANT))
		//{
		//	GameManager.setCurrentPlayerIndex(numberOfPlayers - 1);
		//}
		//else
		//{
		//	GameManager.setCurrentPlayerIndex(0);
		//}

		if (!setPossibleActions()) {
			/* If nobody can do anything, keep executing 
			 * Operating and Start rounds until someone has got
			 * enough money to buy one of the remaining items.
			 * The game mechanism ensures that this will
			 * ultimately be possible.
			 */
			//possibleActions.add (new NullAction (NullAction.CLOSE));
			GameManager.getInstance().nextRound(this);
		}
		
	}

	/**
	 * Get a list of items that may be bought immediately.
	 * <p>
	 * In an 1835-style auction this method will usually return several items.
	 * 
	 * @return An array of start items that can be bought.
	 */
	//public StartItem[] getBuyableItems() {return null;}
	
	public boolean setPossibleActions() {
		
		List startItems = startPacket.getItems();
		StartItem item;
		int row;
		boolean buyable;
		int items = 0;
		int minRow = 0;
		
		/* First, mark which items are buyable.
		 * Once buyable, they always remain so until bought,
		 * so there is no need to check is an item is still buyable.
		 */ 
		Iterator it = startItems.iterator();
		while (it.hasNext())
		{
			item = (StartItem) it.next();
			buyable = false;
			
			if (item.isSold()) {
				// Already sold: skip
			} else if (variant.equalsIgnoreCase(CLEMENS_VARIANT)) {
				buyable = true;
			} else {
				row = item.getRow();
				if (minRow == 0)
					minRow = row;
				if (row == minRow)
				{
					// Allow all items in the top row.
					buyable = true;
					items++;
				}
				else if (row == minRow + 1 && items == 1)
				{
					// Allow the first item in the next row if the
					// top row has only one item.
					buyable = true;
				}
			}
			if (buyable) {
				item.setStatus (StartItem.BUYABLE);
				//log.debug("Item "+item.getName()+" is buyable");
			//} else {
				//log.debug("Item "+item.getName()+" is NOT buyable");
			}
		}
		possibleActions.clear();
		
		/* Repeat until we have found a player with enough money 
		 * to buy some item */ 
		while (possibleActions.isEmpty()) {
		
			Player currentPlayer = getCurrentPlayer();
			int cashToSpend = currentPlayer.getCash();
			
			it = startItems.iterator();
			while (it.hasNext())
			{
				item = (StartItem) it.next();
				
				if (item.getStatus() == StartItem.BUYABLE) {
					if (item.getBasePrice() <= cashToSpend) {
						/* Player does have the cash */
						possibleActions.add(new BuyOrBidStartItem (
								item,
								item.getBasePrice(),
								item.getStatus()));
						//log.debug("For player "+currentPlayer.getName()+": item "+item.getName()+" is buyable (price="+item.getBasePrice()+" cash="+cashToSpend+")");
					//} else {
						//log.debug("For player "+currentPlayer.getName()+": item "+item.getName()+" is NOT buyable (price="+item.getBasePrice()+" cash="+cashToSpend+")");
					}
				}
			}

			if (possibleActions.isEmpty()) {
				String message = LocalText.getText("CannotBuyAnything",
						currentPlayer.getName());
				ReportBuffer.add(message);
				DisplayBuffer.add (message);
				numPasses.add(1);
				if (numPasses.intValue() == numberOfPlayers) {
					/* No-one has enough cash left to buy anything,
					 * so close the Start Round. */
					return false;
				}
				setNextPlayer();
			}
		}
		
		/* Pass is always allowed */
		possibleActions.add (new NullAction (NullAction.PASS));
		
		return true;
	}
	
	public List<StartItem> getStartItems()
	{
		Player currentPlayer = GameManager.getCurrentPlayer();
		int cashToSpend = currentPlayer.getCash();
		List<StartItem> startItems = startPacket.getItems();
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

	/*----- MoveSet methods -----*/

	public boolean bid(String playerName, BuyOrBidStartItem item)
	{

		DisplayBuffer.add(LocalText.getText("InvalidAction"));
		return false;
	}

	/**
	 * Set the next player turn.
	 * 
	 */
	protected void setNextPlayer()
	{

		/* Select the player that has the turn.*/
		
		if (startRoundNumber.intValue() == 1) {
			/* Some variants have a reversed player order in the first 
			 * or second cycle of the first round
			 * (a cycle spans one turn of all players).
			 * In such a case we need to keep track of 
			 * the number of player turns.  
			 */
			turn.add (1);
			int turnNumber = turn.intValue();
			int cycleNumber = turnNumber / numberOfPlayers;
			int turnIndex = turnNumber % numberOfPlayers;
			int newIndex;
		
			if (variant.equalsIgnoreCase(CLEMENS_VARIANT))
			{
				/* Reverse ordee in the first cycle only */
				newIndex = cycleNumber == 0 ? numberOfPlayers - 1 - turnIndex : turnIndex;
			}
			else if (variant.equalsIgnoreCase(SNAKE_VARIANT))
			{
				/* Reverse order in the second cycle only */
				newIndex = cycleNumber == 1 ? numberOfPlayers - 1 - turnIndex : turnIndex;
			}
			else
			{
				newIndex = turnIndex;
			}
			Player oldPlayer = GameManager.getCurrentPlayer();
			GameManager.setCurrentPlayerIndex(newIndex);
			Player newPlayer = GameManager.getCurrentPlayer();
			log.debug ("Game turn has moved from "
					+ oldPlayer.getName()+" to "+newPlayer.getName()
					+" [startRound="+startRoundNumber
					+" cycle="+cycleNumber
					+" turn="+turnNumber
					+" newIndex="+newIndex
						+"]");
			
		} else {
			
			/* In any subsequent Round, the normal order applies. */
			Player oldPlayer = GameManager.getCurrentPlayer();
			super.setNextPlayer();
			Player newPlayer = GameManager.getCurrentPlayer();
			log.debug ("Game turn has moved from "
					+ oldPlayer.getName()+" to "+newPlayer.getName());
		}

		return;
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
				errMsg = LocalText.getText("WrongPlayer", playerName);
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
		
		MoveSet.start();

		numPasses.add(1);
		
		if (numPasses.intValue() >= numPlayers)
		{
			// All players have passed.
			ReportBuffer.add(LocalText.getText("ALL_PASSED"));
			numPasses.set(0);
			GameManager.getInstance().nextRound(this);
		} else {
			setNextPlayer();
		}

		return true;
	}

	public String getHelp() {
	    return "1835 Start Round help text";
	}


}
