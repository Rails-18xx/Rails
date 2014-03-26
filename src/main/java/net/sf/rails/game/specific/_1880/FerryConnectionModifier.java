package net.sf.rails.game.specific._1880;

import java.util.Set;

import com.google.common.collect.ImmutableList;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueBonus;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.game.PrivateCompany;

/**
 * This modifier has to remove the RevenueBonus (in fact malus) for ferries for the owner
 * of private Yanda Ferry Company (YC)
 * 
 * Rails 2.0: Simplified by using RevenueBonuses
 */

public class FerryConnectionModifier implements RevenueStaticModifier {

    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        Set<PrivateCompany> privateCompanies =
                revenueAdapter.getCompany().getPresident().getPortfolioModel().getPrivateCompanies();
        for (PrivateCompany company : privateCompanies) {
            if (company.getId().equals("YC")) {
                removeFerryBonuses(revenueAdapter);
            }
        }
        return false; // no pretty print required
    }
    
    private void removeFerryBonuses(RevenueAdapter revenueAdapter) {
        for (RevenueBonus bonus:ImmutableList.copyOf(revenueAdapter.getRevenueBonuses())) {
            if (bonus.getName().toLowerCase().startsWith("ferry")) {
                revenueAdapter.removeRevenueBonus(bonus);
            }
        }
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return null;
    }

}
