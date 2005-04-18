/*
 * Created on 12-Mar-2005
 */
package game;

import java.util.ArrayList;

/**
 * @author Erik
 */
public interface StockSpaceI {

	/*--- Constants ---*/
	/** The name of the XML tag used to configure a stock space. */
	public static final String ELEMENT_ID = "StockSpace";

	/** The name of the XML attribute for the stock price's name 
	 * (like "A1" - naming is like spreadsheet cells. */
	public static final String NAME_TAG = "name";

	/** The name of the XML attribute for the stock price. */
	public static final String PRICE_TAG = "price";

	/** The name of the XML attribute for the stock price type (optional). */
	public static final String TYPE_TAG = "type";

	/** The name of the XML tag for the "startSpace" property. 
	 * (indicating an allowed PAR price) */
	public static final String START_SPACE_TAG = "StartSpace";
	
	/** The name of the XML tag for the "below ledge" property. */
	public static final String BELOW_LEDGE_TAG = "BelowLedge";

	/** The name of the XML tag for the "left of ledge" property. */
	public static final String LEFT_OF_LEDGE_TAG = "LeftOfLedge";

	/** The name of the XML tag for the "closes company" property. */
	public static final String CLOSES_COMPANY_TAG = "ClosesCompany";

	/** The name of the XML tag for the "gamn over" property. */
	public static final String GAME_OVER_TAG = "GameOver";

	// No constructors (yet) for the booleans, which are rarely needed. Use the setters.
	public abstract boolean isBelowLedge();
	/**
	 * @return TRUE if the square closes companies landing on it.
	 */
	public abstract boolean closesCompany();
	/**
	 * @return The square's colour.
	 */
	public abstract String getColour();
	/**
	 * @return TRUE if the game ends if a company lands on this square.
	 */
	public abstract boolean endsGame();
	/**
	 * @return The stock price associated with the square.
	 */
	public abstract int getPrice();
	/**
	 * @return
	 */
	public abstract int getColumn();
	/**
	 * @return
	 */
	public abstract String getName();
	/**
	 * @return
	 */
	public abstract int getRow();
	/**
	 * @return
	 */
	public abstract StockSpaceTypeI getType();
	
	/**
	 * @param b
	 */
	public abstract boolean isStart();
	
	/*--- Setters ---*/
	public abstract void setBelowLedge(boolean b);
	/**
	 * @param b See isClosesCompany.
	 */
	public abstract void setClosesCompany(boolean b);
	/**
	 * @param b See isEndsGame.
	 */
	public abstract void setEndsGame(boolean b);
	/**
	 * @return
	 */
	public abstract void setStart(boolean b);
	/**
	 * Add a token at the end of the array (i.e. at the bottom of the pile)
	 * @param company The company object to add.
	 */
	public abstract void addToken(CompanyI company);
	/**
	 * Remove a token from the pile.
	 * @param company The company object to remove.
	 * @return False if the token was not found.
	 */
	public abstract boolean removeToken(CompanyI company);
	/**
	 * @return
	 */
	public abstract ArrayList getTokens();
	/**
	 * @return
	 */
	public abstract boolean isLeftOfLedge();
	/**
	 * @param b
	 */
	public abstract void setLeftOfLedge(boolean b);
	/**
	 * @return
	 */
	public boolean isNoBuyLimit();

	/**
	 * @return
	 */
	public boolean isNoCertLimit();

	/**
	 * @return
	 */
	public boolean isNoHoldLimit();

}