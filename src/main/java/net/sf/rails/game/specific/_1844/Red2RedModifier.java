package net.sf.rails.game.specific._1844;

import java.util.HashSet;
import java.util.Set;

import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueBonus;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.algorithms.RevenueAdapter.VertexVisit;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Station;
import net.sf.rails.game.TileColour;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO: Check for East -West Connection and North South all others dont get Boni.

public class Red2RedModifier implements RevenueStaticModifier {

    protected static Logger log =
        LoggerFactory.getLogger(Red2RedModifier.class);
    

    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        
        // 1. define value
        Phase phase = revenueAdapter.getPhase();
        int bonusValue;
        if (phase.isTileColourAllowed(TileColour.GREY.name())) {
            bonusValue = 30;
        } else if (phase.isTileColourAllowed(TileColour.BROWN.name())) {
            bonusValue = 20;
        } else if (phase.isTileColourAllowed(TileColour.GREEN.name())) {
            bonusValue = 10;
        } else {
            return false;
        }

        log.info("OffBoardRevenueModifier: bonusValue = " + bonusValue);
        
        // 2. get all off-board type stations and Hamburg
        Set<NetworkVertex> offBoard = new HashSet<NetworkVertex>();
        for (NetworkVertex vertex:revenueAdapter.getVertices()) {
            if (vertex.isStation() && vertex.getStation().getType() == Station.Type.OFFMAPCITY){
                offBoard.add(vertex);
            }
        }

/*        // 3. get Hamburg ...
        NetworkVertex hamburgCity = NetworkVertex.getVertexByIdentifier(revenueAdapter.getVertices(), "B7.-1");
        if (hamburgCity != null) {
            // ... and duplicate the vertex
            NetworkVertex hamburgTerminal = NetworkVertex.duplicateVertex(revenueAdapter.getGraph(), hamburgCity, "B7", true);
            hamburgTerminal.setSink(true);
            offBoard.add(hamburgTerminal);
            // following statement was missing, it removes bug reported by James Romano (2012/01/05)
            offBoard.remove(hamburgCity);
            
            // vertexVisitSet for the two Hamburgs
            VertexVisit hamburgSet = revenueAdapter.new VertexVisit();
            hamburgSet.set.add(hamburgCity);
            hamburgSet.set.add(hamburgTerminal);
            revenueAdapter.addVertexVisitSet(hamburgSet);
        }

        log.info("OffBoardRevenueModifier: offBoard = " + offBoard);
        */
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
        // no additional text required
        return false;
    }


    public String prettyPrint(RevenueAdapter revenueAdapter) {
        // nothing to do
        return null;
    }
}
