package net.sf.rails.game.specific._1880;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueBonus;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.algorithms.RevenueTrainRun;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.PrivateCompany;

/**
 * This modifier has to remove the RevenueBonus (in fact malus) for ferries for the owner
 * of private Yanda Ferry Company (YC)
 * 
 * Rails 2.0: Simplified by using RevenueBonuses
 */

public class FerryConnectionModifier implements RevenueStaticModifier {
   
    
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

    private boolean playerHasFerryCompany;

    

    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        playerHasFerryCompany=false;
        Set<PrivateCompany> privateCompanies =
                revenueAdapter.getCompany().getPresident().getPortfolioModel().getPrivateCompanies();
        for (PrivateCompany company : privateCompanies) {
            if (company.getId().equals("YC")) {
                playerHasFerryCompany=true;
            }
        }
        return true; // no pretty print required
    }
    public int predictionValue() {
        return 0;
    }

    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
        int value = 0;
        if (playerHasFerryCompany == false) {
            for (RevenueTrainRun run : runs) {
                MapHex prevHex = null;
                MapHex thisHex = null;
                for (NetworkVertex vertex : run.getRunVertices()) {
                    thisHex = vertex.getHex();
                    if ((prevHex != null)
                        && (connectionIsOverWater(prevHex, thisHex))) {
                        value = value - 10;
                    }
                    prevHex = thisHex;
                }
            }
        }
        return value;
    }            

    private boolean connectionIsOverWater(MapHex firstHex, MapHex secondHex) {
        String hexesString = firstHex.getId() + "-" + secondHex.getId();
        if (FERRY_RUN_SET.contains(hexesString) == true) {
            return true;
        }
        return false;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return null;
    }

}
