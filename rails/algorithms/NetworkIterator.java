package rails.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.jgrapht.*;
import org.jgrapht.traverse.*;

import rails.game.PublicCompanyI;

public class NetworkIterator extends 
    AbstractGraphIterator<NetworkVertex, NetworkEdge> {

    public static enum greedyState {
        seen,
        nonGreedy,
        greedy,
        done
    }
    
    // settings
    private NetworkVertex startVertex;
    private boolean startVertexVisited;
    private boolean routeIterator;

    // internal data
    private List<NetworkVertex> stack = new ArrayList<NetworkVertex>();
    private List<Boolean> greedyStack = new ArrayList<Boolean>();
    private Map<NetworkVertex, greedyState> seen = new HashMap<NetworkVertex, greedyState>();
    

    private final Graph<NetworkVertex, NetworkEdge> graph;

    protected static Logger log =
        Logger.getLogger(NetworkIterator.class.getPackage().getName());

    
    public NetworkIterator(Graph<NetworkVertex, NetworkEdge> graph,
            NetworkVertex startVertex) {
        this(graph, startVertex, null);
    }

    /**
     * Returns NetworkIterator for specific company
     */
    public NetworkIterator(Graph<NetworkVertex, NetworkEdge> graph, NetworkVertex startVertex,
            PublicCompanyI company) {
        super();
        
        if (graph == null)
            throw new IllegalArgumentException("graph must not be null");
        
        if (!graph.containsVertex(startVertex))
            throw new IllegalArgumentException("graph must contain the start vertex");

        this.graph = graph;
        this.startVertex = startVertex;
        this.startVertexVisited = false;
        this.routeIterator = false;
    }
    
    NetworkIterator setRouteIterator(boolean routeIterator) {
        this.routeIterator = routeIterator;
        return this;
    }
    
    /**
     * @return the graph being traversed
     */
    public Graph<NetworkVertex, NetworkEdge> getGraph()
    {
        return graph;
    }

    public List<NetworkVertex> getCurrentRoute() {
        // extract all networkvertices just before a null
        List<NetworkVertex> route = new ArrayList<NetworkVertex>();
        NetworkVertex previousVertex = null;
        for (NetworkVertex vertex:stack) {
            if (previousVertex != null && vertex == null) {
                route.add(previousVertex);
            }
            previousVertex = vertex;
        }
        return route;
    }
    
    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        if (!startVertexVisited) {
            encounterStartVertex();
        }
        
        int i = stack.size() - 1;
        while (i >= 0) {
            if (stack.get(i) != null) 
                break;
            else
                i = i - 2;
        }
        return i >=0; 
    }

    /**
     * @see java.util.Iterator#next()
     */
    public NetworkVertex next()
    {
        if (!startVertexVisited) {
            encounterStartVertex();
        }

        if (hasNext()) {
            NetworkVertex nextVertex;
            while (true) {
                 nextVertex = stack.remove(stack.size() - 1);
                if (nextVertex != null)
                    break;
                stack.remove(stack.size() - 1);
            }

            log.debug("Iterator: provides next vertex" + nextVertex);
            boolean nextGreedy = greedyStack.remove(greedyStack.size() - 1);

            putSeenData(nextVertex, nextGreedy);
            stack.add(nextVertex);
            stack.add(null); // add sentinel that we know when we are ready
            
            addUnseenChildrenOf(nextVertex, nextGreedy);

            return nextVertex;
        } else {
            throw new NoSuchElementException();
        }
    }

    
    /**
     * Stores iterator-dependent data for a vertex that has been seen.
     *
     * @param vertex a vertex which has been seen.
     * @param data data to be associated with the seen vertex.
     *
     * @return previous value associated with specified vertex or <code>
     * null</code> if no data was associated with the vertex. A <code>
     * null</code> return can also indicate that the vertex was explicitly
     * associated with <code>null</code>.
     */
    private void putSeenData(NetworkVertex vertex, boolean greedy)
    {
        if (!vertex.isSide()) {
            seen.put(vertex, greedyState.seen);
            log.debug("Iterator:  Vertex " + vertex + " seen with greedyState = seen");
            return;
        }
        // side
        if (seen.containsKey(vertex)){
            seen.put(vertex, greedyState.done);
            log.debug("Iterator:  Vertex " + vertex + " seen with greedyState = done");
        } else if (greedy) {
            seen.put(vertex, greedyState.greedy);
            log.debug("Iterator:  Vertex " + vertex + " seen with greedyState = greedy");
        } else {
            seen.put(vertex, greedyState.nonGreedy);
            log.debug("Iterator:  Vertex " + vertex + " seen with greedyState = nonGreedy");
        }
    }
    
    private void addUnseenChildrenOf(NetworkVertex vertex, boolean greedy) {

        if (vertex.isSink()) return;
        log.debug("Iterator: Add unseen children of " + vertex);

        for (NetworkEdge edge : graph.edgesOf(vertex)) {
            log.debug("Iterator: Check edge for neighbor in edge " + edge.toFullInfoString());
            if (!greedy || edge.isGreedy()) {
                NetworkVertex oppositeV = Graphs.getOppositeVertex(graph, edge, vertex);
                log.debug("Iterator: Neighbor is " + oppositeV);
                encounterVertex(oppositeV, edge);
            }
        }
    }
    
    private void encounterStartVertex() {
        putSeenData(startVertex, false);
        stack.add(startVertex);
        greedyStack.add(false);
        log.debug("Iterator: Added to stack " + startVertex +  " with greedy set to false");
        startVertexVisited = true;
    }


    private void encounterVertex(NetworkVertex v, NetworkEdge e){

        if (routeIterator) {
//            if (v == startVertex) return;
            // check the stack
            if (getCurrentRoute().contains(v))
                return;
            // check the seen components 
//            if (seen.containsKey(v) && (seen.get(v) == greedyState.seen && !v.isSink() 
//                    || seen.get(v) == greedyState.done || (e.isGreedy() && seen.get(v) == greedyState.nonGreedy)
//                    || (!e.isGreedy() && seen.get(v) == greedyState.greedy) )) {
//                log.debug("Do not add vertex " + v  + " to stack");
//                return;
//            }
        } else {
            if (stack.contains(v)) return;
            if (v.isSide() && seen.containsKey(v) && (seen.get(v) == greedyState.done || (e.isGreedy() && seen.get(v) == greedyState.nonGreedy)
                    || (!e.isGreedy() && seen.get(v) == greedyState.greedy) )) {
                log.debug("Leave vertex " + v + " due to greedState rules");
                return;
            }
        }
        stack.add(v);
        greedyStack.add(v.isSide() && !e.isGreedy());
        log.debug("Iterator: Added to stack " + v +  " with greedy set to " + (v.isSide() && !e.isGreedy()));
    }

}
