package rails.game.specific._1835;

import java.util.Set;

import org.apache.log4j.Logger;
import org.jgrapht.graph.SimpleGraph;

import rails.algorithms.NetworkEdge;
import rails.algorithms.NetworkGraphBuilder;
import rails.algorithms.NetworkGraphModifier;
import rails.algorithms.NetworkVertex;
import rails.game.GameManager;
import rails.game.GameManagerI;
import rails.game.MapHex;

/**
 * Analogue to the BirminghamTileModifier in 1851
 * Removes Elsas from the map if value is equal to zero
 * @author freystef
 */

public class ElsasModifier implements NetworkGraphModifier {

    protected static Logger log =
            Logger.getLogger(ElsasModifier.class.getPackage().getName());

    public void modifyGraph(NetworkGraphBuilder graphBuilder) {

        GameManagerI gm = GameManager.getInstance();
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
