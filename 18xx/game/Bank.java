/*
 * Rails: an 18xx game system. Copyright (C) 2005 Brett Lentz
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package game;

import java.util.*;
import org.w3c.dom.*;
import util.XmlUtils;

public class Bank implements CashHolder, ConfigurableComponentI
{

	/** Default limit of shares in the bank pool */
	private static final int DEFAULT_POOL_SHARE_LIMIT = 50;
	
	/** The Bank's amont of cash */
	private static int money;

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
	public static void transferCash(CashHolder from, CashHolder to, int amount)
	{
		if (from == null)
			from = instance;
		else if (to == null)
			to = instance;
		// System.out.println("Amount: " + amount);
		from.addCash(-amount);
		to.addCash(amount);
	}

	public Bank()
	{

		instance = this;
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
			money = XmlUtils.extractIntegerAttribute(nnp, "amount");
		}
		if (money == 0)
			money = 12000;
		Log.write("Bank size is " + money);

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
	 * @param percentage of a company allowed to be in the Bank pool.
	 */
	public static void setShareLimit(int percentage)
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
				// Log.write("*** Cert"+cert+"added to IPO");
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
		return money;
	}

	/**
	 * Adds cash back to the bank
	 */
	public void addCash(int amount)
	{
		money += amount;
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
	 * @param Set Bank's cash.
	 */
	public void setCash(int i)
	{
		money = i;
	}

	public String getName()
	{
		return "Bank";
	}

	public String getFormattedCash()
	{
		return Bank.format(money);
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
