/**
 * 
 */
package rails.game.specific._1880;

import java.util.List;

import rails.game.GameManagerI;
import rails.game.PublicCompanyI;
import rails.game.StockMarket;
import rails.game.StockSpace;
import rails.game.StockSpaceI;
import rails.game.StockSpaceType;
import rails.game.StockSpaceTypeI;
import rails.game.state.IntegerState;
import rails.common.LocalText;
import rails.common.parser.*;

/**
 * @author Martin
 * 
 */
public class StockMarket_1880 extends StockMarket {

    public IntegerState parPlace_100;

    public IntegerState parPlace_90;

    public IntegerState parPlace_80;

    public IntegerState parPlace_70;

    protected int[] parSlots = { 4, 4, 4, 4 };

    GameManagerI gameManager;

    /**
     * 
     */
    public StockMarket_1880() {
        // TODO Auto-generated constructor stub
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {
        // Define a default stockspace type with colour white
        defaultType = new StockSpaceType(DEFAULT, StockSpaceType.WHITE);
        stockSpaceTypes.put(DEFAULT, defaultType);

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
                // Could be made even more Generic by using Revenue as Param and
                // a numer as integer Attribute...
                if (typeTag.getChild("Revenue5") != null)
                    type.setAddRevenue(5);
                if (typeTag.getChild("Revenue10") != null)
                    type.setAddRevenue(10);
                if (typeTag.getChild("Revenue15") != null)
                    type.setAddRevenue(15);
                if (typeTag.getChild("Revenue20") != null)
                    type.setAddRevenue(20);
                if (typeTag.getChild("Revenue40") != null)
                    type.setAddRevenue(40);

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

    @Override
    public void finishConfiguration(GameManagerI gameManager) {

        this.gameManager = gameManager;

        for (PublicCompanyI comp : gameManager.getCompanyManager().getAllPublicCompanies()) {
            if (!comp.hasStarted() && comp.getStartSpace() != null) {
                comp.getStartSpace().addFixedStartPrice(comp);
            }
        }
    }

    /**
     * @return the parSlots
     */
    public int[] getParSlots() {
        return parSlots;
    }

    /**
     * @param parSlots the parSlots to set
     */
    public void setParSlots(int[] parSlots) {
        this.parSlots = parSlots;
    }

    public boolean setParSlot(int price, int position) {
        int position2 = 0;
        switch (price) {
        case 100:
            position2 = 3;
            break;
        case 90:
            position2 = 2;
            break;
        case 80:
            position2 = 1;
            break;
        case 70:
            position2 = 0;
            break;
        default:
            return false;
        }
        if (this.parSlots[position2] > 0) {
            this.parSlots[position2] = this.parSlots[position2] - 1;
            return true;
        } else {
            return false;
        }
    }

    public boolean setParSlot(int price) {
        int position = 0;
        switch (price) {
        case 100:
            position = 3;
            break;
        case 90:
            position = 2;
            break;
        case 80:
            position = 1;
            break;
        case 70:
            position = 0;
            break;
        default:
            return false;
        }
        if (this.parSlots[position] > 0) {
            this.parSlots[position] = this.parSlots[position] - 1;
            return true;
        } else {
            return false;
        }
    }

    public boolean getParSlot(int price) {
        int position = 0;
        switch (price) {
        case 100:
            position = 3;
            break;
        case 90:
            position = 2;
            break;
        case 80:
            position = 1;
            break;
        case 70:
            position = 0;
            break;
        default:
            return false;
        }
        if (this.parSlots[position] > 0) {
            return true;
        } else {
            return false;
        }

    }

    public boolean getParSlot(int price, int position) {
        return true;
    }
}
