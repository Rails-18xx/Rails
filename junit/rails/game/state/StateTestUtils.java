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
        startActionChangeSet(root);
        return root;
    }
    
    public static void startActionChangeSet(Root root) {
        root.getStateManager().getChangeStack().startChangeSet(new ChangeSet(true, false));
    }

    public static void close(Root root) {
        root.getStateManager().getChangeStack().closeCurrentChangeSet();
    }
    
    public static void undo(Root root) {
        root.getStateManager().getChangeStack().undo();
    }
    
    public static void closeAndUndo(Root root) {
        root.getStateManager().getChangeStack().closeCurrentChangeSet();
        root.getStateManager().getChangeStack().undo();
    }
    
    public static void redo(Root root) {
        root.getStateManager().getChangeStack().redo();
    }
    
    public static ChangeSet getCurrentChangeSet(Root root) {
        return root.getStateManager().getChangeStack().getCurrentChangeSet();
    }
    
    public static ChangeSet getLastClosedChangeSet(Root root) {
        return root.getStateManager().getChangeStack().getLastClosedChangeSet();
        
    }
     
}
