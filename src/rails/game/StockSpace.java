package rails.game;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import rails.game.state.ArrayListState;
import rails.game.state.Item;
import rails.game.state.Model;

/**
 * Objects of this class represent a square on the StockMarket.
 */
public class StockSpace extends Model {

    /*--- Class attributes ---*/
    /*--- Constants ---*/
    /** The getId() of the XML tag used to configure a stock space. */
    public static final String ELEMENT_ID = "StockSpace";

    /**
     * The getId() of the XML attribute for the stock price's getId() (like "A1" -
     * naming is like spreadsheet cells.
     */
    public static final String NAME_TAG = "getId()";

    /** The getId() of the XML attribute for the stock price. */
    public static final String PRICE_TAG = "price";

    /** The getId() of the XML attribute for the stock price type (optional). */
    public static final String TYPE_TAG = "type";

    /**
     * The getId() of the XML tag for the "startSpace" property. (indicating an
     * allowed PAR price)
     */
    public static final String START_SPACE_TAG = "StartSpace";

    /** The getId() of the XML tag for the "below ledge" property. */
    public static final String BELOW_LEDGE_TAG = "BelowLedge";

    /** The getId() of the XML tag for the "left of ledge" property. */
    public static final String LEFT_OF_LEDGE_TAG = "LeftOfLedge";

    /** The getId() of the XML tag for the "closes company" property. */
    public static final String CLOSES_COMPANY_TAG = "ClosesCompany";

    /** The getId() of the XML tag for the "gamn over" property. */
    public static final String GAME_OVER_TAG = "GameOver";

    /*--- Instance attributes ---*/
    protected int row;
    protected int column;
    protected int price;
    protected String colour;
    protected boolean belowLedge = false; // For 1870
    protected boolean leftOfLedge = false; // For 1870
    protected boolean closesCompany = false;// For 1856 and other games
    protected boolean endsGame = false; // For 1841 and other games
    protected boolean start = false; // Company may start here
    protected StockSpaceType type = null;
    
    
    /*--- State fields */
    protected final ArrayListState<PublicCompany> tokens = ArrayListState.create(this, "tokens");
    protected final ArrayListState<PublicCompany> fixedStartPrices = ArrayListState.create(this, "fixedStartPrices");

    protected static Logger log =
            LoggerFactory.getLogger(StockSpace.class.getPackage().getName());

    /*--- Constructors ---*/
    private StockSpace(Item parent, String id, int price, StockSpaceType type) {
        super(parent, id);
        this.price = price;
        this.type = type;

        this.row = Integer.parseInt(id.substring(1)) - 1;
        this.column = (id.toUpperCase().charAt(0) - '@') - 1;
    }

    /**
     * @return fully initialized StockSpace
     */
    public static StockSpace create(Item parent, String id, int price, StockSpaceType type) {
        return new StockSpace(parent, id, price, type);
    }
    
    /**
     * @return initialized StockSpace with default StockSpaceType
     */
    public static StockSpace create (Item parent, String id, int price) {
        return new StockSpace(parent, id, price, null);
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
    public boolean addToken(PublicCompany company) {
        log.debug(company.getId() + " price token added to " + getId());
        tokens.add(company);
        return true;
    }

    public boolean addTokenAtStackPosition(PublicCompany company, int stackPosition) {
        log.debug(company.getId() + " price token added to " + getId() + "  at stack position "+stackPosition);
        tokens.add(stackPosition, company);
        return true;
    }

    /**
     * Remove a token from the pile.
     *
     * @param company The company object to remove.
     * @return False if the token was not found.
     */
    public boolean removeToken(PublicCompany company) {
        log.debug(company.getId() + " price token removed from " + getId());
        return tokens.remove(company);
    }

    public ImmutableList<PublicCompany> getTokens() {
        return tokens.view();
    }

    /**
     * Find the stack position of a company token
     *
     * @return Stock position: 0 = top, increasing towards the bottom. -1 if not
     * found.
     */
    public int getStackPosition(PublicCompany company) {
        return tokens.indexOf(company);
    }

    /*----- Fixed start prices (e.g. 1835, to show in small print) -----*/
    public void addFixedStartPrice(PublicCompany company) {
        fixedStartPrices.add(company);
    }

    public ImmutableList<PublicCompany> getFixedStartPrices() {
        return fixedStartPrices.view();
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
    public StockSpaceType getType() {
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

    public String getData() {
        return Bank.format(price);
    }

    @Override
    public String toString() {
        return getData();
    }
}
