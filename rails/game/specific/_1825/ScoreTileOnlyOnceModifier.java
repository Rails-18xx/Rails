package rails.game.specific._1825;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueStaticModifier;
import rails.game.MapHex;

/**
 * This modifier ensures that on each tile only one station can be visited
 */

public class ScoreTileOnlyOnceModifier implements RevenueStaticModifier {

    public void modifyCalculator(RevenueAdapter revenueAdapter) {
        // 1. define for each hex a list of stations
        HashMap<MapHex, List<NetworkVertex>> hexStations = new HashMap<MapHex, List<NetworkVertex>>();
        for (NetworkVertex v:revenueAdapter.getVertices()) {
            if (v.isStation()) {
                if (!hexStations.containsKey(v.getHex())) {
                    List<NetworkVertex> stations = new ArrayList<NetworkVertex>();
                    hexStations.put(v.getHex(), stations);
                }
                hexStations.get(v.getHex()).add(v);
            }
        }
        // 2. convert those with more than one station to a vertex visit set
        for (MapHex hex:hexStations.keySet()) {
            if (hexStations.get(hex).size() >= 2) {
                revenueAdapter.addVertexVisitSet(revenueAdapter.new VertexVisit(hexStations.get(hex)));
            }
        }
       
    }

}
