/*
 * Created on 24-Feb-2005
 * Changes: 
 * 05mar2005 EV: Changed some names.
 */
package game;

import java.util.ArrayList;

/**
 * Objects of this class represent a square on the StockMarket.
 * 
 * @author Erik Vos
 */
public class StockSpace implements StockSpaceI {
	
	/*--- Class attributes ---*/
	
	/*--- Instance attributes ---*/
	protected String name;
	protected int row;
	protected int column;
	protected int price;
	protected int colour;
	protected boolean belowLedge = false;	// For 1870
	protected boolean leftOfLedge = false;	// For 1870
	protected boolean closesCompany = false;// For 1856 and other games
	protected boolean endsGame = false;		// For 1841 and other games
	protected boolean start = false; 		// Company may start here
	protected boolean noCertLimit = false;  // In yellow zone
	protected boolean noHoldLimit = false;	// In orange zone (1830)
	protected boolean noBuyLimit = false;	// In brown zone (1830)
	
	
	protected ArrayList tokens = new ArrayList();
	
	// Other colours exist but can probably be mapped to this set.
	
	/*--- Contructors ---*/
	public StockSpace (String name, int price) {
		this (name, price, WHITE);
	}
	
	public StockSpace (String name, int price, int colour) {
		this.name = name;
		this.price = price;
		this.colour = colour;
		this.row = Integer.parseInt(name.substring(1)) - 1;
		this.column = (int)(name.toUpperCase().charAt(0) - '@') - 1;
	}
	// No constructors (yet) for the booleans, which are rarely needed. Use the setters.
		
	/*--- Getters ---*/
	/**
	 * @return TRUE is the square is just above a ledge.
	 */
	public boolean isBelowLedge() {
		return belowLedge;
	}

	/**
	 * @return TRUE if the square closes companies landing on it.
	 */
	public boolean closesCompany() {
		return closesCompany;
	}

	/**
	 * @return The square's colour.
	 */
	public int getColour() {
		return colour;
	}

	/**
	 * @return TRUE if the game ends if a company lands on this square.
	 */
	public boolean endsGame() {
		return endsGame;
	}

	/**
	 * @return The stock price associated with the square.
	 */
	public int getPrice() {
		return price;
	}

	/**
	 * @return
	 */
	public int getColumn() {
		return column;
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
	public int getRow() {
		return row;
	}

	/*--- Setters ---*/
	/**
	 * @param b See isAboveLedge.
	 */
	public void setBelowLedge(boolean b) {
		belowLedge = b;
	}

	/**
	 * @param b See isClosesCompany.
	 */
	public void setClosesCompany(boolean b) {
		closesCompany = b;
	}

	/**
	 * @param b See isEndsGame.
	 */
	public void setEndsGame(boolean b) {
		endsGame = b;
	}

	/**
	 * @return
	 */
	public boolean isStart() {
		return start;
	}

	/**
	 * @param b
	 */
	public void setStart(boolean b) {
		start = b;
	}
	
	/**
	 * Add a token at the end of the array (i.e. at the bottom of the pile)
	 * @param company The company object to add.
	 */
	public void addToken (CompanyI company) {
		tokens.add(company);
	}

	/**
	 * Remove a token from the pile.
	 * @param company The company object to remove.
	 * @return False if the token was not found.
	 */
	public boolean removeToken (CompanyI company) {
		int index = tokens.indexOf (company);
		if (index >= 0) {
			tokens.remove(index);
			return true;
		} else {
			return false; 
		}
	}
	/**
	 * @return
	 */
	public ArrayList getTokens() {
		return tokens;
	}

	/**
	 * @return
	 */
	public boolean isLeftOfLedge() {
		return leftOfLedge;
	}

	/**
	 * @param b
	 */
	public void setLeftOfLedge(boolean b) {
		leftOfLedge = b;
	}

	/**
	 * @return
	 */
	public boolean isNoBuyLimit() {
		return noBuyLimit;
	}

	/**
	 * @return
	 */
	public boolean isNoCertLimit() {
		return noCertLimit;
	}

	/**
	 * @return
	 */
	public boolean isNoHoldLimit() {
		return noHoldLimit;
	}

	/**
	 * @param b
	 */
	public void setNoBuyLimit(boolean b) {
		noBuyLimit = b;
	}

	/**
	 * @param b
	 */
	public void setNoCertLimit(boolean b) {
		noCertLimit = b;
	}

	/**
	 * @param b
	 */
	public void setNoHoldLimit(boolean b) {
		noHoldLimit = b;
	}

}
