package rails.game.specific._1835;

import java.util.Set;

import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.algorithms.NetworkEdge;
import rails.algorithms.NetworkGraphBuilder;
import rails.algorithms.NetworkGraphModifier;
import rails.algorithms.NetworkVertex;
import rails.game.GameManager;
import rails.game.GameManager;
import rails.game.MapHex;
import rails.game.RailsRoot;

/**
 * Analogue to the BirminghamTileModifier in 1851
 * Removes Elsas from the map if value is equal to zero
 * @author freystef
 */

public class ElsasModifier implements NetworkGraphModifier {

    private static final Logger log =
            LoggerFactory.getLogger(ElsasModifier.class);

    public void modifyGraph(NetworkGraphBuilder graphBuilder) {
        
        RailsRoot root = RailsRoot.getInstance();

        SimpleGraph<NetworkVertex, NetworkEdge> graph = graphBuilder.getMapGraph();

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
}
