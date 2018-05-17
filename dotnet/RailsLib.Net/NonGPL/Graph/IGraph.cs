// This source file is licensed under the MIT license
// Author: jim.wuerch@outlook.com

using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Graph
{
    // T - vertex
    // TEdge - edge
    public interface IGraph<TVertex, TEdge>
    {
        // get all edges from src to dst
        ICollection<TEdge> GetAllEdges(TVertex src, TVertex dst);

        // get a single edge
        TEdge GetEdge(TVertex src, TVertex dst);

        //TEdge AddEdge(TVertex src, TVertex dst);

        bool AddEdge(TVertex src, TVertex dst, TEdge edge);

        // returns true if added
        bool AddVertex(TVertex vertex);

        bool ContainsEdge(TVertex src, TVertex dst);

        bool ContainsEdge(TEdge edge);

        bool ContainsVertex(TVertex vertex);

        ICollection<TEdge> Edges { get; }

        // get edges touching a vertex
        ICollection<TEdge> EdgesOf(TVertex vertex);

        // remove edges in src list
        // returns true if graph changed
        bool RemoveEdges(IEnumerable<TEdge> edges);

        // remove edges going from src to dst
        // returns true if graph changed
        bool RemoveEdges(TVertex src, TVertex dst);

        // remove vertices in list
        // returns true if the graph changed
        bool RemoveVertices(IEnumerable<TVertex> vertices);

        TEdge RemoveEdge(TVertex src, TVertex dst);

        bool RemoveEdge(TEdge edge);

        // removes a vertex and all edges touching it
        bool RemoveVertex(TVertex vertex);

        ICollection<TVertex> Vertices { get; }

        TVertex GetOppositeVertex(TVertex vertex, TEdge edge);

        bool AddGraph(IGraph<TVertex, TEdge> src);

        IGraph<TVertex, TEdge> CreateSubgraph(IEnumerable<TVertex> verticies);

        bool AddVertices(IEnumerable<TVertex> vertices);

        ICollection<TVertex> NeighborsOf(TVertex v);
    }
}
