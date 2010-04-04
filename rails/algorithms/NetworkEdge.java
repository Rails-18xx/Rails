package rails.algorithms;

public final class NetworkEdge {
    
    private final NetworkVertex source;

    private final NetworkVertex target;
    
    private final boolean autoEdge;
    
    public NetworkEdge(NetworkVertex source, NetworkVertex target, boolean autoEdge) {
        this.source = source;
        this.target = target;
        this.autoEdge = autoEdge;
    }
    
    public NetworkVertex getSource() {
        return source;
    }
    
    public NetworkVertex getTarget() {
        return target;
    }

    public boolean isAutoEdge() {
        return autoEdge;
    }
  
    public String getConnection() {
      return source + " - >" + target;
    }
    
    @Override
    // set to "" to faciltate visual graph
    public String toString() {
        if (!autoEdge)
            return "***";
        else
          return "";
    }
}
