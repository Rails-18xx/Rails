package rails.game.state;

import rails.game.Player;
import rails.game.action.PossibleAction;

final class ActionChangeSet extends ChangeSet {

    private final Player player;
    
    private final PossibleAction action;
    
    ActionChangeSet(Player player, PossibleAction action) {
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
