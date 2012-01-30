package rails.game;

import java.awt.Color;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;

import rails.game.model.Model;
import rails.game.state.ArrayListState;
import rails.game.state.Item;

/**
 * Objects of this class represent a square on the StockMarket.
 */
public class StockSpace extends Model {

    /*--- Class attributes ---*/
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
    protected StockSpaceType type = null;
    
    
    /*--- State fields */
    protected final ArrayListState<PublicCompany> tokens = ArrayListState.create("tokens");
    protected final ArrayListState<PublicCompany> fixedStartPrices = ArrayListState.create("fixedStartPrices");

    protected static Logger log =
            Logger.getLogger(StockSpace.class.getPackage().getName());

    /*--- Contructors ---*/
    private StockSpace(String id, int price, StockSpaceType type) {
        super(id);
        this.name = id;
        this.price = price;
        this.type = type;
        this.row = Integer.parseInt(id.substring(1)) - 1;
        this.column = (id.toUpperCase().charAt(0) - '@') - 1;
    }

    /**
     * Factory method for initialized StockSpace
     */
    public static StockSpace create(Item parent, String id, int price, StockSpaceType type) {
        return new StockSpace(id, price, type).init(parent);
    }
    
    /**
     * Factory method for initialized StockSpace with default StockSpaceType
     */
    public static StockSpace create (Item parent, String id, int price) {
        return create(parent, id, price, null);
    }
    
    @Override
    public StockSpace init(Item parent){
        super.init(parent);
        tokens.init(this);
        fixedStartPrices.init(this);
        return this;
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
        log.debug(company.getId() + " price token added to " + name);
        tokens.add(company);
        return true;
    }

    public boolean addTokenAtStackPosition(PublicCompany company, int stackPosition) {
        log.debug(company.getId() + " price token added to " + name + "  at stack position "+stackPosition);
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
        log.debug(company.getId() + " price token removed from " + name);
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
    public String getName() {
        return name;
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
