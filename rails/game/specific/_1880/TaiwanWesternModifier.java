package rails.game.specific._1880;

import java.util.List;
import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueBonus;
import rails.algorithms.RevenueStaticModifier;
import rails.game.PrivateCompanyI;

public class TaiwanWesternModifier implements RevenueStaticModifier {
    
    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return null;
    }

    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        List<PrivateCompanyI> privateCompanies =
                revenueAdapter.getCompany().getPresident().getPortfolio().getPrivateCompanies();
        for (PrivateCompanyI company : privateCompanies) {
            if (company.getName().equals("TL")) {
                revenueAdapter.addRevenueBonus(createTaiwanBonus(revenueAdapter));
            }
        }
        
        return false;
    }

    
    private RevenueBonus createTaiwanBonus(RevenueAdapter revenueAdapter) {
        NetworkVertex taiwan = NetworkVertex.getVertexByIdentifier(revenueAdapter.getVertices(),"N16.-1");
        RevenueBonus bonus = new RevenueBonus(20,"1880 TaiwanWesternBonus");
        bonus.addVertex(taiwan);
        return bonus;
        
    }

}
