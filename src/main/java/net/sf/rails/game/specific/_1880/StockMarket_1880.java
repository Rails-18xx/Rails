/**
 * 
 */
package net.sf.rails.game.specific._1880;

import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.StockMarket;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.financial.StockSpaceType;
import net.sf.rails.game.state.IntegerState;

import com.google.common.collect.Iterables;

/**
 * @author Martin
 * 
 */

// FIXME: This seems to be a non-finalized version, fix it for Rails 2.0
public class StockMarket_1880 extends StockMarket {

   

    public IntegerState parPlace_100;

    public IntegerState parPlace_90;

    public IntegerState parPlace_80;

    public IntegerState parPlace_70;
    
  

    /**
     * 
     */
   
    public StockMarket_1880(RailsRoot parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }
    
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

            StockSpace space =
                    StockSpace.create(this, name, Integer.parseInt(price), type);
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

        int[] startPrices = new int[startSpaces.size()];

        for (int i = 0; i < startPrices.length; i++) {
            startPrices[i] = Iterables.get(startSpaces,i).getPrice();
        }

        stockChart = new StockSpace[numRows][numCols];
        for (StockSpace space : stockChartSpaces.values()) {
            stockChart[space.getRow()][space.getColumn()] = space;
        }

        upOrDownRight = tag.getChild("UpOrDownRight") != null;
    }

    @Override
    public void finishConfiguration(RailsRoot root) {
                
        for (PublicCompany_1880 company : PublicCompany_1880.getPublicCompanies(getRoot().getCompanyManager())) {
            if (!company.hasStarted() && company.getStartSpace() != null) {
                company.getStartSpace().addFixedStartPrice(company);
            }
        }
    }
}
