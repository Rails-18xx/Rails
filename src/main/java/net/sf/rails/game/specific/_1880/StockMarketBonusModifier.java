package net.sf.rails.game.specific._1880;

import java.util.List;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueDynamicModifier;
import net.sf.rails.algorithms.RevenueTrainRun;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.PublicCompany;

/**
 * StockMarketBonus is a fixed addition to the revenue, however it is easy to
 * implement as a dynamic ex-post modifier
 */

public class StockMarketBonusModifier implements RevenueDynamicModifier {

    private int bonusValue;

    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        PublicCompany company = revenueAdapter.getCompany();
        if (company instanceof PublicCompany_1880) {
            bonusValue =
                    company.getCurrentSpace().getType().hasAddRevenue() * 10;
            return true;
        }
        return false;
    }

    public int predictionValue(List<RevenueTrainRun> runs) {
        return bonusValue;
    }

    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
        return bonusValue;
    }

    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {}

    public boolean providesOwnCalculateRevenue() {
        return false;
    }

    public int calculateRevenue(RevenueAdapter revenueAdpater) {
        return 0;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return LocalText.getText("1880StockMarketBonus", bonusValue);
    }

}
