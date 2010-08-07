package rails.algorithms;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;

import rails.ui.swing.hexmap.HexMap;

public final class NetworkEdge implements Comparable<NetworkEdge> {

    protected static Logger log =
        Logger.getLogger(NetworkEdge.class.getPackage().getName());
    
    private final NetworkVertex source;

    private final NetworkVertex target;
    
    private boolean greedy;
    
    private final int distance;
    
    private final List<NetworkVertex> hiddenVertices;
    // list of vertexes that were merged into the edge
    
    private int routeCosts;
    // for the multigraph approach defines the number of routes excluded
    
    public NetworkEdge(NetworkVertex source, NetworkVertex target, boolean greedy) {
        this.source = source;
        this.target = target;
        this.greedy = greedy;
        if (greedy)
            this.distance = 1;
        else
            this.distance = 0;
        hiddenVertices = new ArrayList<NetworkVertex>();
    }
    
    public NetworkEdge(NetworkVertex source, NetworkVertex target, boolean greedy, 
                int distance, List<NetworkVertex> hiddenVertexes) {
        this.source = source;
        this.target = target;
        this.greedy = greedy;
        this.distance = distance;
        this.hiddenVertices = hiddenVertexes;
    }
    
    public NetworkVertex getSource() {
        return source;
    }
    
    public NetworkVertex getTarget() {
        return target;
    }
    
    private NetworkVertex getLowVertex() {
        if (source.compareTo(target) <= 0) {
            return source;
        } else {
            return target;
        }
    }

    private NetworkVertex getHighVertex() {
        if (source.compareTo(target) <= 0) {
            return target;
        } else {
            return source;
        }
    }
    
    /** returns the other vertex, if given vertex is not source/target of vertex, returns null */
    NetworkVertex getOtherVertex(NetworkVertex vertex) {
        if (this.source == vertex) {
            return target;
        } else if (this.target == vertex) {
            return source;
        }
        return null;
    }
    
    /** gets common vertex, if both source and target are common, returns source of this edge */
    NetworkVertex getCommonVertex(NetworkEdge otherEdge) {
       if (this.source == otherEdge.source || this.source == otherEdge.target) {
           return this.source;
       } else if (this.target == otherEdge.source || this.target == otherEdge.target) {
           return this.target;
       }
       return null;
    }

    public boolean isGreedy() {
        return greedy;
    }
  
    public void setGreedy(boolean greedy) {
       this.greedy = greedy;
    }
    
    public int getDistance() {
        return distance;
    }
    
    int getRouteCosts() {
        return routeCosts;
    }
    
    void setRouteCosts(int routeCosts) {
        this.routeCosts = routeCosts;
    }
    
    public List<NetworkVertex> getHiddenVertices() {
        return hiddenVertices;
    }
   
    /**
     * all vertices from source to target, including hidden vertices
     */
    public List<NetworkVertex> getVertexPath() {
        List<NetworkVertex> vertexPath = new ArrayList<NetworkVertex>();
        vertexPath.add(source);
        vertexPath.addAll(hiddenVertices);
        vertexPath.add(target);
        return vertexPath;
    }
    
    public String toFullInfoString() {
        StringBuffer info = new StringBuffer();
        info.append("Edge " + getConnection());
        info.append(", greedy = " + greedy);
        info.append(", distance = " + distance);
        info.append(", hidden vertexes = " + hiddenVertices);
        return info.toString();
    }
    
    public String getOrderedConnection() {
        return getLowVertex() + " -> " + getHighVertex();
    }
    
    public String getConnection() {
      return source + " -> " + target;
    }

    @Override
    // set to "" to faciltate visual graph
    public String toString() {
        if (!greedy)
            return "*** / " + distance;
        else
          return "" + distance;
    }

    /** 
     * Natural order based on the ordering of the connected vertices
     */
    public int compareTo(NetworkEdge otherEdge) {
        int result = this.getLowVertex().compareTo(otherEdge.getLowVertex());
        if (result == 0) {
            result = this.getHighVertex().compareTo(otherEdge.getHighVertex());
        }
        return result;
    }
    
    /**
     * Ordering based on routecosts
     */
    public static final class CostOrder implements Comparator<NetworkEdge> {
        
        public int compare(NetworkEdge edgeA, NetworkEdge edgeB) {
            int result = ((Integer)edgeA.getRouteCosts()).compareTo(edgeB.getRouteCosts()); 
            if (result == 0)
                result = edgeA.compareTo(edgeB); // otherwise use natural ordering
            return result;
        }
    }

 
    static class MergeResult {
        NetworkEdge newEdge;
        NetworkVertex removedVertex;
        MergeResult(NetworkEdge newEdge, NetworkVertex removedVertex) {
            this.newEdge = newEdge;
            this.removedVertex = removedVertex;
        }
    }
    
    public static MergeResult mergeEdges(NetworkEdge edgeA, NetworkEdge edgeB) {
        log.info("Merge of edge " + edgeA.toFullInfoString() + " and edge " + edgeB.toFullInfoString());

        NetworkVertex sourceA = edgeA.getSource();
        NetworkVertex targetA = edgeA.getTarget();
        NetworkVertex sourceB = edgeB.getSource();
        NetworkVertex targetB = edgeB.getTarget();
        
        NetworkVertex newSource, newTarget, vertex = null;
        
        boolean reverseA = false, reverseB = false;
        if (sourceA == sourceB) {
            newSource = targetA;
            newTarget = targetB;
            vertex = sourceA;
            reverseA = true;
        } else if (sourceA == targetB) {
            newSource = targetA;
            newTarget = sourceB;
            vertex = sourceA;
            reverseA = true;
            reverseB = true;
        } else if (targetA == sourceB) {
            newSource = sourceA;
            newTarget = targetB;
            vertex = targetA;
        } else if (targetA == targetB) {
            newSource = sourceA;
            newTarget = sourceB;
            vertex = targetA;
            reverseB = true;
        } else {
            return null;
        }

        log.info("Merge newSource = " + newSource + " newTarget = " + newTarget + " remove vertex = " + vertex);
        
        // define new edge
        int distance = edgeA.getDistance() + edgeB.getDistance();

        // create new hiddenVertexes
        List<NetworkVertex> hiddenVertexes = new ArrayList<NetworkVertex>();
        List<NetworkVertex> hiddenA = edgeA.getHiddenVertices();
        if (reverseA) {
            hiddenA = new ArrayList<NetworkVertex>(hiddenA); // clone
            Collections.reverse(hiddenA);
        }
        List<NetworkVertex> hiddenB = edgeB.getHiddenVertices();
        if (reverseB) {
            hiddenB = new ArrayList<NetworkVertex>(hiddenB); // clone
            Collections.reverse(hiddenB);
        }
        hiddenVertexes.addAll(hiddenA);
        hiddenVertexes.add(vertex);
        hiddenVertexes.addAll(hiddenB);
        NetworkEdge newEdge = 
            new NetworkEdge(newSource, newTarget, true, distance, hiddenVertexes);
        log.info("New edge = " + newEdge.toFullInfoString());
        
        // returns newEdge
        return new MergeResult(newEdge, vertex);
    }
    
    public static boolean mergeEdgesInGraph(Graph<NetworkVertex, NetworkEdge> graph,
            NetworkEdge edgeA, NetworkEdge edgeB) {

        // use generic merge function
        MergeResult mergeResult = mergeEdges(edgeA, edgeB);
        NetworkEdge newEdge = mergeResult.newEdge;
        NetworkVertex removedVertex = mergeResult.removedVertex;

        if (newEdge == null) return false;
        
        // check if graph contains the edge already
        if (graph.containsEdge(newEdge.getSource(), newEdge.getTarget())) return false;
        
        graph.addEdge(newEdge.getSource(), newEdge.getTarget(), newEdge);

        log.info("New edge =  " + newEdge.toFullInfoString());

        // remove vertex
        graph.removeVertex(removedVertex);

        return true;
    }

    /**
     * for a given edge it replaces one of the vertices by a different one
     * otherwise copies all edge attributes
     * @return copied edge with replaced vertex, null if oldVertex is neither source, nor target
     */
    public static NetworkEdge replaceVertex(NetworkEdge edge, NetworkVertex oldVertex, NetworkVertex newVertex) {
        NetworkEdge newEdge;
        if (edge.source == oldVertex) {
            newEdge= new NetworkEdge(newVertex, edge.target, edge.greedy, edge.distance, edge.hiddenVertices);
        } else if (edge.target == oldVertex) {
            newEdge= new NetworkEdge(edge.source, newVertex, edge.greedy, edge.distance, edge.hiddenVertices);
        } else {
            newEdge = null;
        }
        return newEdge;
    }

    public static Shape getEdgeShape(HexMap map, NetworkEdge edge){
        Point2D source = NetworkVertex.getVertexPoint2D(map, edge.getSource());
        Point2D target = NetworkVertex.getVertexPoint2D(map, edge.getTarget());
        Shape edgeShape;
        if (source != null && target != null) {
            edgeShape = new Line2D.Double(source, target);
        } else {
            edgeShape = null;
        }
        return edgeShape;
    }
    
}
