package net.sf.rails.game.round;

import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsManager;

/**
 * RoundNG is the abstract base class for Round types (like StockRound, OperatingRound, StartRound) in Rails.
 */
public abstract class RoundNG extends RailsManager implements RoundFacade {

    protected RoundNG(RailsItem parent, String id) {
        super(parent, id);
    }

    public abstract void start();
    
    public abstract void finish();
 
}
