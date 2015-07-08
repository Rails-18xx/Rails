package net.sf.rails.algorithms;

import java.util.Collection;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class coordinates the creation of company related graphs
 * 
 * TODO: NetworkAdapter should be changed to a NetworkManager and include functionality currently in NetworkGraph
 * e.g. NetworkGraphModifier
 */
public class NetworkAdapter {

    protected static Logger log =
            LoggerFactory.getLogger(NetworkAdapter.class);

    private final RailsRoot root;
    
    private NetworkAdapter(RailsRoot root) {
        this.root = root;
    }

    public static NetworkAdapter create(RailsRoot root) {
        return new NetworkAdapter(root);
    }
    
    public NetworkGraph getMapGraph() {
        return NetworkGraph.createMapGraph(root);
    }
        
    public NetworkGraph getRouteGraph(PublicCompany company, boolean addHQ) {
        return NetworkGraph.createRouteGraph(getMapGraph(), company, addHQ);
    }
    
    public NetworkGraph getRevenueGraph(PublicCompany company, Collection<NetworkVertex> protectedVertices) {
        return NetworkGraph.createOptimizedGraph(getRouteGraph(company, false), protectedVertices);
    }
    
    public NetworkMultigraph getMultigraph(PublicCompany company,
            Collection<NetworkVertex> protectedVertices) {
        return NetworkMultigraph.create(getRevenueGraph(company, protectedVertices), protectedVertices);
    }
    
}
