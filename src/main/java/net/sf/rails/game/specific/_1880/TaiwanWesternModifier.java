package net.sf.rails.game.specific._1880;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueDynamicModifier;
import net.sf.rails.algorithms.RevenueTrainRun;
import net.sf.rails.game.PrivateCompany;

public class TaiwanWesternModifier implements RevenueDynamicModifier {
    
    private static final Set<String> FERRY_RUN_SET = createSet();
    private static Set<String> createSet() {
        Set<String> result = new HashSet<String>();
        result.add("E13-F10");
        result.add("F10-E13");
        result.add("E13-G13");
        result.add("G13-E13");
        result.add("H14-K15");
        result.add("K15-H14");
        return Collections.unmodifiableSet(result);
    }

    private boolean playerHasTaiwanWestern;
    
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        playerHasTaiwanWestern = false;
        Set<PrivateCompany> privateCompanies =
                revenueAdapter.getCompany().getPresident().getPortfolioModel().getPrivateCompanies();
        for (PrivateCompany company : privateCompanies) {
            if (company.getName().equals("TL")) {
                playerHasTaiwanWestern = true;
            }
        }
        return true;
    }
    
    public int predictionValue() {
        return 0;
    }

    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
        int value = 0;
        if (playerHasTaiwanWestern == true) {
            for (RevenueTrainRun run : runs) {
                for (NetworkVertex vertex : run.getRunVertices()) {
                    if (vertex.getHex().getId().equals("N16")) {
                        value = value + 20;
                    }
                }
            }
        }
        return value;
    }            
    
    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {        
    }

    public boolean providesOwnCalculateRevenue() {
        return false;
    }

    public int calculateRevenue(RevenueAdapter revenueAdpater) {
        return 0;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return null;
    }



}
