package rails.game.state;

import rails.game.Player;

/**
 * AutoChangeSets are linked to previous ActionChangeSets 
 * @author freystef
 */
final class AutoChangeSet extends ChangeSet {

    private final ActionChangeSet previous;

    AutoChangeSet(ActionChangeSet previous) {
        this.previous = previous;
    }
    
    ActionChangeSet getPrevious() {
        return previous;
    }
    
    @Override
    boolean isUndoableByPlayer(Player player) {
        return false;
    }

    @Override
    public String toString() {
        return "AutoChangeSet linked to action " + previous.getAction();
    }
    
}
