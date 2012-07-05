package rails.game.state;
/**
 * Base Class for all Change Objects
 * 
 * Replaces previous move interface
 */
abstract class Change {
    
    protected void init(State state){
        state.getRoot().getStateManager().getChangeStack().getCurrentChangeSet().addChange(this);
    }

    abstract void execute();
    abstract void undo();  
    abstract State getState();

}
