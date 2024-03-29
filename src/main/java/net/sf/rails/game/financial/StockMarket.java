package net.sf.rails.game.financial;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.model.StockMarketModel;
import net.sf.rails.game.state.Owner;

import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;


public class StockMarket extends RailsManager implements Configurable {
    /**
     * This is the name by which the CompanyManager should be registered with
     * the ComponentManager.
     */
    public static final String COMPONENT_NAME = "StockMarket";

    public static final String DEFAULT = "default";

    protected final HashMap<String, StockSpaceType> stockSpaceTypes = new HashMap<>();
    protected final HashMap<String, StockSpace> stockChartSpaces = new HashMap<>();
    protected final SortedSet<StockSpace> startSpaces = new TreeSet<>();

    protected final StockMarketModel marketModel = StockMarketModel.create(this);

    protected ChartType stockChartType = ChartType.RECTANGULAR;
    protected StockSpace[][] stockChart;
    protected int numRows = 0;
    protected int numCols = 0;
    protected StockSpaceType defaultType;

    /* Game-specific flags */
    protected boolean upOrDownRight = false;
    /* Sold out and at top: go down or right (1870) */

    // TODO: There used to be a BooleanState gameOver, did this have a function?

    public enum ChartType {
        LINEAR,
        RECTANGULAR,
        HEXAGONAL   // Not yet used
    }

    /**
     * Used by Configure (via reflection) only
     */
    public StockMarket(RailsRoot parent, String id) {
        super(parent, id);
    }

    /**
     * @see net.sf.rails.common.parser.Configurable#configureFromXML(Tag)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        // Define a default stockspace type with colour white
        defaultType = new StockSpaceType(DEFAULT, StockSpaceType.WHITE);
        stockSpaceTypes.put(DEFAULT, defaultType);

        stockChartType = ChartType.valueOf(tag.getAttributeAsString(
                "type", stockChartType.toString()).toUpperCase());

        /* Read and configure the stock market space types */
        List<Tag> typeTags = tag.getChildren(StockSpaceType.ELEMENT_ID);

        if (typeTags != null) {
            for (Tag typeTag : typeTags) {
                /* Extract the attributes of the Stock space type */
                String name =
                        typeTag.getAttributeAsString(StockSpaceType.NAME_TAG);
                if (name == null) {
                    throw new ConfigurationException(
                            LocalText.getText("UnnamedStockSpaceType"));
                }
                String colour =
                        typeTag.getAttributeAsString(StockSpaceType.COLOUR_TAG);

                /* Check for duplicates */
                if (stockSpaceTypes.get(name) != null) {
                    throw new ConfigurationException(LocalText.getText(
                            "StockSpaceTypeConfiguredTwice", name));
                }

                /* Create the type */
                StockSpaceType type = new StockSpaceType(name, colour);
                stockSpaceTypes.put(name, type);

                // Check the stock space type flags
                type.setNoBuyLimit(typeTag.getChild(StockSpaceType.NO_BUY_LIMIT_TAG) != null);
                type.setNoCertLimit(typeTag.getChild(StockSpaceType.NO_CERT_LIMIT_TAG) != null);
                type.setNoHoldLimit(typeTag.getChild(StockSpaceType.NO_HOLD_LIMIT_TAG) != null);
            }
        }

        /* Read and configure the stock market spaces */
        List<Tag> spaceTags = tag.getChildren(StockSpace.ELEMENT_ID);
        StockSpaceType type;
        int row, col;
        for (Tag spaceTag : spaceTags) {
            type = null;

            // Extract the attributes of the Stock space
            String name = spaceTag.getAttributeAsString(StockSpace.NAME_TAG);
            if (name == null) {
                throw new ConfigurationException(
                        LocalText.getText("UnnamedStockSpace"));
            }
            String price = spaceTag.getAttributeAsString(StockSpace.PRICE_TAG);
            if (price == null) {
                throw new ConfigurationException(LocalText.getText(
                        "StockSpaceHasNoPrice", name));
            }
            String typeName =
                    spaceTag.getAttributeAsString(StockSpace.TYPE_TAG);
            if (typeName != null
                    && (type = stockSpaceTypes.get(typeName)) == null) {
                throw new ConfigurationException(LocalText.getText(
                        "StockSpaceTypeUndefined", type));
            }
            if (type == null) type = defaultType;

            if (stockChartSpaces.get(name) != null) {
                throw new ConfigurationException(LocalText.getText(
                        "StockSpacesConfiguredTwice", name));
            }

            StockSpace space = StockSpace.create(this, name, Integer.parseInt(price), type);
            stockChartSpaces.put(name, space);

            row = Integer.parseInt(name.substring(1));
            col = (name.toUpperCase().charAt(0) - '@');
            if (row > numRows) numRows = row;
            if (col > numCols) numCols = col;

            // Loop through the stock space flags
            if (spaceTag.getChild(StockSpace.START_SPACE_TAG) != null) {
                space.setStart(true);
                startSpaces.add(space);
            }
            space.setClosesCompany(spaceTag.getChild(StockSpace.CLOSES_COMPANY_TAG) != null);
            space.setEndsGame(spaceTag.getChild(StockSpace.GAME_OVER_TAG) != null);
            space.setBelowLedge(spaceTag.getChild(StockSpace.BELOW_LEDGE_TAG) != null);
            space.setLeftOfLedge(spaceTag.getChild(StockSpace.LEFT_OF_LEDGE_TAG) != null);

        }

        stockChart = new StockSpace[numRows][numCols];
        for (StockSpace space : stockChartSpaces.values()) {
            stockChart[space.getRow()][space.getColumn()] = space;
        }

        upOrDownRight = tag.getChild("UpOrDownRight") != null;

    }

    /**
     * Final initialisations, to be called after all XML processing is complete.
     * The purpose is to register fixed company start prices.
     */
    public void finishConfiguration(RailsRoot root) {
        for (PublicCompany comp : root.getCompanyManager().getAllPublicCompanies()) {
            if (!comp.hasStarted() && comp.getStartSpace() != null) {
                comp.getStartSpace().addFixedStartPrice(comp);
            }
        }

    }

    public ChartType getStockChartType() {
        return stockChartType;
    }

    public StockSpace getStockSpace(int row, int col) {
        if (row >= 0 && row < numRows && col >= 0 && col < numCols) {
            return stockChart[row][col];
        } else {
            return null;
        }
    }

    public StockSpace getStockSpace(String name) {
        return stockChartSpaces.get(name);
    }

    /*--- Actions ---*/

    public void start(PublicCompany company, StockSpace price) {
        prepareMove(company, null, price);
        // make marketModel updating on company price model
        company.getCurrentPriceModel().addModel(marketModel);
    }

    public void payOut(PublicCompany company) {
        payOut (company, 1);
        //moveRightOrUp(company);
    }

    public void payOut(PublicCompany company, int jumps) {
        if (stockChartType == ChartType.LINEAR) {
            moveRight(company,  jumps);
        } else {
            moveRightOrUp(company, jumps);
        }
    }

    public void withhold(PublicCompany company) {
        moveLeftOrDown(company);
    }

    // Old version for backwards compatibility. See below for new version.
    /*
    public void sell(PublicCompany company, int numberOfSpaces) {
        moveDown(company, null, numberOfSpaces);
    }
    */
    public void sell(PublicCompany company, Owner seller, int sharesSold) {

        int numberOfSpacesDown = spacesDownOnSale (sharesSold, seller);
        if (stockChartType == ChartType.LINEAR) {
            moveLeft(company, seller, numberOfSpacesDown);
        } else {
            moveDown(company, seller, numberOfSpacesDown);
        }
    }

    /** Stub that must be overridden in special cases where
     * after selling shares the number of spaces down is not
     * (always) equal to the number of shares sold.
     *
     * Examples: 1835, 1837, SOH.
     *
     * @param sharesSold The number of share units sold
     * @param seller The player or company that sold these shares
     * @return The number of spaces down (or left in linear markets).
     */
    public int spacesDownOnSale (int sharesSold, Owner seller) {
        return sharesSold;
    }

    public void soldOut(PublicCompany company) {
        moveUp(company);
    }

    public void moveUp(PublicCompany company) {
        StockSpace oldsquare = company.getCurrentSpace();
        StockSpace newsquare = oldsquare;
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();
        if (row > 0) {
            newsquare = getStockSpace(row - 1, col);
        } else if (upOrDownRight && col < numCols - 1) {
            newsquare = getStockSpace(row + 1, col + 1);
        }
        if (newsquare != null) prepareMove(company, oldsquare, newsquare);
    }

    public void close(PublicCompany company) {
        prepareMove(company, company.getCurrentSpace(), null);
    }

    protected void moveDown (PublicCompany company, int numberOfSpaces) {
        moveDown (company, null, numberOfSpaces);
    }

    // Default version, ignoring seller.
    // Games where the seller matters must override this method.
    protected void moveDown(PublicCompany company, Owner seller, int numberOfSpaces) {
        StockSpace oldsquare = company.getCurrentSpace();
        StockSpace newsquare = oldsquare;
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();

        /* Drop the indicated number of rows */
        int newrow = row + numberOfSpaces;

        /* Don't drop below the bottom of the chart */
        while (newrow >= numRows || getStockSpace(newrow, col) == null)
            newrow--;

        // Check if company may enter a "Closed" area
        while (getStockSpace(newrow, col).closesCompany() && !company.canClose())
            newrow--;

        /*
         * If marker landed just below a ledge, and NOT because it was bounced
         * by the bottom of the chart, it will stay just above the ledge.
         */
        if (getStockSpace(newrow, col).isBelowLedge()
                && newrow == row + numberOfSpaces) newrow--;

        if (newrow > row) {
            newsquare = getStockSpace(newrow, col);
        }
        if (newsquare != oldsquare) {
            prepareMove(company, oldsquare, newsquare);
        }
    }

    /** Replaces (moveRightOrUp) for linear stockmarkets.
     * @param company Company that has just paid dividends
     * @param numberOfSpaces The number of spaces to move.
     */
    public void moveRight (PublicCompany company, int numberOfSpaces) {

        StockSpace oldsquare = company.getCurrentSpace();
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();
        for (int i=0; i<numberOfSpaces; i++) {
            if (col < numCols - 1
                    && (getStockSpace(row, col + 1)) != null) {
                col++;
           }
        }
        StockSpace newsquare = getStockSpace (row, col);
        prepareMove(company, oldsquare, newsquare);
    }

    /** Replaces moveDown() for linear stockmarkets.
     * As we don't yet need a generic version, this default method does nothing.
     * SOH overrides it, as it needs a special version.
     * @param company Company of which shares are sold
     * @param seller The selling entity (player or company)
     * @param numberOfSpaces The number of spaces to move.
     */
    protected void moveLeft (PublicCompany company, Owner seller, int numberOfSpaces) {

    }

    protected void moveRightOrUp(PublicCompany company) {
        moveRightOrUp (company, 1);
    }

    public void moveRightOrUp(PublicCompany company, int numberOfMoves) {
        /* Ignore the amount for now */
        StockSpace oldsquare = company.getCurrentSpace();
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();
        for (int i=0; i<numberOfMoves; i++) {
            if (col < numCols - 1 && !getStockSpace(row, col).isLeftOfLedge()
                    && (getStockSpace(row, col + 1)) != null) {
                col++;
            } else if (row > 0
                    && (getStockSpace(row - 1, col)) != null) {
                row--;
            }
        }
        StockSpace newsquare = getStockSpace (row, col);
        prepareMove(company, oldsquare, newsquare);
    }

    protected void moveLeftOrDown(PublicCompany company) {
        moveLeftOrDown(company, 1);
    }

    public void moveLeftOrDown(PublicCompany company, int numberOfMoves) {
        StockSpace oldsquare = company.getCurrentSpace();
        StockSpace newsquare = oldsquare;
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();
        for (int i=0; i<numberOfMoves; i++) {
            if (col > 0 &&
                    (newsquare = getStockSpace(row, col - 1)) != null) {
                col--;
            } else if (row < numRows - 1 &&
                    (newsquare = getStockSpace(row + 1, col)) != null) {
                row++;
            }
        }
        prepareMove(company, oldsquare, newsquare);
    }

    protected void prepareMove(PublicCompany company, StockSpace from,
                               StockSpace to) {
        // To be written to a log file in the future.
        if (from != null && from == to) {
            ReportBuffer.add(this, LocalText.getText("PRICE_STAYS_LOG",
                    company.getId(),
                    Bank.format(this, from.getPrice()),
                    from.getId()));
            return;
        } else if (from == null && to != null) {

        } else if (from != null && to != null) {
            ReportBuffer.add(this, LocalText.getText("PRICE_MOVES_LOG",
                    company.getId(),
                    Bank.format(this, from.getPrice()),
                    from.getId(),
                    Bank.format(this, to.getPrice()),
                    to.getId()));

            /* Check for rails.game closure */
            if (to.endsGame()) {
                ReportBuffer.add(this, LocalText.getText("GAME_OVER"));
                getRoot().getGameManager().registerMaxedSharePrice(company, to);
            }

        }
        company.setCurrentSpace(to);

        if (to != null) {
            to.addToken(company);
        }
        if (from != null) {
            from.removeToken(company);
        }
    }

    // FIXME: The StockSpace changes have to update the players worth
    // thus link the state of company space to the players worth

    /**
     * Return start prices as list of prices
     */
    public List<Integer> getStartPrices() {
        List<Integer> prices = Lists.newArrayList();
        for (StockSpace space : startSpaces) {
            prices.add(space.getPrice());
        }
        return prices;
    }

    /**
     * Return start prices as an sorted set of stockspaces
     */
    public ImmutableSortedSet<StockSpace> getStartSpaces() {
        return ImmutableSortedSet.copyOf(startSpaces);
    }

    public StockSpace getStartSpace(int price) {
        for (StockSpace space : startSpaces) {
            if (space.getPrice() == price) return space;
        }
        return null;
    }

    /**
     * @return number of columns
     */
    public int getNumberOfColumns() {
        return numCols;
    }

    /**
     * @return number of rows
     */
    public int getNumberOfRows() {
        return numRows;
    }

    public StockMarketModel getMarketModel() {
        return marketModel;
    }

}
