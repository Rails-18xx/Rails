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

public class CoalMineModifier_1817 implements RevenueDynamicModifier {

    private static final Logger log = LoggerFactory.getLogger(CoalMineModifier_1817.class);
    private int calculatedBonus = 0;

    @Override
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        return true;
    }


    @Override
    public int predictionValue(List<RevenueTrainRun> runs) {
        return calculateCoalMineBonus(runs);
    }

    @Override
    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
        int bonus = calculateCoalMineBonus(runs);
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
            return " + $" + calculatedBonus + " (Coal Mine)";
        }
        return null;
    }

    // We are modifying CoalMineModifier_1817.java

    private boolean hasCoalMineToken(MapHex hex) {
        if (hex == null || hex.getBonusTokens() == null)
            return false;

        for (BonusToken t : hex.getBonusTokens()) {
            if (t != null) {
                if ("CoalMine".equals(t.getName()) || "CoalMine".equals(t.getId())) {
                    return true;
                }
            }
        }
        return false;
    }



private int calculateCoalMineBonus(List<RevenueTrainRun> runs) {
        int totalBonus = 0;
        if (runs == null) return 0;
        
        for (RevenueTrainRun run : runs) {
            Set<MapHex> visitedHexes = new HashSet<>();
            
            // 1. Check hexes associated with explicit vertices (Cities/Off-maps)
            if (run.getRunVertices() != null) {
                for (NetworkVertex v : run.getRunVertices()) {
                    if (v != null && v.getHex() != null) {
                        visitedHexes.add(v.getHex());
                    }
                }
            }

            // 2. Check hexes associated with edges (Plain track/Mountains)
            List<NetworkEdge> edges = run.getEdges(); 
            if (edges != null) {
                for (NetworkEdge e : edges) {
                    // getVertexPath() returns source, target, and all hidden track hexes
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
            
            // 3. Award $10 once per hex touched by this specific train run
            for (MapHex hex : visitedHexes) {
               boolean hasMine = hasCoalMineToken(hex);
                // log.info("Checking hex {} for Coal Mine. Found: {}", (hex != null ? hex.getId() : "null"), hasMine);
                if (hasMine) {
                    totalBonus += 10;
                }
            }
        }
        // log.info("Total Coal Mine bonus for all runs: {}", totalBonus);
        return totalBonus;
    }









}