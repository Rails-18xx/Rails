using GameLib.Net.Common;
using GameLib.Net.Game;
using GameLib.Net.Graph;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Algorithms
{
    public class NetworkIterator : GraphEnumerator<NetworkVertex, NetworkEdge>//, IEnumerator<NetworkVertex>
    {
        public enum GreedyState
        {
            seen,
            nonGreedy,
            greedy,
            done
        }

        // settings
        private NetworkVertex startVertex;
        private bool startVertexVisited;
        private bool routeIterator;

        // internal data
        private List<NetworkVertex> stack = new List<NetworkVertex>();
        private List<bool> greedyStack = new List<bool>();
        private Dictionary<NetworkVertex, GreedyState> seen = new Dictionary<NetworkVertex, GreedyState>();


        private IGraph<NetworkVertex, NetworkEdge> graph;

        protected static Logger<NetworkIterator> log = new Logger<NetworkIterator>();

    
    public NetworkIterator(IGraph<NetworkVertex, NetworkEdge> graph,
            NetworkVertex startVertex) : this(graph, startVertex, null)
        {
            
        }

        /**
         * Returns NetworkIterator for specific company
         */
        public NetworkIterator(IGraph<NetworkVertex, NetworkEdge> graph, NetworkVertex startVertex, PublicCompany company) : base()
        {
            if (graph == null)
                throw new ArgumentException("graph must not be null");

            if (!graph.ContainsVertex(startVertex))
                throw new ArgumentException("graph must contain the start vertex");

            this.graph = graph;
            this.startVertex = startVertex;
            this.startVertexVisited = false;
            this.routeIterator = false;
        }

        public NetworkIterator SetRouteIterator(bool routeIterator)
        {
            this.routeIterator = routeIterator;
            return this;
        }

        /**
         * @return the graph being traversed
         */
        public IGraph<NetworkVertex, NetworkEdge> Graph
        {
            get
            {
                return graph;
            }
        }

        public Dictionary<NetworkVertex, GreedyState> SeenData
        {
            get
            {
                return seen;
            }
        }

        public NetworkVertex Current { get; set; }

        public List<NetworkVertex> GetCurrentRoute()
        {
            // extract all networkvertices just before a null
            List<NetworkVertex> route = new List<NetworkVertex>();
            NetworkVertex previousVertex = null;
            foreach (NetworkVertex vertex in stack)
            {
                if (previousVertex != null && vertex == null)
                {
                    route.Add(previousVertex);
                }
                previousVertex = vertex;
            }
            return route;
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        private bool HasNext()
        {
            if (!startVertexVisited)
            {
                EncounterStartVertex();
            }

            int i = stack.Count - 1;
            while (i >= 0)
            {
                if (stack[i] != null)
                    break;
                else
                    i = i - 2;
            }
            return i >= 0;
        }

        /**
         * @see java.util.Iterator#next()
         */
        public /*NetworkVertex*/bool MoveNext()
        {
            if (!startVertexVisited)
            {
                EncounterStartVertex();
            }

            if (HasNext())
            {
                NetworkVertex nextVertex;
                while (true)
                {
                    nextVertex = stack[stack.Count - 1];
                    stack.RemoveAt(stack.Count - 1);

                    if (nextVertex != null)
                        break;
                    stack.RemoveAt(stack.Count - 1);
                }

                log.Debug("Iterator: provides next vertex" + nextVertex);
                bool nextGreedy = greedyStack[greedyStack.Count - 1];
                    greedyStack.RemoveAt(greedyStack.Count - 1);

                PutSeenData(nextVertex, nextGreedy);
                stack.Add(nextVertex);
                stack.Add(null); // add sentinel that we know when we are ready

                AddUnseenChildrenOf(nextVertex, nextGreedy);

                //return nextVertex;
                Current = nextVertex;
                return true;
            }
            else
            {
                //throw new NoSuchElementException();
                Current = null;
                return false;
            }
        }

        private void PutSeenData(NetworkVertex vertex, bool greedy)
        {
            if (!vertex.IsSide)
            {
                seen[vertex] = GreedyState.seen;
                log.Debug("Iterator:  Vertex " + vertex + " seen with greedyState = seen");
                return;
            }
            // side
            if (seen.ContainsKey(vertex))
            {
                seen[vertex] = GreedyState.done;
                log.Debug("Iterator:  Vertex " + vertex + " seen with greedyState = done");
            }
            else if (greedy)
            {
                seen[vertex] = GreedyState.greedy;
                log.Debug("Iterator:  Vertex " + vertex + " seen with greedyState = greedy");
            }
            else
            {
                seen[vertex] = GreedyState.nonGreedy;
                log.Debug("Iterator:  Vertex " + vertex + " seen with greedyState = nonGreedy");
            }
        }

        private void AddUnseenChildrenOf(NetworkVertex vertex, bool greedy)
        {
            if (vertex.IsSink) return;
            log.Debug("Iterator: Add unseen children of " + vertex);

            foreach (NetworkEdge edge in graph.EdgesOf(vertex))
            {
                log.Debug("Iterator: Check edge for neighbor in edge " + edge.ToFullInfoString());
                if (!greedy || edge.IsGreedy)
                {
                    NetworkVertex oppositeV = graph.GetOppositeVertex(vertex, edge);//Graphs.getOppositeVertex(graph, edge, vertex);
                    log.Debug("Iterator: Neighbor is " + oppositeV);
                    EncounterVertex(oppositeV, edge);
                }
            }
        }

        private void EncounterStartVertex()
        {
            PutSeenData(startVertex, false);
            stack.Add(startVertex);
            greedyStack.Add(false);
            log.Debug("Iterator: Added to stack " + startVertex + " with greedy set to false");
            startVertexVisited = true;
        }

        private void EncounterVertex(NetworkVertex v, NetworkEdge e)
        {
            if (routeIterator)
            {
                //            if (v == startVertex) return;
                // check the stack
                if (GetCurrentRoute().Contains(v))
                    return;
                // check the seen components 
                //            if (seen.containsKey(v) && (seen.get(v) == greedyState.seen && !v.isSink() 
                //                    || seen.get(v) == greedyState.done || (e.isGreedy() && seen.get(v) == greedyState.nonGreedy)
                //                    || (!e.isGreedy() && seen.get(v) == greedyState.greedy) )) {
                //                log.debug("Do not add vertex " + v  + " to stack");
                //                return;
                //            }
            }
            else
            {
                if (stack.Contains(v)) return;
                if (v.IsSide && seen.ContainsKey(v) && (seen[v] == GreedyState.done || (e.IsGreedy && seen[v] == GreedyState.nonGreedy)
                        || (!e.IsGreedy && seen[v] == GreedyState.greedy)))
                {
                    log.Debug("Leave vertex " + v + " due to greedState rules");
                    return;
                }
            }
            stack.Add(v);
            greedyStack.Add(v.IsSide && !e.IsGreedy);
            log.Debug("Iterator: Added to stack " + v + " with greedy set to " + (v.IsSide && !e.IsGreedy));
        }
    }
}
