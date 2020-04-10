package net.sf.rails.algorithms;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.*;

import net.sf.rails.game.*;
import net.sf.rails.ui.swing.hexmap.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jgrapht.graph.SimpleGraph;


public final class NetworkVertex implements Comparable<NetworkVertex> {

    private static final Logger log = LoggerFactory.getLogger(NetworkVertex.class);

    public static enum VertexType {
        STATION,
        SIDE,
        HQ,
    }
    public static enum StationType {
        MAJOR,
        MINOR,
        COALMINE
    }

    // vertex types and flag for virtual (thus not related to a rails object)
    private final VertexType type;
    private final boolean virtual;

    // vertex properties (for virtual vertexes)
    private final String virtualId;

    // general vertex properties
    private StationType stationType;
    private int value = 0;
    private boolean sink = false;
    private String stopName = null;

    // references to rails objects, if not virtual
    private final MapHex hex;
    private final TrackPoint trackPoint;
    // only for station objects
    private final Stop stop;


    /** constructor for station on mapHex */
    public NetworkVertex(MapHex hex, Station station) {
        this.type = VertexType.STATION;
        this.hex = hex;
        this.trackPoint = station;
        this.stop = hex.getRelatedStop(station);
        if (stop != null) {
            log.debug("Found stop {}", stop);
        } else {
            log.debug("No stop found");
        }

        this.virtual = false;
        this.virtualId = null;
    }

    /** constructor for side on mapHex */
    public NetworkVertex(MapHex hex, HexSide side) {
        this.type = VertexType.SIDE;
        this.hex = hex;
        this.trackPoint = side;
        this.stop = null;

        this.virtual = false;
        this.virtualId = null;
    }

    /**  constructor for public company hq */
    public NetworkVertex(PublicCompany company) {
        this(VertexType.HQ, "HQ");
    }

    private NetworkVertex(VertexType type, String name) {
        this.type = type;
        this.hex = null;
        this.trackPoint = null;
        this.stop = null;

        this.virtual = true;
        this.virtualId = name;
    }

    /** factory method for virtual vertex
     */
    public static NetworkVertex getVirtualVertex(VertexType type, String name) {
        NetworkVertex vertex = new NetworkVertex(type, name);
        return vertex;
    }

    void addToRevenueCalculator(RevenueCalculator rc, int vertexId) {
        rc.setVertex(vertexId, isMajor(), isMinor(), sink);
    }

    public String getIdentifier(){
        if (virtual) {
            return virtualId;
        } else {
            return hex.getId() + "." + trackPoint.getTrackPointNumber();
        }
    }

    public boolean isVirtual() {
        return virtual;
    }

    public boolean isStation(){
        return type == VertexType.STATION;
    }

    public boolean isSide(){
        return type == VertexType.SIDE;
    }

    public boolean isHQ(){
        return type == VertexType.HQ;
    }

    public VertexType getType() {
        return type;
    }

    public boolean isMajor(){
        return (stationType != null && stationType == StationType.MAJOR);
    }

    public boolean isMinor(){
        return (stationType != null && stationType == StationType.MINOR);
    }

    public boolean isMine( ) {
        return (stationType != null && stationType == StationType.COALMINE);
    }

    public StationType getStationType() {
        return stationType;
    }

    public NetworkVertex setStationType(StationType stationType) {
        this.stationType = stationType;
        return this;
    }

    public int getValue() {
        return value;
    }

    public int getValueByTrain(NetworkTrain train) {
        int valueByTrain;
        if (isMajor()) {
            valueByTrain = value * train.getMultiplyMajors();
        } else if (isMinor()) {
            if (train.ignoresMinors()) {
                valueByTrain = 0;
            } else {
                valueByTrain = value * train.getMultiplyMinors();
            }
        } else if (isMine()) { //Sofar Coal mines count nothing for the dividend income, the revenue from a mine is counted differently.
            valueByTrain = 0;
        } else {
            valueByTrain = value;
        }
        return valueByTrain;
    }

    public NetworkVertex setValue(int value) {
        this.value = value;
        return this;
    }

    public boolean isSink() {
        return sink;
    }

    public NetworkVertex setSink(boolean sink) {
        this.sink = sink;
        return this;
    }

    public String getStopName() {
        return stopName;
    }

    public NetworkVertex setStopName(String locationName) {
        this.stopName = locationName;
        return this;
    }

    // getter for rails objects
    public MapHex getHex(){
        return hex;
    }

    public Station getStation(){
        if (type == VertexType.STATION) {
            return (Station) trackPoint;
        } else {
            return null;
        }
    }

    public HexSide getSide(){
        if (type == VertexType.SIDE) {
            return (HexSide) trackPoint;
        } else {
            return null;
        }
    }

    public Stop getStop() {
        return stop;
    }

    public boolean isOfType(VertexType vertexType, StationType stationType) {
        return (type == vertexType && (!isStation() || getStationType() == stationType));
    }

    /**
     * Initialize for rails vertexes
     * @return true = can stay inside the network, false = has to be removed
     */
    public boolean initRailsVertex(PublicCompany company) {
        // side vertices use the defaults, virtuals cannot use this function
        if (virtual || type == VertexType.SIDE) return true;

        // Only station remains
        Station station = (Station) trackPoint;

        log.debug("Init of vertex {}", this);

        // check if it has to be removed because it is run-to only
        // if company == null, then no vertex gets removed
        if (company != null && !stop.isRunToAllowedFor(company)) {
           log.info("Vertex is removed");
           return false;
        }

        // check if it is a major or minor
        if (stop.getScoreType() == StopType.Score.MAJOR) {
            setStationType(StationType.MAJOR);
        } else if (stop.getScoreType() == StopType.Score.MINOR) {
            setStationType(StationType.MINOR);
        }

        // check if it is a sink
        if (company == null) { // if company == null, then all sinks are deactivated
            sink = false;
        } else {
            sink = !stop.isRunThroughAllowedFor(company);
        }

        // define locationName
        stopName = null;
        if (station.getType() == Station.Type.OFFMAPCITY) {
            if (hex.getStopName() != null && !hex.getStopName().equals("")) {
                stopName = hex.getStopName();
            }
        } else {
            if (hex.getStopName() != null && !hex.getStopName().equals("")
                    && station.getStopName() != null && !station.getStopName().equals("")) {
                stopName = hex.getStopName() + "." + station.getStopName();
            }
        }

        // no removal
        return true;

    }

    public void setRailsVertexValue(Phase phase) {
        // side vertices and  virtuals cannot use this function
        if (virtual || type == VertexType.SIDE) return;

        // define value
        value = stop.getValueForPhase(phase);
    }


    @Override
    public String toString(){
        StringBuilder message = new StringBuilder();
        if (isVirtual())
            message.append(virtualId);
        else if (isStation())
            message.append(hex.getId()).append(".").append(((Station) trackPoint).getNumber());
        else if (isSide())
            message.append(hex.getId()).append(".").append(hex.getOrientationName((HexSide) trackPoint));
        else
            message.append("HQ");
        if (isSink())
            message.append("/*");
        return message.toString();
    }

    public int compareTo(NetworkVertex otherVertex) {
        return this.getIdentifier().compareTo(otherVertex.getIdentifier());
    }

    public static final class ValueOrder implements Comparator<NetworkVertex> {

        public int compare(NetworkVertex vA, NetworkVertex vB) {
            int result = -((Integer)vA.getValue()).compareTo(vB.getValue()); // compare by value, descending
            if (result == 0)
                result = vA.compareTo(vB); // otherwise use natural ordering
            return result;
        }
    }

    /**
     *
     * @param graph network graph
     * @param company the company (with regard to values, sinks and removals)
     * @param phase the current phase (with regard to values)
     */
    public static void initAllRailsVertices(NetworkGraph graph,
            PublicCompany company,  Phase phase) {

        // store vertices for removal
        List<NetworkVertex> verticesToRemove = new ArrayList<NetworkVertex>();
        for (NetworkVertex v:graph.getGraph().vertexSet()) {
            if (company != null) {
                if (!v.initRailsVertex(company)) {
                    verticesToRemove.add(v);
                }
            }
            if (phase != null) {
                v.setRailsVertexValue(phase);
            }
        }
        graph.getGraph().removeAllVertices(verticesToRemove);
    }

    /**
     * Returns the maximum positive value (lower bound zero)
     */
    public static int maxVertexValue(Collection<NetworkVertex> vertices) {
        int maximum = 0;
        for (NetworkVertex vertex:vertices) {
            maximum = Math.max(maximum, vertex.getValue());
        }
        return maximum;
    }


    /**
     * Return the sum of vertex values
     */
    public static int sum(Collection<NetworkVertex> vertices) {
        int sum = 0;
        for (NetworkVertex vertex:vertices) {
            sum += vertex.getValue();
        }
        return sum;
    }

    /**
     * Returns the number of specified vertex type in a vertex collection
     * If station then specify station type
     */
    public static int numberOfVertexType(Collection<NetworkVertex> vertices, VertexType vertexType, StationType stationType) {
        int number = 0;
        for (NetworkVertex vertex:vertices) {
            if (vertex.isOfType(vertexType, stationType)) number++;
        }
        return number;
    }

    /**
     * creates a new virtual vertex with identical properties and links
     */
    public static NetworkVertex duplicateVertex(SimpleGraph<NetworkVertex, NetworkEdge> graph,
            NetworkVertex vertex, String newIdentifier, boolean addOldVertexAsHidden) {
        // create new vertex
        NetworkVertex newVertex = NetworkVertex.getVirtualVertex(vertex.type, newIdentifier);
        // copy values
        newVertex.stationType = vertex.stationType;
        newVertex.value = vertex.value;
        newVertex.sink = vertex.sink;
        newVertex.stopName = vertex.stopName;
        graph.addVertex(newVertex);
        // copy edges
        Set<NetworkEdge> edges = graph.edgesOf(vertex);
        for (NetworkEdge edge:edges) {
            List<NetworkVertex> hiddenVertices;
            if (edge.getSource() == vertex) {
                hiddenVertices = edge.getHiddenVertices();
                if (addOldVertexAsHidden) hiddenVertices.add(vertex);
                NetworkEdge newEdge = new NetworkEdge(newVertex, edge.getTarget(), edge.isGreedy(), edge.getDistance(), hiddenVertices);
                graph.addEdge(newVertex, edge.getTarget(), newEdge);
            } else {
                hiddenVertices = new ArrayList<NetworkVertex>();
                if (addOldVertexAsHidden) hiddenVertices.add(vertex);
                hiddenVertices.addAll(edge.getHiddenVertices());
                NetworkEdge newEdge = new NetworkEdge(edge.getSource(), newVertex, edge.isGreedy(), edge.getDistance(), hiddenVertices);
                graph.addEdge(newEdge.getSource(), newVertex, newEdge);
            }
        }
        return newVertex;
    }

    /**
     * replaces one vertex by another for a network graph
     * copies all edges
     */
    public static boolean replaceVertex(SimpleGraph<NetworkVertex, NetworkEdge> graph,
            NetworkVertex oldVertex, NetworkVertex newVertex) {
        // add new vertex
        graph.addVertex(newVertex);
        // replace old edges
        Set<NetworkEdge> oldEdges = graph.edgesOf(oldVertex);
        for (NetworkEdge oldEdge:oldEdges) {
            NetworkEdge newEdge = NetworkEdge.replaceVertex(oldEdge, oldVertex, newVertex);
            if (newEdge.getSource() == newVertex) {
                graph.addEdge(newVertex, newEdge.getTarget(), newEdge);
            } else {
                graph.addEdge(newEdge.getSource(), newVertex, newEdge);
            }
        }
        // remove old vertex
        return graph.removeVertex(oldVertex);
    }

    /**
     * Filters all vertices from a collection of vertices that lay in a specified collection of hexes
     */
    public static Set<NetworkVertex> getVerticesByHexes(Collection<NetworkVertex> vertices, Collection<MapHex> hexes) {
        Set<NetworkVertex> hexVertices = new HashSet<NetworkVertex>();
        for (NetworkVertex vertex:vertices) {
            if (vertex.getHex() != null && hexes.contains(vertex.getHex())) {
                hexVertices.add(vertex);
            }
        }
        return hexVertices;
    }

    /**
     * Returns all vertices for a specified hex
     */
    public static Set<NetworkVertex> getVerticesByHex(Collection<NetworkVertex> vertices, MapHex hex) {
        Set<NetworkVertex> hexVertices = new HashSet<NetworkVertex>();
        for (NetworkVertex vertex:vertices) {
            if (vertex.getHex() != null && hex == vertex.getHex()) {
                hexVertices.add(vertex);
            }
        }
        return hexVertices;
    }

    public static NetworkVertex getVertexByIdentifier(Collection<NetworkVertex> vertices, String identifier) {
        for (NetworkVertex vertex:vertices) {
            if (vertex.getIdentifier().equals(identifier)) {
                return vertex;
            }
        }
        return null;
    }

    public static Point2D getVertexPoint2D(HexMap map, NetworkVertex vertex) {
        if (vertex.isVirtual()) return null;

        GUIHex guiHex = map.getHex(vertex.getHex());
        if (vertex.isMajor()) {
            return guiHex.getStopPoint2D(vertex.getStop());
        } else if (vertex.isMinor()) {
            return guiHex.getStopPoint2D(vertex.getStop());
            //            return guiHex.getCenterPoint2D();
        } else if (vertex.isSide()) {
            // FIXME: Check if this still works
            return guiHex.getSidePoint2D(vertex.getSide());
        } else {
            return null;
        }
    }

    public static Rectangle getVertexMapCoverage(HexMap map, Collection<NetworkVertex> vertices) {

        Rectangle rectangle = null;

        // find coverage are of the vertices
        double minX=0,minY=0,maxX=0,maxY=0;
        for (NetworkVertex vertex:vertices) {
            Point2D point = getVertexPoint2D(map, vertex);
            if (point != null) {
                if (minX == 0) { // init
                    rectangle = new Rectangle((int)point.getX(), (int)point.getY(), 0, 0);
                    minX = point.getX();
                    minY = point.getY();
                    maxX = minX; maxY = minY;
                } else {
                    rectangle.add(point);
                    minX = Math.min(minX, point.getX());
                    minY = Math.min(minY, point.getY());
                    maxX = Math.max(maxX, point.getX());
                    maxY = Math.max(maxY, point.getY());
                }
            }
        }
        log.debug("Vertex Map Coverage minX={}, minY={}, maxX={}, maxY={}", minX, minY, maxX, maxY);
        //        Rectangle rectangle = new Rectangle((int)minX, (int)minY, (int)maxX, (int)maxY);
        log.debug("Created rectangle={}", rectangle);
        return (rectangle);
    }
}
