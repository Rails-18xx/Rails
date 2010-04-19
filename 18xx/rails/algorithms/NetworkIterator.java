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

    /**
     * Standard vertex visit state enumeration.
     * Copied from CrossComponentIterator due to visibility for generic definition above
     */
    protected static enum VisitColor
    {
        /**
         * Vertex has not been returned via iterator yet.
         */
        WHITE,
        /** 
        * Vertex has been returned via iterator, but we're
        * not done with all of its out-edges yet.
        */
        GRAY,
        /**
         * Vertex has been returned via iterator, and we're
         * done with all of its out-edges.
         */
        BLACK
    }

    /**
     * Stores the vertices that have been seen during iteration and
     * some additional traversal info regarding each vertex.
     */
    private Map<NetworkVertex, VisitColor> seen = new HashMap<NetworkVertex, VisitColor>();
    private Map<NetworkVertex, Boolean> mustUseGreedy = new HashMap<NetworkVertex, Boolean>();
    private NetworkVertex startVertex;

    private final PublicCompanyI company;
    private final Graph<NetworkVertex, NetworkEdge> graph;
    
    /** LIFO stack for DFS */
    private List<NetworkVertex> stack = new ArrayList<NetworkVertex>();

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
        this.company = company;
    }
    
    
    /**
     * @return the graph being traversed
     */
    public Graph<NetworkVertex, NetworkEdge> getGraph()
    {
        return graph;
    }
    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        if (startVertex != null) {
            encounterStartVertex();
        }
        
        return !isConnectedComponentExhausted();
    }

    /**
     * @see java.util.Iterator#next()
     */
    public NetworkVertex next()
    {
        if (startVertex != null) {
            encounterStartVertex();
        }

        if (hasNext()) {

            NetworkVertex nextVertex = provideNextVertex();

            VisitColor previousColor = putSeenData(nextVertex , VisitColor.GRAY);
            
            addUnseenChildrenOf(nextVertex, previousColor);

            return nextVertex;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Access the data stored for a seen vertex.
     *
     * @param vertex a vertex which has already been seen.
     *
     * @return data associated with the seen vertex or <code>null</code> if no
     * data was associated with the vertex. A <code>null</code> return can also
     * indicate that the vertex was explicitly associated with <code>
     * null</code>.
     */
    private VisitColor getSeenData(NetworkVertex vertex) {
        return seen.get(vertex);
    }

    /**
     * Determines whether a vertex has been seen yet by this traversal.
     *
     * @param vertex vertex in question
     *
     * @return <tt>true</tt> if vertex has already been seen
     */
    private boolean isSeenVertex(NetworkVertex vertex, boolean mustUseGreedy)
    {
        return seen.containsKey(vertex) && (mustUseGreedy || !this.mustUseGreedy.get(vertex) );
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
    private VisitColor putSeenData(NetworkVertex vertex, VisitColor data)
    {
        return seen.put(vertex, data);
    }
    
    /**
     * Called when a vertex has been finished (meaning is dependent on traversal
     * represented by subclass).
     *
     * @param vertex vertex which has been finished
     */
    private void finishVertex(NetworkVertex vertex) {
        // do nothing
    }
    
    private void addUnseenChildrenOf(NetworkVertex vertex, VisitColor previousColor) {

        if (company != null && !vertex.canCompanyRunThrough(company)) return;
        
        for (NetworkEdge edge : graph.edgesOf(vertex)) {
            
            if (previousColor == VisitColor.WHITE || edge.isGreedy()) {  

                NetworkVertex oppositeV = Graphs.getOppositeVertex(graph, edge, vertex);
                if (isSeenVertex(oppositeV, vertex.isSide() && !edge.isGreedy() )) {
                    encounterVertexAgain(oppositeV, edge);
                } else {
                    encounterVertex(oppositeV, edge);
                }
            }
        }
    }
    
    private void encounterStartVertex() {
        putSeenData(startVertex, VisitColor.WHITE);
        stack.add(startVertex);
        startVertex = null;
        log.debug("Iterator: Added to stack " + startVertex);
    }

    /** copy of standard dfs */
    private void encounterVertex(NetworkVertex vertex, NetworkEdge edge) {
        putSeenData(vertex, VisitColor.WHITE);
        mustUseGreedy.put(vertex, vertex.isSide() && !edge.isGreedy());
        stack.add(vertex);
        log.debug("Iterator: Added to stack " + vertex);
    }

    /** copy of standard dfs */
    private void encounterVertexAgain(NetworkVertex vertex, NetworkEdge edge) {
        VisitColor color = getSeenData(vertex);
        if (color != VisitColor.WHITE) {
            // We've already visited this vertex; no need to mess with the
            // stack (either it's BLACK and not there at all, or it's GRAY
            // and therefore just a sentinel).
            return;
        }
        int i = stack.indexOf(vertex);

        // Since we've encountered it before, and it's still WHITE or YELLOW, it
        // *must* be on the stack.
        assert (i > -1);
        stack.remove(i);
        stack.add(vertex);
    }

    /** copy of standard dfs */
    private boolean isConnectedComponentExhausted() {
        while (true) {
            if (stack.isEmpty()) {
                return true;
            }
            if (peekStack() != null) {
                // Found a non-sentinel.
                return false;
            }

            // Found a sentinel:  pop it, record the finish time,
            // and then loop to check the rest of the stack.

            // Pop null we peeked at above.
            popStack();

            // This will pop corresponding vertex to be recorded as finished.
            recordFinish();
        }
    }

    /** copy of standard dfs */
    private NetworkVertex provideNextVertex() {
        NetworkVertex v;
        while (true) {
            v = popStack();
            if (v == null) {
                // This is a finish-time sentinel we previously pushed.
                recordFinish();
                // Now carry on with another pop until we find a non-sentinel
            } else {
                // Got a real vertex to start working on
                break;
            }
        }

        // Push a sentinel for v onto the stack so that we'll know
        // when we're done with it.
        stack.add(v);
        stack.add(null);
        return v;
    }

    private NetworkVertex popStack()
    {
        return stack.remove(stack.size() - 1);
    }

    private NetworkVertex peekStack()
    {
        return stack.get(stack.size() - 1);
    }

    private void recordFinish()
    {
        NetworkVertex v = popStack();
        if (getSeenData(v) == VisitColor.WHITE)
            putSeenData(v, VisitColor.BLACK);
        finishVertex(v);
    }
}
