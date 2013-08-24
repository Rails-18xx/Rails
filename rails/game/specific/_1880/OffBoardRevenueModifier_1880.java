/**
 * 
 */
package rails.game.specific._1880;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueBonus;
import rails.algorithms.RevenueStaticModifier;
import rails.game.Station;

/**
 * @author Martin
 *
 */
public class OffBoardRevenueModifier_1880 implements RevenueStaticModifier {

    
    protected static Logger log =
            Logger.getLogger(OffBoardRevenueModifier_1880.class.getPackage().getName());
        
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
       
        // 1. get the two off-board type stations (Russia and Wladiwostok) 
        Set<NetworkVertex> offBoard = new HashSet<NetworkVertex>();
        for (NetworkVertex vertex:revenueAdapter.getVertices()) { // We just need the two offboard Cities
            if (vertex.isStation() && ((vertex.getStation().getName().equals("Russia") ||(vertex.getStation().getName().equals("Vladivostok"))))) {
                offBoard.add(vertex);
            }
        }
        // 2. get all base tokens (=> start vertices)
        Set<NetworkVertex> bases = revenueAdapter.getStartVertices();
        
        // 3. combine those to revenueBonuses
        // always two offboard areas and one base  
        Set<NetworkVertex> destOffBoard = new HashSet<NetworkVertex>(offBoard);
        for (NetworkVertex offA:offBoard) {
            destOffBoard.remove(offA);
            for (NetworkVertex offB:destOffBoard) {
                for (NetworkVertex base:bases) {
                    RevenueBonus bonus = new RevenueBonus(50, "Red-To-Red");
                    bonus.addVertex(offA); bonus.addVertex(offB); bonus.addVertex(base);
                    revenueAdapter.addRevenueBonus(bonus);
                }
            }
        }

        return false;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        // TODO Auto-generated method stub
        return null;
    }

}
