package rails.game.specific._1835;

import java.util.ArrayList;
import java.util.List;
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
