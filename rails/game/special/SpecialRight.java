/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialRight.java,v 1.19 2010/05/05 21:37:18 evos Exp $ */
package rails.game.special;

import java.util.*;

import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueBonus;
import rails.algorithms.RevenueStaticModifier;
import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.*;
import rails.util.*;

public class SpecialRight extends SpecialProperty implements RevenueStaticModifier {

    /** The public company of which a share can be obtained. */
    protected String rightName;
    protected String rightDefaultValue;
    protected String rightValue;
    protected int cost = 0;
    protected String locationNames;
    protected List<MapHex> locations;

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

        Tag rightTag = tag.getChild("SpecialRight");
        if (rightTag == null) {
            throw new ConfigurationException("<SpecialRight> tag missing");
        }

        rightName = rightTag.getAttributeAsString("name");
        if (!Util.hasValue(rightName))
            throw new ConfigurationException(
                    "SpecialRight: no Right name specified");
        
        rightDefaultValue = rightValue = rightTag.getAttributeAsString("defaultValue", null);

        cost = rightTag.getAttributeAsInteger("cost", 0);
        
        locationNames = rightTag.getAttributeAsString("location", null);
    }

    @Override
    public void finishConfiguration (GameManagerI gameManager) throws ConfigurationException {
        super.finishConfiguration(gameManager);
        
        // add them to the call list of the RevenueManager
        gameManager.getRevenueManager().addStaticModifier(this);
        
        if (locationNames != null) {
            locations = new ArrayList<MapHex>();
            MapManager mmgr = gameManager.getMapManager();
            MapHex hex;
            for (String hexName : locationNames.split(",")) {
                hex = mmgr.getHex(hexName);
                if (hex == null) {
                    throw new ConfigurationException ("Unknown hex '"+hexName+"' for Special Right");
                }
                locations.add (hex);
            }
        }
    }
    
    public boolean isExecutionable() {

        return originalCompany.getPortfolio().getOwner() instanceof Player;
    }

 
    public String getName() {
        return rightName;
    }

    public String getDefaultValue() {
        return rightDefaultValue;
    }

    public String getValue() {
        return rightValue;
    }

    public void setValue(String rightValue) {
        this.rightValue = rightValue;
    }

    public int getCost() {
        return cost;
    }

    public String getLocationNames() {
        return locationNames;
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(cost > 0 ? "Buy '" : "Get '").append(rightName).append("'");
        if (locationNames != null) b.append(" at ").append(locationNames);
        if (cost > 0) b.append(" for ").append(Bank.format(cost));
        return b.toString();
    }
    
    @Override
    public String toMenu() {
        return LocalText.getText("BuyRight",
                rightName,
                Bank.format(cost));
    }
    
    public String getInfo() {
        return toMenu();
    }

    /** 
     *  modify revenue calculation of the 
     *  TODO: if owner would be known or only one rights object pretty print would be possible
     */
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        // 1. check operating company if it has the right then it is excluded from the removal
        if (revenueAdapter.getCompany().hasRight(rightName)) return false;
        
        // 2. find vertices to hex and remove those
        Set<NetworkVertex> verticesToRemove = NetworkVertex.getVerticesByHexes(revenueAdapter.getVertices(), locations);
        revenueAdapter.getGraph().removeAllVertices(verticesToRemove);
        
        // nothing to print, as the owner is unknown
        return false;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        // nothing to print
        return null;
    }
}
