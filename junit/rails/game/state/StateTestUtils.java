package rails.game.state;

/**
 * Common Utilities for State Testing
 */
class StateTestUtils {

    public static Root setUpRoot() {
        Root root = Root.create();
        closeAndNew(root);
        return root;
    }
    
    public static void closeAndNew(Root root) {
        // starts a non-initial ChangeSet
        root.getStateManager().getChangeStack().close();
        root.getStateManager().getChangeStack().newChangeSet(null);
    }

    public static void close(Root root) {
        root.getStateManager().getChangeStack().close();
    }
    
    public static void newChangeSet(Root root) {
        root.getStateManager().getChangeStack().newChangeSet(null);
    }
    
    public static void undo(Root root) {
        root.getStateManager().getChangeStack().undo();
    }
    
    public static void closeAndUndo(Root root) {
        root.getStateManager().getChangeStack().close();
        root.getStateManager().getChangeStack().undo();
    }
    
    public static void redo(Root root) {
        root.getStateManager().getChangeStack().redo();
    }
    
    public static ChangeSet getCurrentChangeSet(Root root) {
        return root.getStateManager().getChangeStack().getCurrentChangeSet();
    }
    
    public static ChangeSet getLastClosedChangeSet(Root root) {
        return root.getStateManager().getChangeStack().getPreviousChangeSet();
        
    }
     
}
