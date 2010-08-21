/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PhaseManager.java,v 1.20 2010/01/31 22:22:28 macfreek Exp $ */
package rails.game;

import java.util.*;

import org.apache.log4j.Logger;

import rails.game.state.State;
import rails.util.Tag;

public class PhaseManager implements ConfigurableComponentI {

    protected ArrayList<Phase> phaseList;
    protected HashMap<String, Phase> phaseMap;

    protected int numberOfPhases = 0;
    protected State currentPhase = new State("CurrentPhase", Phase.class);

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

    public void finishConfiguration (GameManagerI gameManager) {
        this.gameManager = gameManager;
        PhaseI initialPhase = phaseList.get(0);
        setPhase(initialPhase);
    }

    public PhaseI getCurrentPhase() {
        return (PhaseI) currentPhase.get();
    }

    public int getCurrentPhaseIndex() {
        return getCurrentPhase().getIndex();
    }

    public void setPhase(String name) {
        setPhase(phaseMap.get(name));
    }

    protected void setPhase(PhaseI phase) {
        if (phase != null) {
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
