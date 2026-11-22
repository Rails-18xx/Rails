package net.sf.rails.game.specific._1856;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueBonus;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.BonusToken;

public class PortModifier_1856 implements RevenueStaticModifier {

    @Override
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        net.sf.rails.game.PublicCompany comp = revenueAdapter.getCompany();
        if (comp == null) return false;

        boolean ownsPortPriv = false;
        for (net.sf.rails.game.PrivateCompany priv : comp.getPrivates()) {
            if (priv != null) {
                String id = priv.getId().toLowerCase();
                // Check if the company owns the Great Lakes Shipping Company private
                if (id.contains("glsc") || id.contains("port")) {
                    ownsPortPriv = true;
                    break;
                }
            }
        }

        boolean applied = false;
        String compIdLow = comp.getId() != null ? comp.getId().toLowerCase() : "";

        for (NetworkVertex v : revenueAdapter.getVertices()) {
            MapHex hex = v.getHex();
            // Restrict the bonus to revenue-generating stops
            if (hex != null && hex.getBonusTokens() != null && (v.isMajor() || v.isMinor())) {
                for (BonusToken t : hex.getBonusTokens()) {
                    String tName = t.getName();
                    if (tName != null) {
                        String lowerName = tName.toLowerCase();
                        if (lowerName.contains("port")) {
                            // The token belongs to this company if they own the private 
                            // OR if the token name is explicitly prefixed with their ID (e.g. "GT_Port")
                            boolean owner = ownsPortPriv || (!compIdLow.isEmpty() && lowerName.startsWith(compIdLow + "_"));
                            if (owner) {
                                RevenueBonus bonus = new RevenueBonus(20, "Port");
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
        // Simple bonuses are naturally included in the route string (e.g., 40+20)
        return null;
    }
}