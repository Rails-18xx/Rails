package rails.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jgrapht.Graphs;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.Subgraph;

import rails.algorithms.RevenueAdapter.EdgeTravel;
import rails.game.PublicCompanyI;
import rails.game.TokenI;

/**
 * This class stores and creates the various graphs
 * defined for each company
 *
 */
public class NetworkCompanyGraph {
    protected static Logger log =
        Logger.getLogger(NetworkCompanyGraph.class.getPackage().getName());

    private final NetworkGraphBuilder graphBuilder;
    private final PublicCompanyI company;
    
    private SimpleGraph<NetworkVertex, NetworkEdge> routeGraph;
    private SimpleGraph<NetworkVertex, NetworkEdge> revenueGraph;

    private Multigraph<NetworkVertex, NetworkEdge> phase2Graph;
    private Map<NetworkEdge, Set<NetworkEdge>> partial2route;
    private Map<NetworkEdge, Set<NetworkEdge>> route2partial;
    
    private Collection<NetworkVertex> protectedVertices;
    
    private NetworkCompanyGraph(NetworkGraphBuilder graphBuilder, PublicCompanyI company) {
        this.graphBuilder = graphBuilder;
        this.company = company;
        this.routeGraph = null;
        this.revenueGraph = null;
        this.phase2Graph = null;
    }
    
    public static NetworkCompanyGraph create(NetworkGraphBuilder graphBuilder, PublicCompanyI company) {
       return new NetworkCompanyGraph(graphBuilder, company);
    }
    
    public SimpleGraph<NetworkVertex, NetworkEdge> createRouteGraph(boolean addHQ) {
        // get mapgraph from builder
        SimpleGraph<NetworkVertex, NetworkEdge> mapGraph = graphBuilder.getMapGraph();
        
        // set sinks on mapgraph
        NetworkVertex.initAllRailsVertices(mapGraph.vertexSet(), company, null);
        
        // initialized simple graph
        SimpleGraph<NetworkVertex, NetworkEdge> graph = new SimpleGraph<NetworkVertex, NetworkEdge>(NetworkEdge.class);
        // add Company HQ
        NetworkVertex hqVertex = new NetworkVertex(company); 
        graph.addVertex(hqVertex);
        
        // create vertex set for subgraph
        List<NetworkVertex> tokenVertexes = getCompanyBaseTokenVertexes(company);
        Set<NetworkVertex> vertexes = new HashSet<NetworkVertex>();
        
        for (NetworkVertex vertex:tokenVertexes){
            // allow to leave tokenVertices even if those are sinks
            boolean storeSink = vertex.isSink(); vertex.setSink(false);
            vertexes.add(vertex);
            // add connection to graph
            graph.addVertex(vertex);
            graph.addEdge(vertex, hqVertex, new NetworkEdge(vertex, hqVertex, false));
            NetworkIterator iterator = new NetworkIterator(mapGraph, vertex, company);
            for (;iterator.hasNext();)
                vertexes.add(iterator.next());
            // restore sink property
            vertex.setSink(storeSink);
        }

        Subgraph<NetworkVertex, NetworkEdge, SimpleGraph<NetworkVertex, NetworkEdge>> subGraph = 
            new Subgraph<NetworkVertex, NetworkEdge, SimpleGraph<NetworkVertex, NetworkEdge>>
            (mapGraph, vertexes);
        // now add all vertexes and edges to the graph
        Graphs.addGraph(graph, subGraph);

        // if addHQ is not set remove HQ vertex
        if (!addHQ) graph.removeVertex(hqVertex);
        
        // deactivate sinks on mapgraph
        NetworkVertex.initAllRailsVertices(mapGraph.vertexSet(), null, null);
        
        // store and return
        routeGraph = graph;
        return graph;
    }
    
    public List<NetworkVertex> getCompanyBaseTokenVertexes(PublicCompanyI company) {
        List<NetworkVertex> vertexes = new ArrayList<NetworkVertex>();
        for (TokenI token:company.getTokens()){
            NetworkVertex vertex = graphBuilder.getVertex(token);
            if (vertex == null) continue;
            vertexes.add(vertex);
        }
        return vertexes;
    }
    
    public SimpleGraph<NetworkVertex, NetworkEdge> createRevenueGraph(Collection<NetworkVertex> protectedVertices) {
 
        // store protected vertices
        this.protectedVertices = protectedVertices;
            
        // optimize graph (optimizeGraph clones the graph)
        revenueGraph = NetworkGraphBuilder.optimizeGraph(routeGraph, protectedVertices);

        return revenueGraph;
    }

    Map<NetworkEdge, EdgeTravel> getPhaseTwoEdgeSets(RevenueAdapter revenueAdapter) {
        
        Map<NetworkEdge, EdgeTravel> edgeSets = new HashMap<NetworkEdge, EdgeTravel>();
        // convert route2partial and partial2route into edgesets
        for (NetworkEdge route:route2partial.keySet()){
            EdgeTravel edgeTravel = revenueAdapter.new EdgeTravel(); 
            for (NetworkEdge partial:route2partial.get(route)) {
                if (partial2route.get(partial).size() >= 2) { // only keep true sets
                    edgeTravel.set.addAll(partial2route.get(partial));
                }
            }
            edgeTravel.set.remove(route);
          route.setRouteCosts(edgeTravel.set.size());
//          route.setRouteCosts(-(route.getSource().getValue() + route.getTarget().getValue()));
            // define route costs as the size of the travel set
            if (edgeTravel.set.size() != 0) {
                edgeSets.put(route, edgeTravel);
            }
        }
        
        
        
        return edgeSets;
        
        
    }
    
    public Multigraph<NetworkVertex, NetworkEdge> createPhaseTwoGraph() {
       
       // clone the revenueGraph
       SimpleGraph<NetworkVertex, NetworkEdge> graph = new SimpleGraph<NetworkVertex, NetworkEdge>(NetworkEdge.class);
       Graphs.addGraph(graph, revenueGraph);
       
       // the phase 2 graph is a multigraph due to the multiple routes between vertices
       Multigraph<NetworkVertex, NetworkEdge> graph2 = new Multigraph<NetworkVertex, NetworkEdge>(NetworkEdge.class); 

       // define the relevant vertices: stations and protected
       Set<NetworkVertex> relevantVertices = new HashSet<NetworkVertex>();
       if (protectedVertices != null) {
           // check if they are in the graph
           for (NetworkVertex vertex:protectedVertices) {
               if (graph.containsVertex(vertex)) {
                   relevantVertices.add(vertex);
               }
           }
               
       }
       
       // add station vertices
       for (NetworkVertex vertex:graph.vertexSet()) {
           if (vertex.isStation()) {
               relevantVertices.add(vertex);
           }
       }
       
       // change to sink and store them
       List<NetworkVertex> sinkVertices = new ArrayList<NetworkVertex>();
       for (NetworkVertex vertex:relevantVertices) {
           if (!vertex.isSink()) {
               vertex.setSink(true);
           } else {
              sinkVertices.add(vertex);
           }
       }
       
       // add all the relevantVertices to the phase 2 graph
       Graphs.addAllVertices(graph2, relevantVertices);
       
       // define the storage for the new edges to the old edges
       partial2route = new HashMap<NetworkEdge, Set<NetworkEdge>>();
       for (NetworkEdge edge:graph.edgeSet()) {
           partial2route.put(edge, new HashSet<NetworkEdge>());
       }
       route2partial = new HashMap<NetworkEdge, Set<NetworkEdge>>();
       
       List<NetworkVertex> relevantVertices2 = new ArrayList<NetworkVertex>(relevantVertices);
//       Collections.sort(relevantVertices2);
       
       // run the iterator for routes for each vertex
       for (NetworkVertex startVertex:relevantVertices2) {
           startVertex.setSink(false); // deactivate sink for that vertex
           // define iterator to find all routes from here
           NetworkIterator iterator = new NetworkIterator(graph, startVertex).setRouteIterator(true);
           log.info("Phase 2 Graph: Start routes from " + startVertex);
           for (;iterator.hasNext();) {
               // found new route
               NetworkVertex nextVertex = iterator.next();
               if (nextVertex.isSink() && nextVertex != startVertex) {
                   List<NetworkVertex> route = iterator.getCurrentRoute();
                   log.info("Phase 2 Graph: Route found to " + nextVertex + " with route = " + route);
                   // define routeEdge
                   NetworkEdge routeEdge = null;
                   Set<NetworkEdge> partialEdges = new HashSet<NetworkEdge>();
                   // previousVertex
                   NetworkVertex currentVertex = null;
                   // define new edge by going through the route edges
                   for (NetworkVertex routeVertex:route) {
                       if (currentVertex != null) {
                           NetworkEdge partialEdge = graph.getEdge(currentVertex, routeVertex); 
                           if (routeEdge == null) {
                               routeEdge = partialEdge;
                           } else {
                              routeEdge = NetworkEdge.mergeEdges(routeEdge, partialEdge).newEdge;
                           }
                           partialEdges.add(partialEdge);
                       }
                       currentVertex = routeVertex;
                   }
                   // define partial2route entries
                   for (NetworkEdge partialEdge:partialEdges) {
                       partial2route.get(partialEdge).add(routeEdge);
                   }
                   // store route2partial
                   route2partial.put(routeEdge, partialEdges);
                   // add new route
                   graph2.addEdge(startVertex, currentVertex, routeEdge);
               }
           }
           // remove that vertex from the graph to avoid duplication of the routes
           graph.removeVertex(startVertex);
       }

       // restore sinkVertices
       for (NetworkVertex vertex:sinkVertices) {
            vertex.setSink(true);
       }
       
       log.info("Defined graph phase 2 = " + graph2);
       
       List<NetworkEdge> edges = new ArrayList<NetworkEdge>(graph2.edgeSet());
       Collections.sort(edges);
       StringBuffer s = new StringBuffer();
       for (NetworkEdge e:edges) {
           s.append("\n" + e.getOrderedConnection());
       }
       log.info("Edges = " + s.toString());

       // store and return
       phase2Graph = graph2;
       
       return graph2;
    }
    
}
