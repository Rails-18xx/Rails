package rails.algorithms;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import rails.ui.swing.hexmap.HexMap;

public final class NetworkEdge {
    
    private final NetworkVertex source;

    private final NetworkVertex target;
    
    private boolean greedy;
    
    private final int distance;
    
    public NetworkEdge(NetworkVertex source, NetworkVertex target, boolean greedy) {
        this.source = source;
        this.target = target;
        this.greedy = greedy;
        if (greedy)
            this.distance = 1;
        else
            this.distance = 0;
    }
    
    public NetworkEdge(NetworkVertex source, NetworkVertex target, boolean greedy, int distance) {
        this.source = source;
        this.target = target;
        this.greedy = greedy;
        this.distance = distance;
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
    
    public String getConnection() {
      return source + " - >" + target;
    }

    @Override
    // set to "" to faciltate visual graph
    public String toString() {
        if (!greedy)
            return "*** / " + distance;
        else
          return "" + distance;
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
