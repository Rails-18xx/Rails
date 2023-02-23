package net.sf.rails.algorithms;

import net.sf.rails.game.*;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * DLL stands for Doubly Linked List.
 * This class creates a company network representation, using:
 * 
 * - Junctions: places where at least 3 different tracks meet.
 *   This can be stations or tile edges where plain track branches off.
 *
 * - Dead ends: stations where only one track is connected to.
 *   Multiple track stations where the company cannot run through 
 *   for whatever reason are also considered dead ends.
 *   For simplicity, all dead ends are also considered Junctions
 *   (a not very appropriate name here).
 *   
 * - Segments: doubly linked lists connecting junctions (and dead ends),
 *   included all TrackPoints on that route.
 *   Each edge where tracks of two different tiles meet, is
 *   represented by two TrackPoints in a Segment, one in each tile.
 *   The two-way linking ensures that backtracking is possible.
 *   
 * DLLGraph has two inner classes: Junction and Segment.
 * Segment extends the java LinkedList class.
 * Junction currently does not extend NetworkVertex for the only
 * reason that NetworkVertex is final. For now I will leave it so.
 *   
 * This class has been created to support the 1826 feature that
 * the hex distances that define token laying costs have to be counted
 * along existing routes, rather than as the crow flies (as in 1835).
 * 
 * While working on 1826, I initially managed to calculate these costs
 * correctly using the NetworkIterator provided by the existing company
 * Graph code. But as it turned out, my algorithm failed when closed
 * loops arose. So far, I did not manage to fix this.
 * No doubt there must be a way, but I have given up.
 * 
 * DLLGraph only creates the structure mentioned above. The actual
 * cost calculation will (for now) be done in the Routes class.
 *   
 * Started by Erik Vos, October 2022.
 */
public class DLLGraph {

    private static final Logger log = LoggerFactory.getLogger(DLLGraph.class);

    private PublicCompany company;
    private boolean toHomeOnly;
    private RailsRoot root;

    private List<Segment> segments;
    private Map<NetworkVertex, Segment> danglingSegments;
    private Map<NetworkVertex, Junction> junctions;

    private NetworkGraph mapGraph;

    private Map<Stop, NetworkVertex> vertexOfStop;

    public DLLGraph (PublicCompany company, boolean toHomeOnly) {
        this.company = company;
        this.toHomeOnly = toHomeOnly;
        root = company.getRoot();
        createGraph();
    }

    /*-----------------------*/
    /* GRAPH CREATION METHOD */
    /*-----------------------*/
    
    private void createGraph () {

        mapGraph = NetworkGraph.createMapGraph(company.getRoot());
        NetworkGraph companyGraph =
                NetworkGraph.createRouteGraph(mapGraph, company, false, false);
        SimpleGraph<NetworkVertex, NetworkEdge> graph = companyGraph.getGraph();
        List<Stop> usableBases = new ArrayList<>();
        vertexOfStop = new HashMap<>();

        for (BaseToken token : company.getLaidBaseTokens()) {
            Stop stop = ((Stop)token.getOwner());
            MapHex hex = stop.getParent();
            if (!toHomeOnly || company.getHomeHexes().contains(hex)) {
                usableBases.add (stop);
            }
        }
        log.debug(">>>>> Usable bases: {}", usableBases);

        /* Map from visited hex sides to the minimal track distances
         * from a base token.
         */
        Map<NetworkVertex, Integer> beenAtEdge = new HashMap<>();

        /* Map from visited stops (stations) to the minimal
         * track distances from a base token.
         */
        Map<Stop, Integer> beenAtStop = new HashMap<>();
        Set<MapHex> beenAtHex = new HashSet<>();

        // Hex distances from nearest token (excluded)
        //Map<NetworkVertex, Integer> distances = new HashMap<>();
        //Map<Stop, Integer> tokenableStopDistances = new HashMap<>();

        /* Elements for a new network structure, where
         * track sequences can be traversed in both directions.
         */
        danglingSegments = new HashMap<>();
        junctions = new HashMap<>();
        segments = new ArrayList<>();

        NetworkVertex expectedEdge = null;

        /* Build an object network of nodes and segments,
         * where the segments can be traversed in both directions.
         */
        for (Stop baseTokenStop : usableBases) {
            if (beenAtStop.containsKey(baseTokenStop)) continue;

            MapHex baseTokenHex = baseTokenStop.getParent();

            Station station = baseTokenStop.getRelatedStation();
            NetworkVertex startVertex = mapGraph.getVertex(baseTokenHex, station);
            log.debug(">>>>> Start search from base at {}", baseTokenStop);

            NetworkIterator iterator = new NetworkIterator(graph, startVertex);

            NetworkVertex prevItem = startVertex;
            MapHex prevHex = startVertex.getHex();
            Segment currentSegment = null;
            Junction junction;
            List<NetworkVertex> exits = new ArrayList<>();
            boolean entering;

            while (iterator.hasNext()) {
                NetworkVertex item = iterator.next();
                MapHex hex = item.getHex();
                List<NetworkVertex> neighbours = getNeighbours(mapGraph, item);
                log.debug("      Checking {} item {}, neighbours {}", item.getType(), item, neighbours);
                if (item.isStation()) {
                    Stop stop = item.getStop();
                    vertexOfStop.put (stop, item);

                    log.debug("  S   {} is a stop", item);

                    boolean newJunction = false;
                    if (neighbours.size() != 2 || item.isSink()
                            || !stop.isRunThroughAllowedFor(company)
                            || stop.hasTokenOf(company)) {
                        // We are at a junction or a dead end, both are Segment ends
                        if (!junctions.containsKey(item)) {
                            newJunction = true;
                            createJunction(item);
                        }
                        log.debug("  J   {} is {} junction", item,
                                (newJunction ? "a new" : "an existing"));
                    }
                    if (neighbours.size() > 2 && stop.isRunThroughAllowedFor(company)) {
                        // We are at a branching point
                        junction = junctions.get(item);

                        // Finish current Segment
                        if (currentSegment != null) { // Can it ever be null?
                            // Finish the old segment
                            //danglingSegments.remove(prevItem);
                            addItem(currentSegment, item, 1);
                            /*
                            String oldSegment = currentSegment.toString();
                            currentSegment.addItem(item);
                            log.debug("++1++ Added new item {} to {}, becoming {}", item, oldSegment, currentSegment);*/
                            //junction.addSegment(currentSegment);
                            closeSegment (currentSegment, item, 1);
                            /*currentSegment.close();
                            log.debug("XX1XX Closed segment {}", currentSegment);*/
                            currentSegment = null;
                        }

                        // Create new segments if the stop can be run through
                        if (newJunction && stop.isRunThroughAllowedFor(company)) {
                            for (NetworkVertex neighbour : neighbours) {
                                if (neighbour == prevItem) continue;

                                Segment newSegment = new Segment(item);
                                newSegment.add(neighbour);
                                segments.add(newSegment);
                                junction.addSegment(newSegment, 1);
                                danglingSegments.put(neighbour, newSegment);
                                log.debug(">>1>> Opened new segment {}, dangling at {}",
                                        newSegment, neighbour);
                            }
                        }
                    } else if (neighbours.size() == 1 || item.isSink() || !stop.isRunThroughAllowedFor(company)) {
                        // This is an end point
                        if (currentSegment != null) {
                            // Add it to the segment and close that
                            addItem(currentSegment, item, 2);
                            closeSegment(currentSegment, item, 2);
                            //if (!junctions.containsKey(item)) createJunction(item);
                            //junctions.get(item).addSegment(currentSegment, 2);
                            currentSegment = null;
                        } else {
                            // Start hex, not yet visited
                            currentSegment = openSegment(item, 2);
                        }
                    } else {
                        // Normal progress in a segment (2 neighbours)
                        if (beenAtStop.containsKey(stop)) {
                            log.debug ("      Skipping stop {}: visited before", stop);
                            continue;
                        }
                        for (NetworkVertex neighbour : neighbours) {
                            if (currentSegment != null && currentSegment.contains(neighbour)) {
                                // Silently skip already visited item
                                continue;
                            }
                            if (currentSegment != null) {
                                if (item.isSink() || !stop.isRunThroughAllowedFor(company)) {
                                    // This stop is a sink
                                    addItem(currentSegment, item, 3);

                                    closeSegment(currentSegment, item, 3);
                                    currentSegment = null;

                                } else {
                                    addItem(currentSegment, item, 4);
                                }

                            } else {
                                // Start hex, not yet visited
                                currentSegment = openSegment(item, 4);
                            }

                        }

                    }
                    beenAtStop.put(stop, 0);
                } else if (item.isSide()) {
                    /* Causes missing edges in a segment
                    if (beenAtEdge.containsKey(item)) {
                        log.debug("      Skipping edge {}: visited before", item);
                        //continue;
                    }*/
                    // Check if we haven't made a strange jump
                    if (hex != prevItem.getHex() && currentSegment != null) {
                        // Entering hex
                        if (expectedEdge != item) log.debug("      Expected {}, actual {}", expectedEdge, item);
                        if (expectedEdge != null && expectedEdge != item) {
                            log.debug ("!!!!! We have a jump from {} to {}, expected {}", prevItem, item, expectedEdge);
                            // Join the two unfinished segments and then continue normally
                            joinSegments(currentSegment, expectedEdge, 5);
                            currentSegment = null;
                        }
                    }
                    expectedEdge = null;
                    entering = !hex.equals(prevHex);

                    log.debug ("  E   {} is an edge", item);
                    if (currentSegment == null) {
                        // Find a dangling segment where this item is at the end
                        if (danglingSegments.containsKey(item)) {
                            currentSegment = danglingSegments.get(item);
                            log.debug("===== Side {} found at dangling segment {}", item, currentSegment);
                            entering = false; // Dangling sides always leave (or I hope so)
                        } else {
                            log.warn ("!?!?! No dangling segment found for side {}", item);
                        }
                    }
                    if (currentSegment != null && !currentSegment.contains(item)) {
                        if (neighbours.size() == 0) {
                            // No tracks at entry edge: drop it
                            log.debug("----- Dropping side {}, no track", item);
                            pruneSegment (currentSegment);
                            currentSegment = null;
                        } else if (neighbours.size() > 1 && entering) {
                            // We enter hex at a branching point, remember exit points
                            exits.clear(); // For all certainty, should be redundant
                            exits.addAll(neighbours);
                        }
                        if (currentSegment != null) {
                            // If we are leaving a branching tile, duplicate this segment
                            // as many times as there are other exits
                            if (!entering && !exits.isEmpty()) {
                                for (NetworkVertex exit : exits) {
                                    if (exit.isSide() && exit != item) { // Not the current exit side
                                        Segment newSegment = new Segment(currentSegment);
                                        newSegment.add(exit);
                                        danglingSegments.put (exit, newSegment);
                                        log.debug("++8++ Added extra dangling segment {}",
                                                newSegment);
                                    }
                                }
                                exits.clear();
                            }
                            addItem(currentSegment, item, 6);
                        }
                    }
                    beenAtEdge.put(item, 0);
                    if(beenAtHex.contains(hex)) {
                        // We may be leaving a hex, set which edge we expect next
                        MapManager mapManager = root.getMapManager();
                        MapHex neighbourHex = mapManager.getNeighbour(hex, item.getSide());
                        expectedEdge = companyGraph.getVertex(neighbourHex, item.getSide().opposite());
                        log.debug ("      Expecting hex={} edge={}", neighbourHex, expectedEdge);
                    }
                }
                prevItem = item;
                prevHex = hex;
                if (!beenAtHex.contains(hex)) beenAtHex.add (hex);
            }
        }
        int i;
        log.debug("Junctions:");
        for (NetworkVertex j : junctions.keySet()) log.debug("    {}", junctions.get(j));
        log.debug("Segments:"); i=0;
        for (Segment s : segments) log.debug ("   {}", segments.get(i++).toLongString());
        log.debug("Dangling segments:");
        if (danglingSegments.isEmpty()) {
            log.debug("   None");
        } else {
            for (NetworkVertex j : danglingSegments.keySet()) log.debug("   {} => {}", j, danglingSegments.get(j));
        }

    }

    /*-------------------------------*/
    /* GRAPH CREATION HELPER METHODS */
    /*-------------------------------*/

    private Segment openSegment (NetworkVertex startVertex, int marker) {
        Segment segment = new Segment(startVertex);
        log.debug("++{}++ Creating new segment {} at {} {}",
                marker, segment, vertexType(startVertex), startVertex);
        segments.add(segment);
        danglingSegments.put(startVertex, segment);
        if (junctions.containsKey(startVertex)) {
            Junction junction = junctions.get(startVertex);
            if (!junction.hasSegment(segment)) {
                junctions.get(startVertex).addSegment(segment, marker);
            }
        }

        return segment;
    }

    private void addItem (Segment segment, NetworkVertex vertex, int marker) {
        String oldSegment = segment.toString();
        danglingSegments.remove(segment.getLast());
        segment.add (vertex);
        danglingSegments.put(vertex, segment);

        log.debug ("++{}++ Added {} {} to {}, becoming {}",
                marker, vertexType (vertex), vertex, oldSegment, segment.toLongString());
    }

    private void closeSegment(Segment segment, NetworkVertex vertex, int marker) {
        log.debug("##{}## Closing segment {} at {} {}",
                marker, segment, vertexType(vertex), vertex);
        Junction junction = junctions.get(vertex);
        if (junction == null) junction = createJunction(vertex);
        junction.addSegment(segment, marker);
        segment.close();
        danglingSegments.remove(vertex);
    }

    private void joinSegments (Segment segment, NetworkVertex expectedEdge, int marker) {
        Segment otherSegment = danglingSegments.get(expectedEdge);
        if (otherSegment == null) {
            log.error("Cannot find a dangling segment at {}", expectedEdge);
        } else {
            log.debug("      Found dangling segment {} at {}", otherSegment, expectedEdge);
            danglingSegments.remove(segment.getLast());
            otherSegment.setIterator(expectedEdge);
            while (otherSegment.hasNext()) {
                NetworkVertex vertex = otherSegment.getNext();
                addItem (segment, vertex, 5);
                /*Segment oldSegment = segment;
                segment.add(vertex);
                log.debug("++5++ Added new {} {} to {}, becoming {}",
                        vertexType(vertex), vertex, oldSegment, segment);*/
            }
            NetworkVertex lastItem = segment.getLast();
            closeSegment (segment, lastItem, 5);
            junctions.get(lastItem).addSegment(segment, 5);

            danglingSegments.remove(expectedEdge);
            log.debug("--5-- Removed dangling segment {}", otherSegment);
            discardSegment(otherSegment, 5);
        }
    }

    private void pruneSegment (Segment segment) {
        while (segment.getLast().isSide()) {
            removeItem(segment, segment.getLast(), 7);
        }
        if (segment.size() == 1) {
            // Drop empty segment
            discardSegment (segment, 7);
            NetworkVertex stop = segment.getFirst();
        } else {
            closeSegment (segment, segment.getLast(), 6);
        }

    }

    private void removeItem (Segment segment, NetworkVertex item, int marker) {
        String oldSegment = segment.toString();
        segment.remove(item);
        danglingSegments.remove(item);
        log.debug("--{}-- Dropping {} {} from segment {}, becoming {}",
                marker, item.getType(), item, oldSegment, segment);

    }

    private void discardSegment (Segment segment, int marker) {
        log.debug("--{}-- Dropping segment {}", marker, segment);
        Junction junction;
        if (segment.size() > 0) {
            NetworkVertex lastItem = segment.getLast();
            if (danglingSegments.containsKey(lastItem)) {
                danglingSegments.remove(lastItem, segment);
            }
            junction = junctions.get(lastItem);
            if (junction != null && junction.hasSegment(segment))  {
                junction.removeSegment(segment, marker);
            }
            junction = junctions.get(segment.getFirst());
            if (junction != null && junction.hasSegment(segment)) {
                junction.removeSegment(segment, marker);
            }
        }
        segments.remove(segment);
    }

    private String vertexType (NetworkVertex vertex) {
        if (vertex.isSide()) {
            return "Edge";
        } else if (junctions.containsKey(vertex)) {
            return "Junction";
        } else if (vertex.isSink() || getNeighbours(mapGraph, vertex).size() == 1) {
            return "Sink";
        } else {
            return "Stop";
        }
    }

    private List<NetworkVertex> getNeighbours(NetworkGraph graph, NetworkVertex vertex) {
        MapHex hex = vertex.getHex();
        Tile tile = hex.getCurrentTile();
        HexSide rotation = hex.getCurrentTileRotation();

        List<NetworkVertex> neighbours = new ArrayList<>();
        Set<Track> tracks = tile.getTracks();
        for (Track track : tracks) {
            NetworkVertex from = graph.getVertex(hex, track.getStart().rotate(rotation));
            NetworkVertex to = graph.getVertex(hex, track.getEnd().rotate(rotation));
            if (vertex == from) {
                neighbours.add (to);
            } else if (vertex == to) {
                neighbours.add (from);
            }
        }
        return neighbours;
    }

    private Junction createJunction(NetworkVertex item) {
        Junction junction = null;
        if (!junctions.containsKey(item)) {
            junction = new Junction(item);
            junctions.put (item, junction);
            log.debug ("+++++ New junction created at {}", item);
        }
        return junction;
    }

    /*----------------------*/
    /* GRAPH ACCESS METHODS */
    /*----------------------*/

    public List<Segment> getSegments (Stop stop) {
        NetworkVertex vertex = vertexOfStop.get (stop);
        return junctions.get(vertex).getSegments();
    }

    /*---------------*/
    /* INNER CLASSES */
    /*---------------*/

    /** A junction is a Rails item where more than two tracks come together.
     * It can be a stop (village or city) or an edge (where plain track splits).
     *
     * TODO: Check whether this class is really necessary, it plays no role in Routes
     */
    public class Junction {

        NetworkVertex junction;
        List<Segment> segments;

        Junction(NetworkVertex vertex) {
            this.junction = vertex;
            segments = new ArrayList<>();
        }

        void addSegment(Segment segment, int marker) {
            if (!segments.contains(segment)) {
                segments.add(segment);
                log.debug("++{}++ Added segment {} to junction {}", marker, segment, this);
            } else {
                log.debug ("      Junction {} already has segment {}", this, segment);
            }
        }

        void removeSegment(Segment segment, int marker) {
            segments.remove(segment);
            log.debug("--{}-- Removed segment {} from junction {}", marker, segment, this);
        }

        List<Segment> getSegments() {
            return segments;
        }

        boolean hasSegment(Segment segment) {
            return segments.contains(segment);
        }

        NetworkVertex getVertex() {
            return junction;
        }

        public String toString() {
            return junction + " => " + segments;
        }

    } // End of Junction inner class

    /** A segment is a stretch of items between two nodes (branch points)
     * including all edges and stops
     */
    public class Segment extends LinkedList<NetworkVertex> {

        boolean isForward;
        boolean isOpen;

        Segment () {
            super();
            isForward = true;
            isOpen = true;
        }

        Segment (NetworkVertex vertex) {
            this();
            addItem(vertex);
        }

        Segment (Segment segmentToCopy) {
            this();
            segmentToCopy.setIterator (segmentToCopy.getEnd(true));
            while (segmentToCopy.hasNext()) {
                this.addItem(segmentToCopy.getNext());
            }
        }

        public void addItem(NetworkVertex item) {
            if (isOpen) add (item);
        }

        public void close() {
            isOpen = false;
        }

        public String toString() {

            if (size() > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(getFirst());
                if (size() > 1) sb.append("=>").append(getLast());
                return sb.toString();
            } else {
                return "empty";
            }
        }

        public String toLongString() {
            if (size() > 0) {
                StringBuilder segString = new StringBuilder();
                setIterator(getFirst());
                while (hasNext()) {
                    if (segString.length() > 0) segString.append("->");
                    segString.append(getNext().toString());
                }
                return segString.toString();
            } else {
                return "empty";
            }
        }

        /*---------------------*/
        /* GRAPH USAGE METHODS */
        /*---------------------*/

        /**
         * Iteration through a segment uses the iterators provided
         * by LinkedList. However, these are not symmetrical:
         * there is no method to start a backwards iteration
         * in the middle of a segment. So I had to provide that myself (EV).
         *
         * These iterators are hidden from the calling code;
         * there can only be one iterator active at any time.
         */
        private Iterator<NetworkVertex> iterator;

        /** These values refer to the first and last vertex
         *  being visited in an actual scan
         */
        NetworkVertex from = null;
        NetworkVertex to = null;

        /**
         * Get Iterator from one of the segment ends
         * @param startVertex should be either the first or the last vertex
         */
        public void setIterator(NetworkVertex startVertex) {
            if (startVertex.equals(getFirst())) {
                isForward = true;
                iterator = listIterator();
            } else if (startVertex.equals(getLast())) {
                isForward = false;
                iterator = descendingIterator();
            } else {
                log.error ("Start vertex {} is not at one end of segment {}",
                        startVertex, this);
            }
        }

        /**
         * @param fromVertex may be any NetworkVertex in the Segment
         * @param toVertex is the segment end towards the iteration will run
         *                 (ignored if fromVertex is at one end of the segment)
         */
        public void setIterator(NetworkVertex fromVertex, NetworkVertex toVertex) {
            this.from = fromVertex;
            this.to = toVertex;
            if (isAtEnd(fromVertex)) { // ignore given direction
                to = otherEnd(fromVertex);
                isForward = to.equals(getLast());
                setIterator(fromVertex);
            } else if (isAtEnd(toVertex)) {
                isForward = to.equals(getLast());
                int i = indexOf(fromVertex);
                if (isForward) {
                    iterator = listIterator(i);
                } else {
                    iterator = descendingIterator();
                    // The index version does not exist, so the skipping must be done here.
                    while (iterator.hasNext() && !iterator.next().equals(fromVertex)) ;
                }
            }
        }

        public void setIteratorReversed(NetworkVertex fromVertex) {
            NetworkVertex toVertex;
            if (isForward) {
                toVertex = getFirst();
            } else {
                toVertex = getLast();
            }
            setIterator (fromVertex, toVertex);
        }

        public NetworkVertex otherEnd(NetworkVertex thisVertex) {
            if (thisVertex.equals(getFirst())) {
                return getLast();
            } else if (thisVertex.equals(getLast())) {
                return getFirst();
            } else {
                log.error ("ERROR: {} is not at an end of segment {}", thisVertex, this);
                return null;
            }
        }

        /** Return the next vertex in the current direction */
        public NetworkVertex getNext() {
            return iterator.next();
        }

        /** Check if the iterator has more items in the current direction */
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /** Get the first vertex in the current direction */
        public NetworkVertex getEnd(boolean first) {
            if (isForward == first) {
                return super.getFirst();
            } else {
                return super.getLast();
            }
        }

        public boolean isAtEnd (NetworkVertex vertex) {
            return vertex.equals(getFirst()) || vertex.equals(getLast());
        }

        public boolean isForward() {
            return isForward;
        }

        public NetworkVertex getFrom() {
            return from;
        }

        public NetworkVertex getTo() {
            return to;
        }

        public int indexOf (NetworkVertex vertex) {
            for (int i=0; i<size()-1; i++) {
                if (vertex.toString().equals(get(i).toString())) {
                    return i;
                }
            }
            return -1;
        }

    } // End of Segment inner class
}
