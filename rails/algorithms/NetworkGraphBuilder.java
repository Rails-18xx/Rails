package rails.algorithms;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JScrollPane;


import org.apache.log4j.Logger;
import org.jgraph.JGraph;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.SimpleGraph;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;
import com.jgraph.layout.organic.JGraphFastOrganicLayout;

import rails.game.BaseToken;
import rails.game.City;
import rails.game.GameManagerI;
import rails.game.MapHex;
import rails.game.MapManager;
import rails.game.PublicCompanyI;
import rails.game.Station;
import rails.game.TileI;
import rails.game.TokenHolder;
import rails.game.TokenI;
import rails.game.Track;

public final class NetworkGraphBuilder implements Iterable<NetworkVertex> {

    protected static Logger log =
        Logger.getLogger(NetworkGraphBuilder.class.getPackage().getName());
    
    private final SimpleGraph<NetworkVertex, NetworkEdge> mapGraph;
    
    private final Map<String, NetworkVertex> mapVertexes;
    
    private NetworkIterator iterator;
    
    private NetworkGraphBuilder() {
        mapGraph = new SimpleGraph<NetworkVertex, NetworkEdge>(NetworkEdge.class);
        mapVertexes = new HashMap<String, NetworkVertex> ();
    }
    
    public static NetworkGraphBuilder create(GameManagerI gameManager) {
        NetworkGraphBuilder graphBuilder = new NetworkGraphBuilder();
        graphBuilder.generateGraph(gameManager.getMapManager(), gameManager.getRevenueManager());
        return graphBuilder;
    }
    

    public void generateGraph(MapManager mapManager, RevenueManager revenueManager) {
        
        for (MapHex hex:mapManager.getHexesAsList()) {
            // get Tile
            TileI tile = hex.getCurrentTile();
            
            // then get stations
            List<Station> stations = tile.getStations(); 
            // and add those to the mapGraph
            for (Station station: stations) {
                NetworkVertex stationVertex = new NetworkVertex(hex, station);
                mapGraph.addVertex(stationVertex);
                mapVertexes.put(stationVertex.getIdentifier(), stationVertex);
                log.info("Added " + stationVertex);
            }
            
            // get tracks per side to add that vertex
            for (int side=0; side<6; side++) 
                if (tile.getTracksPerSide(side).size() != 0) {
                    NetworkVertex sideVertex = new NetworkVertex(hex, side + hex.getCurrentTileRotation());
                    mapGraph.addVertex(sideVertex);
                    mapVertexes.put(sideVertex.getIdentifier(), sideVertex);
                    log.info("Added " + sideVertex);
                }
        }
        
        // loop over all maps and add tracks
        for (MapHex hex:mapManager.getHexesAsList()) {
            // get Tile
            TileI tile = hex.getCurrentTile();
            // get Tracks
            List<Track> tracks = tile.getTracks();

            for (Track track:tracks) {
                int[] points = track.points();
                NetworkVertex startVertex = getVertexRotated(hex, points[0]);
                NetworkVertex endVertex = getVertexRotated(hex, points[1]);
                log.info("Track: " + track);
                NetworkEdge edge =  new NetworkEdge(startVertex, endVertex, false);
                if (startVertex == endVertex) {
                    log.error("Track " + track + " on hex " + hex + "has identical start/end");
                } else {
                    mapGraph.addEdge(startVertex, endVertex, edge);
                    log.info("Added non-greedy edge " + edge.getConnection());
                }
            }

            // and connect to neighbouring hexes (for sides 0-2)
            for (int side=0; side <= 2; side++) {
                NetworkVertex vertex = getVertex(hex, side);
                MapHex neighborHex = hex.getNeighbor(side);
                if (neighborHex == null) {
                    log.info("No connection for Hex " + hex.getName() + " at "
                            + hex.getOrientationName(side) + ", No Neighbor");
                    continue;
                }
                NetworkVertex otherVertex = getVertex(neighborHex, side + 3);
                if (vertex == null && otherVertex == null){
                    log.info("Hex " + hex.getName() + " has no track at "
                            + hex.getOrientationName(side));
                    log.info("And Hex " + neighborHex.getName() + " has no track at "
                            + neighborHex.getOrientationName(side + 3));
                    continue;
                }
                else if (vertex == null && otherVertex != null) { 
                    log.info("Deadend connection for Hex " + neighborHex.getName() + " at "
                            + neighborHex.getOrientationName(side + 3) + ", NeighborHex "
                            + hex.getName() + " has no track at side " +
                            hex.getOrientationName(side));
                    vertex = new NetworkVertex(hex, side);
                    mapGraph.addVertex(vertex);
                    mapVertexes.put(vertex.getIdentifier(), vertex);
                    log.info("Added deadend vertex " + vertex);
                }
                else if (otherVertex == null)  {
                    log.info("Deadend connection for Hex " + hex.getName() + " at "
                            + hex.getOrientationName(side) + ", NeighborHex "
                            + neighborHex.getName() + " has no track at side " +
                            neighborHex.getOrientationName(side+3));
                    otherVertex = new NetworkVertex(neighborHex, side + 3);
                    mapGraph.addVertex(otherVertex);
                    mapVertexes.put(otherVertex.getIdentifier(), otherVertex);
                    log.info("Added deadend vertex " + otherVertex);
                }
                NetworkEdge edge =  new NetworkEdge(vertex, otherVertex, true);
                mapGraph.addEdge(vertex, otherVertex, 
                        edge);
                log.info("Added greedy edge " + edge.getConnection());
            }
        }
        
        // add graph modifiers
        if (revenueManager != null) {
            revenueManager.callGraphModifiers(this);
        }
    }        

    
    public SimpleGraph<NetworkVertex, NetworkEdge> getMapGraph() {
        return mapGraph;
    }

    
    public void setIteratorStart(MapHex hex, Station station) {
        iterator = new NetworkIterator(mapGraph, getVertex(hex, station));
    }
    
    public Iterator<NetworkVertex> iterator() {
        return iterator; 
    }
    
    public NetworkVertex getVertexByIdentifier(String identVertex) {
        return mapVertexes.get(identVertex);
    }
    
    public NetworkVertex getVertex(TokenI token) {
        if (!(token instanceof BaseToken)) return null;
        TokenHolder holder = token.getHolder();
        if (!(holder instanceof City)) return null;
        City city = (City)holder;
        MapHex hex = city.getHolder();
        Station station = city.getRelatedStation();
        return getVertex(hex, station);
    }
    
    private NetworkVertex getVertex(MapHex hex, Station station) {
        return mapVertexes.get(hex.getName() + "." + -station.getNumber());
    }
    
    public NetworkVertex getVertex(MapHex hex, int side) {
        if (side >= 0)
            side = side % 6;
        return mapVertexes.get(hex.getName() + "." + side);
    }
    
    private NetworkVertex getVertexRotated(MapHex hex, int side) {
        if (side >= 0)
            side = (side + hex.getCurrentTileRotation()) % 6;
        return mapVertexes.get(hex.getName() + "." + side);
    }

    public static List<MapHex> getMapHexes(Graph<NetworkVertex, NetworkEdge> graph){
        List<MapHex> hexes = new ArrayList<MapHex>();
        for(NetworkVertex vertex:graph.vertexSet()) {
            if (vertex.isStation() || vertex.isSide()) {
                hexes.add(vertex.getHex());
            }
        }
        return hexes;
    }
    
    public static List<MapHex> getTokenableStationHexes(Graph<NetworkVertex, NetworkEdge> graph,
                                PublicCompanyI company){
        List<MapHex> hexes = new ArrayList<MapHex>();
        for(NetworkVertex vertex:graph.vertexSet()) {
            City city = vertex.getCity();
            if (city != null && city.hasTokenSlotsLeft() && !city.hasTokenOf(company)) {
                hexes.add(vertex.getHex());
            }
        }
        return hexes;
    }
    
    
    
    public static SimpleGraph<NetworkVertex, NetworkEdge> optimizeGraph(
            SimpleGraph<NetworkVertex, NetworkEdge> inGraph) {
        return optimizeGraph(inGraph, new ArrayList<NetworkVertex>());
    }
    
    public static SimpleGraph<NetworkVertex, NetworkEdge> optimizeGraph(
            SimpleGraph<NetworkVertex, NetworkEdge> inGraph, Collection<NetworkVertex> protectedVertices) {
        
        // clone graph
       SimpleGraph<NetworkVertex, NetworkEdge> graph = new SimpleGraph<NetworkVertex, NetworkEdge>(NetworkEdge.class);
       Graphs.addGraph(graph, inGraph);
 
       // increase greedness
        for (NetworkEdge edge:graph.edgeSet()) {
            NetworkVertex source = edge.getSource();
            NetworkVertex target = edge.getTarget();
            if ((source.isSide() && graph.edgesOf(source).size() == 2 || source.isStation()) &&
                    target.isSide() && graph.edgesOf(target).size() == 2 || target.isStation()) {
                edge.setGreedy(true);
                log.info("Increased greedness for " + edge.getConnection());
            }
        }
      
        while (removeVertexes(graph, protectedVertices));
        
        return graph;
    }
    
    /** remove deadend and vertex with only two edges */ 
    private static boolean removeVertexes(SimpleGraph<NetworkVertex, NetworkEdge> graph,
            Collection<NetworkVertex> protectedVertices){
        
        boolean removed = false;
        for (NetworkVertex vertex:graph.vertexSet()) {
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
                break;
            }
            
            // the following only for side vertexes
            if (!vertex.isSide()) continue;

            if (vertexEdges.size() == 1) { 
                log.info("Remove deadend side (single connection) = "  + vertex);
                graph.removeVertex(vertex);
                removed = true;
                break;
            } // vertex is not necessary and not on the protected list
                else if (vertexEdges.size() == 2) { 
                NetworkEdge[] edges = vertexEdges.toArray(new NetworkEdge[2]);
                if (edges[0].isGreedy() == edges[1].isGreedy()) {
                    if (!edges[0].isGreedy()) {
                        log.info("Remove deadend side (no greedy connection) = "  + vertex);
                        // two non greedy edges indicate a deadend
                        graph.removeVertex(vertex);
                        removed = true;
                        break;
                    } 
                    // greedy case:
                    // merge greed edges if the vertexes are not already connected
                    if (edges[0].isGreedy()) {
                        removed = NetworkEdge.mergeEdgesInGraph(graph, edges[0], edges[1]);
                        if (removed) break;
                    }
                }
            }
        }     
        return removed;
    }
    
    public static void visualize(Graph<NetworkVertex, NetworkEdge> graph, String title) {
        // show network graph
        JGraphModelAdapter<NetworkVertex, NetworkEdge> jGAdapter =
            new JGraphModelAdapter<NetworkVertex, NetworkEdge>(graph);
        
        JGraph jgraph = new JGraph(jGAdapter);
        
        List<NetworkVertex> vertexes= new ArrayList<NetworkVertex>(graph.vertexSet());
         
        Object[] rootCell = new Object[1];
        rootCell[0] =  jGAdapter.getVertexCell(vertexes.get(0));
        
        JGraphFacade facade = new JGraphFacade(jgraph, rootCell);
        JGraphLayout layout = new JGraphFastOrganicLayout();
        layout.run(facade);
        
        facade.scale(new Rectangle(1600,1200));
        Map nested = facade.createNestedMap(true,true);
        jgraph.getGraphLayoutCache().edit(nested);

        jgraph.setScale(0.8);
        
        JFrame frame = new JFrame();
        frame.setTitle(title + "(V=" + graph.vertexSet().size() + 
        		",E=" + graph.edgeSet().size() + ")");
        frame.setSize(new Dimension(800,600));
        frame.getContentPane().add(new JScrollPane(jgraph));
        frame.pack();
        frame.setVisible(true);
    }
}
