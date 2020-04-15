package net.sf.rails.algorithms;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Phase;
import net.sf.rails.game.PhaseManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.TrainManager;
import net.sf.rails.game.TrainType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * defines a template for a revenue bonus at creation time of rails objects
 * will be converted to a true RevenueBonus object during each revenue calculation
 * @author freystef
 */
public final class RevenueBonusTemplate implements Configurable {

    private static final Logger log = LoggerFactory.getLogger(RevenueBonusTemplate.class);

    // bonus value
    private int value;

    // bonus name
    private String name;

    // template condition attributes
    private final List<Integer> identVertices;
    private final List<String> identTrainTypes;
    private final List<String> identPhases;

    public RevenueBonusTemplate() {
        identVertices = new ArrayList<>();
        identTrainTypes = new ArrayList<>();
        identPhases = new ArrayList<>();
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {
        value = tag.getAttributeAsInteger("value");
        name = tag.getAttributeAsString("name");

        // check for vertices
        List<Tag> vertexTags = tag.getChildren("Vertex");
        if (vertexTags != null) {
            for (Tag vertexTag:vertexTags) {
                Integer id = vertexTag.getAttributeAsInteger("id");
                if (id != null) {
                    identVertices.add(id);
                }
            }
        }

        // check for train (types)
        List<Tag> trainTags = tag.getChildren("Train");
        if (trainTags != null) {
            for (Tag trainTag:trainTags) {
                String type = trainTag.getAttributeAsString("type");
                if (type != null) {
                    identTrainTypes.add(type);
                }
            }
        }

        // check for phases
        List<Tag> phaseTags = tag.getChildren("phase");
        if (phaseTags != null) {
            for (Tag phaseTag:phaseTags) {
                String type = phaseTag.getAttributeAsString("name");
                if (type != null) {
                    identPhases.add(type);
                }
            }
        }
        log.debug("Configured {}", this);

    }

    /**
     * is not used, use toRevenueBonus instead
     */
    public void finishConfiguration(RailsRoot parent)
            throws ConfigurationException {
        throw new ConfigurationException("Use toRevenueBonus");
    }

    public RevenueBonus toRevenueBonus(MapHex hex, RailsRoot root, NetworkGraph graph) {
        log.debug("Convert {}", this);
        RevenueBonus bonus = new RevenueBonus(value, name);
        if (!convertVertices(bonus, graph, hex)) {
            log.warn("Not all vertices of RevenueBonusTemplate found " + this.toString());
            return null;
        }
        convertTrainTypes(bonus, root.getTrainManager());
        convertPhases(bonus, root.getPhaseManager());
        log.debug("Converted to {}", bonus);
        return bonus;
    }

    private boolean convertVertices(RevenueBonus bonus, NetworkGraph graph, MapHex hex) {
        for (Integer identVertex:identVertices) {
            NetworkVertex vertex = graph.getVertex(hex, identVertex);
            if (vertex == null) {
                return false;
            } else {
                bonus.addVertex(vertex);
            }
        }
        return true;
    }

    private void convertTrainTypes(RevenueBonus bonus, TrainManager tm) {
        for (String identTrainType:identTrainTypes) {
            TrainType trainType = tm.getTypeByName(identTrainType);
            if (trainType != null) {
                bonus.addTrainType(trainType);
            }
        }
    }

    private void convertPhases(RevenueBonus bonus, PhaseManager pm) {
        for (String identPhase:identPhases) {
            Phase phase = pm.getPhaseByName(identPhase);
            if (phase != null) {
                bonus.addPhase(phase);
            }
        }
    }

    /**
     *  @return bonus name for display
     */
    public String getName() {
        return name;
    }

    /**
     * @return bonus toolTip text
     */
    public String getToolTip() {
        StringBuilder s = new StringBuilder();
        s.append(value);
        if (identPhases.size() != 0) {
            s.append(identPhases);
            if (identTrainTypes.size() != 0) {
                s.append("");
            }
        }
        if (identTrainTypes.size() != 0) {
            s.append(identTrainTypes);
        }
        return s.toString();
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("RevenueBonusTemplate");
        if (name == null)
            s.append (" unnamed");
        else
            s.append(" name = ").append(name);
        s.append(", value ").append(value);
        s.append(", identVertices = ").append(identVertices);
        s.append(", identTrainTypes = ").append(identTrainTypes);
        s.append(", identPhases = ").append(identPhases);
        return s.toString();
    }

}
