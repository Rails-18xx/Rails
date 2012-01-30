package rails.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rails.common.LocalText;
import rails.common.parser.ConfigurableComponentI;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.state.GameItem;
import rails.game.state.BooleanState;

public class StockMarket extends GameItem implements ConfigurableComponentI {

     /**
     *  This is the name by which the CompanyManager should be registered with
     * the ComponentManager.
     */
    public static final String COMPONENT_NAME = "StockMarket";
    
    
    protected HashMap<String, StockSpaceType> stockSpaceTypes =
        new HashMap<String, StockSpaceType>();
    protected HashMap<String, StockSpace> stockChartSpaces =
        new HashMap<String, StockSpace>();
    
    protected StockSpace stockChart[][];
    protected StockSpace currentSquare;
    protected int numRows = 0;
    protected int numCols = 0;
    protected ArrayList<StockSpace> startSpaces = new ArrayList<StockSpace>();
    protected int[] startPrices;
    protected StockSpaceType defaultType;
    
    GameManager gameManager;

    /* Game-specific flags */
    protected boolean upOrDownRight = false; /*
     * Sold out and at top: go down
     * right (1870)
     */

    /* States */
    /** GameOver becomes true if a stock market square is reached that is marked as such */ 
    protected BooleanState gameOver = BooleanState.create(this, "GameOver", false);

    ArrayList<PublicCertificate> ipoPile;

    public static final String DEFAULT = "default";

    public StockMarket() {

    }

    /**
     * @see rails.common.parser.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {

        // Define a default stockspace type with colour white
        defaultType = new StockSpaceType(DEFAULT, StockSpaceType.WHITE);
        stockSpaceTypes.put (DEFAULT, defaultType);

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

        startPrices = new int[startSpaces.size()];
        for (int i = 0; i < startPrices.length; i++) {
            startPrices[i] = (startSpaces.get(i)).getPrice();
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
    public void finishConfiguration (GameManager gameManager) {

        this.gameManager = gameManager;
        
        for (PublicCompany comp : gameManager.getCompanyManager().getAllPublicCompanies()) {
            if (!comp.hasStarted() && comp.getStartSpace() != null) {
                comp.getStartSpace().addFixedStartPrice(comp);
            }
        }

    }

    /**
     * @return
     */
    public StockSpace[][] getStockChart() {
        return stockChart;
    }

    public StockSpace getStockSpace(int row, int col) {
        if (row >= 0 && row < numRows && col >= 0 && col < numCols) {
            return stockChart[row][col];
        } else {
            return null;
        }
    }

    public StockSpace getStockSpace(String name) {
        return (StockSpace) stockChartSpaces.get(name);
    }

    /*--- Actions ---*/

    public void start(PublicCompany company, StockSpace price) {
        prepareMove(company, null, price);
    }

    public void payOut(PublicCompany company) {
        moveRightOrUp(company);
    }

    public void withhold(PublicCompany company) {
        moveLeftOrDown(company);
    }

    public void sell(PublicCompany company, int numberOfSpaces) {
        moveDown(company, numberOfSpaces);
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

    public void close (PublicCompany company) {
        prepareMove(company, company.getCurrentSpace(), null);
    }

    protected void moveDown(PublicCompany company, int numberOfSpaces) {
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

    protected void moveRightOrUp(PublicCompany company) {
        /* Ignore the amount for now */
        StockSpace oldsquare = company.getCurrentSpace();
        StockSpace newsquare = oldsquare;
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();
        if (col < numCols - 1 && !oldsquare.isLeftOfLedge()
                && (newsquare = getStockSpace(row, col + 1)) != null) {}
        else if (row > 0
                && (newsquare = getStockSpace(row - 1, col)) != null) {}
        prepareMove(company, oldsquare, newsquare);
    }

    protected void moveLeftOrDown(PublicCompany company) {
        StockSpace oldsquare = company.getCurrentSpace();
        StockSpace newsquare = oldsquare;
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();
        if (col > 0 && (newsquare = getStockSpace(row, col - 1)) != null) {}
        else if (row < numRows - 1 &&
                (newsquare = getStockSpace(row + 1, col)) != null) {}
        else {
            newsquare = oldsquare;
        }
        prepareMove(company, oldsquare, newsquare);
    }

    protected void prepareMove(PublicCompany company, StockSpace from,
            StockSpace to) {
        // To be written to a log file in the future.
        if (from != null && from == to) {
            ReportBuffer.add(LocalText.getText("PRICE_STAYS_LOG",
                    company.getId(),
                    Bank.format(from.getPrice()),
                    from.getName() ));
            return;
        } else if (from == null && to != null) {
            ;
        } else if (from != null && to != null) {
            ReportBuffer.add(LocalText.getText("PRICE_MOVES_LOG",
                    company.getId(),
                    Bank.format(from.getPrice()),
                    from.getName(),
                    Bank.format(to.getPrice()),
                    to.getName() ));

            /* Check for rails.game closure */
            if (to.endsGame()) {
                ReportBuffer.add(LocalText.getText("GAME_OVER"));
                gameManager.registerMaxedSharePrice(company, to);
            }

        }
        company.setCurrentSpace(to);
        
        // the following 2 commands replaced: new PriceTokenMove(company, from, to, this);
        to.addToken(company);
        from.removeToken(company);
    }
    
    // TODO: Check what states effect players worth and link those
//    public void processMove(PublicCompany company, StockSpace from,
//            StockSpace to) {
//        if (from != null) from.removeToken(company);
//        if (to != null) to.addToken(company);
//        company.updatePlayersWorth();
//    }
//
//    public void processMoveToStackPosition(PublicCompany company, StockSpace from,
//            StockSpace to, int toStackPosition) {
//        if (from != null) from.removeToken(company);
//        if (to != null) to.addTokenAtStackPosition(company, toStackPosition);
//        company.updatePlayersWorth();
//    }

    /**
     * @return
     */
    public List<StockSpace> getStartSpaces() {
        return startSpaces;
    }

    /**
     * Return start prices as an int array. Note: this array is NOT sorted.
     *
     * @return
     */
    public int[] getStartPrices() {
        return startPrices;
    }

    public StockSpace getStartSpace(int price) {
        for (StockSpace square : startSpaces) {
            if (square.getPrice() == price) return square;
        }
        return null;
    }

    public PublicCertificate removeShareFromPile(PublicCertificate stock) {
        if (ipoPile.contains(stock)) {
            int index = ipoPile.lastIndexOf(stock);
            stock = ipoPile.get(index);
            ipoPile.remove(index);
            return stock;
        } else {
            return null;
        }

    }

    /**
     * @return
     */
    public int getNumberOfColumns() {
        return numCols;
    }

    /**
     * @return
     */
    public int getNumberOfRows() {
        return numRows;
    }

}
