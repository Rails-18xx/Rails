/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/StockSpaceTypeI.java,v 1.4 2009/12/13 16:39:48 evos Exp $ */
package rails.game;

import java.awt.Color;

/**
 * The interface for StockSpaceType.
 */
public interface StockSpaceTypeI {

    /*--- Constants ---*/
    /** The name of the XML tag used to configure a stock space. */
    public static final String ELEMENT_ID = "StockSpaceType";

    /**
     * The name of the XML attribute for the stock price type's name (any
     * string, usually the space colour).
     */
    public static final String NAME_TAG = "name";

    /**
     * The name of the XML attribute for the stock price's colour. (optional;
     * only provided as a possible help to the UI, which is free to redefine the
     * colour as it seems fit).
     */
    public static final String COLOUR_TAG = "colour";

    /**
     * The name of the XML tag for the "NoCertLimit" property. (1830: yellow
     * stock market area)
     */
    public static final String NO_CERT_LIMIT_TAG = "NoCertLimit";

    /**
     * The name of the XML tag for the "NoHoldLimit" property. (1830: orange
     * area)
     */
    public static final String NO_HOLD_LIMIT_TAG = "NoHoldLimit";

    /**
     * The name of the XML tag for the "NoBuyLimit" property. (1830: brown area)
     */
    public static final String NO_BUY_LIMIT_TAG = "NoBuyLimit";

    /**
     * @return Color
     */
    public abstract Color getColour();

    /**
     * @return Name
     */
    public abstract String getName();

    /**
     * @return if space triggers No Purchase Limit for stock
     */
    public boolean isNoBuyLimit();

    /**
     * @return if space triggers Stock not counting toward certificate ownership
     * limit
     */
    public boolean isNoCertLimit();

    /**
     * @return if space triggers Stock not counting toward certificate limit
     */
    public boolean isNoHoldLimit();

    /**
     * @param b
     */
    public void setNoBuyLimit(boolean b);

    /**
     * @param b
     */
    public void setNoCertLimit(boolean b);

    /**
     * @param b
     */
    public void setNoHoldLimit(boolean b);

}
