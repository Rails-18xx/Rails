package net.sf.rails.game.specific._18Kaas;

import java.util.HashSet;
import java.util.Set;

import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueBonus;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.RailsRoot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RuhrRevenueModifier implements RevenueStaticModifier, Configurable {

    private static final Logger log = LoggerFactory.getLogger(RuhrRevenueModifier.class);

    private boolean doublesOnlyMajors;

    public void configureFromXML(Tag tag) throws ConfigurationException {
        // does nothing
    }

    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {
        doublesOnlyMajors = GameOption.getAsBoolean(root, "18KaasRuhrgebiedDoublesOnlyMajors");
        log.debug("Finish configuration of RuhrRevenueModifier, doublesOnlyMajors = " + doublesOnlyMajors);
    }

    // creates revenueBonuses that double the value of each station/value vertex
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {

        Set<NetworkVertex> ruhrGebied = new HashSet<NetworkVertex>();
        for (NetworkVertex vertex:revenueAdapter.getVertices()) {
            // 1. get all vertices that point to Ruhrgebied
            if (vertex.getStopName() != null && vertex.getStopName().equals("Ruhrgebied")){
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
