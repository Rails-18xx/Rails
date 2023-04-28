package net.sf.rails.game;

import net.sf.rails.algorithms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Routes {

    private static final Logger log = LoggerFactory.getLogger(Routes.class);

    PublicCompany company;

    /* Map from visited hex sides to the minimal track distances
     * from a base token.
     */
    private Map<NetworkVertex, Integer> vertexDistances = new HashMap<>();
    Map<Stop, Integer> tokenableStopDistances = new HashMap<>();

    private NetworkGraph mapGraph;
    private MapManager mapManager;

    private LinkedList<WaitingPath> waitingList = new LinkedList<>();

    public Routes(PublicCompany company) {
        this.company = company;
    }

    /**
     * Create a map with all tokenable Stops for a PublicCompany,
     * and their distances to a valid start base token.
     * @param includeStartHex True if both the start and final hex must be counted.
     * @param toHomeOnly True if only the distance to a home hex counts.
     * @return A HashMap with tokenable stops as keys and the distances as values.
     */
    public Map<Stop, Integer> getTokenLayRouteDistances(
            boolean includeStartHex, boolean toHomeOnly){

        RailsRoot root = company.getRoot();
        mapGraph = NetworkGraph.createMapGraph(root);
        mapManager = root.getMapManager();

        NetworkGraph companyGraph =
                NetworkGraph.createRouteGraph(mapGraph, company, false, false);

        List<Stop> usableBases = new ArrayList<>();

        for (BaseToken token : company.getLaidBaseTokens()) {
            Stop stop = ((Stop) token.getOwner());
            MapHex hex = stop.getParent();
            if (!toHomeOnly || company.getHomeHexes().contains(hex)) {
                usableBases.add(stop);
            }
        }

        // Make startbase scanning sequence reproducible
        usableBases.sort((a, b) -> (a.getId().compareTo(b.getId())));
        log.debug(">>>>> Usable bases: {}", usableBases);

        for (Stop baseTokenStop : usableBases) {
            MapHex baseTokenHex = baseTokenStop.getParent();
            Station station = baseTokenStop.getRelatedStation();
            NetworkVertex fromVertex = mapGraph.getVertex(baseTokenHex, station);
            NetworkVertex toVertex;
            log.debug(">>>>> Start search from base at {}", baseTokenStop);

            waitingList.clear();

            int distance = 0;
            MapHex hex;

            queueNeighbours (fromVertex, distance);

            while (!waitingList.isEmpty()) {

                WaitingPath waiting = dequeuePath();
                log.debug ("Starting to process {}", waiting);

                fromVertex = waiting.fromVertex;
                toVertex = waiting.toVertex;
                distance = waiting.distance;
                hex = fromVertex.getHex();

                if (toVertex.isSide()) {
                    // find any track on the next side
                    MapHex neighbourHex = mapManager.getNeighbour(hex, toVertex.getSide());
                    NetworkVertex expectedSide = companyGraph.getVertex(neighbourHex, toVertex.getSide().opposite());
                    log.debug ("      Expecting hex={} side={}", neighbourHex, expectedSide);
                    if (expectedSide != null) {
                        if (vertexDistances.containsKey(expectedSide)) {
                            if (vertexDistances.get(expectedSide) < distance + 2) {
                                log.debug ("  Stopping scan at {}, distance found is {}",
                                        expectedSide, vertexDistances.get(expectedSide));
                                continue;
                            }
                        }
                        if (queueNeighbours(expectedSide, distance + 1)) {
                            setVertexDistance (expectedSide, distance + 1);
                        } else {
                            log.debug ("  Stopping scan at {}, found no track", expectedSide);
                            continue;
                        }
                    }
                } else if (toVertex.isStation()) {
                    Stop stop = toVertex.getStop();
                    if (stop.hasTokenOf(company)) {
                        setVertexDistance(fromVertex, 0);
                        setVertexDistance(toVertex, 0);
                        log.debug ("    Stopping scan at tokened stop {}, distance {}",
                                stop, distance);
                        continue;
                    } else if (stop.isTokenableFor(company)) {
                        setTokenableStopDistance(stop, distance + (includeStartHex ? 1 : 0));
                    }
                    setVertexDistance(toVertex, distance);
                    if (stop.isRunThroughAllowedFor(company)) {
                        queueNeighbours(toVertex, distance, fromVertex);
                    }
                }


            }
        }
        log.debug("{} tokenable at distances: {}", company, tokenableStopDistances);
        return tokenableStopDistances;
    }

    private boolean queueNeighbours (NetworkVertex begin, int distance) {
        return queueNeighbours(begin, distance, null);
    }

    private boolean queueNeighbours (NetworkVertex begin, int distance, NetworkVertex excluded) {
        boolean found = false;
        for (NetworkVertex neighbour : getNeighbours(begin)) {
            if (excluded == null || !neighbour.equals(excluded)) {
                queuePath(new WaitingPath(begin, neighbour, distance));
                found = true;
            }
        }
        if (!found) log.debug ("No neighbours found at {}", begin);
        return found;
    }

    private List<NetworkVertex> getNeighbours(NetworkVertex vertex) {

        MapHex hex = vertex.getHex();
        Tile tile = hex.getCurrentTile();
        HexSide rotation = hex.getCurrentTileRotation();

        List<NetworkVertex> neighbours = new ArrayList<>();
        Set<Track> tracks = tile.getTracks();
        for (Track track : tracks) {
            NetworkVertex from = mapGraph.getVertex(hex, track.getStart().rotate(rotation));
            NetworkVertex to = mapGraph.getVertex(hex, track.getEnd().rotate(rotation));
            if (vertex == from) {
                neighbours.add (to);
            } else if (vertex == to) {
                neighbours.add (from);
            }
        }
        return neighbours;
    }


    private class WaitingPath {
        private NetworkVertex fromVertex;
        private NetworkVertex toVertex;
        private int distance; // from token

        protected WaitingPath(NetworkVertex fromVertex, NetworkVertex toVertex,
                              int distance) {
            this.fromVertex = fromVertex;
            this.toVertex = toVertex;
            this.distance = distance;
        }

        public String toString() {
            return String.format("from %s to %s, distance %d",
                    fromVertex, toVertex, distance);
        }
    }

    private void queuePath(WaitingPath wp){
        waitingList.add(wp);
        log.debug("    Queued {} to {}, distance {}", wp.fromVertex, wp.toVertex, wp.distance);
    }

    private WaitingPath dequeuePath() {
        WaitingPath wp = waitingList.removeFirst();
        log.debug("    Dequeued {} to {}, distance {}", wp.fromVertex, wp.toVertex, wp.distance);
        return wp;
    }

    private boolean setVertexDistance (NetworkVertex vertex, int distance) {
        if (!vertexDistances.containsKey(vertex) || distance < vertexDistances.get(vertex)) {
            log.debug ("  Set vertex {} distance from {} to {}",
                    vertex, vertexDistances.get(vertex), distance);
            vertexDistances.put (vertex, distance);
            return true;
        } else {
            log.debug("  Skipped setting vertex distance from {} to {}",
                    vertex, vertexDistances.get(vertex), distance);
            return false;
        }
    }

    private boolean setTokenableStopDistance (Stop stop, int distance) {
        if (!tokenableStopDistances.containsKey(stop) || distance < tokenableStopDistances.get(stop)) {
            log.debug ("  Set tokenable stop {} distance from {} to {}",
                    stop, tokenableStopDistances.get(stop), distance);
            tokenableStopDistances.put (stop, distance);
            return true;
        } else {
            log.debug("  Skipped setting tokenable stop distance from {} to {}",
                    stop, tokenableStopDistances.get(stop), distance);
            return false;
        }
    }

}
