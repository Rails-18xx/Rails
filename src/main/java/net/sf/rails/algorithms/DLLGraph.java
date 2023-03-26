package net.sf.rails.algorithms;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
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
 *   This can be stations where plain track branches off.
 *   Edges where track branches are handles by creating
 *   separate segments per branch.
 *
 * - Dead ends: stations where only one track is connected to.
 *   Multiple track stations where the company cannot run through 
 *   for whatever reason are also considered dead ends.
 *   For simplicity, all dead ends are also considered Junctions
 *   (a not very appropriate name in this case).
 *   
 * - Segments: doubly linked lists connecting junctions (and dead ends),
 *   included all TrackPoints on that route.
 *   Each edge where tracks of two different tiles meet, is
 *   represented by two TrackPoints in a Segment, one in each tile.
 *   The two-way linking ensures that backtracking is possible.
 *   
 * DLLGraph has two inner classes: Junction and Segment.
 * Segment extends the java LinkedList class.
 * Junction currently does not extend NetworkVertex for the sole
 * reason that NetworkVertex is declared final. For now I will leave it so.
 *   
 * This class has been created to support the 1826 feature that
 * the hex distances that define token laying costs have to be counted
 * along existing routes, rather than as the crow flies (as in 1835).
 * 
 * While working on 1826, I initially managed to calculate these costs
 * correctly using the NetworkIterator provided by the existing Company
 * Graph code. But as it turned out, my algorithm failed when closed
 * loops arose. So far, I did not manage to fix this.
 * No doubt there must be a way, but I have given up.
 * 
 * DLLGraph only creates the structure mentioned above. The actual
 * cost calculation will (for now) be done in the Routes class.
 *   
 * By Erik Vos, 10/2022-03/2023.
 */
public class DLLGraph {

    private static final Logger log = LoggerFactory.getLogger(DLLGraph.class);

    private PublicCompany company;
    private boolean toHomeOnly;
    private RailsRoot root;

    private List<Segment> segments;
    private Multimap<NetworkVertex, Segment> danglingSegments;
    private Map<NetworkVertex, Junction> junctions;
    private Multimap<NetworkVertex, NetworkVertex> backwardEdges;

    private NetworkGraph mapGraph;

    private Map<Stop, NetworkVertex> vertexOfStop;

    public DLLGraph (PublicCompany company, boolean toHomeOnly) {
        this.company = company;
        this.toHomeOnly = toHomeOnly;
        root = company.getRoot();

        if (log.isDebugEnabled()) logLegend();
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

        /* Elements for a new network structure, where
         * track sequences can be traversed in both directions.
         */
        danglingSegments = ArrayListMultimap.create();
        backwardEdges = ArrayListMultimap.create();
        junctions = new TreeMap<>();
        segments = new ArrayList<>();

        NetworkVertex expectedEdge = null;

        /* Build an object network of nodes and segments,
         * where the segments can be traversed in both directions.
         */
        for (Stop baseTokenStop : usableBases) {
            //if (beenAtStop.containsKey(baseTokenStop)) continue;

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
                List<NetworkVertex> neighbours = getNeighbours(item);
                log.debug("      Checking {} item {}, neighbours {}", item.getType(), item, neighbours);
                if (item.isStation()) {
                    Stop stop = item.getStop();
                    vertexOfStop.put (stop, item);

                    log.debug("  S   {} is a stop ({})", item, stop);

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
                    if ((neighbours.size() > 2 && stop.isRunThroughAllowedFor(company)
                            || stop.hasTokenOf(company))) {
                        // We are at a branching point, or a search starter
                        junction = junctions.get(item);

                        // Finish current Segment
                        if (currentSegment != null) { // Can it ever be null?
                            // Finish the old segment
                            addItem(currentSegment, item, 1);
                            checkForBackwardBranches(currentSegment);
                            closeSegment (currentSegment, item, 1);
                        }

                        // Create new segments if the stop can be run through
                        if (newJunction && stop.isRunThroughAllowedFor(company)) {
                            for (NetworkVertex neighbour : neighbours) {

                                if (currentSegment == null || !currentSegment.contains(neighbour)) {
                                    Segment newSegment = new Segment(item);
                                    newSegment.add(neighbour);
                                    junction.addSegment(newSegment, 3);
                                    danglingSegments.put(neighbour, newSegment);
                                    log.debug(">>3>> Opened new segment {}, dangling at {}",
                                            newSegment, neighbour);
                                }
                            }
                        }
                        if (currentSegment != null && !currentSegment.isOpen()) currentSegment = null;

                    } else if (neighbours.size() == 1 || item.isSink() || !stop.isRunThroughAllowedFor(company)) {
                        // This is an end point
                        if (currentSegment != null) {
                            // Add it to the segment and close that
                            addItem(currentSegment, item, 2);
                            closeSegment(currentSegment, item, 2);
                            checkForBackwardBranches(currentSegment);
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
                                /* Duplicates above code
                                if (item.isSink() || !stop.isRunThroughAllowedFor(company)) {
                                    // This stop is a sink
                                    addItem(currentSegment, item, 3);

                                    closeSegment(currentSegment, item, 3);
                                    checkForBackwardBranches(currentSegment);
                                    currentSegment = null;

                                } else {*/
                                    addItem(currentSegment, item, 4);
                                //}

                            } else {
                                // Start hex, not yet visited
                                currentSegment = openSegment(item, 4);
                            }

                        }

                    }
                    beenAtStop.put(stop, 0);

                } else if (item.isSide()) {

                    entering = !hex.equals(prevHex);

                    if (entering && currentSegment != null) {

                        // Check if we haven't made a strange jump
                        if (expectedEdge != item) {
                            log.debug("      Expected {}, actual {}", expectedEdge, item);
                            if (expectedEdge != null) {
                                log.debug("!!!!! We have a jump from {} to {}, expected {}", prevItem, item, expectedEdge);
                                // Join the two unfinished segments and then continue normally
                                joinSegments(currentSegment, expectedEdge, 5);
                                currentSegment = null;
                            }
                        }
                    }
                    expectedEdge = null;

                    log.debug ("  E   {} is an edge", item);
                    if (currentSegment == null) {
                        // Find a dangling segment where this item is at the end
                        if (danglingSegments.containsKey(item)) {
                            List<Segment> segments = Lists.newArrayList (danglingSegments.get(item));
                            log.debug("===== Side {} found at dangling segment(s) {}", item, segments);

                            // If there is more than one segment, we have a backwards branch.
                            // FIXME ***** FOR NOW, PICK THE FIRST ONE *****
                            for (Segment segment : segments) {
                                currentSegment = segment;
                                break;
                            }
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
                            if (!currentSegment.isOpen()) currentSegment = null;
                        } else if (neighbours.size() > 1) {
                            if (entering) {
                                // We enter hex at a branching point, remember exit points
                                exits.clear(); // For all certainty, should be redundant
                                exits.addAll(neighbours);
                            } else {
                                // We are leaving a hex having a backward entry point
                                /**/
                                for (NetworkVertex neighbour : neighbours) {
                                    if (!currentSegment.contains(neighbour)) {
                                        backwardEdges.put (item, neighbour);
                                    }
                                }/**/
                            }
                        }
                        if (currentSegment != null) {
                            // If we are leaving a branching tile, duplicate this segment
                            // as many times as there are other exits
                            if (!entering && !exits.isEmpty()) {
                                for (NetworkVertex exit : exits) {
                                    if (exit.isSide() && exit != item) { // Not the current exit side
                                        Segment newSegment = new Segment(currentSegment, 8);
                                        newSegment.add(exit);
                                        danglingSegments.put (exit, newSegment);
                                        junctions.get(newSegment.getFirst()).addSegment(newSegment, 8);
                                        log.debug("++8++ Added extra dangling segment {} to {}",
                                                newSegment, newSegment.getFirst());


                                    }
                                }
                                exits.clear();
                            }
                            if (currentSegment.isOpen()) addItem(currentSegment, item, 6);
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

        List<Segment> segsToRemove = new ArrayList<>();
        for (Segment s : segments) {
            if (s.size() <= 1 || s.getFirst().equals(s.getLast())) segsToRemove.add (s);
        }
        for (Segment s : segsToRemove) {
            discardSegment(s, 0);
        }

        // Join remaining dangling segments at the same vertex
        Multimap<NetworkVertex, DLLGraph.Segment> dsCopy = ArrayListMultimap.create (danglingSegments);
        for (NetworkVertex vertex : dsCopy.keySet()) {
            List<Segment> segments = (List<Segment>) dsCopy.get(vertex);
            if (segments.size() >= 2) {
                Segment segment1 = segments.get(0);
                for (Segment segment2 : segments) {
                    if (!segment2.equals(segment1)
                            && !segment2.getNextTo(vertex).equals(segment1.getNextTo(vertex))) {
                        joinSegments (segment1, vertex, 9);
                    }
                }

            }
        }


        // Remove all remaining dangling and single-vertex, and returning segments
        // (Not sure if these are features or bugs - EV)
        segsToRemove = new ArrayList<>();
        for (NetworkVertex v : danglingSegments.keySet()) {
            for (Segment s : danglingSegments.get(v)) {
                segsToRemove.add (s);
            }
        }
        for (Segment s : segsToRemove) {
            discardSegment(s, 0);
        }

        if (log.isDebugEnabled()) {
            logLegend();
            log.debug(toString());
        }
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        int i;
        b.append("\nJunctions:\n");
        for (NetworkVertex j : junctions.keySet()) b.append("   ").append(junctions.get(j)).append("\n");
        b.append("Segments:\n"); i=0;
        for (Segment s : segments) b.append ("   ").append(segments.get(i++).toLongString()).append("\n");
        b.append("Dangling segments:\n");
        if (danglingSegments.isEmpty()) {
            b.append("   None\n");
        } else {
            for (NetworkVertex v : danglingSegments.keySet()) {
                for (Segment s : danglingSegments.get(v)) {
                    b.append("   ").append(v).append(" : ").append(s.toLongString()).append("\n");
                }
            }
        }
        return b.toString();
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
        danglingSegments.remove(segment.getLast(), segment);
        segment.add (vertex);
        danglingSegments.put(vertex, segment);

        log.debug ("++{}++ Added {} {} to {}, becoming {}",
                marker, vertexType (vertex), vertex, oldSegment, segment.toLongString());
    }

    private void closeSegment(Segment segment, NetworkVertex finalVertex, int marker) {
        log.debug("##{}## Closing segment {} at {} {}",
                marker, segment, vertexType(finalVertex), finalVertex);
        Junction junction = junctions.get(finalVertex);
        if (junction == null) junction = createJunction(finalVertex);
        junction.addSegment(segment, marker);
        log.debug ("  {}   Junction {} at {} stored for {}", marker, junction, finalVertex, segment);

        if (isDuplicate(segment)) {
            discardSegment(segment, marker);
        } else {
            segment.close();
        }
        danglingSegments.remove(finalVertex, segment);
        segment.iterator = null;
    }

    public boolean isDuplicate (Segment segment) {

        Junction first = junctions.get(segment.getFirst());
        Junction last = junctions.get(segment.getLast());
        if (first == null || last == null) return false;

        Junction first2, last2;
        // Identical duplicate?
        for (Segment s : first.getSegments()) {
            if (s.equals(segment)) continue;
            first2 = junctions.get(s.getFirst());
            last2 = junctions.get(s.getLast());
            if (first.equals(first2) && last.equals(last2)) {
                if (!isIdentical(segment, s, false)) return false;
            } else if (first.equals(last2) && last.equals(first2)) {
                if (!isIdentical(segment, s, true)) return false;
            } else {
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean isIdentical(Segment s1, Segment s2, boolean reversed) {
        Segment s1c = new Segment (s1, 0);
        Segment s2c = new Segment (s2, 0);
        s1c.setIterator(s1c.getFirst());
        s2c.setIterator(reversed ? s2c.getLast() : s2c.getFirst());
        while (s1c.hasNext() && s2c.hasNext()) {
            if (!s1c.getNext().equals(s2c.getNext())) return false;
        }

        return true;
    }

    private void checkForBackwardBranches (Segment segment) {
        // Check if we have backward dangling edges from this closing segment
        Segment template = new Segment();
        Segment backSegment;
        // Scan the segment for branching points
        NetworkVertex backItem = segment.getLast();
        segment.setIteratorReversed(backItem);
        Junction junction = junctions.get(backItem);

        /**/
        while (segment.hasNext()) {
            backItem = segment.getNext();
            if (backwardEdges.containsKey(backItem)) {
                for (NetworkVertex backEdge : backwardEdges.get(backItem)) {
                    backSegment = new Segment (template, 9);
                    backSegment.add (backItem);
                    // Check if there already is a dangling segment at this edge
                    if (danglingSegments.containsKey(backEdge)) {
                        // Join the segments
                        joinSegments(backSegment, backEdge, 9);
                    } else {
                        backSegment.add(backEdge);
                        log.debug("++9++ Created backwards dangling segment {} : {}",
                                backSegment, backSegment.toLongString());
                        junction.addSegment(backSegment, 9);
                        danglingSegments.put(backEdge, backSegment);
                    }
                }
            }
            template.addItem(backItem);
        }
        discardSegment(template, 9);
        /**/
    }

    private void joinSegments (Segment oldSegment, NetworkVertex joiningEdge, int marker) {
        // Make a new ArrayList to prevent ConcurrentModificationException
        List<Segment> otherSegments = Lists.newArrayList (danglingSegments.get(joiningEdge));
        if (otherSegments == null || otherSegments.isEmpty()) {
            log.debug("?!{}!? Cannot find a dangling segment at {}", marker, joiningEdge);
            return;
        }

        for (Segment otherSegment : otherSegments) {
            if (oldSegment.equals(otherSegment)) continue;

            // Skip one-item segments
            // FIXME: these should not be created!
            if (otherSegment.size() <= 1) continue;
            // The next-to-last vertices of both segments must be in different hexes,
            // otherwise we would be backtracking.
            log.debug ("At {} trying to join {} to {}", joiningEdge, oldSegment.toLongString(), otherSegment.toLongString());
            if (oldSegment.getNextToLast().getHex().equals(otherSegment.getNextToLast().getHex())) {
                // Backtracking, so skip this segment
                log.debug("      Skipping backtracking join of segments {} and {}",
                        oldSegment, otherSegment);
                continue;
            }


            log.debug("  {}   Found joinable dangling segment {} at {}", marker, otherSegment, joiningEdge);
            // Create a new segment, we may need the old one twice (or more).
            Segment newSegment = new Segment(oldSegment, marker);
            NetworkVertex endVertex = oldSegment.getLast();

            otherSegment.setIterator(joiningEdge, otherSegment.getFirst());
            while (otherSegment.hasNext()) {
                NetworkVertex vertex = otherSegment.getNext();
                if (vertex != endVertex) {
                    String ns = newSegment.toString();
                    newSegment.add(vertex); // Avoid joining edge duplication
                    log.debug ("++{}++ Added {} {} to {}, becoming {}",
                            marker, vertexType (vertex), vertex, ns, newSegment.toLongString());

                }
            }
            log.debug("++{}++ At {} {} joined {} to {}, becoming {}", marker,
                    vertexType(joiningEdge), joiningEdge, oldSegment, otherSegment, newSegment.toLongString());
            NetworkVertex lastItem = otherSegment.getLast();
            junctions.get(newSegment.getFirst()).addSegment(newSegment, 5);
            junctions.get(newSegment.getLast()).addSegment(newSegment, 5);
            closeSegment(newSegment, newSegment.getLast(), 5); // Joins junction at end

            danglingSegments.remove(joiningEdge, otherSegment);
            log.debug("--{}-- Removed dangling segment {}", marker, otherSegment);
            discardSegment(otherSegment, 5);
        }
        discardSegment(oldSegment, 5);

    }

    private void pruneSegment (Segment segment) {
        while (segment.getLast().isSide()) {
            removeItem(segment, segment.getLast(), 7);
        }
        if (segment.size() == 1) {
            // Drop empty segment
            discardSegment (segment, 7);
        } else {
            closeSegment (segment, segment.getLast(), 6);
        }
    }

    private void removeItem (Segment segment, NetworkVertex item, int marker) {
        String oldSegment = segment.toString();
        danglingSegments.remove(item, segment);
        segment.remove(item);
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
        segment.close();
        segments.remove(segment);
    }

    private String vertexType (NetworkVertex vertex) {
        if (vertex.isSide()) {
            return "Edge";
        } else if (junctions.containsKey(vertex)) {
            return "Junction";
        } else if (vertex.isSink() || getNeighbours(vertex).size() == 1) {
            return "Sink";
        } else {
            return "Stop";
        }
    }

    public List<NetworkVertex> getNeighbours(NetworkVertex vertex) {

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

    private Junction createJunction(NetworkVertex item) {
        Junction junction = null;
        if (!junctions.containsKey(item)) {
            junction = new Junction(item);
            log.debug ("+++++ New junction {} created at {}", junction, item);
        }
        return junction;
    }

    /*----------------------*/
    /* GRAPH ACCESS METHODS */
    /*----------------------*/

    public List<Segment> getSegments (Stop stop, int marker) {
        NetworkVertex vertex = vertexOfStop.get (stop);
        //log.debug("  {}   Stop {} has segments {}", marker, stop, junctions.get(vertex).getSegments());
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
            junction = vertex;
            segments = new ArrayList<>();
            junctions.put(vertex, this);
        }

        void addSegment(Segment segment, int marker) {
            if (!segment.contains(junction)) log.error("??{}?? Adding segment {} to junction {} that is no part of it!",
                    marker, segment, junction);
            if (!segments.contains(segment)) {
                segments.add(segment);
                log.debug("++{}++ Added segment {} to junction {}", marker, segment, this);
            } else {
                log.debug ("--{}-- Junction {} already has segment {}", marker, this, segment);
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

        public NetworkGraph getMapGraph() {
            return mapGraph;
        }

        public String toString() {
            return junction + " : " + segments;
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
            segments.add (this);
        }

        Segment (NetworkVertex vertex) {
            this();
            addItem(vertex);
            log.debug("+++++ Created new segment {}", this);
        }

        Segment (Segment segmentToCopy, int marker) {
            this();
            segmentToCopy.setIterator (segmentToCopy.getEnd(true));
            while (segmentToCopy.hasNext()) {
                this.addItem(segmentToCopy.getNext());
            }
            if (marker > 0) {
                log.debug("++{}++ Created copied segment {} from {} at {}", marker, this, segmentToCopy, getFirst());
            }

        }

        public void addItem(NetworkVertex item) {
            if (isOpen) add (item);
        }

        public void close() {
            isOpen = false;
            iterator = null;
        }

        public boolean isOpen() {
            return isOpen;
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
         * a segment can only have one iterator active at any time.
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
            iterator = null;
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

        public Segment reverse() {
            Segment reversed = new Segment();
            setIteratorReversed(getLast());
            while (hasNext()) {
                reversed.add(getNext());
            }
            log.debug ("      Created reversed from {} : {}", this, reversed);
            return reversed;
        }

        public boolean isDuplicate (Segment s) {
            return isIdentical(this, s, false)
                    || isIdentical(this, s, true);
        }

        public boolean isAtEnd (NetworkVertex vertex) {
            return vertex.equals(getFirst()) || vertex.equals(getLast());
        }

        public NetworkVertex getFrom() {
            return from;
        }

        public NetworkVertex getTo() {
            return to;
        }

        /** Get the next-to-first or next-to-last vertex of this segment.
         * Will return null if the argument is not the first or last vertex.
         * @param end First or last vertex of this segment
         * @return The vertex next to the first or last one, or null
         */
        public NetworkVertex getNextTo (NetworkVertex end) {
            if (end.equals(getLast())) {
                return get (indexOf(getLast()) - 1);
            } else if (end.equals(getFirst())) {
                return get(1);
            }
            return null;
        }

        public NetworkVertex getNextToLast () {
            return get (indexOf(getLast()) - 1);
        }

    } // End of Segment inner class

    /** Legend of marker values used in debug logging */
    private void logLegend() { // not finished yet
        log.debug("Legend:");
        log.debug(" 1 = closing a segment at a stop with at least 3 exits");
        log.debug(" 2 = closing a segment at a sink (stop with one usable exit)");
        log.debug(" 3 = opening a new segment at a stop with at least 3 exits");
        log.debug(" 4 = adding vertices in normal progress, including 2-exit stops");
        log.debug(" 5 = joining two dangling segments");
        log.debug(" 6 = closing a segment after pruning, at an intermediate (2-exit) stop");
        log.debug(" 7 = discarding a segment ending at a non-track tile, having no intermediate stops");
        log.debug(" 8 = duplicating segment at forward branch");
        log.debug(" 9 = duplicating segment at backward branch");
    }
}
