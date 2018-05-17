using GameLib.Net.Common;
using GameLib.Net.Game;
using GameLib.Net.Graph;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Text;

namespace GameLib.Net.Algorithms
{
    public class NetworkVertex : IComparable<NetworkVertex>, IEquatable<NetworkVertex>
    {
        protected static Logger<NetworkVertex> log = new Logger<NetworkVertex>();

        public enum VertexTypes
        {
            STATION,
            SIDE,
            HQ,
        }

        public enum StationTypes
        {
            MAJOR,
            MINOR
        }

        // vertex types and flag for virtualVertex (thus not related to a rails object)
        private VertexTypes type;
        private bool virtualVertex;

        // vertex properties (for virtualVertex vertexes)
        private string virtualId;

        // general vertex properties
        private StationTypes stationType;
        private int value = 0;
        private bool sink = false;
        private string stopName = null;

        // references to rails objects, if not virtualVertex
        private MapHex hex;
        private TrackPoint trackPoint;
        // only for station objects
        private Stop stop;

        /** constructor for station on mapHex */
        public NetworkVertex(MapHex hex, Station station)
        {
            this.type = VertexTypes.STATION;
            this.hex = hex;
            this.trackPoint = station;
            this.stop = hex.GetRelatedStop(station);
            if (stop != null)
            {
                log.Info("Found stop " + stop);
            }
            else
            {
                log.Info("No stop found");
            }

            this.virtualVertex = false;
            this.virtualId = null;
        }

        /** constructor for side on mapHex */
        public NetworkVertex(MapHex hex, HexSide side)
        {
            this.type = VertexTypes.SIDE;
            this.hex = hex;
            this.trackPoint = side;
            this.stop = null;

            this.virtualVertex = false;
            this.virtualId = null;
        }

        /**  constructor for public company hq */
        public NetworkVertex(PublicCompany company) : this(VertexTypes.HQ, "HQ")
        {

        }

        private NetworkVertex(VertexTypes type, string name)
        {
            this.type = type;
            this.hex = null;
            this.trackPoint = null;
            this.stop = null;

            this.virtualVertex = true;
            this.virtualId = name;
        }

        /** factory method for virtualVertex vertex
         */
        public static NetworkVertex GetVirtualVertex(VertexTypes type, string name)
        {
            NetworkVertex vertex = new NetworkVertex(type, name);
            return vertex;
        }

        public void AddToRevenueCalculator(RevenueCalculator rc, int vertexId)
        {
            rc.SetVertex(vertexId, IsMajor, IsMinor, sink);
        }

        public string Identifier
        {
            get
            {
                if (virtualVertex)
                {
                    return virtualId;
                }
                else
                {
                    return hex.Id + "." + trackPoint.TrackPointNumber;
                }
            }
        }

        public bool IsVirtual
        {
            get
            {
                return virtualVertex;
            }
        }

        public bool IsStation
        {
            get
            {
                return type == VertexTypes.STATION;
            }
        }

        public bool IsSide
        {
            get
            {
                return type == VertexTypes.SIDE;
            }
        }

        public bool IsHQ
        {
            get
            {
                return type == VertexTypes.HQ;
            }
        }

        public VertexTypes VertexType
        {
            get
            {
                return type;
            }
        }

        public bool IsMajor
        {
            get
            {
                return (/*stationType != null &&*/ stationType == StationTypes.MAJOR);
            }
        }

        public bool IsMinor
        {
            get
            {
                return (/*stationType != null &&*/ stationType == StationTypes.MINOR);
            }
        }

        public StationTypes StationType
        {
            get
            {
                return stationType;
            }
            set
            {
                stationType = value;
            }
        }

        public NetworkVertex SetStationType(StationTypes stationType)
        {
            this.stationType = stationType;
            return this;
        }

        public int Value
        {
            get
            {
                return value;
            }
            set
            {
                this.value = value;
            }
        }

        public int GetValueByTrain(NetworkTrain train)
        {
            int valueByTrain;
            if (IsMajor)
            {
                valueByTrain = value * train.MultiplyMajors;
            }
            else if (IsMinor)
            {
                if (train.IgnoresMinors)
                {
                    valueByTrain = 0;
                }
                else
                {
                    valueByTrain = value * train.MultiplyMinors;
                }
            }
            else
            {
                valueByTrain = value;
            }
            return valueByTrain;
        }

        public NetworkVertex SetValue(int value)
        {
            this.value = value;
            return this;
        }

        public bool IsSink
        {
            get
            {
                return sink;
            }
            set
            {
                sink = value;
            }
        }

        //public NetworkVertex SetSink(bool sink)
        //{
        //    this.sink = sink;
        //    return this;
        //}

        public string StopName
        {
            get
            {
                return stopName;
            }
        }

        public NetworkVertex SetStopName(string locationName)
        {
            this.stopName = locationName;
            return this;
        }

        // getter for rails objects
        public MapHex Hex
        {
            get
            {
                return hex;
            }
        }

        public Station Station
        {
            get
            {
                if (type == VertexTypes.STATION)
                {
                    return (Station)trackPoint;
                }
                else
                {
                    return null;
                }
            }
        }

        public HexSide Side
        {
            get
            {
                if (type == VertexTypes.SIDE)
                {
                    return (HexSide)trackPoint;
                }
                else
                {
                    return null;
                }
            }
        }

        public Stop Stop
        {
            get
            {
                return stop;
            }
        }

        public bool IsOfType(VertexTypes vertexType, StationTypes stationType)
        {
            return (type == vertexType && (!IsStation || StationType == stationType));
        }

        /**
         * Initialize for rails vertexes
         * @return true = can stay inside the network, false = has to be removed
         */
        public bool InitRailsVertex(PublicCompany company)
        {
            // side vertices use the defaults, virtuals cannot use this function
            if (virtualVertex || type == VertexTypes.SIDE) return true;

            // Only station remains
            Station station = (Station)trackPoint;

            log.Info("Init of vertex " + this);

            // check if it has to be removed because it is run-to only
            // if company == null, then no vertex gets removed
            if (company != null && !stop.IsRunToAllowedFor(company))
            {
                log.Info("Vertex is removed");
                return false;
            }

            // check if it is a major or minor
            if (stop.ScoreType == StopType.Score.MAJOR)
            {
                SetStationType(StationTypes.MAJOR);
            }
            else if (stop.ScoreType == StopType.Score.MINOR)
            {
                SetStationType(StationTypes.MINOR);
            }

            // check if it is a sink
            if (company == null)
            { // if company == null, then all sinks are deactivated
                sink = false;
            }
            else
            {
                sink = !stop.IsRunThroughAllowedFor(company);
            }

            // define locationName
            stopName = null;
            if (station.GetStationType() == Station.StationType.OFFMAPCITY)
            {
                if (hex.StopName != null && !hex.StopName.Equals(""))
                {
                    stopName = hex.StopName;
                }
            }
            else
            {
                if (hex.StopName != null && !hex.StopName.Equals("")
                        && station.StopName != null && !station.StopName.Equals(""))
                {
                    stopName = hex.StopName + "." + station.StopName;
                }
            }

            // no removal
            return true;

        }

        public void SetRailsVertexValue(Phase phase)
        {
            // side vertices and  virtuals cannot use this function
            if (virtualVertex || type == VertexTypes.SIDE) return;

            // define value
            value = stop.GetValueForPhase(phase);
        }


        override public string ToString()
        {
            StringBuilder message = new StringBuilder();
            if (IsVirtual)
                message.Append(virtualId);
            else if (IsStation)
                message.Append(hex.Id + "." + ((Station)trackPoint).Number);
            else if (IsSide)
                message.Append(hex.Id + "." + hex.GetOrientationName((HexSide)trackPoint));
            else
                message.Append("HQ");
            if (IsSink)
                message.Append("/*");
            return message.ToString();
        }

        public int CompareTo(NetworkVertex otherVertex)
        {
            return this.Identifier.CompareTo(otherVertex.Identifier);
        }

        public bool Equals(NetworkVertex other)
        {
            if (other == null) return false;
            return Identifier == other.Identifier;
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (!(obj is NetworkVertex)) return false;
            return Identifier == ((NetworkVertex)obj).Identifier;
        }

        public override int GetHashCode()
        {
            return Identifier.GetHashCode();
        }

        public class ValueOrder : IComparer<NetworkVertex>
        {
            public int Compare(NetworkVertex vA, NetworkVertex vB)
            {
                int result = -vA.Value.CompareTo(vB.Value); // compare by value, descending
                if (result == 0)
                    result = vA.CompareTo(vB); // otherwise use natural ordering
                return result;
            }
        }

        /**
         * 
         * @param graph network graph
         * @param company the company (with regard to values, sinks and removals)
         * @param phase the current phase (with regard to values)
         */
        public static void InitAllRailsVertices(NetworkGraph graph,
                PublicCompany company, Phase phase)
        {

            // store vertices for removal
            List<NetworkVertex> verticesToRemove = new List<NetworkVertex>();
            foreach (NetworkVertex v in graph.Graph.Vertices)
            {
                if (company != null)
                {
                    if (!v.InitRailsVertex(company))
                    {
                        verticesToRemove.Add(v);
                    }
                }
                if (phase != null)
                {
                    v.SetRailsVertexValue(phase);
                }
            }
            graph.Graph.RemoveVertices(verticesToRemove);
        }

        /**
         * Returns the maximum positive value (lower bound zero)
         */
        public static int MaxVertexValue(IEnumerable<NetworkVertex> vertices)
        {
            int maximum = 0;
            foreach (NetworkVertex vertex in vertices)
            {
                maximum = Math.Max(maximum, vertex.Value);
            }
            return maximum;
        }


        /**
         * Return the sum of vertex values
         */
        public static int Sum(IEnumerable<NetworkVertex> vertices)
        {
            int sum = 0;
            foreach (NetworkVertex vertex in vertices)
            {
                sum += vertex.Value;
            }
            return sum;
        }

        /**
         * Returns the number of specified vertex type in a vertex collection
         * If station then specify station type
         */
        public static int NumberOfVertexType(IEnumerable<NetworkVertex> vertices, VertexTypes vertexType, StationTypes stationType)
        {
            int number = 0;
            foreach (NetworkVertex vertex in vertices)
            {
                if (vertex.IsOfType(vertexType, stationType)) number++;
            }
            return number;
        }

        /**
         * creates a new virtualVertex vertex with identical properties and links
         */
        public static NetworkVertex DuplicateVertex(SimpleGraph<NetworkVertex, NetworkEdge> graph,
                NetworkVertex vertex, string newIdentifier, bool addOldVertexAsHidden)
        {
            // create new vertex
            NetworkVertex newVertex = NetworkVertex.GetVirtualVertex(vertex.type, newIdentifier);
            // copy values
            newVertex.stationType = vertex.stationType;
            newVertex.value = vertex.value;
            newVertex.sink = vertex.sink;
            newVertex.stopName = vertex.stopName;
            graph.AddVertex(newVertex);
            // copy edges
            //HashSet<NetworkEdge> edges = graph.EdgesOf(vertex);
            var edges = graph.EdgesOf(vertex);
            foreach (NetworkEdge edge in edges)
            {
                List<NetworkVertex> hiddenVertices;
                if (edge.Source == vertex)
                {
                    hiddenVertices = edge.HiddenVertices;
                    if (addOldVertexAsHidden) hiddenVertices.Add(vertex);
                    NetworkEdge newEdge = new NetworkEdge(newVertex, edge.Target, edge.IsGreedy, edge.Distance, hiddenVertices);
                    graph.AddEdge(newVertex, edge.Target, newEdge);
                }
                else
                {
                    hiddenVertices = new List<NetworkVertex>();
                    if (addOldVertexAsHidden) hiddenVertices.Add(vertex);
                    hiddenVertices.AddRange(edge.HiddenVertices);
                    NetworkEdge newEdge = new NetworkEdge(edge.Source, newVertex, edge.IsGreedy, edge.Distance, hiddenVertices);
                    graph.AddEdge(newEdge.Source, newVertex, newEdge);
                }
            }
            return newVertex;
        }

        /**
         * replaces one vertex by another for a network graph
         * copies all edges
         */
        public static bool ReplaceVertex(SimpleGraph<NetworkVertex, NetworkEdge> graph,
                NetworkVertex oldVertex, NetworkVertex newVertex)
        {
            // add new vertex
            graph.AddVertex(newVertex);
            // replace old edges
            //Set<NetworkEdge> oldEdges = graph.edgesOf(oldVertex);
            var oldEdges = graph.EdgesOf(oldVertex);
            foreach (NetworkEdge oldEdge in oldEdges)
            {
                NetworkEdge newEdge = NetworkEdge.ReplaceVertex(oldEdge, oldVertex, newVertex);
                if (newEdge.Source == newVertex)
                {
                    graph.AddEdge(newVertex, newEdge.Target, newEdge);
                }
                else
                {
                    graph.AddEdge(newEdge.Source, newVertex, newEdge);
                }
            }
            // remove old vertex
            return graph.RemoveVertex(oldVertex);
        }

        /**
         * Filters all vertices from a collection of vertices that lay in a specified collection of hexes
         */
        public static HashSet<NetworkVertex> GetVerticesByHexes(IEnumerable<NetworkVertex> vertices, ICollection<MapHex> hexes)
        {
            HashSet<NetworkVertex> hexVertices = new HashSet<NetworkVertex>();
            foreach (NetworkVertex vertex in vertices)
            {
                if (vertex.Hex != null && hexes.Contains(vertex.Hex))
                {
                    hexVertices.Add(vertex);
                }
            }
            return hexVertices;
        }

        /**
         * Returns all vertices for a specified hex
         */
        public static HashSet<NetworkVertex> GetVerticesByHex(IEnumerable<NetworkVertex> vertices, MapHex hex)
        {
            HashSet<NetworkVertex> hexVertices = new HashSet<NetworkVertex>();
            foreach (NetworkVertex vertex in vertices)
            {
                if (vertex.Hex != null && hex == vertex.Hex)
                {
                    hexVertices.Add(vertex);
                }
            }
            return hexVertices;
        }

        public static NetworkVertex GetVertexByIdentifier(IEnumerable<NetworkVertex> vertices, string identifier)
        {
            foreach (NetworkVertex vertex in vertices)
            {
                if (vertex.Identifier.Equals(identifier))
                {
                    return vertex;
                }
            }
            return null;
        }

        // #FIXME_GUI
        public static PointF? GetVertexPoint2D(HexMap map, NetworkVertex vertex)
        {
            if (vertex.IsVirtual) return default(PointF);//null;

            GUIHex guiHex = map.GetHex(vertex.Hex);
            if (vertex.IsMajor)
            {
                return guiHex.GetStopPoint2D(vertex.Stop);
            }
            else if (vertex.IsMinor)
            {
                return guiHex.GetStopPoint2D(vertex.Stop);
                //            return guiHex.getCenterPoint2D();
            }
            else if (vertex.IsSide)
            {
                // FIXME: Check if this still works
                return guiHex.GetSidePoint2D(vertex.Side);
            }
            else
            {
                return null;
            }
        }

        public static Rectangle? GetVertexMapCoverage(HexMap map, IEnumerable<NetworkVertex> vertices)
        {

            Rectangle? rectangle = null;

            // find coverage are of the vertices
            float minX = 0, minY = 0, maxX = 0, maxY = 0;
            foreach (NetworkVertex vertex in vertices)
            {
                PointF? point_nullable = GetVertexPoint2D(map, vertex);
                if (point_nullable != null)
                {
                    PointF point = (PointF)point_nullable;
                    if (minX == 0)
                    { // init
                      //rectangle = new Rectangle((int)point.X, (int)point.Y, 0, 0);
                        minX = point.X;
                        minY = point.Y;
                        maxX = minX; maxY = minY;
                    }
                    else
                    {
                        //rectangle.Add(point);
                        minX = Math.Min(minX, point.X);
                        minY = Math.Min(minY, point.Y);
                        maxX = Math.Max(maxX, point.X);
                        maxY = Math.Max(maxY, point.Y);
                    }
                }
            }
            rectangle = new Rectangle((int)minX, (int)minY, (int)(maxX - minX), (int)(maxY - minY));
            log.Info("Vertex Map Coverage minX=" + minX + ", minY=" + minY + ", maxX=" + maxX + ", maxY=" + maxY);
            //        Rectangle rectangle = new Rectangle((int)minX, (int)minY, (int)maxX, (int)maxY);
            log.Info("Created rectangle=" + rectangle);
            return (rectangle);
        }
    }
}
