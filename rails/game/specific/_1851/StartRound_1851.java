/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_1851/StartRound_1851.java,v 1.2 2008/01/27 15:23:44 evos Exp $ */
package rails.game.specific._1851;

import java.util.*;

import rails.game.*;
import rails.game.action.BuyOrBidStartItem;
import rails.util.LocalText;


/**
 * Implements an 1835-style startpacket sale.
 */
public class StartRound_1851 extends StartRound
{

	/**
	 * Constructor, only to be used in dynamic instantiation.
	 */
	public StartRound_1851()
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

		if (!setPossibleActions()) {
			/* If nobody can do anything, keep executing 
			 * Operating and Start rounds until someone has got
			 * enough money to buy one of the remaining items.
			 * The game mechanism ensures that this will
			 * ultimately be possible.
			 */
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
	
	public boolean setPossibleActions() {
		
		BuyOrBidStartItem action;
		List<StartItem> startItems = startPacket.getItems();
		
		possibleActions.clear();

		for (StartItem item : startItems)
		{
			if (!item.isSold()) {
				item.setStatus (StartItem.BUYABLE);
				possibleActions.add(action = new BuyOrBidStartItem (
						item,
						item.getBasePrice()));
				log.debug(GameManager.getCurrentPlayer().getName()+" may: "+action.toString());
			}

		}
		
		/* Pass is not allowed */
		
		return true;
	}
	
	public List<StartItem> getStartItems()
	{
		Player currentPlayer = GameManager.getCurrentPlayer();
		int cashToSpend = currentPlayer.getCash();
		List<StartItem> startItems = startPacket.getItems();
		
		for (StartItem item : startItems)
		{
			if (item.isSold()) {
				item.setStatus (StartItem.SOLD);
			} else if (item.getBasePrice() > cashToSpend) {
				item.setStatus (StartItem.UNAVAILABLE);
			} else {
				item.setStatus (StartItem.BUYABLE);
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
	 * Process a player's pass.
	 * 
	 * @param playerName
	 *            The name of the current player (for checking purposes).
	 */
	public boolean pass(String playerName)
	{
		log.error ("Unexcpected pass");
		return false;
	}

	public String getHelp() {
	    return "1851 Start Round help text";
	}


}
