package net.sf.rails.algorithms;

import java.util.Collection;

import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class coordinates the creation of company related graphs
 */
public class NetworkAdapter {

    private static final Logger log = LoggerFactory.getLogger(NetworkAdapter.class);

    private final RailsRoot root;

    private NetworkGraph mapGraph;
    private NetworkGraph routeGraph;
    private NetworkGraph revenueGraph;
    private NetworkMultigraph multiGraph;

    private PublicCompany company;
    private boolean addHQ;

    private NetworkAdapter(RailsRoot root) {
        this.root = root;
    }

    public static NetworkAdapter create(RailsRoot root) {
        return new NetworkAdapter(root);
    }

    public NetworkGraph getMapGraph() {
        mapGraph = NetworkGraph.createMapGraph(root);
        log.debug("MapGraph created");
        return mapGraph;
    }

    /**
     *
     * @param company
     * @param addHQ
     * @param running true for train runs, false for tile or token lay allowances
     * @return
     */
    public NetworkGraph getRouteGraph(PublicCompany company, boolean addHQ, boolean running) {
        routeGraph = NetworkGraph.createRouteGraph(getMapGraph(), company, addHQ, running);
        this.company = company;
        this.addHQ = addHQ;
        log.debug("RouteGraph created");
        return routeGraph;
    }

    public NetworkGraph getRouteGraphCached(PublicCompany company, boolean addHQ) {
        if (routeGraph == null || company != this.company || addHQ != this.addHQ) {
            if (mapGraph != null) {
                routeGraph = NetworkGraph.createRouteGraph(mapGraph, company, addHQ, true);
            } else {
                getRouteGraph(company, addHQ, true);
            }
        }
        return routeGraph;
    }

    public NetworkGraph getRevenueGraph(PublicCompany company,
            Collection<NetworkVertex> protectedVertices) {
        if (revenueGraph == null) {
            revenueGraph = NetworkGraph.createOptimizedGraph(getRouteGraphCached(company, false),
                    protectedVertices);
            log.debug("RevenueGraph created");
        }

        return revenueGraph;
    }

    public NetworkMultigraph getMultigraph(PublicCompany company,
            Collection<NetworkVertex> protectedVertices) {
        if (multiGraph == null) {
            multiGraph = NetworkMultigraph.create(
                    getRevenueGraph(company, protectedVertices), protectedVertices);
            log.debug("MultiGraph created");
        }
        return multiGraph;
    }

}
