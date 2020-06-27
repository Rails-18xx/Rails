package net.sf.rails.game.specific._1851;

import java.util.HashSet;
import java.util.Set;

import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueBonus;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.game.Stop;


public class OffBoardRevenueModifier implements RevenueStaticModifier {

    private static final int BONUS_VALUE = 10;
    
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        // 1. get all off-board type stations and all other stations
        Set<NetworkVertex> offBoard = new HashSet<>();
        Set<NetworkVertex> otherStations = new HashSet<>();
        for (NetworkVertex vertex:revenueAdapter.getVertices()) {
            if (vertex.isStation()) {
                if (vertex.getStation().getType() == Stop.Type.OFFMAP) {
                    offBoard.add(vertex);
                } else {
                    otherStations.add(vertex);
                }
            }
        }
        // 2. combine those to revenueBonuses
        // always two offboard areas and one other
        Set<NetworkVertex> destOffBoard = new HashSet<>(offBoard);
        RevenueBonus bonus;
        for (NetworkVertex offA:offBoard) {
            destOffBoard.remove(offA);
            for (NetworkVertex offB:destOffBoard) {
                bonus = new RevenueBonus(2*BONUS_VALUE, "Red-To-Red");
                bonus.addVertex(offA); bonus.addVertex(offB);
                revenueAdapter.addRevenueBonus(bonus);
                for (NetworkVertex station:otherStations) {
                    bonus = new RevenueBonus(BONUS_VALUE, "Red-To-Red");
                    bonus.addVertex(offA); bonus.addVertex(offB); bonus.addVertex(station);
                    revenueAdapter.addRevenueBonus(bonus);
                }
            }
        }
        // no additional text required
        return false;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        // nothing to print
        return null;
    }
}
