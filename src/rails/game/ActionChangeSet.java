package rails.game;

import rails.game.action.PossibleAction;
import rails.game.state.ChangeSet;

class ActionChangeSet extends ChangeSet {

    // static fields
    private final Player player;
    private final PossibleAction action;
    
    ActionChangeSet(Player player, PossibleAction action) {
        // it is blocking, but never intitial
        super(true, false);
        this.player = player;
        this.action = action;
    }
    
    Player getPlayer() {
        return player;
    }
    
    PossibleAction getAction() {
        return action;
    }
    
    @Override
    public String toString() {
        return "ActionChangeSet for player " + player + " and action " + action;
    }
}
