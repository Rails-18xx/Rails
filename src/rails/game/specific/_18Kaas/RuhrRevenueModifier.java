package rails.game.specific._18Kaas;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueBonus;
import rails.algorithms.RevenueStaticModifier;
import rails.common.parser.ConfigurableComponent;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.GameManager;

public class RuhrRevenueModifier implements RevenueStaticModifier, ConfigurableComponent {

    protected static Logger log =
        LoggerFactory.getLogger(RuhrRevenueModifier.class.getPackage().getName());

    private boolean doublesOnlyMajors;
    
    public void configureFromXML(Tag tag) throws ConfigurationException {
        // does nothing
    }

    public void finishConfiguration(GameManager parent)
            throws ConfigurationException {
        doublesOnlyMajors = parent.getGameOption("18KaasRuhrgebiedDoublesOnlyMajors").equalsIgnoreCase("yes");
        log.debug("Finish configuration of RuhrRevenueModifier, doublesOnlyMajors = " + doublesOnlyMajors);
    }

    // creates revenueBonuses that double the value of each station/value vertex
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
         
        Set<NetworkVertex> ruhrGebied = new HashSet<NetworkVertex>();
        for (NetworkVertex vertex:revenueAdapter.getVertices()) {
            // 1. get all vertices that point to Ruhrgebied
            if (vertex.getCityName() != null && vertex.getCityName().equals("Ruhrgebied")){
                ruhrGebied.add(vertex);
            } 
        }
        
        // 2. add revenue bonuses for stations
        for (NetworkVertex vertex:revenueAdapter.getVertices()) {
            if (!ruhrGebied.contains(vertex) && vertex.isStation() && (vertex.isMajor() || !doublesOnlyMajors)) {
                for (NetworkVertex ruhrVertex:ruhrGebied) {
                    RevenueBonus bonus = new RevenueBonus(vertex.getValue(), "Ruhrgebied");
                    bonus.addVertex(vertex);
                    bonus.addVertex(ruhrVertex);
                    revenueAdapter.addRevenueBonus(bonus);
                }
            }
        }
        
        // nothing to print
        return false;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        // nothing to print
        return null;
    }
}
