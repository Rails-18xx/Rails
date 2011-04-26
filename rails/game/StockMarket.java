/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/StockMarket.java,v 1.27 2010/03/10 17:26:45 stefanfrey Exp $ */
package rails.game;

import java.util.*;

import rails.game.move.PriceTokenMove;
import rails.game.state.BooleanState;
import rails.util.LocalText;
import rails.util.Tag;

public class StockMarket implements StockMarketI, ConfigurableComponentI {

    protected HashMap<String, StockSpaceTypeI> stockSpaceTypes =
        new HashMap<String, StockSpaceTypeI>();
    protected HashMap<String, StockSpaceI> stockChartSpaces =
        new HashMap<String, StockSpaceI>();
    protected StockSpaceI stockChart[][];
    protected StockSpaceI currentSquare;
    protected int numRows = 0;
    protected int numCols = 0;
    protected ArrayList<StockSpaceI> startSpaces = new ArrayList<StockSpaceI>();
    protected int[] startPrices;
    protected StockSpaceTypeI defaultType;
    
    GameManagerI gameManager;

    /* Game-specific flags */
    protected boolean upOrDownRight = false; /*
     * Sold out and at top: go down
     * right (1870)
     */

    /* States */
    /** GameOver becomes true if a stock market square is reached that is marked as such */ 
    protected BooleanState gameOver = new BooleanState ("GameOver", false);

    ArrayList<PublicCertificate> ipoPile;

    public static final String DEFAULT = "default";

    public StockMarket() {

    }

    /**
     * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {

        // Define a default stockspace type with colour white
        defaultType = new StockSpaceType(DEFAULT, StockSpaceType.WHITE);
        stockSpaceTypes.put (DEFAULT, defaultType);

        /* Read and configure the stock market space types */
        List<Tag> typeTags = tag.getChildren(StockSpaceTypeI.ELEMENT_ID);

        if (typeTags != null) {
            for (Tag typeTag : typeTags) {
                /* Extract the attributes of the Stock space type */
                String name =
                    typeTag.getAttributeAsString(StockSpaceTypeI.NAME_TAG);
                if (name == null) {
                    throw new ConfigurationException(
                            LocalText.getText("UnnamedStockSpaceType"));
                }
                String colour =
                    typeTag.getAttributeAsString(StockSpaceTypeI.COLOUR_TAG);

                /* Check for duplicates */
                if (stockSpaceTypes.get(name) != null) {
                    throw new ConfigurationException(LocalText.getText(
                            "StockSpaceTypeConfiguredTwice", name));
                }

                /* Create the type */
                StockSpaceTypeI type = new StockSpaceType(name, colour);
                stockSpaceTypes.put(name, type);

                // Check the stock space type flags
                type.setNoBuyLimit(typeTag.getChild(StockSpaceTypeI.NO_BUY_LIMIT_TAG) != null);
                type.setNoCertLimit(typeTag.getChild(StockSpaceTypeI.NO_CERT_LIMIT_TAG) != null);
                type.setNoHoldLimit(typeTag.getChild(StockSpaceTypeI.NO_HOLD_LIMIT_TAG) != null);
            }
        }

        /* Read and configure the stock market spaces */
        List<Tag> spaceTags = tag.getChildren(StockSpaceI.ELEMENT_ID);
        StockSpaceTypeI type;
        int row, col;
        for (Tag spaceTag : spaceTags) {
            type = null;

            // Extract the attributes of the Stock space
            String name = spaceTag.getAttributeAsString(StockSpaceI.NAME_TAG);
            if (name == null) {
                throw new ConfigurationException(
                        LocalText.getText("UnnamedStockSpace"));
            }
            String price = spaceTag.getAttributeAsString(StockSpaceI.PRICE_TAG);
            if (price == null) {
                throw new ConfigurationException(LocalText.getText(
                        "StockSpaceHasNoPrice", name));
            }
            String typeName =
                spaceTag.getAttributeAsString(StockSpaceI.TYPE_TAG);
            if (typeName != null
                    && (type = stockSpaceTypes.get(typeName)) == null) {
                throw new ConfigurationException(LocalText.getText(
                        "StockSpaceTypeUndefined", type));
            }
            if (type == null) type = defaultType;

            if (stockChartSpaces.get(name) != null) {
                throw new ConfigurationException(LocalText.getText(
                        "StockSpaceIsConfiguredTwice", name));
            }

            StockSpaceI space =
                new StockSpace(name, Integer.parseInt(price), type);
            stockChartSpaces.put(name, space);

            row = Integer.parseInt(name.substring(1));
            col = (name.toUpperCase().charAt(0) - '@');
            if (row > numRows) numRows = row;
            if (col > numCols) numCols = col;

            // Loop through the stock space flags
            if (spaceTag.getChild(StockSpaceI.START_SPACE_TAG) != null) {
                space.setStart(true);
                startSpaces.add(space);
            }
            space.setClosesCompany(spaceTag.getChild(StockSpaceI.CLOSES_COMPANY_TAG) != null);
            space.setEndsGame(spaceTag.getChild(StockSpaceI.GAME_OVER_TAG) != null);
            space.setBelowLedge(spaceTag.getChild(StockSpaceI.BELOW_LEDGE_TAG) != null);
            space.setLeftOfLedge(spaceTag.getChild(StockSpaceI.LEFT_OF_LEDGE_TAG) != null);

        }

        startPrices = new int[startSpaces.size()];
        for (int i = 0; i < startPrices.length; i++) {
            startPrices[i] = (startSpaces.get(i)).getPrice();
        }

        stockChart = new StockSpaceI[numRows][numCols];
        for (StockSpaceI space : stockChartSpaces.values()) {
            stockChart[space.getRow()][space.getColumn()] = space;
        }

        upOrDownRight = tag.getChild("UpOrDownRight") != null;

    }

    /**
     * Final initialisations, to be called after all XML processing is complete.
     * The purpose is to register fixed company start prices.
     */
    public void finishConfiguration (GameManagerI gameManager) {

        this.gameManager = gameManager;
        
        for (PublicCompanyI comp : gameManager.getCompanyManager().getAllPublicCompanies()) {
            if (!comp.hasStarted() && comp.getStartSpace() != null) {
                comp.getStartSpace().addFixedStartPrice(comp);
            }
        }

    }

    /**
     * @return
     */
    public StockSpaceI[][] getStockChart() {
        return stockChart;
    }

    public StockSpaceI getStockSpace(int row, int col) {
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

    public void start(PublicCompanyI company, StockSpaceI price) {
        prepareMove(company, null, price);
    }

    public void payOut(PublicCompanyI company) {
        moveRightOrUp(company);
    }

    public void withhold(PublicCompanyI company) {
        moveLeftOrDown(company);
    }

    public void sell(PublicCompanyI company, int numberOfSpaces) {
        moveDown(company, numberOfSpaces);
    }

    public void soldOut(PublicCompanyI company) {
        moveUp(company);
    }

    public void moveUp(PublicCompanyI company) {
        StockSpaceI oldsquare = company.getCurrentSpace();
        StockSpaceI newsquare = oldsquare;
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();
        if (row > 0) {
            newsquare = getStockSpace(row - 1, col);
        } else if (upOrDownRight && col < numCols - 1) {
            newsquare = getStockSpace(row + 1, col + 1);
        }
        if (newsquare != null) prepareMove(company, oldsquare, newsquare);
    }

    public void close (PublicCompanyI company) {
        prepareMove(company, company.getCurrentSpace(), null);
    }

    protected void moveDown(PublicCompanyI company, int numberOfSpaces) {
        StockSpaceI oldsquare = company.getCurrentSpace();
        StockSpaceI newsquare = oldsquare;
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

    protected void moveRightOrUp(PublicCompanyI company) {
        /* Ignore the amount for now */
        StockSpaceI oldsquare = company.getCurrentSpace();
        StockSpaceI newsquare = oldsquare;
        int row = oldsquare.getRow();
        int col = oldsquare.getColumn();
        if (col < numCols - 1 && !oldsquare.isLeftOfLedge()
                && (newsquare = getStockSpace(row, col + 1)) != null) {}
        else if (row > 0
                && (newsquare = getStockSpace(row - 1, col)) != null) {}
        prepareMove(company, oldsquare, newsquare);
    }

    protected void moveLeftOrDown(PublicCompanyI company) {
        StockSpaceI oldsquare = company.getCurrentSpace();
        StockSpaceI newsquare = oldsquare;
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

    protected void prepareMove(PublicCompanyI company, StockSpaceI from,
            StockSpaceI to) {
        // To be written to a log file in the future.
        if (from != null && from == to) {
            ReportBuffer.add(LocalText.getText("PRICE_STAYS_LOG",
                    company.getName(),
                    Bank.format(from.getPrice()),
                    from.getName() ));
            return;
        } else if (from == null && to != null) {
            ;
        } else if (from != null && to != null) {
            ReportBuffer.add(LocalText.getText("PRICE_MOVES_LOG",
                    company.getName(),
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
        new PriceTokenMove(company, from, to, this);
    }

    public void processMove(PublicCompanyI company, StockSpaceI from,
            StockSpaceI to) {
        if (from != null) from.removeToken(company);
        if (to != null) to.addToken(company);
        company.updatePlayersWorth();
    }

    public void processMoveToStackPosition(PublicCompanyI company, StockSpaceI from,
            StockSpaceI to, int toStackPosition) {
        if (from != null) from.removeToken(company);
        if (to != null) to.addTokenAtStackPosition(company, toStackPosition);
        company.updatePlayersWorth();
    }

    /**
     * @return
     */
    public List<StockSpaceI> getStartSpaces() {
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

    public StockSpaceI getStartSpace(int price) {
        for (StockSpaceI square : startSpaces) {
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
