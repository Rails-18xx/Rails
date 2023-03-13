package net.sf.rails.game;

import net.sf.rails.algorithms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Routes {

    private static final Logger log = LoggerFactory.getLogger(Routes.class);

    private static final int NO_VALUE = -1;
    private static final int HIGH_VALUE = 99;

    PublicCompany company;

    /* Map from visited hex sides to the minimal track distances
     * from a base token.
     */
    private Map<NetworkVertex, Integer> vertexDistances = new HashMap<>();

    private Map<NetworkVertex, Integer> junctionDistances = new HashMap<>();

    private Map<Stop, HashMap<NetworkVertex, Integer>> startDistances
            = new HashMap<>();

    private LinkedList<WaitingSegment> waitingList = new LinkedList<>();

    DLLGraph dllGraph;

    boolean distancesHaveChanged;

    /** This map of maps records the minimal token distances for a stop,
     *  as it was set when entering the stop hex from a particular side.
     *  Such a value would NOT be useable as a starting distance when
     *  travelling from that stop via that same side, probably to a
     *  different downstream branch. In such a case, the starting distance
     *  must be derived as the lowest value obtained from all other sides.
     */
    //Map<Stop, HashMap<NetworkVertex, Integer>> sideDistances
    //        = new HashMap<>();


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
        NetworkGraph mapGraph = NetworkGraph.createMapGraph(root);

        log.debug("Start finding distances for {}", company);
        waitingList.clear();

        /* Build an object network of nodes and segments,
         * where the segments can be traversed in both directions.
         */
        dllGraph = new DLLGraph(company, false);
        log.debug("DLL graph: {}", dllGraph);

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
        usableBases.sort((a, b) -> (a.getId().compareTo(b.getId())));
        log.debug(">>>>> Usable bases: {}", usableBases);

        for (Stop baseTokenStop : usableBases) {
            MapHex baseTokenHex = baseTokenStop.getParent();

            Station station = baseTokenStop.getRelatedStation();
            NetworkVertex startVertex = mapGraph.getVertex(baseTokenHex, station);
            NetworkVertex toVertex;
            log.debug(">>>>> Start search from base at {}", baseTokenStop);

            //setLowestValue (startVertex, 0);

            List<DLLGraph.Segment> segments = dllGraph.getSegments(baseTokenStop, 1);
            log.debug ("Segments from {} : {}", baseTokenStop, segments);
            for (DLLGraph.Segment segment : segments) {
                pushWaitingList(new WaitingSegment(segment, startVertex, 1));
            }

            int edgeCount = 0;
            int lowestValue;
            MapHex hex;
            MapHex prevHex = baseTokenHex;
            NetworkVertex prevItem = null;

NEXTSEG:    while (!waitingList.isEmpty()) {

                WaitingSegment waiting = waitingList.pop();
                log.debug("Iteration start of segment {}", waiting);

                DLLGraph.Segment currentSegment = waiting.getSegment();
                startVertex = waiting.getFromVertex();
                toVertex = waiting.getToVertex();
                NetworkVertex nextAfterStart = currentSegment.getNextTo(startVertex);
                NetworkVertex lastBeforeEnd = currentSegment.getNextTo(toVertex);
                distancesHaveChanged = false;

                currentSegment.setIterator(startVertex, toVertex);

                if (startVertex.isStation()) {// && startVertex.getStop().hasTokenOf(company)) {
                    //edgeCount = 0;
                    //} else if (vertexDistances.containsKey(startVertex)) {
                    //    edgeCount = vertexDistances.get(startVertex);
                    /*} else*/
                    if ((edgeCount = getStartDistance (
                            startVertex, nextAfterStart)) < HIGH_VALUE) {
                        log.debug("Start count of segment {} is {}", currentSegment, edgeCount);
                    } else {
                        log.debug("No edgeCount available for stop {}, what now??", startVertex);
                    }
                } else {
                    if (vertexDistances.containsKey(startVertex)) {
                        edgeCount = vertexDistances.get(startVertex);
                    } else {
                        log.debug("No edgeCount available, for edge {}, what now??", startVertex);
                    }
                }
                log.debug ("Starting edgeCount = {}", edgeCount);

                while (currentSegment.hasNext()) {
                    NetworkVertex item = currentSegment.getNext();
                    hex = item.getHex();
                    boolean foundLowerEdgeCount = false;
                    boolean beenAtStopBefore = false;

                    log.debug("      Checking item {} {} of segment {}",
                            item.getType(), item, currentSegment);

                    if (item.isStation()) {
                        Stop stop = item.getStop();
                        log.debug("  S   {} is a stop ({})", item, stop);

                        if (stop.hasTokenOf(company) && !item.equals(startVertex)) {
                            // There will always be a scan from this stop backwards,
                            // so we can stop scanning here
                            //setLowestValue (item, 0);
                            //setSideDistance(stop, currentSegment.getNextTo(item), 0);

                            continue NEXTSEG;
                        }
                        if (stop.isTokenableFor(company)) {
                            if (!cityDistancesMap.containsKey(stop) || edgeCount < cityDistancesMap.get(stop)) {
                                cityDistancesMap.put(stop, edgeCount + (includeStartHex ? 1 : 0));
                                log.debug("Stop {} is tokenable for {} at distance {}", stop, company, edgeCount);
                            }
                        }

                        /*
                        if (currentSegment.isAtEnd(item) && !item.equals(startVertex)
                                && !hex.getCurrentTile().getColourText().equalsIgnoreCase("red")
                                && (foundLowerEdgeCount || !beenAtStopBefore)) {
                            int d = edgeCount;
                            if (vertexDistances.containsKey(item)) {
                                d = Math.min (vertexDistances.get (item), edgeCount);
                            }
                            setLowestValue (item, d);*/
                        if (currentSegment.isAtEnd(item) && !item.equals(startVertex)) {
                            //setSideDistance(stop, currentSegment.getNextTo(item), edgeCount);
                            setDistancesAtFinalStop(item, prevItem, edgeCount);

                            // Check if there are other segments from this segment end
                            // This only makes sense if the current scan has changed any distance value
                            if (distancesHaveChanged && !item.isSink()) {
                                for (DLLGraph.Segment segment : dllGraph.getSegments(stop, 2)) {
                                    log.debug("Segment {} found at {} ({})", segment, item, stop);
                                    if (!segment.isDuplicate(currentSegment) || foundLowerEdgeCount) {
                                        pushWaitingList(new WaitingSegment(segment,
                                                item, segment.otherEnd(item), 2));
                                    }
                                }
                            }
                        }

                    } else if (item.isSide()) {

                        // Only increase the edge count when entering a hex, except the next-to-last one
                        if (!hex.equals(prevHex)) {
                            // Entering a hex.
                            edgeCount++;

                            // Find the previous value at this side
                            int oldValue = getDistance (item);

                            // If that value is at least 3 lower than the originally assigned one,
                            // it is necessary to schedule a scan from this vertex backwards
                            if (oldValue != HIGH_VALUE && oldValue != NO_VALUE) {
                                if (edgeCount > oldValue + 2) {
                                    pushWaitingList(new WaitingSegment(currentSegment, item, startVertex, 3));
                                    log.debug("      Scheduled a backward scan in {} from {} to {}",
                                            currentSegment, item, startVertex);
                                }
                                if (edgeCount > oldValue) {
                                    // It doesn't make sense to continue from here
                                    continue NEXTSEG;
                                }
                            }
                        }
                        assignDistance(item, edgeCount);
                    }
                    prevHex = hex;
                    prevItem = item;
                    log.debug("Item {} has distance {}", item, edgeCount);
                }
            }
        }
        log.debug("{} tokenable at distances: {}", company, cityDistancesMap);
        return cityDistancesMap;
    }

    private boolean assignDistance (NetworkVertex item, int newValue) {
        distancesHaveChanged = false;
        if (vertexDistances.containsKey(item)) {
            int oldValue = vertexDistances.get(item);
            if (oldValue > newValue) {
                log.debug("      Item {} set from {} to {}", item, oldValue, newValue);
                vertexDistances.put (item, newValue);
                distancesHaveChanged = true;
            } else if (oldValue < newValue){
                log.debug ("WARNING: does NOT set item {} from {} to {}",
                        item, oldValue, newValue);
            }
        } else {
            log.debug("Item {} set to {}", item, newValue);
            vertexDistances.put(item, newValue);
            distancesHaveChanged = true;
        }
        return distancesHaveChanged;
    }

    /**
     * This method is used when entering the <b>final</b> hex of a segment.
     *
     * The purpose is to update all connected edges to the value that should
     * be used as a starting edgeCount for any new segment scan starting there.
     * Connected edges include those behind a connected station.
     *
     * The entry side gets the lowest values of all other connected sides.
     * The other sides are updated wherever the edgeCount of entry is lower.

     * @param entryVertex The first vertex encountered when entering a hex.
     * @param entryDistance The provisional distance of this vertex on entry.
     * @return The (updated) distance value assigned to the vertex of entry.
     */

    /*
    private int updateSegmentEndSides(NetworkVertex entryVertex, int entryDistance) {

        int otherValue;
        int newEntryValue = HIGH_VALUE;
        Map<NetworkVertex, Integer> toUpdate = new HashMap<>();

        // First collect all side vertices of that hex, possibly connected via a stop.
        // (The rare connected multiple stops case is ignored so far)
        List<NetworkVertex> otherSides = new ArrayList<>();
        for (NetworkVertex otherVertex : dllGraph.getNeighbours(entryVertex)) {
            if (otherVertex.isStation()) {
                for (NetworkVertex v : dllGraph.getNeighbours(otherVertex)) {
                    if (!v.equals(entryVertex)) otherSides.add(v);
                }
            } else {
                otherSides.add(otherVertex);
            }
        }

        // Find the lowest value of all other sides, and update these where needed
        for (NetworkVertex otherVertex : otherSides) {
            if (vertexDistances.containsKey(otherVertex)) {
                otherValue = vertexDistances.get(otherVertex);
                newEntryValue = Math.min(newEntryValue, otherValue);
                if (entryDistance < otherValue) toUpdate.put(otherVertex, entryDistance);
            } else {
                toUpdate.put(otherVertex, entryDistance);
            }
        }

        // Apply the lowest value to the entry side
        if (newEntryValue == HIGH_VALUE) newEntryValue = entryDistance;
        toUpdate.put (entryVertex, newEntryValue);

        // Execute all updates found necessary
        for (NetworkVertex v : toUpdate.keySet()) {
            //assignDistance (v, toUpdate.get(v));
            //vertexDistances.put (entryVertex, newEntryValue);

        }
        return newEntryValue;
    }*/

    /*
    public void setStartDistances(Stop stop, NetworkVertex entrySide, int entryDistance) {

        int otherValue;
        int newEntryValue = HIGH_VALUE;
        Map<NetworkVertex, Integer> toUpdate = new HashMap<>();

        NetworkVertex stopVertex = dllGraph.

        // First collect all side vertices of that hex, possibly connected via a stop.
        // (The rare connected multiple stops case is ignored so far)
        List<NetworkVertex> otherSides = new ArrayList<>();
        for (NetworkVertex otherVertex : dllGraph.getNeighbours(entryVertex)) {
            if (otherVertex.isStation()) {
                for (NetworkVertex v : dllGraph.getNeighbours(otherVertex)) {
                    if (!v.equals(entryVertex)) otherSides.add(v);
                }
            } else {
                otherSides.add(otherVertex);
            }
        }

        // Find the lowest value of all other sides, and update these where needed
        for (NetworkVertex otherVertex : otherSides) {
            if (vertexDistances.containsKey(otherVertex)) {
                otherValue = vertexDistances.get(otherVertex);
                newEntryValue = Math.min(newEntryValue, otherValue);
                if (entryDistance < otherValue) toUpdate.put(otherVertex, entryDistance);
            } else {
                toUpdate.put(otherVertex, entryDistance);
            }
        }

        // Apply the lowest value to the entry side
        if (newEntryValue == HIGH_VALUE) newEntryValue = entryDistance;
        toUpdate.put (entryVertex, newEntryValue);

        // Execute all updates found necessary
        for (NetworkVertex v : toUpdate.keySet()) {
            //assignDistance (v, toUpdate.get(v));
            //vertexDistances.put (entryVertex, newEntryValue);

        }
        return newEntryValue;

    }
    */

    /*----- Methods to handle sideDistances map of maps -----*/

    /*
    private boolean setSideDistance (Stop stop, NetworkVertex side, int distance) {
        if (!sideDistances.containsKey(stop)) sideDistances.put (stop, new HashMap<>());
        if (sideDistances.get(stop).containsKey(side)) {
            int oldValue = sideDistances.get(stop).get(side);
            if (distance >= oldValue) return false;
        }
        sideDistances.get(stop).put (side, distance);
        return true;
    }

    private int getSideDistance (Stop stop, NetworkVertex side) {
        if (!sideDistances.containsKey(stop) || !sideDistances.get(stop).containsKey(side)) {
            return NO_VALUE;
        }
        return sideDistances.get(stop).get(side);
    }*/

    /** At a segment final stop, the side distance values must be updated
     * except the entering side.
     * @param junction Final junction  (stop) of a closing segment
     * @param entrySide The side through which the segment scan has entered the final hex.
     *                 This is (normally) the next-to-last vertex of the segment.
     * @param entrySideDistance The base token distance (edgeCount) after entering the final hex.
     */
    private int setDistancesAtFinalStop (NetworkVertex junction, NetworkVertex entrySide, int entrySideDistance) {

        Stop stop = junction.getStop();
        boolean tokened = stop.hasTokenOf(company);
        if (tokened) entrySideDistance = 0;

        int sideDistance;
        int newEntrySideDistance = HIGH_VALUE;
        int newStopDistance = entrySideDistance;
        Map<NetworkVertex, Integer> toUpdate = new HashMap<>();

        // Find the lowest value of all other sides, and update these where needed
        // Ignore the rare case of connected stops on one hex (does not occur in 1826)
        for (NetworkVertex sideVertex : dllGraph.getNeighbours(junction)) {
            if (!sideVertex.equals(entrySide)) {
                sideDistance = getDistance(sideVertex);
                newStopDistance = Math.min(newStopDistance, sideDistance);
                newEntrySideDistance = Math.min(newEntrySideDistance, sideDistance);
                if (entrySideDistance < sideDistance) toUpdate.put(sideVertex, entrySideDistance);
            }
        }

        // Apply the lowest value to the entry side
        if (newEntrySideDistance == HIGH_VALUE) newEntrySideDistance = entrySideDistance;
        toUpdate.put (entrySide, newEntrySideDistance);

        // Execute all updates found necessary
        for (NetworkVertex v : toUpdate.keySet()) {
            setDistance (v, toUpdate.get(v));
        }
        setStartDistance(junction.getStop(), entrySide, toUpdate.get(entrySide));
        return newEntrySideDistance;
    }

    private void setDistance (NetworkVertex side, int distance) {
        int newValue = distance;
        int oldValue = HIGH_VALUE;
        if (vertexDistances.containsKey(side)) {
            oldValue = vertexDistances.get(side);
            newValue = Math.min(oldValue, distance);
        }
        vertexDistances.put(side, newValue);
        log.debug ("      Side distance {} set from {} to {}", side, oldValue, newValue);
    }

    private int getDistance (NetworkVertex vertex) {
        if (vertexDistances.containsKey(vertex)) {
            return vertexDistances.get(vertex);
        } else {
            return HIGH_VALUE;
        }
    }

    private void setStartDistance (Stop junction, NetworkVertex side, int distance) {
        if (!startDistances.containsKey(junction)) {
            startDistances.put (junction, new HashMap<>());
        }
        startDistances.get(junction).put (side, distance);
    }

    private int getStartDistance (NetworkVertex junction, NetworkVertex side) {
        Stop stop = junction.getStop();
        if (stop.hasTokenOf(company)) {
            return 0;
        }
        if (startDistances.containsKey(stop) && startDistances.get(stop).containsKey(side)) {
            return startDistances.get(stop).get(side);
        }
        int distance = HIGH_VALUE;
        for (NetworkVertex otherSide : dllGraph.getNeighbours(junction)) {
            if (!otherSide.equals(side)) {
                distance = Math.min (distance, vertexDistances.get(side));
            }
        }
        return distance; // HIGH_VALUE should not occur
    }

    /*
    private void updateSide (NetworkVertex side, int distance) {
        if (!vertexDistances.containsKey(side) || vertexDistances.get(side) > distance) {
            vertexDistances.put(side, distance);
        }
    }*/

    /** Get start distance of a segment leaving via a certain side
     * The distance recorded at entry via that side should be ignored.
     * If side == null, the lowest of all sides will be returned.
     * @param stop The starting stop (junction) of a segment to be scanned
     * @param side The edge via which the segment leaves that hex, or null
     * @return The distance found; HIGH_VALUE if none was found.
     */
    /*
    private int getStartDistanceViaSide (NetworkVertex stop, NetworkVertex side) {
        int distance = HIGH_VALUE;
        for (NetworkVertex vertex : dllGraph.getNeighbours(stop)) {
            if (side != null && vertex.equals(side)) continue;
            //distance = Math.min (distance, getSideDistance(stop.getStop(), vertex));
            distance = Math.min (distance, vertexDistances.get(vertex));
        }
        return distance;
    }*/

    /*----- Methods to deal with the queue of segments waiting to be scanned -----*/

    /** Objects to be placed in the waiting LIFO list */
    private class WaitingSegment {
        private DLLGraph.Segment segment;
        private NetworkVertex fromVertex;
        private NetworkVertex toVertex;

        protected WaitingSegment(DLLGraph.Segment segment,
                                 NetworkVertex fromVertex, NetworkVertex toVertex, int marker) {
            this.segment = segment;
            this.fromVertex = fromVertex;
            this.toVertex = toVertex; // Will be ignored at the segment ends
            log.debug ("  {}   Created waiting {}", marker, this);
        }

        protected WaitingSegment(DLLGraph.Segment segment, NetworkVertex fromVertex, int marker) {
            this.segment = segment;
            this.fromVertex = fromVertex;
            this.toVertex = segment.otherEnd(fromVertex);
            log.debug ("  {}   Created waiting {}", marker, this);        }

        public DLLGraph.Segment getSegment() {
            return segment;
        }

        public NetworkVertex getFromVertex() {
            return fromVertex;
        }

        public NetworkVertex getToVertex() { return toVertex; }

        public String toString() {
            return String.format("%s waiting for scan from %s to %s",
                    segment.toLongString(), fromVertex, toVertex);
        }
    }

    private void pushWaitingList (WaitingSegment ws){
        // One-item segments should not exist
        waitingList.push(ws);
        log.debug("Pushed waiting {} at {}", ws.segment, ws.fromVertex);
    }
}
