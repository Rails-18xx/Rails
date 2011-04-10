package rails.algorithms;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jgrapht.graph.SimpleGraph;

import rails.game.City;
import rails.game.MapHex;
import rails.game.PhaseI;
import rails.game.PublicCompanyI;
import rails.game.Station;
import rails.ui.swing.hexmap.EWHexMap;
import rails.ui.swing.hexmap.GUIHex;
import rails.ui.swing.hexmap.HexMap;

public final class NetworkVertex implements Comparable<NetworkVertex> {

    protected static Logger log =
        Logger.getLogger(NetworkVertex.class.getPackage().getName());

    public static enum VertexType {
        STATION,
        SIDE,
        HQ,
    }
    public static enum StationType {
        MAJOR,
        MINOR
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
    private String cityName = null;

    // references to rails objects, if not virtual
    private final MapHex hex;
    
    private final Station station;
    private final City city;
    private final int side;
    
    
    /** constructor for station on mapHex */
    public NetworkVertex(MapHex hex, Station station) {
        this.type = VertexType.STATION;
        this.hex = hex;
        this.station = station;
        this.side = -1;
        this.city = hex.getRelatedCity(station);
        if (city != null) {
            log.info("Found city " + city);
        } else {
            log.info("No city found");
        }

        this.virtual = false;
        this.virtualId = null;
    }
    
    /** constructor for side on mapHex */
    public NetworkVertex(MapHex hex, int side) {
        this.type = VertexType.SIDE;
        this.hex = hex;
        this.station = null;
        this.city = null;
        this.side = (side % 6);
        
        this.virtual = false;
        this.virtualId = null;
    }
    
    /**  constructor for public company hq */
    public NetworkVertex(PublicCompanyI company) {
        this(VertexType.HQ, "HQ");
    }
    
    private NetworkVertex(VertexType type, String name) {
        this.type = type;
        this.hex = null;
        this.station = null;
        this.city = null;
        this.side = -1;

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
        if (virtual)
            return virtualId;
        else if (isStation())
            return hex.getName() + "." + -station.getNumber();
        else if (isSide())
            return hex.getName() + "." + side; 
        else
            return null;
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
    
    public String getCityName() {
        return cityName;
    }
    
    public NetworkVertex setCityName(String locationName) {
        this.cityName = locationName;
        return this;
    }
    
    // getter for rails objects
    public MapHex getHex(){
        return hex;
    }
    
    public Station getStation(){
        return station;
    }
    
    public City getCity() {
        return city;
    }
    
    public int getSide(){
        return side;
    }

    public boolean isOfType(VertexType vertexType, StationType stationType) {
        return (type == vertexType && (!isStation() || getStationType() == stationType));
    }
       
    /** 
     * Initialize for rails vertexes
     */
    public void initRailsVertex(PublicCompanyI company) {
        // side vertices use the defaults, virtuals cannot use this function
        if (virtual || type == VertexType.SIDE) return;
        
        log.info("Init of vertex " + this);
        
        // check if it is a major or minor
        if (station.getType().equals(Station.CITY) || station.getType().equals(Station.OFF_MAP_AREA)) {
            setStationType(StationType.MAJOR);
        } else if (station.getType().equals(Station.TOWN) || station.getType().equals(Station.PORT)
                || station.getType().equals(Station.HALT)) {
            setStationType(StationType.MINOR);
        }
        
        // check if it is a sink 
        if (company == null) { // if company == null, then all sinks are deactivated
            sink = false;
        } else if (station.getType().equals(Station.OFF_MAP_AREA) || 
                station.getType().equals(Station.CITY) && !city.hasTokenSlotsLeft() && city.getSlots() != 0 && !city.hasTokenOf(company)) { 
            sink = true;
        }
        
        // define locationName
        cityName = null;
        if (station.getType().equals(Station.OFF_MAP_AREA)) {
            if (hex.getCityName() != null && !hex.getCityName().equals("")) {
                cityName = hex.getCityName();
            }
        } else {
            if (hex.getCityName() != null && !hex.getCityName().equals("")
                    && station.getCityName() != null && !station.getCityName().equals("")) {
                cityName = hex.getCityName() + "." + station.getCityName();
            }
        }
    }
    
    public void setRailsVertexValue(PhaseI phase) {
        // side vertices and  virtuals cannot use this function
        if (virtual || type == VertexType.SIDE) return;

        // define value
//      if (station.getType().equals(Station.OFF_MAP_AREA) || station.getValue() == -1) {
        if (hex.hasOffBoardValues()) {
            value = hex.getCurrentOffBoardValue(phase);
        } else {
            value = station.getValue();
        }
    }
    
    
    @Override
    public String toString(){
        StringBuffer message = new StringBuffer();
        if (isVirtual())
            message.append(virtualId);
        else if (isStation()) 
            message.append(hex.getName() + "." + station.getNumber());
        else if (isSide())
            message.append(hex.getName() + "." + hex.getOrientationName(side));
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

    public static void initAllRailsVertices(Collection<NetworkVertex> vertices, 
            PublicCompanyI company,  PhaseI phase) {
        for (NetworkVertex v:vertices) {
            if (company != null)
                v.initRailsVertex(company);
            if (phase != null)
                v.setRailsVertexValue(phase);
        }
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
        newVertex.cityName = vertex.cityName;
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
        
        GUIHex guiHex = map.getHexByName(vertex.getHex().getName());
        if (vertex.isMajor()) {
            return guiHex.getCityPoint2D(vertex.getCity());
        } else if (vertex.isMinor()) {
            return guiHex.getCityPoint2D(vertex.getCity());
//            return guiHex.getCenterPoint2D();
        } else if (vertex.isSide()) {
            if (map instanceof EWHexMap) 
                return guiHex.getSidePoint2D(5-vertex.getSide());
            else
                return guiHex.getSidePoint2D((3+vertex.getSide())%6);
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
        log.info("Vertex Map Coverage minX=" + minX + ", minY=" + minY + ", maxX=" + maxX + ", maxY=" + maxY );
//        Rectangle rectangle = new Rectangle((int)minX, (int)minY, (int)maxX, (int)maxY);
        log.info("Created rectangle=" + rectangle);
        return (rectangle);
    }
}
