package net.sf.rails.algorithms;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import net.sf.rails.game.BaseToken;
import net.sf.rails.game.HexSide;
import net.sf.rails.game.HexSidesSet;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Station;
import net.sf.rails.game.Stop;
import net.sf.rails.game.Tile;
import net.sf.rails.game.Track;
import net.sf.rails.game.TrackPoint;
import net.sf.rails.game.state.Owner;

import org.jgraph.JGraph;
import org.jgrapht.Graphs;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.Subgraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;
import com.jgraph.layout.organic.JGraphFastOrganicLayout;

/**
 * NetworkGraph mirrors the structure of a 18xx track
 *
 * TODO: Rewrite this by separating the creation code from the data code
 */
public class NetworkGraph {

    protected static Logger log =
            LoggerFactory.getLogger(NetworkGraph.class);

    private final SimpleGraph<NetworkVertex, NetworkEdge> graph;

    private final Map<String, NetworkVertex> vertices;

    private NetworkIterator iterator;

    private NetworkGraph() {
        graph = new SimpleGraph<NetworkVertex, NetworkEdge>(NetworkEdge.class);
        vertices = Maps.newHashMap();
    }
    
    private NetworkGraph(NetworkGraph inGraph) {
        graph = new SimpleGraph<NetworkVertex, NetworkEdge>(NetworkEdge.class);
        Graphs.addGraph(graph, inGraph.graph);
        vertices = Maps.newHashMap(inGraph.vertices);
    }

    public static NetworkGraph createMapGraph(RailsRoot root) {
        NetworkGraph graph = new NetworkGraph();
        graph.generateMapGraph(root);
        return graph;
    }
    
    public static NetworkGraph createRouteGraph(NetworkGraph mapGraph, PublicCompany company, boolean addHQ) {
        NetworkGraph newGraph = new NetworkGraph() ; 
        newGraph.initRouteGraph(mapGraph, company, addHQ);
        newGraph.rebuildVertices();
        return newGraph;
    }

    public static NetworkGraph createOptimizedGraph(NetworkGraph inGraph,
            Collection<NetworkVertex> protectedVertices) {
        NetworkGraph newGraph = new NetworkGraph(inGraph);
        newGraph.optimizeGraph(protectedVertices);
        newGraph.rebuildVertices();
        return newGraph;
    }
    
    public NetworkGraph cloneGraph() {
        return new NetworkGraph(this);
    }
    
    public SimpleGraph<NetworkVertex, NetworkEdge> getGraph() {
        return graph;
    }
    
    public void setIteratorStart(MapHex hex, Station station) {
        iterator = new NetworkIterator(graph, getVertex(hex, station));
    }
    
    public Iterator<NetworkVertex> iterator() {
        return iterator; 
    }
    
    public NetworkVertex getVertexByIdentifier(String identVertex) {
        return vertices.get(identVertex);
    }
    
    public NetworkVertex getVertex(BaseToken token) {
        Owner owner = token.getOwner();
        // TODO: Check if this still works
        if (!(owner instanceof Stop)) return null;
        Stop city = (Stop)owner;
        MapHex hex = city.getParent();
        Station station = city.getRelatedStation();
        return getVertex(hex, station);
    }
    
    public NetworkVertex getVertex(MapHex hex, TrackPoint point) {
        return vertices.get(hex.getId() + "." + point.getTrackPointNumber());
    }
    
    public NetworkVertex getVertex(MapHex hex, int trackPointNr) {
        return vertices.get(hex.getId() + "." + trackPointNr);
    }
    
    public NetworkVertex getVertexRotated(MapHex hex, TrackPoint point) {
        if (point.getTrackPointType() == TrackPoint.Type.SIDE)
            point = point.rotate(hex.getCurrentTileRotation());
        return vertices.get(hex.getId() + "." + point.getTrackPointNumber());
    }
    
    public ImmutableMap<MapHex, HexSidesSet> getReachableSides() {
        // first create builders for all HexSides
        Map<MapHex, HexSidesSet.Builder> hexSides = Maps.newHashMap();
        for(NetworkVertex vertex:graph.vertexSet()) {
            if (vertex.isSide() && iterator.getSeenData().get(vertex) 
                    != NetworkIterator.greedyState.greedy) {
                MapHex hex = vertex.getHex();
                if (!hexSides.containsKey(hex)) {
                    hexSides.put(hex, HexSidesSet.builder());
                }
                hexSides.get(hex).set(vertex.getSide());
            }
        }
        // second build the map of mapHex to HexSides
        ImmutableMap.Builder<MapHex, HexSidesSet> hexBuilder = ImmutableMap.builder();
        for (MapHex hex:hexSides.keySet()) {
            hexBuilder.put(hex, hexSides.get(hex).build());
        }
        return hexBuilder.build();
    }

    /**
     * @return a map of all hexes and stations that can be run through
     */
    public Multimap<MapHex, Station> getPassableStations() {
        
        ImmutableMultimap.Builder<MapHex, Station> hexStations = 
                ImmutableMultimap.builder();

        for(NetworkVertex vertex:graph.vertexSet()) {
            if (vertex.isStation() && !vertex.isSink()) {
                hexStations.put(vertex.getHex(), vertex.getStation());
            }
        }

        return hexStations.build();
    }

    /**
     * @return a list of all stops that are tokenable for the argument company
     */
    public Multimap<MapHex,Stop> getTokenableStops(PublicCompany company){
        
        ImmutableMultimap.Builder<MapHex, Stop> hexStops = 
                ImmutableMultimap.builder();

        for(NetworkVertex vertex:graph.vertexSet()) {
            Stop stop = vertex.getStop();
            if (stop != null && stop.isTokenableFor(company)) {
                hexStops.put(vertex.getHex(),stop);
            }
        }
        return hexStops.build();
    }

    private void rebuildVertices() {
        // rebuild mapVertices
        vertices.clear();
        for (NetworkVertex v:graph.vertexSet()) {
            vertices.put(v.getIdentifier(), v);
        }
    }

    private void generateMapGraph(RailsRoot root) {
        MapManager mapManager = root.getMapManager();
        RevenueManager revenueManager = root.getRevenueManager();
        for (MapHex hex:mapManager.getHexes()) {
            // get Tile
            Tile tile = hex.getCurrentTile();
            
            // then get stations
            Collection<Station> stations = tile.getStations(); 
            // and add those to the mapGraph
            for (Station station: stations) {
                NetworkVertex stationVertex = new NetworkVertex(hex, station);
                graph.addVertex(stationVertex);
                vertices.put(stationVertex.getIdentifier(), stationVertex);
                log.info("Added " + stationVertex);
            }
            
            // get tracks per side to add that vertex
            for (HexSide side:HexSide.all()) 
                if (tile.hasTracks(side)) {
                    HexSide rotated = side.rotate(hex.getCurrentTileRotation());
                    NetworkVertex sideVertex = new NetworkVertex(hex, rotated); 
                    graph.addVertex(sideVertex);
                    vertices.put(sideVertex.getIdentifier(), sideVertex);
                    log.info("Added " + sideVertex);
                }
        }
        
        // loop over all hex and add tracks
        for (MapHex hex:mapManager.getHexes()) {
            // get Tile
            Tile tile = hex.getCurrentTile();
            // get Tracks
            Set<Track> tracks = tile.getTracks();

            for (Track track:tracks) {
                NetworkVertex startVertex = getVertexRotated(hex, track.getStart());
                NetworkVertex endVertex = getVertexRotated(hex, track.getEnd());
                log.info("Track: " + track);
                NetworkEdge edge =  new NetworkEdge(startVertex, endVertex, false);
                if (startVertex == endVertex) {
                    log.error("Track " + track + " on hex " + hex + "has identical start/end");
                } else {
                    graph.addEdge(startVertex, endVertex, edge);
                    log.info("Added non-greedy edge " + edge.getConnection());
                }
            }

            // TODO: Rewrite this by employing the features of Trackpoint
            // and connect to neighbouring hexes (for sides 0-2)
            for (HexSide side:HexSide.head()) {
                MapHex neighborHex = mapManager.getNeighbour(hex, side);
                if (neighborHex == null) {
                    log.info("No connection for Hex " + hex.getId() + " at "
                            + hex.getOrientationName(side) + ", No Neighbor");
                    continue;
                }
                NetworkVertex vertex = getVertex(hex, side);
                HexSide rotated = side.opposite();
                NetworkVertex otherVertex = getVertex(neighborHex, rotated);
                if (vertex == null && otherVertex == null){
                    log.info("Hex " + hex.getId() + " has no track at "
                            + hex.getOrientationName(side));
                    log.info("And Hex " + neighborHex.getId() + " has no track at "
                            + neighborHex.getOrientationName(rotated));
                    continue;
                }
                else if (vertex == null && otherVertex != null) { 
                    log.info("Deadend connection for Hex " + neighborHex.getId() + " at "
                            + neighborHex.getOrientationName(rotated) + ", NeighborHex "
                            + hex.getId() + " has no track at side " +
                            hex.getOrientationName(side));
                    vertex = new NetworkVertex(hex, side);
                    graph.addVertex(vertex);
                    vertices.put(vertex.getIdentifier(), vertex);
                    log.info("Added deadend vertex " + vertex);
                }
                else if (otherVertex == null)  {
                    log.info("Deadend connection for Hex " + hex.getId() + " at "
                            + hex.getOrientationName(side) + ", NeighborHex "
                            + neighborHex.getId() + " has no track at side " +
                            neighborHex.getOrientationName(rotated));
                    otherVertex = new NetworkVertex(neighborHex, rotated);
                    graph.addVertex(otherVertex);
                    vertices.put(otherVertex.getIdentifier(), otherVertex);
                    log.info("Added deadend vertex " + otherVertex);
                }
                NetworkEdge edge =  new NetworkEdge(vertex, otherVertex, true);
                graph.addEdge(vertex, otherVertex, 
                        edge);
                log.info("Added greedy edge " + edge.getConnection());
            }
        }
        
        // add graph modifiers
        if (revenueManager != null) {
            revenueManager.initGraphModifiers(this);
        }

    }

    public void optimizeGraph() {
        optimizeGraph(new ArrayList<NetworkVertex>(0));
    }
    
    private void optimizeGraph(Collection<NetworkVertex> protectedVertices) {
       
        // remove vertices until convergence
        boolean notDone = true;
        while (notDone) {
            increaseGreedness();
            notDone = removeVertexes(protectedVertices); 
            // removedVertices can change Greedness, but not vice-versa
        }
    }

    // Increase Greedness implies that an edge that 
    // connects stations and/or sides with only one track in/out
    // can be set to greedy (as one has to follow the exit anyway)
    private void increaseGreedness() {
        for (NetworkEdge edge:graph.edgeSet()) {
            if (edge.isGreedy()) continue;
            NetworkVertex source = edge.getSource();
            NetworkVertex target = edge.getTarget();
            if ((source.isSide() && graph.edgesOf(source).size() == 2 || source.isStation()) &&
                    (target.isSide() && graph.edgesOf(target).size() == 2 || target.isStation())) {
                edge.setGreedy(true);
                log.info("Increased greedness for " + edge.getConnection());
            }
        }
    }
    
    /** remove deadend and vertex with only two edges */ 
    private boolean removeVertexes(Collection<NetworkVertex> protectedVertices){

        boolean removed = false;

        for (NetworkVertex vertex:ImmutableSet.copyOf(graph.vertexSet())) {
            Set<NetworkEdge> vertexEdges = graph.edgesOf(vertex);

            // always keep protected vertices
            if (protectedVertices.contains(vertex)) {
                continue;
            }

            // remove hermit
            if (vertexEdges.size() == 0) {
                log.info("Remove hermit (no connection) = "  + vertex);
                graph.removeVertex(vertex);
                removed = true;
            }

            // the following only for side vertexes
            if (!vertex.isSide()) continue;

            if (vertexEdges.size() == 1) { 
                log.info("Remove deadend side (single connection) = "  + vertex);
                graph.removeVertex(vertex);
                removed = true;
            } else if (vertexEdges.size() == 2) { // not necessary vertices 
                NetworkEdge[] edges = vertexEdges.toArray(new NetworkEdge[2]);
                if (edges[0].isGreedy() == edges[1].isGreedy()) {
                    if (!edges[0].isGreedy()) {
                        log.info("Remove deadend side (no greedy connection) = "  + vertex);
                        // two non greedy edges indicate a deadend
                        graph.removeVertex(vertex);
                        removed = true;
                    } else {
                        // greedy case:
                        // merge greedy edges if the vertexes are not already connected
                        if (NetworkEdge.mergeEdgesInGraph(graph, edges[0], edges[1])) {
                            removed = true;
                        }
                    }
                }
            }     
        }
        return removed;
    }

    private void initRouteGraph(NetworkGraph mapGraph, PublicCompany company, boolean addHQ) {
        
        // set sinks on mapgraph
        NetworkVertex.initAllRailsVertices(mapGraph, company, null);
        
        // add Company HQ
        NetworkVertex hqVertex = new NetworkVertex(company); 
        graph.addVertex(hqVertex);
        
        // create vertex set for subgraph
        List<NetworkVertex> tokenVertexes = mapGraph.getCompanyBaseTokenVertexes(company);
        Set<NetworkVertex> vertexes = new HashSet<NetworkVertex>();
        
        for (NetworkVertex vertex:tokenVertexes){
            // allow to leave tokenVertices even if those are sinks
            // Examples are tokens in offBoard hexes
            boolean storeSink = vertex.isSink(); vertex.setSink(false);
            vertexes.add(vertex);
            // add connection to graph
            graph.addVertex(vertex);
            graph.addEdge(vertex, hqVertex, new NetworkEdge(vertex, hqVertex, false));
            iterator = new NetworkIterator(mapGraph.getGraph(), vertex, company);
            for (;iterator.hasNext();)
                vertexes.add(iterator.next());
            // restore sink property
            vertex.setSink(storeSink);
        }

        Subgraph<NetworkVertex, NetworkEdge, SimpleGraph<NetworkVertex, NetworkEdge>> subGraph = 
            new Subgraph<NetworkVertex, NetworkEdge, SimpleGraph<NetworkVertex, NetworkEdge>>
            (mapGraph.getGraph(), vertexes);
        // now add all vertexes and edges to the graph
        Graphs.addGraph(graph, subGraph);

        // if addHQ is not set remove HQ vertex
        if (!addHQ) graph.removeVertex(hqVertex);
        
    }
    
    public List<NetworkVertex> getCompanyBaseTokenVertexes(PublicCompany company) {
        List<NetworkVertex> vertexes = new ArrayList<NetworkVertex>();
        for (BaseToken token:company.getLaidBaseTokens()){
            NetworkVertex vertex = getVertex(token);
            if (vertex == null) continue;
            vertexes.add(vertex);
        }
        return vertexes;
    }

    public void visualize(String title) {
        // show network mapGraph
        JGraphModelAdapter<NetworkVertex, NetworkEdge> jGAdapter =
            new JGraphModelAdapter<NetworkVertex, NetworkEdge>(graph);
        
        JGraph jgraph = new JGraph(jGAdapter);
        
        List<NetworkVertex> vertexes= new ArrayList<NetworkVertex>(graph.vertexSet());
         
        Object[] rootCell = new Object[1];
        rootCell[0] =  jGAdapter.getVertexCell(vertexes.get(0));
        
        JGraphFacade facade = new JGraphFacade(jgraph, rootCell);
        JGraphLayout layout = new JGraphFastOrganicLayout();
        layout.run(facade);
        
        // calculate size of network graph
        double ratio = Math.sqrt(graph.vertexSet().size() / 50.0);
        int width = (int) Math.floor(2400 * ratio);
        int height = (int) Math.floor(1800 * ratio);
        log.info("ratio=" + ratio + "width= " + width + "height" + height);
        facade.scale(new Rectangle(width, height));
        @SuppressWarnings("rawtypes")
        Map nested = facade.createNestedMap(true,true);
        jgraph.getGraphLayoutCache().edit(nested);

        jgraph.setScale(0.75);
        
        JFrame frame = new JFrame();
        frame.setTitle(title + "(V=" + graph.vertexSet().size() + 
                ",E=" + graph.edgeSet().size() + ")");
        frame.setSize(new Dimension(800,600));
        frame.getContentPane().add(new JScrollPane(jgraph));
        frame.pack();
        frame.setVisible(true);
    }
    
}
