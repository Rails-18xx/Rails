package net.sf.rails.game.round;

import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsManager;

/**
 * RoundManager is the parent of all Rounds.
 */

public abstract class RoundManager extends RailsManager {

    protected RoundManager(RailsItem parent, String id) {
        super(parent, id);
    }
    
    public abstract RoundNG getCurrentRound();
    
}
