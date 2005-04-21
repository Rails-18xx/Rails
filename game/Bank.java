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

public class Bank implements CashHolder, ConfigurableComponentI {
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

		instance = this;
		// Create the IPO and the Bank Pool.
		// Here the Pool pays out, but that should be made configurable.
		ipo = new Portfolio ("IPO", this, false);
		pool = new Portfolio ("Pool", this, true);

	}

	public void configureFromXML (Element element) throws ConfigurationException {

		NamedNodeMap nnp;
		int number, startCash, certLimit;
		int maxNumber = 0;

		// Parse the Bank element
		Element node = (Element) element.getElementsByTagName("Bank").item(0);
		if (node != null) {
			nnp = node.getAttributes();
			money = XmlUtils.extractIntegerAttribute(nnp, "amount");
		}
		if (money == 0) money = 12000;
		Log.write("Bank size is "+money);

		NodeList players = element.getElementsByTagName("Players");
		for (int i=0; i<players.getLength(); i++) {
			nnp = ((Element)players.item(i)).getAttributes();
			number = XmlUtils.extractIntegerAttribute(nnp, "number");
			startCash = XmlUtils.extractIntegerAttribute(nnp, "cash");
			certLimit = XmlUtils.extractIntegerAttribute(nnp, "certLimit");
			
			Player.setLimits(number, startCash, certLimit);

		}

	}

	/** Put all available certificates in the IPO
	 */
	public void initIpo () {
		// Add privates
		List privates = Game.getInstance().getCompanyManager().getAllPrivateCompanies();
		Iterator it = privates.iterator();
		while (it.hasNext()) {
			ipo.addPrivate((PrivateCompanyI)it.next());
		}

		// Add public companies
		List companies = Game.getInstance().getCompanyManager().getAllPublicCompanies();
		it = companies.iterator();
		while (it.hasNext()) {
			Iterator it2 = ((PublicCompanyI)it.next()).getCertificates().iterator();
			while (it2.hasNext()) {
				ipo.addCertificate((CertificateI)it2.next());
			}
		}
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