package rails.game.specific._18EU;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueBonus;
import rails.algorithms.RevenueStaticModifier;
import rails.algorithms.RevenueAdapter.VertexVisit;
import rails.game.PhaseI;
import rails.game.Station;
import rails.game.Tile;

public class OffBoardRevenueModifier implements RevenueStaticModifier {

    protected static Logger log =
        Logger.getLogger(OffBoardRevenueModifier.class.getPackage().getName());
    

    public void modifyCalculator(RevenueAdapter revenueAdapter) {
        
        // 1. define value
        PhaseI phase = revenueAdapter.getPhase();
        int bonusValue;
        if (phase.isTileColourAllowed(Tile.GREY_COLOUR_NAME)) {
            bonusValue = 30;
        } else if (phase.isTileColourAllowed(Tile.BROWN_COLOUR_NAME)) {
            bonusValue = 20;
        } else if (phase.isTileColourAllowed(Tile.GREEN_COLOUR_NAME)) {
            bonusValue = 10;
        } else {
            return;
        }

        log.info("OffBoardRevenueModifier: bonusValue = " + bonusValue);
        
        // 2. get all off-board type stations and Hamburg
        Set<NetworkVertex> offBoard = new HashSet<NetworkVertex>();
        for (NetworkVertex vertex:revenueAdapter.getVertices()) {
            if (vertex.isStation() && vertex.getStation().getType().equals(Station.OFF_MAP_AREA)){
                offBoard.add(vertex);
            }
        }

        // 3. get Hamburg ...
        NetworkVertex hamburgCity = NetworkVertex.getVertexByIdentifier(revenueAdapter.getVertices(), "B7.-1");
        if (hamburgCity != null) {
            // ... and duplicate the vertex
            NetworkVertex hamburgTerminal = NetworkVertex.duplicateVertex(revenueAdapter.getGraph(), hamburgCity, "B7", true);
            hamburgTerminal.setSink(true);
            offBoard.add(hamburgTerminal);
            
            // vertexVisitSet for the two Hamburgs
            VertexVisit hamburgSet = revenueAdapter.new VertexVisit();
            hamburgSet.set.add(hamburgCity);
            hamburgSet.set.add(hamburgTerminal);
            revenueAdapter.addVertexVisitSet(hamburgSet);
        }

        log.info("OffBoardRevenueModifier: offBoard = " + offBoard);
        
        // 4. get all base tokens (=> start vertices)
        Set<NetworkVertex> bases = revenueAdapter.getStartVertices();
        
        
        // 5. combine those to revenueBonuses
        // always two offboard areas and one base
        Set<NetworkVertex> destOffBoard = new HashSet<NetworkVertex>(offBoard);
        for (NetworkVertex offA:offBoard) {
            destOffBoard.remove(offA);
            for (NetworkVertex offB:destOffBoard) {
                for (NetworkVertex base:bases) {
                    RevenueBonus bonus = new RevenueBonus(bonusValue, "Red-To-Red");
                    bonus.addVertex(offA); bonus.addVertex(offB); bonus.addVertex(base);
                    revenueAdapter.addRevenueBonus(bonus);
                }
            }
        }
    }
}
