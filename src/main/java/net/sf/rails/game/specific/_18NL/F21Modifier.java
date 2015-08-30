package net.sf.rails.game.specific._18NL;

import java.util.Set;

import net.sf.rails.algorithms.NetworkEdge;
import net.sf.rails.algorithms.NetworkGraph;
import net.sf.rails.algorithms.NetworkGraphModifier;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;

import org.jgrapht.graph.SimpleGraph;

/**
 * Analogue to the BirminghamTileModifier in 1851
 * Removes F21 from the map if value is equal to zero
 */

public class F21Modifier implements NetworkGraphModifier {

    @Override
    public void modifyMapGraph(NetworkGraph mapGraph) {
        
        RailsRoot root = RailsRoot.getInstance();
        SimpleGraph<NetworkVertex, NetworkEdge> graph = mapGraph.getGraph();

        // Check if F21 has zero value
        MapHex hex = root.getMapManager().getHex("F21");
        if (hex.getCurrentValueForPhase(root.getPhaseManager().getCurrentPhase()) == 0) {
            // ... then remove those vertices
            Set<NetworkVertex> vertices = NetworkVertex.getVerticesByHex(graph.vertexSet(), hex);
            graph.removeAllVertices(vertices);
        }
    }
    
    @Override
    public void modifyRouteGraph(NetworkGraph mapGraph, PublicCompany company) {
        // do nothing
    }

}
