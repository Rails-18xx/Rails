package net.sf.rails.game;

import net.sf.rails.algorithms.NetworkEdge;
import net.sf.rails.algorithms.NetworkGraph;
import net.sf.rails.algorithms.NetworkIterator;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.game.state.Owner;
import net.sf.rails.ui.swing.ORUIManager;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Routes {

    private static final Logger log = LoggerFactory.getLogger(Routes.class);

    /**
     * Create a map with all tokenable Stops for a PublicCompany,
     * and their distances to a valid start base token.
     * @param root Root
     * @param company Currently active PublicCompany
     * @param includeStartHex True if both the start and final hex must be counted.
     * @param toHomeOnly True if only the distance to a home hex counts.
     * @return A HashMap with tokenable stops as keys and the distances as values.
     */
    public static Map<Stop, Integer> getTokenLayRouteDistances(
            RailsRoot root, PublicCompany company,
            boolean includeStartHex, boolean toHomeOnly) {

        Map<Stop, Integer> stopsAndDistancesMap = new HashMap<>();

        NetworkGraph mapGraph = NetworkGraph.createMapGraph(root);
        NetworkGraph companyGraph =
                NetworkGraph.createRouteGraph(mapGraph, company, false, false);
        SimpleGraph<NetworkVertex, NetworkEdge> graph = companyGraph.getGraph();

        List<Stop> usableBases = new ArrayList<>();

        for (BaseToken token : company.getLaidBaseTokens()) {
            Stop stop = ((Stop)token.getOwner());
            MapHex hex = stop.getParent();
            if (!toHomeOnly || company.getHomeHexes().contains(hex)) {
                usableBases.add (stop);
            }
        }

        Map<MapHex, Integer> beenThere = new HashMap<>();

        for (Stop baseTokenStop : usableBases) {
            MapHex tokenedHex = baseTokenStop.getParent();
            Station station = baseTokenStop.getRelatedStation();
            NetworkVertex startVertex = mapGraph.getVertex(tokenedHex, station);
            log.debug(">>>>> Start search from base at {}", tokenedHex);

            NetworkIterator iterator = new NetworkIterator(graph, startVertex);
            int edges = 0;
            boolean skipUntilStartVertex = false;

            NetworkVertex prev = startVertex;
            while (iterator.hasNext()) {
                NetworkVertex item = iterator.next();
                log.debug("      Checking {}", item);
                MapHex hex = item.getHex();
                if (skipUntilStartVertex && hex != tokenedHex) continue;
                if (hex == tokenedHex) {
                    log.debug("Continue search from base at {}", tokenedHex);
                    skipUntilStartVertex = false;
                }
                if (item.isSide() && !hex.equals(prev.getHex())) {
                    if (hex == prev.getHex()) continue;
                    if (hex.equals(startVertex.getHex())) {
                        edges = 0;
                    } else if (!beenThere.containsKey(hex)){
                        edges++;
                        beenThere.put(hex, edges);
                    } else if (beenThere.containsKey(hex) && beenThere.get(hex) > edges+1) {
                        edges++;
                        beenThere.put(hex, edges);
                    } else if (beenThere.containsKey(hex)) {
                        edges = beenThere.get(hex);
                    } else {
                        continue;
                    }
                    log.debug("~~~~~ Start: {}  Edge: {} count={}", startVertex, item.getIdentifier(), edges);
                } else if (item.isStation()) {
                    log.debug("===== Start={} End={} Sides={}", startVertex,
                            item.getIdentifier().replaceFirst("-", ""), edges);
                    Stop stop = item.getStop();
                    if (stop.hasTokenOf(company)) {
                        skipUntilStartVertex = true;
                        continue;
                    }
                    if (beenThere.containsKey(hex)) edges = beenThere.get(hex);
                    if (stop.isTokenableFor(company)) {
                        log.debug("+++++ Found {} edges from {} to {}", edges, baseTokenStop, item.getStop());
                        if (!stopsAndDistancesMap.containsKey(stop)
                                || edges < stopsAndDistancesMap.get(stop)) {
                            int distance = includeStartHex ? edges+1 : edges;
                            stopsAndDistancesMap.put (stop, distance);
                            log.debug("Found distance {} from {} to {}", distance, baseTokenStop, stop);
                        }
                    }
                }
                prev = item;
            }
        }
        return stopsAndDistancesMap;
    }
}
