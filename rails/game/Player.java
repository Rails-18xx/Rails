/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Player.java,v 1.10 2007/12/21 21:18:12 evos Exp $ */
package rails.game;


import java.util.*;

import rails.game.model.CalculatedMoneyModel;
import rails.game.model.CashModel;
import rails.game.model.CertCountModel;
import rails.game.model.ModelObject;
import rails.game.model.MoneyModel;
import rails.util.LocalText;


/**
 * Player class holds all player-specific data
 */

public class Player implements CashHolder, Comparable<Player>
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

	private CertCountModel certCount = new CertCountModel (this);

	private MoneyModel blockedCash;
	private CalculatedMoneyModel freeCash; 
	private CalculatedMoneyModel worth;

	private boolean hasPriority = false;

	private boolean hasBoughtStockThisTurn = false;

	private Portfolio portfolio = null;

	private ArrayList<PublicCompanyI> companiesSoldThisTurn 
	    = new ArrayList<PublicCompanyI>();

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
	public static void initPlayers(List<Player> players)
	{
		int numberOfPlayers = players.size();
		int startCash = playerStartCash[numberOfPlayers];

		// Give each player the initial cash amount
		int index = 0;
		for (Player player : players)
		{
			player.index = index++;
			Bank.transferCash(null, player, startCash);
			ReportBuffer.add(LocalText.getText("PlayerIs", new String[] {
					String.valueOf(index),
					player.getName()
			}));
		}
		ReportBuffer.add(LocalText.getText("PlayerCash",
				Bank.format(startCash)));
		ReportBuffer.add(LocalText.getText("BankHas", 
				Bank.format(Bank.getInstance().getCash())));

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
	

	public static int getShareLimit() {
		return playerShareLimit;
	}

	public Player(String name)
	{
		this.name = name;
		portfolio = new Portfolio(name, this);
		freeCash = new CalculatedMoneyModel (this, "getFreeCash");
		wallet.addDependent(freeCash);
		blockedCash = new MoneyModel (name+"_blockedCash");
		blockedCash.setOption(MoneyModel.SUPPRESS_ZERO);
		worth = new CalculatedMoneyModel (this, "getWorth");
		wallet.addDependent(worth);
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
	 * company, given the "hold limit" per company, that is the percentage
	 * of shares of one company that a player may hold (typically 60%).
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
		if (portfolio.getShare(company) + number * company.getShareUnit() > playerShareLimit
		        && !company.getCurrentPrice().isNoHoldLimit()) return false;
		return true;
	}
	
	/**
	 * Return the number of <i>additional</i> shares of a certain company 
	 * and of a certain size that a player may buy, 
	 * given the share "hold limit" per company, that is the percentage 
	 * of shares of one company that a player may hold (typically 60%).
	 * <p>If no hold limit applies, it is taken to be 100%.
	 * @param company
	 *            The company from which to buy
	 * @param number
	 *            The share unit (typically 10%).
	 * @return The maximum number of such shares that would not
	 * let the player overrun the per-company share hold limit.
	 */
	public int maxAllowedNumberOfSharesToBuy (PublicCompanyI company,
			int shareSize) {
		
		int limit;
		if (!company.hasStarted()) {
			limit = playerShareLimit;
		} else {
			limit = company.getCurrentPrice().isNoHoldLimit() 
				? 100 
						: playerShareLimit;
		}
		return (limit - portfolio.getShare(company)) / shareSize;
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

	public ModelObject getCashModel()
	{
		return wallet;
	}

	public boolean addCash(int amount)
	{
		boolean result = wallet.addCash(amount);
		return result;
	}

	/**
	 * Get the player's total worth.
	 * 
	 * @return Total worth
	 */
	public int getWorth()
	{
		int worth = wallet.getCash();

		for (PublicCertificateI cert : portfolio.getCertificates())
		{
			worth += cert.getCertificatePrice();
		}
		for (PrivateCompanyI priv :  portfolio.getPrivateCompanies())
		{
			worth += priv.getBasePrice();
		}
		return worth;
	}

	public CalculatedMoneyModel getWorthModel()
	{
		return worth;
	}
	
	public CertCountModel getCertCountModel () {
	    return certCount;
	}
	
	public CalculatedMoneyModel getFreeCashModel() {
		return freeCash;
	}
	
	public MoneyModel getBlockedCashModel () {
		return blockedCash;
	}

	public String toString()
	{
		return name;
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
		if (amount > wallet.getCash() - blockedCash.intValue())
		{
			return false;
		}
		else
		{
			blockedCash.add (amount);
			freeCash.update();
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
		if (amount > blockedCash.intValue())
		{
			return false;
		}
		else
		{
			blockedCash.add (-amount);
			freeCash.update();
			return true;
		}
	}

	/**
	 * Return the unblocked cash (available for bidding)
	 * 
	 * @return
	 */
	public int getFreeCash()
	{
		return wallet.getCash() - blockedCash.intValue();
	}

	public int getBlockedCash()
	{
		return blockedCash.intValue();
	}

	public int getIndex()
	{
		return index;
	}
	
	/**
	 * Compare Players by their total worth, in descending order.
	 * This method implements the Comparable interface.
	 */
	public int compareTo (Player p) {
	    return - new Integer (getWorth()).compareTo(new Integer (p.getWorth()));
	}
}
