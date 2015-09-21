package net.sf.rails.game.special;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.SimpleGraph;

import net.sf.rails.algorithms.NetworkEdge;
import net.sf.rails.algorithms.NetworkGraph;
import net.sf.rails.algorithms.NetworkGraphModifier;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.util.Util;


public class SpecialRight extends SpecialProperty implements NetworkGraphModifier {

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
    public void finishConfiguration (RailsRoot root) throws ConfigurationException {
        super.finishConfiguration(root);
        
        // add them to the call list of the RevenueManager
        root.getRevenueManager().addGraphModifier(this);
        
        if (locationNames != null) {
            locations = new ArrayList<MapHex>();
            MapManager mmgr = root.getMapManager();
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
        if (cost > 0) b.append(" for ").append(Bank.format(this, cost));
        return b.toString();
    }
    
    @Override
    public String toMenu() {
        return LocalText.getText("BuyRight",
                rightName,
                Bank.format(this, cost));
    }
    
    public String getInfo() {
        return toMenu();
    }

    @Override
    public void modifyMapGraph(NetworkGraph mapGraph) {
        // Do nothing
    }

    @Override
    public void modifyRouteGraph(NetworkGraph routeGraph, PublicCompany company) {
        // 1. check operating company if it has the right then it is excluded from the removal
        // TODO: Only use one right for all companies instead of one per company
        if (this.getOriginalCompany() != company || company.hasRight(this)) return;
        
        SimpleGraph<NetworkVertex, NetworkEdge> graph = routeGraph.getGraph();
        
        // 2. find vertices to hex and remove the station
        Set<NetworkVertex> verticesToRemove = NetworkVertex.getVerticesByHexes(graph.vertexSet(), locations);
        // 3 ... and remove them from the graph
        graph.removeAllVertices(verticesToRemove);
    }

 

}
