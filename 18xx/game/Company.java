
/*
 * Created on 05mar2005
 *
 */
package game;

import java.util.ArrayList;

/**
 * @author Erik Vos
 */
public class Company implements CompanyI {
	
	protected static int numberOfCompanies = 0;
	protected String name;
	protected String type;
	protected String fgcolour;
	protected String bgcolour;
	protected int number; // For internal use
	
	protected StockSpaceI parPrice = null;
	protected StockSpaceI currentPrice = null;
	
	protected int treasury;
	protected boolean hasFloated = false;
	protected boolean closed = false;
	protected boolean canBuyStock;
	protected ArrayList trainsOwned;
	protected ArrayList portfolio;
	protected ArrayList littleCoOwned;

	public Company(String name, String type, String fgColour, String bgColour) {
		this.name = name;
		this.type = type;
		this.fgcolour = fgColour;
		this.bgcolour = bgColour;
		this.number = numberOfCompanies++;
	}
	
	public void start (StockSpaceI startPrice) {
		parPrice = currentPrice = startPrice;
		hasFloated = true;
		parPrice.addToken(this);
	}
	
	

	/**
	 * @return
	 */
	public String getBgColour() {
		return bgcolour;
	}

	/**
	 * @return
	 */
	public boolean canBuyStock() {
		return canBuyStock;
	}

	/**
	 * @return
	 */
	public String getFgColour() {
		return fgcolour;
	}

	/**
	 * @return
	 */
	public boolean hasFloated() {
		return hasFloated;
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return
	 */
	public ArrayList getPortfolio() {
		return portfolio;
	}

	/**
	 * @return
	 */
	public StockSpaceI getParPrice() {
		return parPrice;
	}

	/**
	 * @return
	 */
	public ArrayList getTrainsOwned() {
		return trainsOwned;
	}

	/**
	 * @return
	 */
	public int getTreasury() {
		return treasury;
	}

	/**
	 * @param list
	 */
	public void setTrainsOwned(ArrayList list) {
		trainsOwned = list;
	}

	/**
	 * @param i
	 */
	public void setTreasury(int i) {
		treasury = i;
	}

	/**
	 * @return
	 */
	public StockSpaceI getCurrentPrice() {
		return currentPrice;
	}

	/**
	 * @param price
	 */
	public void setCurrentPrice(StockSpaceI price) {
		currentPrice = price;
	}

	/**
	 * @param b
	 */
	public void setFloated(boolean b) {
		hasFloated = b;
	}

	/**
	 * @return
	 */
	public static int getNumberOfCompanies() {
		return numberOfCompanies;
	}

	/**
	 * @return
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * @param i
	 */
	public static void setNumberOfCompanies(int i) {
		numberOfCompanies = i;
	}

	/**
	 * @return
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * @param b
	 */
	public void setClosed(boolean b) {
		closed = b;
		if (closed) currentPrice = null;
	}

	/**
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param string
	 */
	public void setBgColour(String string) {
		bgcolour = string;
	}

	/**
	 * @param string
	 */
	public void setFgColour(String string) {
		fgcolour = string;
	}


}
