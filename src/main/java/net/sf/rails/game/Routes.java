package net.sf.rails.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.sf.rails.algorithms.*;
import net.sf.rails.util.Util;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Routes {

    private static final Logger log = LoggerFactory.getLogger(Routes.class);

    /*
     * Create a map with all tokenable Stops for a PublicCompany,
     * and their distances to a valid start base token.
     * @param root Root
     * @param company Currently active PublicCompany
     * @param includeStartHex True if both the start and final hex must be counted.
     * @param toHomeOnly True if only the distance to a home hex counts.
     * @return A HashMap with tokenable stops as keys and the distances as values.
     */

    /* THIS CODE is left in for now, perhaps it can be extended to include closed loops.
    public static Map<Stop, Integer> getTokenLayRouteDistances(
            RailsRoot root, PublicCompany company,
            boolean includeStartHex, boolean toHomeOnly) {

        /* The ultimate minimal distances from each reachable and
         * tokenable stop to the nearest base token of a company.
         *//*
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
        log.debug(">>>>> Usable bases: {}", usableBases);

        /* Map from visited hexes to the minimal track distances
         * from a base token.
          *//*
        Map<MapHex, Integer> beenAtHex = new HashMap<>();
        /* Map from visited stops (stations) to the minimal
         * track distances from a base token.
         *//*
        Map<Stop, Integer> beenAtStop = new HashMap<>();

        for (Stop baseTokenStop : usableBases) {
            MapHex baseTokenHex = baseTokenStop.getParent();
            Station station = baseTokenStop.getRelatedStation();
            NetworkVertex startVertex = mapGraph.getVertex(baseTokenHex, station);
            log.debug(">>>>> Start search from base at {}", baseTokenStop);

            NetworkIterator iterator = new NetworkIterator(graph, startVertex);
            int edges = 0;
            int prevEdges = 0;
            boolean skipUntilStartVertex = false;

            NetworkVertex prev = startVertex;
            while (iterator.hasNext()) {
                NetworkVertex item = iterator.next();
                log.debug("      Checking item {}", item);
                MapHex hex = item.getHex();
                if (skipUntilStartVertex && hex != baseTokenHex) {
                    log.debug("<<<<< Skipping {} {}", item.getType(), item);
                    continue;
                }
                if (hex.equals(baseTokenHex)) {
                    log.debug("Continue search from base at {}", baseTokenStop);
                    skipUntilStartVertex = false;
                }
                if (item.isSide()) {
                    // Ignore any additional sides on the same hex
                    if(hex.equals(prev.getHex())) {
                        log.debug ("      Skipping {} on same hex {}", item, hex);
                        continue;
                    }
                    // Ignore any hex sides that have no track
                    if (!hex.getTrackSides().get(item.getSide())) {
                        log.debug ("      Skipping {}: no tracks", item);
                        continue;
                    }
                    if (hex.equals(startVertex.getHex())) {
                        Stop s = findConnectedStop(item);
                        log.debug(">>>>> Side {} is connected to stop {}",
                                item.getIdentifier(), s);
                        log.debug("      Hex {} rotation = {}", hex, hex.getCurrentTileRotation());
                        prevEdges = edges; // Setting to 0 may be wrong if the hex has multiple stations
                        edges = 0;
                        log.debug("  1   Edges set to {}", edges);
                    } else if (hex != prev.getHex() && prev.isStation()
                            && beenAtHex.containsKey(hex)) {
                        // We jumped from a station being an end point
                        // back to an edge on a hex visited before.
                        edges = beenAtHex.get(hex);
                        log.debug("  7   Hex {} edges set to {}", hex, edges);
                    } else if (!beenAtHex.containsKey(hex)) {
                        log.debug("..... At hex={} side={}: tile={} rotation={} trackSides={} or {}",
                                hex, item.getSide().getTrackPointNumber(),
                                hex.getCurrentTile(), hex.getCurrentTileRotation(),
                                hex.getTrackSides(),
                                hex.getTrackSides(hex.getCurrentTile(), hex.getCurrentTileRotation().getTrackPointNumber()));
                        edges++;
                        beenAtHex.put(hex, edges);
                        log.debug("  2   Hex {} edges set to {}", hex, edges);
                    } else if (edges > 0 && beenAtHex.get(hex) > edges+1) {
                        edges++;
                        beenAtHex.put(hex, edges);
                        log.debug("  3   Hex {} edges set to {}", hex, edges);
                    } else {
                        edges = Math.min (beenAtHex.get(hex), edges + 1);
                        beenAtHex.put(hex, edges);
                        log.debug("  4   Hex {} edges set to {}", hex, edges);
                    }/* else {
                        log.debug("<<<<< Skipping side {}", item.getIdentifier());
                        continue;
                    }*//*
                    log.debug("~~~~~ Start: {}  Edge: {} count={}",
                            startVertex, item.getIdentifier(), edges);
                } else if (item.isStation()) {
                    log.debug("===== Start={} End={} Sides={}", startVertex,
                            item.getIdentifier().replaceFirst("-", ""), edges);
                    Stop stop = item.getStop();
                    if (hex.equals(baseTokenHex) && !stop.equals(baseTokenStop)) {
                        // E.g. 1826 Paris: we may end up at a different station
                        beenAtStop.put(stop, prevEdges + 1);
                        log.debug("  +   {} is tokened by {}", stop, company);
                        log.debug("  5   Stop {} distance set to {}", stop, prevEdges + 1);
                    } else if (stop.hasTokenOf(company)) {
                        //skipUntilStartVertex = true;
                        //log.debug("      Skipping tokened stop {}", stop);
                        beenAtStop.put(stop, 0);
                        log.debug("  8   Stop {} distance set to {}", stop, 0);
                        beenAtHex.put (hex, 0);
                        log.debug("  9   Hex {} edges set to {}", hex, 0);
                        edges = 0;
                        continue;
                    } else {
                        beenAtStop.put (stop, edges);
                        log.debug("  6   Stop {} distance set to {}", stop, edges);
                    }


                    if (beenAtHex.containsKey(hex)) {
                        edges = beenAtHex.get(hex);
                    }
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

    private static Stop findConnectedStop (NetworkVertex item) {
        if (item.isStation()) {
            return item.getStop();
        } else {
            MapHex hex = item.getHex();
            Tile tile = hex.getCurrentTile();
            HexSide rotation = hex.getCurrentTileRotation();
            int toRotate = 6 - rotation.getTrackPointNumber();
            for (TrackPoint trackPoint : tile.getTracks(item.getSide().rotate(toRotate))) {
                if (trackPoint.getTrackPointType() == TrackPoint.Type.STATION) {
                    return hex.getRelatedStop((Station)trackPoint);
                }
            }
            return null;
        }
    }*/

    public Routes() {}

    LinkedList<WaitingSegment> waitingList = new LinkedList<>();

    /**
     * Create a map with all tokenable Stops for a PublicCompany,
     * and their distances to a valid start base token.
     * @param company Currently active PublicCompany
     * @param includeStartHex True if both the start and final hex must be counted.
     * @param toHomeOnly True if only the distance to a home hex counts.
     * @return A HashMap with tokenable stops as keys and the distances as values.
     */
    public Map<Stop, Integer> getTokenLayRouteDistances2 (
            PublicCompany company, boolean includeStartHex, boolean toHomeOnly){

        RailsRoot root = company.getRoot();
        NetworkGraph mapGraph = NetworkGraph.createMapGraph(root);

        waitingList.clear();

        DLLGraph graph = new DLLGraph(company, PublicCompany.FROM_HOME_ONLY);

        log.debug("Called getTokenLayRouteDistances2");

        /* The ultimate minimal distances from each reachable and
         * tokenable stop to the nearest base token of a company.
         */
        Map<Stop, Integer> cityDistancesMap = new HashMap<>();

        List<Stop> usableBases = new ArrayList<>();

        for (BaseToken token : company.getLaidBaseTokens()) {
            Stop stop = ((Stop) token.getOwner());
            MapHex hex = stop.getParent();
            if (!toHomeOnly || company.getHomeHexes().contains(hex)) {
                usableBases.add(stop);
            }
        }

        // Make startbase scanning sequence reproducible
        //log.debug(">>>>> Usable bases: {}, unsorted", usableBases);
        usableBases.sort((a, b) -> (a.getId().compareTo(b.getId())));
        log.debug(">>>>> Usable bases: {}", usableBases);

        /* Map from visited hex sides to the minimal track distances
         * from a base token.
         */
        Map<NetworkVertex, Integer> vertexDistances = new HashMap<>();

        /* Map from visited stops (stations) to the minimal
         * track distances from a base token.
         */
        Map<Stop, Integer> stopDistances = new HashMap<>();

        /* Map from visited hexes to the minimal track distances from a base token.
         * The values only serve to equalise distances of multiple trackpoints in any hex.
         */
        Map<MapHex, Integer> hexDistances = new HashMap<>();

        /* Build an object network of nodes and segments,
         * where the segments can be traversed in both directions.
         */
        for (Stop baseTokenStop : usableBases) {
            MapHex baseTokenHex = baseTokenStop.getParent();

            Station station = baseTokenStop.getRelatedStation();
            NetworkVertex startVertex = mapGraph.getVertex(baseTokenHex, station);
            log.debug(">>>>> Start search from base at {}", baseTokenStop);

            for (DLLGraph.Segment segment : graph.getSegments(baseTokenStop)) {
                pushWaitingList(new WaitingSegment(segment, startVertex, true));
            }

            hexDistances.put(baseTokenHex, 0);

            int edgeCount = 0;
            MapHex hex = baseTokenHex;
            MapHex prevHex = baseTokenHex;

            while (!waitingList.isEmpty()) {

                WaitingSegment waiting = waitingList.pop();
                DLLGraph.Segment currentSegment = waiting.getSegment();
                startVertex = waiting.getStartVertex();

                NetworkVertex prevItem = startVertex;

                currentSegment.setIterator(startVertex);
                log.debug("Iteration start of segment {} from {} to {}",
                        currentSegment, startVertex,
                        currentSegment.otherEnd(startVertex));

                if (startVertex.getStop().hasTokenOf(company)) {
                    edgeCount = 0;
                } else if (stopDistances.containsKey(startVertex.getStop())) {
                    edgeCount = stopDistances.get(startVertex.getStop());
                }

                while (currentSegment.hasNext()) {
                    NetworkVertex item = currentSegment.getNext();
                    hex = item.getHex();
                    boolean foundLowerEdgeCount = false;
                    boolean beenAtStopBefore = false;

                    log.debug("      Checking segment {} item {} {}",
                            currentSegment, item.getType(), item);

                    if (item.isStation()) {
                        Stop stop = item.getStop();
                        log.debug("  S   {} is a stop", item);

                        if (stopDistances.containsKey(stop)) {
                            beenAtStopBefore = true;
                            if (edgeCount < stopDistances.get(stop)) {
                                foundLowerEdgeCount = true;
                                stopDistances.put(stop, edgeCount);
                            }
                        }
                        if (!beenAtStopBefore || foundLowerEdgeCount) {
                            stopDistances.put(stop, edgeCount);
                            log.debug("Distance at stop {} is {}", stop, edgeCount);
                        }

                        if (stop.hasTokenOf(company)) {
                            if (edgeCount > 0) foundLowerEdgeCount = true;
                            edgeCount = 0;
                            stopDistances.put(stop, 0);
                            if (hex.getStops().size() == 1) {
                                hexDistances.put(hex, 0);
                            }
                        } else if (stop.isTokenableFor(company)) {
                            cityDistancesMap.put(stop, edgeCount + (includeStartHex ? 1 : 0));
                            log.debug("Stop {} is tokenable for {}", stop, company);
                        }

                        if (currentSegment.isAtEnd(item) && !item.equals(startVertex)
                                && !hex.getCurrentTile().getColourText().equalsIgnoreCase("red")
                                && (foundLowerEdgeCount || !beenAtStopBefore)) {
                            // Check if there are other segments from this segment end
                            for (DLLGraph.Segment segment : graph.getSegments(stop)) {
                                if (segment != currentSegment || foundLowerEdgeCount) {
                                    pushWaitingList(new WaitingSegment(segment, item));
                                }
                            }
                        }
                        foundLowerEdgeCount = false;
                        beenAtStopBefore = false;

                    } else if (item.isSide()) {
                        // Only increase the edge count when entering a hex
                        if (hex.equals(prevHex)) {
                            // Leaving a hex.
                            vertexDistances.put(item, edgeCount);
                        } else {
                            // Entering a hex.
                            edgeCount++;

                            if (vertexDistances.containsKey(item)) {
                                // Been here before, check edgeCount
                                int oldEdgeCount = vertexDistances.get(item);
                                if (oldEdgeCount > edgeCount) {
                                    log.debug("Seg {} at {} lowering distance from {} to {}",
                                            currentSegment, item, oldEdgeCount, edgeCount);
                                    // OK, we can lower that count, and continue
                                    vertexDistances.put(item, edgeCount);
                                    hexDistances.put(hex, edgeCount);
                                } else if (oldEdgeCount < edgeCount - 2) {
                                    log.debug("Reversing seg {} at {}: found lower distance {} than {}",
                                            currentSegment, item, oldEdgeCount, edgeCount);
                                    // The new hex already has a lower value,
                                    // and we must backtrack
                                    currentSegment.setIteratorReversed(item);
                                    edgeCount = vertexDistances.get(item);
                                    foundLowerEdgeCount = true;
                                    // Unclear why IJ grays this out, it is really necessary!
                                } else {
                                    log.debug("Seg {} at {} found similar distances {} and {}",
                                            currentSegment, item, oldEdgeCount, edgeCount);
                                    foundLowerEdgeCount = false;
                                    break;
                                }

                            }
                        }
                    }
                    prevItem = item;
                    prevHex = hex;
                    log.debug("Item {} has distance {}", item, edgeCount);
                }
            }
        }
        log.debug("Distances: {}", cityDistancesMap);
        return cityDistancesMap;
    }

    /** Objects to be placed in the waiting LIFO list */
    private class WaitingSegment {
        private DLLGraph.Segment segment;
        private NetworkVertex startVertex;
        private boolean forward;

        protected WaitingSegment(DLLGraph.Segment segment, NetworkVertex startVertex, boolean forward) {
            this.segment = segment;
            this.startVertex = startVertex;
            this.forward = forward; // Will be ignored at the segment ends
        }

        protected WaitingSegment(DLLGraph.Segment segment, NetworkVertex startVertex) {
            this.segment = segment;
            this.startVertex = startVertex;
        }

        public DLLGraph.Segment getSegment() {
            return segment;
        }

        public NetworkVertex getStartVertex() {
            return startVertex;
        }

        public boolean isForward() {
            return forward;
        }
    }

    private void pushWaitingList (WaitingSegment ws){
        waitingList.push(ws);
        log.debug("Pushed waiting {} at {}", ws.segment, ws.startVertex);
    }
}
