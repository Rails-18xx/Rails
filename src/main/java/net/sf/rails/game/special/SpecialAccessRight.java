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


public class SpecialAccessRight extends SpecialRight implements NetworkGraphModifier {

    /** The public company of which a share can be obtained. */
    private String rightDefaultValue;
    private String rightValue;
    private String locationNames;
    private List<MapHex> locations;


    /**
     * Used by Configure (via reflection) only
     */
    public SpecialAccessRight(RailsItem parent, String id) {
        super(parent, id);
        rightType = "access";
    }

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {
        super.configureFromXML(tag);

        rightDefaultValue = rightValue = rightTag.getAttributeAsString("defaultValue", null);

        locationNames = rightTag.getAttributeAsString("location", null);
    }

    public void setRoot(RailsRoot root) { }

    @Override
    public void finishConfiguration (RailsRoot root) throws ConfigurationException {
        super.finishConfiguration(root);

        // add them to the call list of the RevenueManager
        root.getRevenueManager().addGraphModifier(this);

        if (locationNames != null) {
            locations = new ArrayList<>();
            MapManager mmgr = root.getMapManager();
            MapHex hex;
            for (String hexName : locationNames.split(",")) {
                hex = mmgr.getHex(hexName);
                if (hex == null) {
                    throw new ConfigurationException ("Unknown hex '"+hexName+"' for SpecialAccessRight");
                }
                locations.add (hex);
            }
        }
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

    public String getLocationNames() {
        return locationNames;
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    @Override
    public String toMenu() {
        StringBuilder b = new StringBuilder();
        b.append(getCost() > 0 ? "Buy '" : "Get '").append(getName()).append("'");
        if (locationNames != null) b.append(" at ").append(locationNames);
        if (getCost() > 0) b.append(" for ").append(Bank.format(this, getCost()));
        return b.toString();
    }

    /*
    @Override
    public String toMenu() {
        return LocalText.getText("BuyRight",
                getName(),
                Bank.format(this, getCost()));
    }*/

    public String toString() {
        return getName();
    }

    public String toText() {
        return getName();
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
