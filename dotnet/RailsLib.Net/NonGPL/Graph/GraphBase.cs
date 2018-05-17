// This source file is licensed under the MIT license
// Author: jim.wuerch@outlook.com

using GameLib.Net.Common;
using System;
using System.Collections.Generic;
using System.Text;
using Wintellect.PowerCollections;

namespace GameLib.Net.Graph
{
    public class GraphBase<TVertex, TEdge> : IGraph<TVertex, TEdge>
        where TVertex : IEquatable<TVertex>
    {
        private static Logger<GraphBase<TVertex, TEdge>> log = new Logger<GraphBase<TVertex, TEdge>>();

        private HashSet<TVertex> vertices;
        //private HashSet<TEdge> edges;
        //private MultiDictionary<TVertex, TEdge> edgesByVertex;
        //private MultiDictionary<TVertex, TVertex> neighbors;
        private Dictionary<TEdge, Tuple<TVertex, TVertex>> edges;

        public GraphBase(bool multigraph, bool allowLoops, bool directed)
        {
            IsMulti = multigraph;
            AllowLoops = allowLoops;
            IsDirected = directed;

            vertices = new HashSet<TVertex>();
            edges = new Dictionary<TEdge, Tuple<TVertex, TVertex>>(); //new HashSet<TEdge>();
            //edgesByVertex = new MultiDictionary<TVertex, TEdge>(multigraph);
            //neighbors = new MultiDictionary<TVertex, TVertex>(false);
        }

        public bool IsMulti { get; private set; }
        public bool AllowLoops { get; private set; }
        public bool IsDirected { get; private set; }

        public ICollection<TEdge> Edges
        {
            get
            {
                return edges.Keys;
            }
        }

        public ICollection<TVertex> Vertices
        {
            get
            {
                return vertices;
            }
        }

        public bool AddEdge(TVertex src, TVertex dst, TEdge edge)
        {
            if (src == null || dst == null || edge == null)
            {
                throw new ArgumentException("Arguments can't be null");
            }

            if (ContainsEdge(edge)) return false;

            if (!AllowLoops && src.Equals(dst))
            {
                log.Debug("Skipping adding edge because no loops allowed");
                return false;
            }

            if (!IsMulti && ContainsEdge(src, dst))
            {
                log.Debug("Skipping adding edge because graph is not multi and edge exists");
                return false;
            }

            edges.Add(edge, new Tuple<TVertex, TVertex>(src, dst));
            return true;
        }

        public bool AddGraph(IGraph<TVertex, TEdge> src)
        {
            if (src == null)
            {
                throw new ArgumentException("Argument is null");
            }

            bool bChanged = false;

            GraphBase<TVertex, TEdge> srcGraph = src as GraphBase<TVertex, TEdge>;
            if (srcGraph == null)
            {
                throw new ArgumentException("Source graph is not a GraphBase type");
            }

            foreach (var v in srcGraph.vertices)
            {
                if (AddVertex(v))
                    bChanged = true;
            }

            foreach (var edge in srcGraph.edges)
            {
                if (AddEdge(edge.Value.Item1, edge.Value.Item2, edge.Key))
                {
                    bChanged = true;
                }
            }

            return bChanged;
        }

        public bool AddVertex(TVertex vertex)
        {
            if (vertex == null)
            {
                throw new ArgumentException("Can't add null vertex");
            }

            if (ContainsVertex(vertex))
            {
                log.Debug("Skipping adding duplicate vertex");
                throw new ArgumentException("Can't add duplicate vertex");
            }

            vertices.Add(vertex);

            return true;
        }

        public bool AddVertices(IEnumerable<TVertex> vertices)
        {
            if (vertices == null)
            {
                throw new ArgumentException("Argument is null");
            }

            bool bChanged = false;

            foreach (var v in vertices)
            {
                if (AddVertex(v))
                {
                    bChanged = true;
                }
            }

            return bChanged;
        }

        public bool ContainsEdge(TVertex src, TVertex dst)
        {
            if (src == null || dst == null)
            {
                throw new ArgumentException("Argument is null");
            }

            foreach (var edge in edges)
            {
                // check one direction
                if (edge.Value.Item1.Equals(src) && edge.Value.Item2.Equals(dst))
                {
                    return true;
                }
                // check the other direction
                if (!IsDirected)
                {
                    if (edge.Value.Item1.Equals(dst) && edge.Value.Item2.Equals(src))
                    {
                        return true;
                    }
                }
            }

            return false;
        }

        public bool ContainsEdge(TEdge edge)
        {
            if (edge == null)
            {
                throw new ArgumentException("Argument is null");
            }

            return edges.ContainsKey(edge);
        }

        public bool ContainsVertex(TVertex vertex)
        {
            if (vertex == null)
            {
                throw new ArgumentException("Argument is null");
            }

            return vertices.Contains(vertex);
        }

        public IGraph<TVertex, TEdge> CreateSubgraph(IEnumerable<TVertex> verticies)
        {
            if (vertices == null)
            {
                throw new ArgumentException("Argument is null");
            }

            GraphBase<TVertex, TEdge> graph = new GraphBase<TVertex, TEdge>(IsMulti, AllowLoops, IsDirected);

            foreach (var v in vertices)
            {
                graph.AddVertex(v);
            }

            foreach (var edge in edges)
            {
                if (graph.ContainsVertex(edge.Value.Item1) && graph.ContainsVertex(edge.Value.Item2))
                {
                    graph.AddEdge(edge.Value.Item1, edge.Value.Item2, edge.Key);
                }
            }

            return graph;
        }

        public ICollection<TEdge> EdgesOf(TVertex vertex)
        {
            if (vertex == null)
            {
                throw new ArgumentException("Null vertex not allowed");
            }

            List<TEdge> ret = new List<TEdge>();
            foreach (var edge in edges)
            {
                if (edge.Value.Item1.Equals(vertex) || edge.Value.Item2.Equals(vertex))
                {
                    ret.Add(edge.Key);
                }
            }

            return ret;
        }

        public ICollection<TEdge> GetAllEdges(TVertex src, TVertex dst)
        {
            if (src == null || dst == null)
            {
                throw new ArgumentException("Null vertex not allowed");
            }

            List<TEdge> ret = new List<TEdge>();
            foreach (var edge in edges)
            {
                if (edge.Value.Item1.Equals(src) && edge.Value.Item2.Equals(dst))
                {
                    ret.Add(edge.Key);
                }
                else if (!IsDirected && edge.Value.Item1.Equals(dst) && edge.Value.Item2.Equals(src))
                {
                    ret.Add(edge.Key);
                }
            }

            return ret;
        }

        public TEdge GetEdge(TVertex src, TVertex dst)
        {
            if (src == null || dst == null)
            {
                throw new ArgumentException("Argument is null");
            }

            foreach (var edge in edges)
            {
                if (edge.Value.Item1.Equals(src) && edge.Value.Item2.Equals(dst))
                {
                    return edge.Key;
                }
                else if (!IsDirected && edge.Value.Item1.Equals(dst) && edge.Value.Item2.Equals(src))
                {
                    return edge.Key;
                }
            }

            return default(TEdge);
        }

        public TVertex GetOppositeVertex(TVertex vertex, TEdge edge)
        {
            if (vertex == null || edge == null)
            {
                throw new ArgumentException("Null argument passed");
            }

            if (!ContainsEdge(edge))
            {
                throw new ArgumentException("No such edge");
            }

            if (!ContainsVertex(vertex))
            {
                throw new ArgumentException("No such vertex");
            }

            var e = edges[edge];
            if (e.Item1.Equals(vertex))
            {
                return e.Item2;
            }
            if (e.Item2.Equals(vertex))
            {
                return e.Item1;
            }

            throw new ArgumentException($"No such vertex {vertex.ToString()}");
        }

        public ICollection<TVertex> NeighborsOf(TVertex v)
        {
            if (v == null)
            {
                throw new ArgumentException("Argument is null");
            }

            List<TVertex> ret = new List<TVertex>();

            foreach (var edge in edges)
            {
                if (edge.Value.Item1.Equals(v))
                {
                    ret.Add(edge.Value.Item2);
                }
                if (!IsDirected && edge.Value.Item2.Equals(v))
                {
                    ret.Add(edge.Value.Item1);
                }
            }

            return ret;
        }

        public TEdge RemoveEdge(TVertex src, TVertex dst)
        {
            if (src == null || dst == null)
            {
                throw new ArgumentException("Argument is null");
            }

            var edge = GetEdge(src, dst);
            if (edge == null)
            {
                throw new ArgumentException("Edge doesn't exist");
            }

            edges.Remove(edge);

            return edge;
        }

        public bool RemoveEdge(TEdge edge)
        {
            if (edge == null)
            {
                throw new ArgumentException("Edge is null");
            }

            if (!edges.ContainsKey(edge))
            {
                return false;
            }

            edges.Remove(edge);
            return true;
        }

        public bool RemoveEdges(IEnumerable<TEdge> edges)
        {
            bool bChanged = false;

            if (edges == null)
            {
                throw new ArgumentException("Edges is null");
            }

            foreach (var edge in edges)
            {
                if (RemoveEdge(edge))
                    bChanged = true;
            }

            return bChanged;
        }

        public bool RemoveEdges(TVertex src, TVertex dst)
        {
            if (src == null || dst == null)
            {
                throw new ArgumentException("Argument is null");
            }

            var e = GetAllEdges(src, dst);

            bool bChanged = false;
            foreach (var edge in e)
            {
                if (RemoveEdge(edge))
                {
                    bChanged = true;
                }
            }

            return bChanged;
        }

        public bool RemoveVertex(TVertex vertex)
        {
            if (vertex == null)
            {
                throw new ArgumentException("Vertex is null");
            }

            if (!vertices.Contains(vertex))
            {
                throw new ArgumentException("No such vertex");
            }

            var e = EdgesOf(vertex);
            foreach (var edge in e)
            {
                RemoveEdge(edge);
            }

            vertices.Remove(vertex);
            return true;
        }

        public bool RemoveVertices(IEnumerable<TVertex> vertices)
        {
            if (vertices == null)
            {
                throw new ArgumentException("Argument is null");
            }

            bool bChanged = false;
            foreach (var v in vertices)
            {
                bChanged = true;
                RemoveVertex(v);
            }

            return bChanged;
        }
    }
}
