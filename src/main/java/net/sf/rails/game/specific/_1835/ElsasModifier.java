package net.sf.rails.game.specific._1835;

import java.util.Set;

import net.sf.rails.algorithms.NetworkEdge;
import net.sf.rails.algorithms.NetworkGraph;
import net.sf.rails.algorithms.NetworkGraphModifier;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;

import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Analogue to the BirminghamTileModifier in 1851
 * Removes Elsas from the map if value is equal to zero
 */

public class ElsasModifier implements NetworkGraphModifier {

    private static final Logger log =
            LoggerFactory.getLogger(ElsasModifier.class);

    @Override
    public void modifyMapGraph(NetworkGraph mapGraph) {
        
        RailsRoot root = RailsRoot.getInstance();
        SimpleGraph<NetworkVertex, NetworkEdge> graph = mapGraph.getGraph();

        // Check if (one of the  elsasHex has zero value ...
        MapHex hex = root.getMapManager().getHex("M5");
        if (hex.getCurrentValueForPhase(root.getPhaseManager().getCurrentPhase()) == 0) {
            // .. then remove both
            Set<NetworkVertex> vertices = NetworkVertex.getVerticesByHex(graph.vertexSet(), hex);
            graph.removeAllVertices(vertices);
            hex = root.getMapManager().getHex("N4");
            vertices = NetworkVertex.getVerticesByHex(graph.vertexSet(), hex);
            graph.removeAllVertices(vertices);
            log.debug("Elsas is inactive");
        }
    }
    
    @Override
    public void modifyRouteGraph(NetworkGraph mapGraph, PublicCompany company) {
        // do nothing
    }

}
