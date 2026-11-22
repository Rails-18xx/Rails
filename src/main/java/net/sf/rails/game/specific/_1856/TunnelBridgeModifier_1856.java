package net.sf.rails.game.specific._1856;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueBonus;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.game.MapHex;

public class TunnelBridgeModifier_1856 implements RevenueStaticModifier {

    @Override
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        net.sf.rails.game.PublicCompany comp = revenueAdapter.getCompany();
        if (comp == null) return false;

        boolean hasTunnelRight = false;
        boolean hasBridgeRight = false;

        // 1. Check if the company currently owns the active private company certificates
        for (net.sf.rails.game.PrivateCompany priv : comp.getPrivates()) {
            if (priv != null && !priv.isClosed()) {
                String id = priv.getId().toLowerCase();
                if (id.contains("tunn")) {
                    hasTunnelRight = true;
                }
                if (id.contains("brid")) {
                    hasBridgeRight = true;
                }
            }
        }

      for (net.sf.rails.game.Bonus bonus : comp.getBonuses()) {
            if (bonus != null && bonus.getName() != null) {
                String name = bonus.getName().toLowerCase();
                if (name.contains("tunn")) {
                    hasTunnelRight = true;
                }
                if (name.contains("brid")) {
                    hasBridgeRight = true;
                }
            }
        }

        boolean applied = false;
        
        // 3. Scan route vertices using precise coordinate match constraints
        for (NetworkVertex v : revenueAdapter.getVertices()) {
            MapHex hex = v.getHex();
            if (hex != null && (v.isMajor() || v.isMinor())) {
                String hexId = hex.getId();
                if (hexId == null) continue;
                
                // Sarnia connection (Hex B13) -> Tunnel Bonus
                if (hasTunnelRight && hexId.equalsIgnoreCase("B13")) {
                    RevenueBonus bonus = new RevenueBonus(10, "Tunnel");
                    bonus.addVertex(v);
                    revenueAdapter.addRevenueBonus(bonus);
                    applied = true;
                }
                
                // Buffalo connections (Hexes P17 or P19) -> Bridge Bonus
                if (hasBridgeRight && (hexId.equalsIgnoreCase("P17") || hexId.equalsIgnoreCase("P19"))) {
                    RevenueBonus bonus = new RevenueBonus(10, "Bridge");
                    bonus.addVertex(v);
                    revenueAdapter.addRevenueBonus(bonus);
                    applied = true;
                }
            }
        }

        return applied;
    }

    @Override
    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return null;
    }
}