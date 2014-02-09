package net.sf.rails.game.specific._1880;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.rails.algorithms.NetworkEdge;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.PublicCompany;

import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.Subgraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * This class stores and creates the various graphs
 * defined for each company
 *
 */
public class NetworkCompanyGraph_1880 {
    protected static Logger log =
            LoggerFactory.getLogger(NetworkCompanyGraph_1880.class.getPackage().getName());

    private final NetworkGraphBuilder graphBuilder;
    private final PublicCompany company;
        
    private NetworkCompanyGraph_1880(NetworkGraphBuilder graphBuilder, PublicCompany company) {
        this.graphBuilder = graphBuilder;
        this.company = company;
    }
    
    public static NetworkCompanyGraph_1880 create(NetworkGraphBuilder graphBuilder, PublicCompany company) {
       return new NetworkCompanyGraph_1880(graphBuilder, company);
    }
        
    public SimpleGraph<NetworkVertex, NetworkEdge> createConnectionGraph(boolean addHQ) {
        // get mapgraph from builder
        SimpleGraph<NetworkVertex, NetworkEdge> mapGraph = graphBuilder.getMapGraph();
        
        // set sinks on mapgraph
        NetworkVertex.initAllRailsVertices(mapGraph, company, null);
        
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
            NetworkIterator_1880 iterator = new NetworkIterator_1880(mapGraph, vertex, company);
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
        NetworkVertex.initAllRailsVertices(mapGraph, null, null);
        
        // store and return
        return graph;
    }
        
    private List<NetworkVertex> getCompanyBaseTokenVertexes(PublicCompany company) {
        List<NetworkVertex> vertexes = new ArrayList<NetworkVertex>();
        for (BaseToken token:company.getAllBaseTokens()){
            NetworkVertex vertex = graphBuilder.getVertex(token);
            if (vertex == null) continue;
            vertexes.add(vertex);
        }
        return vertexes;
    }

    
}
