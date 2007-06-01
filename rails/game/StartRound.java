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
	//protected Map itemMap = null;
	protected int[] itemIndex;
	protected List<StartItem> itemsToSell = null;
	protected State auctionItemState = new State ("AuctionItem", StartItem.class);
	protected IntegerState numPasses = new IntegerState("StartRoundPasses");
	protected int numPlayers;
	protected String variant;
	//protected int nextStep;
	//protected int defaultStep;
	
	/** Should the UI present bidding into and facilities?
	 * This value MUST be set in the actual StartRound constructor.
	 */
	protected boolean hasBidding; 

	/*----- Start Round states -----*/
	/** The current player must buy, bid or pass */
	//public static final int BID_BUY_OR_PASS = 0;
	/** The current player must set a par price */
	//public static final int SET_PRICE = 1;
	/** The current player must buy or pass */
	//public static final int BUY_OR_PASS = 2;
	/** The current player must buy (pass not allowed) */
	//public static final int BUY = 3;
	/** The current player must bid or pass */
	//public static final int BID_OR_PASS = 4;
	/** The start round is closed */
	//public static final int CLOSED = 9;

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
		this.variant = GameManager.getVariant();
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
			case NullAction.UNDO:
				MoveSet.undo();
				result = true;
				break;
			case NullAction.REDO:
				MoveSet.redo();
				result = true;
				break;
			case NullAction.CLOSE:
				numPasses.set(0);
				GameManager.getInstance().nextRound(this);
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
					/* We still need a share price for this item */
					startItemAction.getStartItem().setStatus(StartItem.NEEDS_SHARE_PRICE);
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
		
		if (MoveSet.isOpen()) MoveSet.finish();
		
		if (MoveSet.isUndoable()) {
			possibleActions.add (new NullAction (NullAction.UNDO));
		}
		if (MoveSet.isRedoable()) {
			possibleActions.add(new NullAction (NullAction.REDO));
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

			// Check player
			if (!playerName.equals(player.getName()))
			{
				errMsg = LocalText.getText("WrongPlayer", playerName);
				break;
			}
			// Check item
			boolean validItem = false;
			// TODO It seems this loop can't be turned into a for/in one
			for (Iterator it = possibleActions.getType(BuyOrBidStartItem.class).iterator();
					it.hasNext();) { 
				BuyOrBidStartItem activeItem = (BuyOrBidStartItem) it.next();
				if (boughtItem.equals(activeItem)) {
					validItem = true;
					break;
				}
				
			}
			if (!validItem)
			{
				errMsg = LocalText.getText("ActionNotAllowed", boughtItem.toString());
				break;
			}

			// Is the item buyable?
			if (status == StartItem.BUYABLE 
					|| status == StartItem.NEEDS_SHARE_PRICE) {
				price = item.getBasePrice();
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
		
		MoveSet.start();
		
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
	 * Return the StartRound state, i.e. which action is next?
	 * 
	 * @return The next step number.
	 */
	//public int nextStep()
	//{
	//	return nextStep;
	//}

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
	//public abstract StartItem[] getBuyableItems();
	
	public abstract List<StartItem> getStartItems ();
	
	////public StartItem getAuctionedItem() {
	//	return auctionItem;
	//}
	
	protected abstract boolean setPossibleActions();

	/**
	 * Get a list of items that the current player may bid upon.
	 * 
	 * @return An array of start items, possibly empty.
	 */
	//public abstract StartItem[] getBiddableItems();

	public StartPacket getStartPacket()
	{
		return startPacket;
	}

	//public List getSpecialProperties () {
	//    return null;
	//}

	public boolean hasBidding() {
		return hasBidding;
	}
	
	//public int getItemIndex(int i) {
	//	if (i >= 0 && i < itemIndex.length) {
	//		return itemIndex[i];
	//	} else {
	//		return -1;
	//	}
	//}
	
	public ModelObject getBidModel (int privateIndex, int playerIndex) {
		//log.debug("Asking BidModel for private="+privateIndex+", player="+playerIndex+", itemsToSell.size="+itemsToSell.size());
		//if (itemsToSell.size() <= privateIndex) {
		//	for (Iterator it = itemsToSell.iterator(); it.hasNext(); ) {
		//		log.debug("  Item="+((StartItem)it.next()).getName());
		//	}
		//}
		return ((StartItem)itemsToSell.get(privateIndex)).getBidForPlayerModel(playerIndex);
	}
	
	public ModelObject getMinimumBidModel (int privateIndex) {
		return ((StartItem)itemsToSell.get(privateIndex)).getMinimumBidModel();
	}
	
	public ModelObject getFreeCashModel (int playerIndex) {
		return Game.getPlayerManager().getPlayersArray()[playerIndex]
		        .getFreeCashModel();
	}

	public ModelObject getBlockedCashModel (int playerIndex) {
		return Game.getPlayerManager().getPlayersArray()[playerIndex]
		        .getBlockedCashModel();
	}

}
