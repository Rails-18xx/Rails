package rails.game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rails.game.model.ModelObject;

/**
 * Objects of this class represent a square on the StockMarket.
 */
public class StockSpace extends ModelObject implements StockSpaceI {

    /*--- Class attributes ---*/

    /*--- Instance attributes ---*/
    protected String name;
    protected int row;
    protected int column;
    protected int price;
    protected String colour;
    protected boolean belowLedge = false; // For 1870
    protected boolean leftOfLedge = false; // For 1870
    protected boolean closesCompany = false;// For 1856 and other games
    protected boolean endsGame = false; // For 1841 and other games
    protected boolean start = false; // Company may start here
    protected StockSpaceTypeI type = null;
    protected ArrayList<PublicCompanyI> tokens =
            new ArrayList<PublicCompanyI>();
    protected ArrayList<PublicCompanyI> fixedStartPrices =
            new ArrayList<PublicCompanyI>();

    protected static Logger log =
            Logger.getLogger(StockSpace.class.getPackage().getName());

    /*--- Contructors ---*/
    public StockSpace(String name, int price, StockSpaceTypeI type) {
        this.name = name;
        this.price = price;
        this.type = type;
        this.row = Integer.parseInt(name.substring(1)) - 1;
        this.column = (name.toUpperCase().charAt(0) - '@') - 1;
    }

    public StockSpace(String name, int price) {
        this(name, price, null);
    }

    // No constructors for the booleans. Use the setters.

    /*--- Token handling methods ---*/
    /**
     * Add a token at the end of the array (i.e. at the bottom of the pile)
     *
     * Always returns true;
     *
     * @param company The company object to add.
     */
    public boolean addToken(PublicCompanyI company) {
        log.debug(company.getName() + " price token added to " + name);
        tokens.add(company);
        update();
        return true;
    }

    public boolean addTokenAtStackPosition(PublicCompanyI company, int stackPosition) {
        log.debug(company.getName() + " price token added to " + name + "  at stack position "+stackPosition);
        tokens.add(stackPosition, company);
        update();
        return true;
    }

    /**
     * Remove a token from the pile.
     *
     * @param company The company object to remove.
     * @return False if the token was not found.
     */
    public boolean removeToken(PublicCompanyI company) {
        log.debug(company.getName() + " price token removed from " + name);
        int index = tokens.indexOf(company);
        if (index >= 0) {
            tokens.remove(index);
            update();
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public List<PublicCompanyI> getTokens() {
        return tokens;
    }

    /**
     * Find the stack position of a company token
     *
     * @return Stock position: 0 = top, increasing towards the bottom. -1 if not
     * found.
     */
    public int getStackPosition(PublicCompanyI company) {
        return tokens.indexOf(company);
    }

    /*----- Fixed start prices (e.g. 1835, to show in small print) -----*/
    public void addFixedStartPrice(PublicCompanyI company) {
        fixedStartPrices.add(company);
    }

    public List<PublicCompanyI> getFixedStartPrices() {
        return fixedStartPrices;
    }

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
    public Color getColour() {
        if (type != null) {
            return type.getColour();
        } else {
            return Color.WHITE;
        }
    }

    /**
     * @return TRUE if the rails.game ends if a company lands on this square.
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
    public StockSpaceTypeI getType() {
        return type;
    }

    /**
     * @return
     */
    public int getRow() {
        return row;
    }

    /**
     * @return
     */
    public boolean isStart() {
        return start;
    }

    /**
     * @return
     */
    public boolean isLeftOfLedge() {
        return leftOfLedge;
    }

    /**
     * @return
     */
    public boolean isNoBuyLimit() {
        return type != null && type.isNoBuyLimit();
    }

    /**
     * @return
     */
    public boolean isNoCertLimit() {
        return type != null && type.isNoCertLimit();
    }

    /**
     * @return
     */
    public boolean isNoHoldLimit() {
        return type != null && type.isNoHoldLimit();
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
     * @param set space as a starting space
     */
    public void setStart(boolean b) {
        start = b;
    }

    /**
     * @param set if token is left of ledge
     */
    public void setLeftOfLedge(boolean b) {
        leftOfLedge = b;
    }

    /**
     * @return Returns if the space hasTokens.
     */
    public boolean hasTokens() {
        return !tokens.isEmpty();
    }

    @Override
    public String getText() {
        return Bank.format(price);
    }

    @Override
    public String toString() {
        return getText();
    }
}
