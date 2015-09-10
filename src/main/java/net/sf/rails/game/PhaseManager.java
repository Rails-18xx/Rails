package net.sf.rails.game;

import java.util.List;
import java.util.Map;

import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.Owner;
import net.sf.rails.game.state.State;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class PhaseManager extends RailsManager implements Configurable {
    // static data
    private final List<Phase> phaseList = Lists.newArrayList();
    private final Map<String, Phase> phaseMap = Maps.newHashMap();
    
    // dynamic data
    private final GenericState<Phase> currentPhase = GenericState.create(this, "currentPhase");

    /**
     * Used by Configure (via reflection) only
     */
    public PhaseManager(RailsRoot parent, String id) {
        super(parent, id);
    }
    
    public void configureFromXML(Tag tag) throws ConfigurationException {
        /*
         * Phase class name is now fixed but can be made configurable, if
         * needed.
         */
        List<Tag> phaseTags = tag.getChildren("Phase");
        Phase phase;
        Phase previousPhase = null;
        String name;

        int n = 0;
        for (Tag phaseTag : phaseTags) {
            name = phaseTag.getAttributeAsString("name", String.valueOf(n + 1));
            phase = new Phase(this, name, n++, previousPhase);
            phaseList.add(phase);
            phaseMap.put(name, phase);
            phase.configureFromXML(phaseTag);
            previousPhase = phase;
        }
    }
    
    public void finishConfiguration (RailsRoot root) 
    throws ConfigurationException {
        
        for (Phase phase : phaseList) {
            phase.finishConfiguration(root);
        }
        
        Phase initialPhase = phaseList.get(0);
        setPhase(initialPhase, null);
    }

    public Phase getCurrentPhase() {
        return currentPhase.value();
    }
    
    public State getCurrentPhaseModel() {
        return currentPhase;
    }

    public int getCurrentPhasendex() {
        return getCurrentPhase().getIndex();
    }

    public void setPhase(String name, Owner lastTrainBuyer) {
        setPhase(phaseMap.get(name), lastTrainBuyer);
    }

    public void setPhase(Phase phase, Owner lastTrainBuyer) {
        if (phase != null) {
            phase.setLastTrainBuyer (lastTrainBuyer);
            currentPhase.set(phase);
            phase.activate();
        }
    }

    public Phase getPhaseByName(String name) {
        return phaseMap.get(name);
    }

    public boolean hasReachedPhase(String phaseName) {
        return getCurrentPhase().getIndex() >= getPhaseByName(phaseName).getIndex();

    }

    public List<Phase> getPhases() {
        return phaseList;
    }
    
}
