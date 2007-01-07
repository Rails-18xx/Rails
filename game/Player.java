package game;

import game.model.CashModel;
import game.model.CertCountModel;
import game.model.ModelObject;
import game.model.WorthModel;

import java.util.*;

/**
 * Player class holds all player-specific data
 */

public class Player implements CashHolder, Comparable
{

	/** Default limit to percentage of a company a player may hold */
	private static final int DEFAULT_PLAYER_SHARE_LIMIT = 60;

	public static int MAX_PLAYERS = 8;

	public static int MIN_PLAYERS = 2;

	private static int[] playerStartCash = new int[MAX_PLAYERS];

	private static int[] playerCertificateLimits = new int[MAX_PLAYERS];

	private static int playerCertificateLimit = 0;

	private static int playerShareLimit = DEFAULT_PLAYER_SHARE_LIMIT;
	// May need to become an array

	private String name = "";

	private int index = 0;

	private CashModel wallet = new CashModel(this);

	private WorthModel worth = new WorthModel(this);
	
	private CertCountModel certCount = new CertCountModel (this);

	private int blockedCash = 0;

	private boolean hasPriority = false;

	private boolean hasBoughtStockThisTurn = false;

	private Portfolio portfolio = null;

	private ArrayList companiesSoldThisTurn = new ArrayList();

	public static void setLimits(int number, int cash, int certLimit)
	{
		if (number > 1 && number <= MAX_PLAYERS)
		{
			playerStartCash[number] = cash;
			playerCertificateLimits[number] = certLimit;
		}
	}

	/**
	 * Initialises each Player's parameters which depend on the number of
	 * players. To be called when all Players have been added.
	 * 
	 */
	public static void initPlayers(Player[] players)
	{
		Player player;
		int numberOfPlayers = players.length;
		int startCash = playerStartCash[numberOfPlayers];

		// Give each player the initial cash amount
		for (int i = 0; i < numberOfPlayers; i++)
		{
			player = (Player) players[i];
			player.index = i;
			Bank.transferCash(null, player, startCash);
			LogBuffer.add("Player " + player.getName() + " receives "
					+ Bank.format(startCash) + ". Bank now has "
					+ Bank.getInstance().getFormattedCash());
		}

		// Set the sertificate limit
		playerCertificateLimit = playerCertificateLimits[numberOfPlayers];
	}

	/**
	 * @return Certificate Limit for Players
	 */
	public static int getCertLimit()
	{
		return playerCertificateLimit;
	}

	public static void setShareLimit(int percentage)
	{
		playerShareLimit = percentage;
	}

	public Player(String name)
	{
		this.name = name;
		portfolio = new Portfolio(name, this);
	}

	/**
	 * @param share
	 * @throws NullPointerException
	 *             if company hasn't started yet. UI needs to handle this.
	 */
	public void buyShare(PublicCertificate share, int price)
			throws NullPointerException
	{
		if (hasBoughtStockThisTurn)
			return;

		for (int i = 0; i < companiesSoldThisTurn.size(); i++)
		{
			if (share.company.getName()
					.equalsIgnoreCase(companiesSoldThisTurn.get(i).toString()))
				return;
		}

		if (portfolio.getCertificates().size() >= playerCertificateLimit)
			return;

		try
		{
			// throws nullpointer if company hasn't started yet.
			// it's up to the UI to catch this and gracefully start the company.
			getPortfolio().buyCertificate(share, share.getPortfolio(), price);
		}
		catch (NullPointerException e)
		{
			throw e;
		}

		Game.getPlayerManager().setBoughtStockLast(this);
		hasBoughtStockThisTurn = true;
	}

	public void buyShare(PublicCertificate share) throws NullPointerException
	{
		try
		{
			buyShare(share, share.getCompany().getCurrentPrice().getPrice());
		}
		catch (NullPointerException e)
		{
			throw e;
		}
	}

	/**
	 * Check if a player may buy the given number of certificates.
	 * 
	 * @param number
	 *            Number of certificates to buy (usually 1 but not always so).
	 * @return True if it is allowed.
	 */
	public boolean mayBuyCertificate(PublicCompanyI comp, int number)
	{
	    if (comp.hasFloated() && comp.getCurrentPrice().isNoCertLimit()) return true;
		if (portfolio.getNumberOfCountedCertificates() + number > playerCertificateLimit)
			return false;
		return true;
	}

	/**
	 * Check if a player may buy the given number of shares from a given
	 * company.
	 * 
	 * @param company
	 *            The company from which to buy
	 * @param number
	 *            The number of shares (usually 1 but not always so).
	 * @return True if it is allowed.
	 */
	public boolean mayBuyCompanyShare(PublicCompanyI company, int number)
	{
	    // Check for per-company share limit
		if (portfolio.ownsShare(company) + number * company.getShareUnit() > playerShareLimit
		        && !company.getCurrentPrice().isNoHoldLimit()) return false;
		return true;
	}

	/**
	 * Front-end method for buying any kind of certificate from anyone.
	 * 
	 * @param cert
	 *            PrivateCompany or PublicCertificate.
	 * @param from
	 *            Portfolio of seller.
	 * @param price
	 *            Price.
	 */
	public void buy(Certificate cert, int price)
	{

		if (cert instanceof PrivateCompanyI)
		{
			portfolio.buyPrivate((PrivateCompanyI) cert,
					cert.getPortfolio(),
					price);
		}
		else if (cert instanceof PublicCertificateI)
		{
			Portfolio from = cert.getPortfolio();
			portfolio.buyCertificate((PublicCertificateI) cert, from, price);
			((PublicCertificateI) cert).getCompany().checkPresidencyOnBuy(this);
		}
	}

	public int sellShare(PublicCertificate share)
	{
		Portfolio.sellCertificate(share, portfolio, share.getCompany()
				.getCurrentPrice()
				.getPrice());
		Game.getStockMarket().sell(share.getCompany(), 1);
		return 1;
	}

	/**
	 * @return Returns if the Player hasPriority.
	 */
	public boolean hasPriority()
	{
		return hasPriority;
	}

	/**
	 * @param hasPriority
	 *            The hasPriority to set.
	 */
	public void setHasPriority(boolean hasPriority)
	{
		this.hasPriority = hasPriority;
	}

	/**
	 * @return Returns the player's portfolio.
	 */
	public Portfolio getPortfolio()
	{
		return portfolio;
	}

	/**
	 * @return Returns the player's name.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return Returns the player's wallet.
	 */
	public int getCash()
	{
		return wallet.getCash();
	}

	public String getFormattedCash()
	{
		return wallet.toString();
	}

	public ModelObject getCashModel()
	{
		return wallet;
	}

	public boolean addCash(int amount)
	{
		return wallet.addCash(amount);
	}

	/**
	 * Get the player's total worth.
	 * 
	 * @return Total worth
	 */
	public int getWorth()
	{
		int worth = wallet.getCash();
		Iterator it = portfolio.getCertificates().iterator();
		while (it.hasNext())
		{
			worth += ((PublicCertificateI) it.next()).getCertificatePrice();
		}
		it = portfolio.getPrivateCompanies().iterator();
		while (it.hasNext())
		{
			worth += ((PrivateCompanyI) it.next()).getBasePrice();
		}
		return worth;
	}

	public String getFormattedWorth()
	{
		return Bank.format(getWorth());
	}

	public WorthModel getWorthModel()
	{
		return worth;
	}
	
	public CertCountModel getCertCountModel () {
	    return certCount;
	}

	public String toString()
	{
		return "Name: " + name + " Cash: " + wallet;
	}

	/**
	 * @return Returns the hasBoughtStockThisTurn.
	 */
	public boolean hasBoughtStockThisTurn()
	{
		return hasBoughtStockThisTurn;
	}

	/**
	 * Block cash allocated by a bid.
	 * 
	 * @param amount
	 *            Amount of cash to be blocked.
	 * @return false if the amount was not available.
	 */
	public boolean blockCash(int amount)
	{
		if (amount > wallet.getCash() - blockedCash)
		{
			return false;
		}
		else
		{
			blockedCash += amount;
			return true;
		}
	}

	/**
	 * Unblock cash.
	 * 
	 * @param amount
	 *            Amount to be unblocked.
	 * @return false if the given amount was not blocked.
	 */
	public boolean unblockCash(int amount)
	{
		if (amount > blockedCash)
		{
			return false;
		}
		else
		{
			blockedCash -= amount;
			return true;
		}
	}

	/**
	 * Unblock all blocked cash.
	 * 
	 * @return Always true.
	 */
	public boolean unblockCash()
	{
		blockedCash = 0;
		return true;
	}

	/**
	 * Return the unblocked cash (available for bidding)
	 * 
	 * @return
	 */
	public int getUnblockedCash()
	{
		return wallet.getCash() - blockedCash;
	}

	public int getBlockedCash()
	{
		return blockedCash;
	}

	public int getIndex()
	{
		return index;
	}
	
	/**
	 * Compare Players by their total worth, in descending order.
	 * This method implements the Comparable interface.
	 */
	public int compareTo (Object p) {
	    return - new Integer (getWorth()).compareTo(new Integer (((Player)p).getWorth()));
	}
}
