using GameLib.Net.Common;
using GameLib.Net.Game;
using GameLib.Net.Game.State;
using GameLib.Net.Graph;
using System;
using System.Collections.Generic;
using System.Text;
using Wintellect.PowerCollections;

/**
 * NetworkGraph mirrors the structure of a 18xx track
 *
 * TODO: Rewrite this by separating the creation code from the data code
 * TODO: Rails 2.0 add a NetworkManager
 */

namespace GameLib.Net.Algorithms
{
    public class NetworkGraph
    {
        protected static Logger<NetworkGraph> log = new Logger<NetworkGraph>();

        private SimpleGraph<NetworkVertex, NetworkEdge> graph;

        private Dictionary<string, NetworkVertex> vertices;

        private NetworkIterator iterator;

        private NetworkGraph()
        {
            graph = new SimpleGraph<NetworkVertex, NetworkEdge>();
            vertices = new Dictionary<string, NetworkVertex>();
        }

        private NetworkGraph(NetworkGraph inGraph)
        {
            graph = new SimpleGraph<NetworkVertex, NetworkEdge>();
            //Graphs.addGraph(graph, inGraph.graph);
            graph.AddGraph(inGraph.graph);
            vertices = new Dictionary<string, NetworkVertex>(inGraph.vertices);
        }

        public static NetworkGraph CreateMapGraph(RailsRoot root)
        {
            NetworkGraph graph = new NetworkGraph();
            graph.GenerateMapGraph(root);
            return graph;
        }

        public static NetworkGraph CreateRouteGraph(NetworkGraph mapGraph, PublicCompany company, bool addHQ)
        {
            NetworkGraph newGraph = new NetworkGraph();
            newGraph.InitRouteGraph(mapGraph, company, addHQ);
            newGraph.RebuildVertices();
            return newGraph;
        }

        public static NetworkGraph CreateOptimizedGraph(NetworkGraph inGraph,
                ICollection<NetworkVertex> protectedVertices)
        {
            NetworkGraph newGraph = new NetworkGraph(inGraph);
            newGraph.OptimizeGraph(protectedVertices);
            newGraph.RebuildVertices();
            return newGraph;
        }

        public NetworkGraph CloneGraph()
        {
            return new NetworkGraph(this);
        }

        public SimpleGraph<NetworkVertex, NetworkEdge> Graph
        {
            get
            {
                return graph;
            }
        }

        public void SetIteratorStart(MapHex hex, Station station)
        {
            iterator = new NetworkIterator(graph, GetVertex(hex, station));
        }

        public NetworkIterator Iterator
        {
            get
            {
                return iterator;
            }
            set
            {

            }
        }

        public NetworkVertex GetVertexByIdentifier(string identVertex)
        {
            return vertices[identVertex];
        }

        public NetworkVertex GetVertex(BaseToken token)
        {
            IOwner owner = token.Owner;
            // TODO: Check if this still works
            if (!(owner is Stop)) return null;
            Stop city = (Stop)owner;
            MapHex hex = city.Parent;
            Station station = city.GetRelatedStation();
            return GetVertex(hex, station);
        }

        public NetworkVertex GetVertex(MapHex hex, TrackPoint point)
        {
            return vertices[hex.Id + "." + point.TrackPointNumber];
        }

        public NetworkVertex GetVertex(MapHex hex, int trackPointNr)
        {
            return vertices[hex.Id + "." + trackPointNr];
        }

        public NetworkVertex GetVertexRotated(MapHex hex, TrackPoint point)
        {
            if (point.TrackPointType == TrackPoint.TrackPointTypeEnum.SIDE)
                point = point.Rotate(hex.CurrentTileRotation);
            return vertices[hex.Id + "." + point.TrackPointNumber];
        }

        public Dictionary<MapHex, HexSidesSet> GetReachableSides()
        {
            // first create builders for all HexSides
            Dictionary<MapHex, HexSidesSet.Builder> hexSides = new Dictionary<MapHex, HexSidesSet.Builder>();
            foreach (NetworkVertex vertex in graph.Vertices)
            {
                if (vertex.IsSide && iterator.SeenData[vertex]
                        != NetworkIterator.GreedyState.greedy)
                {
                    MapHex hex = vertex.Hex;
                    if (!hexSides.ContainsKey(hex))
                    {
                        hexSides[hex] = HexSidesSet.GetBuilder();
                    }
                    hexSides[hex].Set(vertex.Side);
                }
            }
            // second build the map of mapHex to HexSides
            Dictionary<MapHex, HexSidesSet> hexBuilder = new Dictionary<MapHex, HexSidesSet>();
            foreach (MapHex hex in hexSides.Keys)
            {
                hexBuilder[hex] = hexSides[hex].Build();
            }
            return hexBuilder;
        }

        /**
         * @return a map of all hexes and stations that can be run through
         */
        public MultiDictionary<MapHex, Station> GetPassableStations()
        {

            MultiDictionary<MapHex, Station> hexStations = new MultiDictionary<MapHex, Station>(false);

            foreach (NetworkVertex vertex in graph.Vertices)
            {
                if (vertex.IsStation && !vertex.IsSink)
                {
                    hexStations.Add(vertex.Hex, vertex.Station);
                }
            }

            return hexStations;
        }

        /**
         * @return a list of all stops that are tokenable for the argument company
         */
        public MultiDictionary<MapHex, Stop> GetTokenableStops(PublicCompany company)
        {

            MultiDictionary<MapHex, Stop> hexStops = new MultiDictionary<MapHex, Stop>(false);

            foreach (NetworkVertex vertex in graph.Vertices)
            {
                Stop stop = vertex.Stop;
                if (stop != null && stop.IsTokenableFor(company))
                {
                    hexStops.Add(vertex.Hex, stop);
                }
            }
            return hexStops;
        }

        private void RebuildVertices()
        {
            // rebuild mapVertices
            vertices.Clear();
            foreach (NetworkVertex v in graph.Vertices)
            {
                vertices[v.Identifier] = v;
            }
        }

        private void GenerateMapGraph(RailsRoot root)
        {
            MapManager mapManager = root.MapManager;
            RevenueManager revenueManager = root.RevenueManager;
            foreach (MapHex hex in mapManager.GetHexes())
            {
                // get Tile
                Tile tile = hex.CurrentTile;

                // then get stations
                IReadOnlyCollection<Station> stations = tile.Stations;
                // and add those to the mapGraph
                foreach (Station station in stations)
                {
                    NetworkVertex stationVertex = new NetworkVertex(hex, station);
                    graph.AddVertex(stationVertex);
                    vertices[stationVertex.Identifier] = stationVertex;
                    log.Info("Added " + stationVertex);
                }

                // get tracks per side to add that vertex
                foreach (HexSide side in HexSide.All())
                    if (tile.HasTracks(side))
                    {
                        HexSide rotated = (HexSide)side.Rotate(hex.CurrentTileRotation);
                        NetworkVertex sideVertex = new NetworkVertex(hex, rotated);
                        graph.AddVertex(sideVertex);
                        vertices[sideVertex.Identifier] = sideVertex;
                        log.Info("Added " + sideVertex);
                    }
            }

            // loop over all hex and add tracks
            foreach (MapHex hex in mapManager.GetHexes())
            {
                // get Tile
                Tile tile = hex.CurrentTile;
                // get Tracks
                List<Track> tracks = tile.Tracks;

                foreach (Track track in tracks)
                {
                    NetworkVertex startVertex = GetVertexRotated(hex, track.Start);
                    NetworkVertex endVertex = GetVertexRotated(hex, track.End);
                    log.Info("Track: " + track);
                    NetworkEdge edge = new NetworkEdge(startVertex, endVertex, false);
                    if (startVertex == endVertex)
                    {
                        log.Error("Track " + track + " on hex " + hex + "has identical start/end");
                    }
                    else
                    {
                        graph.AddEdge(startVertex, endVertex, edge);
                        log.Info("Added non-greedy edge " + edge.GetConnection());
                    }
                }

                // TODO: Rewrite this by employing the features of Trackpoint
                // and connect to neighboring hexes (for sides 0-2)
                foreach (HexSide side in HexSide.Head())
                {
                    MapHex neighborHex = mapManager.GetNeighbor(hex, side);
                    if (neighborHex == null)
                    {
                        log.Info("No connection for Hex " + hex.Id + " at "
                                + hex.GetOrientationName(side) + ", No Neighbor");
                        continue;
                    }
                    NetworkVertex vertex = GetVertex(hex, side);
                    HexSide rotated = side.Opposite;
                    NetworkVertex otherVertex = GetVertex(neighborHex, rotated);
                    if (vertex == null && otherVertex == null)
                    {
                        log.Info("Hex " + hex.Id + " has no track at "
                                + hex.GetOrientationName(side));
                        log.Info("And Hex " + neighborHex.Id + " has no track at "
                                + neighborHex.GetOrientationName(rotated));
                        continue;
                    }
                    else if (vertex == null && otherVertex != null)
                    {
                        log.Info("Deadend connection for Hex " + neighborHex.Id + " at "
                                + neighborHex.GetOrientationName(rotated) + ", NeighborHex "
                                + hex.Id + " has no track at side " +
                                hex.GetOrientationName(side));
                        vertex = new NetworkVertex(hex, side);
                        graph.AddVertex(vertex);
                        vertices[vertex.Identifier] = vertex;
                        log.Info("Added deadend vertex " + vertex);
                    }
                    else if (otherVertex == null)
                    {
                        log.Info("Deadend connection for Hex " + hex.Id + " at "
                                + hex.GetOrientationName(side) + ", NeighborHex "
                                + neighborHex.Id + " has no track at side " +
                                neighborHex.GetOrientationName(rotated));
                        otherVertex = new NetworkVertex(neighborHex, rotated);
                        graph.AddVertex(otherVertex);
                        vertices[otherVertex.Identifier] = otherVertex;
                        log.Info("Added deadend vertex " + otherVertex);
                    }
                    NetworkEdge edge = new NetworkEdge(vertex, otherVertex, true);
                    graph.AddEdge(vertex, otherVertex, edge);
                    log.Info("Added greedy edge " + edge.GetConnection());
                }
            }

            // add graph modifiers
            if (revenueManager != null)
            {
                revenueManager.ActivateMapGraphModifiers(this);
            }
        }

        public void OptimizeGraph()
        {
            OptimizeGraph(new List<NetworkVertex>());
        }

        private void OptimizeGraph(ICollection<NetworkVertex> protectedVertices)
        {

            // remove vertices until convergence
            bool notDone = true;
            while (notDone)
            {
                IncreaseGreedness();
                notDone = RemoveVertexes(protectedVertices);
                // removedVertices can change Greedness, but not vice-versa
            }
        }

        // Increase Greedness implies that an edge that 
        // connects stations and/or sides with only one track in/out
        // can be set to greedy (as one has to follow the exit anyway)
        private void IncreaseGreedness()
        {
            foreach (NetworkEdge edge in graph.Edges)
            {
                if (edge.IsGreedy) continue;
                NetworkVertex source = edge.Source;
                NetworkVertex target = edge.Target;
                if ((source.IsSide && graph.EdgesOf(source).Count == 2 || source.IsStation) &&
                        (target.IsSide && graph.EdgesOf(target).Count == 2 || target.IsStation))
                {
                    edge.IsGreedy = true;
                    log.Info("Increased greedness for " + edge.GetConnection());
                }
            }
        }

        /** remove deadend and vertex with only two edges */
        private bool RemoveVertexes(ICollection<NetworkVertex> protectedVertices)
        {

            bool removed = false;

            foreach (NetworkVertex vertex in new List<NetworkVertex>(graph.Vertices))
            {
                ICollection<NetworkEdge> vertexEdges = graph.EdgesOf(vertex);

                // always keep protected vertices
                if (protectedVertices.Contains(vertex))
                {
                    continue;
                }

                // remove hermit
                if (vertexEdges.Count == 0)
                {
                    log.Info("Remove hermit (no connection) = " + vertex);
                    graph.RemoveVertex(vertex);
                    removed = true;
                }

                // the following only for side vertexes
                if (!vertex.IsSide) continue;

                if (vertexEdges.Count == 1)
                {
                    log.Info("Remove deadend side (single connection) = " + vertex);
                    graph.RemoveVertex(vertex);
                    removed = true;
                }
                else if (vertexEdges.Count == 2)
                { // not necessary vertices 
                    NetworkEdge[] edges = new List<NetworkEdge>(vertexEdges).ToArray();
                    if (edges[0].IsGreedy == edges[1].IsGreedy)
                    {
                        if (!edges[0].IsGreedy)
                        {
                            log.Info("Remove deadend side (no greedy connection) = " + vertex);
                            // two non greedy edges indicate a deadend
                            graph.RemoveVertex(vertex);
                            removed = true;
                        }
                        else
                        {
                            // greedy case:
                            // merge greedy edges if the vertexes are not already connected
                            if (NetworkEdge.MergeEdgesInGraph(graph, edges[0], edges[1]))
                            {
                                removed = true;
                            }
                        }
                    }
                }
            }
            return removed;
        }

        private void InitRouteGraph(NetworkGraph mapGraph, PublicCompany company, bool addHQ)
        {

            // add graph modifiers
            RevenueManager revenueManager = company.GetRoot.RevenueManager;
            if (revenueManager != null)
            {
                revenueManager.ActivateRouteGraphModifiers(mapGraph, company);
            }

            // set sinks on mapgraph
            NetworkVertex.InitAllRailsVertices(mapGraph, company, null);

            // add Company HQ
            NetworkVertex hqVertex = new NetworkVertex(company);
            graph.AddVertex(hqVertex);

            // create vertex set for subgraph
            List<NetworkVertex> tokenVertexes = mapGraph.GetCompanyBaseTokenVertexes(company);
            HashSet<NetworkVertex> vertexes = new HashSet<NetworkVertex>();

            foreach (NetworkVertex vertex in tokenVertexes)
            {
                // allow to leave tokenVertices even if those are sinks
                // Examples are tokens in offBoard hexes
                bool storeSink = vertex.IsSink;
                vertex.IsSink = false;
                vertexes.Add(vertex);
                // add connection to graph
                graph.AddVertex(vertex);
                graph.AddEdge(vertex, hqVertex, new NetworkEdge(vertex, hqVertex, false));
                iterator = new NetworkIterator(mapGraph.Graph, vertex, company);
                //for (; iterator.hasNext();)
                while (iterator.MoveNext())
                {
                    vertexes.Add(iterator.Current);
                }
                // restore sink property
                vertex.IsSink = storeSink;
            }

            //Subgraph<NetworkVertex, NetworkEdge, SimpleGraph<NetworkVertex, NetworkEdge>> subGraph =
            //    new Subgraph<NetworkVertex, NetworkEdge, SimpleGraph<NetworkVertex, NetworkEdge>>
            //    (mapGraph.getGraph(), vertexes);
            var subGraph = mapGraph.Graph.CreateSubgraph(vertexes);
            // now add all vertexes and edges to the graph
            graph.AddGraph(subGraph);

            // if addHQ is not set remove HQ vertex
            if (!addHQ) graph.RemoveVertex(hqVertex);
        }

        public List<NetworkVertex> GetCompanyBaseTokenVertexes(PublicCompany company)
        {
            List<NetworkVertex> vertexes = new List<NetworkVertex>();
            foreach (BaseToken token in company.GetLaidBaseTokens())
            {
                NetworkVertex vertex = GetVertex(token);
                if (vertex == null) continue;
                vertexes.Add(vertex);
            }
            return vertexes;
        }

        public void Visualize(string title)
        {
            // show network mapGraph
#if false
            if (graph.Vertices.Count > 0)
            {
                JGraphModelAdapter<NetworkVertex, NetworkEdge> jGAdapter =
                    new JGraphModelAdapter<NetworkVertex, NetworkEdge>(graph);

                JGraph jgraph = new JGraph(jGAdapter);

                List<NetworkVertex> vertexes = new ArrayList<NetworkVertex>(graph.vertexSet());

                Object[] rootCell = new Object[1];
                rootCell[0] = jGAdapter.getVertexCell(vertexes.get(0));

                JGraphFacade facade = new JGraphFacade(jgraph, rootCell);
                JGraphLayout layout = new JGraphFastOrganicLayout();
                layout.run(facade);

                // calculate size of network graph
                double ratio = Math.sqrt(graph.vertexSet().size() / 50.0);
                int width = (int)Math.floor(2400 * ratio);
                int height = (int)Math.floor(1800 * ratio);
                log.info("ratio=" + ratio + "width= " + width + "height" + height);
                facade.scale(new Rectangle(width, height));
                @SuppressWarnings("rawtypes")
                    Map nested = facade.createNestedMap(true, true);
                jgraph.getGraphLayoutCache().edit(nested);

                jgraph.setScale(0.75);

                JFrame frame = new JFrame();
                frame.setTitle(title + "(V=" + graph.vertexSet().size() +
                        ",E=" + graph.edgeSet().size() + ")");
                frame.setSize(new Dimension(800, 600));
                frame.getContentPane().add(new JScrollPane(jgraph));
                frame.pack();
                frame.setVisible(true);
            }
#endif
        }
    }
}
