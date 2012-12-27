package rails.game.specific._1835;

import java.util.Set;

import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.algorithms.NetworkEdge;
import rails.algorithms.NetworkGraph;
import rails.algorithms.NetworkGraphModifier;
import rails.algorithms.NetworkVertex;
import rails.game.GameManager;
import rails.game.MapHex;

/**
 * Analogue to the BirminghamTileModifier in 1851
 * Removes Elsas from the map if value is equal to zero
 * @author freystef
 */

public class ElsasModifier implements NetworkGraphModifier {

    private static final Logger log =
            LoggerFactory.getLogger(ElsasModifier.class);

    public void modifyGraph(NetworkGraph mapGraph) {

        GameManager gm = GameManager.getInstance();
        SimpleGraph<NetworkVertex, NetworkEdge> graph = mapGraph.getGraph();

        // Check if (one of the  elsasHex has zero value ...
        MapHex hex = gm.getMapManager().getHex("M5");
        if (hex.getCurrentValueForPhase(gm.getCurrentPhase()) == 0) {
            // .. then remove both
            Set<NetworkVertex> vertices = NetworkVertex.getVerticesByHex(graph.vertexSet(), hex);
            graph.removeAllVertices(vertices);
            hex = gm.getMapManager().getHex("N4");
            vertices = NetworkVertex.getVerticesByHex(graph.vertexSet(), hex);
            graph.removeAllVertices(vertices);
            log.debug("Elsas is inactive");
        }
    }
}
