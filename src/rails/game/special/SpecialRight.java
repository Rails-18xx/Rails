package rails.game.special;

import java.util.*;

import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueStaticModifier;
import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.*;
import rails.game.Currency;
import rails.util.*;

public class SpecialRight extends SpecialProperty implements RevenueStaticModifier {

    /** The public company of which a share can be obtained. */
    private String rightName;
    private String rightDefaultValue;
    private String rightValue;
    private int cost = 0;
    private String locationNames;
    private List<MapHex> locations;

    /**
     * Used by Configure (via reflection) only
     */
    public SpecialRight(RailsItem parent, String id) {
        super(parent, id);
    }

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
    public void finishConfiguration (GameManager gameManager) throws ConfigurationException {
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
        // FIXME: Check if this works correctly
        // IT is better to rewrite this check
        // see ExchangeForShare
        return ((PrivateCompany)originalCompany).getOwner() instanceof Player;
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
    public String toText() {
        StringBuilder b = new StringBuilder();
        b.append(cost > 0 ? "Buy '" : "Get '").append(rightName).append("'");
        if (locationNames != null) b.append(" at ").append(locationNames);
        if (cost > 0) b.append(" for ").append(Currency.format(this, cost));
        return b.toString();
    }
    
    @Override
    public String toMenu() {
        return LocalText.getText("BuyRight",
                rightName,
                Currency.format(this, cost));
    }
    
    public String getInfo() {
        return toMenu();
    }

    /** 
     *  modify revenue calculation of the special rights
     *  TODO: if owner would be known or only one rights object pretty print would be possible
     *  TODO: check if calculation is only modified or the whole graph (including tile and token lays)
     */
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        // 1. check operating company if it has the right then it is excluded from the removal
        if (revenueAdapter.getCompany().hasRight(rightName)) return false;
        
        // 2. find vertices to hex and remove the station
        Set<NetworkVertex> verticesToRemove = NetworkVertex.getVerticesByHexes(revenueAdapter.getVertices(), locations);
        for (NetworkVertex v:verticesToRemove) {
            if (v.isStation()) revenueAdapter.getGraph().removeVertex(v);
        }
        
        // nothing to print, as the owner is unknown
        return false;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        // nothing to print
        return null;
    }
}
