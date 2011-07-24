/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PhaseManager.java,v 1.20 2010/01/31 22:22:28 macfreek Exp $ */
package rails.game;

import java.util.*;

import org.apache.log4j.Logger;

import rails.common.parser.ConfigurableComponentI;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.state.AbstractItem;
import rails.game.state.GenericState;
import rails.game.state.State;

public class PhaseManager extends AbstractItem implements ConfigurableComponentI {

    protected ArrayList<Phase> phaseList;
    protected HashMap<String, Phase> phaseMap;

    protected int numberOfPhases = 0;
    protected GenericState<PhaseI> currentPhase = new GenericState<PhaseI>(this, "CurrentPhase");

    // Can be removed once setPhase() has been redone.
    protected GameManagerI gameManager;

    protected static Logger log =
        Logger.getLogger(PhaseManager.class.getPackage().getName());

    public PhaseManager() {}

    public void configureFromXML(Tag tag) throws ConfigurationException {
        /*
         * Phase class name is now fixed but can be made configurable, if
         * needed.
         */
        List<Tag> phaseTags = tag.getChildren("Phase");
        numberOfPhases = phaseTags.size();
        phaseList = new ArrayList<Phase>();
        phaseMap = new HashMap<String, Phase>();
        Phase phase;
        Phase previousPhase = null;
        String name;

        int n = 0;
        for (Tag phaseTag : phaseTags) {
            name = phaseTag.getAttributeAsString("name", String.valueOf(n + 1));
            phase = new Phase(n++, name, previousPhase);
            phaseList.add(phase);
            phaseMap.put(name, phase);
            phase.configureFromXML(phaseTag);
            previousPhase = phase;
        }
    }

    public void finishConfiguration (GameManagerI gameManager) 
    throws ConfigurationException {
        this.gameManager = gameManager;
        
        for (Phase phase : phaseList) {
            phase.finishConfiguration(gameManager);
        }
        
        PhaseI initialPhase = phaseList.get(0);
        setPhase(initialPhase, null);
    }

    public PhaseI getCurrentPhase() {
        return (PhaseI) currentPhase.get();
    }
    
    public State getCurrentPhaseModel() {
        return currentPhase;
    }

    public int getCurrentPhaseIndex() {
        return getCurrentPhase().getIndex();
    }

    public void setPhase(String name, Portfolio lastTrainBuyer) {
        setPhase(phaseMap.get(name), lastTrainBuyer);
    }

    protected void setPhase(PhaseI phase, Portfolio lastTrainBuyer) {
        if (phase != null) {
            phase.setLastTrainBuyer (lastTrainBuyer);
            currentPhase.set(phase);

            // TODO Redundant, should be replaced by phase.activate()
            // as soon as privates closing is included there.
            // Please consider Undo/Redo as well
            gameManager.initialiseNewPhase(phase);
        }
    }

    public PhaseI getPhaseByName(String name) {
        return phaseMap.get(name);
    }

    public boolean hasReachedPhase(String phaseName) {
        return getCurrentPhase().getIndex() >= getPhaseByName(phaseName).getIndex();

    }

    public List<Phase> getPhases() {
        return phaseList;
    }

}
