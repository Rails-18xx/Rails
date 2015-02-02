package rails.game.specific._1880;

import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueBonus;
import rails.algorithms.RevenueStaticModifier;
import rails.game.PublicCompanyI;

public class NewStockMarketBonusModifer implements RevenueStaticModifier  {

    private PublicCompanyI company;
    
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
      
       company = revenueAdapter.getCompany();
       int bonusValue = 0;
       if (company instanceof PublicCompany_1880) {
           bonusValue = company.getCurrentSpace().getType().hasAddRevenue()*10;
           RevenueBonus bonus = new RevenueBonus(bonusValue, "StockMarketPosition");
         
        revenueAdapter.addRevenueBonus(bonus);
        return true;
       }
        return false;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        // TODO Auto-generated method stub
        return null;
    }

}
