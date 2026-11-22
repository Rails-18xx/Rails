package net.sf.rails.game.specific._1870;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueBonus;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.BonusToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GulfModifier_1870 implements RevenueStaticModifier {

    private static final Logger log = LoggerFactory.getLogger(GulfModifier_1870.class);

    @Override
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        net.sf.rails.game.PublicCompany comp = revenueAdapter.getCompany();
        if (comp == null) return false;

        boolean applied = false;
        String compIdLow = comp.getId() != null ? comp.getId().toLowerCase() : "";

        for (NetworkVertex v : revenueAdapter.getVertices()) {
            MapHex hex = v.getHex();
           // Restrict the bonus to revenue-generating stops to prevent multiple additions from track junctions
            if (hex != null && hex.getBonusTokens() != null && (v.isMajor() || v.isMinor())) {
                for (BonusToken t : hex.getBonusTokens()) {
                    String tName = t.getName();

                    if (tName != null && tName.toLowerCase().contains("gulf")) {
                        String lowerName = tName.toLowerCase();

                        // Determine ownership of the Gulf token
                        boolean ownsGulf = !compIdLow.isEmpty() && lowerName.startsWith(compIdLow + "_");
                        if (!ownsGulf) {
                            for (net.sf.rails.game.PrivateCompany priv : comp.getPrivates()) {
                                String privId = priv.getId();
                                if ("Gulf".equalsIgnoreCase(privId) || (priv.getName() != null && priv.getName().toLowerCase().contains("gulf"))) {
                                    ownsGulf = true;
                                    break;
                                }
                            }
                        }

                        int bonusVal = 0;
                        if (ownsGulf) {
                            bonusVal = 20; // Owner gets +$20 for Open or Closed
                        } else {
                            // Non-owners get +$10 only if it's the Open Port
                            if (lowerName.endsWith("_0") || lowerName.contains("open")) {
                                bonusVal = 10;
                            }
                        }

                        if (bonusVal > 0) {
                            RevenueBonus bonus = new RevenueBonus(bonusVal, "Gulf");
                            bonus.addVertex(v);
                            revenueAdapter.addRevenueBonus(bonus);
                            applied = true;
                        }
                    }
                }
            }
        }
        return applied;
    }

    @Override
    public String prettyPrint(RevenueAdapter revenueAdapter) {
        // Static modifiers automatically append to the vertex string (e.g., 40+10), so we return null here.
        return null;
    }
}