package rails.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rails.game.ConfigurationException;
import rails.game.GameManagerI;
import rails.game.MapHex;
import rails.game.PhaseI;
import rails.game.PhaseManager;
import rails.game.TrainManager;
import rails.game.TrainTypeI;
import rails.util.Tag;

/** 
 * defines a template for a revenue bonus at creation time of rails objects
 * will be converted to a true RevenueBonus object during each revenue calculation
 * @author freystef
 */
public class RevenueBonusTemplate {

    protected static Logger log =
        Logger.getLogger(RevenueBonusTemplate.class.getPackage().getName());

    // bonus value
    private final int value;

    // template condition attributes
    private final List<Integer> identVertices;
    private final List<String> identTrainTypes;
    private final List<String> identPhases;

    public RevenueBonusTemplate(Tag tag) throws
    ConfigurationException {
     
        value = tag.getAttributeAsInteger("value");

        identVertices = new ArrayList<Integer>();
        identTrainTypes = new ArrayList<String>();
        identPhases = new ArrayList<String>();

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
        log.info("Created " + this);
    }

    public RevenueBonus toRevenueBonus(MapHex hex, GameManagerI gm, NetworkGraphBuilder ngb) {
        log.info("Convert " + this);
        RevenueBonus bonus = new RevenueBonus(value);
        if (!convertVertices(bonus, ngb, hex)) {
            log.info("Not all vertices found");
            return null; 
        }
        convertTrainTypes(bonus, gm.getTrainManager());
        convertPhases(bonus, gm.getPhaseManager());
        log.info("Converted to " + bonus);
        return bonus;
    }

    private boolean convertVertices(RevenueBonus bonus, NetworkGraphBuilder ngb, MapHex hex) {
        for (Integer identVertex:identVertices) {
            NetworkVertex vertex = ngb.getVertex(hex, identVertex);
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
            TrainTypeI trainType = tm.getTypeByName(identTrainType);
            if (trainType != null) {
                bonus.addTrainType(trainType);
            }
        }
    }
    
    private void convertPhases(RevenueBonus bonus, PhaseManager pm) {
        for (String identPhase:identPhases) {
            PhaseI phase = pm.getPhaseByName(identPhase);
            if (phase != null) {
                bonus.addPhase(phase);
            }
        }
    }
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("RevenueBonusTemplate with value " + value);
        s.append(", identVertices = " + identVertices);
        s.append(", identTrainTypes = " + identTrainTypes);
        s.append(", identPhases = " + identPhases);
        return s.toString();
    }
}
