package net.sf.rails.game.specific._1870;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueBonus;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.BonusToken;

public class CattleModifier_1870 implements RevenueStaticModifier {

    @Override
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        net.sf.rails.game.PublicCompany comp = revenueAdapter.getCompany();
        if (comp == null) return false;

        boolean ownsCattlePriv = false;
        for (net.sf.rails.game.PrivateCompany priv : comp.getPrivates()) {
            if (priv != null) {
                String id = priv.getId();
                if ("SCC".equals(id) || "Cattle".equals(id) || "Cattl".equals(id)) {
                    ownsCattlePriv = true;
                    break;
                }
            }
        }

        boolean applied = false;
        String compIdLow = comp.getId() != null ? comp.getId().toLowerCase() : "";

        for (NetworkVertex v : revenueAdapter.getVertices()) {
            MapHex hex = v.getHex();
           // Restrict the bonus to revenue-generating stops to prevent multiple additions from track junctions
            if (hex != null && hex.getBonusTokens() != null && (v.isMajor() || v.isMinor())) {
                for (BonusToken t : hex.getBonusTokens()) {
                    String tName = t.getName();
                    if (tName != null) {
                        String lowerName = tName.toLowerCase();
                        if (lowerName.contains("cattle")) {
                            boolean owner = ownsCattlePriv || (!compIdLow.isEmpty() && lowerName.startsWith(compIdLow + "_"));
                            if (owner) {
                                RevenueBonus bonus = new RevenueBonus(10, "Cattle");
                                bonus.addVertex(v);
                                revenueAdapter.addRevenueBonus(bonus);
                                applied = true;
                            }
                        }
                    }
                }
            }
        }
        return applied;
    }

    @Override
    public String prettyPrint(RevenueAdapter revenueAdapter) {
        // Simple bonuses are naturally included in the route string (e.g., 40+10)
        return null;
    }
}