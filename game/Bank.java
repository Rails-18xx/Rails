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

public class Bank implements CashHolder {
	private int money;

	private int gameType;

	private Portfolio ipo = null;
	private Portfolio pool = null;
	private static Bank instance = null;
	
	public static Bank getInstance () {
		return instance;
	}
	
	/**
	 * Central method for transferring all cash.
	 * @param from Who pays the money (null = Bank).
	 * @param to Who received the money (null = Bank).
	 * @param amount The amount of money.
	 */
	public static void transferCash (CashHolder from, CashHolder to, int amount) {
		if (from == null) from = instance;
		else if (to == null) to = instance;
		from.addCash(-amount);
		to.addCash(amount);
	}

	public Bank() {
		this(0, 0);
	}

	public Bank(int numPlayers) {
		this(numPlayers, 0);
	}

	public Bank(int numPlayers, int gameType) {
		
		instance = this;
		// Create the IPO and the Bank Pool. 
		// Here the Pool pays out, but that should be made configurable.
		ipo = new Portfolio ("IPO", this, false);
		pool = new Portfolio ("Pool", this, true);

		money = 12000; // To be made configurable
	}
	/**
	 * @return
	 */
	public int getGameType() {
		return gameType;
	}

	/**
	 * @return
	 */
	public Portfolio getIpo() {
		return ipo;
	}

	/**
	 * @return
	 */
	public int getCash() {
		return money;
	}
	
	public void addCash (int amount) {
		this.money += amount;
	}

	/**
	 * @return
	 */
	public Portfolio getPool() {
		return pool;
	}

	/**
	 * @param i
	 */
	public void setCash(int i) {
		money = i;
	}
	
	public String getName() {
		return "Bank";
	}

}