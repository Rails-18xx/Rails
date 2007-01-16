package game;

import java.util.*;

import util.LocalText;

public abstract class StartRound extends Round implements StartRoundI
{

	protected StartPacket startPacket = null;
	protected Map itemMap = null;
	protected List itemsToSell = null;
	protected StartItem auctionItem = null;
	protected int numPasses = 0;
	protected int numPlayers;
	protected String variant;
	protected int nextStep;
	protected int defaultStep;
	
	/** Should the UI present bidding into and facilities?
	 * This value MUST be set in the actual StartRound constructor.
	 */
	protected boolean hasBidding; 

	/*----- Start Round states -----*/
	/** The current player must buy, bid or pass */
	public static final int BID_BUY_OR_PASS = 0;
	/** The current player must set a par price */
	public static final int SET_PRICE = 1;
	/** The current player must buy or pass */
	public static final int BUY_OR_PASS = 2;
	/** The current player must buy (pass not allowed) */
	public static final int BUY = 3;
	/** The current player must bid or pass */
	public static final int BID_OR_PASS = 4;
	/** The start round is closed */
	public static final int CLOSED = 9;

	/** A company in need for a par price. */
	PublicCompanyI companyNeedingPrice = null;

	/** Enables UI to check if stock chart needs to be updated */
	boolean companyJustStarted = false;

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
		numPlayers = PlayerManager.getNumberOfPlayers();

		itemMap = new HashMap();
		itemsToSell = new ArrayList();
		Iterator it = startPacket.getItems().iterator();
		StartItem item;
		while (it.hasNext())
		{
			item = (StartItem) it.next();
			
			// New: we only include items that have not yet been sold
			// at the start of the current StartRound
			if (!item.isSold()) {
				itemMap.put(item.getName(), item);
				itemsToSell.add(item);
			}
		}

		GameManager.getInstance().setRound(this);
		GameManager.setCurrentPlayerIndex(GameManager.getPriorityPlayer().getIndex());
		ReportBuffer.add("");
		ReportBuffer.add(LocalText.getText("StartOfInitialRound"));
		ReportBuffer.add (LocalText.getText("HasPriority", getCurrentPlayer().getName()));
	}
	

	/*----- Processing player actions -----*/
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
	public abstract boolean bid5(String playerName, String itemName);

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
	public abstract boolean bid(String playerName, String itemName, int amount);

	/**
	 * Buy a start item against the base price.
	 * 
	 * @param playerName
	 *            Name of the buying player.
	 * @param itemName
	 *            Name of the bought start item.
	 * @return False in case of any errors.
	 */
	public boolean buy(String playerName, String itemName)
	{
		StartItem item = null;
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
			// Check name of item
			if (!itemMap.containsKey(itemName))
			{
				errMsg = LocalText.getText("DoesNotExist");
				break;
			}

			item = (StartItem) itemMap.get(itemName);

			// Is the item buyable?
			if (!isBuyable(item))
			{
				errMsg = LocalText.getText("NotForSale");
				break;
			}
			
			if (player.getUnblockedCash() < item.getBasePrice()) {
				errMsg = LocalText.getText("NoMoney");
				break;
			}

			break;
		}

		if (errMsg != null)
		{
			DisplayBuffer.add (LocalText.getText("CantBuyItem", new String[] {
					playerName,
					itemName,
					errMsg
				}));
			return false;
		}

		assignItem(player, item, item.getBasePrice());

		// Set priority
		GameManager.setPriorityPlayer();
		numPasses = 0;

		// Next action
		setNextAction();
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
	protected void assignItem(Player player, StartItem item, int price)
	{

		Certificate primary = item.getPrimary();
		ReportBuffer.add(LocalText.getText("BuysItemFor", new String[] {
				player.getName(),
				primary.getName(),
				Bank.format(price)
			}));
		player.buy(primary, price);
		checksOnBuying(primary);
		if (item.hasSecondary())
		{
			Certificate extra = item.getSecondary();
			ReportBuffer.add(LocalText.getText("ALSO_GETS", new String[] {
					player.getName(),
					extra.getName()
				}));
			player.buy(extra, 0);
			checksOnBuying(extra);
		}
		item.setSold(price);
	}

	protected void checksOnBuying(Certificate cert)
	{
		if (cert instanceof PublicCertificateI)
		{
			PublicCertificateI pubCert = (PublicCertificateI) cert;
			PublicCompanyI comp = pubCert.getCompany();
			// Start the company, look for a fixed start price
			if (!comp.hasStarted()) {
			    if (!comp.hasStockPrice()) {
			        comp.start();
					companyJustStarted = true;
					comp.checkFlotation();
			    } else if (pubCert.isPresidentShare()) {
					companyJustStarted = true;
					// If there is no start price, we need to get one
					if (comp.getParPrice() == null) {
						companyNeedingPrice = comp;
						nextStep = SET_PRICE;
					} else {
						comp.start();
						comp.checkFlotation();
					}
			    }
			} else {
				comp.checkFlotation();
			}

			// Check if the company has floated (also applies to minors)
			// Redundant?? Cannot be done here when buying SFSL presidency
			// from start packet in 1870.
			//comp.checkFlotation();
		}
	}

	/**
	 * Set a public company initial share price.
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

		String errMsg = null;
		Player player = GameManager.getCurrentPlayer();
		StockSpaceI startSpace = null;

		while (true)
		{

			// Check player
			if (!playerName.equals(player.getName()))
			{
				errMsg = LocalText.getText("WrongPlayer");
				break;
			}
			// Check state
			if (nextStep != SET_PRICE)
			{
				errMsg = LocalText.getText("NoPriceToSet");
				break;
			}
			// Check company
			if (!companyName.equals(companyNeedingPrice.getName()))
			{
				errMsg = LocalText.getText("WrongCompany");
				break;
			}
			// Check par price
			if ((startSpace = StockMarket.getInstance().getStartSpace(parPrice)) == null)
			{
				errMsg = LocalText.getText("InvalidStartPrice");
				break;
			}
			break;
		}

		if (errMsg != null)
		{
			DisplayBuffer.add(LocalText.getText("InvalidParPriceSetting", new String[] {
					Bank.format(parPrice),
					playerName,
					companyName,
					errMsg
				}));
			return false;
		}

		ReportBuffer.add(LocalText.getText("START_COMPANY", new String[] {
				playerName,
				companyName,
				Bank.format(parPrice)
			}));
		companyNeedingPrice.start(startSpace);

		// Check if company already floats
		if (!companyNeedingPrice.hasFloated()
				&& Bank.getIpo().ownsShare(companyNeedingPrice) <= (100 - companyNeedingPrice.getFloatPercentage()))
		{
			// Float company
			companyNeedingPrice.setFloated();
			//ReportBuffer.add(LocalText.getText("FLOATS", new String[] {
			//		companyName,
			//		Bank.format(companyNeedingPrice.getCash())
			//	}));
		}

		companyNeedingPrice = null;
		nextStep = defaultStep;

		setNextAction();

		return true;
	}

	/**
	 * Process a player's pass.
	 * 
	 * @param playerName
	 *            The name of the current player (for checking purposes).
	 */
	public abstract boolean pass(String playerName);

	protected abstract void setNextAction();

	/*----- Setting up the UI for the next action -----*/
	/**
	 * Return the StartRound state, i.e. which action is next?
	 * 
	 * @return The next step number.
	 */
	public int nextStep()
	{
		return nextStep;
	}

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

	/**
	 * Get a list of items that may be bought immediately.
	 * 
	 * @return An array of start items, possibly empry.
	 */
	public abstract StartItem[] getBuyableItems();
	
	public abstract List getStartItems ();
	
	public StartItem getAuctionedItem() {
		return auctionItem;
	}

	/**
	 * Get a list of items that the current player may bid upon.
	 * 
	 * @return An array of start items, possibly empty.
	 */
	public abstract StartItem[] getBiddableItems();

	/**
	 * Get the company for which a par price must be set in the SET_PRICE state.
	 * <p>
	 * The default implementation returns null, but subclasses may override this
	 * in cases where President certificates are sold in a Start Round.
	 * 
	 * @return A PublicCompany object, or null.
	 */
	public PublicCompanyI getCompanyNeedingPrice()
	{
		return null;
	}

	public StartPacket getStartPacket()
	{
		return startPacket;
	}

	/**
	 * Check if a given item can be bought immediately by the current player.
	 * 
	 * @param item
	 *            The start item to check.
	 * @return True or false.
	 */
	public abstract boolean isBuyable(StartItem item);

	/**
	 * Check if a given item may be be bis upon by the current player.
	 * 
	 * @param item
	 *            The start item to check.
	 * @return True or false.
	 */
	public abstract boolean isBiddable(StartItem item);

	public boolean hasCompanyJustStarted()
	{
		return companyJustStarted;
	}

	public void resetCompanyJustStarted()
	{
		companyJustStarted = false;
	}
	
	public List getSpecialProperties () {
	    return null;
	}

	public boolean hasBidding() {
		return hasBidding;
	}

}
