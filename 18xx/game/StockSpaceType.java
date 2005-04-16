/*
 * $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/StockSpaceType.java,v 1.2 2005/04/16 22:43:53 evos Exp $
 * Created on 19mar2005 by Erik Vos
 * Changes: 
 */
package game;

/**
 * Objects of this class represent a type of square on the StockMarket
 * with special properties,usually represented by a non-white square colour.
 * The default type is "white", which has no special properties. 
 * 
 * @author Erik Vos
 */
public class StockSpaceType implements StockSpaceTypeI {
	
	/*--- Class attributes ---*/
	
	/*--- Instance attributes ---*/
	protected String name;
	protected String colour;
	protected boolean noCertLimit = false;  // In yellow zone
	protected boolean noHoldLimit = false;	// In orange zone (1830)
	protected boolean noBuyLimit = false;	// In brown zone (1830)
	
	
	/*--- Contructors ---*/
	public StockSpaceType (String name) {
		this (name, "");
	}
	
	public StockSpaceType (String name, String colour) {
		this.name = name;
		this.colour = colour;
	}
		
	/*--- Getters ---*/
	/**
	 * @return The square type's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return The square type's colour.
	 */
	public String getColour() {
		return colour;
	}

	/**
	 * @return TRUE if the square type has no buy limit ("brown area")
	 */
	public boolean isNoBuyLimit() {
		return noBuyLimit;
	}

	/**
	 * @return TRUE if the square type has no certificate limit ("yellow area")
	 */
	public boolean isNoCertLimit() {
		return noCertLimit;
	}

	/**
	 * @return TRUE if the square type has no hold limit ("orange area")
	 */
	public boolean isNoHoldLimit() {
		return noHoldLimit;
	}

	/*--- Setters ---*/
	/**
	 * @param b TRUE if the square type has no buy limit ("brown area")
	 */
	public void setNoBuyLimit(boolean b) {
		noBuyLimit = b;
	}

	/**
	 * @param b TRUE if the square type has no certificate limit ("yellow area")
	 */
	public void setNoCertLimit(boolean b) {
		noCertLimit = b;
	}

	/**
	 * @param b TRUE if the square type has no hold limit ("orange area")
	 */
	public void setNoHoldLimit(boolean b) {
		noHoldLimit = b;
	}

}
