package net.sf.rails.game.specific._1880;

import java.util.Set;

import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueBonus;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.game.PrivateCompany;

public class TaiwanWesternModifier implements RevenueStaticModifier {

    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        Set<PrivateCompany> privateCompanies =
                revenueAdapter.getCompany().getPresident().getPortfolioModel().getPrivateCompanies();
        for (PrivateCompany company : privateCompanies) {
            if (company.getId().equals("TL")) {
                revenueAdapter.addRevenueBonus(createTaiwanBonus(revenueAdapter));
            }
        }
        return false; // no pretty print
    }
    
    private RevenueBonus createTaiwanBonus(RevenueAdapter revenueAdapter) {
        NetworkVertex taiwan = NetworkVertex.getVertexByIdentifier(revenueAdapter.getVertices(), "N16.-1");
        RevenueBonus bonus = new RevenueBonus(20, "1880TaiwanWesternBonus");
        bonus.addVertex(taiwan);
        return bonus;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return null;
    }

}
