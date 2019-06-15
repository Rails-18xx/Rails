package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.rails.algorithms.NetworkEdge;
import net.sf.rails.algorithms.NetworkGraph;
import net.sf.rails.algorithms.NetworkGraphModifier;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jgrapht.graph.SimpleGraph;


public class BzHTileModifier implements NetworkGraphModifier {

    protected static Logger log =
        LoggerFactory.getLogger(BzHTileModifier.class);
    private List<MapHex> bzhMapHexes = new ArrayList<MapHex> ();

    @Override
    public void modifyMapGraph(NetworkGraph mapGraph) {
        
        SimpleGraph<NetworkVertex, NetworkEdge> graph = mapGraph.getGraph();
        RailsRoot root = RailsRoot.getInstance();
        
        // 1. check Phase
        // this is a violation of the assumption that the track network only dependents on the map configuration
        // but not on other things (like phases)
        int phaseIndex = root.getPhaseManager().getCurrentPhase().getIndex(); 
        if (phaseIndex >= 3 ) {
            log.debug("Boznia-Herzegovina active, index of phase = " + phaseIndex);
            return;
        }
        
        // 2. retrieve BzH vertices ...
        String[] bzhHexes = {"L17","L19","L21","L23","M18","M20","M22","N19","N21"};
        for(String bzhHex:bzhHexes){
            bzhMapHexes.add(root.getMapManager().getHex(bzhHex));
        }
        Set<NetworkVertex> bzhVertices = NetworkVertex.getVerticesByHexes(graph.vertexSet(), bzhMapHexes);

        // 3 ... and remove them from the graph
        graph.removeAllVertices(bzhVertices);
        log.debug("Bosnia Herzegovina inactive, index of phase = " + phaseIndex);
        
    }

    @Override
    public void modifyRouteGraph(NetworkGraph mapGraph, PublicCompany company) {
        // do nothing
    }

}
