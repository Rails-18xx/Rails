package rails.game.specific._1880;

import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueBonus;
import rails.algorithms.RevenueStaticModifier;

public class TransSibModifier implements RevenueStaticModifier {

    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        RevenueBonus bonus = new RevenueBonus(50, "TransSib");
        NetworkVertex russia = NetworkVertex.getVertexByIdentifier(revenueAdapter.getVertices(),"A3.-1");
        NetworkVertex vladiwostok = NetworkVertex.getVertexByIdentifier(revenueAdapter.getVertices(),"A15.-1");
        bonus.addVertex(russia);
        bonus.addVertex(vladiwostok);
        
        revenueAdapter.addRevenueBonus(bonus);
        
        return false;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        // TODO Auto-generated method stub
        return null;
    }

   

}
