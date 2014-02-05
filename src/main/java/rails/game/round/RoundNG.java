package rails.game.round;

import rails.game.Player;
import rails.game.RailsItem;
import rails.game.RailsManager;
import rails.game.action.PossibleAction;

/**
 * RoundNG is the abstract base class for Round types (like StockRound, OperatingRound, StartRound) in Rails.
 * 
 */

public abstract class RoundNG extends RailsManager {

    protected RoundNG(RailsItem parent, String id) {
        super(parent, id);
    }

    public abstract void start();
    
    public abstract Iterable<PossibleAction> getActions();
    
    public abstract Player getCurrentPlayer();
    
    public abstract boolean process(PossibleAction action);
    
    public abstract void finish();
    
}
