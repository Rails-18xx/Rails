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

public class Player implements CashHolder {
	
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

	private int wallet = 0;

	private int blockedCash = 0;

	private boolean hasPriority = false;

	private boolean hasBoughtStockThisTurn = false;

	private Portfolio portfolio = null;

	private ArrayList companiesSoldThisTurn = new ArrayList();

	public static void setLimits(int number, int cash, int certLimit) {
		if (number > 1 && number <= MAX_PLAYERS) {
			playerStartCash[number] = cash;
			playerCertificateLimits[number] = certLimit;
		}
	}

	/**
	 * Initialises each Player's parameters which depend on the number of
	 * players. To be called when all Players have been added.
	 *  
	 */
	public static void initPlayers(Player[] players) {
		Player player;
		int numberOfPlayers = players.length;
		int startCash = playerStartCash[numberOfPlayers];

		// Give each player the initial cash amount
		for (int i = 0; i < numberOfPlayers; i++) {
			player = (Player) players[i];
			player.index = i;
			Bank.transferCash(null, player, startCash);
			Log.write("Player " + player.getName() + " receives "
					+ Bank.format(startCash) + ". Bank now has "
					+ Bank.getInstance().getFormattedCash());
		}

		// Set the sertificate limit
		playerCertificateLimit = playerCertificateLimits[numberOfPlayers];
	}

	public static int getCertLimit() {
		return playerCertificateLimit;
	}
	
	public static void setShareLimit (int percentage) {
	    playerShareLimit = percentage;
	}

	public Player(String name) {
		this.name = name;
		portfolio = new Portfolio(name, this);
	}

	/**
	 * @param share
	 * @throws NullPointerException
	 *             if company hasn't started yet. UI needs to handle this.
	 */
	public void buyShare(PublicCertificate share, int price)
			throws NullPointerException {
		if (hasBoughtStockThisTurn)
			return;

		for (int i = 0; i < companiesSoldThisTurn.size(); i++) {
			if (share.company.getName().equalsIgnoreCase(
					companiesSoldThisTurn.get(i).toString()))
				return;
		}

		if (portfolio.getCertificates().size() >= playerCertificateLimit)
			return;

		try {
			//throws nullpointer if company hasn't started yet.
			//it's up to the UI to catch this and gracefully start the company.
			getPortfolio().buyCertificate(share, share.getPortfolio(), price);
		} catch (NullPointerException e) {
			throw e;
		}

		Game.getPlayerManager().setBoughtStockLast(this);
		hasBoughtStockThisTurn = true;
	}

	public void buyShare(PublicCertificate share) throws NullPointerException {
		try {
			buyShare(share, share.getCompany().getCurrentPrice().getPrice());
		} catch (NullPointerException e) {
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
	public boolean mayBuyCertificates(int number) {
		if (portfolio.getCertificates().size() + number > playerCertificateLimit)
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
	public boolean mayBuyCompanyShare(PublicCompanyI company, int number) {
		if (portfolio.ownsShare(company) + number * company.getShareUnit() > playerShareLimit)
			return false;
		/** TODO The '60' above must of course be made configurable! */
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
	public void buy(Certificate cert, int price) {

		if (cert instanceof PrivateCompanyI) {
			portfolio.buyPrivate((PrivateCompanyI) cert, cert.getPortfolio(),
					price);
		} else if (cert instanceof PublicCertificateI) {
			Portfolio from = cert.getPortfolio();
			portfolio.buyCertificate((PublicCertificateI) cert, from, price);
			((PublicCertificateI) cert).getCompany().checkPresidencyOnBuy(this);
		}
	}

	public int sellShare(PublicCertificate share) {
		Portfolio.sellCertificate(share, portfolio, share.getCompany()
				.getCurrentPrice().getPrice());
		Game.getStockMarket().sell(share.getCompany(), 1);
		return 1;
	}

	/**
	 * @return Returns the hasPriority.
	 */
	public boolean hasPriority() {
		return hasPriority;
	}

	/**
	 * @param hasPriority
	 *            The hasPriority to set.
	 */
	public void setHasPriority(boolean hasPriority) {
		this.hasPriority = hasPriority;
	}

	/**
	 * @return Returns the portfolio.
	 */
	public Portfolio getPortfolio() {
		return portfolio;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Returns the wallet.
	 */
	public int getCash() {
		return wallet;
	}

	public String getFormattedCash() {
		return Bank.format(wallet);
	}

	public void addCash(int amount) {
		wallet += amount;
	}

	/**
	 * Get the player's total worth.
	 * 
	 * @return Total worth
	 */
	public int getWorth() {
		int worth = wallet;
		Iterator it = portfolio.getCertificates().iterator();
		while (it.hasNext()) {
			worth += ((PublicCertificateI) it.next()).getCertificatePrice();
		}
		it = portfolio.getPrivateCompanies().iterator();
		while (it.hasNext()) {
			worth += ((PrivateCompanyI) it.next()).getBasePrice();
		}
		return worth;
	}

	public String getFormattedWorth() {
		return Bank.format(getWorth());
	}

	public String toString() {
		return "Name: " + name + " Cash: " + wallet;
	}

	/**
	 * @return Returns the hasBoughtStockThisTurn.
	 */
	public boolean hasBoughtStockThisTurn() {
		return hasBoughtStockThisTurn;
	}

	/**
	 * Block cash allocated by a bid.
	 * 
	 * @author Erik Vos
	 * @param amount
	 *            Amount of cash to be blocked.
	 * @return false if the amount was not available.
	 */
	public boolean blockCash(int amount) {
		if (amount > wallet - blockedCash) {
			return false;
		} else {
			blockedCash += amount;
			return true;
		}
	}

	/**
	 * Unblock cash.
	 * 
	 * @author Erik Vos
	 * @param amount
	 *            Amount to be unblocked.
	 * @return false if the given amount was not blocked.
	 */
	public boolean unblockCash(int amount) {
		if (amount > blockedCash) {
			return false;
		} else {
			blockedCash -= amount;
			return true;
		}
	}

	/**
	 * Unblock all blocked cash.
	 * 
	 * @author Erik Vos
	 * @return Always true.
	 */
	public boolean unblockCash() {
		blockedCash = 0;
		return true;
	}

	/**
	 * Return the unblocked cash (available for bidding)
	 * 
	 * @return
	 */
	public int getUnblockedCash() {
		return wallet - blockedCash;
	}
	
	public int getBlockedCash() {
	    return blockedCash;
	}
	
	public int getIndex() {
		return index;
	}
}