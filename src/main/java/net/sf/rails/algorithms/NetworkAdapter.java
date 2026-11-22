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
        // *** ADD LOGGING ***
        boolean willRebuild = (routeGraph == null || company != this.company || addHQ != this.addHQ);
      //  log.error(">>> NetworkAdapter.getRouteGraphCached called for {}. WillRebuild = {}",
        //        (company != null ? company.getId() : "null"), willRebuild);
        // *** END LOGGING ***

        if (willRebuild) {

            // [AI TMR FIX] Always call getRouteGraph() directly.
            // This forces it to call getMapGraph() internally, which rebuilds
            // the mapGraph from the live (and temporarily modified) game state.
            // This prevents using a stale mapGraph cached from a previous simulation.

//            log.error(">>> NetworkAdapter.getRouteGraphCached: Rebuilding graphs by calling getRouteGraph directly.");
            getRouteGraph(company, addHQ, true); // This line builds a fresh mapGraph AND routeGraph

            // Update context after rebuild
            this.company = company;
            this.addHQ = addHQ;
        } else {
        //    log.error(">>> NetworkAdapter.getRouteGraphCached: RETURNING CACHED routeGraph for {}",
        //            (company != null ? company.getId() : "null"));
        }
        return routeGraph;
    }

    public NetworkGraph getRevenueGraph(PublicCompany company,
            Collection<NetworkVertex> protectedVertices) {
        if (revenueGraph == null) {
            revenueGraph = NetworkGraph.createOptimizedGraph(getRouteGraphCached(company, false),
                    protectedVertices);
    //        log.debug("RevenueGraph created");
        }

        return revenueGraph;
    }

    public NetworkMultigraph getMultigraph(PublicCompany company,
            Collection<NetworkVertex> protectedVertices) {
        if (multiGraph == null) {
            multiGraph = NetworkMultigraph.create(
                    getRevenueGraph(company, protectedVertices), protectedVertices);
   //         log.debug("MultiGraph created");
        }
        return multiGraph;
    }

    public void clearGraphCache() {
     //   log.debug("Clearing internal graph references (mapGraph, routeGraph, etc.)");
        this.mapGraph = null;
        this.routeGraph = null;
        this.revenueGraph = null;
        this.multiGraph = null;
        // Also reset company/addHQ context if needed
        this.company = null;
        this.addHQ = false; // Reset to default
    }

}
