/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/StockSpaceI.java,v 1.6 2010/03/10 17:26:49 stefanfrey Exp $ */
package rails.game;

import java.awt.Color;
import java.util.*;

public interface StockSpaceI // extends TokenHolderI
{

    /*--- Constants ---*/
    /** The name of the XML tag used to configure a stock space. */
    public static final String ELEMENT_ID = "StockSpace";

    /**
     * The name of the XML attribute for the stock price's name (like "A1" -
     * naming is like spreadsheet cells.
     */
    public static final String NAME_TAG = "name";

    /** The name of the XML attribute for the stock price. */
    public static final String PRICE_TAG = "price";

    /** The name of the XML attribute for the stock price type (optional). */
    public static final String TYPE_TAG = "type";

    /**
     * The name of the XML tag for the "startSpace" property. (indicating an
     * allowed PAR price)
     */
    public static final String START_SPACE_TAG = "StartSpace";

    /** The name of the XML tag for the "below ledge" property. */
    public static final String BELOW_LEDGE_TAG = "BelowLedge";

    /** The name of the XML tag for the "left of ledge" property. */
    public static final String LEFT_OF_LEDGE_TAG = "LeftOfLedge";

    /** The name of the XML tag for the "closes company" property. */
    public static final String CLOSES_COMPANY_TAG = "ClosesCompany";

    /** The name of the XML tag for the "gamn over" property. */
    public static final String GAME_OVER_TAG = "GameOver";

    // No constructors (yet) for the booleans, which are rarely needed. Use the
    // setters.
    public abstract boolean isBelowLedge();

    /**
     * @return TRUE if the square closes companies landing on it.
     */
    public abstract boolean closesCompany();

    /**
     * @return The square's colour.
     */
    public abstract Color getColour();

    /**
     * @return TRUE if the rails.game ends if a company lands on this square.
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
     * Find the stack position of a company token
     * 
     * @return Stock position: 0 = top, increasing towards the bottom. -1 if not
     * found.
     */
    public int getStackPosition(PublicCompanyI company);

    public void addFixedStartPrice(PublicCompanyI company);

    public List<PublicCompanyI> getFixedStartPrices();

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

    public boolean addToken(PublicCompanyI company);

    public boolean addTokenAtStackPosition(PublicCompanyI company, int stackPosition);
    
    public boolean removeToken(PublicCompanyI company);

    public boolean hasTokens();

    public List<PublicCompanyI> getTokens();

}
