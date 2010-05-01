package rails.algorithms;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;

import rails.ui.swing.hexmap.HexMap;

public final class NetworkEdge {

    protected static Logger log =
        Logger.getLogger(NetworkEdge.class.getPackage().getName());
    
    private final NetworkVertex source;

    private final NetworkVertex target;
    
    private boolean greedy;
    
    private final int distance;
    
    private final List<NetworkVertex> hiddenVertexes;
    // list of vertexes that were merged into the 
    
    public NetworkEdge(NetworkVertex source, NetworkVertex target, boolean greedy) {
        this.source = source;
        this.target = target;
        this.greedy = greedy;
        if (greedy)
            this.distance = 1;
        else
            this.distance = 0;
        hiddenVertexes = new ArrayList<NetworkVertex>();
    }
    
    public NetworkEdge(NetworkVertex source, NetworkVertex target, boolean greedy, 
                int distance, List<NetworkVertex> hiddenVertexes) {
        this.source = source;
        this.target = target;
        this.greedy = greedy;
        this.distance = distance;
        this.hiddenVertexes = hiddenVertexes;
    }
    
    public NetworkVertex getSource() {
        return source;
    }
    
    public NetworkVertex getTarget() {
        return target;
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
    
    public List<NetworkVertex> getHiddenVertexes() {
        return hiddenVertexes;
    }
    
    public String toFullInfoString() {
        StringBuffer info = new StringBuffer();
        info.append("Edge " + getConnection());
        info.append(", greedy = " + greedy);
        info.append(", distance = " + distance);
        info.append(", hidden vertexes = " + hiddenVertexes);
        return info.toString();
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
    
    public static boolean mergeEdges(Graph<NetworkVertex, NetworkEdge> graph,
            NetworkEdge edgeA, NetworkEdge edgeB) {

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
            return false;
        }

        log.info("Merge newSource = " + newSource + " newTarget = " + newTarget + " remove vertex = " + vertex);
        
        // check if graph contains the edge already
        if (graph.containsEdge(newSource, newTarget)) return false;
        
        // define new edge
        int distance = edgeA.getDistance() + edgeB.getDistance();

        // create new hiddenVertexes
        List<NetworkVertex> hiddenVertexes = new ArrayList<NetworkVertex>();
        List<NetworkVertex> hiddenA = edgeA.getHiddenVertexes();
        if (reverseA)
            Collections.reverse(hiddenA);
        List<NetworkVertex> hiddenB = edgeB.getHiddenVertexes();
        if (reverseB)
            Collections.reverse(hiddenB);
        hiddenVertexes.addAll(hiddenA);
        hiddenVertexes.add(vertex);
        hiddenVertexes.addAll(hiddenB);
        NetworkEdge newEdge = 
            new NetworkEdge(newSource, newTarget, true, distance, hiddenVertexes);
        graph.addEdge(newSource, newTarget, newEdge);

        log.info("New edge =  " + newEdge.toFullInfoString());

        // remove vertex
        graph.removeVertex(vertex);

        return true;
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
