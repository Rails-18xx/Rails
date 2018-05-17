using GameLib.Net.Common;
using GameLib.Net.Graph;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Text;

namespace GameLib.Net.Algorithms
{
    public class NetworkEdge : IComparable<NetworkEdge>
    {
        protected static Logger<NetworkEdge> log = new Logger<NetworkEdge>();

        private NetworkVertex source;

        private NetworkVertex target;

        private bool greedy;

        private int distance;

        private List<NetworkVertex> hiddenVertices;
        // list of vertexes that were merged into the edge

        private int routeCosts;
        // for the multigraph approach defines the number of routes excluded

        public NetworkEdge(NetworkVertex source, NetworkVertex target, bool greedy)
        {
            this.source = source;
            this.target = target;
            this.greedy = greedy;
            if (greedy)
                this.distance = 1;
            else
                this.distance = 0;
            hiddenVertices = new List<NetworkVertex>();
        }

        public NetworkEdge(NetworkVertex source, NetworkVertex target, bool greedy,
                    int distance, List<NetworkVertex> hiddenVertexes)
        {
            this.source = source;
            this.target = target;
            this.greedy = greedy;
            this.distance = distance;
            this.hiddenVertices = hiddenVertexes;
        }

        public NetworkVertex Source
        {
            get
            {
                return source;
            }
        }

        public NetworkVertex Target
        {
            get
            {
                return target;
            }
        }

        private NetworkVertex LowVertex
        {
            get
            {
                if (source.CompareTo(target) <= 0)
                {
                    return source;
                }
                else
                {
                    return target;
                }
            }
        }

        private NetworkVertex HighVertex
        {
            get
            {
                if (source.CompareTo(target) <= 0)
                {
                    return target;
                }
                else
                {
                    return source;
                }
            }
        }

        /** returns the other vertex, if given vertex is not source/target of vertex, returns null */
        public NetworkVertex GetOtherVertex(NetworkVertex vertex)
        {
            if (this.source == vertex)
            {
                return target;
            }
            else if (this.target == vertex)
            {
                return source;
            }
            return null;
        }

        /** gets common vertex, if both source and target are common, returns source of this edge */
        public NetworkVertex GetCommonVertex(NetworkEdge otherEdge)
        {
            if (this.source == otherEdge.source || this.source == otherEdge.target)
            {
                return this.source;
            }
            else if (this.target == otherEdge.source || this.target == otherEdge.target)
            {
                return this.target;
            }
            return null;
        }

        public bool IsGreedy
        {
            get
            {
                return greedy;
            }
            set
            {
                greedy = value;
            }
        }

        public int Distance
        {
            get
            {
                return distance;
            }
        }

        public int RouteCosts
        {
            get
            {
                return routeCosts;
            }
            set
            {
                routeCosts = value;
            }
        }

        public List<NetworkVertex> HiddenVertices
        {
            get
            {
                return hiddenVertices;
            }
        }

        /**
         * all vertices from source to target, including hidden vertices
         */
        public List<NetworkVertex> GetVertexPath()
        {
            List<NetworkVertex> vertexPath = new List<NetworkVertex>();
            vertexPath.Add(source);
            vertexPath.AddRange(hiddenVertices);
            vertexPath.Add(target);
            return vertexPath;
        }

        public string ToFullInfoString()
        {
            StringBuilder info = new StringBuilder();
            info.Append("Edge " + GetConnection());
            info.Append(", greedy = " + greedy);
            info.Append(", distance = " + distance);
            info.Append(", hidden vertexes = " + hiddenVertices);
            return info.ToString();
        }

        public string GetOrderedConnection()
        {
            return LowVertex + " -> " + HighVertex;
        }

        public string GetConnection()
        {
            return source + " -> " + target;
        }

        // set to "" to facilitate visual graph
        override public string ToString()
        {
            return GetConnection();
            //        if (!greedy)
            //            return "*** / " + distance;
            //        else
            //          return "" + distance;
        }

        /** 
         * Natural order based on the ordering of the connected vertices
         */
        public int CompareTo(NetworkEdge otherEdge)
        {
            int result = LowVertex.CompareTo(otherEdge.LowVertex);
            if (result == 0)
            {
                result = this.HighVertex.CompareTo(otherEdge.HighVertex);
            }
            return result;
        }

        /**
         * Ordering based on route costs
         */
        public class CostOrder : IComparer<NetworkEdge>
        {
            public int Compare(NetworkEdge edgeA, NetworkEdge edgeB)
            {
                int result = edgeA.RouteCosts.CompareTo(edgeB.RouteCosts);
                if (result == 0)
                    result = edgeA.CompareTo(edgeB); // otherwise use natural ordering
                return result;
            }
        }


        public class MergeResult
        {
            public NetworkEdge newEdge;
            public NetworkVertex removedVertex;
            public MergeResult(NetworkEdge newEdge, NetworkVertex removedVertex)
            {
                this.newEdge = newEdge;
                this.removedVertex = removedVertex;
            }
        }

        public static MergeResult MergeEdges(NetworkEdge edgeA, NetworkEdge edgeB)
        {
            log.Info("Merge of edge " + edgeA.ToFullInfoString() + " and edge " + edgeB.ToFullInfoString());

            NetworkVertex sourceA = edgeA.Source;
            NetworkVertex targetA = edgeA.Target;
            NetworkVertex sourceB = edgeB.Source;
            NetworkVertex targetB = edgeB.Target;

            NetworkVertex newSource, newTarget, vertex = null;

            bool reverseA = false, reverseB = false;
            if (sourceA == sourceB)
            {
                newSource = targetA;
                newTarget = targetB;
                vertex = sourceA;
                reverseA = true;
            }
            else if (sourceA == targetB)
            {
                newSource = targetA;
                newTarget = sourceB;
                vertex = sourceA;
                reverseA = true;
                reverseB = true;
            }
            else if (targetA == sourceB)
            {
                newSource = sourceA;
                newTarget = targetB;
                vertex = targetA;
            }
            else if (targetA == targetB)
            {
                newSource = sourceA;
                newTarget = sourceB;
                vertex = targetA;
                reverseB = true;
            }
            else
            {
                return null;
            }

            log.Info("Merge newSource = " + newSource + " newTarget = " + newTarget + " remove vertex = " + vertex);

            // define new edge
            int distance = edgeA.Distance + edgeB.Distance;

            // create new hiddenVertexes
            List<NetworkVertex> hiddenVertexes = new List<NetworkVertex>();
            List<NetworkVertex> hiddenA = edgeA.HiddenVertices;
            if (reverseA)
            {
                hiddenA = new List<NetworkVertex>(hiddenA); // clone
                hiddenA.Reverse();
            }
            List<NetworkVertex> hiddenB = edgeB.HiddenVertices;
            if (reverseB)
            {
                hiddenB = new List<NetworkVertex>(hiddenB); // clone
                hiddenB.Reverse();
            }
            hiddenVertexes.AddRange(hiddenA);
            hiddenVertexes.Add(vertex);
            hiddenVertexes.AddRange(hiddenB);
            NetworkEdge newEdge = new NetworkEdge(newSource, newTarget, true, distance, hiddenVertexes);
            log.Info("New edge = " + newEdge.ToFullInfoString());

            // returns newEdge
            return new MergeResult(newEdge, vertex);
        }

        public static bool MergeEdgesInGraph(IGraph<NetworkVertex, NetworkEdge> graph,
                NetworkEdge edgeA, NetworkEdge edgeB)
        {
            // use generic merge function
            MergeResult mergeResult = MergeEdges(edgeA, edgeB);
            NetworkEdge newEdge = mergeResult.newEdge;
            NetworkVertex removedVertex = mergeResult.removedVertex;

            if (newEdge == null) return false;

            // check if graph contains the edge already
            if (graph.ContainsEdge(newEdge.Source, newEdge.Target)) return false;

            graph.AddEdge(newEdge.Source, newEdge.Target, newEdge);

            log.Info("New edge =  " + newEdge.ToFullInfoString());

            // remove vertex
            graph.RemoveVertex(removedVertex);

            return true;
        }

        /**
         * for a given edge it replaces one of the vertices by a different one
         * otherwise copies all edge attributes
         * @return copied edge with replaced vertex, null if oldVertex is neither source, nor target
         */
        public static NetworkEdge ReplaceVertex(NetworkEdge edge, NetworkVertex oldVertex, NetworkVertex newVertex)
        {
            NetworkEdge newEdge;
            if (edge.source == oldVertex)
            {
                newEdge = new NetworkEdge(newVertex, edge.target, edge.greedy, edge.distance, edge.hiddenVertices);
            }
            else if (edge.target == oldVertex)
            {
                newEdge = new NetworkEdge(edge.source, newVertex, edge.greedy, edge.distance, edge.hiddenVertices);
            }
            else
            {
                newEdge = null;
            }
            return newEdge;
        }

        // #FIXME_GUI
        public static Shape GetEdgeShape(HexMap map, NetworkEdge edge)
        {
            PointF source = (PointF)NetworkVertex.GetVertexPoint2D(map, edge.Source);
            PointF target = (PointF)NetworkVertex.GetVertexPoint2D(map, edge.Target);
            Shape edgeShape;

            if (source != null && target != null)
            {
                edgeShape = new Line2D(source, target);
            }
            else
            {
                edgeShape = null;
            }
            return edgeShape;
        }
    }
}
