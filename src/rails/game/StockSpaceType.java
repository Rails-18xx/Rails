package rails.game;

import java.awt.Color;

import rails.common.parser.ConfigurationException;
import rails.util.Util;

/**
 * Objects of this class represent a type of square on the StockMarket with
 * special properties,usually represented by a non-white square colour. The
 * default type is "white", which has no special properties.
 */
public class StockSpaceType {

    /*--- Class attributes ---*/
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


    /*--- Instance attributes ---*/
    protected String name;
    protected String colourString;
    protected Color colour;
    protected boolean noCertLimit = false; // In yellow zone
    protected boolean noHoldLimit = false; // In orange zone (1830)
    protected boolean noBuyLimit = false; // In brown zone (1830)

    public static final String WHITE = "FFFFFF";

    /*--- Contructors ---*/
    public StockSpaceType(String name) throws ConfigurationException {
        this(name, "");
    }

    public StockSpaceType(String name, String colour) throws ConfigurationException {
        this.name = name;
        this.colourString = colour;
        //this.colour = new Color(Integer.parseInt(colourString, 16));
        this.colour = Util.parseColour(colourString);
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
    public Color getColour() {
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
