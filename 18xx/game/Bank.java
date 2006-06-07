package game;

import game.model.CashModel;
import game.model.ModelObject;

import java.util.*;
import org.w3c.dom.*;
import util.XmlUtils;

public class Bank implements CashHolder, ConfigurableComponentI
{

	/** Default limit of shares in the bank pool */
	private static final int DEFAULT_POOL_SHARE_LIMIT = 50;

	/** The Bank's amont of cash */
	private static CashModel money;

	private static int gameType;

	/** The IPO */
	private static Portfolio ipo = null;
	/** The Bank Pool */
	private static Portfolio pool = null;
	/** Collection of items that will (may) become available in the future */
	private static Portfolio unavailable = null;
	/** Collection of items that have bene discarded (but are kept to allow Undo) */
	private static Portfolio scrapHeap = null;

	private static Bank instance = null;

	/** Is the bank broken (remains true once set) */
	private static boolean broken = false;
	/** Is the bank just broken (returns true exactly once) */
	private static boolean brokenReported = false;

	/**
	 * The money format template. '@' is replsced by the amount, the rest is
	 * copied.
	 */
	private static String moneyFormat = "$@";

	private static int poolShareLimit = DEFAULT_POOL_SHARE_LIMIT;

	/**
	 * @return an instance of the Bank object
	 */
	public static Bank getInstance()
	{
		return instance;
	}

	/**
	 * Central method for transferring all cash.
	 * 
	 * @param from
	 *            Who pays the money (null = Bank).
	 * @param to
	 *            Who received the money (null = Bank).
	 * @param amount
	 *            The amount of money.
	 */
	public static boolean transferCash(CashHolder from, CashHolder to,
			int amount)
	{
		if (from == null)
			from = instance;
		else if (to == null)
			to = instance;
		to.addCash(amount);
		return from.addCash(-amount);
	}

	public Bank()
	{

		instance = this;
		money = new CashModel(this);
		// Create the IPO and the Bank Pool.
		// Here the Pool pays out, but that should be made configurable.
		ipo = new Portfolio("IPO", this, false);
		pool = new Portfolio("Pool", this, true);
		unavailable = new Portfolio("Unavailable", this, false);
		scrapHeap = new Portfolio("ScrapHeap", this, false);

	}

	/**
	 * @see game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
	 */
	public void configureFromXML(Element element) throws ConfigurationException
	{
		NamedNodeMap nnp;
		int number, startCash, certLimit;
		int maxNumber = 0;

		// Parse the Bank element
		Element node = (Element) element.getElementsByTagName("Bank").item(0);
		if (node != null)
		{
			nnp = node.getAttributes();
			money.setCash(XmlUtils.extractIntegerAttribute(nnp, "amount", 12000));
		}
		Log.write("Bank size is " + money.getCash());

		NodeList players = element.getElementsByTagName("Players");
		for (int i = 0; i < players.getLength(); i++)
		{
			nnp = ((Element) players.item(i)).getAttributes();
			number = XmlUtils.extractIntegerAttribute(nnp, "number");
			startCash = XmlUtils.extractIntegerAttribute(nnp, "cash");
			certLimit = XmlUtils.extractIntegerAttribute(nnp, "certLimit");

			Player.setLimits(number, startCash, certLimit);

			if (i == 0)
			{
				Player.MIN_PLAYERS = number;
				System.out.println("MIN_PLAYERS: " + Player.MIN_PLAYERS);
			}

			if (i == players.getLength() - 1)
			{
				Player.MAX_PLAYERS = number;
				System.out.println("MAX_PLAYERS: " + Player.MAX_PLAYERS);
			}
		}

		node = (Element) element.getElementsByTagName("PoolLimit").item(0);
		if (node != null)
		{
			nnp = node.getAttributes();
			poolShareLimit = XmlUtils.extractIntegerAttribute(nnp,
					"percentage",
					DEFAULT_POOL_SHARE_LIMIT);
		}

		node = (Element) element.getElementsByTagName("Money").item(0);
		if (node != null)
		{
			nnp = node.getAttributes();
			moneyFormat = XmlUtils.extractStringAttribute(nnp, "format", "$@");
		}

	}

	/**
	 * @param percentage
	 *            of a company allowed to be in the Bank pool.
	 */
	public static void setPoolShareLimit(int percentage)
	{
		poolShareLimit = percentage;
	}

	/**
	 * Put all available certificates in the IPO
	 */
	public static void initIpo()
	{
		// Add privates
		List privates = Game.getCompanyManager().getAllPrivateCompanies();
		Iterator it = privates.iterator();
		PrivateCompanyI priv;
		while (it.hasNext())
		{
			ipo.addPrivate(priv = (PrivateCompanyI) it.next());
		}

		// Add public companies
		List companies = Game.getCompanyManager().getAllPublicCompanies();
		it = companies.iterator();
		PublicCompanyI comp;
		PublicCertificateI cert;
		while (it.hasNext())
		{
			comp = (PublicCompanyI) it.next();
			Iterator it2 = (comp).getCertificates().iterator();
			while (it2.hasNext())
			{
				cert = (PublicCertificateI) it2.next();
				ipo.addCertificate(cert);
				/** TODO in some games not all certs are buyable at the start */  
			}
		}
	}

	/**
	 * @return Which type of game we're playing (1830, 1856, 1870, etc.)
	 */
	public static int getGameType()
	{
		return gameType;
	}

	/**
	 * @return IPO Portfolio
	 */
	public static Portfolio getIpo()
	{
		return ipo;
	}

	public static Portfolio getScrapHeap()
	{
		return scrapHeap;
	}

	/**
	 * @return Bank's current cash level
	 */
	public int getCash()
	{
		return money.getCash();
	}

	/**
	 * Adds cash back to the bank
	 */
	public boolean addCash(int amount)
	{
		boolean negative = money.addCash(amount);

		/*
		 * Check if the bank has broken. In some games <0 could apply, so this
		 * will become configurable.
		 */
		if (money.getCash() <= 0 && !broken)
		{
			broken = true;
			Log.write("Bank is broken");
		}
		return negative;
	}

	public static boolean isBroken()
	{
		return broken;
	}

	public static boolean isJustBroken()
	{
		boolean result = broken && !brokenReported;
		brokenReported = true;
		return result;
	}

	/**
	 * @return Portfolio of stock in Bank Pool
	 */
	public static Portfolio getPool()
	{
		return pool;
	}

	/**
	 * @return Portfolio of unavailable shares
	 */
	public static Portfolio getUnavailable()
	{
		return unavailable;
	}

	/**
	 * @param Set
	 *            Bank's cash.
	 */
	public void setCash(int i)
	{
		money.setCash(i);
	}

	public String getName()
	{
		return "Bank";
	}

	public String getFormattedCash()
	{
		return money.toString();
	}

	public ModelObject getCashModel()
	{
		return money;
	}

	/**
	 * Get the maximum share percentage that may be sold to the Bank Pool.
	 * 
	 * @return The maximum percentage.
	 */
	public static int getPoolShareLimit()
	{
		return poolShareLimit;
	}

	public static String format(int amount)
	{
		return moneyFormat.replaceFirst("@", "" + amount);
	}
	
}
