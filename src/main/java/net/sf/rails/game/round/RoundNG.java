package net.sf.rails.game.round;

import rails.game.action.PossibleAction;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsManager;

/**
 * RoundNG is the abstract base class for Round types (like StockRound, OperatingRound, StartRound) in Rails.
 */
public abstract class RoundNG extends RailsManager {

    protected RoundNG(RailsItem parent, String id) {
        super(parent, id);
    }

    public abstract void start();
    
    public abstract Iterable<PossibleAction> getActions();
    
    public abstract boolean process(PossibleAction action);
    
    public abstract void finish();
    

}
