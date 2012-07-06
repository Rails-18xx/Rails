package rails.game.state;

import org.mockito.Mock;

import rails.game.Player;
import rails.game.action.PossibleAction;

/**
 * Common Utilities for State Testing
 */
class StateTestUtils {
    @Mock static Player player;
    @Mock static PossibleAction action;
    
    public static Root setUpRoot() {
        Root root = Root.create();
        // avoid initial changeSet as it is not undoable
        root.getStateManager().getChangeStack().startActionChangeSet(player, action);
        return root;
    }
}
