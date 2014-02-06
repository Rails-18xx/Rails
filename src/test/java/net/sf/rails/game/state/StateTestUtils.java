package net.sf.rails.game.state;

import net.sf.rails.game.state.ChangeSet;
import net.sf.rails.game.state.Root;

/**
 * Common Utilities for State Testing
 */
class StateTestUtils {

    public static Root setUpRoot() {
        Root root = Root.create();
        close(root);
        return root;
    }
    
    public static void close(Root root) {
        // starts a non-initial ChangeSet
        root.getStateManager().getChangeStack().close(new ChangeActionImpl());
    }

    public static void undo(Root root) {
        root.getStateManager().getChangeStack().undo();
    }
    
    public static void closeAndUndo(Root root) {
        root.getStateManager().getChangeStack().close(new ChangeActionImpl());
        root.getStateManager().getChangeStack().undo();
    }
    
    public static void redo(Root root) {
        root.getStateManager().getChangeStack().redo();
    }
    
    public static ChangeSet getPreviousChangeSet(Root root) {
        return root.getStateManager().getChangeStack().getPreviousChangeSet();
        
    }
     
}
