package rails.game.state;

import rails.game.Player;

/**
 * AutoChangeSets are ChangeSets that belong to no action directly
 */
final class AutoChangeSet extends ChangeSet {


    AutoChangeSet() {}
    
    @Override
    boolean isUndoableByPlayer(Player player) {
        return false;
    }

    @Override
    public String toString() {
        return "AutoChangeSet";
    }
    
}
