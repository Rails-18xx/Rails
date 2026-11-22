package net.sf.rails.game.specific._1870;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueDynamicModifier;
import net.sf.rails.algorithms.RevenueTrainRun;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.BonusToken;
import net.sf.rails.game.Phase;
import net.sf.rails.game.PublicCompany;
import org.slf4j.Logger;
import net.sf.rails.game.GameManager;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DestinationModifier_1870 implements RevenueDynamicModifier {

    private static final Logger log = LoggerFactory.getLogger(DestinationModifier_1870.class);
    private int calculatedBonus = 0;
    private RevenueAdapter revenueAdapter;
    private Phase phase;
    private boolean isConnectionRunRound = false;
    private MapHex homeHex;
    private MapHex destHex;
    public static boolean testMode = false;

    @Override
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        PublicCompany comp = revenueAdapter.getCompany();
        if (comp == null) return false;
        
        this.revenueAdapter = revenueAdapter;
        this.phase = revenueAdapter.getPhase();
        
GameManager gm = revenueAdapter.getRoot().getGameManager();
        this.isConnectionRunRound = (gm.getCurrentRound() instanceof ConnectionRunRound_1870);

this.destHex = comp.getDestinationHex();
        this.homeHex = comp.getHomeHexes().isEmpty() ? null : comp.getHomeHexes().get(0);

// Active if it's the Connection Run, testing phase, or destination reached
        return comp.hasReachedDestination() || this.isConnectionRunRound || testMode;

    }


@Override
    public String prettyPrint(RevenueAdapter revenueAdapter) {
        if (calculatedBonus > 0) {
            return "Destination Bonus: $" + calculatedBonus;
        }
        return "";
    }

    @Override
    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
        // No path adjustment is necessary for the destination modifier
    }

private int calculateDestinationBonus(List<RevenueTrainRun> runs) {
        int totalBonus = 0;
        if (runs == null) return 0;
        
        PublicCompany comp = revenueAdapter.getCompany();
        String destTokenName = comp.getId() + "_Dest";

        for (RevenueTrainRun run : runs) {
            List<NetworkVertex> vertices = run.getRunVertices();
            if (vertices == null || vertices.isEmpty()) continue;

            NetworkVertex startNode = vertices.get(0);
            NetworkVertex endNode = vertices.get(vertices.size() - 1);
            
            boolean startsAtDest = (destHex != null && startNode.getHex() == destHex);
            boolean endsAtDest = (destHex != null && endNode.getHex() == destHex);

            if (startsAtDest || (vertices.size() > 1 && endsAtDest)) {
                int runBonus = 0;
                MapHex targetHex = startsAtDest ? startNode.getHex() : endNode.getHex();

                // Edge vertices often return 0 value. We must scan the hex's vertices
                // within the run to find the actual station value.

                for (NetworkVertex v : vertices) {
                    if (v.getHex() == targetHex) {

                        // The adapter already returns the correct value for THIS company,
                        // including its own bonuses (Port/Cattle) and excluding others'.
                        // Rule 822: The marker doubles the value of the city FOR THAT company.
                        int val = revenueAdapter.getVertexValue(v, run.getTrain(), phase);

                        if (val > runBonus) {
                            runBonus = val;
                        }
                    }
                }

                totalBonus += runBonus;
                
                // Dynamically update the token's UI value to stop it from showing '0' on the map
                if (runBonus > 0 && targetHex != null && targetHex.getBonusTokens() != null) {
                    for (BonusToken t : targetHex.getBonusTokens()) {
                        if (destTokenName.equals(t.getName())) {
                            t.setValue(runBonus);
                        }
                    }
                }
            }
        }
        return totalBonus;
    }
    


@Override
    public int predictionValue(List<RevenueTrainRun> runs) {
        if (testMode) {
            return 999999;
        }
        // Return authentic bonus. Do not hack prediction bounds for normal rounds.
        return calculateDestinationBonus(runs);
    }

@Override
    public int evaluationValue(List<RevenueTrainRun> runs, boolean isFinal) {
        // testMode relies on this massive value to confirm a valid connection exists
        if (testMode) {
            return isValidConnectionRun(runs) ? 999999 : 0;
        }
        
        int bonus = calculateDestinationBonus(runs);
        if (isFinal) {
            calculatedBonus = bonus;
        }
        return bonus;
    }


    private boolean isValidConnectionRun(List<RevenueTrainRun> runs) {
if (destHex == null || homeHex == null) return false;
        if (destHex == null || homeHex == null) return false;
        
        for (RevenueTrainRun run : runs) {
            if (!run.hasAValidRun()) continue;

            List<NetworkVertex> vertices = run.getRunVertices();
            if (vertices == null || vertices.isEmpty()) continue;

            String firstId = vertices.get(0).getHex().getId();
            String lastId = vertices.get(vertices.size() - 1).getHex().getId();

            boolean matchesDirect = (firstId.equals(homeHex.getId()) && lastId.equals(destHex.getId()));
            boolean matchesReverse = (firstId.equals(destHex.getId()) && lastId.equals(homeHex.getId()));

            if (matchesDirect || matchesReverse) {
                return true; // Found the required connection train
            }
        }
        
        return false;
    }
    

}