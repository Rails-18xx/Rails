package rails.algorithms;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.jgrapht.graph.Subgraph;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;
import com.jgraph.layout.organic.JGraphFastOrganicLayout;

import rails.game.BaseToken;
import rails.game.City;
import rails.game.MapHex;
import rails.game.PublicCompanyI;
import rails.game.Station;
import rails.game.TileI;
import rails.game.TokenHolder;
import rails.game.TokenI;
import rails.game.Track;

public final class NetworkGraphBuilder implements Iterable<NetworkVertex> {

    protected static Logger log =
        Logger.getLogger(NetworkGraphBuilder.class.getPackage().getName());
    
    private SimpleGraph<NetworkVertex, NetworkEdge> mapGraph;  
    private Map<String, NetworkVertex> mapVertexes;
    private NetworkIterator iterator;
    
    public NetworkGraphBuilder() {
        this.mapGraph = null;
    }

    public void generateGraph(List<MapHex> mHexes) {
        
        mapGraph = new SimpleGraph<NetworkVertex, NetworkEdge>(NetworkEdge.class);
        mapVertexes = new HashMap<String, NetworkVertex> ();
        
        for (MapHex hex:mHexes) {
            // get Tile
            TileI tile = hex.getCurrentTile();
            
            // then get stations
            List<Station> stations = tile.getStations(); 
            // and add those to the mapGraph
            for (Station station: stations) {
                NetworkVertex stationVertex = new NetworkVertex(hex, station);
                mapGraph.addVertex(stationVertex);
                mapVertexes.put(stationVertex.getIdentifier(), stationVertex);
                log.debug("Added " + stationVertex + " / " + stationVertex.printTokens());
            }
            
            // get tracks per side to add that vertex
            for (int side=0; side<6; side++) 
                if (tile.getTracksPerSide(side).size() != 0) {
                    NetworkVertex sideVertex = new NetworkVertex(hex, side + hex.getCurrentTileRotation());
                    mapGraph.addVertex(sideVertex);
                    mapVertexes.put(sideVertex.getIdentifier(), sideVertex);
                    log.debug("Added " + sideVertex);
                }
        }
        
        // loop over all maps and add tracks
        for (MapHex hex:mHexes) {
            // get Tile
            TileI tile = hex.getCurrentTile();
            // get Tracks
            List<Track> tracks = tile.getTracks();

            for (Track track:tracks) {
                int[] points = track.points();
                NetworkVertex startVertex = getVertexRotated(hex, points[0]);
                NetworkVertex endVertex = getVertexRotated(hex, points[1]);
                log.debug("Track: " + track);
                NetworkEdge edge =  new NetworkEdge(startVertex, endVertex, false);
                mapGraph.addEdge(startVertex, endVertex, edge);
                log.debug("Added edge " + edge.getConnection());
            }

            // and connect to neighbouring hexes (for sides 0-2)
            for (int side=0; side <= 2; side++) {
                NetworkVertex vertex = getVertex(hex, side);
                MapHex neighborHex = hex.getNeighbor(side);
                if (neighborHex == null) {
                    log.debug("No connection for Hex " + hex.getName() + " at "
                            + hex.getOrientationName(side) + ", No Neighbor");
                    continue;
                }
                NetworkVertex otherVertex = getVertex(neighborHex, side + 3);
                if (vertex == null && otherVertex == null){
                    log.debug("Hex " + hex.getName() + " has no track at "
                            + hex.getOrientationName(side));
                    log.debug("And Hex " + neighborHex.getName() + " has no track at "
                            + neighborHex.getOrientationName(side + 3));
                    continue;
                }
                else if (vertex == null && otherVertex != null) { 
                    log.debug("Deadend connection for Hex " + neighborHex.getName() + " at "
                            + neighborHex.getOrientationName(side + 3) + ", NeighborHex "
                            + hex.getName() + " has no track at side " +
                            hex.getOrientationName(side));
                    vertex = new NetworkVertex(hex, side);
                    mapGraph.addVertex(vertex);
                    mapVertexes.put(vertex.getIdentifier(), vertex);
                    log.debug("Added deadend vertex " + vertex);
                }
                else if (otherVertex == null)  {
                    log.debug("Deadend connection for Hex " + hex.getName() + " at "
                            + hex.getOrientationName(side) + ", NeighborHex "
                            + neighborHex.getName() + " has no track at side " +
                            neighborHex.getOrientationName(side+3));
                    otherVertex = new NetworkVertex(neighborHex, side + 3);
                    mapGraph.addVertex(otherVertex);
                    mapVertexes.put(otherVertex.getIdentifier(), otherVertex);
                    log.debug("Added deadend vertex " + otherVertex);
                }
                NetworkEdge edge =  new NetworkEdge(vertex, otherVertex, true);
                mapGraph.addEdge(vertex, otherVertex, 
                        edge);
                log.debug("Added edge " + edge.getConnection());
            }
        }
    }        

    
    public SimpleGraph<NetworkVertex, NetworkEdge> getMapGraph() {
        return mapGraph;
    }

    public SimpleGraph<NetworkVertex, NetworkEdge> getRailRoadGraph(PublicCompanyI company) {
        
        // initialized simple graph
        SimpleGraph<NetworkVertex, NetworkEdge> graph = new SimpleGraph<NetworkVertex, NetworkEdge>(NetworkEdge.class);
        // add Company HQ
        NetworkVertex hqVertex = new NetworkVertex(company); 
        graph.addVertex(hqVertex);
        
        // create vertex set for subgraph
        List<TokenI> tokens = company.getTokens();
        Set<NetworkVertex> vertexes = new HashSet<NetworkVertex>();
        
        for (TokenI token:tokens){
            if (!(token instanceof BaseToken)) continue;
            TokenHolder holder = token.getHolder();
            if (!(holder instanceof City)) continue;
            City city = (City)holder;
            MapHex hex = city.getHolder();
            Station station = city.getRelatedStation();
            NetworkVertex vertex = getVertex(hex, station);
            vertexes.add(vertex);
            // add connection to graph
            graph.addVertex(vertex);
            graph.addEdge(vertex, hqVertex, new NetworkEdge(vertex, hqVertex, false));
            NetworkIterator iterator = new NetworkIterator(mapGraph, vertex, company);
            for (;iterator.hasNext();)
                vertexes.add(iterator.next());
        }

        Subgraph<NetworkVertex, NetworkEdge, SimpleGraph<NetworkVertex, NetworkEdge>> subGraph = 
            new Subgraph<NetworkVertex, NetworkEdge, SimpleGraph<NetworkVertex, NetworkEdge>>
            (mapGraph, vertexes);
        // now add all vertexes and edges to the graph
        Graphs.addGraph(graph, subGraph);
        
        return graph;
    }
    
    public void setIteratorStart(MapHex hex, Station station) {
        iterator = new NetworkIterator(mapGraph, getVertex(hex, station));
    }
    
    @Override
    public Iterator<NetworkVertex> iterator() {
        return iterator; 
    }
    
    private NetworkVertex getVertex(MapHex hex, Station station) {
        return mapVertexes.get(hex.getName() + "." + -station.getNumber());
    }
    
    private NetworkVertex getVertex(MapHex hex, int side) {
        if (side >= 0)
            side = side % 6;
        return mapVertexes.get(hex.getName() + "." + side);
    }
    
    private NetworkVertex getVertexRotated(MapHex hex, int side) {
        if (side >= 0)
            side = (side + hex.getCurrentTileRotation()) % 6;
        return mapVertexes.get(hex.getName() + "." + side);
    }

    public static void optimizeGraph(Graph<NetworkVertex, NetworkEdge> graph) {
        while (removeVertexes(graph));
    }
    
    /** remove deadend and vertex with only two edges */ 
    private static boolean removeVertexes(Graph<NetworkVertex, NetworkEdge> graph){
        
        boolean removed = false;
        for (NetworkVertex vertex:graph.vertexSet()) {
            if (!vertex.isSide()) continue;
            
            if (graph.edgesOf(vertex).size() == 1) { 
                graph.removeVertex(vertex);
                removed = true;
                break;
            } else  if (graph.edgesOf(vertex).size() == 2) { // vertex is not necessary
                // reconnect
                NetworkEdge[] edges = graph.edgesOf(vertex).toArray(new NetworkEdge[2]);
                NetworkVertex firstVertex = Graphs.getOppositeVertex(graph, edges[0], vertex);
                NetworkVertex secondVertex = Graphs.getOppositeVertex(graph, edges[1], vertex);
                boolean autoEdge = edges[0].isAutoEdge() || edges[1].isAutoEdge();
                graph.addEdge(firstVertex, secondVertex,
                        new NetworkEdge(firstVertex, secondVertex, autoEdge));
                // remove vertex
                graph.removeVertex(vertex);
                removed = true;
                break;
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
        @SuppressWarnings("unchecked")
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
