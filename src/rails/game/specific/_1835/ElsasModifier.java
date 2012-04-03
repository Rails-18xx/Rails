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
import rails.game.MapHex;

/**
 * Analogue to the BirminghamTileModifier in 1851
 * Removes Elsas from the map if value is equal to zero
 * @author freystef
 */

public class ElsasModifier implements NetworkGraphModifier {

    private static final Logger log =
            LoggerFactory.getLogger(ElsasModifier.class);

    public void modifyGraph(NetworkGraphBuilder graphBuilder) {

        GameManager gm = GameManager.getInstance();
        SimpleGraph<NetworkVertex, NetworkEdge> graph = graphBuilder.getMapGraph();

        // Check if elsasHex has zero value ...
        MapHex elsasHex = gm.getMapManager().getHex("M5");
        if (elsasHex.getCurrentValueForPhase(gm.getCurrentPhase()) == 0) {
            // .. then remove 
            Set<NetworkVertex> elsasVertices = NetworkVertex.getVerticesByHex(graph.vertexSet(), elsasHex);
            graph.removeAllVertices(elsasVertices);
            log.debug("Elsas is inactive");
        }
    }
}
