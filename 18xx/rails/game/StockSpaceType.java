/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/StockSpaceType.java,v 1.4 2009/12/13 16:39:49 evos Exp $ */
package rails.game;

import java.awt.Color;

import rails.util.Util;

/**
 * Objects of this class represent a type of square on the StockMarket with
 * special properties,usually represented by a non-white square colour. The
 * default type is "white", which has no special properties.
 */
public class StockSpaceType implements StockSpaceTypeI {

    /*--- Class attributes ---*/

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
