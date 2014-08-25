package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.rails.algorithms.NetworkEdge;
import net.sf.rails.algorithms.NetworkGraph;
import net.sf.rails.algorithms.NetworkGraphModifier;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.RailsRoot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jgrapht.graph.SimpleGraph;


public class ItalyTileModifier implements NetworkGraphModifier {

    protected static Logger log =
        LoggerFactory.getLogger(ItalyTileModifier.class);

    @SuppressWarnings("null")
    public void modifyGraph(NetworkGraph mapGraph) {
        
        // TODO (Rails 2.0): Add root reference to modifiers
        SimpleGraph<NetworkVertex, NetworkEdge> graph = mapGraph.getGraph();
        RailsRoot root = RailsRoot.getInstance();
        List<MapHex> italyMapHexes = new ArrayList<MapHex> ();
        // 1. check Phase
        // this is a violation of the assumption that the track network only dependents on the map configuration
        // but not on other things (like phases)
        int phaseIndex = root.getPhaseManager().getCurrentPhase().getIndex(); 
        if (phaseIndex < 4 ) {
            log.debug("Italy active, index of phase = " + phaseIndex);
            return;
        }
        
        // 2. retrieve Italy vertices ...
        String [] italyHexes = {"K2","K4","K8","K10","L3","L5","L7","L9","M4","M6","M8"};
         for (String italyHex:italyHexes){
             italyMapHexes.add(root.getMapManager().getHex(italyHex));
         }
        Set<NetworkVertex> italyVertices = NetworkVertex.getVerticesByHexes(graph.vertexSet(), italyMapHexes);

        // 3 ... and remove them from the graph
        graph.removeAllVertices(italyVertices);
        log.debug("Italy inactive, index of phase = " + phaseIndex);
        
    }

}
