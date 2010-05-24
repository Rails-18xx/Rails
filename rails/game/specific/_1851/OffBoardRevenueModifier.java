package rails.game.specific._1851;

import java.util.HashSet;
import java.util.Set;

import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueBonus;
import rails.algorithms.RevenueStaticModifier;
import rails.game.Station;

public class OffBoardRevenueModifier implements RevenueStaticModifier {

    private static final int BONUS_VALUE = 10;
    
    public void modifyCalculator(RevenueAdapter revenueAdapter) {
        // 1. get all off-board type stations and all other stations
        Set<NetworkVertex> offBoard = new HashSet<NetworkVertex>();
        Set<NetworkVertex> otherStations = new HashSet<NetworkVertex>();
        for (NetworkVertex vertex:revenueAdapter.getVertices()) {
            if (vertex.isStation()) {
                if (vertex.getStation().getType().equals(Station.OFF_MAP_AREA)){
                    offBoard.add(vertex);
                } else {
                    otherStations.add(vertex);
                }
            }
        }
        // 2. combine those to revenueBonuses
        // always two offboard areas and one other
        Set<NetworkVertex> destOffBoard = new HashSet<NetworkVertex>(offBoard);
        RevenueBonus bonus;
        for (NetworkVertex offA:offBoard) {
            destOffBoard.remove(offA);
            for (NetworkVertex offB:destOffBoard) {
                bonus = new RevenueBonus(2*BONUS_VALUE, "Red-To-Red");
                bonus.addVertex(offA); bonus.addVertex(offB);;
                revenueAdapter.addRevenueBonus(bonus);
                for (NetworkVertex station:otherStations) {
                    bonus = new RevenueBonus(BONUS_VALUE, "Red-To-Red");
                    bonus.addVertex(offA); bonus.addVertex(offB); bonus.addVertex(station);
                    revenueAdapter.addRevenueBonus(bonus);
                }
            }
        }
    }
}
