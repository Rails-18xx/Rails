using GameLib.Net.Common;
using GameLib.Net.Graph;
using System;
using System.Collections.Generic;
using System.Text;
using Wintellect.PowerCollections;

/** 
 * the phase 2 graph is a multigraph due to the multiple routes between vertices
 */

namespace GameLib.Net.Algorithms
{
    public class NetworkMultigraph
    {
        protected static Logger<NetworkMultigraph> log = new Logger<NetworkMultigraph>();

        private NetworkGraph inGraph;
        private Multigraph<NetworkVertex, NetworkEdge> graph2 = new Multigraph<NetworkVertex, NetworkEdge>();
        private MultiDictionary<NetworkEdge, NetworkEdge> partial2route = new MultiDictionary<NetworkEdge, NetworkEdge>(false);
        private MultiDictionary<NetworkEdge, NetworkEdge> route2partial = new MultiDictionary<NetworkEdge, NetworkEdge>(false);

        private NetworkMultigraph(NetworkGraph inGraph)
        {
            this.inGraph = inGraph;
        }

        public static NetworkMultigraph Create(NetworkGraph inGraph, IEnumerable<NetworkVertex> protectedVertices)
        {
            NetworkMultigraph newGraph = new NetworkMultigraph(inGraph);
            newGraph.InitMultigraph(protectedVertices);
            return newGraph;
        }

        public Multigraph<NetworkVertex, NetworkEdge> Graph
        {
            get
            {
                return graph2;
            }
        }

        private void InitMultigraph(IEnumerable<NetworkVertex> protectedVertices)
        {
            log.Info("Ingraph" + inGraph.Graph);
            // clone the inGraph
            SimpleGraph<NetworkVertex, NetworkEdge> graph = new SimpleGraph<NetworkVertex, NetworkEdge>();
            graph.AddGraph(inGraph.Graph);

            // define the relevant vertices: stations and protected
            HashSet<NetworkVertex> relevantVertices = new HashSet<NetworkVertex>();
            if (protectedVertices != null)
            {
                // check if they are in the graph
                foreach (NetworkVertex vertex in protectedVertices)
                {
                    if (graph.ContainsVertex(vertex))
                    {
                        relevantVertices.Add(vertex);
                    }
                }

            }

            // add station vertices
            foreach (NetworkVertex vertex in graph.Vertices)
            {
                if (vertex.IsStation)
                {
                    relevantVertices.Add(vertex);
                }
            }

            // change to sink and store them
            List<NetworkVertex> sinkVertices = new List<NetworkVertex>();
            foreach (NetworkVertex vertex in relevantVertices)
            {
                if (!vertex.IsSink)
                {
                    vertex.IsSink = true;
                }
                else
                {
                    sinkVertices.Add(vertex);
                }
            }

            // add all the relevantVertices to the phase 2 graph
            graph2.AddVertices(relevantVertices);

            List<NetworkVertex> relevantVertices2 = new List<NetworkVertex>(relevantVertices);
            //  Collections.sort(relevantVertices2);

            // run the iterator for routes for each vertex
            foreach (NetworkVertex startVertex in relevantVertices2)
            {
                startVertex.IsSink = false; // deactivate sink for that vertex
                                            // define iterator to find all routes from here
                NetworkIterator iterator = new NetworkIterator(graph, startVertex).SetRouteIterator(true);
                log.Info("Phase 2 Graph: Start routes from " + startVertex);
                //for (;iterator.hasNext();)
                while (iterator.MoveNext())
                {
                    // found new route
                    NetworkVertex nextVertex = iterator.Current; // next();
                    if (nextVertex.IsSink && nextVertex != startVertex)
                    {
                        List<NetworkVertex> route = iterator.GetCurrentRoute();
                        log.Info("Phase 2 Graph: Route found to " + nextVertex + " with route = " + route);
                        // define routeEdge
                        NetworkEdge routeEdge = null;
                        HashSet<NetworkEdge> partialEdges = new HashSet<NetworkEdge>();
                        // previousVertex
                        NetworkVertex currentVertex = null;
                        // define new edge by going through the route edges
                        foreach (NetworkVertex routeVertex in route)
                        {
                            if (currentVertex != null)
                            {
                                NetworkEdge partialEdge = graph.GetEdge(currentVertex, routeVertex);
                                if (routeEdge == null)
                                {
                                    routeEdge = partialEdge;
                                }
                                else
                                {
                                    routeEdge = NetworkEdge.MergeEdges(routeEdge, partialEdge).newEdge;
                                }
                                partialEdges.Add(partialEdge);
                            }
                            currentVertex = routeVertex;
                        }
                        // define partial2route entries
                        foreach (NetworkEdge partialEdge in partialEdges)
                        {
                            partial2route.Add(partialEdge, routeEdge);
                        }
                        // store route2partial
                        route2partial.AddMany(routeEdge, partialEdges);
                        graph2.AddEdge(startVertex, currentVertex, routeEdge);
                    }
                }
                // remove that vertex from the graph to avoid duplication of the routes
                graph.RemoveVertex(startVertex);
            }

            // restore sinkVertices
            foreach (NetworkVertex vertex in sinkVertices)
            {
                vertex.IsSink = true;
            }

            log.Info("Defined graph phase 2 = " + graph2);

            // TODO: Check if this has no effect as it only logs?
            List<NetworkEdge> edges = new List<NetworkEdge>(graph2.Edges);
            edges.Sort();
            StringBuilder s = new StringBuilder();
            foreach (NetworkEdge e in edges)
            {
                s.Append("\n" + e.GetOrderedConnection());
            }
            log.Info("Edges = " + s.ToString());
        }

        public Dictionary<NetworkEdge, RevenueAdapter.EdgeTravel> GetPhaseTwoEdgeSets(RevenueAdapter adapter)
        {

            Dictionary<NetworkEdge, RevenueAdapter.EdgeTravel> edgeSets = new Dictionary<NetworkEdge, RevenueAdapter.EdgeTravel>();
            // convert route2partial and partial2route into edgesets
            foreach (NetworkEdge route in route2partial.Keys)
            {
                RevenueAdapter.EdgeTravel edgeTrav = new RevenueAdapter.EdgeTravel();
                foreach (NetworkEdge partial in route2partial[route])
                {
                    if (partial2route[partial].Count >= 2)
                    { // only keep true sets
                        edgeTrav.set.UnionWith(partial2route[partial]);
                    }
                }
                edgeTrav.set.Remove(route);
                route.RouteCosts = edgeTrav.set.Count;
                //    route.setRouteCosts(-(route.getSource().getValue() + route.getTarget().getValue()));
                // define route costs as the size of the travel set
                if (edgeTrav.set.Count != 0)
                {
                    edgeSets[route] = edgeTrav;
                }
            }



            return edgeSets;

        }

    }
}
