package net.sf.rails.game.specific._1817;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueDynamicModifier;
import net.sf.rails.algorithms.RevenueTrainRun;
import net.sf.rails.algorithms.NetworkEdge;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.BonusToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class BridgeModifier_1817 implements RevenueDynamicModifier {

    private static final Logger log = LoggerFactory.getLogger(BridgeModifier_1817.class);
    private int calculatedBonus = 0;

    @Override
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        return true;
    }

    @Override
    public int predictionValue(List<RevenueTrainRun> runs) {
        return calculateBridgeBonus(runs);
    }

    @Override
    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
        int bonus = calculateBridgeBonus(runs);
        if (optimalRuns) {
            calculatedBonus = bonus;
            if (bonus > 0) {
            }
        }
        return bonus;
    }

    @Override
    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
    }

    @Override
    public String prettyPrint(RevenueAdapter revenueAdapter) {
        if (calculatedBonus > 0) {
            return " + $" + calculatedBonus + " (Bridge)";
        }
        return null;
    }

    private boolean hasBridgeToken(MapHex hex) {
        if (hex == null || hex.getBonusTokens() == null)
            return false;

        for (BonusToken t : hex.getBonusTokens()) {
            if (t != null) {
              
                if ((t.getName() != null && t.getName().contains("Bridge")) || 
                    (t.getId() != null && t.getId().contains("Bridge")) || 
                    (t.getId() != null && t.getId().contains("UBC")) ||
                    (t.getId() != null && t.getId().contains("OBC"))) {

                    return true;
                }
            }
        }
        return false;
    }

    private int calculateBridgeBonus(List<RevenueTrainRun> runs) {

int totalBridgeVisits = 0;
                if (runs == null) return 0;
        
        for (RevenueTrainRun run : runs) {
            Set<MapHex> visitedHexes = new HashSet<>();
            
            // 1. Check hexes associated with explicit vertices (Cities like Louisville/Cincinnati)
            if (run.getRunVertices() != null) {
                for (NetworkVertex v : run.getRunVertices()) {
                    if (v != null && v.getHex() != null) {
                        visitedHexes.add(v.getHex());
                    }
                }
            }

            // 2. Check edges for hidden path hexes (if any)
            List<NetworkEdge> edges = run.getEdges(); 
            if (edges != null) {
                for (NetworkEdge e : edges) {
                    List<NetworkVertex> path = e.getVertexPath();
                    if (path != null) {
                        for (NetworkVertex v : path) {
                            if (v != null && v.getHex() != null) {
                                visitedHexes.add(v.getHex());
                            }
                        }
                    }
                }
            }

            // 3. Award $10 once per bridge hex touched by this train run
            for (MapHex hex : visitedHexes) {

                boolean hasBridge = hasBridgeToken(hex);
                // log.info("Checking hex {} for Bridge. Found: {}", (hex != null ? hex.getId() : "null"), hasBridge);

                if (hasBridge) {
                    totalBridgeVisits++;
                }
            }
        }

        int dynamicBonus = totalBridgeVisits * 10;
        // log.info("Bridge hexes visited {} times. Dynamic bonus added: {}", totalBridgeVisits, dynamicBonus);

        return dynamicBonus;
    }
}