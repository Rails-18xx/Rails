package rails.game.specific._1880;

import java.util.List;

import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueDynamicModifier;
import rails.algorithms.RevenueTrainRun;
import rails.common.LocalText;
import rails.game.PublicCompanyI;

// TODO: Rails 2.0 allow for a simpler modifier to change the revenue value

public class StockMarketBonusModifier implements RevenueDynamicModifier {

    private PublicCompanyI company;
    private int value;
    
    // activate only for 1880 public companies
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        company = revenueAdapter.getCompany();
        if (company instanceof PublicCompany_1880) {
            value = company.getCurrentSpace().getType().hasAddRevenue()*10;
            return value != 0;
        }
        return false;
    }
    
    public int predictionValue() {
        return value;
    }

    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
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
        return LocalText.getText("1880StockMarketBonus", value);
    }

}
