/**
 * 
 */
package net.sf.rails.game.specific._1880;

import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueBonus;
import net.sf.rails.algorithms.RevenueStaticModifier;

/**
 * Transsib modifier for 1880: If Russia and Wladivostok offboard hexes
 * are connected a 50 value is added for that run
 */
public class TranssibModifier implements RevenueStaticModifier {
        
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        RevenueBonus bonus = new RevenueBonus(50, "Transsib");
        NetworkVertex russia = NetworkVertex.getVertexByIdentifier(revenueAdapter.getVertices(), "A3.-1");
        NetworkVertex vladivostok = NetworkVertex.getVertexByIdentifier(revenueAdapter.getVertices(), "A15.-1");
        bonus.addVertex(russia);
        bonus.addVertex(vladivostok);
        
        revenueAdapter.addRevenueBonus(bonus);
        
        return false; // no pretty print
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return null;
    }

}
