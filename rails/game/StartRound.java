package rails.game;

import java.util.*;

import rails.game.action.BuyOrBidStartItem;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.model.ModelObject;
import rails.game.move.MoveSet;
import rails.game.state.IntegerState;
import rails.game.state.State;
import rails.util.LocalText;


public abstract class StartRound extends Round implements StartRoundI
{

	protected StartPacket startPacket = null;
	protected int[] itemIndex;
	protected List<StartItem> itemsToSell = null;
	protected State auctionItemState = new State ("AuctionItem", StartItem.class);
	protected IntegerState numPasses = new IntegerState("StartRoundPasses");
	protected int numPlayers;
	protected String variant;
	
	/** Should the UI present bidding into and facilities?
	 * This value MUST be set in the actual StartRound constructor.
	 */
	protected boolean hasBidding; 

	/** A company in need for a par price. */
	PublicCompanyI companyNeedingPrice = null;

	/*----- Initialisation -----*/
	/**
	 * Will be created dynamically.
	 * 
	 */
	public StartRound()
	{
	}

	/**
	 * Start the start round.
	 * 
	 * @param startPacket
	 *            The startpacket to be sold in this start round.
	 */
	public void start(StartPacket startPacket)
	{

		this.startPacket = startPacket;
		this.variant = Game.getGameOption(GameManager.VARIANT_KEY);
        if (variant == null) variant = "";
		numPlayers = GameManager.getNumberOfPlayers();

		//itemMap = new HashMap();
		itemsToSell = new ArrayList<StartItem>();
		itemIndex = new int[startPacket.getItems().size()];
		int index = 0;
		
		//Iterator it = startPacket.getItems().iterator();
		//StartItem item;
		//while (it.hasNext())
		for (StartItem item : startPacket.getItems())
		{
			//item = (StartItem) it.next();
			
			// New: we only include items that have not yet been sold
			// at the start of the current StartRound
			if (!item.isSold()) {
				//itemMap.put(item.getName(), item);
				itemsToSell.add(item);
				itemIndex[index++] = item.getIndex();
			}
		}
		numPasses.set(0);
		auctionItemState.set(null);

		GameManager.getInstance().setRound(this);
		GameManager.setCurrentPlayerIndex(GameManager.getPriorityPlayer().getIndex());
		
		ReportBuffer.add("");
		ReportBuffer.add(LocalText.getText("StartOfInitialRound"));
		ReportBuffer.add (LocalText.getText("HasPriority", getCurrentPlayer().getName()));
	}
	

	public boolean process (PossibleAction action) {
		
		boolean result = false;
		
		log.debug("Processing action "+action);

		if (action instanceof NullAction) {
			
			String playerName = action.getPlayerName();
			NullAction nullAction = (NullAction) action;
			switch (nullAction.getMode()) {
			case NullAction.PASS:
				result = pass (playerName);
				break;
			}
		
		} else if (action instanceof BuyOrBidStartItem) {
			
			BuyOrBidStartItem startItemAction = (BuyOrBidStartItem) action;
			int status = startItemAction.getStatus();
			String playerName = action.getPlayerName();
			
			log.debug ("Item details: status="+status+" bid="+startItemAction.getActualBid());
			
			if (status == StartItem.BUYABLE) {
				if (startItemAction.hasSharePriceToSet()
						&& startItemAction.getSharePrice() == 0) {
					// We still need a share price for this item
					startItemAction.getStartItem().setStatus(StartItem.NEEDS_SHARE_PRICE);
                    // We must set the priority player, though
                    GameManager.setPriorityPlayer();
                    result = true;
				} else {
					result = buy (playerName, startItemAction);
				}
			} else if (status == StartItem.BIDDABLE) {
				result = bid (playerName, startItemAction);
			} else if (status == StartItem.AUCTIONED) {
				result = bid (playerName, startItemAction);
			} else if (status == StartItem.NEEDS_SHARE_PRICE) {
				result = buy (playerName, startItemAction);
			}
		} else {
		
			DisplayBuffer.add (LocalText.getText("UnexpectedAction",
				action.toString()));
		}
		
		if (startPacket.areAllSold()) {
			/* If the complete start packet has been sold, 
			 * start a Stock round, */
			possibleActions.clear();
			GameManager.getInstance().nextRound(this);
		} else if (!setPossibleActions()) {
			/* If nobody can do anything, keep executing 
			 * Operating and Start rounds until someone has got
			 * enough money to buy one of the remaining items.
			 * The game mechanism ensures that this will
			 * ultimately be possible.
			 */
			GameManager.getInstance().nextRound(this);
		}
		
		return result;
	}
	
	/*----- Processing player actions -----*/

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
	protected abstract boolean bid(String playerName, BuyOrBidStartItem startItem);

	/**
	 * Buy a start item against the base price.
	 * 
	 * @param playerName
	 *            Name of the buying player.
	 * @param itemName
	 *            Name of the bought start item.
	 * @param sharePrice If nonzero: share price if item contains a President's share
	 * @return False in case of any errors.
	 */

	protected boolean buy(String playerName, BuyOrBidStartItem boughtItem)
	{
		StartItem item = boughtItem.getStartItem();
		int status = boughtItem.getStatus();
		String errMsg = null;
		Player player = GameManager.getCurrentPlayer();
		int price = 0;
		int sharePrice = 0;
		String shareCompName = "";

		while (true)
		{

			// Is the item buyable?
			if (status == StartItem.BUYABLE 
					|| status == StartItem.NEEDS_SHARE_PRICE) {
				price = item.getBasePrice();
				if (item.getBid() > price) price = item.getBid();
			} else {
				errMsg = LocalText.getText("NotForSale");
				break;
			}
			
			if (status == StartItem.BUYABLE 
					&& player.getFreeCash() < price) {
				errMsg = LocalText.getText("NoMoney");
				break;
			}
			
			if (boughtItem.hasSharePriceToSet()) {
				shareCompName = boughtItem.getCompanyToSetPriceFor();
				sharePrice = boughtItem.getSharePrice();
				if (sharePrice == 0) {
					errMsg = LocalText.getText("NoSharePriceSet", shareCompName);
					break;
				}
				if ((StockMarket.getInstance().getStartSpace(sharePrice)) == null)
				{
					errMsg = LocalText.getText("InvalidStartPrice", new String[] {
							Bank.format(sharePrice),
							shareCompName
					});
					break;
				}
			}
			break;
		}

		if (errMsg != null)
		{
			DisplayBuffer.add (LocalText.getText("CantBuyItem", new String[] {
					playerName,
					item.getName(),
					errMsg
				}));
			return false;
		}
		
		MoveSet.start(false);
		
		assignItem(player, item, price, sharePrice);

		// Set priority
		if (status == StartItem.BUYABLE) {
			GameManager.setPriorityPlayer();
			setNextPlayer();
		} else if (status == StartItem.AUCTIONED) {
		    setPriorityPlayer();
		}
		
		auctionItemState.set (null);
		numPasses.set(0);

		
		return true;

	}

	/**
	 * This method executes the start item buy action.
	 * 
	 * @param player
	 *            Buying player.
	 * @param item
	 *            Start item being bought.
	 * @param price
	 *            Buy price.
	 */
	protected void assignItem(Player player, StartItem item, int price,
			int sharePrice)
	{
		Certificate primary = item.getPrimary();
		ReportBuffer.add(LocalText.getText("BuysItemFor", new String[] {
				player.getName(),
				primary.getName(),
				Bank.format(price)
			}));
		player.buy(primary, price);
		checksOnBuying(primary, sharePrice);
		if (item.hasSecondary())
		{
			Certificate extra = item.getSecondary();
			ReportBuffer.add(LocalText.getText("ALSO_GETS", new String[] {
					player.getName(),
					extra.getName()
				}));
			player.buy(extra, 0);
			checksOnBuying(extra, sharePrice);
		}
		item.setSold(player, price);
	}

	protected void checksOnBuying(Certificate cert, int sharePrice)
	{
		if (cert instanceof PublicCertificateI)
		{
			PublicCertificateI pubCert = (PublicCertificateI) cert;
			PublicCompanyI comp = pubCert.getCompany();
			// Start the company, look for a fixed start price
			if (!comp.hasStarted()) {
			    if (!comp.hasStockPrice()) {
			        comp.start();
			    } else if (pubCert.isPresidentShare()) {
					/* Company to be started. Check if it has a start price */
			    	if (sharePrice > 0) {
			    		// User has told us the start price
						comp.start(sharePrice);
					} else if (comp.getParPrice() != null) {
						// Company has a fixed start price
						comp.start();
					} else {
						log.error ("No start price for "+comp.getName());
					}
			    }
			}
			if (comp.hasStarted() && !comp.hasFloated()) {
				comp.checkFlotation();
			}

		}
	}

	/**
	 * Process a player's pass.
	 * 
	 * @param playerName
	 *            The name of the current player (for checking purposes).
	 */
	protected abstract boolean pass(String playerName);

	/*----- Setting up the UI for the next action -----*/

	/**
	 * Get the currentPlayer.
	 * 
	 * @return The current Player object.
	 * @see GameManager.getCurrentPlayer().
	 */
	public Player getCurrentPlayer()
	{
		return GameManager.getCurrentPlayer();
	}

	/**
	 * Get the currentPlayer index in the player list (starting at 0).
	 * 
	 * @return The index of the current Player.
	 * @see GameManager.getCurrentPlayerIndex().
	 */
	public int getCurrentPlayerIndex()
	{
		return GameManager.getCurrentPlayerIndex();
	}

	protected void setPriorityPlayer () {
		GameManager.setCurrentPlayer(GameManager.getPriorityPlayer());
	}
	
	protected void setPlayer(Player player) {
		GameManager.setCurrentPlayer(player);
	}
	
	protected void setNextPlayer () {
		GameManager.setCurrentPlayerIndex(GameManager.getCurrentPlayerIndex()+1);
	}

	/**
	 * Get a list of items that may be bought immediately.
	 * 
	 * @return An array of start items, possibly empry.
	 */
	
	public abstract List<StartItem> getStartItems ();
	
	/**
	 * Get a list of items that the current player may bid upon.
	 * 
	 * @return An array of start items, possibly empty.
	 */

	public StartPacket getStartPacket()
	{
		return startPacket;
	}

	public boolean hasBidding() {
		return hasBidding;
	}
	
	public ModelObject getBidModel (int privateIndex, int playerIndex) {
		return ((StartItem)itemsToSell.get(privateIndex)).getBidForPlayerModel(playerIndex);
	}
	
	public ModelObject getMinimumBidModel (int privateIndex) {
		return ((StartItem)itemsToSell.get(privateIndex)).getMinimumBidModel();
	}
	
	public ModelObject getFreeCashModel (int playerIndex) {
		return Game.getPlayerManager().getPlayerByIndex(playerIndex)
		        .getFreeCashModel();
	}

	public ModelObject getBlockedCashModel (int playerIndex) {
		return Game.getPlayerManager().getPlayerByIndex(playerIndex)
		        .getBlockedCashModel();
	}

}
