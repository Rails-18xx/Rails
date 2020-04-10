package net.sf.rails.algorithms;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.rails.algorithms.RevenueAdapter.EdgeTravel;

import org.jgrapht.Graphs;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * the phase 2 graph is a multigraph due to the multiple routes between vertices
 */
public class NetworkMultigraph {

    private static final Logger log = LoggerFactory.getLogger(NetworkMultigraph.class);

    private final NetworkGraph inGraph;
    private final Multigraph<NetworkVertex, NetworkEdge> graph2 =
            new Multigraph<NetworkVertex, NetworkEdge>(NetworkEdge.class);
    private final Multimap<NetworkEdge, NetworkEdge> partial2route =
            HashMultimap.create();
    private final Multimap<NetworkEdge, NetworkEdge> route2partial =
            HashMultimap.create();

    private NetworkMultigraph(NetworkGraph inGraph) {
        this.inGraph = inGraph;
    }

    public static NetworkMultigraph create(NetworkGraph inGraph, Collection<NetworkVertex> protectedVertices) {
        NetworkMultigraph newGraph = new NetworkMultigraph(inGraph);
        newGraph.initMultigraph(protectedVertices);
        return newGraph;
    }

    public Multigraph<NetworkVertex, NetworkEdge> getGraph() {
        return graph2;
    }

    private void initMultigraph(Collection<NetworkVertex> protectedVertices) {
        log.debug("Ingraph" + inGraph.getGraph());
        // clone the inGraph
        SimpleGraph<NetworkVertex, NetworkEdge> graph = new SimpleGraph<NetworkVertex, NetworkEdge>(NetworkEdge.class);
        Graphs.addGraph(graph, inGraph.getGraph());

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

        List<NetworkVertex> relevantVertices2 = new ArrayList<NetworkVertex>(relevantVertices);
        //  Collections.sort(relevantVertices2);

        // run the iterator for routes for each vertex
        for (NetworkVertex startVertex:relevantVertices2) {
            startVertex.setSink(false); // deactivate sink for that vertex
            // define iterator to find all routes from here
            NetworkIterator iterator = new NetworkIterator(graph, startVertex).setRouteIterator(true);
            log.info("Phase 2 Graph: Start routes from {}", startVertex);
            while ( iterator.hasNext() ) {
                // found new route
                NetworkVertex nextVertex = iterator.next();
                if (nextVertex.isSink() && nextVertex != startVertex) {
                    List<NetworkVertex> route = iterator.getCurrentRoute();
                    log.info("Phase 2 Graph: Route found to {} with route = {}", nextVertex, route);
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
                        partial2route.put(partialEdge, routeEdge);
                    }
                    // store route2partial
                    route2partial.putAll(routeEdge, partialEdges);
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

        log.debug("Defined graph phase 2 = {}", graph2);

        // TODO: Check if this has no effect as it only logs?
        List<NetworkEdge> edges = new ArrayList<NetworkEdge>(graph2.edgeSet());
        Collections.sort(edges);
        StringBuilder s = new StringBuilder();
        for (NetworkEdge e:edges) {
            s.append("\n").append(e.getOrderedConnection());
        }
        log.debug("Edges = {}", s.toString());
    }

    public Map<NetworkEdge, EdgeTravel> getPhaseTwoEdgeSets(RevenueAdapter adapter) {

        Map<NetworkEdge, EdgeTravel> edgeSets = new HashMap<NetworkEdge, EdgeTravel>();
        // convert route2partial and partial2route into edgesets
        for (NetworkEdge route:route2partial.keySet()){
            EdgeTravel edgeTrav = new EdgeTravel();
            for (NetworkEdge partial:route2partial.get(route)) {
                if (partial2route.get(partial).size() >= 2) { // only keep true sets
                    edgeTrav.set.addAll(partial2route.get(partial));
                }
            }
            edgeTrav.set.remove(route);
            route.setRouteCosts(edgeTrav.set.size());
            //    route.setRouteCosts(-(route.getSource().getValue() + route.getTarget().getValue()));
            // define route costs as the size of the travel set
            if (edgeTrav.set.size() != 0) {
                edgeSets.put(route, edgeTrav);
            }
        }



        return edgeSets;

    }

}
